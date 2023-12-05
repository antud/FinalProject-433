package com.example.finalproject_433;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class Styling extends AppCompatActivity {
    public static void applyButtonStyling(final Button button) {
        int[] colors = {
                Color.parseColor("#00796B"), // teal
                Color.parseColor("#303F9F"), // indigo
                Color.parseColor("#0288D1"), // light Blue
                Color.parseColor("#C2185B"), // pink
                Color.parseColor("#7B1FA2"), // purple
                Color.parseColor("#F57C00"), // orange
                Color.parseColor("#5D4037")  // brown
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
