package controller.multinet;

import android.webkit.HttpAuthHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class myWebViewClient extends WebViewClient {
	String username;
	String password;
	
	public myWebViewClient(String u, String p) {
		username = u;
		password = p;
	}
	
	@Override
	public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
		handler.proceed(username, password);
	}
}
