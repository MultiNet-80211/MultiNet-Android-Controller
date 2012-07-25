package controller.multinet;

public class MultinetNetwork {
	public String ssid;
	public String deviceName;
	public Boolean active;
	
	
	public MultinetNetwork() {
		// TODO Auto-generated constructor stub
	}
	
	public MultinetNetwork(String id, boolean act) {
		this.ssid = id;
		this.active = act;
	}
	
	public MultinetNetwork(String id, String name, boolean act) {
		this.ssid = id;
		this.active = act;
		this.setDeviceName(name);
	}
	
	public String toString() {
		if(deviceName != null) {
			return this.deviceName;
		}
		return this.ssid;
	}
	
	public void setDeviceName(String name) {
		this.deviceName = name;
	}
	
	public void setActive(String text) {
		if(text.equalsIgnoreCase("true")) {
			this.active = true;
		} else {
			this.active = false;
		}
	}

}
