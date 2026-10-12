package com.example.radyotunca;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RadioAdapter adapter;
    private AppDatabase database;
    private MediaPlayer mediaPlayer;
    private Radio currentlyPlayingRadio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
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


        com.google.android.material.floatingactionbutton.FloatingActionButton fabAi = findViewById(R.id.fabAiRecommend);
        fabAi.setOnClickListener(v -> recommendRadioWithAI());


        adapter.setOnItemClickListener(new RadioAdapter.OnItemClickListener() {
            @Override
            public void onPlayClick(Radio radio) {
                playOrStopRadio(radio);
            }

            @Override
            public void onFavoriteClick(Radio radio) {
                toggleFavorite(radio);
            }
        });


        loadRadios();


        android.widget.SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new android.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                filterRadios(newText);
                return true;
            }
        });

        com.google.android.material.floatingactionbutton.FloatingActionButton fabMood = findViewById(R.id.fabMood);
        fabMood.setOnClickListener(v -> showMoodDialog());
    }

    private void loadRadios() {
        new Thread(() -> {
            List<Radio> radios = database.radioDao().getAllRadios();


            if (radios.isEmpty()) {
                try {

                    String apiUrl = "https://gist.githubusercontent.com/Aetherisx/c573c8f4f11f743a5759965029d575ac/raw/6133ff5f8945deaa91e278151f29f74a27d6fd18/radyolar.json";

                    java.net.URL url = new java.net.URL(apiUrl);
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");


                    java.io.InputStream inputStream = connection.getInputStream();
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
                    }
                    reader.close();


                    String jsonStr = stringBuilder.toString();
                    org.json.JSONArray jsonArray = new org.json.JSONArray(jsonStr);
                    Radio[] initialRadios = new Radio[jsonArray.length()];

                    for (int i = 0; i < jsonArray.length(); i++) {
                        org.json.JSONObject obj = jsonArray.getJSONObject(i);

                        Radio r = new Radio();
                        r.name = obj.getString("name");
                        r.category = obj.getString("category");
                        r.streamUrl = obj.getString("streamUrl");
                        r.isFavorite = false;

                        initialRadios[i] = r;
                    }

                    database.radioDao().insertAll(initialRadios);

                    radios = database.radioDao().getAllRadios();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            final List<Radio> finalRadios = radios;
            runOnUiThread(() -> {
                adapter.setRadios(finalRadios);
            });
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

    private void toggleFavorite(Radio radio) {
        radio.isFavorite = !radio.isFavorite;

        new Thread(() -> {
            database.radioDao().updateRadio(radio);

            List<Radio> updatedRadios = database.radioDao().getAllRadios();
            runOnUiThread(() -> adapter.setRadios(updatedRadios));
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void filterRadios(String query) {
        new Thread(() -> {
            List<Radio> filteredList;

            if (query.isEmpty()) {
                filteredList = database.radioDao().getAllRadios();
            } else {
                filteredList = database.radioDao().searchRadios(query);
            }

            runOnUiThread(() -> {
                adapter.setRadios(filteredList);
            });
        }).start();
    }


    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_categories) {
            showCategoryDialog();
            return true;
        } else if (id == R.id.action_favorites) {
            android.content.Intent intent = new android.content.Intent(MainActivity.this, FavoritesActivity.class);
            startActivity(intent);
            return true;

        } else if (id == R.id.action_about) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle(getString(R.string.about_title))
                    .setMessage(getString(R.string.about_message))
                    .setPositiveButton(getString(R.string.ok_button), null)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showCategoryDialog() {
        String[] categories = {
                "Tümü",
                "Pop",
                "Slow",
                "Arabesk",
                "Haber",
                "Spor",
                "Yabancı",
                "Genel",
                "İslami",
                "Elektronik",
                "Jazz"
        };

        new android.app.AlertDialog.Builder(this)
                .setTitle("Bir Kategori Seçin")
                .setItems(categories, (dialog, which) -> {
                    String selectedCategory = categories[which];


                    new Thread(() -> {
                        List<Radio> filteredList;

                        if (which == 0) {
                            filteredList = database.radioDao().getAllRadios();
                        } else {

                            filteredList = database.radioDao().getRadiosByCategory(selectedCategory);
                        }

                        runOnUiThread(() -> {
                            adapter.setRadios(filteredList);
                            Toast.makeText(MainActivity.this, selectedCategory + " radyoları listeleniyor", Toast.LENGTH_SHORT).show();
                        });
                    }).start();
                })
                .show();
    }

    private void showAboutDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Hakkında")
                .setMessage("Bu radyo uygulaması Mobil Programlama Dersi Final Projesi olarak geliştirilmiştir.\n\nGeliştirici: Arda Tunca\nSürüm: 1.0")
                .setPositiveButton("Tamam", null)
                .show();
    }


    private void recommendRadioWithAI() {
        new Thread(() -> {
            List<Radio> allRadios = database.radioDao().getAllRadios();
            List<Radio> favoriteRadios = database.radioDao().getFavoriteRadios();

            if (allRadios.isEmpty()) return;

            Radio recommendedRadio = null;

            if (!favoriteRadios.isEmpty()) {
                int randomFavIndex = new java.util.Random().nextInt(favoriteRadios.size());
                String favoriteCategory = favoriteRadios.get(randomFavIndex).category;

                List<Radio> categoryRadios = database.radioDao().getRadiosByCategory(favoriteCategory);

                if (!categoryRadios.isEmpty()) {
                    recommendedRadio = categoryRadios.get(new java.util.Random().nextInt(categoryRadios.size()));
                }
            }

            if (recommendedRadio == null) {
                recommendedRadio = allRadios.get(new java.util.Random().nextInt(allRadios.size()));
            }

            final Radio finalRadio = recommendedRadio;

            runOnUiThread(() -> {
                new android.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle("✨ Akıllı Öneri Motoru")
                        .setMessage("Analizlerime göre bugün '" + finalRadio.category + "' kategorisinden \n\n🎵 " + finalRadio.name + " \n\nistasyonunu seveceğini düşünüyorum.\n\nHemen çalmaya başlayayım mı?")
                        .setPositiveButton("Evet, Çal", (dialog, which) -> {
                            playRadioFromAI(finalRadio);
                        })
                        .setNegativeButton("Farklı Öner", (dialog, which) -> {
                            recommendRadioWithAI();
                        })
                        .show();
            });
        }).start();
    }


    private void playRadioFromAI(Radio radio) {
        playOrStopRadio(radio);
    }

    private void showMoodDialog() {
        String[] moods = {
                "😎 Mutlu & Enerjik",
                "😢 Hüzünlü & Duygusal",
                "🙏 Huzurlu & Manevi",
                "💪 Heyecanlı & Güçlü",
                "☕ Sakin & Odaklanmış"
        };

        String[] targetCategories = {
                "Pop",
                "Arabesk",
                "İslami",
                "Spor",
                "Jazz"
        };

        new android.app.AlertDialog.Builder(this)
                .setTitle("Şu an nasıl hissediyorsun?")
                .setItems(moods, (dialog, which) -> {

                    String selectedMood = moods[which];
                    String targetCategory = targetCategories[which];

                    new Thread(() -> {
                        List<Radio> categoryRadios = database.radioDao().getRadiosByCategory(targetCategory);

                        if (!categoryRadios.isEmpty()) {
                            Radio selectedRadio = categoryRadios.get(new java.util.Random().nextInt(categoryRadios.size()));

                            runOnUiThread(() -> {
                                android.widget.Toast.makeText(MainActivity.this, selectedMood + " moduna uygun radyo açılıyor:\n" + selectedRadio.name, android.widget.Toast.LENGTH_LONG).show();
                                playOrStopRadio(selectedRadio);
                            });
                        } else {
                            runOnUiThread(() -> {
                                android.widget.Toast.makeText(MainActivity.this, "Bu moda uygun radyo henüz eklenmemiş!", android.widget.Toast.LENGTH_SHORT).show();
                            });
                        }
                    }).start();
                })
                .show();
    }

}