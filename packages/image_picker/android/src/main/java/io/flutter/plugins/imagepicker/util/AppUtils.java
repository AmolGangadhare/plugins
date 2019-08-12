package io.flutter.plugins.imagepicker.util;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import io.flutter.plugins.imagepicker.ImagePickerDelegate;
import io.flutter.plugins.imagepicker.R;

public class AppUtils {
    private static AlertDialog.Builder alertBuilder;
    private static AlertDialog alertDialog;

    /**
     * Show progress dialog
     */
    public static void showProgressDialog(String message) {
        try {
            if (null != alertBuilder && null != alertDialog) {
                if (alertDialog.isShowing()) {
                    return;
                }
            }

            Context context = ImagePickerDelegate.activity;

            if (context == null)
                return;

            LinearLayout linearLayout = new LinearLayout(context);

            linearLayout.setOrientation(LinearLayout.HORIZONTAL);

            TextView textView1 = new TextView(context);
            textView1.setText(message);
            ProgressBar progressBar = new ProgressBar(context);

            linearLayout.setGravity(Gravity.CENTER);
            linearLayout.addView(textView1);
            linearLayout.addView(progressBar);

            alertBuilder = new AlertDialog.Builder(context);
            alertBuilder.setView(linearLayout);
            alertBuilder.setCancelable(false);
            alertDialog = alertBuilder.create();
            if (!ImagePickerDelegate.activity.isFinishing()) {
                alertDialog.show();
            }
        } catch (Exception e) {
            Log.e("AppUtils", "showProgressDialog: " + e.getLocalizedMessage());
        }
    }


    /**
     * Cancel progress dialog
     */
    public static void cancelProgressDialog() {
        try {
            if (null != alertBuilder && null != alertDialog) {
                if (alertDialog.isShowing()) {
                    alertDialog.dismiss();
                    alertDialog.cancel();
                }
            }
        } catch (Exception e) {
            Log.e("AppUtils", "cancelProgressDialog: " + e.getLocalizedMessage());
        }
    }
}