package com.example.buddy_prototype_v1.utility_apps;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.buddy_prototype_v1.menus.DevToolsActivity;
import com.example.buddy_prototype_v1.MainActivity;
import com.example.buddy_prototype_v1.R;
import com.example.buddy_prototype_v1.tools.MLKitFaceAnalyzer;
import com.example.buddy_prototype_v1.tools.PointView;
import com.example.buddy_prototype_v1.tools.SeesoGazeTracker;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.concurrent.ExecutionException;

import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.callback.InitializationCallback;
import camp.visual.gazetracker.constant.InitializationErrorType;
import camp.visual.gazetracker.gaze.GazeInfo;
import camp.visual.gazetracker.state.ScreenState;
import camp.visual.gazetracker.util.ViewLayoutChecker;

public class DiagnosticsActivity extends AppCompatActivity {
    String TAG = "DiagnosticsActivity";
    private PreviewView previewView;
    TextureView cameraPreview;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    SeesoGazeTracker myGazeTracker;
    PointView viewPoint;
    ViewLayoutChecker viewLayoutChecker = new ViewLayoutChecker();
    FileOutputStream outputStream;

    TextView seesoDisplayText;
    enum State {
        SEESO,
        MLKIT;
    }
    State currentState;

    Button recordButton;
    boolean recording;
    String startRecordingFlag = "###STARTING RECORDING\n";
    String endRecordingFlag = "###ENDING RECORDING\n";
    String fileName;
    File path;

    float height_min;
    float height_max;
    float width_min;
    float width_max;

