package com.example.finalproject_433;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button photoTaggerButton = findViewById(R.id.photoTagger);
        Button sketchTaggerButton = findViewById(R.id.sketchTagger);
        Button storyTellerButton = findViewById(R.id.storyTeller);


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

    private void applyButtonStyling(final Button button) {
        int[] colors = {
                Color.parseColor("#00796B"), // Teal
                Color.parseColor("#303F9F"), // Indigo
                Color.parseColor("#0288D1"), // Light Blue
                Color.parseColor("#C2185B"), // Pink
                Color.parseColor("#7B1FA2"), // Purple
                Color.parseColor("#F57C00"), // Orange
                Color.parseColor("#5D4037")  // Brown
        };
        Integer[] colorObjects = new Integer[colors.length];
        for (int i = 0; i < colors.length; i++) {
            colorObjects[i] = Integer.valueOf(colors[i]);
        }

        if (button != null && colors != null) {
            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), (Object[]) colorObjects);
            colorAnimation.setDuration(20000); //20 secs
            colorAnimation.setRepeatCount(ValueAnimator.INFINITE);
            colorAnimation.setRepeatMode(ValueAnimator.RESTART);
            colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    button.setBackgroundColor((int) animator.getAnimatedValue());
                }
            });
            colorAnimation.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel timers if necessary
    }

}