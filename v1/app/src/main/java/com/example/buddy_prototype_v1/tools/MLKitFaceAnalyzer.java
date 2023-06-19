package com.example.buddy_prototype_v1.tools;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.media.Image;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.lifecycle.Lifecycle;

import com.example.buddy_prototype_v1.R;
import com.example.buddy_prototype_v1.configure.StateDetectionActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import camp.visual.gazetracker.state.ScreenState;

// sources:
// https://developers.google.com/ml-kit/vision/face-detection/android#java
// https://github.com/googlesamples/mlkit/blob/master/android/android-snippets/app/src/main/java/com/google/example/mlkit/FaceDetectionActivity.java
// https://www.meekcode.com/blog/camerax-live-face-detection

public class MLKitFaceAnalyzer extends AppCompatActivity implements ImageAnalysis.Analyzer  {
    String TAG = "MLKitFaceAnalyzer2";

    String eyeOpenState = "open";
    String eyeClosedState = "closed";
    FaceDetectorOptions options =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .setMinFaceSize(0.15f)
                    .enableTracking()
                    .build();
    FaceDetector detector;
    public Activity activity;
    TextView displayText;
    int frameCount;

    List<Double> noseChanges;
    List<Float> eyeStates;

    Float prevNoseX;
    Float prevNoseY;

    // Default values
    static final Integer COLLECT_FRAMES_DEFAULT = 10;
    static final Double EYE_OPEN_DEFAULT = 0.25;
    static final Integer EYE_OPEN_COUNT_DEFAULT = 8;
    static final Double MOVEMENT_CHANGE_DEFAULT = 25.0;
    static final Integer CONSECUTIVE_STATE_DEFAULT = 30;

    // Variables to set via config menu
    Integer collectFrames;
    Double eyeOpenDeterminationThreshold;
    Integer eyeOpenCountDeterminationThreshold;
    Double movementChangeThreshold;
    Integer consecutiveStateThreshold;

    ScreenState currentEyeScreenState;
    Boolean trackingGazeCurrently;

    String currentEyeState;
    Double currentMovement;
    String currentState;
    Integer consecutiveStateCount;
    String longTermState;

    public static final String LISTENER_OPTION1 = "stateDiagnosticText";
    public static final String LISTENER_OPTION2 = "buddy1.0";
    public static final String LISTENER_OPTION3 = "diagnosticText";

    String currentSuccessListener = LISTENER_OPTION1;

    SharedPreferences sharedPref;

    FileOutputStream writerStream;
    boolean recording;
    String startRecordingFlag = "###STARTING RECORDING\n";
    String endRecordingFlag = "###ENDING RECORDING\n";

    public MLKitFaceAnalyzer() {
        Log.d(TAG, "default constructor");
        initValues();
    }

    public MLKitFaceAnalyzer(Activity _activity, Lifecycle lifecycle, String tag, String listenOption) {
        TAG = tag;
        Log.d(TAG, "constructor");
        detector = FaceDetection.getClient(options);
        lifecycle.addObserver(detector);
        this.activity = _activity;
        sharedPref = this.activity.getSharedPreferences(StateDetectionActivity.SETTINGS_KEY, MODE_PRIVATE);
        setListenerOption(listenOption);
        initValues();
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        detectFaces(image);
    }

    private void initValues() {
        frameCount = 0;
        noseChanges = new ArrayList<>();
        eyeStates = new ArrayList<>();
        longTermState = "";
        consecutiveStateCount = 0;
        setConfigVariablesToSavedValues();
        trackingGazeCurrently = false;
        recording = false;
        displayText = (TextView)this.activity.findViewById(R.id.mlKitText);
    }

