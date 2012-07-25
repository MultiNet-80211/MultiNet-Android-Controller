package controller.multinet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.xmlpull.v1.XmlPullParser;

import android.util.Base64;
import android.util.Log;
import android.util.Xml;

public class MultiNetAPI {

	private static final String TAG = "MultinetControllerActivity";
	String routerIP;
	String routerPort;
	String routerUsername;
	String routerPassword;
	String error;	
	
	String protocall = "http";
	String delimiter = "~";
	
	ArrayList<MultinetNetwork> networks;
	
	public MultiNetAPI(String ip, String port, String user, String passwd) {

		routerIP = ip;
		routerPort = port;
		routerUsername = user;
		routerPassword = passwd;
		
	}

	public  ArrayList<MultinetNetwork> getNetworks() {
		
		String url = this.protocall + "://" + 
				 this.routerIP + ":" + 
				 this.routerPort + "/" + 
				 "list/";
		
		Log.v(TAG, "getNetworks: " + url);
		Boolean res = getRequest(url);
		
		if(res == true) {
			return networks;
		} else {
			return null;
		}
		
	}

	
	public void getNetworkInfo() {
		// TODO Auto-generated method stub
		
	}
	
	public Boolean removeNetwork(String ssid) {
		String url = this.protocall + "://" + 
				 this.routerIP + ":" + 
				 this.routerPort + "/" + 
				 "remove/" +
				 ssid + "/";
	
		Log.v(TAG, "removeNetwork: " + url);
		return getRequest(url);
	}

	public Boolean addNetwork(String contents, String format) {

		String[] action;
		
		action = contents.split(this.delimiter);
		String url = "";
		if(action.length == 3) {
			//no device name in the qrcode use the ssid
			url = this.protocall + "://" + 
					 this.routerIP + ":" + 
					 this.routerPort + "/" + 
					 "create/" +
					 action[1] + "/" +
					 action[2] + "/" +
					 action[1] + "/";
		} else {
			url = this.protocall + "://" + 
						 this.routerIP + ":" + 
						 this.routerPort + "/" + 
						 "create/" +
						 action[1] + "/" +
						 action[2] + "/" +
						 action[3] + "/";
		}
		
		Log.v(TAG, "addNetwork: " + url);
		return getRequest(url);
	}
	
	public Boolean getRequest(String url) {
		
		String xml = "";
		
		String currentResult = "flase";
        MultinetNetwork currentNetwork = null;
        
		try {
			//TODO handle missing service
			
			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters, 5000);
			HttpConnectionParams.setSoTimeout(httpParameters, 10000);

			HttpClient client = new DefaultHttpClient(httpParameters);
			HttpGet request = new HttpGet(url);
			
			//basic HTTP auth
			String auth = this.routerUsername + ":" + this.routerPassword;
			auth = Base64.encodeToString(auth.getBytes(),0);
			request.setHeader("Authorization", "Basic " +  auth);

			HttpResponse response = client.execute(request);
			
			//check for Unauthorized message
			if(response.getStatusLine().getReasonPhrase().equalsIgnoreCase("Unauthorized") ) {
				this.error = "Incorrect username and/or password please check your settings";
				return false;
			}
			
			// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));
			String line = "";
			while ((line = rd.readLine()) != null) {
				xml += line + "\n";
			}
			
			XmlPullParser p = Xml.newPullParser();
			p.setInput( new StringReader(xml) );
            int eventType = p.getEventType();
            boolean done = false;
            while (eventType != XmlPullParser.END_DOCUMENT && !done) {
                String name = null;
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        networks = new ArrayList<MultinetNetwork>();
                        break;
                    case XmlPullParser.START_TAG:
                        name = p.getName();
                        
                        if (name.equalsIgnoreCase("network")){
                            currentNetwork = new MultinetNetwork();
                        } else if (currentNetwork != null){
                            if (name.equalsIgnoreCase("ssid")){
                            	currentNetwork.ssid = p.nextText();
                            }
                            if (name.equalsIgnoreCase("devicename")){
                            	currentNetwork.setDeviceName(p.nextText());
                            } 
                            if (name.equalsIgnoreCase("active")){
                            	currentNetwork.setActive(p.nextText());
                            } 
                        }
                        
                        if (name.equalsIgnoreCase("result")){
                        	currentNetwork = new MultinetNetwork();
                        } else if (currentNetwork != null) {
                        	if (name.equalsIgnoreCase("success")){
                        		currentResult = p.nextText();
                            } 
                        }
                        
                        break;
                    case XmlPullParser.END_TAG:
                        name = p.getName();
                        if (name.equalsIgnoreCase("network") && currentNetwork != null){
                        	networks.add(currentNetwork);
                        } 
                        
                        if (name.equalsIgnoreCase("networkList") && currentNetwork != null){
                        	done = true;
                        } 
                        
                        if (name.equalsIgnoreCase("result") && currentNetwork != null){
                        	done = true;
                        }
                        break;
                }
                eventType = p.next();
            }
            
            
			
		} catch (Exception e) {
			//TODO handle error!!
			this.error = e.getMessage();
			return false;
		}
		
		if(networks != null) {
			return true;
		} else if (currentResult.equalsIgnoreCase("true")) {
			return true;
		} else {	
			return false;
		}
	}
	
	public String getError() {
	  if(this.error != null) {
		  return this.error;
	  }
	  return "";
	}
}



