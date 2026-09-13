package com.excalite.moneybook;

import android.database.Cursor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class SupabaseSyncClient {
    private final String baseUrl, anonKey, email, password;
    private String accessToken, userId;

    public SupabaseSyncClient(String url, String key, String mail, String pass) {
        baseUrl=(url==null?"":url.trim().replaceAll("/+$",""));
        anonKey=key==null?"":key.trim(); email=mail==null?"":mail.trim(); password=pass==null?"":pass;
    }

    public void login() throws Exception {
        JSONObject p=new JSONObject();
        p.put("email",email); p.put("password",password);
        String r=request("POST",baseUrl+"/auth/v1/token?grant_type=password",p.toString(),false,null);
        JSONObject o=new JSONObject(r);
        accessToken=o.getString("access_token");
        userId=o.getJSONObject("user").getString("id");
    }

    public void pushDirty(DBHelper db) throws Exception {
        Cursor c=db.dirtyRows();
        JSONArray rows=new JSONArray();
        while(c.moveToNext()) {
            JSONObject o=new JSONObject();
            o.put("id",c.getString(1));
            o.put("user_id",userId);
            o.put("txn_date",c.getString(2));
            o.put("txn_type",c.getString(3));
            o.put("category",c.getString(4)==null?"":c.getString(4));
            o.put("amount",c.getDouble(5));
            o.put("note",c.getString(6)==null?"":c.getString(6));
            o.put("updated_at",c.getString(7));
            o.put("deleted",c.getInt(8)!=0);
            rows.put(o);
        }
        c.close();
        if(rows.length()>0)
            request("POST",baseUrl+"/rest/v1/money_transactions?on_conflict=id",
                    rows.toString(),true,"resolution=merge-duplicates,return=minimal");
    }

    public void pullAll(DBHelper db) throws Exception {
        String r=request("GET",baseUrl+
            "/rest/v1/money_transactions?select=id,txn_date,txn_type,category,amount,note,updated_at,deleted&order=updated_at.asc",
            null,true,null);
        JSONArray a=new JSONArray(r);
        for(int i=0;i<a.length();i++) {
            JSONObject o=a.getJSONObject(i);
            db.mergeCloud(o.getString("id"),o.getString("txn_date"),o.optString("txn_type",""),
                    o.optString("category",""),o.optDouble("amount",0),o.optString("note",""),
                    o.optString("updated_at",DBHelper.nowUtc()),o.optBoolean("deleted",false));
        }
    }

    private String request(String method,String url,String body,boolean authorized,String prefer) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setRequestMethod(method); c.setConnectTimeout(15000); c.setReadTimeout(20000);
        c.setRequestProperty("Accept","application/json");
        c.setRequestProperty("Content-Type","application/json");
        c.setRequestProperty("apikey",anonKey);
        if(authorized)c.setRequestProperty("Authorization","Bearer "+accessToken);
        if(prefer!=null)c.setRequestProperty("Prefer",prefer);

        if(body!=null) {
            c.setDoOutput(true);
            OutputStream os=c.getOutputStream();
            os.write(body.getBytes("UTF-8")); os.close();
        }

        int code=c.getResponseCode();
        InputStream is=(code>=200&&code<300)?c.getInputStream():c.getErrorStream();
        BufferedReader br=new BufferedReader(new InputStreamReader(is,"UTF-8"));
        StringBuilder sb=new StringBuilder(); String line;
        while((line=br.readLine())!=null)sb.append(line);
        br.close(); c.disconnect();
        if(code<200||code>=300)throw new Exception("HTTP "+code+": "+sb.toString());
        return sb.toString();
    }
}
