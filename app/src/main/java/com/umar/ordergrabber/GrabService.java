package com.umar.ordergrabber;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GrabService extends AccessibilityService {

    private static final String CLAIM = "claim purchase order";
    private static final String DONE = "done";
    private static final Pattern AMOUNT = Pattern.compile("([0-9][0-9,]*\\.?[0-9]*)\\s*PKR");

    private final Handler h = new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private TextView button;
    private boolean running = false;

    private long lastRefresh = 0;

    private int maxAmount = 500;
    private int intervalMs = 200;

    private final Runnable loop = new Runnable() {
        @Override public void run() {
            if (!running) return;
            try { tick(); } catch (Exception ignored) {}
            h.postDelayed(this, 40);
        }
    };

    @Override
    protected void onServiceConnected() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        addFloatingButton();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        if (running) { try { tick(); } catch (Exception ignored) {} }
    }

    @Override public void onInterrupt() {}

    // States: 0 = looking for orders, 1 = claim tapped (waiting for wallet list), 2 = wallet tapped (waiting for result)
    private int state = 0;
    private long stateAt = 0;
    private String walletName = "digitt";

    private static final String[] FAIL_WORDS = {"already been claimed", "claim failed", "order failed", "failed"};
    private static final String[] OK_WORDS = {"order get", "order got", "success", "successful", "claimed successfully"};

    private void setState(int s, long now) { state = s; stateAt = now; }

    private void tick() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        long now = SystemClock.uptimeMillis();

        for (String w : FAIL_WORDS) {
            if (hasText(root, w)) {
                AccessibilityNodeInfo done = findClickableByText(root, DONE);
                if (done == null) done = findClickableByText(root, "ok");
                if (done != null) done.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                setState(0, now);
                lastRefresh = 0;
                return;
            }
        }

        if (state == 2) {
            for (String w : OK_WORDS) if (hasText(root, w)) { success(); return; }
            if (now - stateAt > 4000) { success(); return; }
            return;
        }

        if (state == 1) {
            AccessibilityNodeInfo wallet = findWallet(root);
            if (wallet != null) {
                wallet.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                setState(2, now);
                return;
            }
            if (now - stateAt > 2500) setState(0, now);
            return;
        }

        List<AccessibilityNodeInfo> claims = findAllByText(root, CLAIM);
        AccessibilityNodeInfo best = null;
        int bestTop = Integer.MAX_VALUE;
        for (AccessibilityNodeInfo c : claims) {
            AccessibilityNodeInfo btn = clickableSelfOrParent(c);
            if (btn == null || !btn.isEnabled() || !c.isEnabled()) continue;
            double amt = amountForCard(c);
            if (amt > 0 && amt > maxAmount) continue;
            android.graphics.Rect r = new android.graphics.Rect();
            btn.getBoundsInScreen(r);
            if (r.top < bestTop && r.height() > 0) { bestTop = r.top; best = btn; }
        }
        if (best != null) {
            best.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            setState(1, now);
            return;
        }

        if (now - lastRefresh >= intervalMs) {
            lastRefresh = now;
            pullToRefresh();
        }
    }

    private AccessibilityNodeInfo findWallet(AccessibilityNodeInfo root) {
        if (walletName.isEmpty()) return null;
        for (AccessibilityNodeInfo n : findAllByText(root, walletName)) {
            AccessibilityNodeInfo c = clickableSelfOrParent(n);
            if (c != null && c.isEnabled()) return c;
        }
        return null;
    }

    private void success() {
        stop();
        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (v != null) v.vibrate(VibrationEffect.createWaveform(new long[]{0, 600, 200, 600, 200, 600}, -1));
        if (button != null) { button.setText("MIL GAYA!\nPAY KAREIN"); button.setBackgroundColor(Color.rgb(0, 150, 60)); }
    }

    private void pullToRefresh() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float x = dm.widthPixels * 0.5f;
        Path p = new Path();
        p.moveTo(x, dm.heightPixels * 0.38f);
        p.lineTo(x, dm.heightPixels * 0.78f);
        GestureDescription g = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(p, 0, 120)).build();
        dispatchGesture(g, null, null);
    }

    private double amountForCard(AccessibilityNodeInfo claim) {
        AccessibilityNodeInfo n = claim;
        for (int up = 0; up < 6 && n != null; up++) {
            n = n.getParent();
            if (n == null) break;
            String t = collectText(n, 0);
            Matcher m = AMOUNT.matcher(t);
            if (m.find()) {
                try { return Double.parseDouble(m.group(1).replace(",", "")); } catch (Exception ignored) {}
            }
        }
        return -1;
    }

    private String collectText(AccessibilityNodeInfo n, int depth) {
        if (n == null || depth > 8) return "";
        StringBuilder sb = new StringBuilder();
        if (n.getText() != null) sb.append(n.getText()).append(' ');
        if (n.getContentDescription() != null) sb.append(n.getContentDescription()).append(' ');
        for (int i = 0; i < n.getChildCount(); i++) sb.append(collectText(n.getChild(i), depth + 1));
        return sb.toString();
    }

    private boolean hasText(AccessibilityNodeInfo root, String s) {
        return !findAllByText(root, s).isEmpty();
    }

    private List<AccessibilityNodeInfo> findAllByText(AccessibilityNodeInfo root, String s) {
        List<AccessibilityNodeInfo> out = new ArrayList<>();
        List<AccessibilityNodeInfo> found = root.findAccessibilityNodeInfosByText(s);
        if (found != null) out.addAll(found);
        if (out.isEmpty()) walk(root, s.toLowerCase(), out, 0);
        return out;
    }

    private void walk(AccessibilityNodeInfo n, String s, List<AccessibilityNodeInfo> out, int d) {
        if (n == null || d > 30) return;
        CharSequence t = n.getText() != null ? n.getText() : n.getContentDescription();
        if (t != null && t.toString().toLowerCase().contains(s)) out.add(n);
        for (int i = 0; i < n.getChildCount(); i++) walk(n.getChild(i), s, out, d + 1);
    }

    private AccessibilityNodeInfo findClickableByText(AccessibilityNodeInfo root, String s) {
        for (AccessibilityNodeInfo n : findAllByText(root, s)) {
            CharSequence t = n.getText() != null ? n.getText() : n.getContentDescription();
            if (t != null && t.toString().trim().equalsIgnoreCase(s)) {
                AccessibilityNodeInfo c = clickableSelfOrParent(n);
                if (c != null) return c;
            }
        }
        return null;
    }

    private AccessibilityNodeInfo clickableSelfOrParent(AccessibilityNodeInfo n) {
        for (int i = 0; i < 4 && n != null; i++) {
            if (n.isClickable()) return n;
            n = n.getParent();
        }
        return null;
    }

    private void addFloatingButton() {
        button = new TextView(this);
        button.setText("START");
        button.setTextColor(Color.WHITE);
        button.setTextSize(16);
        button.setGravity(Gravity.CENTER);
        button.setPadding(36, 24, 36, 24);
        button.setBackgroundColor(Color.rgb(200, 20, 50));

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = 20;
        lp.y = 300;

        final float[] down = new float[4];
        button.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    down[0] = e.getRawX(); down[1] = e.getRawY(); down[2] = lp.x; down[3] = lp.y;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    lp.x = (int) (down[2] + e.getRawX() - down[0]);
                    lp.y = (int) (down[3] + e.getRawY() - down[1]);
                    wm.updateViewLayout(button, lp);
                    return true;
                case MotionEvent.ACTION_UP:
                    if (Math.abs(e.getRawX() - down[0]) < 15 && Math.abs(e.getRawY() - down[1]) < 15) toggle();
                    return true;
            }
            return false;
        });
        wm.addView(button, lp);
    }

    private void toggle() {
        if (running) stop(); else start();
    }

    private void start() {
        SharedPreferences p = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
        maxAmount = p.getInt(MainActivity.KEY_MAX, 500);
        intervalMs = Math.max(100, p.getInt(MainActivity.KEY_INTERVAL, 200));
        walletName = p.getString(MainActivity.KEY_WALLET, "Digitt").trim().toLowerCase();
        running = true;
        state = 0;
        lastRefresh = 0;
        button.setText("STOP");
        button.setBackgroundColor(Color.rgb(40, 40, 40));
        h.post(loop);
    }

    private void stop() {
        running = false;
        h.removeCallbacks(loop);
        if (button != null) { button.setText("START"); button.setBackgroundColor(Color.rgb(200, 20, 50)); }
    }
}
