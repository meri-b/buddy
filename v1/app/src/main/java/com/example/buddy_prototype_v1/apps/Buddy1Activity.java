package com.example.buddy_prototype_v1.apps;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.LifecycleOwner;

import com.example.buddy_prototype_v1.MainActivity;
import com.example.buddy_prototype_v1.R;
import com.example.buddy_prototype_v1.configure.StateDetectionActivity;
import com.example.buddy_prototype_v1.tools.GameView;
import com.example.buddy_prototype_v1.tools.MLKitFaceAnalyzer;
import com.example.buddy_prototype_v1.tools.SeesoGazeTracker;
import com.example.buddy_prototype_v1.tools.Voiceover;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.callback.InitializationCallback;
import camp.visual.gazetracker.constant.InitializationErrorType;
import camp.visual.gazetracker.gaze.GazeInfo;

public class Buddy1Activity  extends AppCompatActivity {
    String DEV_KEY;
    String TAG = "ViewBuddy1";

    // Saved preference key names (persistent)
    String SETTINGS_KEY = "buddy1Settings";
    String PLAYLIST_ID_KEY = "playlistID";
    String SCRATCH_URIS_KEY = "buddyScratchURIs";
    String SCRATCH_FILENAMES_KEY = "buddyScratchFilenames";
    // Scratch input variable names -- also retrieved from preferences
    String STEP_X = "stepX";
    String STEP_Y = "stepY";
    String START_X = "startX";
    String START_Y = "startY";
    String THRES_X = "thresX";
    String THRES_Y = "thresY";

    SharedPreferences sharedPref;
    private GameView gameView;
    Voiceover voice;

    String[] modes = new String[]{"debug", "regular"};
    String[] sessionLengths = new String[]{"1", "2", "5", "10", "20", "30"};
    String mode;
    String sessionLength;
    Boolean inHangoutSession;
    ScheduledExecutorService executor;

    // MLKit things
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    MLKitFaceAnalyzer faceAnalyzer;

    // Variables for tracking the user's activity state, and how to interpret it
    String currentState;
    List stateOverTime;
    Integer stateWindowToUse;
    String initiateActivityAfterState;
    double initiateActivityAtThreshold;

    // Youtube player things
    FragmentManager fragmentManager;
    String defaultYoutubePlaylist = "https://youtube.com/playlist?list=PL2K75QWb9iU46N64aOyX0zmFSw4OF-0QH";
    YouTubePlayerFragment youTubePlayerFragment;
    YouTubePlayer myYoutubePlayer;
    Boolean inVideoSession;
    String playlistId;
    Boolean hiddenVideoOnce;
    Integer playlistIndex;

    View configPopupView;

    // Gaze things
    SeesoGazeTracker myGazeTracker;
    // a boolean for whether we intiate seeso gaze stuff at all
    // as well as allow certain UI elements to be used
    Boolean gazeEnabled = MainActivity.gazeEnabled;
    // this is for if we want to use gaze + scratch in a hangout session
    // we could have gaze enabled but just opt not to use it for whatever reason
    Boolean useGazeInSession;

    // Scratch things
    // these are only relevant if gazeEnabled = true
    Integer defaultScratchActivityLength; // TODO: eventually should be configurable by user
    WebView webView; // for displaying the scratch game -- which should be loaded as HTML
    Boolean inScratchSession;
    int stepX, stepY; // how much to update the onscreen cursor for each "key press"
    int startX, startY; // the expected cursor starting position for a scratch game
    int thresX, thresY; // how much change a gaze coordinate needs to change to update the cursor coordinate
    int onscreen_x; // the current x coordinate of the cursor in scratch
    int onscreen_y; // the current y coordinate or the cursor in scratch
    String x_move; // expected values are "right" or "left"-- for indicating which keypad input to initiate
    String y_move; // expected values are "up" or "down"-- for indicating which keypad input to initiate

