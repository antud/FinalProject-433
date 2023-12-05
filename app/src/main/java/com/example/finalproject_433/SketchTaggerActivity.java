package com.example.finalproject_433;

import static com.example.finalproject_433.Styling.applyButtonStyling;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

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


public class SketchTaggerActivity extends AppCompatActivity {
    private ArrayList<ListItem> listData;
    SQLiteDatabase db;
    SQLiteDatabase bigDb;
    TextView tagField;
    EditText searchField;
    ListView lv;
    ListItemAdapter adapter;
    MyDrawingArea mda;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sketch_tagger);

        listData = new ArrayList<>();
        adapter = new ListItemAdapter(this, R.layout.list_item, listData);

        lv = findViewById(R.id.sketch_list);
        lv.setAdapter(adapter);

        db = this.openOrCreateDatabase("images", Context.MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS IMAGES (IMAGE BLOB, DATE DATETIME, TAGS TEXT)");

        bigDb = this.openOrCreateDatabase("both", Context.MODE_PRIVATE, null);
        bigDb.execSQL("CREATE TABLE IF NOT EXISTS BOTH (PHOTO BLOB, DATE DATETIME, TAGS TEXT, TYPE TEXT)");


        tagField = findViewById(R.id.generated_tags);
        searchField = findViewById(R.id.tag_search_edit_box);

        ArrayList<ListItem> latestImages = showLatestImages();
        adapter.updateData(latestImages);

        Button backButton = findViewById(R.id.btnBack);
        Button clearButton = findViewById(R.id.clear_button);
        Button classifyButton = findViewById(R.id.classify_button);
        Button saveButton = findViewById(R.id.save_camera_image_button);
        Button findButton = findViewById(R.id.search_for_tags);

        ArrayList<Button> buttons = new ArrayList<>();
        buttons.add(clearButton);
        buttons.add(classifyButton);
        buttons.add(saveButton);
        buttons.add(findButton);
        buttons.add(backButton);

        //doing it like this in case we add more buttons
        for (Button button : buttons) {
            applyButtonStyling(button);
        }

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(SketchTaggerActivity.this, MainActivity.class);
            startActivity(intent);
        });

    }

    public ArrayList<ListItem> showLatestImages() {
        Cursor c = db.rawQuery("SELECT * FROM IMAGES", null);
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

    public void onClassify(View view) {
        mda = findViewById(R.id.drawing_area);
        Bitmap bm = mda.getBitmap();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    myVisionTester(bm, new VisionCallback() {
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

    void myVisionTester(Bitmap image, VisionCallback callback) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, bout);
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
        String tagText = "";
        if (response != null && response.getResponses() != null && !response.getResponses().isEmpty()) {
            List<EntityAnnotation> annotations = response.getResponses().get(0).getLabelAnnotations();
            if (annotations != null && !annotations.isEmpty()) {
                StringBuilder tagsBuilder = new StringBuilder();
                for (EntityAnnotation annotation : annotations) {
                    if (annotation.getScore() != null && annotation.getScore() >= 0.85) {
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

    public void searchTags(View view) {
        Cursor c;
        String tagText = searchField.getText().toString();
        ArrayList<ListItem> searchResults = new ArrayList<>();

        if (tagText.equals("")) {
            c = db.rawQuery("SELECT * FROM IMAGES", null);
            searchResults = showLatestImages();
        } else {
            try {
                //split search on commas
                String[] searchTags = tagText.split(",");

                //make query for each tag, combining them with OR
                StringBuilder queryBuilder = new StringBuilder();
                queryBuilder.append("SELECT * FROM IMAGES WHERE ");

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
            } catch (CursorIndexOutOfBoundsException e) {
                System.out.println("Ouch!");
            }
        }
        adapter.updateData(searchResults);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String formatDateTime(LocalDateTime dateTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d yyyy, h a", Locale.getDefault());
        return sdf.format(Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant()));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void saveDrawing(View view) {
        MyDrawingArea mda = findViewById(R.id.drawing_area);

        String tagStrings = tagField.getText().toString();

        LocalDateTime currentDateTime = LocalDateTime.now();
        String formattedDateTime = formatDateTime(currentDateTime);

        Bitmap b = mda.getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] ba = stream.toByteArray();
        ContentValues cv = new ContentValues();
        cv.put("IMAGE", ba);
        cv.put("DATE", formattedDateTime);
        cv.put("TAGS", tagStrings);
        db.insert("IMAGES", null, cv);

        ContentValues cv2 = new ContentValues();
        cv2.put("PHOTO", ba);
        cv2.put("DATE", formattedDateTime);
        cv2.put("TAGS", tagStrings);
        cv2.put("TYPE", "sketch");
        bigDb.insert("BOTH", null, cv2);

        adapter.updateData(showLatestImages());
    }

    public void onClear(View view) {
        MyDrawingArea mda = findViewById(R.id.drawing_area);
        tagField.setText("Draw and classify an image");
        mda.clear();
    }
}