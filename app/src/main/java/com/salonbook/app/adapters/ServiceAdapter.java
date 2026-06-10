package com.salonbook.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.salonbook.app.R;
import com.salonbook.app.databinding.ItemServiceBinding;
import com.salonbook.app.models.Service;

import java.util.ArrayList;
import java.util.List;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder> {

    private List<Service> services;
    private List<Service> allServices;
    private OnServiceClickListener listener;
    private boolean isOwner;

    public interface OnServiceClickListener {
        void onServiceClick(Service service);
        void onServiceLongClick(Service service, View view);
        void onServiceEditClick(Service service);
        void onServiceDeleteClick(Service service);
    }

    public ServiceAdapter(OnServiceClickListener listener, boolean isOwner) {
        this.services = new ArrayList<>();
        this.allServices = new ArrayList<>();
        this.listener = listener;
        this.isOwner = isOwner;
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemServiceBinding binding = ItemServiceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ServiceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        Service service = services.get(position);
        holder.bind(service);
    }

    @Override
    public int getItemCount() {
        return services.size();
    }

    public void setServices(List<Service> services) {
        this.services = new ArrayList<>(services);
        this.allServices = new ArrayList<>(services);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        services.clear();
        if (query.isEmpty()) {
            services.addAll(allServices);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Service s : allServices) {
                if (s.getName().toLowerCase().contains(lowerQuery) ||
                        s.getCategory().toLowerCase().contains(lowerQuery)) {
                    services.add(s);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void filterByCategory(String category) {
        services.clear();
        if (category == null || category.isEmpty()) {
            services.addAll(allServices);
        } else {
            for (Service s : allServices) {
                if (s.getCategory().equalsIgnoreCase(category)) {
                    services.add(s);
                }
            }
        }
        notifyDataSetChanged();
    }

    class ServiceViewHolder extends RecyclerView.ViewHolder {
        private final ItemServiceBinding binding;

        ServiceViewHolder(ItemServiceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Service service) {
            binding.tvServiceName.setText(service.getName());
            binding.chipCategory.setText(service.getCategory());
            binding.tvDuration.setText(service.getDuration() + " " +
                    itemView.getContext().getString(R.string.min_text));
            binding.tvPrice.setText("₹" + String.format("%.0f", service.getPrice()));

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onServiceClick(service);
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onServiceLongClick(service, v);
                return true;
            });

            if (isOwner) {
                binding.btnEdit.setVisibility(View.VISIBLE);
                binding.btnDelete.setVisibility(View.VISIBLE);
                binding.btnEdit.setOnClickListener(v -> {
                    if (listener != null) listener.onServiceEditClick(service);
                });
                binding.btnDelete.setOnClickListener(v -> {
                    if (listener != null) listener.onServiceDeleteClick(service);
                });
            } else {
                binding.btnEdit.setVisibility(View.GONE);
                binding.btnDelete.setVisibility(View.GONE);
            }
        }
    }
}
