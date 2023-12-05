package com.example.finalproject_433;

import static com.example.finalproject_433.Styling.applyButtonStyling;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private MediaPlayer mp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button photoTaggerButton = findViewById(R.id.photoTagger);
        Button sketchTaggerButton = findViewById(R.id.sketchTagger);
        Button storyTellerButton = findViewById(R.id.storyTeller);

        //Song used with permission from:
        //Music: Local Forecast - Slower by Kevin MacLeod
        //Free download: https://filmmusic.io/song/3988-local-forecast-slower
        //Licensed under CC BY 4.0: https://filmmusic.io/standard-license
        mp = MediaPlayer.create(this, R.raw.lf);

        mp.setLooping(true);
        mp.setVolume(0.2f, 0.2f);
        mp.start();

        photoTaggerButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PhotoTaggerActivity.class);
            startActivity(intent);
        });

        sketchTaggerButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SketchTaggerActivity.class);
            startActivity(intent);
        });

        storyTellerButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StoryTellerActivity.class);
            startActivity((intent));
        });
        applyButtonStyling(photoTaggerButton);
        applyButtonStyling(sketchTaggerButton);
        applyButtonStyling(storyTellerButton);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mp != null && mp.isPlaying()) {
            mp.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mp != null && !mp.isPlaying()) {
            mp.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mp != null) {
            mp.release();
            mp = null;
        }
    }


}