package com.example.buddy_prototype_v1.configure;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.buddy_prototype_v1.tools.MLKitFaceAnalyzer;
import com.example.buddy_prototype_v1.MainActivity;
import com.example.buddy_prototype_v1.menus.SetupActivity;
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

public class StateDetectionActivity extends AppCompatActivity {
    String TAG = "StateDetectionActivity";

    public static final String SETTINGS_KEY = "stateDetectionSettings";
    public static final String COLLECT_FRAMES_KEY = "collectFrames";
    public static final String EYE_OPEN_KEY = "eyeOpen";
    public static final String EYE_OPEN_COUNT_KEY = "eyeOpenCount";
    public static final String MOVEMENT_CHANGE_KEY = "movementChange";
    public static final String CONSECUTIVE_STATE_KEY = "consecutiveState";
    public static final String TRIGGER_STATE_KEY = "triggerState";
    public static final String TRIGGER_ACTIVITY_THRESHOLD_KEY  = "triggerActivityThreshold";
    public static final String STATE_WINDOW_KEY  = "stateWindow";
    public static final String DEBUG_TRIGGER_STATE_KEY  = "debugTriggerState";
    public static final String DEBUG_TRIGGER_ACTIVITY_THRESHOLD_KEY  = "debugTriggerActivityThreshold";
    public static final String DEBUG_STATE_WINDOW_KEY  = "debugStateWindow";

    public static final String[] states = {"not alert", "chill", "active", "focused"};
    public static final String NOT_ALERT = states[0];
    public static final String CHILL = states[1];
    public static final String ACTIVE = states[2];
    public static final String FOCUSED = states[3];

    static final String TRIGGER_STATE_DEFAULT = CHILL;
    static final Float TRIGGER_ACTIVITY_THRESHOLD_DEFAULT = 0.5f;
    static final Integer STATE_WINDOW_DEFAULT = 20;

    static final String DEBUG_TRIGGER_STATE_DEFAULT = CHILL;
    static final Float DEBUG_TRIGGER_ACTIVITY_THRESHOLD_DEFAULT = 0.5f;
    static final Integer DEBUG_STATE_WINDOW_DEFAULT = 10;

    SharedPreferences sharedPref;

    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    MLKitFaceAnalyzer faceAnalyzer;

    Boolean trackGaze = false;
    SeesoGazeTracker myGazeTracker;
    ScreenState currentEyeScreenState;

