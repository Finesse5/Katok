package com.example.katok;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "katok.db";
    private static final int DATABASE_VERSION = 4; // Увеличен номер версии

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Создаем таблицу пользователей
        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "first_name TEXT, " +
                "last_name TEXT, " +
                "phone_number TEXT UNIQUE)"); // Уникальный номер телефона

        // Создаем таблицу записей на каток
        db.execSQL("CREATE TABLE appointments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " + // Внешний ключ для привязки к пользователю
                "appointment_time TEXT, " +
                "phone_number TEXT, " + // Колонка для номера телефона
                "FOREIGN KEY(user_id) REFERENCES users(id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Обновляем структуру базы данных, при необходимости добавляем новые поля или таблицы
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE appointments ADD COLUMN phone_number TEXT;");
        }

        if (oldVersion < 3) {
            // Если в следующей версии базы данных потребуется обновить структуру,
            // например, изменив тип данных или добавив другие ограничения, делаем это здесь.
            // Например, если вам нужно что-то изменить в таблице "appointments":
            // db.execSQL("ALTER TABLE appointments ADD COLUMN new_column_name TEXT;");
        }
    }

    // Метод для получения user_id по номеру телефона
    public long getUserIdByPhoneNumber(SQLiteDatabase db, String phoneNumber) {
        Cursor cursor = db.query(
                "users",                  // Таблица
                new String[]{"id"},        // Поле, которое мы хотим получить (id)
                "phone_number = ?",       // Условие поиска по номеру телефона
                new String[]{phoneNumber},// Параметр запроса (номер телефона)
                null, null, null);        // Не используем GROUP BY, ORDER BY и т.д.

        if (cursor != null && cursor.moveToFirst()) {
            long userId = cursor.getLong(cursor.getColumnIndex("id"));
            cursor.close();
            return userId;
        }
        cursor.close();
        return -1;  // Если пользователь не найден, возвращаем -1
    }
    // Метод для проверки, занято ли указанное время
    public boolean isTimeSlotTaken(SQLiteDatabase db, String phoneNumber, String formattedDateTime) {
        Cursor cursor = db.query(
                "appointments",                   // Таблица
                new String[]{"id"},               // Проверяем только id
                "appointment_time = ?",           // Условие
                new String[]{formattedDateTime},  // Параметр
                null, null, null
        );

        boolean isTaken = (cursor != null && cursor.getCount() > 0);

        if (cursor != null) {
            cursor.close();
        }

        return isTaken;
    }

    // Метод для сохранения записи в таблице appointments
    public void saveAppointment(SQLiteDatabase db, String phoneNumber, String formattedDateTime) {
        // Получаем user_id по номеру телефона
        long userId = getUserIdByPhoneNumber(db, phoneNumber);
        if (userId == -1) {
            Log.e("DatabaseHelper", "Пользователь с таким номером телефона не найден");
            return;
        }

        // Создаем объект ContentValues для вставки записи
        ContentValues values = new ContentValues();
        values.put("phone_number", phoneNumber);
        values.put("appointment_time", formattedDateTime);
        values.put("user_id", userId);  // Привязываем запись к user_id

        long result = db.insert("appointments", null, values);
        if (result == -1) {
            Log.e("DatabaseHelper", "Ошибка при сохранении записи в appointments");
        } else {
            Log.d("DatabaseHelper", "Запись успешно сохранена!");
        }
    }
}