    // we have to store both the URIs and filenames, because the URI is what we actually load, but the filename has meaning in the UI
    // we also keep a list of "temporary" values when the popup menu is open, and only update the "real" values if we save
    List<String> scratchURIs = Arrays.asList(new String[]{"", "", "", ""});
    List<String> scratchFileNames = Arrays.asList(new String[]{"", "", "", ""});
    List<String> temporaryScratchURIs = Arrays.asList(new String[]{"", "", "", ""});
    List<String> temporaryScratchFileNames = Arrays.asList(new String[]{"", "", "", ""});
    ActivityResultLauncher<Intent> scratchURIResult1;
    ActivityResultLauncher<Intent> scratchURIResult2;
    ActivityResultLauncher<Intent> scratchURIResult3;
    ActivityResultLauncher<Intent> scratchURIResult4;

    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.buddy_1);

        // list to track the user state
        stateOverTime = new ArrayList();

        // executor helps us know what actions to take
        executor = Executors.newSingleThreadScheduledExecutor();

        sharedPref = getSharedPreferences(SETTINGS_KEY, MODE_PRIVATE);

        // Game view is for animated character
        gameView = findViewById(R.id.gameView);
        gameView.setTag(TAG);
        gameView.setActionState(gameView.IDLE);

        if (gazeEnabled) {
            // initialize Seeso gaze tracking
            myGazeTracker = new SeesoGazeTracker(getApplicationContext(), TAG, initializationCallback);
            initScratchGazeTrackingValues();
        }

        // web view is for displaying youtube videos
        webView = findViewById(R.id.webView);
        webView.setVisibility(View.INVISIBLE);

        // booleans to help us keep track of what is going on
        inHangoutSession = false;
        inVideoSession = false;

        if (gazeEnabled) {
            useGazeInSession = false;
            inScratchSession = false;
        }

        // initialize voice over object
        voice = new Voiceover(getApplicationContext(), TAG);

        // set up youtube player things
        DEV_KEY = getApplicationContext().getResources().getString(R.string.YOUTUBE_API_KEY);
        fragmentManager = getFragmentManager();
        youTubePlayerFragment = new YouTubePlayerFragment();
        youTubePlayerFragment.initialize(DEV_KEY, youtubeListen);
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.add(R.id.youtubeFragment, youTubePlayerFragment);
        ft.hide(youTubePlayerFragment);
        ft.commit();
        playlistIndex = 0;
        hiddenVideoOnce = false;

        if (gazeEnabled) {
            // Activity results for retrieving and running scratch files from the file system
            initActivityResultIntentsForScratchFiles();
        }

        // -------------------
        // Retrieve saved preference values
        // -------------------

        String retrievedURIString = sharedPref.getString(PLAYLIST_ID_KEY, null);
        if (retrievedURIString != null ) {
            playlistId = retrievedURIString;
        } else {
            playlistId = defaultYoutubePlaylist;
        }

        if (gazeEnabled) {
            String retrievedScratchURIString = sharedPref.getString(SCRATCH_URIS_KEY, null);
            if (retrievedScratchURIString != null) {
                String[] splitIDs = retrievedScratchURIString.split(",");
                for (int i = 0; i < splitIDs.length; i++) {
                    scratchURIs.set(i, splitIDs[i]);
                    temporaryScratchURIs.set(i, splitIDs[i]);
                }
            }

            String retrievedScratchFilenamesString = sharedPref.getString(SCRATCH_FILENAMES_KEY, null);
            if (retrievedScratchFilenamesString != null) {
                String[] splitIDs = retrievedScratchFilenamesString.split(",");
                for (int i = 0; i < splitIDs.length; i++) {
                    scratchFileNames.set(i, splitIDs[i]);
                    temporaryScratchFileNames.set(i, splitIDs[i]);
                }
            }
        }

        // ---------------
        // Gaze tracking set up
        // ---------------

        if (gazeEnabled) {
            // this should call initGazeTracker(), and designated init callback, which will begin tracking
            Runnable initGazeRunnable = new Runnable() {
                public void run() {
                    Log.i(TAG, "start up gaze tracker");
                    myGazeTracker.startGazeTracker();
                }
            };

            // maybe not necessary?
            Runnable startGazeTrackRunnable = new Runnable() {
                public void run() {
                    Log.i(TAG, "starting gaze tracking");
                    myGazeTracker.startTracking();
                }
            };

            Runnable stopGazeTrackRunnable = new Runnable() {
                public void run() {
                    Log.i(TAG, "stopping gaze tracking");
                    myGazeTracker.stopTracking();
                }
            };

            // initialize gaze tracking immediately when the app starts up, and give time to get up before stopping
            executor.schedule(initGazeRunnable, 4000, TimeUnit.MILLISECONDS); // Gaze first starts 4 seconds in
            executor.schedule(stopGazeTrackRunnable, 6000, TimeUnit.MILLISECONDS); // Give it at least 2 seconds to initialize
        }

        // -------------------
        // Return Button
        // -------------------

        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);

        Button returnButton = findViewById(R.id.returnButton);
        returnButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "returning from activity");
                if (inHangoutSession) {
                    stopCamera();
                    if (gazeEnabled) {
                        myGazeTracker.releaseGaze();
                    }
                    if (executor != null) {
                        executor.shutdownNow();
                    }
                }
                Intent returnIntent = new Intent(Buddy1Activity.this, MainActivity.class);
                startActivity(returnIntent);
            }
        });


        // -------------------
        // Setup & Start Button + menu popup
        // -------------------
        Button setupButton = findViewById(R.id.setupButton);
        setupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater inflater = (LayoutInflater)
                        getSystemService(LAYOUT_INFLATER_SERVICE);
                View popupView = inflater.inflate(R.layout.buddy_setup_menu, null);
                final PopupWindow popupWindow = new PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
                popupWindow.setElevation(10.0f);
                popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

                Spinner sessionLengthDropdown = popupView.findViewById(R.id.spinner1);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, sessionLengths);
                sessionLengthDropdown.setAdapter(adapter);
                sessionLengthDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        Log.d(TAG, "setting session length to " + sessionLengths[i]);
                        sessionLength = sessionLengths[i];
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });

                if (!gazeEnabled) {
                    CheckBox gazeCheckbox = popupView.findViewById(R.id.gazeCheckBox);
                    gazeCheckbox.setEnabled(false);
                }

                Spinner modeDropdown = popupView.findViewById(R.id.spinner2);
                ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, modes);
                modeDropdown.setAdapter(modeAdapter);
                modeDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        Log.d(TAG, "setting mode to " + modes[i]);
                        mode = modes[i];
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                    }
                });

                Button cancelButton = popupView.findViewById(R.id.cancelButton);
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        useGazeInSession = false;
                        popupWindow.dismiss();
                    }
                });
                Button startButton = popupView.findViewById(R.id.startButton);
                startButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        popupWindow.dismiss();
                        useGazeInSession = false;
                        if (gazeEnabled) {
                            CheckBox gazeCheckbox = popupView.findViewById(R.id.gazeCheckBox);
                            if (gazeCheckbox.isChecked()) {
                                useGazeInSession = true;
                            }
                        }
                        // start a hangout session! This is where stuff actuall starts happening
                        initiateHangoutSession();
                    }
                });
            }
        });

        // -------------------
        // Config Button + menu popup
        // -------------------
        configPopupView = inflater.inflate(R.layout.buddy1_config, null);
        Button configButton = findViewById(R.id.configButton);
        configButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final PopupWindow popupWindow = new PopupWindow(configPopupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
                popupWindow.setElevation(10.0f);

                popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

                EditText playlistText = configPopupView.findViewById(R.id.editVideo1);
                playlistText.setText(playlistId);

                Button addScratchFileButton1 = configPopupView.findViewById(R.id.scratchFileButton1);
                Button addScratchFileButton2 = configPopupView.findViewById(R.id.scratchFileButton2);
                Button addScratchFileButton3 = configPopupView.findViewById(R.id.scratchFileButton3);
                Button addScratchFileButton4 = configPopupView.findViewById(R.id.scratchFileButton4);
                Button removeScratchFileButton1 = configPopupView.findViewById(R.id.removeScratchFileButton1);
                Button removeScratchFileButton2 = configPopupView.findViewById(R.id.removeScratchFileButton2);
                Button removeScratchFileButton3 = configPopupView.findViewById(R.id.removeScratchFileButton3);
                Button removeScratchFileButton4 = configPopupView.findViewById(R.id.removeScratchFileButton4);

                if (gazeEnabled) {
                    setScratchConfigTextFields();

                    // TODO: probably a more streamlined way to add and remove all of these scratch fields?
                    addScratchFileButton1.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            scratchURIResult1.launch(fileUploaderIntent());
                        }
                    });

                    addScratchFileButton2.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            scratchURIResult2.launch(fileUploaderIntent());
                        }
                    });

                    addScratchFileButton3.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            scratchURIResult3.launch(fileUploaderIntent());
                        }
                    });

                    addScratchFileButton4.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            scratchURIResult4.launch(fileUploaderIntent());
                        }
                    });

                    removeScratchFileButton1.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            EditText scratchFilePathText1 = configPopupView.findViewById(R.id.editScratchFilePath1);
                            scratchFilePathText1.setText("");
                            temporaryScratchURIs.set(0, "");
                            temporaryScratchFileNames.set(0, "");
                        }
                    });

                    removeScratchFileButton2.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            EditText scratchFilePathText2 = configPopupView.findViewById(R.id.editScratchFilePath2);
                            scratchFilePathText2.setText("");
                            temporaryScratchURIs.set(1, "");
                            temporaryScratchFileNames.set(1, "");
                        }
                    });

                    removeScratchFileButton3.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            EditText scratchFilePathText3 = configPopupView.findViewById(R.id.editScratchFilePath3);
                            scratchFilePathText3.setText("");
                            temporaryScratchURIs.set(2, "");
                            temporaryScratchFileNames.set(2, "");
                        }
                    });

                    removeScratchFileButton4.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            EditText scratchFilePathText4 = configPopupView.findViewById(R.id.editScratchFilePath4);
                            scratchFilePathText4.setText("");
                            temporaryScratchURIs.set(3, "");
                            temporaryScratchFileNames.set(3, "");
                        }
                    });
                } else {
                    addScratchFileButton1.setEnabled(false);
                    addScratchFileButton2.setEnabled(false);
                    addScratchFileButton3.setEnabled(false);
                    addScratchFileButton4.setEnabled(false);
                    removeScratchFileButton1.setEnabled(false);
                    removeScratchFileButton2.setEnabled(false);
                    removeScratchFileButton3.setEnabled(false);
                    removeScratchFileButton4.setEnabled(false);
                }

                Button cancelButton = configPopupView.findViewById(R.id.cancelButton);
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (gazeEnabled) {
                            // if cancelling, set values back to what they were before
                            temporaryScratchFileNames.set(0, scratchFileNames.get(0));
                            temporaryScratchFileNames.set(1, scratchFileNames.get(1));
                            temporaryScratchFileNames.set(2, scratchFileNames.get(2));
                            temporaryScratchFileNames.set(3, scratchFileNames.get(3));

                            temporaryScratchURIs.set(0, scratchURIs.get(0));
                            temporaryScratchURIs.set(1, scratchURIs.get(1));
                            temporaryScratchURIs.set(2, scratchURIs.get(2));
                            temporaryScratchURIs.set(3, scratchURIs.get(3));
                        }
                        popupWindow.dismiss();
                    }
                });

                Button saveButton = configPopupView.findViewById(R.id.saveButton);
                saveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String newPlaylistId = String.valueOf(playlistText.getText());
                        if (newPlaylistId == "") {
                            playlistId = defaultYoutubePlaylist;
                        }
                        playlistId = newPlaylistId;

                        // then update what we store in our persistent preferences
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString(PLAYLIST_ID_KEY, playlistId);

                        if (gazeEnabled) {
                            // actually set the values from the temporary values
                            scratchFileNames.set(0, temporaryScratchFileNames.get(0));
                            scratchFileNames.set(1, temporaryScratchFileNames.get(1));
                            scratchFileNames.set(2, temporaryScratchFileNames.get(2));
                            scratchFileNames.set(3, temporaryScratchFileNames.get(3));

                            scratchURIs.set(0, temporaryScratchURIs.get(0));
                            scratchURIs.set(1, temporaryScratchURIs.get(1));
                            scratchURIs.set(2, temporaryScratchURIs.get(2));
                            scratchURIs.set(3, temporaryScratchURIs.get(3));

                            // store these as comma separated strings-- this will be expected when parsing out later upon retrieval
                            String filenamesString = scratchFileNames.get(0) + "," + scratchFileNames.get(1) + "," + scratchFileNames.get(2) + "," + scratchFileNames.get(3);
                            editor.putString(SCRATCH_FILENAMES_KEY, filenamesString);

                            String uriString = scratchURIs.get(0) + "," + scratchURIs.get(1) + "," + scratchURIs.get(2) + "," + scratchURIs.get(3);
                            editor.putString(SCRATCH_URIS_KEY, uriString);
                        }

                        editor.commit();

                        // make sure we reset where we are in a playlist, since it may be a different length
                        if (myYoutubePlayer != null) {
                            playlistIndex = 0;
                            myYoutubePlayer.cuePlaylist(getYoutubePlaylistIDFromString(playlistId), playlistIndex, 0);
                        }
                        popupWindow.dismiss();
                    }
                });
            }
        });

    }

    private void initiateHangoutSession() {
        Log.i(TAG, "beginning session in " + mode + " mode for " + sessionLength + " minutes");
        inHangoutSession = true; // v important for checking expected execution behaviors!
        gameView.setInSession(true); // change border color on screen when in session
        setThresholdsBasedOnMode();
        ArrayList<String> activityPlan = makeActivityPlan();
        Integer atActivityIndex = 0;

        Thread helloVoiceover = voice.playHello();
        // wait until voice over is done before beginning the session
        try {
            helloVoiceover.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        startCamera(); // start face tracking

        // set up and schedule the shut down/ending of this session
        Runnable endSessionRunnable = new Runnable() {
            public void run() {
                Log.i(TAG, "ending hangout session...");
                inHangoutSession = false; // v important to set this! It will be checked elsewhere
                // wait until all voiceover and videos are done
                while (voice.getIsVoiceoverPlaying() || inActivitySession()) {
                    // wait
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopCamera(); //stop MLKit
                        gameView.setInSession(false); // border color goes away when done with session
                        Log.i(TAG, "ended hangout session in UI thread");
                    }
                });;
                voice.playGoodbye();

                Log.i(TAG, "shutting down executor");
                // This will try to shut down all processes in the executor--including the getStateRunnable.
                // However, it only guarantees a "best try." We need to be sure to only
                // be checking state and running sessions if hangout is in session.
                executor.shutdownNow();

            }
        };
        // length of session should have been set up in config menu when starting the session
        executor.schedule(endSessionRunnable, Integer.parseInt(sessionLength), TimeUnit.MINUTES);

        // set up and schedule the method to get user state as we run the session
        Runnable getStateRunnable = new Runnable() {
            @Override
            public void run() {
                if (faceAnalyzer != null && inHangoutSession) {
                    // don't update state info while watching video or listening to voice over
                    // this is so we don't immediately initiate something after every piece of media
                    if (!inActivitySession() && !voice.getIsVoiceoverPlaying()) {
                        currentState = faceAnalyzer.getCurrentState();

                        // pop first item out once we have reached configured size
                        if (stateOverTime.size() >= stateWindowToUse) {
                            stateOverTime.remove(0);
                        }
                        stateOverTime.add(currentState);
                        Log.i(TAG, String.join(",", stateOverTime));
                    }

                    // TODO: this probably needs more nuanced work
                    if (checkIfStateAtThreshold()) {
                        stateOverTime = new ArrayList(); // reset window
                        Log.i(TAG, "can launch youtube video...");
                        if (!inActivitySession() && !voice.getIsVoiceoverPlaying()) {
                             if (activityPlan.get(atActivityIndex) == "video") {
                                 runVideoSession();
                             } else if (activityPlan.get(atActivityIndex) == "scratch" && gazeEnabled) {
                                 runScratchSession();
                             } else {
                                 runVideoSession();
                             }
                        }

                    }
                }
            }
        };
        // call every second
        executor.scheduleAtFixedRate(getStateRunnable, 0, 1, TimeUnit.SECONDS);
    }

    private ArrayList<String> makeActivityPlan() {
        // initialize a list of strings that represent which activities should be done, and in what order
        // length of list should be appropriate for length of session
        // use stateWindowToUse and sessionLength (minutes) to determine the length
        Integer itemsInPlan = (Integer.parseInt(sessionLength) * 60) / stateWindowToUse;
        Log.i(TAG, "going to put " + itemsInPlan + " items in activity plan");

        // initial list is all videos
        ArrayList<String> activities = new ArrayList<String>();
        for (int i = 0; i < itemsInPlan; i++) {
            activities.add("video");
        }

        Log.i(TAG, "activity plan: " + activities);


        // if scratch/gaze is enabled for session
        if (gazeEnabled && useGazeInSession) {
            // how many scratch game files do we even have to work with?
            Integer scratchCount = 0;
            for (int i = 0; i < scratchURIs.size(); i++) {
                if (scratchURIs.get(i) != "") {
                    scratchCount++;
                }
            }
            Log.i(TAG, "there are " + scratchCount + " scratch uris " + scratchURIs);

            // if we have at least or more than half of our total activities
            // we will interleave videos and scratch games
            if (scratchCount >= (itemsInPlan / 2)) {
                Log.i(TAG, "setting every other based on being >= " + (itemsInPlan/2));
                for (int i = 0; i < itemsInPlan; i++) {
                    if (i % 2 == 0) {
                        activities.set(i, "scratch");
                    }
                }
            } else if (scratchCount == 1) {
                // if we only have one, add randomly in one place
                Log.i(TAG, "only 1 scratch game, adding in a random place");
                Random rand = new Random();
                activities.set(rand.nextInt(itemsInPlan), "scratch");
            } else {
                // pick n random integers between 0 and itemsInPlan-1, where n is the number of scratch activities
                // replace the index at the found
                ArrayList<Integer> insertIntoIndices = getNRandomNoRepeats(itemsInPlan, scratchCount);
                Log.i(TAG, "adding scratch game to indices " + insertIntoIndices);
                for (int i = 0; i < insertIntoIndices.size(); i++) {
                    activities.set(insertIntoIndices.get(i), "scratch");
                }
            }
        }
        Log.i(TAG, "activity plan: " + activities);

        return activities;
    }

    private ArrayList<Integer> getNRandomNoRepeats(int rangeSize, int n) {
        ArrayList<Integer> list = new ArrayList<Integer>();
        for(int i = 1; i <= rangeSize; i++) {
            list.add(i);
        }

        Random rand = new Random();
        while(list.size() > n) {
            int index = rand.nextInt(list.size());
            list.remove(index);
        }
        return list;
    }

    private void runScratchSession() {
        // this may or may not be working (wip)
        Log.i(TAG, "running scratch session");
        inScratchSession = true;
        Thread introThread = voice.playIntro();
        // wait until voice over is complete to begin video
        try {
            introThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "launching scratch...");
        // TODO: track scratchURI index
        myGazeTracker.startTracking();
        loadWebView(Uri.parse(scratchURIs.get(0)), 1);
        Log.i(TAG, "done loading web view");
        Runnable endScratchSessionRunnable = new Runnable() {
            public void run() {
                Log.i(TAG, "ending scratch session");
                myGazeTracker.stopTracking();
                webView.setVisibility(View.INVISIBLE);
                inScratchSession = false;
            }
        };
        // TODO: use configured activity length if specified
        Log.i(TAG, "scheduling end");
        executor.schedule(endScratchSessionRunnable, defaultScratchActivityLength, TimeUnit.SECONDS);
        Log.i(TAG, "done scheduling");
    }

    private void runVideoSession() {
        Log.i(TAG, "running video session");
        inVideoSession = true;
        Thread introThread = voice.playIntro();
        // wait until voice over is complete to begin video
        try {
            introThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "launching youtube video...");
        showAndPlayVideoPlayer();
        // don't need to worry about "scheduling" any actions at the end of the video
        // youtube player callbacks will handle this (see later code)
    }

    private void showAndPlayVideoPlayer() {
        Log.i(TAG, "showing youtube video");

        if (hiddenVideoOnce) { // we have started up previously, need to recreate the fragment
            // had some trouble with the player fragments, this is the best known workaround
            Log.i(TAG, "already torn down youtube fragment once, re-creating now...");
            youTubePlayerFragment = new YouTubePlayerFragment();
            youTubePlayerFragment.initialize(DEV_KEY, youtubeListen);

            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.add(R.id.youtubeFragment, youTubePlayerFragment);
            ft.commit();
            // since we are initializing the player fragment here,
            // we will load the playlist in initializer (based on the hiddenOnce var)
            // and it will automatically begin playing
        } else { // only do this on start up
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.show(youTubePlayerFragment);
            ft.commit();
            // we cued the playlist on start up, so now we have to initiate playing
            myYoutubePlayer.play();
        }

        Log.i(TAG, "showing youtube video complete");
    }

    // this is for ending a video while the hangout session is still running
    private void endVideoSession() {
        Log.i(TAG, "ending video session");
        myYoutubePlayer.pause();
        hideVideoPlayer();
        playlistIndex++;
        inVideoSession = false;
    }

    private void hideVideoPlayer() {
        Log.i(TAG, "hide video fragment");
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.remove(youTubePlayerFragment);
        ft.commit();
        hiddenVideoOnce = true;

        Log.i(TAG, "hide video fragment complete");
    }

    private boolean inActivitySession() {
        if (inHangoutSession) {
            if (inVideoSession || inScratchSession) {
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean checkIfStateAtThreshold() {
        if (stateOverTime.size() < stateWindowToUse) {
            return false;
        }

        double countOfInitiateState = 0;
        for (int i = 0; i < stateOverTime.size(); i++) {
            if (stateOverTime.get(i) == initiateActivityAfterState) {
                countOfInitiateState++;
            }
        }
        double thresholdFound = countOfInitiateState / stateWindowToUse;
        Log.i(TAG, "thresholdFound: " + countOfInitiateState + "/" + stateWindowToUse + " = " + thresholdFound + " >= " + initiateActivityAtThreshold);
        if (thresholdFound >= initiateActivityAtThreshold) {

            return true;
        }

        return false;
    }

    private void setThresholdsBasedOnMode() {
         SharedPreferences stateSharedPref = getSharedPreferences(StateDetectionActivity.SETTINGS_KEY, MODE_PRIVATE);
        if (mode == modes[0]) { // debug mode
            initiateActivityAfterState = stateSharedPref.getString(StateDetectionActivity.DEBUG_TRIGGER_STATE_KEY, null);
            if (initiateActivityAfterState == null ) {
                initiateActivityAfterState = StateDetectionActivity.getDefaultDebugTriggerState();
            }
            String stateWindowToUseStr = stateSharedPref.getString(StateDetectionActivity.DEBUG_STATE_WINDOW_KEY, null);
            if (stateWindowToUseStr == null) {
                stateWindowToUse = StateDetectionActivity.getDefaultDebugStateWindow();
            } else {
                stateWindowToUse = Integer.parseInt(stateWindowToUseStr);
            }
            String initiateActivityAtThresholdStr = stateSharedPref.getString(StateDetectionActivity.DEBUG_TRIGGER_ACTIVITY_THRESHOLD_KEY, null);
            if (initiateActivityAtThresholdStr == null) {
                initiateActivityAtThreshold = StateDetectionActivity.getDefaultDebugTriggerStateThreshold();
            } else {
                initiateActivityAtThreshold = Float.parseFloat(initiateActivityAtThresholdStr);
            }
            defaultScratchActivityLength = 10; // TODO: eventually these stored with each scratch file added to app
        } else {
            initiateActivityAfterState = stateSharedPref.getString(StateDetectionActivity.TRIGGER_STATE_KEY, null);
            if (initiateActivityAfterState == null ) {
                initiateActivityAfterState = StateDetectionActivity.getDefaultTriggerState();
            }
            String stateWindowToUseStr = stateSharedPref.getString(StateDetectionActivity.STATE_WINDOW_KEY, null);
            if (stateWindowToUseStr == null) {
                stateWindowToUse = StateDetectionActivity.getDefaultStateWindow();
            } else {
                stateWindowToUse = Integer.parseInt(stateWindowToUseStr);
            }
            String initiateActivityAtThresholdStr = stateSharedPref.getString(StateDetectionActivity.TRIGGER_ACTIVITY_THRESHOLD_KEY, null);
            if (initiateActivityAtThresholdStr == null) {
                initiateActivityAtThreshold = StateDetectionActivity.getDefaultTriggerStateThreshold();
            } else {
                initiateActivityAtThreshold = Float.parseFloat(initiateActivityAtThresholdStr);
            }
            defaultScratchActivityLength = 30;  // TODO: eventually these stored with each scratch file added to app
        }
        Log.i(TAG, "Thresholds set based on mode " + mode + ": " + initiateActivityAfterState + ", " + initiateActivityAtThreshold + ", " + stateWindowToUse);
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.pause();
    }


    // ------------------------------------------
    // MLKit methods
    // ------------------------------------------
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

    private void stopCamera() {
        Log.i(TAG, "stopping camera");
        try {
            ProcessCameraProvider cameraProvider = this.cameraProviderFuture.get();
            cameraProvider.unbindAll();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        Log.i(TAG, "bindImageAnalysis");
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
                .build();
        faceAnalyzer = new MLKitFaceAnalyzer(this, getLifecycle(), TAG, faceAnalyzer.LISTENER_OPTION2);

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), faceAnalyzer);
        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageAnalysis);
        preview.setSurfaceProvider(null); // Do not display camera feed. This MUST happen after binding
    }

    // ------------------------------------------
    // Youtube player methods
    // ------------------------------------------

    YouTubePlayer.OnInitializedListener youtubeListen = new YouTubePlayer.OnInitializedListener() {
        @Override
        public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
            Log.i(TAG, "init youtube player 1  success");
            myYoutubePlayer = youTubePlayer;
            if (hiddenVideoOnce) {
                // in the event that we are re-starting, we want the video to start playing immediately
                Log.i(TAG, "start playing!");
                myYoutubePlayer.loadPlaylist(getYoutubePlaylistIDFromString(playlistId), playlistIndex, 0);
            } else {
                // if we are initializing on startup, only cue the playlist, we will call .play() later
                myYoutubePlayer.cuePlaylist(getYoutubePlaylistIDFromString(playlistId), playlistIndex, 0);
            }
            myYoutubePlayer.setPlayerStateChangeListener(playerStateChangeListener);
            myYoutubePlayer.setPlaybackEventListener(playbackEventListener);
            myYoutubePlayer.setPlaylistEventListener(playlistListener);

        }

        @Override
        public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
            Log.i(TAG, "init youtube player 1 failure" + youTubeInitializationResult);
        }
    };

    YouTubePlayer.PlaylistEventListener playlistListener = new YouTubePlayer.PlaylistEventListener() {
        @Override
        public void onPrevious() {
            Log.i(TAG, "playlistListener onPrevious");
        }

        @Override
        public void onNext() {
            // this will be called when any video besides the last is complete in a playlist
            Log.i(TAG, "playlistListener onNext");
            if (inVideoSession) {
               endVideoSession();
               // if the hangout session timer ended while the video was playing,
               // we want to the skip the outro voiceover
               if (inHangoutSession) {
                   voice.playOutro(); // don't block on this, it screws up closing the youtube fragment
               }
            }
        }

        @Override
        public void onPlaylistEnded() {
            // this will be called when the last video of the playlist has finished
            Log.i(TAG, "playlistListener onPlaylistEnded");
            if (inVideoSession) {
                endVideoSession();
                // if the hangout session timer ended while the video was playing,
                // we want to the skip the outro voiceover
                if (inHangoutSession) {
                    voice.playOutro(); // don't block on this, it screws up closing the youtube fragment
                }
            }
            playlistIndex = 0;
        }
    };

    YouTubePlayer.PlaybackEventListener playbackEventListener = new YouTubePlayer.PlaybackEventListener() {
        @Override
        public void onPlaying() {
            Log.i(TAG, "playbackEventListener onPlaying");
        }

        @Override
        public void onPaused() {
            Log.i(TAG, "playbackEventListener onPaused");
        }

        @Override
        public void onStopped() {
            Log.i(TAG, "playbackEventListener onStopped");
        }

        @Override
        public void onBuffering(boolean b) {
            Log.i(TAG, "playbackEventListener onBuffering");
        }

        @Override
        public void onSeekTo(int i) {
            Log.i(TAG, "playbackEventListener onSeekTo");
        }
    };

    YouTubePlayer.PlayerStateChangeListener playerStateChangeListener = new YouTubePlayer.PlayerStateChangeListener() {
        @Override
        public void onLoading() {
            Log.i(TAG, "playerStateChangeListener onLoading");
        }

        @Override
        public void onLoaded(String s) {
            Log.i(TAG, "playerStateChangeListener onLoaded");
        }

        @Override
        public void onAdStarted() {
            Log.i(TAG, "playerStateChangeListener onAdStarted");
        }

        @Override
        public void onVideoStarted() {
            Log.i(TAG, "playerStateChangeListener onVideoStarted");
        }

        @Override
        public void onVideoEnded() {
            Log.i(TAG, "playerStateChangeListener onVideoEnded");
        }

        @Override
        public void onError(YouTubePlayer.ErrorReason errorReason) {
            Log.i(TAG, "playerStateChangeListener onError");
        }
    };

    private String getYoutubePlaylistIDFromString(String url) {
        if (url.length() == 34) {
            return url;
        } else if (url.length() > 34) {
            String youtubeID = url.substring(url.length() - 34);
            return youtubeID;
        }
        return "";
    }

    // -----------------------------
    // Scratch things
    // -----------------------------

    private void setScratchConfigTextFields() {
        EditText scratchFilePathText1 = configPopupView.findViewById(R.id.editScratchFilePath1);
        EditText scratchFilePathText2 = configPopupView.findViewById(R.id.editScratchFilePath2);
        EditText scratchFilePathText3 = configPopupView.findViewById(R.id.editScratchFilePath3);
        EditText scratchFilePathText4 = configPopupView.findViewById(R.id.editScratchFilePath4);

        scratchFilePathText1.setText(temporaryScratchFileNames.get(0));
        scratchFilePathText2.setText(temporaryScratchFileNames.get(1));
        scratchFilePathText3.setText(temporaryScratchFileNames.get(2));
        scratchFilePathText4.setText(temporaryScratchFileNames.get(3));
    }

    private void handleScratchFileRetrieval(ActivityResult result, Integer index) {
        // callback method when we select a file in the android file system
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent data = result.getData();
            Uri uri = data.getData();
            DocumentFile df = DocumentFile.fromSingleUri(getApplicationContext(), uri);
            String fileName = df.getName();  // that contains the filename
            Log.i(TAG, "setting scratch uri at index" + index + ": " + uri + " " + fileName);
            temporaryScratchFileNames.set(index, fileName);
            temporaryScratchURIs.set(index, uri.toString());
            setScratchConfigTextFields(); // update text fields
        }
    }

    private void loadWebView(Uri uri, int option) {
        // this may or may not be currently working
        Log.i(TAG, "load web view");
        // TODO: run on ui thread?
        webView.setVisibility(View.VISIBLE);
        Log.i(TAG, "getting uri as html");
        String data = uriToHTMLString(uri, option);
        Log.i(TAG, "got uri as html");
        webView.getSettings().setJavaScriptEnabled(true);
        try {
            webView.loadDataWithBaseURL(null, data, "text/html", "utf-8", null);
        } catch (Exception e) {
            Log.w(TAG, "error loading data for web view with uri: " + uri.toString());
        }
    }

    private String uriToHTMLString(Uri uri, int option) {
        ContentResolver contentResolver = getContentResolver();

        if (option == 1) { // only do this when loading in a new uri from the file picker (for some reason)
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION );
        }
        String dataString = "";
        try {
            InputStream inputStream = contentResolver.openInputStream(uri);
            byte[] inputData = new byte[inputStream.available()];
            inputStream.read(inputData);
            dataString = new String(inputData);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataString;
    }

    private void initScratchGazeTrackingValues() {
        // TODO: make these configurable preferences
        String stepXString = sharedPref.getString(STEP_X, null);
        if (stepXString != null) {
            stepX = Integer.parseInt(stepXString);
        } else {
            stepX = 15;
        }
        Log.i(TAG, "step X is " + stepX);
        String stepYString = sharedPref.getString(STEP_Y, null);
        if (stepYString != null) {
            stepY = Integer.parseInt(stepYString);
        } else {
            stepY = 15;
        }


        String startXString = sharedPref.getString(START_X, null);
        if (startXString != null) {
            startX = Integer.parseInt(startXString);
        } else {
            startX = 800;
        }
        String startYString = sharedPref.getString(START_Y, null);
        if (startYString != null) {
            startY = Integer.parseInt(startYString);
        } else {
            startY = 600;
        }

        String thresXString = sharedPref.getString(THRES_X, null);
        if (thresXString != null) {
            thresX = Integer.parseInt(thresXString);
        } else {
            thresX = 20;
        }
        String thresYString = sharedPref.getString(THRES_Y, null);
        if (thresYString != null) {
            thresY = Integer.parseInt(thresYString);
        } else {
            thresY = 40;
        }

        onscreen_x = startX;
        onscreen_y = startY;

        x_move = "";
        y_move = "";
    }

    private Intent fileUploaderIntent() {
        // method to allow us to open file picker for android
        Log.i(TAG, "opening file chooser...");
        Intent fileIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
        fileIntent.setType("*/*");
        fileIntent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        fileIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        return fileIntent;
    }

    private void initActivityResultIntentsForScratchFiles() {
        // TODO: there is probably a better way to initialize/set these. at least do this in a method call
        // they all do the same thing but with a different index so we know which file is which
        scratchURIResult1 = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        handleScratchFileRetrieval(result, 0);
                    }
                });

        scratchURIResult2 = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        handleScratchFileRetrieval(result, 1);
                    }
                });

        scratchURIResult3 = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        handleScratchFileRetrieval(result, 2);
                    }
                });

        scratchURIResult4 = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        handleScratchFileRetrieval(result, 3);
                    }
                });
    }

    // -----------------------------
    // Seeso things
    // -----------------------------

    private InitializationCallback initializationCallback = new InitializationCallback() {
        @Override
        public void onInitialized(GazeTracker gazeTracker, InitializationErrorType error) {
            Log.i(TAG, "init callback");
            if (gazeTracker != null) {
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
            if (inScratchSession) {
                x_move = "";
                y_move = "";

                Log.d(TAG, "gaze coords: " + gazeInfo.x + ", " + gazeInfo.y + " onscreen coords: " + onscreen_x + ", " + onscreen_y + " -- " + x_move + ", " + y_move);

                if (!Float.isNaN(gazeInfo.x) && !Float.isNaN(gazeInfo.y)) {
                    float x_abs = Math.abs(gazeInfo.x - onscreen_x);
                    if (x_abs > thresX) {
                        if (gazeInfo.x > onscreen_x) {
                            onscreen_x = onscreen_x + stepX;
                            x_move = "right";
                        } else if (gazeInfo.x < onscreen_x) {
                            onscreen_x = onscreen_x - stepX;
                            x_move = "left";
                        }
                    }

                    float y_abs = Math.abs(gazeInfo.y - onscreen_y);
                    if (y_abs > thresY) {
                        if (gazeInfo.y > onscreen_y) {
                            onscreen_y = onscreen_y + stepY;
                            y_move = "down";
                        } else if (gazeInfo.y < onscreen_y) {
                            onscreen_y = onscreen_y - stepY;
                            y_move = "up";
                        }
                    }
                    Log.d(TAG, "move: " + x_move + ", " + y_move);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (x_move == "right") {
                                webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
                            }
                            if (x_move == "left") {
                                webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
                            }
                            if (y_move == "down") {
                                webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
                            }
                            if (y_move == "up") {
                                webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
                            }
                        }
                    });


                }
            } else {
                Log.d(TAG, "No session happening -- gaze coords: " + gazeInfo.x + ", " + gazeInfo.y);
            }
        }
    };
}


