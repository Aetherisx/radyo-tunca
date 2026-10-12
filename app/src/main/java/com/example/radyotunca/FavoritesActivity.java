package com.example.radyotunca;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.IOException;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RadioAdapter adapter;
    private AppDatabase database;
    private MediaPlayer mediaPlayer;
    private Radio currentlyPlayingRadio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Favorilerim");
        }

        recyclerView = findViewById(R.id.recyclerViewFavorites);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new RadioAdapter();
        recyclerView.setAdapter(adapter);

        database = AppDatabase.getDatabase(this);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );

        adapter.setOnItemClickListener(new RadioAdapter.OnItemClickListener() {
            @Override
            public void onPlayClick(Radio radio) {
                playOrStopRadio(radio);
            }

            @Override
            public void onFavoriteClick(Radio radio) {
                removeFromFavorites(radio);
            }
        });

        loadFavorites();
    }

    private void loadFavorites() {
        new Thread(() -> {
            List<Radio> favoriteRadios = database.radioDao().getFavoriteRadios();
            runOnUiThread(() -> adapter.setRadios(favoriteRadios));
        }).start();
    }

    private void removeFromFavorites(Radio radio) {
        radio.isFavorite = false;
        new Thread(() -> {
            database.radioDao().updateRadio(radio);
            loadFavorites();
        }).start();
    }

    private void playOrStopRadio(Radio radio) {
        try {
            if (currentlyPlayingRadio != null && currentlyPlayingRadio.name.equals(radio.name) && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.reset();
                currentlyPlayingRadio = null;
                Toast.makeText(this, "Yayın durduruldu", Toast.LENGTH_SHORT).show();
                return;
            }

            mediaPlayer.reset();
            Toast.makeText(this, radio.name + " bağlanıyor... Lütfen bekleyin.", Toast.LENGTH_LONG).show();

            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");

            mediaPlayer.setDataSource(getApplicationContext(), android.net.Uri.parse(radio.streamUrl), headers);

            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                currentlyPlayingRadio = radio;
                Toast.makeText(this, "🎵 " + radio.name + " çalıyor!", Toast.LENGTH_SHORT).show();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "Bağlantı koptu veya istasyon şu an yanıt vermiyor.", Toast.LENGTH_LONG).show();
                mp.reset();
                currentlyPlayingRadio = null;
                return true;
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Radyo linkinde bir sorun oluştu.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }
}