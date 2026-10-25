package com.skypier.capri;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class CapriAccessibilityService extends AccessibilityService {
    static CapriAccessibilityService instance;

    @Override public void onServiceConnected() { instance = this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }
    @Override public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }

    public boolean tapText(String needle) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        try { return search(root, needle.toLowerCase(java.util.Locale.ROOT)); }
        finally { root.recycle(); }
    }

    private boolean search(AccessibilityNodeInfo node, String needle) {
        if (node == null) return false;
        CharSequence text = node.getText(), desc = node.getContentDescription();
        boolean matches = (text != null && text.toString().toLowerCase(java.util.Locale.ROOT).contains(needle)) ||
            (desc != null && desc.toString().toLowerCase(java.util.Locale.ROOT).contains(needle));
        if (matches) {
            AccessibilityNodeInfo cur = node;
            while (cur != null) {
                if (cur.isClickable() && cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
                cur = cur.getParent();
            }
            Rect r = new Rect();
            node.getBoundsInScreen(r);
            if (!r.isEmpty()) return tap(r.centerX(), r.centerY());
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null && search(child, needle)) return true;
        }
        return false;
    }

    private boolean tap(int x, int y) {
        Path p = new Path();
        p.moveTo(x, y);
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, 80)).build();
        return dispatchGesture(gesture, null, null);
    }

    public boolean scroll(boolean down) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        try { if (scrollNode(root, down)) return true; }
        finally { root.recycle(); }
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        float x = dm.widthPixels / 2f;
        float start = dm.heightPixels * (down ? .76f : .30f);
        float end = dm.heightPixels * (down ? .30f : .76f);
        Path p = new Path();
        p.moveTo(x, start);
        p.lineTo(x, end);
        return dispatchGesture(new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, 400)).build(), null, null);
    }

    private boolean scrollNode(AccessibilityNodeInfo n, boolean down) {
        if (n.isScrollable() && n.performAction(down ?
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) return true;
        for (int i = 0; i < n.getChildCount(); i++) {
            AccessibilityNodeInfo child = n.getChild(i);
            if (child != null && scrollNode(child, down)) return true;
        }
        return false;
    }
}
