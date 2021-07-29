package com.example.textrecognition;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG="DatabaseHelper";
    private static final String TABLE_NAME="info_table";
    public static final String DATABASE_NAME="TextRecognition.db";

    public DatabaseHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable="CREATE TABLE info_table(ID INTEGER PRIMARY KEY AUTOINCREMENT,Time TEXT,Image BLOB,Recognised_text TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+ TABLE_NAME);
        onCreate(db);
    }
    public boolean addData(String item,byte[] image)
    {
        SQLiteDatabase db=this.getWritableDatabase();
        ContentValues contentValues=new ContentValues();
        contentValues.put("Image",image);
        contentValues.put("Recognised_text",item);
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String dateTime = simpleDateFormat.format(calendar.getTime()).toString();
        contentValues.put("Time",dateTime);
        Log.d(TAG,"addData: Adding "+item+" to Table_name");
        long result=db.insert(TABLE_NAME,null,contentValues);
        if(result==-1){
            return false;
        }
        else{
            return true;
        }
    }
}
