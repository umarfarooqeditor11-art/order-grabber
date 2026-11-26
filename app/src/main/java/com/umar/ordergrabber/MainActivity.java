package com.umar.ordergrabber;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    static final String PREFS = "grabber";
    static final String KEY_MAX = "max_amount";
    static final String KEY_INTERVAL = "interval_ms";
    static final String KEY_WALLET = "wallet_name";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 96, 48, 48);

        TextView title = new TextView(this);
        title.setText("Order Grabber");
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        root.addView(label("Sab se zyada order raqam (PKR):"));
        EditText max = number(String.valueOf(p.getInt(KEY_MAX, 500)));
        root.addView(max);

        root.addView(label("Refresh ka waqfa (milli-second, 150 se kam na rakhein):"));
        EditText interval = number(String.valueOf(p.getInt(KEY_INTERVAL, 200)));
        root.addView(interval);

        root.addView(label("Wallet ka naam (jo wallet list mein likha hai):"));
        EditText wallet = new EditText(this);
        wallet.setText(p.getString(KEY_WALLET, "Digitt"));
        root.addView(wallet);

        Button save = new Button(this);
        save.setText("Save");
        save.setOnClickListener(v -> {
            int m = parse(max.getText().toString(), 500);
            int i = Math.max(100, parse(interval.getText().toString(), 200));
            p.edit().putInt(KEY_MAX, m).putInt(KEY_INTERVAL, i).putString(KEY_WALLET, wallet.getText().toString().trim()).apply();
            Toast.makeText(this, "Save ho gaya", Toast.LENGTH_SHORT).show();
        });
        root.addView(save);

        Button acc = new Button(this);
        acc.setText("Accessibility on karein");
        acc.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(acc);

        root.addView(label("\nTareeqa:\n1) Raqam, waqfa aur wallet ka naam save karein.\n"
                + "2) Accessibility mein 'Order Grabber' on karein.\n"
                + "3) Market app ki Buy screen kholein, 100-500 tab chunein.\n"
                + "4) Screen par lal 'START' button dabaein.\n"
                + "5) Order milte hi app ruk jayegi aur mobile vibrate karega. Payment khud karein."));

        setContentView(root);
    }

    private TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(16);
        t.setPadding(0, 32, 0, 8);
        return t;
    }

    private EditText number(String v) {
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setText(v);
        return e;
    }

    private static int parse(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
}
package com.umar.ordergrabber;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Settings screen: max order amount, refresh speed, and a shortcut to Accessibility settings. */
public class MainActivity extends Activity {

    static final String PREFS = "grabber";
    static final String KEY_MAX = "max_amount";
    static final String KEY_INTERVAL = "interval_ms";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 96, 48, 48);

        TextView title = new TextView(this);
        title.setText("Order Grabber");
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        root.addView(label("Sab se zyada order raqam (PKR):"));
        EditText max = number(String.valueOf(p.getInt(KEY_MAX, 500)));
        root.addView(max);

        root.addView(label("Refresh ka waqfa (milli-second, 150 se kam na rakhein):"));
        EditText interval = number(String.valueOf(p.getInt(KEY_INTERVAL, 200)));
        root.addView(interval);

        Button save = new Button(this);
        save.setText("Save");
        save.setOnClickListener(v -> {
            int m = parse(max.getText().toString(), 500);
            int i = Math.max(100, parse(interval.getText().toString(), 200));
            p.edit().putInt(KEY_MAX, m).putInt(KEY_INTERVAL, i).apply();
            Toast.makeText(this, "Save ho gaya", Toast.LENGTH_SHORT).show();
        });
        root.addView(save);

        Button acc = new Button(this);
        acc.setText("Accessibility on karein");
        acc.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(acc);

        root.addView(label("\nTareeqa:\n1) Upar raqam aur waqfa save karein.\n"
                + "2) Accessibility mein 'Order Grabber' on karein.\n"
                + "3) Market app ki Buy screen kholein, 100-500 tab chunein.\n"
                + "4) Screen par lal 'START' button dabaein.\n"
                + "5) Order milte hi app ruk jayegi aur mobile vibrate karega. Payment khud karein."));

        setContentView(root);
    }

    private TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(16);
        t.setPadding(0, 32, 0, 8);
        return t;
    }

    private EditText number(String v) {
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setText(v);
        return e;
    }

    private static int parse(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
}
