package com.myapp.app;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.webkit.*;import android.widget.*;import android.Manifest;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
public class MainActivity extends Activity{
static final boolean PULL=false,PROG=false,BACK=true,EXIT=false;
WebView w;ProgressBar pb;SwipeRefreshLayout sr;
@Override protected void onCreate(Bundle b){super.onCreate(b);
Window win=getWindow();



FrameLayout root=new FrameLayout(this);root.setBackgroundColor(0xFFFFFFFF);
root.setOnApplyWindowInsetsListener((x,i)->{x.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});
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
});
root.addView(sr,new FrameLayout.LayoutParams(-1,-1));root.addView(pb,new FrameLayout.LayoutParams(-1,12));
setContentView(root);
String[] ps=new String[]{};
if(Build.VERSION.SDK_INT>=23&&ps.length>0)requestPermissions(ps,1);
w.loadUrl("file:///android_asset/index.html");}
@Override public void onBackPressed(){
if(BACK&&w.canGoBack())w.goBack();
else if(EXIT)new AlertDialog.Builder(this).setMessage("هل تريد الخروج؟").setPositiveButton("نعم",(d,k)->finish()).setNegativeButton("لا",null).show();
else super.onBackPressed();}
}