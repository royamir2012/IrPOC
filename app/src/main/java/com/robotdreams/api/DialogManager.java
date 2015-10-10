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

    private static final String CLIENT_ACCESS_TOKEN_FAIL = "8d055613884d4b49acb6726195b78968";
    private static final String SUBSCRIPTION_KEY_FAIL = "dc39a5a2-0ab8-4ac4-bb7b-67099d8c6064";


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
        final AIConfiguration config = new AIConfiguration(CLIENT_ACCESS_TOKEN_FAIL,
                SUBSCRIPTION_KEY_FAIL,
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
                //return response.getResult().getResolvedQuery();
                return response.getResult().getFulfillment().getSpeech();
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
