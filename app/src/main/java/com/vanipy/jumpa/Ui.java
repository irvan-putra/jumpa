package com.vanipy.jumpa;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small programmatic-UI helpers so every screen shares one look. */
public final class Ui {
    public static final int INK = 0xFF0E5C4B;
    public static final int INK_DARK = 0xFF093B30;
    public static final int AMBER = 0xFFCC8A00;
    public static final int TEXT = 0xFF1B2A26;
    public static final int MUTED = 0xFF5C6B66;

    private Ui() {
    }

    public static int dp(Context ctx, int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density);
    }

    public static LinearLayout column(Context ctx) {
        LinearLayout l = new LinearLayout(ctx);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(ctx, 12), dp(ctx, 4), dp(ctx, 12), dp(ctx, 12));
        return l;
    }

    /** A white rounded card with a soft outline. */
    public static LinearLayout card(Context ctx) {
        LinearLayout l = new LinearLayout(ctx);
        l.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(ctx, 14));
        bg.setStroke(Math.max(1, dp(ctx, 1)), 0xFFDDE5E1);
        l.setBackground(bg);
        l.setPadding(dp(ctx, 14), dp(ctx, 12), dp(ctx, 14), dp(ctx, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(ctx, 8), 0, 0);
        l.setLayoutParams(lp);
        return l;
    }

    public static TextView heading(Context ctx, String text) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextSize(12);
        t.setTextColor(MUTED);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(dp(ctx, 4), dp(ctx, 14), 0, 0);
        return t;
    }

    public static TextView line(Context ctx, String text) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextSize(16);
        t.setTextColor(TEXT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    public static TextView muted(Context ctx, String text) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextSize(13);
        t.setTextColor(MUTED);
        return t;
    }

    public static TextView button(Context ctx, String text) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextSize(14);
        t.setTextColor(INK);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(dp(ctx, 14), dp(ctx, 10), dp(ctx, 14), dp(ctx, 10));
        return t;
    }
}
