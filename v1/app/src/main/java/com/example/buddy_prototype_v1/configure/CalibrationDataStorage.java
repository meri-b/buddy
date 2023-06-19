package com.example.buddy_prototype_v1.configure;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Arrays;

// source: https://github.com/visualcamp/seeso-sample-android/blob/master/app/src/main/java/visual/camp/sample/app/calibration/CalibrationDataStorage.java
public class CalibrationDataStorage {
    private static final String TAG = CalibrationDataStorage.class.getSimpleName();
    private static final String CALIBRATION_DATA = "calibrationData";

    // Store calibration data to SharedPreference
    public static void saveCalibrationData(Context context, double[] calibrationData) {
        if (calibrationData != null && calibrationData.length > 0) {
            SharedPreferences.Editor editor = context.getSharedPreferences(TAG, Context.MODE_PRIVATE).edit();
            editor.putString(CALIBRATION_DATA, Arrays.toString(calibrationData));
            editor.apply();
        } else {
            Log.e(TAG, "Abnormal calibration Data");
        }
    }

    // Get calibration data from SharedPreference
    public static @Nullable
    double[] loadCalibrationData(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(TAG, Context.MODE_PRIVATE);
        String saveData = prefs.getString(CALIBRATION_DATA, null);


        if (saveData != null) {
            try {
                Log.i(TAG, "loading..." + saveData);
                String[] split = saveData.substring(1, saveData.length() - 1).split(", ");
                double[] array = new double[split.length];
                for (int i = 0; i < split.length; i++) {
                    array[i] = Double.parseDouble(split[i]);
                }
                Log.i(TAG, "loading..." + array[6]);
                return array;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Maybe unmatched type of calibration data");
            }
        }
        return null;
    }
}