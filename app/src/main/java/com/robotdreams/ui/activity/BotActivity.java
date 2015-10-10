package com.robotdreams.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
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
import android.widget.Toast;
import android.widget.RadioButton;

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
import android.app.AlertDialog;
/**
 *
 */
public class BotActivity extends BaseActivity implements SendRequestButton.OnSendClickListener, AIListener {
    public static final String ARG_DRAWING_START_LOCATION = "arg_drawing_start_location";
    private static final int REQ_CODE_SPEECH_INPUT = 100;


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

    private TextToSpeech tts;

    private BotAdapter botAdapter;
    private int drawingStartLocation;
    private DialogManager dialogManager;
    private CameraControl cameraControl;

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

        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);

        cameraControl = new CameraControl(messageHandler);
        cameraControl.init(getApplicationContext(), surfaceView);

        dialogManager = new DialogManager(this, this, messageHandler);

        dialogManager.setMoodHappy(); // happy

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

    /**
     * Showing google speech input dialog
     */
    private void promptSpeechInput() {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
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
            if (comment.equals("Calling")) // let's try and open Skype...
            {
                callSkype();
            }
            else
                tts.speak(comment, TextToSpeech.QUEUE_FLUSH, null, "1");
        }
    }

    private void callSkype()
    {
        final String [] whotocall = {"amiruk2004","danielle.mendelsohn","dorinphilly"};
        final String [] whotocallnames = {"Dave", "Betty","Dor"};
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // 2. Chain together various setter methods to set the dialog characteristics

        builder.setTitle("Who Should I call?")
                .setItems(whotocallnames, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        String skypeName = whotocall[which];
                        String mySkypeUri = "skype:" + skypeName + "?call&video=true";
                        String callingStr = "calling"+whotocallnames[which];

                        Uri skypeUri = Uri.parse(mySkypeUri);
                        Intent myIntent = new Intent(Intent.ACTION_VIEW, skypeUri);
                        myIntent.setComponent(new ComponentName("com.skype.raider", "com.skype.raider.Main"));
                        myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(myIntent);

                        tts.speak(callingStr, TextToSpeech.QUEUE_FLUSH, null, "1");

                    }
                });
        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        dialog.show();
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
        switch(view.getId()) {
            case R.id.HappyradioButton:
                if (checked)
                    dialogManager.setMoodHappy();// MOOD_SAD
                    break;
            case R.id.SadradioButton:
                if (checked)
                    dialogManager.setMoodSad();
                    break;
        }
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

        appendComment(BotAdapter.Type.Camera, msg.getData().getString("message"));
    }

}
