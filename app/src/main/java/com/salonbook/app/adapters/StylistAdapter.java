package com.salonbook.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.salonbook.app.R;
import com.salonbook.app.databinding.ItemStylistBinding;
import com.salonbook.app.models.Stylist;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StylistAdapter extends RecyclerView.Adapter<StylistAdapter.StylistViewHolder> {

    private List<Stylist> stylists;
    private List<Stylist> allStylists;
    private OnStylistClickListener listener;
    private boolean isOwner;

    public interface OnStylistClickListener {
        void onStylistClick(Stylist stylist);
        void onStylistLongClick(Stylist stylist, View view);
        void onStylistEditClick(Stylist stylist);
        void onStylistDeleteClick(Stylist stylist);
    }

    public StylistAdapter(OnStylistClickListener listener, boolean isOwner) {
        this.stylists = new ArrayList<>();
        this.allStylists = new ArrayList<>();
        this.listener = listener;
        this.isOwner = isOwner;
    }

    @NonNull
    @Override
    public StylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStylistBinding binding = ItemStylistBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new StylistViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StylistViewHolder holder, int position) {
        Stylist stylist = stylists.get(position);
        holder.bind(stylist);
    }

    @Override
    public int getItemCount() {
        return stylists.size();
    }

    public void setStylists(List<Stylist> stylists) {
        this.stylists = new ArrayList<>(stylists);
        this.allStylists = new ArrayList<>(stylists);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        stylists.clear();
        if (query.isEmpty()) {
            stylists.addAll(allStylists);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Stylist s : allStylists) {
                if (s.getName().toLowerCase().contains(lowerQuery) ||
                        s.getSpecialization().toLowerCase().contains(lowerQuery)) {
                    stylists.add(s);
                }
            }
        }
        notifyDataSetChanged();
    }

    class StylistViewHolder extends RecyclerView.ViewHolder {
        private final ItemStylistBinding binding;

        StylistViewHolder(ItemStylistBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Stylist stylist) {
            binding.tvStylistName.setText(stylist.getName());
            binding.tvSpecialization.setText(stylist.getSpecialization());
            binding.tvExperience.setText(stylist.getExperience() + " " +
                    itemView.getContext().getString(R.string.years_exp));


            // Available chip
            if (stylist.isAvailable()) {
                binding.chipAvailable.setText(itemView.getContext().getString(R.string.available));
                binding.chipAvailable.setChipBackgroundColorResource(R.color.status_completed_bg);
                binding.chipAvailable.setTextColor(ContextCompat.getColor(
                        itemView.getContext(), R.color.chip_available));
            } else {
                binding.chipAvailable.setText(itemView.getContext().getString(R.string.busy));
                binding.chipAvailable.setChipBackgroundColorResource(R.color.status_cancelled_bg);
                binding.chipAvailable.setTextColor(ContextCompat.getColor(
                        itemView.getContext(), R.color.chip_busy));
            }

            // Removed photo loading

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onStylistClick(stylist);
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onStylistLongClick(stylist, v);
                return true;
            });

            if (isOwner) {
                binding.btnEdit.setVisibility(View.VISIBLE);
                binding.btnDelete.setVisibility(View.VISIBLE);
                binding.btnEdit.setOnClickListener(v -> {
                    if (listener != null) listener.onStylistEditClick(stylist);
                });
                binding.btnDelete.setOnClickListener(v -> {
                    if (listener != null) listener.onStylistDeleteClick(stylist);
                });
            } else {
                binding.btnEdit.setVisibility(View.GONE);
                binding.btnDelete.setVisibility(View.GONE);
            }
        }
    }
}
