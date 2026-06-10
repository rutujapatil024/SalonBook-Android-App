package com.salonbook.app.activities;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.salonbook.app.R;
import com.salonbook.app.databinding.ActivityBookAppointmentBinding;
import com.salonbook.app.models.Appointment;
import com.salonbook.app.receivers.AppointmentReceiver;
import com.salonbook.app.utils.Constants;
import com.salonbook.app.utils.LocaleHelper;
import com.salonbook.app.utils.SessionManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class BookAppointmentActivity extends AppCompatActivity {

    private ActivityBookAppointmentBinding binding;
    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private List<String> serviceNames = new ArrayList<>();
    private List<String> serviceIds = new ArrayList<>();
    private List<String> serviceCategories = new ArrayList<>();
    private List<String> stylistNames = new ArrayList<>();
    private List<String> stylistIds = new ArrayList<>();
    private List<String> allStylistNames = new ArrayList<>();
    private List<String> allStylistIds = new ArrayList<>();
    private List<String> allStylistSpecs = new ArrayList<>();
    private String selectedDate = "";
    private String selectedTimeSlot = "";
    private Set<String> bookedSlots = new HashSet<>();
    private String customerName = "";
    private String customerPhone = "";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.attachBaseContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookAppointmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        loadServices();
        loadAllStylists();
        setupDatePicker();
        setupTimeSlots();
        fetchCustomerProfile();

        // Re-filter stylists when service selection changes
        binding.spinnerService.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                filterStylistsByService(position);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Clear slot and update chips when stylist selection changes
        binding.spinnerStylist.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedTimeSlot = "";
                for (int i = 0; i < binding.chipGroupTimeSlots.getChildCount(); i++) {
                    Chip chip = (Chip) binding.chipGroupTimeSlots.getChildAt(i);
                    chip.setChecked(false);
                }
                if (position > 0 && !selectedDate.isEmpty()) {
                    loadBookedSlots();
                } else {
                    bookedSlots.clear();
                    updateTimeSlotChips();
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        binding.btnBookNow.setOnClickListener(v -> bookAppointment());
    }

    private void loadServices() {
        db.collection(Constants.COLLECTION_SERVICES).get().addOnSuccessListener(snap -> {
            serviceNames.clear(); serviceIds.clear(); serviceCategories.clear();
            
            // Add placeholder
            serviceNames.add(getString(R.string.select_service));
            serviceIds.add("");
            serviceCategories.add("");

            for (QueryDocumentSnapshot doc : snap) {
                serviceNames.add(doc.getString("name"));
                serviceIds.add(doc.getId());
                serviceCategories.add(doc.getString("category") != null ? doc.getString("category") : "");
            }
            binding.spinnerService.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, serviceNames));

            // Pre-select if coming from service click
            String preselectedId = getIntent().getStringExtra(Constants.EXTRA_SERVICE_ID);
            if (preselectedId != null) {
                int idx = serviceIds.indexOf(preselectedId);
                if (idx > 0) binding.spinnerService.setSelection(idx);
            }
        });
    }

    private void loadAllStylists() {
        db.collection(Constants.COLLECTION_STYLISTS).whereEqualTo("available", true)
                .get().addOnSuccessListener(snap -> {
            allStylistNames.clear(); allStylistIds.clear(); allStylistSpecs.clear();
            
            // Add placeholder
            allStylistNames.add(getString(R.string.select_stylist));
            allStylistIds.add("");
            allStylistSpecs.add("");

            for (QueryDocumentSnapshot doc : snap) {
                allStylistNames.add(doc.getString("name"));
                allStylistIds.add(doc.getId());
                allStylistSpecs.add(doc.getString("specialization") != null ? doc.getString("specialization") : "General");
            }
            
            // Initial filter based on current service selection
            int servicePos = binding.spinnerService.getSelectedItemPosition();
            if (servicePos > 0) {
                filterStylistsByService(servicePos);
            } else {
                // Show only placeholder if no service selected yet
                stylistNames.clear();
                stylistIds.clear();
                stylistNames.add(allStylistNames.get(0));
                stylistIds.add(allStylistIds.get(0));
                binding.spinnerStylist.setAdapter(new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item, stylistNames));
            }
        });
    }

    private void filterStylistsByService(int servicePosition) {
        stylistNames.clear();
        stylistIds.clear();

        // Always add the placeholder first
        if (!allStylistNames.isEmpty()) {
            stylistNames.add(allStylistNames.get(0));
            stylistIds.add(allStylistIds.get(0));
        } else {
            stylistNames.add(getString(R.string.select_stylist));
            stylistIds.add("");
        }

        if (servicePosition > 0 && servicePosition < serviceCategories.size()) {
            String category = serviceCategories.get(servicePosition);
            String requiredSpec = Constants.mapCategoryToSpecialization(category);

            // Start from 1 to skip the placeholder
            for (int i = 1; i < allStylistNames.size(); i++) {
                String spec = allStylistSpecs.get(i);
                // Show stylists whose specialization matches, or who are "General"
                if ("General".equalsIgnoreCase(spec) || spec.equalsIgnoreCase(requiredSpec)) {
                    stylistNames.add(allStylistNames.get(i));
                    stylistIds.add(allStylistIds.get(i));
                }
            }
        }

        binding.spinnerStylist.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, stylistNames));
    }

    private void setupDatePicker() {
        binding.btnSelectDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, day);
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                selectedDate = sdf.format(selected.getTime());
                binding.btnSelectDate.setText(selectedDate);
                loadBookedSlots();
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            
            dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            dialog.show();
        });
    }

    private boolean isSlotInPast(String slot) {
        if (selectedDate.isEmpty()) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy h:mm a", Locale.getDefault());
            Date slotDateTime = sdf.parse(selectedDate + " " + slot);
            if (slotDateTime != null) {
                return slotDateTime.before(new Date());
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void setupTimeSlots() {
        binding.chipGroupTimeSlots.removeAllViews();
        for (String slot : Constants.TIME_SLOTS) {
            Chip chip = new Chip(this);
            chip.setText(slot);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setChipStrokeColorResource(R.color.purple_primary);
            chip.setChipStrokeWidth(2f);
            chip.setChipBackgroundColorResource(R.color.accent_white);
            chip.setTextColor(getColor(R.color.purple_primary));
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (selectedDate.isEmpty()) {
                        Toast.makeText(this, "Please select date first", Toast.LENGTH_SHORT).show();
                        chip.setChecked(false);
                        return;
                    }
                    int stylistPos = binding.spinnerStylist.getSelectedItemPosition();
                    if (stylistPos <= 0) {
                        Toast.makeText(this, "Please select stylist first", Toast.LENGTH_SHORT).show();
                        chip.setChecked(false);
                        return;
                    }
                    if (bookedSlots.contains(slot) || isSlotInPast(slot)) {
                        Toast.makeText(this, "Slot unavailable, please select another time", Toast.LENGTH_SHORT).show();
                        chip.setChecked(false);
                    } else {
                        selectedTimeSlot = slot;
                        chip.setChipBackgroundColorResource(R.color.purple_primary);
                        chip.setTextColor(getColor(R.color.accent_white));
                        Toast.makeText(this, "Slot available, proceed booking", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (selectedTimeSlot.equals(slot)) {
                        selectedTimeSlot = "";
                    }
                    if (bookedSlots.contains(slot) || isSlotInPast(slot)) {
                        chip.setChipBackgroundColorResource(R.color.disabled_chip);
                        chip.setTextColor(getColor(R.color.disabled_chip_text));
                    } else {
                        chip.setChipBackgroundColorResource(R.color.accent_white);
                        chip.setTextColor(getColor(R.color.purple_primary));
                    }
                }
            });
            binding.chipGroupTimeSlots.addView(chip);
        }
    }

    private void loadBookedSlots() {
        if (selectedDate.isEmpty()) return;
        int stylistPos = binding.spinnerStylist.getSelectedItemPosition();
        if (stylistPos <= 0 || stylistPos >= stylistIds.size()) {
            bookedSlots.clear();
            updateTimeSlotChips();
            return;
        }
        String stylistId = stylistIds.get(stylistPos);

        db.collection(Constants.COLLECTION_APPOINTMENTS)
                .whereEqualTo("date", selectedDate)
                .whereEqualTo("stylistId", stylistId)
                .get().addOnSuccessListener(snap -> {
            bookedSlots.clear();
            for (QueryDocumentSnapshot doc : snap) {
                String status = doc.getString("status");
                if (!Constants.STATUS_CANCELLED.equals(status)) {
                    bookedSlots.add(doc.getString("timeSlot"));
                }
            }
            updateTimeSlotChips();
        });
    }

    private void updateTimeSlotChips() {
        for (int i = 0; i < binding.chipGroupTimeSlots.getChildCount(); i++) {
            Chip chip = (Chip) binding.chipGroupTimeSlots.getChildAt(i);
            String slot = chip.getText().toString();
            boolean isBooked = bookedSlots.contains(slot);
            boolean isPast = isSlotInPast(slot);
            if (isBooked || isPast) {
                chip.setChecked(false);
                chip.setClickable(false);
                chip.setChipBackgroundColorResource(R.color.disabled_chip);
                chip.setTextColor(getColor(R.color.disabled_chip_text));
            } else {
                chip.setClickable(true);
                if (chip.isChecked()) {
                    chip.setChipBackgroundColorResource(R.color.purple_primary);
                    chip.setTextColor(getColor(R.color.accent_white));
                } else {
                    chip.setChipBackgroundColorResource(R.color.accent_white);
                    chip.setTextColor(getColor(R.color.purple_primary));
                }
            }
        }
    }

    private void fetchCustomerProfile() {
        String uid = sessionManager.getFirebaseUid();

        // Pre-fill from cached session data (set during login)
        String cachedName = sessionManager.getUserName();
        String cachedPhone = sessionManager.getUserPhone();
        if (!cachedName.isEmpty()) customerName = cachedName;
        if (!cachedPhone.isEmpty()) customerPhone = cachedPhone;

        if (uid == null || uid.isEmpty()) return;

        db.collection(Constants.COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (isFinishing()) return; // Activity was destroyed
                    if (doc.exists()) {
                        customerName = doc.getString("name") != null ? doc.getString("name") : customerName;
                        customerPhone = doc.getString("phone") != null ? doc.getString("phone") : customerPhone;
                        sessionManager.setUserName(customerName);
                        sessionManager.setUserPhone(customerPhone);
                    }
                });
    }

    private void bookAppointment() {
        int servicePos = binding.spinnerService.getSelectedItemPosition();
        int stylistPos = binding.spinnerStylist.getSelectedItemPosition();

        if (servicePos <= 0) { Toast.makeText(this, R.string.select_service, Toast.LENGTH_SHORT).show(); return; }
        if (stylistPos <= 0) { Toast.makeText(this, R.string.select_stylist, Toast.LENGTH_SHORT).show(); return; }
        if (selectedDate.isEmpty()) { Toast.makeText(this, R.string.select_date, Toast.LENGTH_SHORT).show(); return; }
        if (selectedTimeSlot.isEmpty()) { Toast.makeText(this, R.string.select_time_slot, Toast.LENGTH_SHORT).show(); return; }

        binding.progressBooking.setVisibility(View.VISIBLE);
        binding.btnBookNow.setEnabled(false);

        Appointment appointment = new Appointment(
                sessionManager.getFirebaseUid(), customerName, customerPhone,
                stylistIds.get(stylistPos), serviceIds.get(servicePos),
                serviceNames.get(servicePos), stylistNames.get(stylistPos),
                selectedDate, selectedTimeSlot);

        db.collection(Constants.COLLECTION_APPOINTMENTS)
                .add(appointment.toMap())
                .addOnSuccessListener(docRef -> {
                    binding.progressBooking.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.booking_success, Toast.LENGTH_SHORT).show();
                    scheduleReminder(serviceNames.get(servicePos), stylistNames.get(stylistPos));
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.progressBooking.setVisibility(View.GONE);
                    binding.btnBookNow.setEnabled(true);
                    Toast.makeText(this, R.string.booking_failed, Toast.LENGTH_SHORT).show();
                });
    }

    private void scheduleReminder(String serviceName, String stylistName) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault());
            Date appointmentTime = sdf.parse(selectedDate + " " + selectedTimeSlot);
            if (appointmentTime == null) return;

            long triggerTime = appointmentTime.getTime() - (60 * 60 * 1000); // 1 hour before
            if (triggerTime < System.currentTimeMillis()) return;

            Intent intent = new Intent(this, AppointmentReceiver.class);
            intent.setAction(Constants.ACTION_APPOINTMENT_REMINDER);
            intent.putExtra("serviceName", serviceName);
            intent.putExtra("stylistName", stylistName);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(this,
                    (int) System.currentTimeMillis(), intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
