package com.example.katok;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Locale;

public class RecordActivity extends AppCompatActivity {

    private TextView tvPhoneNumber, tvDate, tvTime, tvUserRecords;
    private Button btnSelectDate, btnSelectTime, btnSaveRecord;
    private SQLiteDatabase db;
    private String phoneNumber;
    private String selectedTime = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        tvUserRecords = findViewById(R.id.tvUserRecords); // Для отображения записей
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSelectTime = findViewById(R.id.btnSelectTime);
        btnSaveRecord = findViewById(R.id.btnSaveRecord);

        // Подключаем базу данных
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();

        // Получаем номер телефона, переданный из LoginActivity или SignUpActivity
        phoneNumber = getIntent().getStringExtra("phone_number");
        if (phoneNumber != null) {
            tvPhoneNumber.setText("Номер телефона: " + phoneNumber);
        } else {
            tvPhoneNumber.setText("Номер телефона не найден");
        }

        // Логирование информации о номере телефона
        Log.d("RecordActivity", "Phone Number: " + phoneNumber);

        // Загрузить записи пользователя
        loadUserRecords();

        // Выбор даты
        btnSelectDate.setOnClickListener(v -> showDatePicker());

        // Выбор времени
        btnSelectTime.setOnClickListener(v -> showTimePicker());

        // Сохранение записи
        btnSaveRecord.setOnClickListener(v -> saveRecordToDatabase());
    }

    // Метод для загрузки записей пользователя из базы данных
    private void loadUserRecords() {
        StringBuilder records = new StringBuilder();

        // Получаем записи для данного номера телефона
        Cursor cursor = db.query(
                "appointments",
                new String[]{"appointment_time"}, // Поля, которые мы хотим получить
                "phone_number = ?",              // Условие (по номеру телефона)
                new String[]{phoneNumber},       // Параметр условия
                null, null, null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String appointmentTime = cursor.getString(cursor.getColumnIndex("appointment_time"));
                records.append("- ").append(appointmentTime).append("\n");
            }
            cursor.close();
        }

        // Если записей нет
        if (records.length() == 0) {
            tvUserRecords.setText("Нет записей для данного пользователя.");
        } else {
            tvUserRecords.setText("Ваши записи:\n" + records.toString());
        }
    }

    // Метод для выбора даты
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    tvDate.setText("Выбрана дата: " + selectedDate);

                    Log.d("RecordActivity", "Selected Date: " + selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    // Метод для выбора времени
    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    tvTime.setText(selectedTime);

                    Log.d("RecordActivity", "Selected Time: " + selectedTime);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }

    // Метод для сохранения записи в базе данных
    private void saveRecordToDatabase() {
        String selectedDate = tvDate.getText().toString();
        String selectedTime = tvTime.getText().toString();

        Log.d("RecordActivity", "Saving Record - Date: " + selectedDate + ", Time: " + selectedTime);

        if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
            Toast.makeText(this, "Выберите дату и время", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDate.startsWith("Выбрана дата: ")) {
            selectedDate = selectedDate.replace("Выбрана дата: ", "");
        }

        String formattedDateTime = formatDateTime(selectedDate, selectedTime);

        if (formattedDateTime == null) {
            Toast.makeText(this, "Ошибка формата времени", Toast.LENGTH_SHORT).show();
            return;
        }

        // Проверяем, доступно ли указанное время
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        if (dbHelper.isTimeSlotTaken(db, phoneNumber, formattedDateTime)) {
            Toast.makeText(this, "Это время уже занято. Выберите другое.", Toast.LENGTH_SHORT).show();
            return;
        }

        long userId = dbHelper.getUserIdByPhoneNumber(db, phoneNumber);
        if (userId == -1) {
            Toast.makeText(this, "Пользователь с таким номером телефона не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put("phone_number", phoneNumber);
        values.put("appointment_time", formattedDateTime);
        values.put("user_id", userId);

        long result = db.insert("appointments", null, values);
        if (result != -1) {
            Toast.makeText(this, "Запись успешно сохранена!", Toast.LENGTH_SHORT).show();
            loadUserRecords(); // Обновить список записей
        } else {
            Toast.makeText(this, "Ошибка при сохранении записи.", Toast.LENGTH_SHORT).show();
        }
    }


    private String formatDateTime(String date, String time) {
        try {
            Log.d("RecordActivity", "Formatting DateTime - Date: " + date + ", Time: " + time);

            String[] dateParts = date.split("/");
            String[] timeParts = time.split(":");

            int day = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]) - 1;
            int year = Integer.parseInt(dateParts[2]);
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, day, hour, minute, 0);

            return String.format("%04d-%02d-%02d %02d:%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE));
        } catch (Exception e) {
            Log.e("RecordActivity", "Error formatting date and time", e);
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}
