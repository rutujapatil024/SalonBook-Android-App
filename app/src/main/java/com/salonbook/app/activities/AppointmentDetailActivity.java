package com.salonbook.app.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.salonbook.app.R;
import com.salonbook.app.databinding.ActivityAppointmentDetailBinding;
import com.salonbook.app.models.Appointment;
import com.salonbook.app.utils.Constants;
import com.salonbook.app.utils.LocaleHelper;
import com.salonbook.app.utils.SessionManager;

public class AppointmentDetailActivity extends AppCompatActivity {

    private ActivityAppointmentDetailBinding binding;
    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private Appointment appointment;
    private String appointmentId;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.attachBaseContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAppointmentDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        appointmentId = getIntent().getStringExtra(Constants.EXTRA_APPOINTMENT_ID);
        if (appointmentId != null) loadAppointment();

        binding.btnConfirm.setOnClickListener(v -> updateStatus(Constants.STATUS_CONFIRMED));
        binding.btnComplete.setOnClickListener(v -> updateStatus(Constants.STATUS_COMPLETED));
        binding.btnCancel.setOnClickListener(v -> confirmCancel());
        binding.btnCustomerCancel.setOnClickListener(v -> confirmCancel());
        binding.btnCall.setOnClickListener(v -> makeCall());
    }

    private void loadAppointment() {
        binding.progressDetail.setVisibility(View.VISIBLE);
        db.collection(Constants.COLLECTION_APPOINTMENTS).document(appointmentId)
                .get()
                .addOnSuccessListener(doc -> {
                    binding.progressDetail.setVisibility(View.GONE);
                    if (doc.exists()) {
                        appointment = doc.toObject(Appointment.class);
                        if (appointment != null) {
                            appointment.setId(doc.getId());
                            displayAppointment();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressDetail.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show();
                });
    }

    private void displayAppointment() {
        binding.tvCustomerName.setText(appointment.getCustomerName());
        binding.tvCustomerPhone.setText(appointment.getCustomerPhone());
        binding.tvServiceName.setText(appointment.getServiceName());
        binding.tvStylistName.setText(appointment.getStylistName());
        binding.tvDate.setText(appointment.getDate());
        binding.tvTimeSlot.setText(appointment.getTimeSlot());
        binding.tvStatus.setText(appointment.getStatus());

        // Status styling
        int bgRes, textColor;
        switch (appointment.getStatus()) {
            case Constants.STATUS_CONFIRMED:
                bgRes = R.drawable.status_confirmed; textColor = R.color.status_confirmed; break;
            case Constants.STATUS_COMPLETED:
                bgRes = R.drawable.status_completed; textColor = R.color.status_completed; break;
            case Constants.STATUS_CANCELLED:
                bgRes = R.drawable.status_cancelled; textColor = R.color.status_cancelled; break;
            default:
                bgRes = R.drawable.status_pending; textColor = R.color.status_pending; break;
        }
        binding.tvStatus.setBackgroundResource(bgRes);
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, textColor));

        // Show role-based actions
        if (sessionManager.isOwner()) {
            binding.layoutOwnerActions.setVisibility(View.VISIBLE);
            binding.btnCall.setText(R.string.call_customer);
            if (!appointment.getStatus().equals(Constants.STATUS_PENDING)) {
                binding.btnConfirm.setVisibility(View.GONE);
            }
            if (!appointment.getStatus().equals(Constants.STATUS_CONFIRMED)) {
                binding.btnComplete.setVisibility(View.GONE);
            }
            if (appointment.getStatus().equals(Constants.STATUS_COMPLETED) ||
                    appointment.getStatus().equals(Constants.STATUS_CANCELLED)) {
                binding.btnCancel.setVisibility(View.GONE);
            }
        } else {
            binding.btnCall.setText(R.string.call_salon);
            if (appointment.getStatus().equals(Constants.STATUS_PENDING)) {
                binding.btnCustomerCancel.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateStatus(String status) {
        binding.progressDetail.setVisibility(View.VISIBLE);
        db.collection(Constants.COLLECTION_APPOINTMENTS).document(appointmentId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    // Send broadcast
                    Intent broadcast = new Intent(Constants.ACTION_APPOINTMENT_STATUS_CHANGED);
                    broadcast.putExtra("status", status);
                    sendBroadcast(broadcast);

                    Toast.makeText(this, R.string.appointment_status_changed, Toast.LENGTH_SHORT).show();
                    loadAppointment();
                })
                .addOnFailureListener(e -> {
                    binding.progressDetail.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmCancel() {
        new MaterialAlertDialogBuilder(this, R.style.SalonBook_Dialog)
                .setTitle(R.string.cancel_appointment)
                .setMessage(R.string.confirm_cancel_appointment)
                .setPositiveButton(R.string.yes, (d, w) -> updateStatus(Constants.STATUS_CANCELLED))
                .setNegativeButton(R.string.no, null).show();
    }

    private void makeCall() {
        String phone = appointment != null ? appointment.getCustomerPhone() : "";
        if (phone != null && !phone.isEmpty()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phone)));
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, 100);
            }
        }
    }
}
