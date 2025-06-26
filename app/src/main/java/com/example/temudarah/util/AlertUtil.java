package com.example.temudarah.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.example.temudarah.R;

public class AlertUtil {

    /**
     * Shows a custom alert dialog with positive and negative buttons
     *
     * @param context       The context of the activity/fragment
     * @param title         Title of the alert
     * @param message       Message to display
     * @param positiveText  Text for the positive button
     * @param negativeText  Text for the negative button
     * @param positiveClick Listener for positive button click
     * @param negativeClick Listener for negative button click
     */
    public static void showAlert(Context context, String title, String message,
                               String positiveText, String negativeText,
                               View.OnClickListener positiveClick,
                               View.OnClickListener negativeClick) {

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.custom_alert_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);

        TextView tvTitle = dialog.findViewById(R.id.tv_alert_title);
        TextView tvMessage = dialog.findViewById(R.id.tv_alert_message);
        Button btnPositive = dialog.findViewById(R.id.btn_positive);
        Button btnNegative = dialog.findViewById(R.id.btn_negative);

        tvTitle.setText(title);
        tvMessage.setText(message);

        // Set button text if provided
        if (positiveText != null && !positiveText.isEmpty()) {
            btnPositive.setText(positiveText);
            btnPositive.setVisibility(View.VISIBLE);
        } else {
            btnPositive.setVisibility(View.GONE);
        }

        if (negativeText != null && !negativeText.isEmpty()) {
            btnNegative.setText(negativeText);
            btnNegative.setVisibility(View.VISIBLE);
        } else {
            btnNegative.setVisibility(View.GONE);
        }

        // Set positive button click listener
        if (positiveClick != null) {
            btnPositive.setOnClickListener(v -> {
                positiveClick.onClick(v);
                dialog.dismiss();
            });
        } else {
            btnPositive.setOnClickListener(v -> dialog.dismiss());
        }

        // Set negative button click listener
        if (negativeClick != null) {
            btnNegative.setOnClickListener(v -> {
                negativeClick.onClick(v);
                dialog.dismiss();
            });
        } else {
            btnNegative.setOnClickListener(v -> dialog.dismiss());
        }

        // Show dialog if activity is not finishing
        if (context instanceof Activity) {
            if (!((Activity) context).isFinishing()) {
                dialog.show();
            }
        } else {
            dialog.show();
        }
    }

    /**
     * Shows a simple alert with just an OK button
     */
    public static void showSimpleAlert(Context context, String title, String message) {
        showAlert(context, title, message, "OK", null, null, null);
    }

    /**
     * Shows a confirmation alert with Yes/No buttons
     */
    public static void showConfirmationAlert(Context context, String title, String message,
                                          View.OnClickListener onConfirm) {
        showAlert(context, title, message, "Ya", "Tidak", onConfirm, null);
    }

    /**
     * Shows an error alert
     */
    public static void showErrorAlert(Context context, String message) {
        showAlert(context, "Error", message, "OK", null, null, null);
    }

    /**
     * Shows a success alert
     */
    public static void showSuccessAlert(Context context, String message) {
        showAlert(context, "Berhasil", message, "OK", null, null, null);
    }
}
