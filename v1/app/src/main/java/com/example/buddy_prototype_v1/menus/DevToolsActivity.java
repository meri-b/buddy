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
import com.example.buddy_prototype_v1.utility_apps.Diagnostics2Activity;
import com.example.buddy_prototype_v1.utility_apps.DiagnosticsActivity;
import com.example.buddy_prototype_v1.utility_apps.SpriteTest1Activity;

public class DevToolsActivity extends AppCompatActivity {

    public static final String DIAGNOSTIC_ACTIVITY_NAME = "diagnostics";
    public static final String SPRITE_TEST_NAME = "sprite test";
    public static final String DIAGNOSTIC_2_ACTIVITY_NAME = "diagnostics 2.0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("DevTools", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dev_tools_layout);

        String[] subAppNames = new String[]{
                DIAGNOSTIC_ACTIVITY_NAME,
                SPRITE_TEST_NAME,
                DIAGNOSTIC_2_ACTIVITY_NAME,
        };

        RecyclerView subAppRecyclerView = findViewById(R.id.subAppRecyclerView);
        SubAppMenuAdapter adapter = new SubAppMenuAdapter(subAppNames);
        adapter.setOnItemClickListener(new SubAppMenuAdapter.ClickListener() {
            @Override
            public void onItemClick(int position) {
                String subApp = subAppNames[position];
                Log.d("DevTools", "onItemClick position: " + position + " subapp: " + subApp);
                String subAppName = subAppNames[position];
                Intent subAppIntent;
                switch(subAppName) {
                    case DIAGNOSTIC_ACTIVITY_NAME:
                        subAppIntent = new Intent(DevToolsActivity.this, DiagnosticsActivity.class);
                        break;
                    case SPRITE_TEST_NAME:
                        subAppIntent = new Intent(DevToolsActivity.this, SpriteTest1Activity.class);
                        break;
                    case DIAGNOSTIC_2_ACTIVITY_NAME:
                        subAppIntent = new Intent(DevToolsActivity.this, Diagnostics2Activity.class);
                        break;
                    default:
                        subAppIntent = new Intent(DevToolsActivity.this, DevToolsActivity.class);
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
                Intent returnIntent = new Intent(DevToolsActivity.this, MainActivity.class);
                startActivity(returnIntent);
            }
        });
    }
}
