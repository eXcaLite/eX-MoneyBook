package com.excalite.moneybook;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    public DBHelper(Context ctx) {
        super(ctx, "moneybook.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tx (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date TEXT NOT NULL," +
                "type TEXT NOT NULL," +
                "category TEXT," +
                "amount REAL NOT NULL," +
                "note TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public long add(String date, String type, String category, double amount, String note) {
        ContentValues v = new ContentValues();
        v.put("date", date);
        v.put("type", type);
        v.put("category", category);
        v.put("amount", amount);
        v.put("note", note);
        return getWritableDatabase().insert("tx", null, v);
    }

    public int delete(long id) {
        return getWritableDatabase().delete("tx", "id=?", new String[]{String.valueOf(id)});
    }

    public Cursor listAll() {
        return getReadableDatabase().rawQuery(
                "SELECT id,date,type,category,amount,note FROM tx ORDER BY date DESC,id DESC", null);
    }

    public double sumType(String type) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(amount),0) FROM tx WHERE type=?", new String[]{type});
        double v = 0;
        if (c.moveToFirst()) v = c.getDouble(0);
        c.close();
        return v;
    }
}
