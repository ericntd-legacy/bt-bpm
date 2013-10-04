package sg.edu.nus.omronhealth.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class dbHelper extends SQLiteOpenHelper {

	private static final String TAG = "dbHelper";
	private static final int dbVersion = 2;
	


	public dbHelper(Context context){
		super(context, "dbName", null, dbVersion); 
	}
	
	// simple table
	@Override
	public void onCreate(SQLiteDatabase db){
		// sql
		Log.d(TAG, "onCreate db");
		//db.execSQL("CREATE TABLE tbl_TEST (id INTEGER PRIMARY KEY AUTOINCREMENT, str TEXT NOT NULL)");
		
		db.execSQL(BP_DAO.tblBpCreate);
							
		
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer){
		Log.d(TAG, "onUpgrade");
		// DROP old table
		db.execSQL("DROP TABLE IF EXISTS tbl_TEST");
		db.execSQL("DROP TABLE IF EXISTS " + BP_DAO.TBL_BP);
		//receate tbl
		onCreate(db);
	}
}
