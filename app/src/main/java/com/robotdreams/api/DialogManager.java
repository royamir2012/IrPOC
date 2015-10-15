package com.robotdreams.api;

import java.util.*;

import ai.api.model.*;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;


import ai.api.AIConfiguration;
import ai.api.AIListener;
import ai.api.AIService;
import ai.api.RequestExtras;

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

    private static final int ATTITUDE_CYNIC = 1;
    private static final int ATTITUDE_DISTANT = 2;
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
    public AudioControl audioControl;
    //
    public skypeControl skypeControl;
    //
    private Context context;
    //
    private Handler messageHandler;

    //
    private WolframAlphaAPI wolframAlphaAPI;

    //
    private int Attitude;
    private int printCameraResults;
    private boolean helpFromCamera;


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

        Attitude = ATTITUDE_DISTANT;
        printCameraResults = NO_PRINT_CAMERA;
        audioControl = new AudioControl(context);
        skypeControl = new skypeControl(context);
        skypeControl.setNameToCall("Betty"); // default is Betty
        helpFromCamera = false;
    }


    public String sendRequest(String input) {

        try {
            return new SendRequest().execute(input).get();
        } catch (Exception e) {
            System.out.println("Exception thrown" + e);
            return null;
        }
    }


    public void setAttitudeDistant()
    {
        Attitude = ATTITUDE_CYNIC;
    }

    public void setAttitudeCynic() {
        Attitude = ATTITUDE_DISTANT;}

    public void setPrintCamera() { printCameraResults = PRINT_CAMERA;}

    public void UnsetPrintCamera() { printCameraResults = NO_PRINT_CAMERA;}

    public int getPrintCamera() { return printCameraResults;}

    public void setHelpFromCamera() {helpFromCamera = true;}

    public void unSetHelpFromCamera() {helpFromCamera = false;}

    private Result updateFulfillment (String newText)
    {
        Result updatedResult = new Result();
        Fulfillment updatedfulfillment = new Fulfillment();
        updatedfulfillment.setSpeech(newText);
        updatedResult.setFulfillment(updatedfulfillment);
        return updatedResult;
    }
    private RequestExtras updateAIContext() // TODO need to debug this code...
    {
        List<AIContext> contexts = new ArrayList<>();
        contexts.add(new AIContext("playing grisham"));
        contexts.add(new AIContext("startpodcast"));
        contexts.add(new AIContext("play grisham"));
        RequestExtras requestExtras = new RequestExtras(contexts, null);
        requestExtras.setContexts(contexts);
        return requestExtras;
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
                audioControl.pauseAudio(); // TODO what to do if never comes back...
                response = currentaiService.textRequest(newInput, new RequestExtras());
            }
            if (response.getResult().getAction().equals("switch_to_normal"))
            {
                switch_to_normal = true;
                String newInput = "I'm back";
                original_text = response.getResult().getFulfillment().getSpeech();
                currentaiService = aiService_PodCast;
                /*if (audioControl.isPaused())
                {
                    RequestExtras requestExtras = updateAIContext(); //TODO is the allocation for requestExtras correct?
                    response = currentaiService.textRequest(newInput,requestExtras);
                }
                else*/ //TODO not sure this code is needed
                    response = currentaiService.textRequest(newInput, new RequestExtras());
            }
            // reset agents and go back to podcast agent
            if (response.getResult().getAction().equals("reset-state"))
            {
                currentaiService = aiService_PodCast;
                currentaiService.resetContexts();
                aiService_help.resetContexts(); // do for both agents
                audioControl.stopAudio();

            }

            // support logic for the podcast agent

            if (currentaiService == aiService_PodCast)
            {
                if (response.getResult().getAction().equals("elder-bored"))
                {
                    switch (Attitude)
                    {
                        case ATTITUDE_DISTANT:
                            response.setResult(updateFulfillment("You are always bored my friend... let's start and have some fun!"));
                            break;
                        case ATTITUDE_CYNIC:
                            break;
                        default:
                            break;
                    }

                }

                if (response.getResult().getAction().equals("back_to_audio"))
                {
                    if (!helpFromCamera)
                        original_text = "Glad to hear that!"; // We want to control the output based on the conversation flow. To overall the "eyes checked" one that is not relevant
                    if (audioControl.isPaused())
                    {
                        response.setResult(updateFulfillment("I will continue playing the book"));
                        audioControl.resumeAudio();
                    }
                }

                if (response.getResult().getAction().equals("play-book"))
                {
                    audioControl.playAudio();
                }
                if (response.getResult().getAction().equals("stop_book"))
                {
                    audioControl.stopAudio();
                }
                if (response.getResult().getAction().equals("pause_book"))
                {
                    audioControl.pauseAudio();
                }
            }
            // suporting logic for the help agent
            if (currentaiService == aiService_help)
            {
                if (response.getResult().getAction().equals("call_skype")) {
                    skypeControl.startSkype();
                }
                if (response.getResult().getAction().equals("call"))
                {
                    response.setResult(updateFulfillment("Shall I call " + skypeControl.getNameToCall() + ", your care giver?"));
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
