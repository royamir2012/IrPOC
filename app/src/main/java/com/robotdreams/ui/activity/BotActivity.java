package com.robotdreams.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ToggleButton;

import com.google.gson.JsonElement;
import com.robotdreams.R;
import com.robotdreams.api.CameraControl;
import com.robotdreams.api.DialogManager;
import com.robotdreams.ui.adapter.BotAdapter;
import com.robotdreams.ui.view.SendRequestButton;
import com.robotdreams.utils.Utils;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import ai.api.AIConfiguration;
import ai.api.AIListener;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import butterknife.InjectView;

/**
 *
 */
public class BotActivity extends BaseActivity implements SendRequestButton.OnSendClickListener, AIListener {
    public static final String ARG_DRAWING_START_LOCATION = "arg_drawing_start_location";
    private static final int REQ_CODE_SPEECH_INPUT = 100;
    private static final String TAG = "BotActivity";

    @InjectView(R.id.contentRoot)
    LinearLayout contentRoot;
    @InjectView(R.id.rvComments)
    RecyclerView rvComments;
    @InjectView(R.id.llAddComment)
    LinearLayout llAddComment;
    @InjectView(R.id.etComment)
    EditText etComment;
    @InjectView(R.id.btnSendComment)
    SendRequestButton btnSendComment;
    @InjectView(R.id.btnSpeak)
    ImageButton btnSpeak;
    @InjectView(R.id.btnCamera)
    ImageButton btnCamera;
    @InjectView(R.id.imageToggleButton)
    ToggleButton imagebtn;


    private TextToSpeech tts;
    private SpeechRecognizer sr;

    private BotAdapter botAdapter;
    private int drawingStartLocation;
    private DialogManager dialogManager;
    private CameraControl cameraControl;
    private String lastpicture;

    private Handler messageHandler = new Handler() {
        public void handleMessage(Message msg) {
            onHandleMessage(msg);
        }
    };


