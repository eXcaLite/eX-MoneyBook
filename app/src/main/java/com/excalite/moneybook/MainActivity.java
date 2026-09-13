package com.excalite.moneybook;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
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
    TextView incomeText, expenseText, balanceText, dateText;
    Spinner typeSpinner, categorySpinner;
    EditText amountEdit, noteEdit;
    String selectedDate;
    DecimalFormat money = new DecimalFormat("#,##0.00");

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

        root.addView(tv("รายการล่าสุด", 18, TEXT, true));
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
        if (!c.moveToFirst()) {
            TextView empty = tv("ยังไม่มีรายการ", 14, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0,dp(20),0,dp(20));
            listBox.addView(empty);
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
    }

    void confirmDelete(long id) {
        new AlertDialog.Builder(this)
                .setTitle("ลบรายการ")
                .setMessage("ต้องการลบรายการนี้หรือไม่?")
                .setNegativeButton("ยกเลิก", null)
                .setPositiveButton("ลบ", (d,w) -> {
                    db.delete(id);
                    refreshAll();
                }).show();
    }
}
