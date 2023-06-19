package com.example.buddy_prototype_v1.tools;

import android.content.Context;
import android.media.MediaPlayer;

import com.example.buddy_prototype_v1.R;

public class Voiceover {

    Boolean voiceoverPlaying;
    MediaPlayer helloTrack;
    MediaPlayer goodbyeTrack;
    MediaPlayer videoIntroTrack;
    MediaPlayer videoOutroTrack;

    Context ctx;
    String TAG;


    public Voiceover(Context context, String tag) {
        ctx = context;
        TAG = tag;

        voiceoverPlaying = false;

        helloTrack = MediaPlayer.create(ctx, R.raw.buddyhello);
        goodbyeTrack = MediaPlayer.create(ctx, R.raw.buddygoodbye);
        videoIntroTrack = MediaPlayer.create(ctx, R.raw.buddyintro);
        videoOutroTrack = MediaPlayer.create(ctx, R.raw.buddyoutro);
    }

    public Boolean getIsVoiceoverPlaying() {
        return voiceoverPlaying;
    }

    public Thread playHello() {
        Thread thread = new Thread(helloRunnable);
        if (!voiceoverPlaying) {
            thread.start();
        }
        return thread;
    }

    Runnable helloRunnable = new Runnable(){
        public void run() {
            //some code here
            helloTrack.start();
            while (helloTrack.isPlaying()) {
                voiceoverPlaying = true;
            }
            voiceoverPlaying = false;
        }
    };

    public Thread playGoodbye() {
        Thread thread = new Thread(goodbyeRunnable);
        if (!voiceoverPlaying) {
            thread.start();
        }
        return thread;
    }

    Runnable goodbyeRunnable = new Runnable(){
        public void run() {
            //some code here
            goodbyeTrack.start();
            while (goodbyeTrack.isPlaying()) {
                voiceoverPlaying = true;
            }
            voiceoverPlaying = false;
        }
    };

    public Thread playIntro() {
        Thread thread = new Thread(introRunnable);
        if (!voiceoverPlaying) {
            thread.start();
        }
        return thread;
    }

    Runnable introRunnable = new Runnable(){
        public void run() {
            //some code here
            videoIntroTrack.start();
            while (videoIntroTrack.isPlaying()) {
                voiceoverPlaying = true;
            }
            voiceoverPlaying = false;
        }
    };

    public Thread playOutro() {
        Thread thread = new Thread(outroRunnable);
        if (!voiceoverPlaying) {
            thread.start();
        }
        return thread;
    }

    Runnable outroRunnable = new Runnable(){
        public void run() {
            //some code here
            videoOutroTrack.start();
            while (videoOutroTrack.isPlaying()) {
                voiceoverPlaying = true;
            }
            voiceoverPlaying = false;
        }
    };
}
