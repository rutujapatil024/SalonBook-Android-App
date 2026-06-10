package com.salonbook.app.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.salonbook.app.databinding.ItemWalkinBinding;
import com.salonbook.app.models.WalkIn;

import java.util.ArrayList;
import java.util.List;

public class WalkInAdapter extends RecyclerView.Adapter<WalkInAdapter.WalkInViewHolder> {

    private List<WalkIn> walkIns;

    public WalkInAdapter() {
        this.walkIns = new ArrayList<>();
    }

    @NonNull
    @Override
    public WalkInViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWalkinBinding binding = ItemWalkinBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new WalkInViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull WalkInViewHolder holder, int position) {
        WalkIn walkIn = walkIns.get(position);
        holder.bind(walkIn);
    }

    @Override
    public int getItemCount() {
        return walkIns.size();
    }

    public void setWalkIns(List<WalkIn> walkIns) {
        this.walkIns = new ArrayList<>(walkIns);
        notifyDataSetChanged();
    }

    class WalkInViewHolder extends RecyclerView.ViewHolder {
        private final ItemWalkinBinding binding;

        WalkInViewHolder(ItemWalkinBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(WalkIn walkIn) {
            binding.tvCustomerName.setText(walkIn.getCustomerName());
            binding.tvPhone.setText(walkIn.getPhone());
            binding.tvServiceName.setText(walkIn.getServiceName());
            binding.tvStylistName.setText(walkIn.getStylistName());
            binding.tvTimeSlot.setText(walkIn.getTimeSlot());
        }
    }
}
