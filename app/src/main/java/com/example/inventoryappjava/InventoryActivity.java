package com.example.inventoryappjava;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends AppCompatActivity {

    private static final String TAG = "InventoryActivity";
    private static final String CHANNEL_ID = "inventory_notifications";
    private static final int NOTIFICATION_PERMISSION_CODE = 1001;
    private static final int LOW_STOCK_THRESHOLD = 5; // Define a threshold for low stock
    private DatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private List<InventoryItem> inventoryList;
    private int userId; // Store the user ID
    private boolean isNotificationPermissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        dbHelper = new DatabaseHelper(this);
        inventoryList = new ArrayList<>();

        // Get the user ID from the intent
        userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId == -1) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "InventoryActivity started with User ID: " + userId);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InventoryAdapter(this, inventoryList, this);
        recyclerView.setAdapter(adapter);

        // Initialize the views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        SearchView searchView = findViewById(R.id.search_view);
        Button addItemButton = findViewById(R.id.add_item_button);

        // Set up toolbar
        setSupportActionBar(toolbar);

        // Set a click listener on the addItemButton
        addItemButton.setOnClickListener(v -> showAddItemDialog());

        // Setup the search view
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Handle the search query submission
                adapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Handle the search text change
                adapter.filter(newText);
                return true;
            }
        });

        // Load existing items from the database
        loadItemsFromDatabase();

        // Request POST_NOTIFICATIONS permission if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            } else {
                isNotificationPermissionGranted = true;
            }
        } else {
            isNotificationPermissionGranted = true;
        }
    }

    public void showAddItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_item, null);
        builder.setView(dialogView);

        EditText itemNameEditText = dialogView.findViewById(R.id.edit_text_item_name);
        EditText itemQuantityEditText = dialogView.findViewById(R.id.edit_text_item_quantity);

        builder.setTitle("Add Item")
                .setPositiveButton("Add", (dialog, which) -> {
                    String itemName = itemNameEditText.getText().toString();
                    String itemQuantityStr = itemQuantityEditText.getText().toString();
                    int itemQuantity = itemQuantityStr.isEmpty() ? 0 : Integer.parseInt(itemQuantityStr);
                    addItemToDatabase(itemName, itemQuantity);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    public void showEditItemDialog(InventoryItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_item, null);
        builder.setView(dialogView);

        EditText itemNameEditText = dialogView.findViewById(R.id.edit_text_item_name);
        EditText itemQuantityEditText = dialogView.findViewById(R.id.edit_text_item_quantity);

        // Pre-fill the dialog with the current item details
        itemNameEditText.setText(item.getName());
        itemQuantityEditText.setText(String.valueOf(item.getQuantity()));

        builder.setTitle("Edit Item")
                .setPositiveButton("Update", (dialog, which) -> {
                    String itemName = itemNameEditText.getText().toString();
                    String itemQuantityStr = itemQuantityEditText.getText().toString();
                    int itemQuantity = itemQuantityStr.isEmpty() ? 0 : Integer.parseInt(itemQuantityStr);
                    updateItemInDatabase(item.getId(), itemName, itemQuantity);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void addItemToDatabase(String name, int quantity) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("Name", name);
        values.put("Quantity", quantity);
        values.put("UserId", userId); // Associate the item with the user

        Log.d(TAG, "Adding item: " + name + " for user ID: " + userId);
        long newRowId = db.insert("Inventory", null, values);

        if (newRowId != -1) {
            Log.i(TAG, "Item added with ID: " + newRowId);
            Toast.makeText(this, "Item added: " + name, Toast.LENGTH_SHORT).show();
            InventoryItem newItem = new InventoryItem((int) newRowId, name, quantity);
            adapter.addItem(newItem);

            // Send notification if permission is granted
            if (isNotificationPermissionGranted) {
                sendNotification("Item Added", "Item " + name + " has been added to your inventory.");
            }

            // Check for low stock and send notification if necessary
            if (quantity < LOW_STOCK_THRESHOLD) {
                sendNotification("Low Stock Alert", "Item " + name + " is running low on stock.");
            }
        } else {
            Log.e(TAG, "Error adding item");
            Toast.makeText(this, "Error adding item", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateItemInDatabase(int itemId, String name, int quantity) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("Name", name);
        values.put("Quantity", quantity);

        String selection = "ID = ?";
        String[] selectionArgs = { String.valueOf(itemId) };

        int count = db.update("Inventory", values, selection, selectionArgs);
        if (count > 0) {
            Log.i(TAG, "Item updated with ID: " + itemId);
            Toast.makeText(this, "Item updated: " + name, Toast.LENGTH_SHORT).show();
            loadItemsFromDatabase(); // Reload items to reflect changes
        } else {
            Log.e(TAG, "Error updating item");
            Toast.makeText(this, "Error updating item", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadItemsFromDatabase() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {
                "ID",
                "Name",
                "Quantity"
        };
        String selection = "UserId = ?";
        String[] selectionArgs = {String.valueOf(userId)};

        Log.d(TAG, "Loading items for user ID: " + userId);
        Cursor cursor = db.query(
                "Inventory",
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        inventoryList.clear(); // Clear the list before adding items
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow("ID"));
            String itemName = cursor.getString(cursor.getColumnIndexOrThrow("Name"));
            int itemQuantity = cursor.getInt(cursor.getColumnIndexOrThrow("Quantity"));
            InventoryItem item = new InventoryItem(id, itemName, itemQuantity);
            inventoryList.add(item);
        }
        Log.d(TAG, "Items loaded from database: " + inventoryList.size());
        cursor.close();
        adapter.setItems(inventoryList); // Update the adapter's list
        adapter.notifyDataSetChanged(); // Notify the adapter of the data change

        // Check for low stock and send notification if necessary
        for (InventoryItem item : inventoryList) {
            if (item.getQuantity() < LOW_STOCK_THRESHOLD && isNotificationPermissionGranted) {
                sendNotification("Low Stock Alert", "Item " + item.getName() + " is running low on stock.");
            }
        }
    }


    public void deleteItemFromDatabase(int itemId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = "ID = ?";
        String[] selectionArgs = { String.valueOf(itemId) };

        int deletedRows = db.delete("Inventory", selection, selectionArgs);
        if (deletedRows > 0) {
            Log.i(TAG, "Item deleted with ID: " + itemId);
            Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show();
            loadItemsFromDatabase(); // Reload items to reflect changes
        } else {
            Log.e(TAG, "Error deleting item");
            Toast.makeText(this, "Error deleting item", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Ensure you have this icon in your res/drawable folder
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // Check for POST_NOTIFICATIONS permission before sending notification
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(0, builder.build());
        } else {
            Log.d(TAG, "Notification permission not granted");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted");
                isNotificationPermissionGranted = true;
            } else {
                Log.d(TAG, "Notification permission denied");
                isNotificationPermissionGranted = false;
            }
        }
    }
}