    public void setConfigVariablesToSavedValues() {
        String collectFramesString = sharedPref.getString(StateDetectionActivity.COLLECT_FRAMES_KEY, null);
        if (collectFramesString != null ) {
           setCollectFrames(Integer.parseInt(collectFramesString));
        } else {
            setCollectFrames(getDefaultCollectFrames());
        }

        String eyeOpenString = sharedPref.getString(StateDetectionActivity.EYE_OPEN_KEY, null);
        if (collectFramesString != null ) {
            setEyeOpenDeterminationThreshold(Double.parseDouble(eyeOpenString));
        } else {
            setEyeOpenDeterminationThreshold(getDefaultEyeOpen());
        }

        String eyeOpenCountString = sharedPref.getString(StateDetectionActivity.EYE_OPEN_COUNT_KEY, null);
        if (eyeOpenCountString != null ) {
            setEyeOpenCountDeterminationThreshold(Integer.parseInt(eyeOpenCountString));
        } else {
            setEyeOpenCountDeterminationThreshold(getDefaultEyeOpenCount());
        }

        String movementChangeString = sharedPref.getString(StateDetectionActivity.MOVEMENT_CHANGE_KEY, null);
        if (movementChangeString != null ) {
            setMovementChangeThreshold(Double.parseDouble(movementChangeString));
        } else {
            setMovementChangeThreshold(getDefaultMovementChange());
        }

        String consecutiveStateString = sharedPref.getString(StateDetectionActivity.CONSECUTIVE_STATE_KEY, null);
        if (consecutiveStateString != null ) {
            setConsecutiveStateThreshold(Integer.parseInt(consecutiveStateString));
        } else {
            setConsecutiveStateThreshold(getDefaultConsecutiveState());
        }
    }

    public Integer getDefaultCollectFrames() {
        return COLLECT_FRAMES_DEFAULT;
    }

    public Integer getCollectFrames() {
        return collectFrames;
    }

    public void setCollectFrames(Integer val) {
        collectFrames = val;
    }

    public Double getDefaultEyeOpen() {
        return EYE_OPEN_DEFAULT;
    }

    public Double getEyeOpen() {
        return eyeOpenDeterminationThreshold;
    }

    public void setEyeOpenDeterminationThreshold(Double val) {
        eyeOpenDeterminationThreshold = val;
    }

    public Integer getDefaultEyeOpenCount() {
        return EYE_OPEN_COUNT_DEFAULT;
    }

    public Integer getEyeOpenCount() {
        return eyeOpenCountDeterminationThreshold;
    }

    public void setEyeOpenCountDeterminationThreshold(Integer val) {
        eyeOpenCountDeterminationThreshold = val;
    }

    public Double getDefaultMovementChange() {
        return MOVEMENT_CHANGE_DEFAULT;
    }

    public Double getMovementChange() {
        return movementChangeThreshold;
    }

    public void setMovementChangeThreshold(Double val) {
        movementChangeThreshold = val;
    }

    public Integer getDefaultConsecutiveState() {
        return CONSECUTIVE_STATE_DEFAULT;
    }

    public Integer getConsecutiveState() {
        return consecutiveStateThreshold;
    }

    public void setConsecutiveStateThreshold(Integer val) {
        consecutiveStateThreshold = val;
    }

    public void setCurrentEyeScreenState(ScreenState state) { currentEyeScreenState = state; }

    public ScreenState getCurrentEyeScreenState() { return currentEyeScreenState; }

    public void setTrackingGazeCurrently(Boolean track) { trackingGazeCurrently = track; }
    public Boolean getTrackingGazeCurrently() { return trackingGazeCurrently; }

    public String getCurrentState() { return currentState; }

    public String getLongTermState() { return longTermState; }

    public void setListenerOption(String option) {
        switch (option) {
            case LISTENER_OPTION1:
                Log.i(TAG, "Using listener option: " + LISTENER_OPTION1);
                currentSuccessListener = LISTENER_OPTION1;
                break;
            case LISTENER_OPTION2:
                Log.i(TAG, "Using listener option: " + LISTENER_OPTION2);
                currentSuccessListener = LISTENER_OPTION2;
                break;
            case LISTENER_OPTION3:
                Log.i(TAG, "Using listener option: " + LISTENER_OPTION3);
                currentSuccessListener = LISTENER_OPTION3;
                break;
            default:
                Log.i(TAG, "invalid listener option, setting to default");
                currentSuccessListener = LISTENER_OPTION1;
        }
    }

