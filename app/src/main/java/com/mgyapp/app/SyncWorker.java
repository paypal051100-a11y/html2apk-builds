package com.mgyapp.app;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.work.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.json.*;

public class SyncWorker extends Worker{
 static final String[] HOSTS={"broker.emqx.io","broker.hivemq.com"};
 public SyncWorker(@NonNull Context c,@NonNull WorkerParameters p){super(c,p);}
 static SharedPreferences sp(Context c){return c.getSharedPreferences("sync",Context.MODE_PRIVATE);}

 public static synchronized void queue(Context c,String topic,String payloads,String batch){
  try{
   JSONObject o=new JSONObject();o.put("topic",topic);o.put("batch",batch);o.put("payloads",new JSONArray(payloads));
   File tmp=new File(c.getFilesDir(),"pending.tmp"),f=new File(c.getFilesDir(),"pending.json");
   FileOutputStream os=new FileOutputStream(tmp);os.write(o.toString().getBytes("UTF-8"));os.close();
   if(!tmp.renameTo(f)){f.delete();tmp.renameTo(f);}
   sp(c).edit().putString("queued",batch).commit();
   Constraints k=new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
   OneTimeWorkRequest r=new OneTimeWorkRequest.Builder(SyncWorker.class).setConstraints(k).setBackoffCriteria(BackoffPolicy.LINEAR,20,TimeUnit.SECONDS).build();
   WorkManager.getInstance(c).enqueueUniqueWork("sync",ExistingWorkPolicy.REPLACE,r);
  }catch(Exception e){}
 }

 public static boolean pending(Context c){
  SharedPreferences s=sp(c);String q=s.getString("queued",null);
  return q!=null&&!q.equals(s.getString("delivered",null));
 }

 @NonNull @Override public Result doWork(){
  try{
   Context c=getApplicationContext();
   File f=new File(c.getFilesDir(),"pending.json");
   if(!f.exists())return Result.success();
   ByteArrayOutputStream bo=new ByteArrayOutputStream();
   FileInputStream is=new FileInputStream(f);byte[] bf=new byte[8192];int n;
   while((n=is.read(bf))>0)bo.write(bf,0,n);
   is.close();
   JSONObject o=new JSONObject(bo.toString("UTF-8"));
   String topic=o.getString("topic"),batch=o.getString("batch");
   JSONArray a=o.getJSONArray("payloads");
   boolean ok=false;
   for(String h:HOSTS){if(isStopped())return Result.retry();if(publishAll(h,topic,a))ok=true;}
   if(!ok||isStopped())return Result.retry();
   sp(c).edit().putString("delivered",batch).commit();
   return Result.success();
  }catch(Exception e){return Result.retry();}
 }

 boolean publishAll(String h,String topic,JSONArray a){
  Socket sk=null;
  try{
   try{
    SSLSocket ss=(SSLSocket)SSLSocketFactory.getDefault().createSocket();
    ss.connect(new InetSocketAddress(h,8883),8000);ss.startHandshake();sk=ss;
   }catch(Exception e){
    sk=new Socket();sk.connect(new InetSocketAddress(h,1883),8000);
   }
   sk.setSoTimeout(15000);
   DataOutputStream out=new DataOutputStream(new BufferedOutputStream(sk.getOutputStream()));
   DataInputStream in=new DataInputStream(new BufferedInputStream(sk.getInputStream()));
   ByteArrayOutputStream b=new ByteArrayOutputStream();
   str(b,"MQTT");b.write(4);b.write(2);b.write(0);b.write(60);str(b,"tz_"+Long.toHexString(System.nanoTime()));
   send(out,0x10,b.toByteArray());
   if(in.readUnsignedByte()!=0x20)return false;
   in.readUnsignedByte();in.readUnsignedByte();
   if(in.readUnsignedByte()!=0)return false;
   for(int i=0;i<a.length();i++){
    if(isStopped())return false;
    ByteArrayOutputStream p=new ByteArrayOutputStream();
    str(p,topic+"/r/"+i);
    int id=i+1;p.write(id>>8);p.write(id&255);
    byte[] body=a.getString(i).getBytes("UTF-8");
    p.write(body,0,body.length);
    send(out,0x33,p.toByteArray());
    int t;
    do{t=in.readUnsignedByte();int len=in.readUnsignedByte();byte[] skip=new byte[len];in.readFully(skip);}while(t!=0x40);
   }
   send(out,0xE0,new byte[0]);
   return true;
  }catch(Exception e){
   return false;
  }finally{
   try{if(sk!=null)sk.close();}catch(Exception e){}
  }
 }

 static void send(DataOutputStream o,int type,byte[] body)throws IOException{
  o.write(type);int n=body.length;
  do{int d=n%128;n/=128;if(n>0)d|=128;o.write(d);}while(n>0);
  o.write(body,0,body.length);o.flush();
 }
 static void str(ByteArrayOutputStream b,String s)throws IOException{
  byte[] x=s.getBytes("UTF-8");b.write(x.length>>8);b.write(x.length&255);b.write(x,0,x.length);
 }
}