package controller.multinet;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

public class NetworkListItemClickListener implements OnItemClickListener {
	
	Context con;
	
	public NetworkListItemClickListener(Context applicationContext) {
		this.con = applicationContext;
	}

	public void onItemClick(AdapterView<?> listView, View view, int pos, long id) {
		Object o = listView.getAdapter().getItem(pos);
		String tmp = o.toString();
		Toast.makeText(this.con, tmp, 10).show();
		view.showContextMenu();
	}

}