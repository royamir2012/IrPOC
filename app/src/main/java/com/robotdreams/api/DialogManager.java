package com.robotdreams.api;

import java.util.*;

import ai.api.model.*;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;


import ai.api.AIConfiguration;
import ai.api.AIListener;
import ai.api.AIService;
import ai.api.RequestExtras;
import android.os.Message;
import com.robotdreams.R;
import com.robotdreams.ui.activity.BotActivity;

/**
 *
 */


public class DialogManager {


    private static final String CLIENT_ACCESS_TOKEN = "38ecf01bfbd74b9a92ae6569932e4db6";
    private static final String SUBSCRIPTION_KEY = "0a802526-a4bf-4eb2-b03e-2b81f2061121";

    private static final String CLIENT_ACCESS_TOKEN_FAIL = "8d055613884d4b49acb6726195b78968";
    private static final String SUBSCRIPTION_KEY_FAIL = "dc39a5a2-0ab8-4ac4-bb7b-67099d8c6064";

    private static final String CLIENT_ACCESS_TOKEN_PODCAST = "988545736d3c4b8c83dee67783abba4d";
    private static final String SUBSCRIPTION_KEY_PODCAST = "dc39a5a2-0ab8-4ac4-bb7b-67099d8c6064";

    private static final int MOOD_SAD = 1;
    private static final int MOOD_HAPPY = 2;
    public static final int PRINT_CAMERA = 1;
    public static final int NO_PRINT_CAMERA = 2;


    public enum msgType {
        Camera, Skype
    }

    //
    private AIService aiService_help;
    private AIService aiService_PodCast;
    private AIService currentaiService;

    //
    private MediaPlayer audioPlayer;
    //
    private Context context;
    //
    private Handler messageHandler;

    //
    private WolframAlphaAPI wolframAlphaAPI;

    //
    private int Mood;
    private int printCameraResults;


