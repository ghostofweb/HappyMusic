package com.example.musicplayer;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ListView listView;
    private SearchView searchView;
    private List<String> mp3List;
    private List<String> filteredList;
    private CustomAdapter adapter;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listViewSong);
        searchView = findViewById(R.id.searchView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermission(Manifest.permission.READ_MEDIA_AUDIO);
        } else {
            requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void requestPermission(String permission) {
        Dexter.withContext(this)
                .withPermission(permission)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        Log.d(TAG, "Permission Granted");
                        loadMp3Files();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Log.d(TAG, "Permission Denied");
                        // Handle the case when permission is denied
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(com.karumi.dexter.listener.PermissionRequest permission, PermissionToken token) {
                        Log.d(TAG, "Permission Rationale Should Be Shown");
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    private void loadMp3Files() {
        Log.d(TAG, "Loading MP3 Files");
        mp3List = new ArrayList<>();
        filteredList = new ArrayList<>();
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE
        };
        Cursor cursor = contentResolver.query(uri, projection, selection, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            do {
                long id = cursor.getLong(idColumn);
                String title = cursor.getString(titleColumn);
                Uri songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                mp3List.add(songUri.toString() + " - " + title);
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            Log.d(TAG, "Cursor is null or empty");
        }

        if (mp3List.isEmpty()) {
            Log.d(TAG, "No MP3 files found");
        } else {
            filteredList.addAll(mp3List);
            adapter = new CustomAdapter(filteredList);
            listView.setAdapter(adapter);
            Log.d(TAG, "MP3 Files loaded successfully");
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                String selectedMp3 = filteredList.get(position).split(" - ")[0];
                Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                intent.putExtra("songList", new ArrayList<>(filteredList));
                intent.putExtra("position", position);
                startActivity(intent);
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterSongs(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSongs(newText);
                return true;
            }
        });
    }

    private void filterSongs(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(mp3List);
        } else {
            String lowerCaseQuery = query.toLowerCase(Locale.ROOT);
            for (String song : mp3List) {
                if (song.toLowerCase(Locale.ROOT).contains(lowerCaseQuery)) {
                    filteredList.add(song);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    class CustomAdapter extends BaseAdapter {

        private List<String> items;

        CustomAdapter(List<String> items) {
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_items, parent, false);
            }
            TextView textSong = convertView.findViewById(R.id.txtsongname);
            textSong.setText(items.get(position).split(" - ")[1]);
            ImageView songImage = convertView.findViewById(R.id.imgsong);
            // Optionally set an image or other properties for songImage here
            return convertView;
        }
    }
}
