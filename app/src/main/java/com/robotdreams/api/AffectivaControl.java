package com.robotdreams.api;

import java.io.InputStream;
import java.lang.Object;
import java.util.List;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.detector.CameraDetector;
import com.affectiva.android.affdex.sdk.detector.Detector;
import com.affectiva.android.affdex.sdk.detector.Face;
import com.affectiva.android.affdex.sdk.detector.PhotoDetector;

/**
 * Created by itaimendelsohn on 10/15/15.
 */
public class AffectivaControl implements PhotoDetector.ImageListener, PhotoDetector.FaceListener {

    private Context context;
    private Handler messageHandler;
    private Detector.ImageListener imageListener;
    public CameraDetector cameraDetector;
    public PhotoDetector detector;


    //Camera-related variables in case we switch to camera mode
    private boolean isFrontFacingCameraDetected;
    private boolean isBackFacingCameraDetected;
    int cameraPreviewWidth = 0;
    int cameraPreviewHeight = 0;
    CameraDetector.CameraType cameraType; //placeholder for camera mode in the future
    boolean mirrorPoints = false;


    public AffectivaControl(Context context, boolean frontFacingCam,boolean backFacingCam ) {
        this.context = context;

        this.isFrontFacingCameraDetected = frontFacingCam;
        this.isBackFacingCameraDetected = backFacingCam;

        determineCameraAvailability(); // not really needed in photo mode
        //initializeCameraDetector(); // place holder for camera mode
        InitializePhotoDetector();
    }

    public void setMsgHandler(Handler messageHandler) {this.messageHandler = messageHandler;}

    /**
     * We check to make sure the device has a front-facing camera.
     * If it does not, we obscure the app with a notice informing the user they cannot
     * use the app.
     */
    //TODO: change this one!!
    private void determineCameraAvailability() {

        //TODO: change this to be taken from settings
        if (isBackFacingCameraDetected) {
            cameraType = CameraDetector.CameraType.CAMERA_BACK;
            mirrorPoints = false;
        }
        if (isFrontFacingCameraDetected) {
            cameraType = CameraDetector.CameraType.CAMERA_FRONT;
            mirrorPoints = true;
        }
    }

    private void initializeCameraDetector() {
        /* Put the SDK in phote mode by using this constructor. The SDK will be in control of
         * the camera. If a SurfaceView is passed in as the last argument to the constructor,
         * that view will be painted with what the camera sees.
         */

        cameraDetector = new CameraDetector(this.context, null/*CameraDetector.CameraType.CAMERA_FRONT*/, null/*surface*/);

        // update the license path here if you name your file something else
        cameraDetector.setLicensePath("sdk_itai@intuitionrobotics.com.license");
        cameraDetector.setImageListener(this);
        cameraDetector.setFaceListener(this);
        //cameraDetector.setOnCameraEventListener(this); camera event (size) interface. No need seen now...
    }

    private void InitializePhotoDetector(){
        AssetManager assetMgr = context.getAssets();
        try {
            final InputStream stream = assetMgr.open("affectiva.license");

        }
        catch (Exception e)
        {

        }

        detector = new PhotoDetector(this.context);
        // update the license path here if you name your file something else
        detector.setLicensePath("affectiva.license");
        detector.setImageListener(this);
        detector.setFaceListener(this);

        detector.setDetectAllEmotions(true);
    }

    public void AffectivaProcessImage(byte[] data)
    {
        Frame.ByteArrayFrame frame = new Frame.ByteArrayFrame(data, 200, 400, Frame.COLOR_FORMAT.RGBA ); // TODO get real params
        detector.process(frame);
    }

    @Override
    public void onFaceDetectionStarted()
    {
        // just for debug
        String emotions = "face detected";
        System.out.println(emotions);

        Message message = new Message();
        Bundle bundle = new Bundle();
        bundle.putString("emotions", emotions);
        message.setData(bundle);
        message.sendingUid = 4; // TODO replace 1 with enum */

        messageHandler.dispatchMessage(message);

    }

    @Override
    public void onFaceDetectionStopped()
    {
        // just for debug
        String emotions = "face detection stopped";
        System.out.println(emotions);

        Message message = new Message();
        Bundle bundle = new Bundle();
        bundle.putString("emotions", emotions);
        message.setData(bundle);
        message.sendingUid = 4; // TODO replace 1 with enum */

        messageHandler.dispatchMessage(message);

    }
    @Override
    public void onImageResults(List<Face> faces, Frame frame, float timestamp)
    {
        if (faces == null)
            return; //frame was not processed
        if (faces.size() == 0)
            return; //no face found

        String emotions = "";
        Face face = faces.get(0); //Currently, the SDK only detects one face at a time
//Some Emotions
        float joy = face.emotions.getJoy();
        float anger = face.emotions.getAnger();
        float disgust = face.emotions.getDisgust();
//Some Expressions
        float smile = face.expressions.getSmile();
        float brow_furrow = face.expressions.getBrowFurrow();
        float brow_raise = face.expressions.getBrowRaise();
//Measurements
        float interocular_distance = face.measurements.getInterocularDistance();
        float yaw = face.measurements.orientation.getYaw();
        float roll = face.measurements.orientation.getRoll();
        float pitch = face.measurements.orientation.getPitch();

        /*if ( joy < 50 )
            emotions.concat("joy");
        if ( anger < 50 )
            emotions.concat("anger");
        if ( disgust < 50 )
            emotions.concat("disgust"); for later*/
        emotions.concat("joy is" + joy + "anger is" + anger + "disgust is" + disgust);

        // TODO send to "UI" queue
        System.out.println(emotions);

        Message message = new Message();
        Bundle bundle = new Bundle();
        bundle.putString("emotions", emotions);
        message.setData(bundle);
        message.sendingUid = 4; // TODO replace 1 with enum */

        messageHandler.dispatchMessage(message);

    }
}
