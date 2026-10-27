package com.myvapp.app;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.webkit.*;import android.widget.*;import android.Manifest;import android.net.Uri;import android.provider.MediaStore;import android.util.Base64;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
public class MainActivity extends Activity{
static final boolean PULL=false,PROG=false,BACK=true,EXIT=false;
WebView w;ProgressBar pb;SwipeRefreshLayout sr;ValueCallback<Uri[]> fc;
@Override protected void onActivityResult(int rq,int rs,Intent d){if(rq==7&&fc!=null){fc.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(rs,d));fc=null;}else super.onActivityResult(rq,rs,d);}
@Override protected void onCreate(Bundle b){super.onCreate(b);
Window win=getWindow();



FrameLayout root=new FrameLayout(this);root.setBackgroundColor(0xFFFFFFFF);
root.setOnApplyWindowInsetsListener((x,i)->{x.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i.consumeSystemWindowInsets();});
w=new WebView(this);WebSettings s=w.getSettings();
s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(true);s.setMediaPlaybackRequiresUserGesture(false);

pb=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);pb.setMax(100);pb.setVisibility(View.GONE);
sr=new SwipeRefreshLayout(this);sr.setEnabled(PULL);sr.setOnRefreshListener(()->w.reload());sr.setOnChildScrollUpCallback((p,c)->w.getScrollY()>0);sr.addView(w);
w.setWebViewClient(new WebViewClient(){
@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){String u=r.getUrl().toString();if(u.startsWith("http")||u.startsWith("file:"))return false;try{startActivity(new Intent(Intent.ACTION_VIEW,r.getUrl()));}catch(Exception e){}return true;}
@Override public void onPageFinished(WebView v,String u){sr.setRefreshing(false);}
@Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){}
});
w.setWebChromeClient(new WebChromeClient(){
@Override public void onProgressChanged(WebView v,int p){pb.setProgress(p);pb.setVisibility(PROG&&p<100?View.VISIBLE:View.GONE);}
@Override public void onPermissionRequest(PermissionRequest r){runOnUiThread(()->r.grant(r.getResources()));}
@Override public void onGeolocationPermissionsShowPrompt(String og,GeolocationPermissions.Callback c){c.invoke(og,true,false);}
@Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> cb,FileChooserParams fp){if(fc!=null)fc.onReceiveValue(null);fc=cb;try{startActivityForResult(fp.createIntent(),7);}catch(Exception e){fc=null;t("تعذر فتح المعرض");return false;}return true;}
});
w.addJavascriptInterface(this,"Android");
w.setDownloadListener((u,ua,cd,mt,ln)->{String fn=URLUtil.guessFileName(u,cd,mt).replace("'","");
if(u.startsWith("blob:"))w.evaluateJavascript("fetch('"+u+"').then(function(r){return r.blob()}).then(function(bl){var f=new FileReader();f.onload=function(){Android.save(f.result,'"+fn+"')};f.readAsDataURL(bl)})",null);
else if(u.startsWith("data:"))save(u,fn);
else{DownloadManager.Request q=new DownloadManager.Request(Uri.parse(u));q.setMimeType(mt);String ck=CookieManager.getInstance().getCookie(u);if(ck!=null)q.addRequestHeader("cookie",ck);q.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);q.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,fn);((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(q);t("بدأ التنزيل");}});
root.addView(sr,new FrameLayout.LayoutParams(-1,-1));root.addView(pb,new FrameLayout.LayoutParams(-1,12));
setContentView(root);
java.util.ArrayList<String> ps=new java.util.ArrayList<>(java.util.Arrays.asList(new String[]{}));
if(Build.VERSION.SDK_INT<29)ps.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
if(Build.VERSION.SDK_INT>=23&&!ps.isEmpty())requestPermissions(ps.toArray(new String[0]),1);
w.post(()->w.loadUrl("file:///android_asset/index.html"));}
void t(String s){runOnUiThread(()->Toast.makeText(this,s,Toast.LENGTH_SHORT).show());}
@JavascriptInterface public void save(String d,String n){try{int i=d.indexOf(',');String m=d.substring(5,d.indexOf(';'));byte[] by=Base64.decode(d.substring(i+1),0);java.io.OutputStream o;
if(Build.VERSION.SDK_INT>=29){ContentValues v=new ContentValues();v.put(MediaStore.MediaColumns.DISPLAY_NAME,n);v.put(MediaStore.MediaColumns.MIME_TYPE,m);v.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS);o=getContentResolver().openOutputStream(getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v));}
else o=new java.io.FileOutputStream(new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),n));
o.write(by);o.close();t("تم الحفظ في التنزيلات");}catch(Exception e){t("فشل الحفظ");}}
@Override public void onBackPressed(){
if(BACK&&w.canGoBack())w.goBack();
else if(EXIT)new AlertDialog.Builder(this).setMessage("هل تريد الخروج؟").setPositiveButton("نعم",(d,k)->finish()).setNegativeButton("لا",null).show();
else super.onBackPressed();}
}