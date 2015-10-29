package com.robotdreams.api;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;
import android.util.Log;

import java.io.IOException;
import java.util.List;

/**
 * Created by assaf_000 on 10/10/2015.
 * Changed to support (control) up to two cameras (Itai) with a LOT!! of ugly assumptions mainly around each camera purpose
 */

public class CamerasControl implements SurfaceHolder.Callback {

    public static final int CONTROL_CAM_FIND = 1;
    public static final int CONTROL_AFFECTIVA = 2;

    Camera camera1=null;
    Camera camera2=null;
    int cam1purpose = 0;
    int cam2purpose = 0;
    int mCam1Width;
    int mCam1Height;
    int mCam2Width;
    int mCam2Height;

    volatile boolean running = false;
    volatile boolean takePictures = false;
    int picPurpose = 0;

    Camera.PictureCallback jpegCallback1;
    Camera.PictureCallback jpegCallback2;
    //
    public AffectivaControl affectivaControl;
    private int affectivaPics = 0;

    SurfaceHolder surfaceHolder;

    TakePictures takePicturesThread;
    CamFindRestClient camFindClient = new CamFindRestClient();
    Context context;

    Handler messageHandler;

    //private int controlWhom;

    public CamerasControl(Handler messageHandler, int whom) {

        this.messageHandler = messageHandler;
    }

