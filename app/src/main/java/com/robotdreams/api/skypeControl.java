package com.robotdreams.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.content.ComponentName;
import android.app.Activity;

/**
 * Created by itaimendelsohn on 10/14/15.
 */
public class skypeControl {

    final String[] whotocall = {"amiruk2004", "danielle.mendelsohn", "dorinphilly"};
    private Context context;
    private TextToSpeech tts = null;
    private int which;
    private String nameToCall = null;

    public skypeControl(Context context)
    {
        this.context = context;
        this.which = 1; // danielle is the default
    }

    public void setTts(TextToSpeech tts) { this.tts = tts;}

    public void setWhich(int which) { this.which = which;}

    public void setNameToCall(String name) {this.nameToCall = name;}

    public String getNameToCall() { return this.nameToCall;}

    public void startSkype()
    {
        String skypeName = whotocall[which];
        String mySkypeUri = "skype:" + skypeName + "?call&video=true";
        String callingStr = "calling" + nameToCall;


        Uri skypeUri = Uri.parse(mySkypeUri);
        Intent myIntent = new Intent(Intent.ACTION_VIEW, skypeUri);
        myIntent.setComponent(new ComponentName("com.skype.raider", "com.skype.raider.Main"));
        myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.context.startActivity(myIntent);

        if (this.tts != null)
            this.tts.speak(callingStr, TextToSpeech.QUEUE_FLUSH, null, "1");
    }


}
