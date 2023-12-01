package com.example.finalproject_433;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


//lets just try to do the first checkbox to include sketches first
public class StoryTellerActivity extends AppCompatActivity {

    SQLiteDatabase bigDb;
    private ArrayList<ListItem> listData;
    private CheckBox includeSketchesToggle;
    boolean isSketchIncluded;
    EditText searchField;
    ListView lv;
    ListItemAdapter adapter;
    String url = "https://api.textcortex.com/v1/texts/social-media-posts";
    String contextString;
    String keywordsString;
    TextView story;
    TextView selectedStoryTags;
    StringBuilder selectedTags;
    TextToSpeech tts;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_teller);

        listData = new ArrayList<>();
        adapter = new ListItemAdapter(this, R.layout.list_item, listData);

        lv = findViewById(R.id.story_image_list);
        lv.setAdapter(adapter);

        bigDb = this.openOrCreateDatabase("both", Context.MODE_PRIVATE, null);
        bigDb.execSQL("CREATE TABLE IF NOT EXISTS BOTH (PHOTO BLOB, DATE DATETIME, TAGS TEXT, TYPE TEXT)");

        searchField = findViewById(R.id.story_tag_search_text);

        includeSketchesToggle = findViewById(R.id.include_sketches_toggle);
        includeSketchesToggle.setChecked(true);

        includeSketchesToggle.setOnCheckedChangeListener((buttonView, isChecked) -> updateImageList());

        ArrayList<ListItem> latestImages = showLatestImages();
        adapter.updateData(latestImages);

        Button backButton = findViewById(R.id.btnBack);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(StoryTellerActivity.this, MainActivity.class);
            startActivity(intent);
        });

        selectedStoryTags = findViewById(R.id.story_selected_tags);
        story = findViewById(R.id.generated_story);

        adapter.setOnItemCheckListener(() -> {
            updateSelectedTagText();
        });
        tts = null;
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.UK);
                }
            }
        });
    }


    //    private void updateSelectedTagText() {
//        selectedTags = new StringBuilder();
//        for (ListItem listItem : adapter.getDataList()) {
//            if (listItem.isChecked()) {
//                String fullTagText = listItem.getTagText().toString();
//                String[] splitTags = fullTagText.split("\\s+[A-Za-z]{3}\\s+\\d{2}\\s+\\d{4},\\s+\\d{1,2}\\s+[A|P]M", 2);
//                String tagsOnly = splitTags[0].trim();
//
//                if (selectedTags.length() > 0) {
//                    selectedTags.append(", ");
//                }
//                selectedTags.append(tagsOnly);
//            }
//        }
//        selectedStoryTags.setText(selectedTags.toString());
//    }
    private void updateSelectedTagText() {
        Set<String> uniqueTags = new HashSet<>();
        for (ListItem listItem : adapter.getDataList()) {
            if (listItem.isChecked()) {
                String fullTagText = listItem.getTagText().toString();
                String[] splitTags = fullTagText.split("\\s+[A-Za-z]{3}\\s+\\d{2}\\s+\\d{4},\\s+\\d{1,2}\\s+[A|P]M", 2);
                String tagsOnly = splitTags[0].trim();
                String[] tags = tagsOnly.split(",\\s*");

                Collections.addAll(uniqueTags, tags);
            }
        }

        selectedTags = new StringBuilder();
        for (String tag : uniqueTags) {
            if (selectedTags.length() > 0) {
                selectedTags.append(", ");
            }
            selectedTags.append(tag);
        }

        selectedStoryTags.setText(selectedTags.toString());
    }

    private void updateImageList() {
        ArrayList<ListItem> latestImages = showLatestImages();
        adapter.updateData(latestImages);
        adapter.notifyDataSetChanged();
    }

    public void onFind(View view) {
        String tagText = searchField.getText().toString();
        boolean isSketchIncluded = includeSketchesToggle.isChecked();
        ArrayList<ListItem> searchResults = new ArrayList<>();

        if (tagText.equals("")) {
            searchResults = showLatestImages();
        } else {
            String[] searchTags = tagText.split(",");
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT * FROM BOTH WHERE ");

            if (!isSketchIncluded) {
                queryBuilder.append("TYPE = 'photo' AND ");
            }

            for (int i = 0; i < searchTags.length; i++) {
                if (i > 0) {
                    queryBuilder.append(" OR ");
                }
                queryBuilder.append("TAGS LIKE ?");
            }

            queryBuilder.append(" ORDER BY DATE DESC");
            String query = queryBuilder.toString();

            String[] queryParameters = new String[searchTags.length];
            for (int i = 0; i < searchTags.length; i++) {
                queryParameters[i] = "%" + searchTags[i].trim() + "%";
            }

            Cursor c = null;
            try {
                c = bigDb.rawQuery(query, queryParameters);
                while (c.moveToNext()) {
                    byte[] ba = c.getBlob(0);
                    String date = c.getString(1);
                    String tagsInDatabase = c.getString(2);
                    searchResults.add(new ListItem(BitmapFactory.decodeByteArray(ba, 0, ba.length), tagsInDatabase + "\n" + date));
                }
            } catch (CursorIndexOutOfBoundsException e) {
                Log.e("ERROR", e.toString());
            } finally {
                if (c != null && !c.isClosed()) {
                    c.close();
                }
            }
        }
        adapter.updateData(searchResults);
    }

    public ArrayList<ListItem> showLatestImages() {
        isSketchIncluded = includeSketchesToggle.isChecked();

        String query;

        if (isSketchIncluded) {
            query = "SELECT * FROM BOTH";
        } else {
            query = "SELECT * FROM BOTH WHERE TYPE = 'photo'";
        }

        Cursor c = bigDb.rawQuery(query, null);
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

    void makeHttpRequest(String c, String[] k) throws JSONException {
        JSONObject data = new JSONObject();
        data.put("context", c);
        data.put("max_tokens", 250);
        data.put("mode", "twitter");
        data.put("model", "chat-sophos-1");

        String[] keywords = k;
        data.put("keywords", new JSONArray(keywords));

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, data, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject data = response.getJSONObject("data");
                    JSONArray outArr = data.getJSONArray("outputs");
                    JSONObject newRes = outArr.getJSONObject(0);
                    story.setText("Response: " + newRes.getString("text"));
                    tts.speak(newRes.getString("text"), TextToSpeech.QUEUE_FLUSH, null, null);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("error", new String(error.networkResponse.data));
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Bearer " + Key.CORETEXT_API_KEY);
                return headers;
            }
        };
        RequestQueue rq = Volley.newRequestQueue(this);
        rq.add(req);
    }

    public void onSubmit(View view) throws JSONException {
        Toast toast = Toast.makeText(this, "Please select some tags", Toast.LENGTH_SHORT);
        try {
            contextString = "story";
            keywordsString = selectedTags.toString();
            if (keywordsString.isEmpty() || keywordsString.equals("")) {
                toast.show();
                story.setText("");
                throw new RuntimeException();
            }

            String[] k = keywordsString.split(",");

            for (int i = 0; i < k.length; i++) {
                k[i] = k[i].trim();
            }
            makeHttpRequest(contextString, k);
        } catch (Exception e) {
            toast.show();
        }
    }
}