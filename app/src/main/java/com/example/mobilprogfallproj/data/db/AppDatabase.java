package com.example.mobilprogfallproj.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.mobilprogfallproj.data.db.dao.MovieDao;
import com.example.mobilprogfallproj.data.db.entities.MovieEntity;

@Database(
        entities = {
                MovieEntity.class
        },
        version = 5,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase INSTANCE;

    public abstract MovieDao movieDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "cinematch_db"
            )
            .fallbackToDestructiveMigration() // Geliştirme için - sürüm değiştiğinde veritabanını temizler
            .build();
        }
        return INSTANCE;
    }
}

