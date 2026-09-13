package com.excalite.moneybook;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public class CategoryPrefs {
    private static final String PREF="inpense_categories";
    private static final String KEY_INCOME="income_custom";
    private static final String KEY_EXPENSE="expense_custom";

    static ArrayList<String> load(Context c,String key) {
        SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        Set<String> s=p.getStringSet(key,new LinkedHashSet<String>());
        return new ArrayList<String>(s);
    }

    static void save(Context c,String key,ArrayList<String> list) {
        LinkedHashSet<String> s=new LinkedHashSet<String>(list);
        c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putStringSet(key,s).apply();
    }

    public static ArrayList<String> loadIncome(Context c) { return load(c,KEY_INCOME); }
    public static ArrayList<String> loadExpense(Context c) { return load(c,KEY_EXPENSE); }
    public static void saveIncome(Context c,ArrayList<String> list) { save(c,KEY_INCOME,list); }
    public static void saveExpense(Context c,ArrayList<String> list) { save(c,KEY_EXPENSE,list); }
}
