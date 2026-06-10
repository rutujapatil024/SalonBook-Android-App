package com.salonbook.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.salonbook.app.R;
import com.salonbook.app.databinding.ItemAppointmentBinding;
import com.salonbook.app.models.Appointment;
import com.salonbook.app.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    private List<Appointment> appointments;
    private OnAppointmentActionListener listener;
    private boolean isOwner;

    public interface OnAppointmentActionListener {
        void onViewDetails(Appointment appointment);
        void onConfirm(Appointment appointment);
        void onCancel(Appointment appointment);
        void onComplete(Appointment appointment);
        void onCall(Appointment appointment);
    }

    public AppointmentAdapter(OnAppointmentActionListener listener, boolean isOwner) {
        this.appointments = new ArrayList<>();
        this.listener = listener;
        this.isOwner = isOwner;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAppointmentBinding binding = ItemAppointmentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new AppointmentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Appointment appointment = appointments.get(position);
        holder.bind(appointment);
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    public void setAppointments(List<Appointment> appointments) {
        this.appointments = new ArrayList<>(appointments);
        notifyDataSetChanged();
    }

    class AppointmentViewHolder extends RecyclerView.ViewHolder {
        private final ItemAppointmentBinding binding;

        AppointmentViewHolder(ItemAppointmentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Appointment appointment) {
            binding.tvCustomerName.setText(appointment.getCustomerName());
            binding.tvServiceName.setText(appointment.getServiceName());
            binding.tvStylistName.setText(appointment.getStylistName());
            binding.tvDate.setText(appointment.getDate());
            binding.tvTimeSlot.setText(appointment.getTimeSlot());
            binding.tvStatus.setText(appointment.getStatus());

            // Status badge styling
            int bgRes;
            int textColor;
            switch (appointment.getStatus()) {
                case Constants.STATUS_CONFIRMED:
                    bgRes = R.drawable.status_confirmed;
                    textColor = R.color.status_confirmed;
                    break;
                case Constants.STATUS_COMPLETED:
                    bgRes = R.drawable.status_completed;
                    textColor = R.color.status_completed;
                    break;
                case Constants.STATUS_CANCELLED:
                    bgRes = R.drawable.status_cancelled;
                    textColor = R.color.status_cancelled;
                    break;
                default: // PENDING
                    bgRes = R.drawable.status_pending;
                    textColor = R.color.status_pending;
                    break;
            }
            binding.tvStatus.setBackgroundResource(bgRes);
            binding.tvStatus.setTextColor(ContextCompat.getColor(
                    itemView.getContext(), textColor));

            // Popup menu on 3-dot icon
            binding.ivMore.setOnClickListener(v -> showPopupMenu(v, appointment));

            // Click to view details
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onViewDetails(appointment);
            });
        }

        private void showPopupMenu(View anchor, Appointment appointment) {
            PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
            popup.inflate(R.menu.menu_popup_appointment);

            // Hide/show based on role & status
            if (!isOwner) {
                popup.getMenu().findItem(R.id.popup_confirm).setVisible(false);
                popup.getMenu().findItem(R.id.popup_complete).setVisible(false);
                popup.getMenu().findItem(R.id.popup_call).setTitle(R.string.call_salon);
            }

            if (!appointment.getStatus().equals(Constants.STATUS_PENDING)) {
                popup.getMenu().findItem(R.id.popup_confirm).setVisible(false);
                popup.getMenu().findItem(R.id.popup_cancel).setVisible(false);
            }

            if (!appointment.getStatus().equals(Constants.STATUS_CONFIRMED)) {
                popup.getMenu().findItem(R.id.popup_complete).setVisible(false);
            }

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.popup_view_details) {
                    if (listener != null) listener.onViewDetails(appointment);
                    return true;
                } else if (id == R.id.popup_confirm) {
                    if (listener != null) listener.onConfirm(appointment);
                    return true;
                } else if (id == R.id.popup_cancel) {
                    if (listener != null) listener.onCancel(appointment);
                    return true;
                } else if (id == R.id.popup_complete) {
                    if (listener != null) listener.onComplete(appointment);
                    return true;
                } else if (id == R.id.popup_call) {
                    if (listener != null) listener.onCall(appointment);
                    return true;
                }
                return false;
            });

            popup.show();
        }
    }
}
