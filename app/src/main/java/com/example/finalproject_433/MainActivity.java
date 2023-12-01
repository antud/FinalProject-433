package com.example.finalproject_433;

import android.content.Intent;
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


    }
}