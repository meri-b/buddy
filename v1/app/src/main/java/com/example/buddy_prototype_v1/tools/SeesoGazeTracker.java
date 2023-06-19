package com.example.buddy_prototype_v1.tools;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.TextureView;

import androidx.core.content.ContextCompat;

import com.example.buddy_prototype_v1.configure.CalibrationDataStorage;
import com.example.buddy_prototype_v1.R;

import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.callback.CalibrationCallback;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.callback.InitializationCallback;
import camp.visual.gazetracker.constant.AccuracyCriteria;
import camp.visual.gazetracker.constant.CalibrationModeType;
import camp.visual.gazetracker.constant.InitializationErrorType;
import camp.visual.gazetracker.device.CameraPosition;
import camp.visual.gazetracker.filter.OneEuroFilterManager;
import camp.visual.gazetracker.gaze.GazeInfo;

public class SeesoGazeTracker {

    GazeTracker gazeTracker = null;
    private OneEuroFilterManager oneEuroFilterManager = new OneEuroFilterManager(2);
    InitializationCallback initializationCallback;
    Context context;
    String tag;
    String API_KEY;

    TextureView cameraPreview;

    public SeesoGazeTracker(Context applicationContext, String t, InitializationCallback icb) {
        context = applicationContext;
        tag = t;
        initializationCallback = icb;

        API_KEY = applicationContext.getResources().getString(R.string.SEESO_API_KEY);
    }

    public boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                this.context,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }


    public void startGazeTracker() {
        if (hasCameraPermission()) {
            Log.i(tag, "startGazeTracker");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    GazeTracker.initGazeTracker(context, API_KEY, initializationCallback);
                }
            }).start();
            Log.i(tag, "started gaze tracker");
        }
    }


    public void defaultInitSuccess(GazeTracker gazeTracker) {
        Log.i(tag, "Seeso gaze tracking initialized");
        this.gazeTracker = gazeTracker;

        // CameraPosition class has information pertaining physical size of the Android device
        // such as distance between the camera origin and the screen origin and whether the camera
        // is on the short axis or the long axis of the device
        CameraPosition cp = new CameraPosition("YT-J706F", -120f, -5f, true);
        this.gazeTracker.addCameraPosition(cp);

        // idk why but for some reason this needs to happen in here,
        // possibly because we need to set it before tracking??
        if (cameraPreview != null) {
            if (cameraPreview.isAvailable()) {
                this.gazeTracker.setCameraPreview(cameraPreview);
            }
        }
        this.gazeTracker.startTracking();


        double[] calibrationData = CalibrationDataStorage.loadCalibrationData(this.context);
        if (calibrationData != null) {
            this.gazeTracker.setCalibrationData(calibrationData);
        }
        Log.i(tag, "done with init success");
    }

    public void defaultInitFail(InitializationErrorType error) {
        String err = "";
        if (error == InitializationErrorType.ERROR_INIT) {
            err = "Initialization failed";
        } else if (error == InitializationErrorType.ERROR_CAMERA_PERMISSION) {
            err = "Required permission not granted";
        } else  {
            err = "init gaze library fail";
        }
        Log.w(tag, "Seeso failed: " + err);
        Log.i(tag, "Seeso failed to init " + err + ", " + error.name());
    }

    public void releaseGaze() {
//        Log.i(tag, "releaseGaze");
        if (gazeTracker != null) {
            GazeTracker.deinitGazeTracker(this.gazeTracker);
            this.gazeTracker = null;
            Log.i(tag, "released gaze");
        }
    }

    public float[] filterGaze(GazeInfo gazeInfo) {
        if (oneEuroFilterManager.filterValues(gazeInfo.timestamp, gazeInfo.x, gazeInfo.y)) {
            return oneEuroFilterManager.getFilteredValues();
        }

        return new float[]{gazeInfo.x, gazeInfo.y};
    }

    public void stopTracking() {
//        Log.i(tag, "stopTracking");
        if (this.gazeTracker != null) {
            this.gazeTracker.stopTracking();
        }
    }

    public void startTracking() {
//        Log.i(tag, "startTracking");
        if (this.gazeTracker != null) {
            this.gazeTracker.startTracking();
        }
    }

    public void setCalibrationCallback(CalibrationCallback ccb) {
        this.gazeTracker.setCalibrationCallback(ccb);
    }

    public void setGazeCallback(GazeCallback gcb) {
        this.gazeTracker.setGazeCallback(gcb);
    }

    public void startCalibration(CalibrationModeType mode, AccuracyCriteria criteria) {
        this.gazeTracker.startCalibration(mode, criteria);
    }

    public void startCollectSamples() {
        this.gazeTracker.startCollectSamples();
    }

    public void setCameraPreview(TextureView cp) {
        this.gazeTracker.setCameraPreview(cp);
    }

    public void setCameraPreviewVar (TextureView cp) {
        this.cameraPreview = cp;
    }


}
