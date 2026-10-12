package com.example.radyotunca;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "radios")
public class Radio {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String streamUrl;
    public String category;
    public boolean isFavorite;
}
