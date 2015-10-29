package com.robotdreams.api;

import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.Object;
import java.util.List;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Environment;
import android.widget.Toast;
import android.util.Log;

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

    private int photo_num = 0;

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


            detector = new PhotoDetector(this.context);
            // update the license path here if you name your file something else
            detector.setLicensePath("affectiva.license");
            detector.setImageListener(this);
            detector.setFaceListener(this);

            detector.setDetectAllEmotions(true);
        }
        catch (Exception e)
        {
            System.out.println("Init photo detector Affectiva" + e.toString());
        }
    }

    private void startDetector() {
        if (!detector.isRunning()) {
            try {
                detector.start();
            } catch (Exception e) {
                Log.e("app","Affectiva" + e.getMessage());
            }
        }
    }

    private void stopDetector() {
        if (detector.isRunning()) {
            try {
                detector.stop();
            } catch (Exception e) {
                Log.e("app","Affectiva" + e.getMessage());
            }
        }
    }

    public void AffectivaProcessImage(byte[] data, int width, int height)
    {
        new AffectivaAnalyzePictures(data, width, height).start(); // move this to a separated process as the UI one can't hang that long
    }

    public void AffectivaAnalyzePictures (byte[] data, int width, int height)
    {
        Log.d("app","Affectiva thread is" + android.os.Process.myTid());
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if ( photo_num < 10 )
            photo_num++;
        else
            photo_num = 1;

        String file = "affectiva"+photo_num+".jpg";
        File photo=new File(path, file);

        if (photo.exists()) {
            photo.delete();
        }
        try {
            FileOutputStream fos=new FileOutputStream(photo.getPath());

            fos.write(data);
            fos.close();
        }
        catch (java.io.IOException e) {
            Log.e("PictureDemo", "Exception in affectiva analyze pic save to file", e);
        }

        try {
            BitmapFactory.Options opt = new BitmapFactory.Options();
            //opt.inDither = true;
            opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitMap = BitmapFactory.decodeByteArray(data, 0,data.length,opt);
            if (bitMap != null)
            {
                Frame.BitmapFrame frame1 = new Frame.BitmapFrame(bitMap,Frame.COLOR_FORMAT.RGBA);
                //Frame.ByteArrayFrame frame = new Frame.ByteArrayFrame(data,width ,height, Frame.COLOR_FORMAT.UNKNOWN_TYPE ); // TODO get real params
                startDetector();
                detector.process(frame1);
                stopDetector();
                //bitMap.recycle(); do we need to free the bitmap..? or it will cause trouble to affectiva sdk
            }
        }
        catch (Exception e)
        {
            System.out.println("Affectiva process image" + e.toString());
        }
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

        String emotions = "So...";
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

        if ( joy > 0.50 )
            emotions = emotions.concat("joy is above 50%");
        if ( anger > 30 )
            emotions = emotions.concat("anger is above 30%");
        if ( disgust > 50 )
            emotions = emotions.concat("disgust is above 50%");
        //emotions = emotions.concat("joy is" + joy + "anger is" + anger + "disgust is" + disgust);

        // TODO send to "UI" queue
        System.out.println(emotions);

        Message message = new Message();
        Bundle bundle = new Bundle();
        bundle.putString("emotions", emotions);
        message.setData(bundle);
        message.sendingUid = 4; // TODO replace 1 with enum */

        messageHandler.dispatchMessage(message);

    }

    class AffectivaAnalyzePictures extends Thread {

        byte[] data;
        int width;
        int height;

        AffectivaAnalyzePictures(byte[] data, int width, int height) {
            this.data = data;
            this.width = width;
            this.height = height;
        }

        public void run() {
            AffectivaAnalyzePictures(data, width, height);
        }
    }


}
