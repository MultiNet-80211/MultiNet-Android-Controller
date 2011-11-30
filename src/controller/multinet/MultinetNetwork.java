package controller.multinet;

public class MultinetNetwork {
	public String ssid;
	public Boolean active;
	
	
	public MultinetNetwork() {
		// TODO Auto-generated constructor stub
	}
	
	public MultinetNetwork(String id, boolean act) {
		this.ssid = id;
		this.active = act;
	}
	
	public String toString() {
		return this.ssid;
	}

	public void setActive(String text) {
		if(text.equalsIgnoreCase("true")) {
			this.active = true;
		} else {
			this.active = false;
		}
	}

}