    public DialogManager(Context context, AIListener listener, Handler messageHandler) {

        // let's try two agents
        final AIConfiguration config = new AIConfiguration(CLIENT_ACCESS_TOKEN_FAIL,
                SUBSCRIPTION_KEY_FAIL,
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
        final AIConfiguration configPodcast = new AIConfiguration(CLIENT_ACCESS_TOKEN_PODCAST,
                SUBSCRIPTION_KEY_PODCAST,
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);


        aiService_help = AIService.getService(context, config);
        aiService_help.setListener(listener);
        aiService_PodCast = AIService.getService(context, configPodcast);
        aiService_PodCast.setListener(listener);
        currentaiService = aiService_PodCast;

        wolframAlphaAPI = new WolframAlphaAPI();

        this.context = context;
        this.messageHandler = messageHandler;
        Mood = MOOD_HAPPY;
        printCameraResults = NO_PRINT_CAMERA;
        audioPlayer = null;
    }


    public String sendRequest(String input) {

        try {
            return new SendRequest().execute(input).get();
        } catch (Exception e) {
            System.out.println("Exception thrown" + e);
            return null;
        }
    }


    public void setMoodSad()
    {
        Mood = MOOD_SAD;
    }

    public void setMoodHappy() {Mood = MOOD_HAPPY;}

    public void setPrintCamera() { printCameraResults = PRINT_CAMERA;}

    public void UnsetPrintCamera() { printCameraResults = NO_PRINT_CAMERA;}

    public int getPrintCamera() { return printCameraResults;}


    private void playAudio() {

        try {
            Thread.sleep(5000);                 // delay by 5000 milliseconds (5 seconds)
        } catch(Exception e) {
        }

        Uri uri = Uri.parse("android.resource://" + this.context.getPackageName() + "/" + R.raw.booksample);
        audioPlayer = audioPlayer.create(this.context,R.raw.booksample1);
        audioPlayer.start();
    }

    private void stopAudio()
    {
        if ((audioPlayer != null)&&(audioPlayer.isPlaying()))
        {
            audioPlayer.stop();
        }
    }
    public void pauseAudio()
    {
        if ((audioPlayer != null)&&(audioPlayer.isPlaying()))
        {
            audioPlayer.pause();
        }
    }

    public void resumeAudio()
    {
        if ((audioPlayer != null)&&(audioPlayer.isPlaying()))
        {
            audioPlayer.start();
        }
    }

    public String sendRequestInternal(String input) {

        try {
            boolean switch_to_normal = false;
            String original_text = "";
            AIResponse response = currentaiService.textRequest(input, new RequestExtras());
            if (response.getResult().getAction().equals("switch_to_help"))
            {
                String newInput = "help";
                currentaiService = aiService_help;
                stopAudio();
                response = currentaiService.textRequest(newInput, new RequestExtras());

                // If all ok delete code below TODO: delete this code once ok
                /*List<AIContext> contexts = new ArrayList<>();
                contexts.add(new AIContext("ext-help"));
                RequestExtras requestExtras = new RequestExtras(contexts, null);
                requestExtras.setContexts(contexts);
                currentaiService.textRequest("help",requestExtras);*/
            }
            if (response.getResult().getAction().equals("switch_to_normal"))
            {
                switch_to_normal = true;
                String newInput = "I'm back";
                original_text = response.getResult().getFulfillment().getSpeech();
                currentaiService = aiService_PodCast;
                response = currentaiService.textRequest(newInput, new RequestExtras());

            }
            // reset agents and go back to podcast agent
            if (response.getResult().getAction().equals("reset-state"))
            {
                currentaiService = aiService_PodCast;
                currentaiService.resetContexts();
                aiService_help.resetContexts(); // do for both agents
                stopAudio();

            }

            // support logic for the podcast agent

            if (currentaiService == aiService_PodCast)
            {
                if (response.getResult().getAction().equals("elder-bored"))
                {
                    switch (Mood)
                    {
                        case MOOD_HAPPY:
                            Result updatedResult = new Result();
                            Fulfillment updatedfullfillment = new Fulfillment();
                            updatedfullfillment.setSpeech("You are always bored my friend... let's start and have some fun!");
                            updatedResult.setFulfillment(updatedfullfillment);
                            response.setResult(updatedResult);
                            break;
                        case MOOD_SAD:
                            break;
                        default:
                            break;
                    }

                }

                if (response.getResult().getAction().equals("play-book"))
                {
                    playAudio();
                }
                if (response.getResult().getAction().equals("stop_book"))
                {
                    stopAudio();
                }
            }
            // suporting logic for the help agent
            if (currentaiService == aiService_help)
            {
                if (response.getResult().getAction().equals("call_skype")) {
                    try {
                    Message message = new Message();
                    Bundle bundle = new Bundle();
                    bundle.putString("message", "skype");
                    message.setData(bundle);
                    message.sendingUid = 2; // TODO replace 2 with enum

                    // this.messageHandler.dispatchMessage(message); //TODO fix this one to be called from async task or UI one to work
                    }
                    catch (Throwable e) {

                        System.out.println(e.toString());
                    }
                }
            }

            if (response.getResult().getAction().equals("ask_wolfram")) {
                return wolframAlphaAPI.sendRequest(response.getResult().getStringParameter("text"));
            } else {
                if (switch_to_normal)
                {
                    return original_text + ", " + response.getResult().getFulfillment().getSpeech();
                }
                else
                {
                    //return response.getResult().getResolvedQuery();
                    return response.getResult().getFulfillment().getSpeech();
                }

            }

        } catch (Exception e) {
            System.out.println("Exception thrown" + e);
            return null;
        }
    }


    class SendRequest extends AsyncTask<String, Void, String> {

        private Exception exception;

        protected String doInBackground(String... input) {
            try {
                return sendRequestInternal(input[0]);
            } catch (Exception e) {
                this.exception = e;
                return null;
            }
        }

        protected void onPostExecute(String output) {
            // TODO: check this.exception
            // TODO: do something with the feed
        }
    }

}
