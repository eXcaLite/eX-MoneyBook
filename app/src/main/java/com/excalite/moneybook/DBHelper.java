package com.excalite.moneybook;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class DBHelper extends SQLiteOpenHelper {
    public DBHelper(Context ctx) { super(ctx, "moneybook.db", null, 2); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tx (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "cloud_id TEXT NOT NULL," +
                "date TEXT NOT NULL," +
                "type TEXT NOT NULL," +
                "category TEXT," +
                "amount REAL NOT NULL," +
                "note TEXT," +
                "updated_at TEXT NOT NULL," +
                "deleted INTEGER NOT NULL DEFAULT 0," +
                "dirty INTEGER NOT NULL DEFAULT 1)");
        db.execSQL("CREATE UNIQUE INDEX idx_tx_cloud_id ON tx(cloud_id)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE tx ADD COLUMN cloud_id TEXT");
            db.execSQL("ALTER TABLE tx ADD COLUMN updated_at TEXT");
            db.execSQL("ALTER TABLE tx ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE tx ADD COLUMN dirty INTEGER NOT NULL DEFAULT 1");

            Cursor c = db.rawQuery("SELECT id FROM tx", null);
            while (c.moveToNext()) {
                ContentValues v = new ContentValues();
                v.put("cloud_id", UUID.randomUUID().toString());
                v.put("updated_at", nowUtc());
                v.put("dirty", 1);
                db.update("tx", v, "id=?", new String[]{String.valueOf(c.getLong(0))});
            }
            c.close();
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_tx_cloud_id ON tx(cloud_id)");
        }
    }

    public static String nowUtc() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return f.format(new Date());
    }

    public long add(String date, String type, String category, double amount, String note) {
        ContentValues v = new ContentValues();
        v.put("cloud_id", UUID.randomUUID().toString());
        v.put("date", date); v.put("type", type); v.put("category", category);
        v.put("amount", amount); v.put("note", note); v.put("updated_at", nowUtc());
        v.put("deleted", 0); v.put("dirty", 1);
        return getWritableDatabase().insert("tx", null, v);
    }

    public int softDelete(long id) {
        ContentValues v = new ContentValues();
        v.put("deleted", 1); v.put("dirty", 1); v.put("updated_at", nowUtc());
        return getWritableDatabase().update("tx", v, "id=?", new String[]{String.valueOf(id)});
    }

    public Cursor listAll() {
        return getReadableDatabase().rawQuery(
                "SELECT id,date,type,category,amount,note,cloud_id,updated_at,dirty FROM tx WHERE deleted=0 ORDER BY date DESC,id DESC", null);
    }

    public Cursor dirtyRows() {
        return getReadableDatabase().rawQuery(
                "SELECT id,cloud_id,date,type,category,amount,note,updated_at,deleted FROM tx WHERE dirty=1", null);
    }

    public double sumType(String type) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(amount),0) FROM tx WHERE type=? AND deleted=0", new String[]{type});
        double v=0; if(c.moveToFirst())v=c.getDouble(0); c.close(); return v;
    }

    public void markAllClean() {
        ContentValues v=new ContentValues(); v.put("dirty",0);
        getWritableDatabase().update("tx",v,null,null);
    }

    public void mergeCloud(String cloudId, String date, String type, String category, double amount,
                           String note, String updatedAt, boolean deleted) {
        SQLiteDatabase db=getWritableDatabase();
        Cursor c=db.rawQuery("SELECT id,updated_at,dirty FROM tx WHERE cloud_id=?",new String[]{cloudId});
        if(!c.moveToFirst()) {
            c.close();
            ContentValues v=new ContentValues();
            v.put("cloud_id",cloudId); v.put("date",date); v.put("type",type); v.put("category",category);
            v.put("amount",amount); v.put("note",note); v.put("updated_at",updatedAt);
            v.put("deleted",deleted?1:0); v.put("dirty",0);
            db.insert("tx",null,v); return;
        }

        long localId=c.getLong(0);
        String localUpdated=c.getString(1);
        int dirty=c.getInt(2);
        c.close();

        boolean remoteNewer = localUpdated == null || updatedAt.compareTo(localUpdated) > 0;
        if(dirty==0 || remoteNewer) {
            ContentValues v=new ContentValues();
            v.put("date",date); v.put("type",type); v.put("category",category);
            v.put("amount",amount); v.put("note",note); v.put("updated_at",updatedAt);
            v.put("deleted",deleted?1:0); v.put("dirty",0);
            db.update("tx",v,"id=?",new String[]{String.valueOf(localId)});
        }
    }
}
