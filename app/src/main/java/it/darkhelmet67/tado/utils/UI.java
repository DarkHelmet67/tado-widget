package it.darkhelmet67.tado.utils;


import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import it.darkhelmet67.tado.R;

public class UI {

    public static void showDialog(Context context,
                                  AlertType alertType,
                                  String title, String message,
                                  DialogInterface.OnClickListener onYesClickListener,
                                  DialogInterface.OnClickListener onNoClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
        // AlertDialog.Builder builder = new AlertDialog.Builder(context);
        switch (alertType) {
            case YES_NO:
                builder.setPositiveButton(context.getString(R.string.label_yes), onYesClickListener);
                builder.setNegativeButton(context.getString(R.string.label_no), onNoClickListener);
                break;
            case OK_CANCEL:
                builder.setPositiveButton(context.getString(R.string.label_ok), onYesClickListener);
                builder.setNegativeButton(context.getString(R.string.label_cancel), onNoClickListener);
                break;
            case OK:
                builder.setPositiveButton(context.getString(R.string.label_ok), onYesClickListener);
                break;
            case CANCEL:
                builder.setNegativeButton(context.getString(R.string.label_cancel), onNoClickListener);
                break;
        }
        builder.setTitle(title);
        builder.setMessage(message);
        builder.show();
    }

    public static enum AlertType {
        YES_NO,
        OK_CANCEL,
        OK,
        CANCEL
    }
}