    public void init(final Context context, SurfaceView surfaceView) {

        this.context = context;
        this.running = true;

        this.surfaceHolder = surfaceView.getHolder();
        initCameras(); // not sure if both (surfacecreated) and here are always called in same sequence

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        surfaceHolder.addCallback(this);

        jpegCallback1 = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                Toast.makeText(context, "Picture Taken", Toast.LENGTH_SHORT).show();
                new HandleImage(data, cam1purpose).start();
                refreshCamera();
            }
        };

        jpegCallback2 = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                Toast.makeText(context, "Picture Taken", Toast.LENGTH_SHORT).show();
                new HandleImage(data, cam2purpose).start();
                refreshCamera();
            }
        };
        boolean frontCam = false;
        boolean backCam = false;
        if (cam1purpose !=0 )
            backCam = true;
        if (cam2purpose != 0)
            frontCam = true;
        affectivaControl = new AffectivaControl(context, frontCam,backCam);
        affectivaControl.setMsgHandler(this.messageHandler);
        affectivaPics = 0;
        takePicturesThread = new TakePictures();
        takePicturesThread.start();
    }

    private Camera.Parameters setImageSizes(int purpose, Camera.Parameters params)
    {
        List sizes = params.getSupportedPictureSizes();

        if (purpose == CONTROL_AFFECTIVA )
        {
            Camera.Size camSize;
            for ( int i = 0; i < sizes.size(); i++)
            {
                camSize = (Camera.Size) sizes.get(i);

                if ( camSize.width == camSize.height) // for now we assume Affective needs a square
                {
                    mCam2Height = camSize.height; // Affectiva uses cam2Control
                    mCam2Width = camSize.width;
                    break;
                }
            }
            params.setPictureSize(mCam2Width, mCam2Height); // w , h
            params.setPreviewSize(mCam2Width, mCam2Height);
        }
        if (purpose == CONTROL_CAM_FIND) // we are looking for the smallest size for Cam Find
        {
            Camera.Size camSize1 = (Camera.Size) sizes.get(0); // first in the list
            Camera.Size camSize2 = (Camera.Size) sizes.get(sizes.size()-1); // last one in the list
            if (camSize1.height < camSize2.height) { // first is the smallest
                mCam1Height = camSize1.height;
                mCam1Width = camSize1.width;
            }
            else {
                mCam1Height = camSize2.height;
                mCam1Width = camSize2.width;
            }
            params.setPictureSize(mCam1Width, mCam1Height); // w , h
            params.setPreviewSize(mCam1Width, mCam1Height);
        }

        return params;
}

    private void initCameras() {
        // open the cameras and define usage
        try {

            int numOfCameras = Camera.getNumberOfCameras();
            int cameraTouse;
            Camera camera; //local just to get hw info
            if (camera1 == null && camera2 == null) // not initilaized yed
            {
                for (cameraTouse = 0; cameraTouse < numOfCameras; cameraTouse++) {
                    CameraInfo cameraInfo = new CameraInfo();
                    Camera.Parameters param;
                    camera = Camera.open(cameraTouse);
                    camera.getCameraInfo(cameraTouse, cameraInfo);
                    camera.release();

                    if (cameraInfo.facing == cameraInfo.CAMERA_FACING_BACK) {
                        cam1purpose = CONTROL_CAM_FIND; // for now back camera goes to cam find
                        camera1 = Camera.open(cameraTouse);
                        param = camera1.getParameters();
                        param = setImageSizes(CONTROL_CAM_FIND,param); //set image sizes
                        // TODO modify parameter
                    /*
                    param.setJpegQuality(60);
                    */
                        camera1.setParameters(param);
                    }
                    if (cameraInfo.facing == cameraInfo.CAMERA_FACING_FRONT) {
                        cam2purpose = CONTROL_AFFECTIVA; // for now front camera goes to affectiva
                        camera2 = Camera.open(cameraTouse);
                        param = camera2.getParameters();
                        param = setImageSizes(CONTROL_AFFECTIVA,param); //set image sizes
                        List formats = camera2.getParameters().getSupportedPictureFormats();
                        for (int i =0; i < formats.size();i++)
                        {
                            if (formats.get(i).equals(ImageFormat.NV21))
                                param.setPictureFormat(ImageFormat.NV21);
                        }
                        param.setRotation(270);
                        camera2.setDisplayOrientation(270);
                        //param.setPictureFormat(ImageFormat.NV21);

                        // TODO modify parameter
                    /*
                    param.setPreviewSize(20, 20);
                    param.setJpegQuality(60);
                    */
                        camera2.setParameters(param);
                    }
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("camera Init failed" + e.toString());
        }
    }

    public void takePictures(int purpose) {
        takePictures = true;
        this.picPurpose = purpose;
    }

    public void captureImage(int purpose) throws IOException {
        Camera camera = this.getRelevantCamera(purpose);
        Camera.PictureCallback jpegCallback = this.getRelevantCallBack(purpose);
        if (camera != null) {
            //take the picture
            camera.takePicture(null, null, jpegCallback);
        }
    }

    public void handleImage(byte[] data, int purpose) {

        try {

            if ( purpose == CONTROL_CAM_FIND)
            {
                String name = camFindClient.imageRequest(context, data);
                System.out.println(name);

                Message message = new Message();
                Bundle bundle = new Bundle();
                bundle.putString("message", name);
                message.setData(bundle);
                message.sendingUid = 1; // TODO replace 1 with enum

                messageHandler.dispatchMessage(message);
            }
            if (purpose == CONTROL_AFFECTIVA)
            {
                affectivaControl.AffectivaProcessImage(data, mCam2Width, mCam2Height); // affectiva uses cam2
                Log.d("app", "Affectiva process"+affectivaPics);
                affectivaPics--;
                //if (affectivaPics == 0)
                  //  affectivaControl.detector.stop();
            }

        } catch (Throwable e) {

            System.out.println("Handle image failed" + e.toString());
        }
    }

    public void refreshCamera() { // for now we assume one camera at a time is being used!!
        if (surfaceHolder.getSurface() == null) {
            // preview surface does not exist
            System.out.println("preview surface does not exist");
            return;
        }

        // stop preview before making changes
        try {
            if (camera1 != null)
                camera1.stopPreview();
            if ( camera2 != null)
                camera2.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
            System.out.println("Stop preview failed in refresh camera" + e.toString());
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        try {
            if ( camera1 != null)
            {
                camera1.setPreviewDisplay(surfaceHolder);
                camera1.startPreview();
            }
            if ( camera2 != null)
            {
                camera2.setPreviewDisplay(surfaceHolder);
                camera2.startPreview();
            }

        } catch (Exception e) {
            System.out.println("Refresh camera failed set preview" + e.toString());
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        refreshCamera();
    }

    private Camera getRelevantCamera(int purpose)
    {
        if (cam1purpose == purpose)
            return camera1;
        if (cam2purpose == purpose)
            return camera2;
        return null;
    }

    private Camera.PictureCallback getRelevantCallBack(int purpose)
    {
        if (cam1purpose == purpose)
            return jpegCallback1;
        if (cam2purpose == purpose)
            return jpegCallback2;
        return null;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            initCameras();
        } catch (RuntimeException e) {
            // check for exceptions
            System.err.println("init cameras in surface created" + e);
            return;
        }

        try {
            // The Surface has been created, now tell the camera where to draw
            // the preview.
            if ( camera1 != null)
            {
                camera1.setPreviewDisplay(holder);
                camera1.startPreview();
            }
            if ( camera2 != null)
            {
                camera2.setPreviewDisplay(holder);
                if (camera1 == null) // if not called in cam1
                    camera2.startPreview(); // do I need both startPreview when using the same surface?
            }
        } catch (Exception e) {
            // check for exceptions
            System.err.println("set preview in surface created" + e);
            return;
        }

    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        this.running = false;
        try {
            takePicturesThread.join();
        } catch (Exception e) {
            System.out.println("Surface destroyed" + e.toString());
        }
        // stop preview and release camera
        if (camera1 != null)
        {
            camera1.stopPreview();
            camera1.release();
            camera1 = null;
            cam1purpose = 0;
        }
        if (camera2 != null)
        {
            camera2.stopPreview();
            camera2.release();
            camera2 = null;
            cam2purpose = 0;
        }
    }


    class TakePictures extends Thread {

        public void run() {

            while (running) {

                while (running && (camera1 == null) && (camera2 ==null)) { // nothing to do yet
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        System.out.println("take pictures failed" + e.toString());
                    }
                }

                if (takePictures) {
                    if ( picPurpose == CONTROL_CAM_FIND)
                        if (camera1 != null)
                        {
                            if (cam1purpose == CONTROL_CAM_FIND) // for now cam1 goes to camfind
                            {
                                for (int i = 0; running && i < 2; i++) {
                                    try {
                                        captureImage(CONTROL_CAM_FIND);
                                        Thread.sleep(10000);
                                    } catch (Exception e) {
                                        System.out.println("take pictures cam1" + e.toString());
                                    }
                                }
                            }
                        }
                    if ( picPurpose == CONTROL_AFFECTIVA)
                        if (camera2 != null)
                        {
                            if (cam2purpose == CONTROL_AFFECTIVA)
                            {

                                //affectivaControl.detector.start();
                                for (int i = 0; running && i < 1; i++) {
                                    try {
                                        captureImage(CONTROL_AFFECTIVA);
                                        affectivaPics++;
                                        Thread.sleep(200);
                                    } catch (Exception e) {
                                        System.out.println("take pictures cam2" + e.toString());
                                    }
                                }
                            }
                        }
                    takePictures = false;
                }
            }

            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }
    }

    class HandleImage extends Thread {

        byte[] data;
        int purpose;

        HandleImage(byte[] data, int purpose) {
            this.data = data;
            this.purpose = purpose;
        }

        public void run() {
            handleImage(data, purpose);
        }
    }
}
