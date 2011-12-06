package controller.multinet;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebViewDatabase;

public class AddDeviceManually extends Activity{
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	//get the settings
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
        WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.webrowser);
        
        WebView myWebView = (WebView) findViewById(R.id.webView);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        myWebView.setWebViewClient(new myWebViewClient( settings.getString("username", ""),settings.getString("passwd", "") ));

        String protocall = null;
        if(settings.getString("routerPort", "").equals("443")) {
        	protocall = "https://";
        } else {
        	protocall = "http://";
        }
        
        String url =  protocall +
        			  settings.getString("routerIp", "") +  
        			  ":" + 
        			  settings.getString("routerPort", "80") +
        			  "/generate.html";
        
        //myWebView.setHttpAuthUsernamePassword(	settings.getString("routerIp", ""), 
       // 										"MultiNet", 
        //										settings.getString("username", ""), 
        //										settings.getString("passwd", "")
        //									 );
                
        myWebView.loadUrl(url);
    }
}
