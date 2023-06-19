package com.example.buddy_prototype_v1.apps;

import android.app.Activity;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.buddy_prototype_v1.MainActivity;
import com.example.buddy_prototype_v1.R;
import com.example.buddy_prototype_v1.tools.SeesoGazeTracker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.callback.InitializationCallback;
import camp.visual.gazetracker.constant.InitializationErrorType;
import camp.visual.gazetracker.gaze.GazeInfo;

public class ScratchGenericActivity extends AppCompatActivity {
    String TAG = "GenericScratch";
    String MIME_TYPE = "text/html";
    String ENCODING = "utf-8";
    String URI_KEY = "scratchURI";
    String SETTINGS_KEY = "scratchSettings";

    String STEP_X = "stepX";
    String STEP_Y = "stepY";
    String START_X = "startX";
    String START_Y = "startY";
    String THRES_X = "thresX";
    String THRES_Y = "thresY";

    int stepX, stepY;
    int startX, startY;
    int thresX, thresY;


    WebView webView;
    Uri currentURI;
    SeesoGazeTracker myGazeTracker;

    int onscreen_x;
    int onscreen_y;
    String x_move;
    String y_move;

    SharedPreferences sharedPref;

    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");

        sharedPref = getSharedPreferences(SETTINGS_KEY, MODE_PRIVATE);

        // uncomment if need to reset saved URI value
//        SharedPreferences.Editor editor = sharedPref.edit();
//        editor.putString(URI_KEY, "");
//        editor.commit();

        myGazeTracker = new SeesoGazeTracker(getApplicationContext(), TAG, initializationCallback);
        initTrackingValues();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.scratch_activity_generic);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        myGazeTracker.startGazeTracker();

        webView = findViewById(R.id.webView);
        String retrievedURIString = sharedPref.getString(URI_KEY, null);
        if (retrievedURIString != null) {
            try {
                loadWebView(Uri.parse(retrievedURIString), 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        Button returnButton = findViewById(R.id.returnButton);
        returnButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "returning");
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(URI_KEY, currentURI.toString());
                editor.commit();
                myGazeTracker.releaseGaze();

                Intent returnIntent = new Intent(ScratchGenericActivity.this, MainActivity.class);
                startActivity(returnIntent);
            }
        });

        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "restarting...");
                myGazeTracker.releaseGaze();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                webView.reload();
                initTrackingValues();
                myGazeTracker.startGazeTracker();
            }
        });

        ActivityResultLauncher<Intent> startForResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            loadWebView(data.getData(), 1);
                        }
                    }
                });



        Button uploadButton = findViewById(R.id.uploadGame);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "opening file chooser...");
                Intent fileIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
                fileIntent.setType("*/*");
                fileIntent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                fileIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startForResult.launch(fileIntent);
            }
        });

        Button configButton = findViewById(R.id.configButton);
        configButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater inflater = (LayoutInflater)
                        getSystemService(LAYOUT_INFLATER_SERVICE);
                View popupView = inflater.inflate(R.layout.scratch_config, null);
                final PopupWindow popupWindow = new PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
                popupWindow.setElevation(10.0f);

                popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

                EditText stepXText = popupView.findViewById(R.id.editStepTextX);
                stepXText.setText(Integer.toString(stepX));
                EditText stepYText = popupView.findViewById(R.id.editStepTextY);
                stepYText.setText(Integer.toString(stepY));

                EditText startXText = popupView.findViewById(R.id.editStartTextX);
                startXText.setText(Integer.toString(startX));
                EditText startYText = popupView.findViewById(R.id.editStartTextY);
                startYText.setText(Integer.toString(startY));

                EditText thresXText = popupView.findViewById(R.id.editThresTextX);
                thresXText.setText(Integer.toString(thresX));
                EditText thresYText = popupView.findViewById(R.id.editThresTextY);
                thresYText.setText(Integer.toString(thresY));

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
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString(STEP_X, stepXText.getText().toString());
                        stepX = Integer.parseInt(stepXText.getText().toString());
                        editor.putString(STEP_Y, stepYText.getText().toString());
                        stepY = Integer.parseInt(stepYText.getText().toString());
                        editor.putString(START_X, startXText.getText().toString());
                        startX = Integer.parseInt(startXText.getText().toString());
                        editor.putString(START_Y, startYText.getText().toString());
                        startY = Integer.parseInt(startYText.getText().toString());
                        editor.putString(THRES_X, thresXText.getText().toString());
                        thresX = Integer.parseInt(thresXText.getText().toString());
                        editor.putString(THRES_Y, thresYText.getText().toString());
                        thresY = Integer.parseInt(thresYText.getText().toString());
                        editor.commit();
                        popupWindow.dismiss();
                    }
                });


            }
        });

    }

    private void loadWebView(Uri uri, int option) {
        currentURI = uri;
        String data = uriToHTMLString(uri, option);
        webView.getSettings().setJavaScriptEnabled(true);
        try {
            webView.loadDataWithBaseURL(null, data, MIME_TYPE, ENCODING, null);
        } catch (Exception e) {
            Log.w(TAG, "error loading data for web view with uri: " + uri.toString());
            if (option != 1) { // if we are loading from a saved value
                Log.w(TAG, "resetting saved URI key to null value");
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(URI_KEY, null);
                editor.commit();
            }
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

    private void initTrackingValues() {
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
        }
    };
}