    OnSuccessListener successListenerBuddy1 = new OnSuccessListener<List<Face>>() {
        @Override
        public void onSuccess(List<Face> faces) {
            frameCount++;
            for (int i = 0; i < faces.size(); i++) {
                PointF nose = faces.get(i).getLandmark(6).getPosition();
                if (i == 0) {
                    if (prevNoseX != null && prevNoseY != null) {
                        Double noseChange = Math.sqrt(Math.pow((prevNoseX-nose.x), 2) + Math.pow((prevNoseY-nose.y), 2));
                        noseChanges.add(noseChange);
                    }
                    prevNoseX = nose.x;
                    prevNoseY = nose.y;

                    eyeStates.add(faces.get(i).getLeftEyeOpenProbability());
                    eyeStates.add(faces.get(i).getRightEyeOpenProbability());
                    if (frameCount > collectFrames) {
                        noseChanges.remove(0);
                        eyeStates.remove(0);
                        eyeStates.remove(0);

                        Double noseSum = noseChanges.stream().mapToDouble(a -> a).sum();
                        currentMovement = noseSum;

                        Integer openCount = 0;
                        for (int j = 0; j < eyeStates.size(); j++) {
                            if (eyeStates.get(j) > eyeOpenDeterminationThreshold) {
                                openCount++;
                            }
                        }
                        if (openCount >= eyeOpenCountDeterminationThreshold) {
                            currentEyeState = eyeOpenState;
                        } else {
                            currentEyeState = eyeClosedState;
                        }

                        String thisState = "";
                        if (currentEyeState == eyeClosedState) {
                            if (currentMovement < movementChangeThreshold) {
                                thisState = StateDetectionActivity.NOT_ALERT;
                            } else {
                                thisState = StateDetectionActivity.ACTIVE;
                            }
                        } else { // eyes are open
                            if (trackingGazeCurrently && currentEyeScreenState == ScreenState.INSIDE_OF_SCREEN) {
                                thisState = StateDetectionActivity.FOCUSED;
                            } else if (currentMovement < movementChangeThreshold) {
                                thisState = StateDetectionActivity.CHILL;
                            } else {
                                thisState = StateDetectionActivity.ACTIVE;
                            }
                        }


                        if (currentState == thisState) {
                            consecutiveStateCount++;
                        } else {
                            consecutiveStateCount = 0;
                        }
                        if (consecutiveStateCount >= consecutiveStateThreshold) {
                            longTermState = thisState;
                        }
                        currentState = thisState;
                    }

                }
            }
        }
    };

    OnSuccessListener  successListenerStateDiagnostic = new OnSuccessListener<List<Face>>() {
        @Override
        public void onSuccess(List<Face> faces) {
            Log.i(TAG, "successListenerStateDiagnostic");
            String textToSet = "";
            frameCount++;
            for (int i = 0; i < faces.size(); i++) {

                if (i > 0 && i < faces.size()-1) {
                    textToSet = textToSet + "\n";
                }
                PointF nose = faces.get(i).getLandmark(6).getPosition();
                textToSet = textToSet +
                       "Face #" + i +
                       "\nLeft eye open: " +  String.format("%.2f", faces.get(i).getLeftEyeOpenProbability()) +
                       "\nRight eye open: " + String.format("%.2f", faces.get(i).getRightEyeOpenProbability()) +
                       "\nNose pos: " + String.format("%.0f", nose.x) + ", " + String.format("%.0f", nose.y) +
                        "\nFrame count: " + frameCount;

                if (i == 0) {
                    if (prevNoseX != null && prevNoseY != null) {
                        Double noseChange = Math.sqrt(Math.pow((prevNoseX-nose.x), 2) + Math.pow((prevNoseY-nose.y), 2));
                        noseChanges.add(noseChange);
                    }
                    prevNoseX = nose.x;
                    prevNoseY = nose.y;

                    eyeStates.add(faces.get(i).getLeftEyeOpenProbability());
                    eyeStates.add(faces.get(i).getRightEyeOpenProbability());
                    if (frameCount > collectFrames) {
                        noseChanges.remove(0);
                        eyeStates.remove(0);
                        eyeStates.remove(0);

                        Double noseSum = noseChanges.stream().mapToDouble(a -> a).sum();
                        currentMovement = noseSum;
                        textToSet = textToSet + "\nNose change: " + String.format("%.2f", noseSum);


                        Integer openCount = 0;
                        for (int j = 0; j < eyeStates.size(); j++) {
                            if (eyeStates.get(j) > eyeOpenDeterminationThreshold) {
                                openCount++;
                            }
                        }
                        if (openCount >= eyeOpenCountDeterminationThreshold) {
                            textToSet = textToSet + "\nEyes: " + eyeOpenState;
                            currentEyeState = eyeOpenState;
                        } else {
                            textToSet = textToSet + "\nEyes: " + eyeClosedState;
                            currentEyeState = eyeClosedState;
                        }

                        String thisState = "";
                        if (currentEyeState == eyeClosedState) {
                            if (currentMovement < movementChangeThreshold) {
                                thisState = StateDetectionActivity.NOT_ALERT;
                            } else {
                                thisState = StateDetectionActivity.ACTIVE;
                            }
                        } else { // eyes are open
                            if (trackingGazeCurrently && currentEyeScreenState == ScreenState.INSIDE_OF_SCREEN) {
                                thisState = StateDetectionActivity.FOCUSED;
                            } else if (currentMovement < movementChangeThreshold) {
                                thisState = StateDetectionActivity.CHILL;
                            } else {
                                thisState = StateDetectionActivity.ACTIVE;
                            }
                        }
                        textToSet = textToSet + "\nCurrent state: " + thisState;

                        if (currentState == thisState) {
                            consecutiveStateCount++;
                        } else {
                            consecutiveStateCount = 0;
                        }
                        if (consecutiveStateCount >= consecutiveStateThreshold) {
                            longTermState = thisState;
                        }
                        textToSet  = textToSet + "\nLong term state: " + longTermState;
                        currentState = thisState;
                    }
                }
            }
            displayText.setText(textToSet);
        }
    };