    MLKitFaceAnalyzer faceAnalyzer;

    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.diagnostics_activity);

        myGazeTracker = new SeesoGazeTracker(getApplicationContext(), TAG, initializationCallback);

        height_min = 100000;
        height_max = -100000;
        width_min = 100000;
        width_max = -100000;

        currentState = State.MLKIT;
        recording  = false;

        seesoDisplayText = findViewById(R.id.seesoTextView);
        viewPoint = findViewById(R.id.gazeViewPoint);
        cameraPreview = findViewById(R.id.cameraTextureView);

        fileName = "diagnosticsLog.txt";
        path = getApplicationContext().getExternalFilesDir(null);
        if (!path.exists()) {
            path.mkdirs();
        }
        Log.i(TAG, "external dir" + path);

        Button returnButton = findViewById(R.id.returnButton);
        returnButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (currentState == State.SEESO) {
                    myGazeTracker.releaseGaze();
                } else {
                    stopCamera();
                }
                Log.i(TAG, "Height min" + height_min + "Height Max" + height_max + "Width min" + width_min + "Width Max" + width_max);
                Intent returnIntent = new Intent(DiagnosticsActivity.this, DevToolsActivity.class);
                startActivity(returnIntent);
            }
        });

        recordButton = findViewById(R.id.recordButton);
        recordButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (recording) {
                    stopRecordingToFile();
                } else {
                    startRecordingToFile();
                }
            }
        });


        Button switchButton = findViewById(R.id.switchButton);
        switchButton.setText("Seeso");
        if (MainActivity.gazeEnabled == false) {
            switchButton.setEnabled(false);
        }
        switchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (recording == true) {
                    stopRecordingToFile();
                }
                if (currentState == State.MLKIT) {
                    switchButton.setText("MLKit");
                    stopCamera();
                    myGazeTracker.startGazeTracker();
                    cameraPreview.setAlpha(1);
                    currentState = State.SEESO;

                } else {
                    switchButton.setText("Seeso");
                    myGazeTracker.releaseGaze();
                    cameraPreview.setAlpha(0);
                    startCamera();
                    currentState = State.MLKIT;
                }
            }
        });

        // FIXME: the camera + MLKit is fighting for camera access
        // need to figure out how to get them to play nice (if possible)
        if (currentState == State.SEESO) {
            myGazeTracker.startGazeTracker();
        } else {
            startCamera();
        }

    }

    protected void onDestroy() {
        super.onDestroy();
        viewLayoutChecker.releaseChecker();
    }

    private void stopCamera() {
        try {
            ProcessCameraProvider cameraProvider = this.cameraProviderFuture.get();
            cameraProvider.unbindAll();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // https://medium.com/swlh/introduction-to-androids-camerax-with-java-ca384c522c5
    // https://developer.android.com/codelabs/camerax-getting-started#0
    private void startCamera() {
        previewView = findViewById(R.id.viewFinder);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
                .build();
        faceAnalyzer = new MLKitFaceAnalyzer(this, getLifecycle(), TAG, MLKitFaceAnalyzer.LISTENER_OPTION3);
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), faceAnalyzer);

        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageAnalysis);
    }

    private InitializationCallback initializationCallback = new InitializationCallback() {
        @Override
        public void onInitialized(GazeTracker gazeTracker, InitializationErrorType error) {
            if (gazeTracker != null) {
                myGazeTracker.setCameraPreviewVar(cameraPreview);
                myGazeTracker.defaultInitSuccess(gazeTracker);
                myGazeTracker.setGazeCallback(gazeCallback);
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

//            Log.i(TAG, "Seeso gaze coord " + gazeInfo.x + "x" + gazeInfo.y);
            String buildText = "Gaze coord: " + String.format("%.0f", gazeInfo.x) + "x" + String.format("%.0f", gazeInfo.y)
                    + "\nFiltered: " +  String.format("%.0f", filteredGaze[0]) + "x" + String.format("%.0f", filteredGaze[1])
                    + "\nTracking: " + gazeInfo.trackingState
                    + "\nEye state: " + gazeInfo.eyeMovementState
                    + "\nScreen state: " + gazeInfo.screenState;


            if (gazeInfo.screenState == ScreenState.INSIDE_OF_SCREEN) {
                if (gazeInfo.x > width_max) {
                    width_max = gazeInfo.x;
                }
                if (gazeInfo.x < width_min) {
                    width_min = gazeInfo.x;
                }
                if (gazeInfo.y > height_max) {
                    height_max = gazeInfo.y;
                }
                if (gazeInfo.y < height_min) {
                    height_min = gazeInfo.y;
                }
            }
            final String textToSet = buildText;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    viewPoint.setType(gazeInfo.screenState == ScreenState.INSIDE_OF_SCREEN ? PointView.TYPE_DEFAULT : PointView.TYPE_OUT_OF_SCREEN);
                    viewPoint.setPosition(filteredGaze[0], filteredGaze[1]);
                    seesoDisplayText.setText(textToSet);
                }
            });
            if (recording) {
                // format: time, x, y, tracking, eye state, screen state
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                String recordText = timestamp.toString() + "," + String.format("%.0f", gazeInfo.x) + "," + String.format("%.0f", gazeInfo.y) + "," + gazeInfo.trackingState + "," + gazeInfo.eyeMovementState + "," + gazeInfo.screenState + "\n";
                try {
                    outputStream.write(recordText.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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

    private void stopRecordingToFile() {
        Log.i(TAG, "stopping recording");
        // stop
        recordButton.setText("record to file");
        recording = false;

        if (currentState == State.SEESO) {
            try {
                outputStream.write(endRecordingFlag.getBytes());
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (faceAnalyzer != null) {
                faceAnalyzer.setRecording(recording, fileName, path);
            }
        }
    }

    private void startRecordingToFile() {
        recordButton.setText("stop recording");
        Log.i(TAG, "starting recording");
        // start
        recording = true;
        if (currentState == State.SEESO) {
            try {
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                String specificFileName = timestamp.toString() + "-SEESO-" + fileName;
                outputStream = new FileOutputStream(new File(path, specificFileName));
                outputStream.write(startRecordingFlag.getBytes());
                outputStream.write((currentState.toString() + "\n").getBytes(StandardCharsets.UTF_8));
                String columnText = "time,x,y,tracking,eye state,screen state\n";
                outputStream.write(columnText.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (faceAnalyzer != null) {
                faceAnalyzer.setRecording(recording, fileName, path);
            }
        }
    }

}
