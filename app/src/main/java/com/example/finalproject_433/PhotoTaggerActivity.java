package com.example.finalproject_433;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PhotoTaggerActivity extends AppCompatActivity {
    private ArrayList<ListItem> listData;
    SQLiteDatabase db;
    SQLiteDatabase bigDb;
    TextView tagField;
    EditText searchField;
    ListView lv;
    ListItemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_tagger);

        listData = new ArrayList<>();
        adapter = new ListItemAdapter(this, R.layout.list_item, listData);

        lv = findViewById(R.id.image_list);
        lv.setAdapter(adapter);

        //create the db / open it
        db = this.openOrCreateDatabase("photos", Context.MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS PHOTOS (PHOTO BLOB, DATE DATETIME, TAGS TEXT)");

        bigDb = this.openOrCreateDatabase("both", Context.MODE_PRIVATE, null);
        bigDb.execSQL("CREATE TABLE IF NOT EXISTS BOTH (PHOTO BLOB, DATE DATETIME, TAGS TEXT, TYPE TEXT)");

        tagField = findViewById(R.id.generated_tags);
        searchField = findViewById(R.id.tag_search_edit_box);

        ArrayList<ListItem> latestImages = showLatestImages();
        adapter.updateData(latestImages);

//        CheckBox checkBox = findViewById(R.id.include_image);
//        checkBox.setVisibility(View.GONE); // Make CheckBox invisible


        Button backButton = findViewById(R.id.btnBack);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(PhotoTaggerActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }


    void myVisionTester(Bitmap image, VisionCallback callback) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 90, bout);
        Image myimage = new Image();
        myimage.encodeContent(bout.toByteArray());

        // PREPARE AnnotateImageRequest
        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
        annotateImageRequest.setImage(myimage);
        Feature f = new Feature();
        f.setType("LABEL_DETECTION");
        f.setMaxResults(5);
        List<Feature> lf = new ArrayList<>();
        lf.add(f);
        annotateImageRequest.setFeatures(lf);

        // BUILD the Vision
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(new VisionRequestInitializer(Key.GOOGLE_API_KEY));
        Vision vision = builder.build();

        // CALL Vision.Images.Annotate
        BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
        List<AnnotateImageRequest> list = new ArrayList<>();
        list.add(annotateImageRequest);
        batchAnnotateImagesRequest.setRequests(list);
        Vision.Images.Annotate task = vision.images().annotate(batchAnnotateImagesRequest);
        BatchAnnotateImagesResponse response = task.execute();
        Log.v("MYTAG", response.toPrettyString());

        // Tags are filled sometimes in a weird order based on the ordering of the actual files in the drawable folder.
        // Depending on the naming of the file, will determine what index gets assigned to it.

        //this will classify the image based on its score
        String tagText = "";
        if (response != null && response.getResponses() != null && !response.getResponses().isEmpty()) {
            List<EntityAnnotation> annotations = response.getResponses().get(0).getLabelAnnotations();
            if (annotations != null && !annotations.isEmpty()) {
                StringBuilder tagsBuilder = new StringBuilder();
                for (EntityAnnotation annotation : annotations) {
                    if (annotation.getScore() != null && annotation.getScore() >= 0.80) {
                        tagsBuilder.append(annotation.getDescription()).append(", ");
                    }
                    if (annotation.getScore() < 0.85) {
                        tagText = annotations.get(0).getDescription();
                    }
                }
                if (tagsBuilder.length() > 0) {
                    tagText = tagsBuilder.substring(0, tagsBuilder.length() - 2);
                }
            }
        }
        // Use callback to send the result
        callback.onResult(tagText);
    }

    public void startCamera(View view) {
        Intent camIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(camIntent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap image = (Bitmap) extras.get("data");

            ImageView imageView = findViewById(R.id.imageView);
            imageView.setImageBitmap(image);

            // Create and start a new thread for myVisionTester,
            // needs to be on its own thread as AS cant use the network on the same thread as the activity or something??
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        myVisionTester(image, new VisionCallback() {
                            @Override
                            public void onResult(String result) {
                                // Update the tagField with generated tags on the main thread
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        tagField.setText(result);
                                    }
                                });
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    //formats the date correctly
    @RequiresApi(api = Build.VERSION_CODES.O)
    public String formatDateTime(LocalDateTime dateTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d yyyy, h a", Locale.getDefault());
        return sdf.format(Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant()));
    }

    //saves the photo and inserts it into the db
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void savePhoto(View view) {
        ImageView img = findViewById(R.id.imageView);
        String tagStrings = tagField.getText().toString();

        LocalDateTime currentDateTime = LocalDateTime.now();
        String formattedDateTime = formatDateTime(currentDateTime);

        Bitmap b = ((BitmapDrawable) img.getDrawable()).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] ba = stream.toByteArray();
        ContentValues cv = new ContentValues();
        cv.put("PHOTO", ba);
        cv.put("DATE", formattedDateTime);
        cv.put("TAGS", tagStrings);
        db.insert("PHOTOS", null, cv);

        cv.put("TYPE", "photo");
        bigDb.insert("BOTH", null, cv);

        adapter.updateData(showLatestImages());
    }

    // on startup, show the latest images, not sure if this needs to be capped at all since we can scroll through the list?
    public ArrayList<ListItem> showLatestImages() {
        Cursor c = db.rawQuery("SELECT * FROM PHOTOS", null);
        ArrayList<ListItem> latestImages = new ArrayList<>();

        //not sure why but for some reason move to last is pointing to the 2nd to last image??
        //so we need to go to the actual last one with next
        c.moveToLast();
        c.moveToNext();
        while (c.moveToPrevious()) {
            byte[] ba = c.getBlob(0);
            String date = c.getString(1);
            String tags = c.getString(2);

            latestImages.add(new ListItem(BitmapFactory.decodeByteArray(ba, 0, ba.length), tags + "\n" + date));
        }
        c.close();
        return latestImages;
    }

    //pretty much same as A3, but instead of setting the 3 image/text views mannually
    // we just add the results to the list
    public void searchTags(View view) {
        Cursor c;
        String tagText = searchField.getText().toString();
        ArrayList<ListItem> searchResults = new ArrayList<>();

        if (tagText.equals("")) {
            c = db.rawQuery("SELECT * FROM PHOTOS", null);
            searchResults = showLatestImages();
        } else {
            try {
                // split entry by commas
                String[] searchTags = tagText.split(",");

                // make query for each tag, combine with or
                StringBuilder queryBuilder = new StringBuilder();
                queryBuilder.append("SELECT * FROM PHOTOS WHERE ");

                for (int i = 0; i < searchTags.length; i++) {
                    if (i > 0) {
                        queryBuilder.append(" OR ");
                    }
                    queryBuilder.append("TAGS LIKE ?");
                }

                queryBuilder.append(" ORDER BY DATE DESC");
                String query = queryBuilder.toString();

                // arr for each param of search tag
                String[] queryParameters = new String[searchTags.length];

                // run the query for each param
                for (int i = 0; i < searchTags.length; i++) {
                    queryParameters[i] = "%" + searchTags[i].trim() + "%";
                }

                //execute the q
                c = db.rawQuery(query, queryParameters);

                int position = 1;
                // populate search images
                while (c.moveToNext() && position <= 3) {
                    byte[] ba = c.getBlob(0);
                    String date = c.getString(1);
                    String tagsInDatabase = c.getString(2);

                    searchResults.add(new ListItem(BitmapFactory.decodeByteArray(ba, 0, ba.length), tagsInDatabase + "\n" + date));
                    position++;
                }

                //just in case
            } catch (CursorIndexOutOfBoundsException e) {
                //dont need to show blank image since if there is not enough found results, the list wont have an empty slot
            }
        }
        adapter.updateData(searchResults);
    }
}