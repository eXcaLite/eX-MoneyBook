package com.excalite.moneybook;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.database.Cursor;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    DBHelper db;
    LinearLayout listBox;
    TextView incomeText, expenseText, balanceText, dateText, syncStatus;
    Spinner typeSpinner, categorySpinner;
    EditText amountEdit, noteEdit, searchEdit;
    Spinner searchTypeSpinner, searchPeriodSpinner;
    TextView searchCount;
    String selectedDate;
    DecimalFormat money = new DecimalFormat("#,##0.00");
    Handler autoHandler = new Handler(Looper.getMainLooper());
    Runnable autoRunnable;
    volatile boolean syncing = false;
    long lastSyncMs = 0;

    final int BG = Color.rgb(16,19,23);
    final int PANEL = Color.rgb(25,30,36);
    final int PANEL2 = Color.rgb(34,40,50);
    final int TEXT = Color.rgb(242,244,247);
    final int MUTED = Color.rgb(167,176,188);
    final int ACCENT = Color.rgb(77,163,255);
    final int INCOME = Color.rgb(61,220,151);
    final int EXPENSE = Color.rgb(255,107,107);
    final int GOLD = Color.rgb(246,200,95);

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        db = new DBHelper(this);
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        buildUi();
        refreshAll();
        configureAutoSync();
        try {
            String[] c=SecurePrefs.load(this);
            if(Boolean.parseBoolean(c[6])) syncNow(false);
        } catch(Exception ignored) {}
    }

    TextView tv(String text, float sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(sp);
        v.setTextColor(color);
        if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD);
        v.setPadding(dp(4), dp(4), dp(4), dp(4));
        return v;
    }

    int dp(int x) { return (int)(x * getResources().getDisplayMetrics().density + 0.5f); }

    LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    TextView card(String title, int color) {
        TextView t = tv(title, 18, color, true);
        t.setGravity(Gravity.CENTER);
        t.setBackgroundColor(PANEL);
        t.setPadding(dp(8),dp(13),dp(8),dp(13));
        return t;
    }

    Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(TEXT);
        b.setBackgroundColor(PANEL2);
        return b;
    }

    void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(12), dp(14), dp(22));
        scroll.addView(root);

        // Brand
        LinearLayout brand = row();
        TextView logo = tv("eX", 30, GOLD, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackgroundColor(PANEL2);
        LinearLayout.LayoutParams lpLogo = new LinearLayout.LayoutParams(dp(64), dp(64));
        brand.addView(logo, lpLogo);

        LinearLayout brandText = new LinearLayout(this);
        brandText.setOrientation(LinearLayout.VERTICAL);
        brandText.setPadding(dp(12), 0, 0, 0);
        brandText.addView(tv("eX Money Book", 24, TEXT, true));
        brandText.addView(tv("รายรับ • รายจ่าย • คงเหลือ", 13, MUTED, false));
        brand.addView(brandText, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(brand);

        LinearLayout cloudRow = row();
        Button cloudSettings = button("Cloud Settings");
        Button syncNow = button("Sync Now");
        syncStatus = tv("Cloud: ยังไม่ Sync", 12, MUTED, false);
        cloudSettings.setOnClickListener(v -> showCloudSettings());
        syncNow.setOnClickListener(v -> syncNow());
        cloudRow.addView(cloudSettings, new LinearLayout.LayoutParams(0, dp(48), 1));
        spaceH(cloudRow, 6);
        cloudRow.addView(syncNow, new LinearLayout.LayoutParams(0, dp(48), 1));
        root.addView(cloudRow);
        root.addView(syncStatus);
        space(root, 14);

        // Summary
        LinearLayout sums = row();
        incomeText = card("รายรับ\n0.00", INCOME);
        expenseText = card("รายจ่าย\n0.00", EXPENSE);
        balanceText = card("คงเหลือ\n0.00", GOLD);
        sums.addView(incomeText, new LinearLayout.LayoutParams(0, -2, 1));
        spaceH(sums, 6);
        sums.addView(expenseText, new LinearLayout.LayoutParams(0, -2, 1));
        spaceH(sums, 6);
        sums.addView(balanceText, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(sums);
        space(root, 14);

        root.addView(tv("เพิ่มรายการ", 18, TEXT, true));
        space(root, 6);

        // Date
        dateText = tv("", 16, TEXT, false);
        dateText.setBackgroundColor(PANEL);
        dateText.setPadding(dp(12),dp(12),dp(12),dp(12));
        dateText.setOnClickListener(v -> chooseDate());
        root.addView(dateText, new LinearLayout.LayoutParams(-1, -2));
        space(root, 8);

        typeSpinner = new Spinner(this);
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"รายจ่าย","รายรับ"});
        typeSpinner.setAdapter(typeAdapter);
        typeSpinner.setBackgroundColor(PANEL);
        typeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { loadCategories(); }
            public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
        root.addView(typeSpinner, new LinearLayout.LayoutParams(-1, dp(52)));
        space(root, 8);

        categorySpinner = new Spinner(this);
        categorySpinner.setBackgroundColor(PANEL);
        root.addView(categorySpinner, new LinearLayout.LayoutParams(-1, dp(52)));
        space(root, 8);

        amountEdit = new EditText(this);
        amountEdit.setHint("จำนวนเงิน");
        amountEdit.setHintTextColor(MUTED);
        amountEdit.setTextColor(TEXT);
        amountEdit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amountEdit.setBackgroundColor(PANEL);
        amountEdit.setPadding(dp(12),0,dp(12),0);
        root.addView(amountEdit, new LinearLayout.LayoutParams(-1, dp(52)));
        space(root, 8);

        noteEdit = new EditText(this);
        noteEdit.setHint("หมายเหตุ");
        noteEdit.setHintTextColor(MUTED);
        noteEdit.setTextColor(TEXT);
        noteEdit.setBackgroundColor(PANEL);
        noteEdit.setPadding(dp(12),0,dp(12),0);
        root.addView(noteEdit, new LinearLayout.LayoutParams(-1, dp(52)));
        space(root, 8);

        Button add = button("+ บันทึกรายการ");
        add.setOnClickListener(v -> addItem());
        root.addView(add, new LinearLayout.LayoutParams(-1, dp(52)));
        space(root, 18);

        root.addView(tv("ค้นหารายการ", 18, TEXT, true));
        space(root, 6);

        searchEdit = new EditText(this);
        searchEdit.setHint("ค้นหา หมายเหตุ / หมวดหมู่ / ประเภท / จำนวนเงิน");
        searchEdit.setHintTextColor(MUTED);
        searchEdit.setTextColor(TEXT);
        searchEdit.setSingleLine(true);
        searchEdit.setBackgroundColor(PANEL);
        searchEdit.setPadding(dp(12),0,dp(12),0);
        root.addView(searchEdit, new LinearLayout.LayoutParams(-1,dp(52)));
        space(root,6);

        LinearLayout searchFilters=row();
        searchTypeSpinner=new Spinner(this);
        searchTypeSpinner.setBackgroundColor(PANEL);
        ArrayAdapter<String> st=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
                new String[]{"ทั้งหมด","รายรับ","รายจ่าย"});
        searchTypeSpinner.setAdapter(st);

        searchPeriodSpinner=new Spinner(this);
        searchPeriodSpinner.setBackgroundColor(PANEL);
        ArrayAdapter<String> sp=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
                new String[]{"ทั้งหมด","วันนี้","เดือนนี้","ปีนี้"});
        searchPeriodSpinner.setAdapter(sp);

        searchFilters.addView(searchTypeSpinner,new LinearLayout.LayoutParams(0,dp(52),1));
        spaceH(searchFilters,6);
        searchFilters.addView(searchPeriodSpinner,new LinearLayout.LayoutParams(0,dp(52),1));
        root.addView(searchFilters);
        space(root,4);

        searchCount=tv("พบ 0 รายการ",12,MUTED,false);
        root.addView(searchCount);
        space(root,8);

        searchEdit.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) { refreshAll(); }
            public void afterTextChanged(Editable e) {}
        });
        searchTypeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { refreshAll(); }
            public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
        searchPeriodSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { refreshAll(); }
            public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });

        root.addView(tv("ผลการค้นหา / รายการล่าสุด", 18, TEXT, true));
        space(root, 6);
        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(listBox);

        space(root, 18);
        TextView sign = tv("created by :: eXcaLite ::", 12, ACCENT, false);
        sign.setGravity(Gravity.CENTER);
        root.addView(sign);

        setContentView(scroll);
        loadCategories();
    }

    void space(LinearLayout l, int h) {
        Space s = new Space(this); l.addView(s, new LinearLayout.LayoutParams(1, dp(h)));
    }
    void spaceH(LinearLayout l, int w) {
        Space s = new Space(this); l.addView(s, new LinearLayout.LayoutParams(dp(w), 1));
    }

    void chooseDate() {
        Calendar c = Calendar.getInstance();
        try {
            String[] p = selectedDate.split("-");
            c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1])-1, Integer.parseInt(p[2]));
        } catch(Exception ignored) {}
        DatePickerDialog d = new DatePickerDialog(this, (view,y,m,day) -> {
            selectedDate = String.format(Locale.US, "%04d-%02d-%02d", y,m+1,day);
            updateDateText();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        d.show();
    }

    void updateDateText() {
        dateText.setText("วันที่: " + selectedDate);
    }

    void loadCategories() {
        String type = String.valueOf(typeSpinner.getSelectedItem());
        String[] cats;
        if ("รายรับ".equals(type))
            cats = new String[]{"เงินเดือน","กำไรเทรด","โบนัส","ขายของ","ดอกเบี้ย","อื่นๆ"};
        else
            cats = new String[]{"อาหาร","เดินทาง","ค่าน้ำ/ไฟ","โทรศัพท์/อินเทอร์เน็ต","บ้าน","ช้อปปิ้ง","สุขภาพ","เทรด/ค่าธรรมเนียม","อื่นๆ"};
        ArrayAdapter<String> a = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, cats);
        categorySpinner.setAdapter(a);
    }

    void addItem() {
        String a = amountEdit.getText().toString().trim();
        double amount;
        try { amount = Double.parseDouble(a); } catch(Exception e) { amount = 0; }
        if (amount <= 0) {
            Toast.makeText(this, "กรุณาใส่จำนวนเงินมากกว่า 0", Toast.LENGTH_SHORT).show();
            return;
        }
        String type = String.valueOf(typeSpinner.getSelectedItem());
        String cat = String.valueOf(categorySpinner.getSelectedItem());
        db.add(selectedDate, type, cat, amount, noteEdit.getText().toString().trim());
        amountEdit.setText("");
        noteEdit.setText("");
        refreshAll();
        Toast.makeText(this, "บันทึกแล้ว", Toast.LENGTH_SHORT).show();
        maybeSyncAfterChange();
    }

    boolean matchesSearch(String date,String type,String cat,double amount,String note) {
        String wantedType=searchTypeSpinner==null||searchTypeSpinner.getSelectedItem()==null ? "ทั้งหมด" : String.valueOf(searchTypeSpinner.getSelectedItem());
        if(!"ทั้งหมด".equals(wantedType) && !wantedType.equals(type)) return false;

        String period=searchPeriodSpinner==null||searchPeriodSpinner.getSelectedItem()==null ? "ทั้งหมด" : String.valueOf(searchPeriodSpinner.getSelectedItem());
        String today=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());
        if("วันนี้".equals(period) && !today.equals(date)) return false;
        if("เดือนนี้".equals(period) && (date==null || date.length()<7 || !date.substring(0,7).equals(today.substring(0,7)))) return false;
        if("ปีนี้".equals(period) && (date==null || date.length()<4 || !date.substring(0,4).equals(today.substring(0,4)))) return false;

        String q=searchEdit==null ? "" : searchEdit.getText().toString().trim().toLowerCase(Locale.getDefault());
        if(q.length()==0) return true;
        String hay=((note==null?"":note)+" "+(cat==null?"":cat)+" "+(type==null?"":type)+" "+
                money.format(amount)+" "+String.valueOf(amount)+" "+(date==null?"":date)).toLowerCase(Locale.getDefault());
        return hay.contains(q);
    }

    void refreshAll() {
        updateDateText();
        double inc = db.sumType("รายรับ");
        double exp = db.sumType("รายจ่าย");
        incomeText.setText("รายรับ\n" + money.format(inc));
        expenseText.setText("รายจ่าย\n" + money.format(exp));
        balanceText.setText("คงเหลือ\n" + money.format(inc-exp));

        listBox.removeAllViews();
        Cursor c = db.listAll();
        int shown=0;
        if (!c.moveToFirst()) {
            TextView empty = tv("ยังไม่มีรายการ", 14, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0,dp(20),0,dp(20));
            listBox.addView(empty);
            if(searchCount!=null) searchCount.setText("พบ 0 รายการ");
            c.close();
            return;
        }

        do {
            long id = c.getLong(0);
            String date = c.getString(1);
            String type = c.getString(2);
            String cat = c.getString(3);
            double amount = c.getDouble(4);
            String note = c.getString(5);

            if(!matchesSearch(date,type,cat,amount,note)) continue;
            shown++;

            LinearLayout item = row();
            item.setPadding(dp(10),dp(10),dp(10),dp(10));
            item.setBackgroundColor(PANEL);

            LinearLayout left = new LinearLayout(this);
            left.setOrientation(LinearLayout.VERTICAL);
            left.addView(tv(cat + (note == null || note.length()==0 ? "" : " • " + note), 15, TEXT, true));
            left.addView(tv(date + " • " + type, 12, MUTED, false));
            item.addView(left, new LinearLayout.LayoutParams(0,-2,1));

            int col = "รายรับ".equals(type) ? INCOME : EXPENSE;
            String sign = "รายรับ".equals(type) ? "+" : "-";
            TextView amt = tv(sign + money.format(amount), 16, col, true);
            amt.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            item.addView(amt);

            item.setOnLongClickListener(v -> {
                confirmDelete(id);
                return true;
            });

            listBox.addView(item, new LinearLayout.LayoutParams(-1,-2));
            space(listBox, 5);
        } while(c.moveToNext());
        c.close();

        if(searchCount!=null) searchCount.setText("พบ "+shown+" รายการ");
        if(shown==0) {
            TextView empty = tv("ไม่พบรายการที่ค้นหา", 14, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0,dp(20),0,dp(20));
            listBox.addView(empty);
        }
    }

    void confirmDelete(long id) {
        new AlertDialog.Builder(this)
                .setTitle("ลบรายการ")
                .setMessage("ต้องการลบรายการนี้หรือไม่?")
                .setNegativeButton("ยกเลิก", null)
                .setPositiveButton("ลบ", (d,w) -> {
                    db.softDelete(id);
                    refreshAll();
                    maybeSyncAfterChange();
                }).show();
    }

    int intervalIndex(int value) {
        int[] vals={1,5,10,15,30,60};
        for(int i=0;i<vals.length;i++) if(vals[i]==value)return i;
        return 1;
    }

    int intervalValue(int index) {
        int[] vals={1,5,10,15,30,60};
        if(index<0||index>=vals.length)return 5;
        return vals[index];
    }

    void showCloudSettings() {
        try {
            String[] old = SecurePrefs.load(this);
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(20), dp(6), dp(20), dp(6));

            EditText url = settingField("Project URL", old[0]);
            EditText key = settingField("anon / publishable key", old[1]);
            EditText email = settingField("Email", old[2]);
            EditText pass = settingField("Password", old[3]);
            pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

            Switch auto = new Switch(this); auto.setText("Auto Sync"); auto.setTextColor(TEXT);
            auto.setChecked(Boolean.parseBoolean(old[4]));

            Spinner interval = new Spinner(this);
            ArrayAdapter<String> ia=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
                    new String[]{"1 นาที","5 นาที","10 นาที","15 นาที","30 นาที","60 นาที"});
            interval.setAdapter(ia);
            int oldInt=5; try{oldInt=Integer.parseInt(old[5]);}catch(Exception ignored){}
            interval.setSelection(intervalIndex(oldInt));

            CheckBox onOpen=new CheckBox(this); onOpen.setText("Sync เมื่อเปิดแอป"); onOpen.setTextColor(TEXT);
            onOpen.setChecked(Boolean.parseBoolean(old[6]));
            CheckBox afterChange=new CheckBox(this); afterChange.setText("Sync หลัง เพิ่ม / แก้ / ลบ"); afterChange.setTextColor(TEXT);
            afterChange.setChecked(Boolean.parseBoolean(old[7]));
            CheckBox onClose=new CheckBox(this); onClose.setText("Sync เมื่อออก/พักแอป"); onClose.setTextColor(TEXT);
            onClose.setChecked(Boolean.parseBoolean(old[8]));

            box.addView(url); box.addView(key); box.addView(email); box.addView(pass);
            box.addView(auto); box.addView(interval); box.addView(onOpen); box.addView(afterChange); box.addView(onClose);

            new AlertDialog.Builder(this)
                    .setTitle("Cloud Settings")
                    .setMessage("ใช้ anon/publishable key เท่านั้น ห้ามใช้ service_role")
                    .setView(box)
                    .setNegativeButton("ยกเลิก", null)
                    .setPositiveButton("Save", (d,w) -> {
                        try {
                            SecurePrefs.save(this,url.getText().toString().trim(),key.getText().toString().trim(),
                                    email.getText().toString().trim(),pass.getText().toString(),
                                    auto.isChecked(),intervalValue(interval.getSelectedItemPosition()),
                                    onOpen.isChecked(),afterChange.isChecked(),onClose.isChecked());
                            configureAutoSync();
                            updateSyncStatus();
                            Toast.makeText(this,"บันทึก Cloud Settings แล้ว",Toast.LENGTH_SHORT).show();
                        } catch(Exception ex) {
                            Toast.makeText(this,"Save Error: "+ex.getMessage(),Toast.LENGTH_LONG).show();
                        }
                    }).show();
        } catch(Exception ex) {
            Toast.makeText(this,"Settings Error: "+ex.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    EditText settingField(String hint, String value) {
        EditText e=new EditText(this);
        e.setHint(hint); e.setHintTextColor(MUTED); e.setTextColor(TEXT); e.setText(value);
        e.setBackgroundColor(PANEL); e.setPadding(dp(10),0,dp(10),0);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52)); p.setMargins(0,0,0,dp(7));
        e.setLayoutParams(p); return e;
    }

    void configureAutoSync() {
        if(autoRunnable!=null) autoHandler.removeCallbacks(autoRunnable);
        try {
            String[] c=SecurePrefs.load(this);
            boolean enabled=Boolean.parseBoolean(c[4]);
            int mins=5; try{mins=Integer.parseInt(c[5]);}catch(Exception ignored){}
            final long intervalMs=Math.max(1,mins)*60L*1000L;
            autoRunnable=new Runnable() {
                @Override public void run() {
                    try {
                        String[] x=SecurePrefs.load(MainActivity.this);
                        if(Boolean.parseBoolean(x[4])) syncNow(false);
                    } catch(Exception ignored) {}
                    autoHandler.postDelayed(this,intervalMs);
                }
            };
            if(enabled) autoHandler.postDelayed(autoRunnable,intervalMs);
        } catch(Exception ignored) {}
    }

    void maybeSyncAfterChange() {
        try {
            String[] c=SecurePrefs.load(this);
            if(Boolean.parseBoolean(c[7])) syncNow(false);
        } catch(Exception ignored) {}
    }

    void updateSyncStatus() {
        try {
            String[] c=SecurePrefs.load(this);
            boolean auto=Boolean.parseBoolean(c[4]);
            int mins=5; try{mins=Integer.parseInt(c[5]);}catch(Exception ignored){}
            String last=lastSyncMs==0?"-":new SimpleDateFormat("HH:mm:ss",Locale.US).format(new Date(lastSyncMs));
            String next="-";
            if(auto) {
                long base=lastSyncMs==0?System.currentTimeMillis():lastSyncMs;
                next=new SimpleDateFormat("HH:mm:ss",Locale.US).format(new Date(base+mins*60L*1000L));
            }
            syncStatus.setText("Cloud: "+(syncing?"กำลัง Sync...":"พร้อม")+"   •   Last: "+last+"   •   Next: "+(auto?next:"Auto OFF"));
        } catch(Exception e) {
            syncStatus.setText("Cloud: ยังไม่ตั้งค่า");
        }
    }

    void syncNow() { syncNow(true); }

    void syncNow(boolean showToast) {
        final String[] c;
        try { c=SecurePrefs.load(this); }
        catch(Exception ex) { Toast.makeText(this,ex.getMessage(),Toast.LENGTH_LONG).show(); return; }
        if(c[0].length()==0 || c[1].length()==0 || c[2].length()==0 || c[3].length()==0) {
            if(showToast) Toast.makeText(this,"กรุณาตั้งค่า Cloud Settings ก่อน",Toast.LENGTH_SHORT).show();
            return;
        }
        if(syncing)return;
        syncing=true;
        updateSyncStatus();
        new Thread(() -> {
            try {
                SupabaseSyncClient cli=new SupabaseSyncClient(c[0],c[1],c[2],c[3]);
                cli.login();
                cli.pushDirty(db);
                cli.pullAll(db);
                db.markAllClean();
                runOnUiThread(() -> {
                    syncing=false;
                    lastSyncMs=System.currentTimeMillis();
                    refreshAll();
                    updateSyncStatus();
                    if(showToast) Toast.makeText(this,"Sync สำเร็จ",Toast.LENGTH_SHORT).show();
                });
            } catch(Exception ex) {
                runOnUiThread(() -> {
                    syncing=false;
                    syncStatus.setText("Cloud: Sync Error");
                    if(showToast) Toast.makeText(this,"Sync Error: "+ex.getMessage(),Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    @Override protected void onResume() {
        super.onResume();
        configureAutoSync();
        updateSyncStatus();
    }

    @Override protected void onStop() {
        try {
            String[] c=SecurePrefs.load(this);
            if(Boolean.parseBoolean(c[8])) syncNow(false);
        } catch(Exception ignored) {}
        super.onStop();
    }

    @Override protected void onDestroy() {
        if(autoRunnable!=null) autoHandler.removeCallbacks(autoRunnable);
        super.onDestroy();
    }

}