    String selectedTriggerState;
    String selectedDebugTriggerState;
    ArrayAdapter<String> stateAdapter;

    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.state_detection_activity);

        myGazeTracker = new SeesoGazeTracker(getApplicationContext(), TAG, initializationCallback);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        stateAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, states);

        Button gazeButton = findViewById(R.id.gazeButton);
        if (MainActivity.gazeEnabled == true) {
            Runnable startGazeRunnable = new Runnable() {
                public void run() {
                    myGazeTracker.startGazeTracker();
                }
            };
            Runnable gazeRunnable = new Runnable() {
                public void run() {
                    if (trackGaze) {
                        Log.i(TAG, "start tracking");
                        myGazeTracker.startTracking();
                    }
                }
            };
            Runnable endGazeRunnable = new Runnable() {
                public void run() {
                    myGazeTracker.stopTracking();
                }
            };
            executor.schedule(startGazeRunnable, 4000, TimeUnit.MILLISECONDS); // Gaze first starts 4 seconds in
            executor.schedule(endGazeRunnable, 6000, TimeUnit.MILLISECONDS); // Give it at least 2 seconds to initialize
            // FIXME: tweak these values??
            executor.scheduleAtFixedRate(gazeRunnable, 8000, 4000, TimeUnit.MILLISECONDS);
            executor.scheduleAtFixedRate(endGazeRunnable, 8500, 4000, TimeUnit.MILLISECONDS);

            gazeButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (trackGaze) {
                        gazeButton.setText("Track gaze");
                        trackGaze = false;
                        if (faceAnalyzer != null) {
                            faceAnalyzer.setTrackingGazeCurrently(false);
                        }
                    } else {
                        gazeButton.setText("Stop gaze track");
                        trackGaze = true;
                        if (faceAnalyzer != null) {
                            faceAnalyzer.setTrackingGazeCurrently(true);
                        }
                    }
                }
            });
        } else {
            gazeButton.setEnabled(false);
        }

        sharedPref = getSharedPreferences(SETTINGS_KEY, MODE_PRIVATE);

        EditText collectFramesText = findViewById(R.id.editCollectFramesText);
        EditText eyeOpenText = findViewById(R.id.editEyeOpenText);
        EditText eyeOpenCountText = findViewById(R.id.editEyeOpenCountText);
        EditText movementChangeText = findViewById(R.id.editMovementChangeText);
        EditText consecutiveStateText = findViewById(R.id.editConsecutiveStateText);

        Spinner triggerActivityDropdown = findViewById(R.id.triggerStateSpinner);
        EditText triggerActivityThresholdText = findViewById(R.id.editTriggerActivityAtThresholdText);
        EditText stateWindowText = findViewById(R.id.editStateWindowText);
        Spinner debugTriggerActivityDropdown = findViewById(R.id.debugTriggerStateSpinner);
        EditText debugTriggerActivityThresholdText = findViewById(R.id.editDebugTriggerActivityAtThresholdText);
        EditText debugStateWindowText = findViewById(R.id.editDebugStateWindowText);


        triggerActivityDropdown.setAdapter(stateAdapter);
        triggerActivityDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedTriggerState = states[i];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                triggerActivityDropdown.setSelection(stateAdapter.getPosition(TRIGGER_STATE_DEFAULT));
                selectedTriggerState = TRIGGER_STATE_DEFAULT;
            }
        });

        debugTriggerActivityDropdown.setAdapter(stateAdapter);
        debugTriggerActivityDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedDebugTriggerState = states[i];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                debugTriggerActivityDropdown.setSelection(stateAdapter.getPosition(DEBUG_TRIGGER_STATE_DEFAULT));
                selectedDebugTriggerState = DEBUG_TRIGGER_STATE_DEFAULT;
            }
        });


        Button returnButton = findViewById(R.id.returnButton);
        returnButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopCamera();
                myGazeTracker.releaseGaze();
                executor.shutdown();
                Intent returnIntent = new Intent(StateDetectionActivity.this, SetupActivity.class);
                startActivity(returnIntent);
            }
        });

        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (faceAnalyzer != null) {
                    faceAnalyzer.setCollectFrames(Integer.parseInt(collectFramesText.getText().toString()));
                    faceAnalyzer.setEyeOpenDeterminationThreshold(Double.parseDouble(eyeOpenText.getText().toString()));
                    faceAnalyzer.setEyeOpenCountDeterminationThreshold(Integer.parseInt(eyeOpenCountText.getText().toString()));
                    faceAnalyzer.setMovementChangeThreshold(Double.parseDouble(movementChangeText.getText().toString()));
                    faceAnalyzer.setConsecutiveStateThreshold(Integer.parseInt(consecutiveStateText.getText().toString()));

                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(COLLECT_FRAMES_KEY, collectFramesText.getText().toString());
                    editor.putString(EYE_OPEN_KEY, eyeOpenText.getText().toString());
                    editor.putString(EYE_OPEN_COUNT_KEY, eyeOpenCountText.getText().toString());
                    editor.putString(MOVEMENT_CHANGE_KEY, movementChangeText.getText().toString());
                    editor.putString(CONSECUTIVE_STATE_KEY, consecutiveStateText.getText().toString());
                    editor.putString(TRIGGER_STATE_KEY, selectedTriggerState);
                    editor.putString(TRIGGER_ACTIVITY_THRESHOLD_KEY , triggerActivityThresholdText.getText().toString());
                    editor.putString(STATE_WINDOW_KEY , stateWindowText.getText().toString());
                    editor.putString(DEBUG_TRIGGER_STATE_KEY, selectedDebugTriggerState);
                    editor.putString(DEBUG_TRIGGER_ACTIVITY_THRESHOLD_KEY , debugTriggerActivityThresholdText.getText().toString());
                    editor.putString(DEBUG_STATE_WINDOW_KEY , debugStateWindowText.getText().toString());

                    editor.commit();
                }

            }
        });

        Button defaultButton = findViewById(R.id.defaultButton);
        defaultButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (faceAnalyzer != null) {
                    collectFramesText.setText(faceAnalyzer.getDefaultCollectFrames().toString());
                    eyeOpenText.setText(faceAnalyzer.getDefaultEyeOpen().toString());
                    eyeOpenCountText.setText(faceAnalyzer.getDefaultEyeOpenCount().toString());
                    movementChangeText.setText(faceAnalyzer.getDefaultMovementChange().toString());
                    consecutiveStateText.setText(faceAnalyzer.getDefaultConsecutiveState().toString());

                    triggerActivityDropdown.setSelection(stateAdapter.getPosition(TRIGGER_STATE_DEFAULT));
                    selectedTriggerState = TRIGGER_STATE_DEFAULT;
                    triggerActivityThresholdText.setText("30");
                    stateWindowText.setText("10");
                    debugTriggerActivityDropdown.setSelection(stateAdapter.getPosition(DEBUG_TRIGGER_STATE_DEFAULT));
                    selectedDebugTriggerState = DEBUG_TRIGGER_STATE_DEFAULT;
                    debugTriggerActivityThresholdText.setText("10");
                    debugStateWindowText.setText("10");
                }
            }
        });

        startCamera();
    }

    private void initConfigValues() {
        String collectFramesString = sharedPref.getString(COLLECT_FRAMES_KEY, null);
        if (collectFramesString != null ) {
            faceAnalyzer.setCollectFrames(Integer.parseInt(collectFramesString));
        } else {
            faceAnalyzer.setCollectFrames(faceAnalyzer.getDefaultCollectFrames());
        }

        String eyeOpenString = sharedPref.getString(EYE_OPEN_KEY, null);
        if (collectFramesString != null ) {
            faceAnalyzer.setEyeOpenDeterminationThreshold(Double.parseDouble(eyeOpenString));
        } else {
            faceAnalyzer.setEyeOpenDeterminationThreshold(faceAnalyzer.getDefaultEyeOpen());
        }

        String eyeOpenCountString = sharedPref.getString(EYE_OPEN_COUNT_KEY, null);
        if (eyeOpenCountString != null ) {
            faceAnalyzer.setEyeOpenCountDeterminationThreshold(Integer.parseInt(eyeOpenCountString));
        } else {
            faceAnalyzer.setEyeOpenCountDeterminationThreshold(faceAnalyzer.getDefaultEyeOpenCount());
        }

        String movementChangeString = sharedPref.getString(MOVEMENT_CHANGE_KEY, null);
        if (movementChangeString != null ) {
            faceAnalyzer.setMovementChangeThreshold(Double.parseDouble(movementChangeString));
        } else {
            faceAnalyzer.setMovementChangeThreshold(faceAnalyzer.getDefaultMovementChange());
        }

        String consecutiveStateString = sharedPref.getString(CONSECUTIVE_STATE_KEY, null);
        if (consecutiveStateString != null ) {
            faceAnalyzer.setConsecutiveStateThreshold(Integer.parseInt(consecutiveStateString));
        } else {
            faceAnalyzer.setConsecutiveStateThreshold(faceAnalyzer.getDefaultConsecutiveState());
        }



        // display all the retrieved or set values in the edit text fields
        EditText collectFramesText = findViewById(R.id.editCollectFramesText);
        EditText eyeOpenText = findViewById(R.id.editEyeOpenText);
        EditText eyeOpenCountText = findViewById(R.id.editEyeOpenCountText);
        EditText movementChangeText = findViewById(R.id.editMovementChangeText);
        EditText consecutiveStateText = findViewById(R.id.editConsecutiveStateText);
        Spinner triggerActivityDropdown = findViewById(R.id.triggerStateSpinner);
        EditText triggerActivityAtThresholdText = findViewById(R.id.editTriggerActivityAtThresholdText);
        EditText stateWindowText = findViewById(R.id.editStateWindowText);
        Spinner debugTriggerActivityDropdown = findViewById(R.id.debugTriggerStateSpinner);
        EditText debugTriggerActivityAtThresholdText = findViewById(R.id.editDebugTriggerActivityAtThresholdText);
        EditText debugStateWindowText = findViewById(R.id.editDebugStateWindowText);

        collectFramesText.setText(Integer.toString(faceAnalyzer.getCollectFrames()));
        eyeOpenText.setText(Double.toString(faceAnalyzer.getEyeOpen()));
        eyeOpenCountText.setText(Integer.toString(faceAnalyzer.getEyeOpenCount()));
        movementChangeText.setText(Double.toString(faceAnalyzer.getMovementChange()));
        consecutiveStateText.setText(Integer.toString(faceAnalyzer.getConsecutiveState()));

        String triggerStateString = sharedPref.getString(TRIGGER_STATE_KEY, null);
        if(triggerStateString != null) {
            triggerActivityDropdown.setSelection(stateAdapter.getPosition(triggerStateString));
        } else {
            triggerActivityDropdown.setSelection(stateAdapter.getPosition(TRIGGER_STATE_DEFAULT));
        }

        String triggerActivityAtThresholdString = sharedPref.getString(TRIGGER_ACTIVITY_THRESHOLD_KEY, null);
        if (triggerActivityAtThresholdString != null) {
            triggerActivityAtThresholdText.setText(triggerActivityAtThresholdString);
        } else {
            triggerActivityAtThresholdText.setText(String.valueOf(TRIGGER_ACTIVITY_THRESHOLD_DEFAULT));
        }

        String stateWindowString = sharedPref.getString(STATE_WINDOW_KEY, null);
        if (stateWindowString != null) {
            stateWindowText.setText(stateWindowString);
        } else {
            stateWindowText.setText(String.valueOf(STATE_WINDOW_DEFAULT));
        }

        String debugTriggerStateString = sharedPref.getString(TRIGGER_STATE_KEY, null);
        if(debugTriggerStateString != null) {
            debugTriggerActivityDropdown.setSelection(stateAdapter.getPosition(debugTriggerStateString));
        } else {
            debugTriggerActivityDropdown.setSelection(stateAdapter.getPosition(DEBUG_TRIGGER_STATE_DEFAULT));
        }

        String debugTriggerActivityAtThresholdString = sharedPref.getString(DEBUG_TRIGGER_ACTIVITY_THRESHOLD_KEY, null);
        if (debugTriggerActivityAtThresholdString != null) {
            debugTriggerActivityAtThresholdText.setText(debugTriggerActivityAtThresholdString);
        } else {
            debugTriggerActivityAtThresholdText.setText(String.valueOf(DEBUG_TRIGGER_ACTIVITY_THRESHOLD_DEFAULT));
        }

        String debugStateWindowString = sharedPref.getString(DEBUG_STATE_WINDOW_KEY, null);
        if (debugStateWindowString != null) {
            debugStateWindowText.setText(debugStateWindowString);
        } else {
            debugStateWindowText.setText(String.valueOf(STATE_WINDOW_DEFAULT));
        }

    }

    public static String getDefaultTriggerState() { return TRIGGER_STATE_DEFAULT; }

    public static Float getDefaultTriggerStateThreshold() { return TRIGGER_ACTIVITY_THRESHOLD_DEFAULT; }

    public static Integer getDefaultStateWindow() { return STATE_WINDOW_DEFAULT; }

    public static String getDefaultDebugTriggerState() { return DEBUG_TRIGGER_STATE_DEFAULT; }

    public static Float getDefaultDebugTriggerStateThreshold() { return DEBUG_TRIGGER_ACTIVITY_THRESHOLD_DEFAULT; }

    public static Integer getDefaultDebugStateWindow() { return DEBUG_STATE_WINDOW_DEFAULT; }

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
        faceAnalyzer = new MLKitFaceAnalyzer(this, getLifecycle(), TAG, MLKitFaceAnalyzer.LISTENER_OPTION1);
        initConfigValues();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), faceAnalyzer);

        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageAnalysis);

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
            currentEyeScreenState = gazeInfo.screenState;
            faceAnalyzer.setCurrentEyeScreenState(currentEyeScreenState);

        }
    };
}
