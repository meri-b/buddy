package com.example.buddy_prototype_v1.configure;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.buddy_prototype_v1.R;
import com.example.buddy_prototype_v1.menus.SetupActivity;
import com.example.buddy_prototype_v1.tools.PointView;
import com.example.buddy_prototype_v1.tools.SeesoGazeTracker;

import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.callback.CalibrationCallback;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.callback.InitializationCallback;
import camp.visual.gazetracker.constant.AccuracyCriteria;
import camp.visual.gazetracker.constant.CalibrationModeType;
import camp.visual.gazetracker.constant.InitializationErrorType;
import camp.visual.gazetracker.gaze.GazeInfo;
import camp.visual.gazetracker.state.ScreenState;
import camp.visual.gazetracker.util.ViewLayoutChecker;

public class CalibrationActivity  extends AppCompatActivity  {
    String TAG = "CalibrationActivity";
    private HandlerThread backgroundThread = new HandlerThread("background");
    private Handler backgroundHandler;

    private CalibrationView calibrationView;
    SeesoGazeTracker myGazeTracker;
    PointView viewPoint;
    ViewLayoutChecker viewLayoutChecker = new ViewLayoutChecker();

    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calibration_activity);

        myGazeTracker = new SeesoGazeTracker(getApplicationContext(), TAG, initializationCallback);

        calibrationView = findViewById(R.id.viewCalibration);
        calibrationView.setVisibility(View.VISIBLE);
        viewPoint = findViewById(R.id.gazeViewPoint);

        Button returnButton = findViewById(R.id.returnButton);
        returnButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                myGazeTracker.releaseGaze();
                Intent returnIntent = new Intent(CalibrationActivity.this, SetupActivity.class);
                startActivity(returnIntent);
            }
        });

        Button calibrateButton = findViewById(R.id.calibrateButton);
        calibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (myGazeTracker != null) {
                    myGazeTracker.startCalibration(CalibrationModeType.ONE_POINT, AccuracyCriteria.DEFAULT);
                }
            }
        });

        myGazeTracker.startGazeTracker();
        initHandler();
    }

    protected void onStart() {
        calibrationView.startChangingColors(10);
        super.onStart();
    }

    protected void onStop() {
        calibrationView.stopRunning();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseHandler();
        viewLayoutChecker.releaseChecker();
    }

    private void initHandler() {
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void releaseHandler() {
        backgroundThread.quitSafely();
    }

    private InitializationCallback initializationCallback = new InitializationCallback() {
        @Override
        public void onInitialized(GazeTracker gazeTracker, InitializationErrorType error) {
            if (gazeTracker != null) {
                myGazeTracker.defaultInitSuccess(gazeTracker);
                myGazeTracker.setGazeCallback(gazeCallback);
                myGazeTracker.setCalibrationCallback(calibrationCallback);
                setOffsetOfView();
            } else {
                myGazeTracker.defaultInitFail(error);
            }
        }
    };


    private GazeCallback gazeCallback = new GazeCallback() {
        @Override
        public void onGaze(GazeInfo gazeInfo) {
            float[] filteredGaze = myGazeTracker.filterGaze(gazeInfo);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    viewPoint.setType(gazeInfo.screenState == ScreenState.INSIDE_OF_SCREEN ? PointView.TYPE_DEFAULT : PointView.TYPE_OUT_OF_SCREEN);
                    viewPoint.setPosition(filteredGaze[0], filteredGaze[1]);

                }
            });
        }
    };


    private void setOffsetOfView() {
        viewLayoutChecker.setOverlayView(viewPoint, new ViewLayoutChecker.ViewLayoutListener() {
            @Override
            public void getOffset(int x, int y) {
                viewPoint.setOffset(x, y);
            }
        });
    }

    private CalibrationCallback calibrationCallback = new CalibrationCallback() {
        @Override
        public void onCalibrationProgress(float progress) {
//            setCalibrationProgress(progress);
        }

        @Override
        public void onCalibrationNextPoint(final float x, final float y) {
            calibrationView.drawOutline();
            // Give time to eyes find calibration coordinates, then collect data samples
            backgroundHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    myGazeTracker.startCollectSamples();
                }
            }, 1000);
        }

        @Override
        public void onCalibrationFinished(double[] calibrationData) {
            CalibrationDataStorage.saveCalibrationData(getApplicationContext(), calibrationData);
            calibrationView.doNotDrawOutline();
            showToast("calibrationFinished", true);
        }
    };

    private void showToast(final String msg, final boolean isShort) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CalibrationActivity.this, msg, isShort ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
            }
        });
    }
}


