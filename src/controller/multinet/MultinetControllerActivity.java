package controller.multinet;

import java.util.ArrayList;
import java.util.List;
import java.net.InetAddress;
import java.net.Socket;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class MultinetControllerActivity extends Activity {
	SharedPreferences settings = null;
	MultiNetAPI router = null;
	ListView lv = null;
	String lastError = "";
	ArrayList<MultinetNetwork> avNetworks = null;
	private Handler myEventHandler = new Handler();
	private static final String TAG = "MultinetControllerActivity";
	
	
	private Runnable myUpdate = new Runnable() {
		   public void run() {
			   update();
		   }
		};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
                
        //get the settings
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        
	    update();
        
    }
    
    @Override
   	protected void onPause() {
       	//remove any pending callbacks no point updating the list in the background!
       	myEventHandler.removeCallbacks(myUpdate);
   		super.onPause();
   	}
    
    @Override
    public void onResume() {
        super.onResume();
	    update();
    }
    
    
	private void update() {	
		//remove any pending callbacks
    	myEventHandler.removeCallbacks(myUpdate);
    	
		if(!checkSettings()) {
			//we have no settings display a message and go to the setting screen
			setContentView(R.layout.missing_settings);
			return;
		}
		 
		if(!connectToAdminNetwork()) {
			setContentView(R.layout.admin_network_not_found);
			Toast.makeText(MultinetControllerActivity.this,"Cannnot connect to the nework. Check your settings", Toast.LENGTH_LONG).show();
			return;
		} 
		
		//we seem to be connected to the admin network so set an update timer 
		myEventHandler.postDelayed(myUpdate, 10000);
		
		
		
		setUpMultiNetAPI();
		
		if(!populateNetworkList()) {
			setContentView(R.layout.admin_coms_error);
			TextView tv = (TextView)findViewById(R.id.message);	
			tv.setText(this.lastError);
			return;
		}
		
		setContentView(R.layout.main);
		this.lv = (ListView)findViewById(R.id.networkList);
	    registerForContextMenu(this.lv);  
	    registerForContextMenu(lv);
	    lv.setTextFilterEnabled(true);
    	lv.setOnItemClickListener(new NetworkListItemClickListener(getApplicationContext()));
    	if(avNetworks != null) {
    	  lv.setAdapter(new ArrayAdapter<MultinetNetwork>(this,R.layout.list_item,avNetworks));
    	}
 
	}
	
	/**
	 *  check the minimum settings have been entered
	 * @return
	 */
	private boolean checkSettings() {
		
		if(settings.getString("ssid", "") == "") {
			return false;
		}
		if(settings.getString("passprase", "") == "") {
			return false;
		}
		if(settings.getString("routerIp", "") == "") {
			return false;
		}
		if(settings.getString("routerPort", "") == "") {
			return false;
		}
		if(settings.getString("username", "") == "") {
			return false;
		}
		if(settings.getString("passwd", "") == "") {
			return false;
		}
		return true;
	}
	
	/**
     *  Are we connected to the Admin Control network ?
     * 
     * @return
     */
    private boolean connectToAdminNetwork() {
    	
    	//TODO android locks a network if it has 3 or more failed join attempts - this needed fixing or unlocking  
    	
    	if (Build.MODEL.equalsIgnoreCase("sdk")) {
    		//we are in the emulator 
    		return true;
    	}
    	
    	//TODO test this in a real device !!
    	WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    	if(wifi == null) {
    		return false;
    	}
    	
    	if(!wifi.isWifiEnabled()) {
    		if(!wifi.setWifiEnabled(true)) {
    			//Can't enable WiFi
    			return false;
    		}
    	}
    	
    	//get current wifi info
    	WifiInfo wifiInfo = wifi.getConnectionInfo();
    	String ssid = wifiInfo.getSSID();
    	if(ssid != null) {
	    	if(ssid.contentEquals(settings.getString("ssid", ""))) {
	    		//we are connected !
	    		return true;
	    	}
    	}
    	
    	//is the admin network configured ?
    	int adminNet = -1;
    	WifiConfiguration tmp_config = null;
    	List<WifiConfiguration> conf = wifi.getConfiguredNetworks();
    	String confSSID = '"' + settings.getString("ssid", "") + '"'; //needs quotes to match !!!
    	for(int i = (conf.size() -1); i >= 0; i--) {
    		tmp_config = conf.get(i);
    		Log.v(TAG, tmp_config.SSID + "==" + confSSID); 
    		if (tmp_config.SSID.contentEquals(confSSID)) {
    			//found the correct configuration
    			adminNet = i;
    			break;
    		}
    	}
    	
    	if(adminNet == -1) {
    		//no config found -> add the configuration
    		Log.v(TAG, "Adding admin network to configured network list");
    		WifiConfiguration admin_config = new WifiConfiguration();
    		admin_config.SSID = '"' + settings.getString("ssid", "") + '"';
    		admin_config.preSharedKey = '"' + settings.getString("passprase", "") + '"';
    		int id = wifi.addNetwork(admin_config);
    		wifi.enableNetwork(id, true);
    		adminNet = id;
    	}
    	
    	boolean connected = false;
    	if(adminNet >= 0) {
    		List<ScanResult> avalable = wifi.getScanResults();
    		ScanResult tmp_result = null;
    		boolean isInRange = false;
    		confSSID = settings.getString("ssid", ""); //Quotes not needed here !!!
    		for(int i = (avalable.size() -1); i >= 0; i--) {
    			tmp_result = avalable.get(i);
    			Log.v(TAG, tmp_result.SSID + "==" + confSSID); 
    			if (tmp_result.SSID.contentEquals(confSSID)) {
        			//found the correct network
    				isInRange = true;
    				connected = wifi.enableNetwork(adminNet, true);
    				break;
        		}
    		}
    		
    		if(!isInRange) {
    			//admin network was not in scan
    			return false;
    		}
    	}
    	
    	
    	if(connected) {
    		//we are connected !
    		
    		//TODO FIX THIS
    		//make sure we have network access
    		try {
    		    Socket socket = new Socket(settings.getString("routerIp", ""), Integer.parseInt((settings.getString("routerPort", "80"))));
    		    if (socket.getLocalAddress() == InetAddress.getByAddress(null)) {
    		    	return false;
    		    }
    		} catch (Exception e) {
    			return false;
    		}
    		return true;
    	}
    	
        return false;
    }
    
    /**
     *  create the MultiNetAPI object from the settings
     * @return
     */
    private boolean setUpMultiNetAPI() {
    	//setup the router API
		router = new MultiNetAPI(
							settings.getString("routerIp", ""),
							settings.getString("routerPort", ""),
							settings.getString("username", ""),
							settings.getString("passwd", "")
						);
		return true;
	}
    
    /**
     * Queries the mutinetAPI for the list of networks
     * then populates the list view.
     * 
     */
    public boolean populateNetworkList() {
    	avNetworks = router.getNetworks();
        if(avNetworks == null) {
        	String error = router.getError();
        	if(error.length() == 0) {
        		error = "Unknone Error";
        	}
        	this.lastError = error;
        	return false;
        } 
        return true;
    }

	/**
     * Network list CONTEXT MENU 
     * 
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	if (v.getId()==R.id.networkList) {
    		menu.setHeaderTitle("Actions");
    		MenuInflater inflater = getMenuInflater();
    		inflater.inflate(R.menu.network_context_menu, menu);
    	}
    }
    
    public boolean onContextItemSelected (MenuItem item) {
    	
    	//TODO Make this better should not use strings in the if!!!
    	    	
    	if (item.getTitle() == "Network Information") {
    		item.getItemId();
    		router.getNetworkInfo();
    		//TODO Launch new intent to show network info
    	}
    	
    	if (item.getTitle() == "Remove Network") {
    		item.getItemId();
    		router.removeNetwork();
    		//TODO give some feedback
    		populateNetworkList();
    	}
    	
    	return super.onContextItemSelected(item);
    }
    
    
    /**
     *  Setting Menu
     * 
     */
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

    /**
     *  Settings menu selection handler
     */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		Intent intent = null;
		switch (id) {
		// We have only one menu option
		case R.id.settingsMenu:
			// Launch Preference activity
			intent = new Intent(MultinetControllerActivity.this, SettingsActivity.class);
			startActivity(intent);
			// Some feedback to the user
			Toast.makeText(MultinetControllerActivity.this,"Here you can enter your user credentials.", Toast.LENGTH_LONG).show();
			break;
		case R.id.addDevice:
			//start the xzing intent
			intent = new Intent("com.google.zxing.client.android.SCAN");
	        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
	        startActivityForResult(intent, 0);
			break;
		case R.id.addDeviceManually:
			intent = new Intent(MultinetControllerActivity.this, AddDeviceManually.class);
			startActivity(intent);
			Toast.makeText(MultinetControllerActivity.this,"Press Back once you have finished", Toast.LENGTH_LONG).show();
			break;
		}
		return true;
	}
	
	/**
	 * onclick handler
	 * @param view
	 */
    public void myClickHandler(View view) {
		switch (view.getId()) {
		case R.id.goTosettings:
			Intent i = new Intent(MultinetControllerActivity.this, SettingsActivity.class);
			startActivityForResult(i,99);
			break;
		case R.id.addNewDevice:
		case R.id.goToqrcode:
			//start the xzing intent
			Intent intent = new Intent("com.google.zxing.client.android.SCAN");
	        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
	        startActivityForResult(intent, 0);
			break;
		case R.id.update:
			update();
			break;
		}
    }
    
    /**
     * Handle returned data from an activity (ZXing) 
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                String contents = intent.getStringExtra("SCAN_RESULT");
                String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
                //Toast.makeText(MultinetControllerActivity.this, contents,Toast.LENGTH_LONG).show();
                handleQRcode(contents, format);
            } else if (resultCode == RESULT_CANCELED) {
            	Toast.makeText(MultinetControllerActivity.this,	"No QRcode scanned :-(",Toast.LENGTH_LONG).show();
            }
        }
        
        //Returned from settings 
        if (requestCode == 99) {
        	//try to find the admin network again
        	update();
        }
    }
    
    void handleQRcode(String contents,String format) {
        
    	String[] temp = contents.split("~");
    	
    	if(temp.length < 1 ) {
    		Toast.makeText(MultinetControllerActivity.this,	"Unsuported QRcode",Toast.LENGTH_LONG).show();
    	}
    	
    	if(temp[0].equalsIgnoreCase("routerSettings")) {
    	    Editor editor = settings.edit();
    	    editor.putString("ssid", temp[1]);
    	    editor.putString("passprase", temp[2]);
    	    editor.putString("routerIp", temp[3]);
    	    editor.putString("routerPort", temp[4]);
    	    editor.commit();
    	    update();
    	    return;
      	}
    	
    	if(temp[0].equalsIgnoreCase("addnetwork")) {
	    	if(router != null) {
		    	if(router.addNetwork(contents,format)) {
		    		populateNetworkList();
		    		Toast.makeText(MultinetControllerActivity.this,	"Network Added",Toast.LENGTH_LONG).show();
		    	} else {
		    		Toast.makeText(MultinetControllerActivity.this,	"Failed to add network",Toast.LENGTH_LONG).show();
		    	}
	    	} else {
	    		Toast.makeText(MultinetControllerActivity.this,	"Can not add device - Not connected to the admin network!",Toast.LENGTH_LONG).show();
	    	}
    	}
    	

    
    }
    
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }
    
    @Override
    public void closeOptionsMenu() {
    }

}
