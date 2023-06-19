package com.example.buddy_prototype_v1.configure;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

// sources:
// https://stackoverflow.com/questions/29427288/how-to-change-color-of-canvas-in-android-dynamically-for-every-5-sec
// https://stackoverflow.com/questions/9843451/how-can-i-put-axis-on-a-png-file-in-java/9875534#9875534
public class CalibrationView extends View {
    private Paint paint;
    private Paint clearingPaint;
    private Paint borderPaint;
    private Thread colorThread;

    private int currentColor = 100;
    private boolean drawStroke = false;

    private final int radius = 250;
    private final int totalColors = 360;
    private List<Integer> colorList;

    public CalibrationView(Context context) {
        super(context);
        init(context);
    }

    public CalibrationView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CalibrationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public CalibrationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        colorList = new ArrayList<Integer>(totalColors);
        for (int i = 0; i < totalColors; i++) {
            float[] hsv = new float[]{i, 1, 1};
            colorList.add(Color.HSVToColor(hsv));
        }
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(colorList.get(currentColor));

        clearingPaint = new Paint();
        clearingPaint.setStyle(Paint.Style.FILL);
        clearingPaint.setColor(Color.WHITE);

        borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStrokeWidth(30);
    }

    public void startChangingColors(final int delayMilis) {
        stopRunning();

        colorThread = new Thread(new Runnable() {

            @Override
            public void run() {
                boolean interrupted = false;
                while (!interrupted) {
                    try {
                        getRootView().post(new Runnable() {
                            @Override
                            public void run() {
                                changeColors();

                            }
                        });

                        Thread.sleep(delayMilis);

                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            // We just woked up to close all this
                            interrupted = true;
                        } else {
                            Log.e("Error:", e.getMessage());
                        }
                    }
                }

            }
        });
        colorThread.start();

    }

    public void stopRunning() {
        if (colorThread != null) {
            colorThread.interrupt();
        }
    }

    public void changeColors() {
        if (currentColor == totalColors - 1) {
            currentColor = 0;
        } else {
            currentColor++;
        }
        paint.setColor(colorList.get(currentColor));
        invalidate();
    }

    public void drawOutline() {
        drawStroke = true;
    }

    public void doNotDrawOutline() {
        drawStroke = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas != null) {
            super.onDraw(canvas);
            // Clear the canvas
            canvas.drawPaint(clearingPaint);
            // Draw circles
            int xPos = canvas.getWidth() / 2;
            int yPos = canvas.getHeight() / 2;
            canvas.drawCircle(xPos, yPos, radius, paint);
            if (drawStroke) {
                canvas.drawCircle(xPos, yPos, radius, borderPaint);
            }
        }
    }
}
