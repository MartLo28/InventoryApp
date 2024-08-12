package com.example.inventoryappjava;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SignupActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private EditText editTextUsername;
    private EditText editTextPassword;
    private static final String TAG = "SignupActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        dbHelper = new DatabaseHelper(this);
        editTextUsername = findViewById(R.id.edit_text_username);
        editTextPassword = findViewById(R.id.edit_text_password);

        Button buttonSignup = findViewById(R.id.button_signup);
        buttonSignup.setOnClickListener(v -> signup());
    }

    private void signup() {
        String username = editTextUsername.getText().toString();
        String password = editTextPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter a username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("Username", username);
        values.put("Password", password);

        long newRowId = db.insert("Users", null, values);
        if (newRowId != -1) {
            Log.i(TAG, "Signup successful. Row ID: " + newRowId);
            Toast.makeText(this, "Signup successful", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            Log.e(TAG, "Error during signup");
            Toast.makeText(this, "Error during signup", Toast.LENGTH_SHORT).show();
        }
    }
}
