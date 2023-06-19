package com.example.buddy_prototype_v1.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.Nullable;

import com.example.buddy_prototype_v1.R;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

// source: https://ssaurel.medium.com/create-a-running-man-game-animation-on-android-b5678ded54cc
public class GameView extends SurfaceView implements Runnable {
    String TAG = "GameView";

    private Thread gameThread;
    private SurfaceHolder ourHolder;
    private Bitmap bitmapOtter;
    private volatile boolean playing;
    private Canvas canvas;
    private int frameWidth = 300, frameHeight = 300;
    private int frameCount = 35;
    private int currentFrame = 0;
    private long lastFrameChangeTime = 0;
    private int frameLengthInMillisecond = 200;


    private float otterXPos = 10, otterYPos = 10;
    private float runSpeedPerSecond = 500;
    private long fps;
    private long timeThisFrame;

    private Rect frameToDraw = new Rect(0, 0, frameWidth, frameHeight);
    Integer leftStart = 500;
    Integer topStart = 0;
    private RectF whereToDraw = new RectF(leftStart, topStart, leftStart+800, topStart+800);

    public static final String IDLE = "idle";
    public static final String JUMP = "jump";
    public static final String RUN = "run";
    public static final String SLEEP = "sleep";

    private int currentLoop = 0;
    private int runLoop = 3;
    private int jumpLoop = 1;
    private int spinLoop = 1;

    HashMap<String, List<Integer>> actionFrames;
    String currentState;
    private int startFrame;
    private int endFrame;

    Boolean inSession;

    public GameView(Context context) {
        super(context);
        init(context);
    }

    public GameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GameView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public GameView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public void init(Context context) {
        Log.i(TAG, "GameView");
        ourHolder = getHolder();
        playing = true;
        bitmapOtter = BitmapFactory.decodeResource(getResources(), R.drawable.ottersheet2);
        bitmapOtter = Bitmap.createBitmap(bitmapOtter, 0, 0,frameWidth * frameCount, frameHeight);

        actionFrames = new HashMap<>();
        actionFrames.put(IDLE, Arrays.asList(0,15));
        actionFrames.put(JUMP, Arrays.asList(16,22));
        actionFrames.put(RUN, Arrays.asList(23,25));
        actionFrames.put(SLEEP, Arrays.asList(26,34));
//        actionFrames.put(IDLE1, Arrays.asList(0, 3));
//        actionFrames.put(IDLE2, Arrays.asList(4, 15));
//        actionFrames.put(JUMP, Arrays.asList(16, 19));
//        actionFrames.put(LAND, Arrays.asList(20, 22));
//        actionFrames.put(RUN, Arrays.asList(23, 25));
//        actionFrames.put(SLEEP, Arrays.asList(26, 31));
//        actionFrames.put(SPIN, Arrays.asList(32, 34));

        currentState = IDLE;
        startFrame = actionFrames.get(currentState).get(0);
        endFrame = actionFrames.get(currentState).get(1);

        inSession = false;
    }

    public void setTag(String tag) {
        TAG = tag;
    }

    @Override
    public void run() {
        while (playing) {
            long startFrameTime = System.currentTimeMillis();
//                update();
            if ((currentState == RUN && currentLoop >= runLoop) || (currentState == JUMP && currentLoop >= jumpLoop)) {
                setActionState(IDLE);
            }
            if (currentState == SLEEP && currentLoop >= spinLoop) {
                endFrame = 31;
            }
            draw();
            timeThisFrame = System.currentTimeMillis() - startFrameTime;
            if (timeThisFrame >= 1) {
                fps = 1000 / timeThisFrame;
            }
        }
    }

    public void setInSession(Boolean session) {
        Log.i(TAG, "in session: " + session);
        inSession = session;
    }

    private void drawOutline() {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(30);
        Rect rect = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawRect(rect, paint);
    }



    public void manageCurrentFrame() {
        long time = System.currentTimeMillis();
        if (time > lastFrameChangeTime + frameLengthInMillisecond) {

            lastFrameChangeTime = time;
            currentFrame++;
            if (currentFrame >= endFrame) {
                currentFrame = startFrame;
                currentLoop++;
            }
//            Log.i(TAG, "manageCurrentFrame:" + frameToDraw.top + ", " + frameToDraw.left + ", " + frameToDraw.bottom + ", " + frameToDraw.right);

        }

        frameToDraw.left = currentFrame * frameWidth;
        frameToDraw.right = frameToDraw.left + frameWidth;
    }


    public void draw() {
        if (ourHolder.getSurface().isValid()) {
            canvas = ourHolder.lockCanvas();
            canvas.drawColor(Color.WHITE);
            manageCurrentFrame();
            canvas.drawBitmap(bitmapOtter, frameToDraw, whereToDraw, null);
            if (inSession) {
                drawOutline();
            }
            ourHolder.unlockCanvasAndPost(canvas);
        }
    }

    public void pause() {
        playing = false;
        try {
            gameThread.join();
        } catch(InterruptedException e) {
            Log.e(TAG, "Joining Thread");
        }
    }


    public void resume() {
        playing = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void setActionState(String stateDesired) {
        switch(stateDesired) {
            case IDLE:
                currentState = IDLE;
                currentFrame = actionFrames.get(currentState).get(0);
                startFrame = actionFrames.get(currentState).get(0);
                endFrame = actionFrames.get(currentState).get(1);
                break;
            case JUMP:
                currentState = JUMP;
                currentFrame = actionFrames.get(currentState).get(0);
                startFrame = actionFrames.get(currentState).get(0);
                endFrame = actionFrames.get(currentState).get(1);
                break;
            case RUN:
                currentState = RUN;
                currentFrame = actionFrames.get(currentState).get(0);
                startFrame = actionFrames.get(currentState).get(0);
                endFrame = actionFrames.get(currentState).get(1);
                break;
            case SLEEP:
                currentState = SLEEP;
                currentFrame = 32;
                startFrame = actionFrames.get(currentState).get(0);
                endFrame = actionFrames.get(currentState).get(1);
                break;
            default:
                Log.i(TAG, "invalid desired state: " + stateDesired);
                break;
        }
        currentLoop = 0;
    }
}