    SurfaceView surfaceView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bot);
        setupComments();
        setupSpeakButton();
        setupCameraButton();
        setupSendCommentButton();
        setupImageDemoButton();

        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);

        cameraControl = new CameraControl(messageHandler);
        cameraControl.init(getApplicationContext(), surfaceView);
        lastpicture = null;

        dialogManager = new DialogManager(this, this, messageHandler);

        dialogManager.setAttitudeCynic(); // happy

        drawingStartLocation = getIntent().getIntExtra(ARG_DRAWING_START_LOCATION, 0);
        if (savedInstanceState == null) {
            contentRoot.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    contentRoot.getViewTreeObserver().removeOnPreDrawListener(this);
                    startIntroAnimation();
                    return true;
                }
            });
        }

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.US);
                }
            }
        });

        dialogManager.skypeControl.setTts(tts);

        sr = SpeechRecognizer.createSpeechRecognizer(this);
        sr.setRecognitionListener(new STTListener());

        final AIConfiguration config = new AIConfiguration("CLIENT_ACCESS_TOKEN_FAIL",
                "SUBSCRIPTION_KEY_FAIL",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setupComments() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvComments.setLayoutManager(linearLayoutManager);
        rvComments.setHasFixedSize(true);

        botAdapter = new BotAdapter(this);
        rvComments.setAdapter(botAdapter);
        rvComments.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rvComments.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    botAdapter.setAnimationsLocked(true);
                }
            }
        });
    }

    private void setupSendCommentButton() {
        btnSendComment.setOnSendClickListener(this);
    }

    private void setupSpeakButton() {
        btnSpeak.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });
    }

    private void setupCameraButton() {
        btnCamera.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                cameraControl.takePictures();
            }
        });
    }

    private void setupImageDemoButton()
    {
        CharSequence txtoff = "Image demo off";
        CharSequence txton = "Image demo on";
        txtoff.toString().toLowerCase();
        txton.toString().toLowerCase();
        imagebtn.setTextOff(txtoff);
        imagebtn.setTextOn(txton);
    }
    /**
     * Showing google speech input dialog
     */
    private void promptSpeechInput() {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.robotdreams.ui.activity");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt));
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,2000);

        sr.startListening(intent);
    }

    private void startIntroAnimation() {
        contentRoot.setScaleY(0.1f);
        contentRoot.setPivotY(drawingStartLocation);
        llAddComment.setTranslationY(200);

        contentRoot.animate()
                .scaleY(1)
                .setDuration(200)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        animateContent();
                    }
                })
                .start();
    }

    private void animateContent() {
        botAdapter.updateItems();
        llAddComment.animate().translationY(0)
                .setInterpolator(new DecelerateInterpolator())
                .setDuration(200)
                .start();
    }


    @Override
    public void onBackPressed() {
        contentRoot.animate()
                .translationY(Utils.getScreenHeight(this))
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        BotActivity.super.onBackPressed();
                        overridePendingTransition(0, 0);
                    }
                })
                .start();
    }

    @Override
    public void onSendClickListener(View v) {

        sendComment();
    }

    private void sendComment() {

        String comment = getComment();
        String userinput = getUserInput();

        if (userinput != null) {

            appendComment(BotAdapter.Type.Voice, userinput);
            etComment.setText(null);
            btnSendComment.setCurrentState(SendRequestButton.STATE_DONE);

        }

        if (comment != null) {

            appendComment(BotAdapter.Type.Voice, comment);
            etComment.setText(null);
            btnSendComment.setCurrentState(SendRequestButton.STATE_DONE);
            /*if (comment.equals("Calling")) // let's try and open Skype...
            {
                callSkype();
            } else*/ // TODO remove after fix message
                tts.speak(comment, TextToSpeech.QUEUE_FLUSH, null, "1");
        }

    }

    private String getComment() {

        String comment = etComment.getText().toString();
        if (TextUtils.isEmpty(comment)) {
            btnSendComment.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_error));
            return null;
        }

        comment = dialogManager.sendRequest(comment);

        if (TextUtils.isEmpty(comment)) {
            btnSendComment.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_error));
            return null;
        }

        return comment;
    }

    private String getUserInput() {

        String userInput = etComment.getText().toString();
        if (TextUtils.isEmpty(userInput)) {
            btnSendComment.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_error));
            return null;
        }


        return userInput;
    }

    private void emulateSendButtonClick() // Not used at the moment
    {
        View v = findViewById(R.id.btnSendComment);
        v.performClick();
    }
    /**
     * Receiving speech input
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    etComment.setText(result.get(0));
                    //emulateSendButtonClick(); // not necessary to avoid pressing the send button
                }
                break;
            }
        }
    }

    @Override
    public void onError(final AIError error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                System.out.println(error.toString());
            }
        });
    }

    public void onResult(final AIResponse response) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Result result = response.getResult();

                // Get parameters
                String parameterString = "";
                if (result.getParameters() != null && !result.getParameters().isEmpty()) {
                    for (final Map.Entry<String, JsonElement> entry : result.getParameters().entrySet()) {
                        parameterString += "(" + entry.getKey() + ", " + entry.getValue() + ") ";
                    }
                }

                // Print results
                System.out.println("Query:" + result.getResolvedQuery() +
                        "\nAction: " + result.getAction() +
                        "\nParameters: " + parameterString);

            }
        });
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.CynicradioButton:
                if (checked)
                    dialogManager.setAttitudeCynic();
                break;
            case R.id.DistantradioButton:
                if (checked)
                    dialogManager.setAttitudeDistant();
                break;
        }
    }

    public void onImageButtonClicked (View view)
    {
        boolean checked = ((ToggleButton) view).isChecked();
        if (checked)
            dialogManager.setPrintCamera();
        else
            dialogManager.UnsetPrintCamera();
    }

    public void onClickWhoToCall (View view)
    {
        final String[] whotocallnames = {"Dave", "Betty", "Dor"};

        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setTitle("Who Should I call?")
                .setItems(whotocallnames, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        dialogManager.skypeControl.setWhich(which);
                        dialogManager.skypeControl.setNameToCall(whotocallnames[which]);
                    }
                });
        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        dialog.show();

    }


    @Override
    public void onListeningStarted() {
        // show recording indicator
    }

    @Override
    public void onListeningCanceled() {
        // hide recording indicator
    }

    @Override
    public void onListeningFinished() {
        // hide recording indicator
    }

    @Override
    public void onAudioLevel(final float level) {
        // show sound level
    }

    private void appendComment(BotAdapter.Type type, String comment) {

        botAdapter.addItem(type, comment);
        botAdapter.setAnimationsLocked(false);
        botAdapter.setDelayEnterAnimation(false);

        if (rvComments.getChildCount() > 0) {
            rvComments.smoothScrollBy(0, rvComments.getChildAt(0).getHeight() * botAdapter.getItemCount());
        }

    }

    public void onHandleMessage(Message msg) {


        if (msg.sendingUid == 1) // TODO replace 1 with enum
        {
            if (dialogManager.getPrintCamera() == dialogManager.PRINT_CAMERA)
                appendComment(BotAdapter.Type.Camera, msg.getData().getString("message"));
            else // do all this only if no camera demo
            {
                if (lastpicture != null) // relevant on every second picture asumess 2 pictures per round!!
                {
                    if (lastpicture.equals(msg.getData().getString("message"))) // no change
                    {
                        appendComment(BotAdapter.Type.Camera, msg.getData().getString("message") + "no change");
                    } else // this one reflects emergency - so switch to help
                    {
                        appendComment(BotAdapter.Type.Camera, "change -> help");
                        tts.speak("Please say help if help is needed", TextToSpeech.QUEUE_FLUSH, null, "1");
                        dialogManager.setHelpFromCamera();
                    }

                    lastpicture = null; // for next round of two TODO handle reset event
                } else // first picture in round
                {
                    lastpicture = msg.getData().getString("message");
                }
            }
        }
        if (msg.sendingUid == 2) // TODO replace 2
        {
            // place holder
        }
        if (msg.sendingUid == 3) // TODO replace 3
            lastpicture = null;

    }

    class STTListener implements RecognitionListener {
        public void onReadyForSpeech(Bundle params) {
        }

        public void onBeginningOfSpeech() {
            dialogManager.audioControl.pauseAudioForInput();
        }

        public void onRmsChanged(float rmsdB) {
        }

        public void onBufferReceived(byte[] buffer) {
        }

        public void onEndOfSpeech() {
                dialogManager.audioControl.resumeAudioFromInputPause();
        }

        public void onError(int error) {
        }

        public void onResults(Bundle results) {
            ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            /*// The confidence array
            float[] confidence = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);

            // The confidence results
            for (int i = 0; i < confidence.length; i++) {
                if ( confidence[i] < 0.5)
                    break;
            }*/ // TODO explore this one...
            if (data != null && !data.isEmpty()) {
                etComment.setText(data.get(0));
                sendComment();
            }
        }

        public void onPartialResults(Bundle partialResults) {
        }

        public void onEvent(int eventType, Bundle params) {
        }
    }
}
