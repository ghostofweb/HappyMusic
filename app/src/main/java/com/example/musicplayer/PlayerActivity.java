package com.example.musicplayer;

import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class PlayerActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private SeekBar seekBar;
    private TextView txtsname, txtsstart, txtsstop;
    private Button playbtn;
    private boolean isPlaying = false;
    private boolean isShuffle = false; // Track shuffle state
    private int currentPosition = 0; // Track the current position of the song
    private ImageView btnnext, btnprev, btnff, btnfr, btnShuffle, imageView;
    private ArrayList<String> mp3List; // List of songs
    private ArrayList<String> shuffledList; // List for shuffle mode
    private int position; // Current song position

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // Initialize UI components
        txtsname = findViewById(R.id.txtsname);
        txtsstart = findViewById(R.id.txtsstart);
        txtsstop = findViewById(R.id.txtsstop);
        playbtn = findViewById(R.id.playbtn);
        btnnext = findViewById(R.id.btnnext);
        btnprev = findViewById(R.id.btnprev);
        seekBar = findViewById(R.id.seekbar);
        imageView = findViewById(R.id.imgview);
        btnff = findViewById(R.id.btnff);
        btnfr = findViewById(R.id.btnfr);
        btnShuffle = findViewById(R.id.btnshuffle); // Shuffle button

        // Get song list and position from intent
        Intent intent = getIntent();
        mp3List = intent.getStringArrayListExtra("songList");
        position = intent.getIntExtra("position", 0);

        if (mp3List != null && !mp3List.isEmpty()) {
            String songUri = mp3List.get(position).split(" - ")[0];
            initializeMediaPlayer(Uri.parse(songUri));
        }

        // Set up SeekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Optional: Handle progress changes
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Optional: Handle start of tracking
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(seekBar.getProgress());
                }
            }
        });

        // Update SeekBar and time labels periodically
        final Handler handler = new Handler();
        final int delay = 1000; // 1 second
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying) {
                    txtsstart.setText(createTime(mediaPlayer.getCurrentPosition()));
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                }
                handler.postDelayed(this, delay);
            }
        }, delay);

        // Play button click listener
        playbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer != null) {
                    if (isPlaying) {
                        mediaPlayer.pause();
                        currentPosition = mediaPlayer.getCurrentPosition();
                        playbtn.setBackgroundResource(R.drawable.baseline_play_arrow_24); // Play icon
                    } else {
                        mediaPlayer.seekTo(currentPosition);
                        mediaPlayer.start();
                        playbtn.setBackgroundResource(R.drawable.baseline_pause_24); // Pause icon
                    }
                    isPlaying = !isPlaying;
                }
            }
        });




        // Fast forward button click listener
        btnff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int newPosition = mediaPlayer.getCurrentPosition() + 10000; // Fast forward 10 seconds
                    mediaPlayer.seekTo(Math.min(newPosition, mediaPlayer.getDuration())); // Ensure not going past end
                }
            }
        });

        // Fast rewind button click listener
        btnfr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int newPosition = mediaPlayer.getCurrentPosition() - 10000; // Rewind 10 seconds
                    mediaPlayer.seekTo(Math.max(newPosition, 0)); // Ensure not going past start
                }
            }
        });

        // Next button click listener
        btnnext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                    position = (position + 1) % getCurrentList().size();
                    String nextSongUri = getCurrentList().get(position).split(" - ")[0];
                    initializeMediaPlayer(Uri.parse(nextSongUri));
                }
            }
        });

        // Previous button click listener
        btnprev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                    position = (position - 1 < 0) ? (getCurrentList().size() - 1) : (position - 1);
                    String prevSongUri = getCurrentList().get(position).split(" - ")[0];
                    initializeMediaPlayer(Uri.parse(prevSongUri));
                }
            }
        });

        // Shuffle button click listener
        btnShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isShuffle = !isShuffle;
                if (isShuffle) {
                    btnShuffle.setBackgroundResource(R.drawable.baseline_shuffle_on_24); // Pressed icon
                    shuffledList = new ArrayList<>(mp3List);
                    Collections.shuffle(shuffledList); // Shuffle the list
                } else {
                    btnShuffle.setBackgroundResource(R.drawable.baseline_shuffle_24); // Default icon
                    shuffledList = null; // Clear the shuffled list
                }
            }
        });
    }

    private void initializeMediaPlayer(Uri uri) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(getApplicationContext(), uri);
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;
            playbtn.setBackgroundResource(R.drawable.baseline_pause_24); // Pause icon
            seekBar.setMax(mediaPlayer.getDuration());
            txtsname.setText(getFileName(uri));
            txtsstop.setText(createTime(mediaPlayer.getDuration()));
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    btnnext.performClick(); // Automatically go to next song
                }
            });
        } catch (IOException e) {
            Log.e("PlayerActivity", "Error initializing media player", e);
        }
    }

    private String createTime(int duration) {
        int minutes = (duration / 1000) / 60;
        int seconds = (duration / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private String getFileName(Uri uri) {
        String[] projection = {MediaStore.Audio.Media.DISPLAY_NAME};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME));
            cursor.close();
            return name;
        }
        return "Unknown";
    }

    private ArrayList<String> getCurrentList() {
        return isShuffle ? shuffledList : mp3List; // Return shuffled list if shuffle is on
    }

    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }
}

