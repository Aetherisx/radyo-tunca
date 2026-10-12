package com.example.radyotunca;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface RadioDao {

    @Query("SELECT * FROM radios")
    List<Radio> getAllRadios();

    @Query("SELECT * FROM radios WHERE category = :kategori")
    List<Radio> getRadiosByCategory(String kategori);

    @Query("SELECT * FROM radios WHERE isFavorite = 1")
    List<Radio> getFavoriteRadios();

    @Query("SELECT * FROM radios WHERE name LIKE '%' || :aramaKelimesi || '%'")
    List<Radio> searchRadios(String aramaKelimesi);

    @Insert
    void insertAll(Radio... radios);

    @Update
    void updateRadio(Radio radio);
}