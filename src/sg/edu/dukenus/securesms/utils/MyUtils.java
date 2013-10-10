package sg.edu.dukenus.securesms.utils;

import sg.edu.dukenus.securesms.crypto.MyKeyUtils;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class MyUtils {
	// debugging
	private static final String TAG = "MyUtils";
	
	public static void alert(String message, Context context) {
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);

		dialogBuilder
				.setTitle(message)
				.setMessage(message)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// continue with delete
					}
				})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								// do nothing
							}
						});

		AlertDialog alertDialog = dialogBuilder.create();
		alertDialog.show();
	}
	
	public static void missingKeyAlert(String message, String contactNum, Context context) {
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
		
		final Context mContext = context;
		final String num = contactNum;
		
		dialogBuilder
				.setTitle(message)
				.setMessage(message)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// TODO trigger requesting of public key from server
						// triggering the service to do it
						// is it a better idea to use say asynctask?
						//MyUtils tmp = new MyUtils();
						Log.w(TAG, "onclick ok");
						MyUtils.RequestKeyTask task = new MyUtils.RequestKeyTask(num, mContext);
						task.execute();
						
					}
				})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								// do nothing
							}
						});

		AlertDialog alertDialog = dialogBuilder.create();
		alertDialog.show();
	}
	
	public static class RequestKeyTask extends AsyncTask<Void, Integer, Long> {
		private String contactNum;
		private Context mContext;
		
		public RequestKeyTask(String phoneNum, Context context) {
			this.contactNum = phoneNum;
			this.mContext = context;
		}
		@Override
		protected Long doInBackground(Void... arg0) {
			Log.w(TAG, "requesting key in background");
			MyKeyUtils.requestForKey(contactNum, this.mContext);
			Long result = Long.valueOf(100);
			return result;
		}
		
		protected void onProgressUpdate(Integer... progress) {
			
		}
		
		@Override
		protected void onPostExecute(Long result) {
			Log.w(TAG, "request key task is done "+result);
			Toast.makeText(this.mContext, "Key request sent", Toast.LENGTH_SHORT).show();
			super.onPostExecute(result);
		}
		
	}

}