    OnSuccessListener successListenerDiagnostic = new OnSuccessListener<List<Face>>() {
        @Override
        public void onSuccess(List<Face> faces) {
            String textToSet = "";
            for (int i = 0; i < faces.size(); i++) {

                if (i > 0 && i < faces.size() - 1) {
                    textToSet = textToSet + "\n";
                }
                PointF rightEye = faces.get(i).getLandmark(10).getPosition();
                PointF leftEye = faces.get(i).getLandmark(4).getPosition();
                PointF nose = faces.get(i).getLandmark(6).getPosition();
                textToSet = textToSet +
                        "Face #" + i +
                        "\nLeft eye open: " + String.format("%.2f", faces.get(i).getLeftEyeOpenProbability()) +
                        "\nRight eye open: " + String.format("%.2f", faces.get(i).getRightEyeOpenProbability()) +
                        "\nLeft eye pos: " + String.format("%.0f", leftEye.x) + ", " + String.format("%.0f", leftEye.y) +
                        "\nRight eye pos: " + String.format("%.0f", rightEye.x) + ", " + String.format("%.0f", rightEye.y) +
                        "\nNose pos: " + String.format("%.0f", nose.x) + ", " + String.format("%.0f", nose.y);
                if (recording) {
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                    String recordText = timestamp.toString() + "," + i + "," + String.format("%.2f", faces.get(i).getLeftEyeOpenProbability()) + "," +
                            String.format("%.2f", faces.get(i).getRightEyeOpenProbability()) + "," +
                            String.format("%.0f", leftEye.x) + "," + String.format("%.0f", leftEye.y) + "," +
                            String.format("%.0f", rightEye.x) + "," + String.format("%.0f", rightEye.y) + "," +
                            String.format("%.0f", nose.x) + "," + String.format("%.0f", nose.y) + "\n";
                    try {
                        writerStream.write(recordText.getBytes(StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            displayText.setText(textToSet);

        }
    };

    OnFailureListener failureListener = new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {
            Log.e(TAG, "Face analysis failure", e);
        }
    };


    private void detectFaces(ImageProxy imageProxy) {
            @SuppressLint("UnsafeOptInUsageError") InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
            detector.process(image)
                    .addOnSuccessListener(getSuccessListener())
                    .addOnFailureListener(failureListener)
                    .addOnCompleteListener(new OnCompleteListener() {
                        @Override
                        public void onComplete(@NonNull Task task) {
                            // Important! See for context:
                            // https://stackoverflow.com/questions/59606400/imageanalysis-analyzer-only-fires-once
                            imageProxy.close();
                        }
                    });
    }

    private OnSuccessListener getSuccessListener() {
        switch(currentSuccessListener) {
            case LISTENER_OPTION2:
                return successListenerBuddy1;
            case LISTENER_OPTION3:
                return successListenerDiagnostic;
            default:
                // also applies to case LISTENER_OPTION1
                return successListenerStateDiagnostic;
        }
    }

    public void setRecording(boolean recordingState, String fileName, File path) {
        Log.d("MLKitFaceAnalyzer", "setting recording" + recordingState);
        if (recording == true && recordingState == false) {
            // stop recording
            try {
                writerStream.write(endRecordingFlag.getBytes());
                writerStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (recording == false && recordingState == true) {
            // start recording
            try {
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                String specificFileName = timestamp.toString() + "-MLKIT-" + fileName;
                writerStream = new FileOutputStream(new File(path, specificFileName));
                writerStream.write(startRecordingFlag.getBytes());
                writerStream.write(("MLKIT\n").getBytes(StandardCharsets.UTF_8));
                String columnText = "time,left eye open,right eye open,left eye x,left eye y, right eye x, right eye y, nose x, nose y\n";
                writerStream.write(columnText.getBytes());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        recording = recordingState;
    }

}