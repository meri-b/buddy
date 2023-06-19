package com.example.buddy_prototype_v1.utility_apps;

import android.content.Intent;
import android.hardware.camera2.CameraDevice;
import android.media.ImageReader;
import android.os.Bundle;
import android.util.Log;
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
import com.example.buddy_prototype_v1.tools.MLKitFaceAnalyzer;
import com.example.buddy_prototype_v1.R;
import com.example.buddy_prototype_v1.tools.SeesoGazeTracker;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.callback.InitializationCallback;
import camp.visual.gazetracker.constant.InitializationErrorType;
import camp.visual.gazetracker.gaze.GazeInfo;
import camp.visual.gazetracker.state.ScreenState;

public class Diagnostics2Activity extends AppCompatActivity {
    String TAG = "Diagnostics2Activity";

    // Seeso Stuff
    SeesoGazeTracker myGazeTracker;
    TextView seesoDisplayText; // onscreen text
    ScreenState currentScreenState;

    // MLKit stuff
    MLKitFaceAnalyzer faceAnalyzer;
    // Custom View that displays the camera feed for CameraX's Preview use case.
    // This class manages the preview Surface's lifecycle
    // It internally uses either a TextureView or SurfaceView to display the camera feed
    PreviewView previewView;
    // used to bind the lifecycle of cameras to any LifecycleOwner within an application's process.
    ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    // Camera2 stuff
    CameraDevice cameraDevice;
    ImageReader imageReader;

    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.diagnostics_2_activity);

        // -------------------
        // Seeso variables
        // -------------------
        myGazeTracker = new SeesoGazeTracker(getApplicationContext(), TAG, initializationCallback);
        seesoDisplayText = findViewById(R.id.seesoTextView);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        Runnable startGazeRunnable = new Runnable() {
            public void run() {
                myGazeTracker.startGazeTracker();
            }
        };

        Runnable gazeRunnable = new Runnable() {
            public void run() {
                myGazeTracker.startTracking();
            }
        };

        Runnable endGazeRunnable = new Runnable() {
            public void run() {
                myGazeTracker.stopTracking();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        seesoDisplayText.setText("Not tracking\nScreen state: " + currentScreenState);
                    }
                });
            }
        };

        executor.schedule(startGazeRunnable, 4000, TimeUnit.MILLISECONDS); // Gaze first starts 4 seconds in
        executor.schedule(endGazeRunnable, 6000, TimeUnit.MILLISECONDS); // Give it at least 2 seconds to initialize
        // scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)
        // Creates and executes a periodic action that becomes enabled first after the given initial delay,
        // and subsequently with the given period; that is executions will commence after initialDelay then
        // initialDelay+period, then initialDelay + 2 * period, and so on.
        executor.scheduleAtFixedRate(gazeRunnable, 8000, 4000, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(endGazeRunnable, 8500, 4000, TimeUnit.MILLISECONDS);

        // -------------------
        // Buttons
        // -------------------
        Button returnButton = findViewById(R.id.returnButton);
        returnButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                myGazeTracker.releaseGaze();
                stopCamera();
                executor.shutdown();
                Intent returnIntent = new Intent(Diagnostics2Activity.this, DevToolsActivity.class);
                startActivity(returnIntent);
            }
        });

        startCamera();
    }


//---------------------------------------
// MLKit stuff
//---------------------------------------
    private void startCamera() {
        Log.i(TAG, "startCamera");

        previewView = findViewById(R.id.viewFinder);
        //  single process camera provider can exist within a process, and it can be retrieved with getInstance.
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i(TAG, "run in startCamera");
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);
                    Log.i(TAG, "run in startCamera done");
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
        Log.i(TAG, "done with startCamera");

    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        Log.i(TAG, "bindImageAnalysis");
        // androidx.Camera.Core

        // A use case providing CPU accessible images for an app to perform image analysis on.
        // acquires images from the camera via an ImageReader.
        // Each image is provided to an ImageAnalysis.Analyzer function which can be
        // implemented by application code, where it can access image data for application
        // analysis via an ImageProxy
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
                .build();
        // A use case that provides a camera preview stream for displaying on-screen
        Preview preview = new Preview.Builder().build();
        // A set of requirements and priorities used to select a camera or return a filtered set of cameras.
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();

        // Sets a SurfaceProvider to provide a Surface for Preview.
        // Setting the provider will signal to the camera that the use case is ready to receive data.
        // If the provider is removed by calling this again with a null SurfaceProvider
        // then the camera will stop producing data for this Preview instance.
        preview.setSurfaceProvider(previewView.getSurfaceProvider());


        faceAnalyzer = new MLKitFaceAnalyzer(this, getLifecycle(), TAG, faceAnalyzer.LISTENER_OPTION3);
        // Setting an analyzer will signal to the camera that it should begin sending data.
        // The stream of data can be stopped by calling clearAnalyzer
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), faceAnalyzer);

        // The state of the lifecycle will determine when the cameras are open, started, stopped and closed.
        // When started, the use cases receive camera data.
        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageAnalysis);

        preview.setSurfaceProvider(null); // if we leave this, the camera feed will not show up on screen

        Log.i(TAG, "done with bindImageAnalysis");

    }

    private void stopCamera() {
        try {
            ProcessCameraProvider cameraProvider = this.cameraProviderFuture.get();
            cameraProvider.unbindAll();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }


//---------------------------------------
//  Seeso Callbacks
//---------------------------------------
    private InitializationCallback initializationCallback = new InitializationCallback() {
        @Override
        public void onInitialized(GazeTracker gazeTracker, InitializationErrorType error) {
            if (gazeTracker != null) {
                Log.i(TAG, "init callback");
                myGazeTracker.defaultInitSuccess(gazeTracker);
                myGazeTracker.setGazeCallback(gazeCallback);
            } else {
                myGazeTracker.defaultInitFail(error);
            }
        }
    };


    private GazeCallback gazeCallback = new GazeCallback() {
        @Override
        public void onGaze(GazeInfo gazeInfo) {
//            Log.i(TAG, "gaze");
            float[] filteredGaze = myGazeTracker.filterGaze(gazeInfo);
            currentScreenState = gazeInfo.screenState;
            String buildText =  "Tracking\nScreen state: " + gazeInfo.screenState;
            final String textToSet = buildText;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    seesoDisplayText.setText(textToSet);
                }
            });
//            Log.i(TAG, "done with onGaze");

        }
    };

}