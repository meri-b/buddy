package com.example.buddy_prototype_v1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.buddy_prototype_v1.apps.Buddy1Activity;
import com.example.buddy_prototype_v1.apps.ScratchGenericActivity;
import com.example.buddy_prototype_v1.apps.VideoChooserActivity;
import com.example.buddy_prototype_v1.menus.SetupActivity;
import com.example.buddy_prototype_v1.menus.SubAppMenuAdapter;
import com.example.buddy_prototype_v1.menus.DevToolsActivity;
import com.example.buddy_prototype_v1.utility_apps.DiagnosticsActivity;

import camp.visual.gazetracker.GazeTracker;

public class MainActivity extends AppCompatActivity {

    public static final int CAMERA_REQUEST_CODE = 10;

    public static final String VIDEO_CHOOSER_ACTIVITY_NAME = "Watch a video!";
    public static final String GENERIC_SCRATCH_ACTIVITY_NAME = "Play a scratch game!";
    public static final String BUDDY_ACTIVITY_NAME = "Hang with buddy!";

    public static final boolean gazeEnabled = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("MainActivity", "onCreate");
        Log.d("SeeSo", "sdk version : " + GazeTracker.getVersionName());
        Log.d("MainActivity", "gaze enabled: " + gazeEnabled);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] subAppNames = new String[]{
                BUDDY_ACTIVITY_NAME,
                VIDEO_CHOOSER_ACTIVITY_NAME,
                GENERIC_SCRATCH_ACTIVITY_NAME,
        };

        RecyclerView subAppRecyclerView = findViewById(R.id.subAppRecyclerView);

        SubAppMenuAdapter adapter = new SubAppMenuAdapter(subAppNames);
        adapter.setOnItemClickListener(new SubAppMenuAdapter.ClickListener() {
            @Override
            public void onItemClick(int position) {
                String subApp = subAppNames[position];
                Log.d("MainActivity", "onItemClick position: " + position + " subapp: " + subApp);
                String subAppName = subAppNames[position];
                Intent subAppIntent;
                switch(subAppName) {
                    case VIDEO_CHOOSER_ACTIVITY_NAME:
                        subAppIntent = new Intent(MainActivity.this, VideoChooserActivity.class);
                        break;
                    case GENERIC_SCRATCH_ACTIVITY_NAME:
                        subAppIntent = new Intent(MainActivity.this, ScratchGenericActivity.class);
                        break;
                    case BUDDY_ACTIVITY_NAME:
                        subAppIntent = new Intent(MainActivity.this, Buddy1Activity.class);
                        break;
                    default:
                        subAppIntent = new Intent(MainActivity.this, DiagnosticsActivity.class);
                        break;
                }
                startActivity(subAppIntent);
            }
        });
        subAppRecyclerView.setAdapter(adapter);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getApplicationContext(), 4);
        subAppRecyclerView.setLayoutManager(mLayoutManager);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);

        // -------------------
        // Static buttons
        // -------------------
        Button devToolsButton = findViewById(R.id.devToolsButton);
        devToolsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent returnIntent = new Intent(MainActivity.this, DevToolsActivity.class);
                startActivity(returnIntent);
            }
        });

        Button setupButton = findViewById(R.id.setupButton);
        setupButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent returnIntent = new Intent(MainActivity.this, SetupActivity.class);
                startActivity(returnIntent);
            }
        });

    }
}