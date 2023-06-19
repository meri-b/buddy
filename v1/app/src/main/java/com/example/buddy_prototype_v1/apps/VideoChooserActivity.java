package com.example.buddy_prototype_v1.apps;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;


import com.example.buddy_prototype_v1.MainActivity;
import com.example.buddy_prototype_v1.R;
import com.example.buddy_prototype_v1.tools.SeesoGazeTracker;
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;

import java.util.Arrays;
import java.util.List;

import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.callback.InitializationCallback;
import camp.visual.gazetracker.constant.InitializationErrorType;
import camp.visual.gazetracker.gaze.GazeInfo;
import camp.visual.gazetracker.state.ScreenState;
import camp.visual.gazetracker.util.ViewLayoutChecker;

public class VideoChooserActivity  extends YouTubeBaseActivity {
    String DEV_KEY;
    String SETTINGS_KEY = "videoChooserSettings";
    String TAG = "VideoChooserActivity";
    String VIDEO_IDS_KEY = "videoIDs";
    String START_BUFFER_KEY = "startBuffer";
    String STOP_BUFFER_KEY = "stopBuffer";

    ViewLayoutChecker viewLayoutChecker = new ViewLayoutChecker();
    YouTubePlayerView youTubePlayerView1;
//    YouTubePlayerView youTubePlayerView2;
    YouTubePlayer myPlayer1;
    YouTubePlayer.PlayerStateChangeListener playerStateChangeListener;
//    YouTubePlayer myPlayer2;
    int screenWidth;
    View mView;

    int bufferCounter;
    Integer startBuffer;
    Integer stopBuffer;

    SeesoGazeTracker myGazeTracker;
    SharedPreferences sharedPref;

    List<String> videoIDs = Arrays.asList("", "", "", "", "");
    int onVideo = 0;


    @SuppressLint("ResourceType")
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_chooser_activity);

        DEV_KEY = getApplicationContext().getResources().getString(R.string.YOUTUBE_API_KEY);

        sharedPref = getSharedPreferences(SETTINGS_KEY, MODE_PRIVATE);
        String retrievedURIString = sharedPref.getString(VIDEO_IDS_KEY, null);
        if (retrievedURIString != null) {
            String[] splitIDs = retrievedURIString.split(",");
            for (int i = 0; i < splitIDs.length; i++) {
                videoIDs.set(i, splitIDs[i]);
            }
        }

        String retrievedStartBuffer = sharedPref.getString(START_BUFFER_KEY, null);
        if (retrievedStartBuffer != null) {
            startBuffer = Integer.parseInt(retrievedStartBuffer);
        } else {
            startBuffer = 10;
        }

        String retrievedStopBuffer = sharedPref.getString(STOP_BUFFER_KEY, null);
        if (retrievedStopBuffer != null) {
            stopBuffer = Integer.parseInt(retrievedStopBuffer);
        } else {
            stopBuffer = 80;
        }
        Log.i(TAG, "start bufffer " + startBuffer + " stop " + stopBuffer);



        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;

        bufferCounter = 0;

        myGazeTracker = new SeesoGazeTracker(getApplicationContext(), TAG, initializationCallback);


        playerStateChangeListener = new YouTubePlayer.PlayerStateChangeListener() {
            @Override
            public void onLoading() {

            }

            @Override
            public void onLoaded(String s) {

            }

            @Override
            public void onAdStarted() {

            }

            @Override
            public void onVideoStarted() {

            }

            @Override
            public void onVideoEnded() {
                Log.i(TAG, "onVideoEnded " + onVideo);
                if (onVideo < videoIDs.size()-1) {
                    onVideo = onVideo + 1;
                    String nextVideo = videoIDs.get(onVideo);
                    getIDAndLoadVideoOrPlaylist(nextVideo);
                } else if (onVideo == videoIDs.size()) {
                    onVideo = 0;
                }
            }

            @Override
            public void onError(YouTubePlayer.ErrorReason errorReason) {

            }
        };

        Button returnButton = findViewById(R.id.returnButton);
        returnButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                myGazeTracker.releaseGaze();
                Intent returnIntent = new Intent(VideoChooserActivity.this, MainActivity.class);
                startActivity(returnIntent);
            }
        });

        Button configButton = findViewById(R.id.configButton);
        configButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater inflater = (LayoutInflater)
                        getSystemService(LAYOUT_INFLATER_SERVICE);
                View popupView = inflater.inflate(R.layout.video_choose_config, null);
                final PopupWindow popupWindow = new PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
                popupWindow.setElevation(10.0f);

                popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

                EditText video1Text = popupView.findViewById(R.id.editVideo1);
                EditText video2Text = popupView.findViewById(R.id.editVideo2);
                EditText video3Text = popupView.findViewById(R.id.editVideo3);
                EditText video4Text = popupView.findViewById(R.id.editVideo4);
                EditText video5Text = popupView.findViewById(R.id.editVideo5);

                EditText startBufferText = popupView.findViewById(R.id.editStartBuffer);
                EditText stopBufferText = popupView.findViewById(R.id.editStopBuffer);

                video1Text.setText(videoIDs.get(0));
                video2Text.setText(videoIDs.get(1));
                video3Text.setText(videoIDs.get(2));
                video4Text.setText(videoIDs.get(3));
                video5Text.setText(videoIDs.get(4));

                startBufferText.setText(startBuffer.toString());
                stopBufferText.setText(stopBuffer.toString());

                Button cancelButton = popupView.findViewById(R.id.cancelButton);
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        popupWindow.dismiss();
                    }
                });

                Button saveButton = popupView.findViewById(R.id.saveButton);
                saveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.i(TAG, "saving video urls");
                        // save stuff
                        String vid1 = String.valueOf(video1Text.getText());
                        String vid2 = String.valueOf(video2Text.getText());
                        String vid3 = String.valueOf(video3Text.getText());
                        String vid4 = String.valueOf(video4Text.getText());
                        String vid5 = String.valueOf(video5Text.getText());

                        videoIDs.set(0, vid1);
                        videoIDs.set(1, vid2);
                        videoIDs.set(2, vid3);
                        videoIDs.set(3, vid4);
                        videoIDs.set(4, vid5);

                        startBuffer = Integer.parseInt(String.valueOf(startBufferText.getText()));
                        stopBuffer = Integer.parseInt(String.valueOf(stopBufferText.getText()));

                        // combine text strings into one string and save
                        SharedPreferences.Editor editor = sharedPref.edit();
                        String idString = vid1 + "," + vid2 + "," + vid3 + "," + vid4 + "," + vid5;
                        editor.putString(VIDEO_IDS_KEY, idString);
                        editor.putString(START_BUFFER_KEY, String.valueOf(startBufferText.getText()));
                        editor.putString(STOP_BUFFER_KEY, String.valueOf(stopBufferText.getText()));
                        editor.commit();

                        if (!vid1.isEmpty()) {
                            onVideo = 0;
                        } else if (!vid2.isEmpty()) {
                            onVideo = 1;
                        } else if (!vid3.isEmpty()) {
                            onVideo = 2;
                        } else if (!vid4.isEmpty()) {
                            onVideo = 3;
                        } else {
                            onVideo = 4;
                        }
                        getIDAndLoadVideoOrPlaylist(videoIDs.get(onVideo));

                        popupWindow.dismiss();
                    }
                });
            }
        });


        youTubePlayerView1 = (YouTubePlayerView) findViewById(R.id.videoView1);
        YouTubePlayer.OnInitializedListener onInitializedListener1 = new YouTubePlayer.OnInitializedListener(){
            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
                Log.i(TAG, "init youtube player 1  success");
//                youTubePlayer.loadVideo("I5cFBi02O34");
                myPlayer1 = youTubePlayer;
                getIDAndLoadVideoOrPlaylist(videoIDs.get(onVideo));
                myPlayer1.setPlayerStateChangeListener(playerStateChangeListener);
            }

            @Override
            public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
                Log.i(TAG, "init youtube player 1 failure" + youTubeInitializationResult);
            }
        };
        youTubePlayerView1.initialize(DEV_KEY, onInitializedListener1);
        myGazeTracker.startGazeTracker();
    }

