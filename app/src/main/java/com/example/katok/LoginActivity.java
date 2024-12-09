package com.example.katok;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private EditText etPhoneNumber;
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnSignUp = findViewById(R.id.btnSignUp);

        // Подключаем базу данных
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();

        // Кнопка для входа
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = etPhoneNumber.getText().toString().trim();

                if (phoneNumber.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Введите номер телефона", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Проверяем, существует ли такой номер телефона в базе данных
                Cursor cursor = db.rawQuery("SELECT * FROM users WHERE phone_number = ?", new String[]{phoneNumber});
                if (cursor.moveToFirst()) {
                    // Пользователь найден, переходим на экран записи на каток
                    Intent intent = new Intent(LoginActivity.this, RecordActivity.class);
                    intent.putExtra("phone_number", phoneNumber); // Передаем номер телефона
                    startActivity(intent);
                    finish();
                } else {
                    // Номер не найден, показываем сообщение и предлагаем зарегистрироваться
                    Toast.makeText(LoginActivity.this, "Пользователь не найден. Зарегистрируйтесь", Toast.LENGTH_SHORT).show();
                }
                cursor.close();
            }
        });

        // Кнопка для перехода к регистрации
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}
