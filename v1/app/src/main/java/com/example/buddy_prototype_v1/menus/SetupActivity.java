package com.example.buddy_prototype_v1.menus;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.buddy_prototype_v1.MainActivity;
import com.example.buddy_prototype_v1.R;
import com.example.buddy_prototype_v1.configure.CalibrationActivity;
import com.example.buddy_prototype_v1.configure.StateDetectionActivity;

public class SetupActivity extends AppCompatActivity {

    public static final String STATE_DETECTION_ACTIVITY_NAME = "Configure state detection";
    public static final String CALIBRATION_ACTIVITY_NAME = "Calibrate gaze";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("SetupActivity", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dev_tools_layout);

        String[] subAppNames = new String[]{
                STATE_DETECTION_ACTIVITY_NAME,
                CALIBRATION_ACTIVITY_NAME,
        };

        RecyclerView subAppRecyclerView = findViewById(R.id.subAppRecyclerView);
        SubAppMenuAdapter adapter = new SubAppMenuAdapter(subAppNames);
        adapter.setOnItemClickListener(new SubAppMenuAdapter.ClickListener() {
            @Override
            public void onItemClick(int position) {
                String subApp = subAppNames[position];
                Log.d("SetupActivity", "onItemClick position: " + position + " subapp: " + subApp);
                String subAppName = subAppNames[position];
                Intent subAppIntent;
                switch(subAppName) {
                    case STATE_DETECTION_ACTIVITY_NAME:
                        subAppIntent = new Intent(SetupActivity.this, StateDetectionActivity.class);
                        break;
                    case CALIBRATION_ACTIVITY_NAME:
                        subAppIntent = new Intent(SetupActivity.this, CalibrationActivity.class);
                        break;
                    default:
                        subAppIntent = new Intent(SetupActivity.this, SetupActivity.class);
                        break;
                }
                startActivity(subAppIntent);
            }
        });
        subAppRecyclerView.setAdapter(adapter);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getApplicationContext(), 4);
        subAppRecyclerView.setLayoutManager(mLayoutManager);

        // -------------------
        // Static buttons
        // -------------------
        Button returnButton = findViewById(R.id.returnButton);
        returnButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent returnIntent = new Intent(SetupActivity.this, MainActivity.class);
                startActivity(returnIntent);
            }
        });
    }
}