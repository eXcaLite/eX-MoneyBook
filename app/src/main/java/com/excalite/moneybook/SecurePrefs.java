package com.excalite.moneybook;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class SecurePrefs {
    private static final String ALIAS="ex_moneybook_sync_key";
    private static final String PREF="cloud_sync";

    private static SecretKey key() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
        if(!ks.containsAlias(ALIAS)) {
            KeyGenerator kg=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            kg.init(new KeyGenParameterSpec.Builder(ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            kg.generateKey();
        }
        return ((KeyStore.SecretKeyEntry)ks.getEntry(ALIAS,null)).getSecretKey();
    }

    public static void save(Context c,String url,String key,String email,String password,
                            boolean autoSync,int interval,boolean onOpen,boolean afterChange,boolean onClose) throws Exception {
        Cipher cp=Cipher.getInstance("AES/GCM/NoPadding"); cp.init(Cipher.ENCRYPT_MODE,key());
        byte[] enc=cp.doFinal(password.getBytes("UTF-8"));
        String blob=Base64.encodeToString(cp.getIV(),Base64.NO_WRAP)+"."+
                Base64.encodeToString(enc,Base64.NO_WRAP);
        c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit()
                .putString("url",url).putString("key",key).putString("email",email).putString("pass",blob)
                .putBoolean("auto",autoSync).putInt("interval",interval)
                .putBoolean("on_open",onOpen).putBoolean("after_change",afterChange).putBoolean("on_close",onClose)
                .apply();
    }

    public static String[] load(Context c) throws Exception {
        SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        String blob=p.getString("pass","");
        String pass="";
        if(blob.length()>0) {
            String[] parts=blob.split("\\.");
            Cipher cp=Cipher.getInstance("AES/GCM/NoPadding");
            cp.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));
            pass=new String(cp.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),"UTF-8");
        }
        return new String[]{
                p.getString("url",""), p.getString("key",""), p.getString("email",""), pass,
                String.valueOf(p.getBoolean("auto",true)),
                String.valueOf(p.getInt("interval",5)),
                String.valueOf(p.getBoolean("on_open",true)),
                String.valueOf(p.getBoolean("after_change",true)),
                String.valueOf(p.getBoolean("on_close",false))
        };
    }
}
