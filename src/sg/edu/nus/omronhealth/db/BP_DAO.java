package sg.edu.nus.omronhealth.db;

import sg.edu.nus.omronhealth.spp.BPMeasurementData;
import java.util.GregorianCalendar;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class BP_DAO {
	private static final String TAG = "BP_DAO";
	private SQLiteDatabase db;
	private dbHelper dbHelper;
	
	static final String TBL_BP = "tbl_bp";
	static final String BP_id = "id";
	static final String BP_timestamp = "timestamp";
	static final String BP_sys = "sys";
	static final String BP_dia = "dia";
	static final String BP_pulse = "pulse";
	static final String BP_bodyMovementFlag = "bodyMovementFlag";
	static final String BP_irregPulseFlag = "irregPulseFlag";
	
	public final static String tblBpCreate = "CREATE TABLE " + TBL_BP + "(" +
			BP_id + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
			BP_sys + " INTEGER NOT NULL," + 
			BP_dia + " INTEGER NOT NULL," + 
			BP_pulse + " INTEGER NOT NULL," + 
			BP_bodyMovementFlag + " INTEGER NOT NULL," + 
			BP_irregPulseFlag + " INTEGER NOT NULL," + 
			BP_timestamp + "  NOT NULL " +
			")";
	
	public BP_DAO(Context context){
		dbHelper = new dbHelper(context);
		db = dbHelper.getWritableDatabase();
	}
	
	public void close(){
		db.close();
	}
	
	public void createBPRecord(String measurementData){
		ContentValues values = new ContentValues();
		values.put("str", measurementData);
		
		//insert into db
		long insertId = db.insert(TBL_BP, null, values);
		Log.d(TAG, "inserted with id: " + insertId);
	}
	
	public void createBPRecord(BPMeasurementData measurementData){
		
		//BPMeasurementData measurementData = new BPMeasurementData(_measurementData);
		
		int YY,MM,DD,hh,mm,ss;
		
		if(measurementData.getYY() > 0){
			YY = measurementData.getYY();
			MM = measurementData.getMM();
			DD = measurementData.getDD();
			hh = measurementData.getHh();
			mm = measurementData.getMm();
			ss = measurementData.getSs();
			
			
		} else {
			YY = 1;
			MM = 1;
			DD = 1;
			hh = 0;
			mm = 0;
			ss = 0;
		}
		
		GregorianCalendar timestamp = new GregorianCalendar(YY, MM, DD, hh, mm, ss);
		
		ContentValues values = new ContentValues();
		values.put(BP_sys, measurementData.getSys());
		values.put(BP_dia, measurementData.getDia());
		values.put(BP_pulse, measurementData.getPulse());
		values.put(BP_bodyMovementFlag, measurementData.getBodyMovementFlag());
		values.put(BP_irregPulseFlag, measurementData.getIrregPulseFlag());
		System.out.println("measurement time: " + timestamp);
		System.out.println("measurement time mills: " + timestamp.getTimeInMillis());
		values.put(BP_timestamp, timestamp.getTimeInMillis());
		

		//insert into db
		long insertId = db.insert(TBL_BP, null, values);
		Log.d(TAG, "inserted with id: " + insertId);
		
	}


	

}
