package com.example.inventoryappjava;

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

public class LoginActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private EditText editTextUsername;
    private EditText editTextPassword;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);
        editTextUsername = findViewById(R.id.edit_text_username);
        editTextPassword = findViewById(R.id.edit_text_password);

        Button buttonLogin = findViewById(R.id.button_login);
        buttonLogin.setOnClickListener(v -> login());
    }

    private void login() {
        String username = editTextUsername.getText().toString();
        String password = editTextPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter a username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = { "ID" };
        String selection = "Username = ? AND Password = ?";
        String[] selectionArgs = { username, password };

        Cursor cursor = db.query(
                "Users",
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        if (cursor.moveToFirst()) {
            int userId = cursor.getInt(cursor.getColumnIndexOrThrow("ID"));
            Log.d(TAG, "User logged in with ID: " + userId);
            Intent intent = new Intent(LoginActivity.this, InventoryActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
            finish();
        } else {
            Log.e(TAG, "Login failed");
            Toast.makeText(this, "Login failed. Please check your credentials", Toast.LENGTH_SHORT).show();
        }

        cursor.close();
    }
}
