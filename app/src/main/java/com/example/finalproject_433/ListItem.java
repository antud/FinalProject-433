package com.example.finalproject_433;

import android.graphics.Bitmap;

public class ListItem {
    private int imageResource;
    private Bitmap imageBitmap;
    private String tagText;
    private boolean isChecked;

    public ListItem(Bitmap imageBitmap, String tagText) {
        this.imageBitmap = imageBitmap;
        this.tagText = tagText;
    }

    public int getImageResource() {
        return imageResource;
    }

    public Bitmap getImageBitmap() {
        return imageBitmap;
    }

    public String getTagText() {
        return tagText;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }
}