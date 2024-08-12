package com.example.inventoryappjava;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    private Context context;
    private List<InventoryItem> inventoryList;
    private List<InventoryItem> filteredInventoryList;
    private InventoryActivity inventoryActivity;

    public InventoryAdapter(Context context, List<InventoryItem> inventoryList, InventoryActivity inventoryActivity) {
        this.context = context;
        this.inventoryList = inventoryList;
        this.filteredInventoryList = new ArrayList<>(inventoryList);
        this.inventoryActivity = inventoryActivity;
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.inventory_item, parent, false);
        return new InventoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        InventoryItem item = filteredInventoryList.get(position);
        holder.textViewName.setText(item.getName());
        holder.textViewQuantity.setText(String.valueOf(item.getQuantity()));

        holder.itemView.setOnClickListener(v -> inventoryActivity.showEditItemDialog(item));
        holder.deleteButton.setOnClickListener(v -> showDeleteItemDialog(item));
    }

    @Override
    public int getItemCount() {
        return filteredInventoryList.size();
    }

    public void filter(String query) {
        if (query.isEmpty()) {
            filteredInventoryList.clear();
            filteredInventoryList.addAll(inventoryList);
        } else {
            List<InventoryItem> filteredList = new ArrayList<>();
            for (InventoryItem item : inventoryList) {
                if (item.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(item);
                }
            }
            filteredInventoryList.clear();
            filteredInventoryList.addAll(filteredList);
        }
        notifyDataSetChanged();
    }

    public void addItem(InventoryItem item) {
        inventoryList.add(item);
        filteredInventoryList.add(item);
        notifyItemInserted(filteredInventoryList.size() - 1);
    }

    public void removeItem(InventoryItem item) {
        int index = filteredInventoryList.indexOf(item);
        if (index != -1) {
            filteredInventoryList.remove(index);
            inventoryList.remove(item);
            notifyItemRemoved(index);
        }
    }

    public void setItems(List<InventoryItem> items) {
        this.inventoryList = items;
        this.filteredInventoryList = new ArrayList<>(items);
        notifyDataSetChanged();
    }

    private void showDeleteItemDialog(InventoryItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    inventoryActivity.deleteItemFromDatabase(item.getId());
                    removeItem(item);
                })
                .setNegativeButton("No", null)
                .create()
                .show();
    }

    static class InventoryViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewQuantity;
        Button deleteButton;

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.text_view_name);
            textViewQuantity = itemView.findViewById(R.id.text_view_quantity);
            deleteButton = itemView.findViewById(R.id.button_delete);
        }
    }
}
