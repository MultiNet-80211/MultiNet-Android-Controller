package controller.multinet;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class settingsActivity extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    addPreferencesFromResource(R.xml.settings);
	}

}
