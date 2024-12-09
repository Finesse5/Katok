package com.example.katok;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

public class SignUpActivity extends AppCompatActivity {
    private EditText etFirstName, etLastName, etPhoneNumber;
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        Button btnSubmit = findViewById(R.id.btnSubmit);

        // Подключаем базу данных
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String firstName = etFirstName.getText().toString().trim();
                String lastName = etLastName.getText().toString().trim();
                String phoneNumber = etPhoneNumber.getText().toString().trim();

                if (firstName.isEmpty() || lastName.isEmpty() || phoneNumber.isEmpty()) {
                    Toast.makeText(SignUpActivity.this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Проверяем, есть ли уже пользователь с таким номером телефона
                Cursor cursor = db.rawQuery("SELECT * FROM users WHERE phone_number = ?", new String[]{phoneNumber});
                if (cursor.moveToFirst()) {
                    // Пользователь найден, выводим сообщение и предлагаем записаться на каток
                    Toast.makeText(SignUpActivity.this, "Пользователь уже зарегистрирован. Вы можете записаться на каток.", Toast.LENGTH_SHORT).show();
                    // Например, можно перейти на экран записи на каток (вы можете добавить переход к другому экрану)
                    cursor.close();
                    finish();
                } else {
                    // Регистрируем нового пользователя
                    ContentValues values = new ContentValues();
                    values.put("first_name", firstName);
                    values.put("last_name", lastName);
                    values.put("phone_number", phoneNumber);

                    long result = db.insert("users", null, values);
                    if (result != -1) {
                        Toast.makeText(SignUpActivity.this, "Запись добавлена!", Toast.LENGTH_SHORT).show();

                        // Проверка содержимого базы данных (для отладки)
                        Cursor debugCursor = db.rawQuery("SELECT * FROM users", null);
                        if (debugCursor.moveToFirst()) {
                            do {
                                String fname = debugCursor.getString(debugCursor.getColumnIndex("first_name"));
                                String lname = debugCursor.getString(debugCursor.getColumnIndex("last_name"));
                                String phone = debugCursor.getString(debugCursor.getColumnIndex("phone_number"));
                                Log.d("DATABASE", "Имя: " + fname + ", Фамилия: " + lname + ", Телефон: " + phone);
                            } while (debugCursor.moveToNext());
                        }
                        debugCursor.close();

                        // Переход в личный кабинет после успешной регистрации
                        Intent intent = new Intent(SignUpActivity.this, RecordActivity.class);
                        intent.putExtra("phone_number", phoneNumber); // Передаем номер телефона
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(SignUpActivity.this, "Ошибка записи", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}
