package com.robotdreams.api;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;

import ai.api.AIConfiguration;
import ai.api.AIListener;
import ai.api.AIService;
import ai.api.RequestExtras;
import ai.api.model.AIResponse;

/**
 *
 */
public class DialogManager {


    private static final String CLIENT_ACCESS_TOKEN = "38ecf01bfbd74b9a92ae6569932e4db6";
    private static final String SUBSCRIPTION_KEY = "0a802526-a4bf-4eb2-b03e-2b81f2061121";


    //
    private AIService aiService;

    //
    private WolframAlphaAPI wolframAlphaAPI;

    //
    private Context context;

    //
    private Handler messageHandler;


    public DialogManager(Context context, AIListener listener, Handler messageHandler) {

        //
        final AIConfiguration config = new AIConfiguration(CLIENT_ACCESS_TOKEN,
                SUBSCRIPTION_KEY,
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        aiService = AIService.getService(context, config);
        aiService.setListener(listener);

        wolframAlphaAPI = new WolframAlphaAPI();

        this.context = context;
        this.messageHandler = messageHandler;
    }


    public String sendRequest(String input) {

        try {
            return new SendRequest().execute(input).get();
        } catch (Exception e) {
            System.out.println("Exception thrown" + e);
            return null;
        }
    }


    public String sendRequestInternal(String input) {

        try {
            AIResponse response = aiService.textRequest(input, new RequestExtras());

            if (response.getResult().getAction().equals("ask_wolfram")) {
                return wolframAlphaAPI.sendRequest(response.getResult().getStringParameter("text"));
            } else {
                return response.getResult().getResolvedQuery();
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
