package com.anasio.battleships.ui.views;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class JavaBattleSurrenderDialog {

    public interface OnConfirmListener {
        void onConfirm();
    }

    public interface OnCancelListener {
        void onCancel();
    }

    public static Dialog show(Context context,
                              String title,
                              String message,
                              String yesLabel,
                              String noLabel,
                              int surfaceColor,
                              int borderColor,
                              int primaryColor,
                              int dangerColor,
                              int textColor,
                              int secondaryTextColor,
                              OnConfirmListener onConfirm,
                              OnCancelListener onCancel) {
        Dialog dialog = new Dialog(context);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(context, 20), dp(context, 20), dp(context, 20), dp(context, 18));
        panel.setBackground(background(context, surfaceColor, borderColor, 16));

        LinearLayout heading = new LinearLayout(context);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);

        TextView icon = new TextView(context);
        icon.setText("🏳️");
        icon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        icon.setGravity(Gravity.CENTER);
        heading.addView(icon, new LinearLayout.LayoutParams(dp(context, 36), dp(context, 36)));

        TextView headingText = new TextView(context);
        headingText.setText(title);
        headingText.setTextColor(textColor);
        headingText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        headingText.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams headingTextParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        headingTextParams.leftMargin = dp(context, 12);
        heading.addView(headingText, headingTextParams);
        panel.addView(heading);

        TextView body = new TextView(context);
        body.setText(message);
        body.setTextColor(secondaryTextColor);
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        body.setLineSpacing(dp(context, 3), 1f);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyParams.topMargin = dp(context, 14);
        panel.addView(body, bodyParams);

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionsParams.topMargin = dp(context, 22);
        panel.addView(actions, actionsParams);

        TextView cancelButton = actionButton(context, noLabel, primaryColor, surfaceColor, borderColor);
        actions.addView(cancelButton, new LinearLayout.LayoutParams(
                0, dp(context, 48), 1f));

        TextView confirmButton = actionButton(context, yesLabel, Color.WHITE, dangerColor, dangerColor);
        LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(
                0, dp(context, 48), 1f);
        confirmParams.leftMargin = dp(context, 10);
        actions.addView(confirmButton, confirmParams);

        dialog.setContentView(panel, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.setOnCancelListener(ignored -> {
            if (onCancel != null) onCancel.onCancel();
        });
        cancelButton.setOnClickListener(view -> {
            dialog.dismiss();
            if (onCancel != null) onCancel.onCancel();
        });
        confirmButton.setOnClickListener(view -> {
            dialog.dismiss();
            if (onConfirm != null) onConfirm.onConfirm();
        });

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.width = Math.min(dp(context, 360),
                    context.getResources().getDisplayMetrics().widthPixels - dp(context, 48));
            attributes.height = WindowManager.LayoutParams.WRAP_CONTENT;
            attributes.gravity = Gravity.CENTER;
            attributes.dimAmount = 0.68f;
            attributes.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            window.setAttributes(attributes);
        }
        return dialog;
    }

    private static TextView actionButton(Context context, String label, int textColor,
                                         int fillColor, int strokeColor) {
        TextView button = new TextView(context);
        button.setText(label);
        button.setTextColor(textColor);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        button.setTypeface(null, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(background(context, fillColor, strokeColor, 10));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private static GradientDrawable background(Context context, int fillColor,
                                               int strokeColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fillColor);
        drawable.setStroke(dp(context, 1), strokeColor);
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    private static int dp(Context context, int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics());
    }
}