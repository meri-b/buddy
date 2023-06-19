package com.example.buddy_prototype_v1.utility_apps;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.buddy_prototype_v1.menus.DevToolsActivity;
import com.example.buddy_prototype_v1.tools.GameView;
import com.example.buddy_prototype_v1.R;

public class SpriteTest1Activity extends AppCompatActivity {
    String TAG = "SpriteTest1Activity";

    private GameView gameView;

    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sprite_test1);

        gameView = findViewById(R.id.gameView);

        Button returnButton = findViewById(R.id.returnButton);
        returnButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent returnIntent = new Intent(SpriteTest1Activity.this, DevToolsActivity.class);
                startActivity(returnIntent);
            }
        });

        Button idleButton = findViewById(R.id.idleButton);
        idleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gameView.setActionState(gameView.IDLE);
            }
        });

        Button jumpButton = findViewById(R.id.jumpButton);
        jumpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gameView.setActionState(gameView.JUMP);
            }
        });

        Button runButton = findViewById(R.id.runButton);
        runButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gameView.setActionState(gameView.RUN);
            }
        });

        Button sleepButton = findViewById(R.id.sleepButton);
        sleepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gameView.setActionState(gameView.SLEEP);
            }
        });

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

}
