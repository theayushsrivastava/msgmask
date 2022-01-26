package com.c2mtechnology.msgmask.Classes;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import androidx.annotation.Nullable;

import com.c2mtechnology.msgmask.Models.AddressModel;
import com.c2mtechnology.msgmask.Models.Sms;
import com.c2mtechnology.msgmask.Models.SmsStats;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class DataBaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "database";
    private static DataBaseHelper sInstance;
    private static final int REPORT_COUNT_THRESHOLD = 10;

    public static synchronized DataBaseHelper getInstance(Context context) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new DataBaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    public DataBaseHelper(@Nullable Context context) {
        super(context,DBschema.DB,null,1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DBschema.CREATE_TABLE_MESSAGES);
        db.execSQL(DBschema.CREATE_TABLE_ADDRESSES);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        if(getDatabaseName().equals(DBschema.DB))
        {
            db.execSQL("DROP TABLE IF EXISTS " + DBschema.TABLE_MESSAGES);
            db.execSQL("DROP TABLE IF EXISTS " + DBschema.TABLE_ADDRESS_STATS);
            // create new tables
            onCreate(db);
        }
    }


    public void addMessageStats(JSONObject jsonObject,long timestamp)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try{
            ContentValues values = new ContentValues();
            values.put(DBschema.KEY_ADDRESS,jsonObject.getString("address"));
            values.put(DBschema.KEY_TIMESTAMP,timestamp);
            if(jsonObject.getBoolean("isSpam"))
            {
                values.put(DBschema.KEY_IS_SPAM,1);
            }else{
                values.put(DBschema.KEY_IS_SPAM,0);
            }

            JSONObject url_report = jsonObject.getJSONObject("url_report");
            Iterator<String> keys = url_report.keys();

            int malicious_link = 0;
            while(keys.hasNext()) {
                String key = keys.next();
                if (url_report.get(key) instanceof String) {
                    // do something with jsonObject here
                    String urlSafe = url_report.getString(key);
//                    Log.i("url safe " , urlSafe);
                    if(urlSafe.equals("bad"))
                    {
                        malicious_link += 1;
                    }
                }
            }
            values.put(DBschema.KEY_MALICIOUS_LINK_FOUND,malicious_link);
            values.put(DBschema.KEY_MALICIOUS_LINKS,jsonObject.getString("url_report"));
            values.put(DBschema.KEY_IS_OVERRIDDEN,0);
//            values.put(DBschema.KEY_IS_SPAM,);
            db.insert(DBschema.TABLE_MESSAGES,null,values);

            String address = jsonObject.getString("address");
            String query = String.format("SELECT address FROM %s WHERE %s = '%s'", DBschema.TABLE_ADDRESS_STATS,DBschema.KEY_ADDRESS,address);
            Cursor cursor = db.rawQuery(query, null);

            ContentValues values1 = new ContentValues();
            JSONObject addressStats = jsonObject.getJSONObject("address_stats");
            int spam_count      = addressStats.getInt("spam_count");
            int ham_count       = addressStats.getInt("ham_count");
            int report_count    = addressStats.getInt("report_count");
            values1.put(DBschema.KEY_SPAM_MSG_COUNTS,spam_count);
            values1.put(DBschema.KEY_HAM_MSG_COUNTS,ham_count);
            values1.put(DBschema.KEY_REPORTED_COUNTS,report_count);
            //calculate spammer or not on the bases of report count and spam  and ham message count
            //
            values1.put(DBschema.KEY_IS_SPAMMER,is_Spammer(spam_count,ham_count,report_count));
            if(cursor.moveToFirst())
            {
                //update
                db.update(DBschema.TABLE_ADDRESS_STATS,values1,DBschema.KEY_ADDRESS + " = ?",new String[]{address});
            }else{
                //insert
                values1.put(DBschema.KEY_IS_REPORTED,0);
                values1.put(DBschema.KEY_ADDRESS,address);
                values1.put(DBschema.KEY_IS_OVERRIDDEN,0);
                db.insert(DBschema.TABLE_ADDRESS_STATS,null,values1);
            }
            db.setTransactionSuccessful();
        }catch (Exception e){
            Log.i("error", e.getLocalizedMessage());
        }finally {
            db.endTransaction();
        }
    }

    public void add_update_address_stats(JSONObject jsonObject,Integer is_reported,Integer is_overriden)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        try{
            ContentValues values1 = new ContentValues();
            int spam_count      = jsonObject.getInt("spam_count");
            int ham_count       = jsonObject.getInt("ham_count");
            int report_count    = jsonObject.getInt("report_count");
            values1.put(DBschema.KEY_SPAM_MSG_COUNTS,spam_count);
            values1.put(DBschema.KEY_HAM_MSG_COUNTS,ham_count);
            values1.put(DBschema.KEY_REPORTED_COUNTS,report_count);
            values1.put(DBschema.KEY_IS_SPAMMER,is_Spammer(spam_count,ham_count,report_count));
            if(is_reported != null)
            {
                values1.put(DBschema.KEY_IS_REPORTED,is_reported);
            }

            if(is_overriden != null)
            {
                values1.put(DBschema.KEY_IS_OVERRIDDEN,is_overriden);
            }


            //calculate spammer or not on the bases of report count and spam  and ham message count

            String address = jsonObject.getString("address");
            String query = String.format("SELECT address FROM %s WHERE %s = '%s'", DBschema.TABLE_ADDRESS_STATS,DBschema.KEY_ADDRESS,address);
            Cursor cursor = db.rawQuery(query, null);

            if(cursor.moveToFirst())
            {
                //update
                db.update(DBschema.TABLE_ADDRESS_STATS,values1,DBschema.KEY_ADDRESS + " = ?",new String[]{address});
            }else{
                //insert
                if(is_reported == null)
                {
                    values1.put(DBschema.KEY_IS_REPORTED,0);
                }
                if(is_overriden == null){
                    values1.put(DBschema.KEY_IS_OVERRIDDEN,0);
                }
                values1.put(DBschema.KEY_ADDRESS,address);
                db.insert(DBschema.TABLE_ADDRESS_STATS,null,values1);
            }

        }catch (Exception e)
        {
            e.printStackTrace();
        }

        db.close();
    }
    //0 => not spammer
    //1 => spammer
    public static int is_Spammer(int spamCounts,int hamCounts,int reportCounts)
    {
        if(reportCounts > REPORT_COUNT_THRESHOLD)
        {
            return 1;
        }
        if(spamCounts > hamCounts){
            return 1;
        }
        return 0;
    }

    public int[] getMessageStats(String address, long time) {
        int[] arr = new int[]{-1,-1};
        SQLiteDatabase db = this.getReadableDatabase();
        String timestamp = String.valueOf(time);
        @SuppressLint("Recycle") Cursor cursor = db.query(DBschema.TABLE_MESSAGES,new String[]{DBschema.KEY_IS_SPAM,DBschema.KEY_IS_OVERRIDDEN},DBschema.KEY_ADDRESS + " = ? AND " + DBschema.KEY_TIMESTAMP + " = ?",new String[]{address,timestamp},null,null,null);
        try{

            if(cursor.moveToFirst())
            {
                arr[0] = cursor.getInt(cursor.getColumnIndexOrThrow(DBschema.KEY_IS_SPAM));
                arr[1] = cursor.getInt(cursor.getColumnIndexOrThrow(DBschema.KEY_IS_OVERRIDDEN));
            }
        }catch (Exception e){

        }

        db.close();
        return arr;
    }

    //function to get all spam address
    public ArrayList<Sms> getAllSpamAddresses()
    {
        ArrayList<Sms> smsArrayList = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = String.format("SELECT address FROM %s WHERE %s = '%s' OR %s = '%s'", DBschema.TABLE_ADDRESS_STATS,DBschema.KEY_IS_SPAMMER,1,DBschema.KEY_IS_REPORTED,1);
        Cursor cursor = db.rawQuery(query, null);
        try {
            if (cursor.moveToFirst()) {
                do {
                    Sms sms = new Sms();
                    sms.setAddress(cursor.getString(cursor.getColumnIndexOrThrow(DBschema.KEY_ADDRESS)));
                    smsArrayList.add(sms);
                } while(cursor.moveToNext());
            }
        } catch (Exception e) {

        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
                db.close();
            }
        }
        return smsArrayList;
    }

    //function to get sms stats object
    public SmsStats getMessage(String address, long time)
    {
        SQLiteDatabase db = this.getReadableDatabase();
        String timestamp = String.valueOf(time);
        @SuppressLint("Recycle") Cursor cursor = db.query(DBschema.TABLE_MESSAGES,new String[]{},DBschema.KEY_ADDRESS + " = ? AND " + DBschema.KEY_TIMESTAMP + " = ?",new String[]{address,timestamp},null,null,null);
        try{

            if(cursor.moveToFirst())
            {
                SmsStats smsStats = new SmsStats();
                smsStats.setAddress(address);
                smsStats.setTimestamp(time);
                smsStats.setIsSpam(cursor.getInt(cursor.getColumnIndexOrThrow(DBschema.KEY_IS_SPAM)));
                smsStats.setMaliciousLinksCount(cursor.getInt(cursor.getColumnIndexOrThrow(DBschema.KEY_MALICIOUS_LINK_FOUND)));
                smsStats.setMalicious_links(cursor.getString(cursor.getColumnIndexOrThrow(DBschema.KEY_MALICIOUS_LINKS)));
                return smsStats;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public AddressModel getAddressStats(String address)
    {
        String query = String.format("SELECT * FROM %s WHERE %s = '%s'", DBschema.TABLE_ADDRESS_STATS,DBschema.KEY_ADDRESS,address);
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        try {
            if (cursor.moveToFirst()) {
                AddressModel addressModel = new AddressModel();
                addressModel.setAddress(address);
                addressModel.setHamCount(cursor.getInt(cursor.getColumnIndexOrThrow(DBschema.KEY_HAM_MSG_COUNTS)));
                addressModel.setSpamCount(cursor.getInt(cursor.getColumnIndexOrThrow(DBschema.KEY_SPAM_MSG_COUNTS)));
                addressModel.setIs_spammer(cursor.getInt(cursor.getColumnIndexOrThrow(DBschema.KEY_IS_SPAMMER)));
                addressModel.setIsOverridden(cursor.getInt(cursor.getColumnIndexOrThrow(DBschema.KEY_IS_OVERRIDDEN)));
                addressModel.setIsReported(cursor.getInt(cursor.getColumnIndexOrThrow(DBschema.KEY_IS_REPORTED)));
                addressModel.setReportCount(cursor.getInt(cursor.getColumnIndexOrThrow(DBschema.KEY_REPORTED_COUNTS)));
                return addressModel;
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to get posts from database");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return null;
    }


    //get percentage secure message
    //int[] => [totalSms,totalHam]
    public int[] getAllSmsStats()
    {
        int[] arr = new int[]{0,0};
        SQLiteDatabase db = this.getReadableDatabase();
        String query1 = "SELECT COUNT(*) FROM " + DBschema.TABLE_MESSAGES ;
        SQLiteStatement statement = db.compileStatement(query1);
        long count = statement.simpleQueryForLong();

        arr[0] = (int)count;
        String query2 = String.format("SELECT COUNT(*) FROM %s WHERE %s = '%s'",DBschema.TABLE_MESSAGES,DBschema.KEY_IS_SPAM,0);
        statement = db.compileStatement(query2);
        count = statement.simpleQueryForLong();
        arr[1] = (int) count;
        return arr;
    }
}
