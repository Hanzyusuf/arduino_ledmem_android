package com.example.arduinoledmemory.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.arduinoledmemory.R;

public class CustomProgressDialog {

    private static CustomProgressDialog instance = null;
    private ImageView ledLightsImg;
    private AlertDialog dialog;
    private TextView messageTextView;
    private boolean isActive;
    private static Activity currentActivity = null;

    private AnimationDrawable animationDrawable;

    public static void registerActivity(Activity activity){
        if(currentActivity!=activity) {
            currentActivity = activity;
            CustomProgressDialog.getInstance().setUpProgressDialog();
        }
    }

    public static void unregisterActivity(Activity activity){
        if(currentActivity == activity){
            currentActivity = null;
        }
    }

    private CustomProgressDialog(){}

    public static CustomProgressDialog getInstance(){
        if(instance==null)
            instance = new CustomProgressDialog();
        return instance;
    }

    private void setUpProgressDialog(){
        dismiss(); //if already shown

        LayoutInflater inflater = LayoutInflater.from(currentActivity);
        View alertLayout = inflater.inflate(R.layout.custom_progress_dialog, null);
        ledLightsImg = (ImageView) alertLayout.findViewById(R.id.custom_progress_dialog_spin_img);
        messageTextView = (TextView) alertLayout.findViewById(R.id.custom_progress_dialog_spin_img_message);

        android.app.AlertDialog.Builder alertDialog = new android.app.AlertDialog.Builder(currentActivity);
        alertDialog.setView(alertLayout);
        alertDialog.setCancelable(false);
        dialog = alertDialog.create();

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        ledLightsImg.setBackgroundResource(R.drawable.anim_blinking_led);
        animationDrawable = (AnimationDrawable)ledLightsImg.getBackground();
    }

    // CALL THIS METHOD IF YOU WANT TO CHANGE THE DEFAULT MESSAGE "Please wait..."
    public void setMessage(String message){
        messageTextView.setText(message);
    }

    public void show(){
        dialog.show();

        if(animationDrawable != null)
            animationDrawable.start();

        isActive = true;
    }

    public void dismiss(){
        try{
            if(animationDrawable != null)
                animationDrawable.stop();
            dialog.dismiss();
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        isActive = false;
    }

    public boolean isShown(){
        return isActive;
    }
}