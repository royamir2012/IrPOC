package com.robotdreams.api;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.IOException;

/**
 * Created by assaf_000 on 10/10/2015.
 */
public class CameraControl implements SurfaceHolder.Callback {
    Camera camera;
    volatile boolean running = false;

    Camera.PictureCallback rawCallback;
    Camera.ShutterCallback shutterCallback;
    Camera.PictureCallback jpegCallback;

    SurfaceHolder surfaceHolder;

    TakePictures takePictures;
    CamFindRestClient camFindClient = new CamFindRestClient();
    Context context;

    public void init(final Context context, SurfaceView surfaceView) {

        this.context = context;
        this.running = true;

        this.surfaceHolder = surfaceView.getHolder();

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        surfaceHolder.addCallback(this);

        jpegCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                Toast.makeText(context, "Picture Taken", Toast.LENGTH_SHORT).show();
                new HandleImage(data).start();
                refreshCamera();
            }
        };

        takePictures = new TakePictures();
        takePictures.start();
    }

    public void captureImage() throws IOException {
        if (camera != null) {
            //take the picture
            camera.takePicture(null, null, jpegCallback);
        }
    }

    public void handleImage(byte[] data) {

        try {

            String name = camFindClient.imageRequest(context, data);
            System.out.println(name);

/*
            FileOutputStream outStream = null;
            try {
                outStream = new FileOutputStream(String.format("/sdcard/%d.jpg", System.currentTimeMillis()));
                outStream.write(data);
                outStream.close();
                Log.d("Log", "onPictureTaken - wrote bytes: " + data.length);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
*/
        } catch (Throwable e) {

            System.out.println(e.toString());
        }
    }

    public void refreshCamera() {
        if (surfaceHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            camera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        refreshCamera();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // open the camera
            camera = Camera.open();
        } catch (RuntimeException e) {
            // check for exceptions
            System.err.println(e);
            return;
        }
        Camera.Parameters param;
        param = camera.getParameters();

        // modify parameter
/*
        param.setPreviewSize(20, 20);
        param.setJpegQuality(60);
*/
        camera.setParameters(param);
        try {
            // The Surface has been created, now tell the camera where to draw
            // the preview.
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
            // check for exceptions
            System.err.println(e);
            return;
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        this.running = false;
        try {
            takePictures.join();
        } catch (Exception e) {

        }
        // stop preview and release camera
        camera.stopPreview();
        camera.release();
        camera = null;
    }


    class TakePictures extends Thread {

        private Exception exception;

        public void run() {
            while (running) {
                try {
                    captureImage();
                    Thread.sleep(30000);
                } catch (Exception e) {
                    this.exception = e;
                }
            }
        }
    }

    class HandleImage extends Thread {

        byte[] data;

        HandleImage(byte[] data) {
            this.data = data;
        }

        private Exception exception;

        public void run() {
            handleImage(data);
        }
    }
}