//    private void loadVideosFromURI() {
//        List<String> ids = new ArrayList<String>();
//        for (int i = 0; i < videoIDs.size(); i++) {
//            // strip prefix
//            String youtubeURL = videoIDs.get(i);
//            Log.i(TAG, youtubeURL);
//            String yID = getIDFromVideoOrPlaylistString(youtubeURL);
//            if (yID != "") {
//                ids.add(yID);
//                Log.i(TAG, yID);
//            }
//        }
//
//    }


    private void getIDAndLoadVideoOrPlaylist(String url) {
        // https://youtu.be/lY0TpTxzoSI
        //  https://www.youtube.com/watch?v=lY0TpTxzoSI
        // https://youtube.com/playlist?list=PLod4zsEYEqn2X4q-qJEakuJeHTIzcFz69
        if (url.contains("playlist")) {
            myPlayer1.loadPlaylist(getYoutubePlaylistIDFromString(url));
        } else {
            myPlayer1.loadVideo(getYoutubeIDFromString(url));
        }
    }

    private String getYoutubeIDFromString(String url) {
        if (url.length() == 11) {
            return url;
        } else if (url.length() > 11) {
            String youtubeID = url.substring(url.length() - 11);
            return youtubeID;
        }
        return "";
    }

    private String getYoutubePlaylistIDFromString(String url) {
        if (url.length() == 34) {
            return url;
        } else if (url.length() > 34) {
            String youtubeID = url.substring(url.length() - 34);
            return youtubeID;
        }
        return "";
    }




    private InitializationCallback initializationCallback = new InitializationCallback() {
        @Override
        public void onInitialized(GazeTracker gazeTracker, InitializationErrorType error) {
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
            //float[] filteredGaze = myGazeTracker.filterGaze(gazeInfo);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                   // viewPoint.setType(gazeInfo.screenState == ScreenState.INSIDE_OF_SCREEN ? PointView.TYPE_DEFAULT : PointView.TYPE_OUT_OF_SCREEN);
                   // viewPoint.setPosition(filteredGaze[0], filteredGaze[1]);
//                    if (filteredGaze[0] <= screenWidth/2 + 50 ) {
                    if (gazeInfo.screenState == ScreenState.INSIDE_OF_SCREEN) {
                        if (myPlayer1.isPlaying() == false) {
                            if (bufferCounter < startBuffer) {
                                bufferCounter = bufferCounter + 1;
                            } else {
                                bufferCounter = 0;
                                myPlayer1.play();
                            }
                        }
                    } else {
                        if (myPlayer1.isPlaying()) {
                            if (bufferCounter < stopBuffer) {
                                bufferCounter = bufferCounter + 1;
                            } else {
                                bufferCounter = 0;
                                myPlayer1.pause();
                            }

                        }
                    }


                }
            });
        }
    };


}