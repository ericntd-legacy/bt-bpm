package sg.edu.dukenus.securesms.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class MyUtils {
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

}
