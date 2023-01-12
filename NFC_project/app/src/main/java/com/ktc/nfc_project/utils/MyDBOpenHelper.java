package com.ktc.nfc_project.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * @author liangcw
 * @description:
 * @date 2023/1/5 10:40
 */
public class MyDBOpenHelper extends SQLiteOpenHelper {

    public static final String CREATE_TABLE="create table nfcdevicerecord("+
            "recordid integer primary key autoincrement,"+
            "deviceid varchar(20),"+
            "appname varchar(20),"+
            "packagename varchar(20),"+
            "alias varchar(20))";


    public MyDBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, "my.db", null, 1);
    }

    @Override

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}