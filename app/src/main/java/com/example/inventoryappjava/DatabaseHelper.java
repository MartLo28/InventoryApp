package com.example.inventoryappjava;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "inventory.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE Users (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "Username TEXT," +
                "Password TEXT)";
        db.execSQL(CREATE_USERS_TABLE);

        String CREATE_INVENTORY_TABLE = "CREATE TABLE Inventory (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "Name TEXT," +
                "Quantity INTEGER," +
                "UserId INTEGER," +
                "FOREIGN KEY(UserId) REFERENCES Users(ID))";
        db.execSQL(CREATE_INVENTORY_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS Users");
        db.execSQL("DROP TABLE IF EXISTS Inventory");
        onCreate(db);
    }
}
