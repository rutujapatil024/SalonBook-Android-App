package com.salonbook.app.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.salonbook.app.R;
import com.salonbook.app.activities.AppointmentDetailActivity;
import com.salonbook.app.adapters.AppointmentAdapter;
import com.salonbook.app.databinding.FragmentAppointmentsBinding;
import com.salonbook.app.models.Appointment;
import com.salonbook.app.models.WalkIn;
import com.salonbook.app.utils.Constants;
import com.salonbook.app.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AppointmentsFragment extends Fragment implements AppointmentAdapter.OnAppointmentActionListener {

    private FragmentAppointmentsBinding binding;
    private AppointmentAdapter adapter;
    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private List<Appointment> allAppointments = new ArrayList<>();
    private boolean showUpcoming = true;
    private String currentCategoryFilter = null;
    private String currentSearchQuery = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAppointmentsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(requireContext());
        setupTabs();
        setupRecyclerView();
        setupFab();
        loadAppointments();
    }

    private void setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.upcoming));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.past));
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                showUpcoming = tab.getPosition() == 0;
                // Hide FAB on Past tab
                if (sessionManager.isOwner()) {
                    binding.fabAddWalkin.setVisibility(showUpcoming ? View.VISIBLE : View.GONE);
                }
                filterAppointments();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new AppointmentAdapter(this, sessionManager.isOwner());
        binding.recyclerAppointments.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerAppointments.setAdapter(adapter);
    }

    private void setupFab() {
        if (sessionManager.isOwner()) {
            binding.fabAddWalkin.setVisibility(View.VISIBLE);
            binding.fabAddWalkin.setOnClickListener(v -> showWalkInDialog());
        }
    }

    private void loadAppointments() {
        if (binding == null) return;
        binding.progressAppointments.setVisibility(View.VISIBLE);
        binding.layoutEmpty.setVisibility(View.GONE);

        com.google.firebase.firestore.Query query;
        if (sessionManager.isOwner()) {
            query = db.collection(Constants.COLLECTION_APPOINTMENTS);
        } else {
            String uid = sessionManager.getFirebaseUid();
            if (uid == null || uid.isEmpty()) {
                binding.progressAppointments.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.VISIBLE);
                return;
            }
            query = db.collection(Constants.COLLECTION_APPOINTMENTS)
                    .whereEqualTo("customerId", uid);
        }

        query.get().addOnSuccessListener(snap -> {
            if (binding == null) return;
            allAppointments.clear();
            for (QueryDocumentSnapshot doc : snap) {
                Appointment a = doc.toObject(Appointment.class);
                a.setId(doc.getId());
                allAppointments.add(a);
            }
            binding.progressAppointments.setVisibility(View.GONE);
            filterAppointments();
        }).addOnFailureListener(e -> {
            if (binding == null) return;
            binding.progressAppointments.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.VISIBLE);
        });
    }

    private void filterAppointments() {
        if (binding == null) return;
        List<Appointment> filtered = new ArrayList<>();
        for (Appointment a : allAppointments) {
            // Filter by tab (upcoming vs past)
            boolean matchesTab;
            if (showUpcoming) {
                matchesTab = Constants.STATUS_PENDING.equals(a.getStatus()) || Constants.STATUS_CONFIRMED.equals(a.getStatus());
            } else {
                matchesTab = Constants.STATUS_COMPLETED.equals(a.getStatus()) || Constants.STATUS_CANCELLED.equals(a.getStatus());
            }
            if (!matchesTab) continue;

            // Filter by category if set
            if (currentCategoryFilter != null && !currentCategoryFilter.isEmpty()) {
                String serviceName = a.getServiceName() != null ? a.getServiceName() : "";
                if (!serviceName.toLowerCase().contains(currentCategoryFilter.toLowerCase())) {
                    continue;
                }
            }

            // Filter by search query if set
            if (currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
                String lowerQuery = currentSearchQuery.toLowerCase();
                String name = a.getCustomerName() != null ? a.getCustomerName().toLowerCase() : "";
                String service = a.getServiceName() != null ? a.getServiceName().toLowerCase() : "";
                String stylist = a.getStylistName() != null ? a.getStylistName().toLowerCase() : "";
                if (!name.contains(lowerQuery) && !service.contains(lowerQuery) && !stylist.contains(lowerQuery)) {
                    continue;
                }
            }

            filtered.add(a);
        }
        adapter.setAppointments(filtered);
        binding.layoutEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    public void filterByCategory(String category) {
        currentCategoryFilter = category;
        filterAppointments();
    }

    public void filterByQuery(String query) {
        currentSearchQuery = query;
        filterAppointments();
    }

    @Override public void onViewDetails(Appointment a) {
        Intent i = new Intent(requireContext(), AppointmentDetailActivity.class);
        i.putExtra(Constants.EXTRA_APPOINTMENT_ID, a.getId());
        startActivity(i);
    }
    @Override public void onConfirm(Appointment a) { updateStatus(a, Constants.STATUS_CONFIRMED); }
    @Override public void onCancel(Appointment a) {
        new MaterialAlertDialogBuilder(requireContext(), R.style.SalonBook_Dialog)
                .setTitle(R.string.cancel_appointment).setMessage(R.string.confirm_cancel_appointment)
                .setPositiveButton(R.string.yes, (d, w) -> updateStatus(a, Constants.STATUS_CANCELLED))
                .setNegativeButton(R.string.no, null).show();
    }
    @Override public void onComplete(Appointment a) { updateStatus(a, Constants.STATUS_COMPLETED); }
    @Override public void onCall(Appointment a) {
        String phone = a.getCustomerPhone();
        if (phone != null && !phone.isEmpty()) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phone)));
            } else {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CALL_PHONE}, 100);
            }
        }
    }

    private void updateStatus(Appointment a, String status) {
        db.collection(Constants.COLLECTION_APPOINTMENTS).document(a.getId())
                .update("status", status).addOnSuccessListener(v -> {
            String msg;
            switch (status) {
                case Constants.STATUS_CONFIRMED: msg = getString(R.string.appointment_confirmed); break;
                case Constants.STATUS_CANCELLED: msg = getString(R.string.appointment_cancelled); break;
                case Constants.STATUS_COMPLETED: msg = getString(R.string.appointment_completed); break;
                default: msg = getString(R.string.appointment_status_changed);
            }
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            Intent broadcast = new Intent(Constants.ACTION_APPOINTMENT_STATUS_CHANGED);
            broadcast.putExtra("status", status);
            requireContext().sendBroadcast(broadcast);
            loadAppointments();
        });
    }

    private void filterWalkinStylists(int servicePosition, List<String> svcCategories,
                                      List<String> allStNames, List<String> allStIds, List<String> allStSpecs,
                                      List<String> currentStNames, List<String> currentStIds,
                                      ArrayAdapter<String> stylistAdapter) {
        currentStNames.clear();
        currentStIds.clear();

        if (servicePosition >= 0 && servicePosition < svcCategories.size()) {
            String category = svcCategories.get(servicePosition);
            String requiredSpec = Constants.mapCategoryToSpecialization(category);

            for (int i = 0; i < allStNames.size(); i++) {
                String spec = allStSpecs.get(i);
                if ("General".equalsIgnoreCase(spec) || spec.equalsIgnoreCase(requiredSpec)) {
                    currentStNames.add(allStNames.get(i));
                    currentStIds.add(allStIds.get(i));
                }
            }
        } else {
            currentStNames.addAll(allStNames);
            currentStIds.addAll(allStIds);
        }
        stylistAdapter.notifyDataSetChanged();
    }

    private void showWalkInDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_walkin, null);
        TextInputEditText etName = dialogView.findViewById(R.id.etWalkinName);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etWalkinPhone);
        Spinner spinnerService = dialogView.findViewById(R.id.spinnerWalkinService);
        Spinner spinnerStylist = dialogView.findViewById(R.id.spinnerWalkinStylist);
        Spinner spinnerTime = dialogView.findViewById(R.id.spinnerWalkinTime);

        spinnerTime.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, Constants.TIME_SLOTS));

        List<String> svcNames = new ArrayList<>(), svcIds = new ArrayList<>(), svcCategories = new ArrayList<>();
        List<String> allStNames = new ArrayList<>(), allStIds = new ArrayList<>(), allStSpecs = new ArrayList<>();
        List<String> currentStNames = new ArrayList<>(), currentStIds = new ArrayList<>();

        ArrayAdapter<String> stylistAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, currentStNames);
        spinnerStylist.setAdapter(stylistAdapter);

        db.collection(Constants.COLLECTION_SERVICES).get().addOnSuccessListener(s -> {
            svcNames.clear(); svcIds.clear(); svcCategories.clear();
            for (QueryDocumentSnapshot d : s) {
                svcNames.add(d.getString("name"));
                svcIds.add(d.getId());
                svcCategories.add(d.getString("category") != null ? d.getString("category") : "");
            }
            spinnerService.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, svcNames));
            int selectedSvc = spinnerService.getSelectedItemPosition();
            if (selectedSvc >= 0 && selectedSvc < svcCategories.size()) {
                filterWalkinStylists(selectedSvc, svcCategories, allStNames, allStIds, allStSpecs, currentStNames, currentStIds, stylistAdapter);
            }
        });

        db.collection(Constants.COLLECTION_STYLISTS).whereEqualTo("available", true).get().addOnSuccessListener(s -> {
            allStNames.clear(); allStIds.clear(); allStSpecs.clear();
            for (QueryDocumentSnapshot d : s) {
                allStNames.add(d.getString("name"));
                allStIds.add(d.getId());
                allStSpecs.add(d.getString("specialization") != null ? d.getString("specialization") : "General");
            }
            int selectedSvc = spinnerService.getSelectedItemPosition();
            if (selectedSvc >= 0 && selectedSvc < svcCategories.size()) {
                filterWalkinStylists(selectedSvc, svcCategories, allStNames, allStIds, allStSpecs, currentStNames, currentStIds, stylistAdapter);
            }
        });

        spinnerService.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                filterWalkinStylists(position, svcCategories, allStNames, allStIds, allStSpecs, currentStNames, currentStIds, stylistAdapter);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.SalonBook_Dialog)
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
            com.google.android.material.textfield.TextInputLayout tilName = dialogView.findViewById(R.id.tilWalkinName);
            com.google.android.material.textfield.TextInputLayout tilPhone = dialogView.findViewById(R.id.tilWalkinPhone);

            tilName.setError(null);
            tilPhone.setError(null);

            if (name.isEmpty()) {
                tilName.setError(getString(R.string.field_required));
                return;
            }
            if (phone.isEmpty()) {
                tilPhone.setError(getString(R.string.field_required));
                return;
            }
            if (phone.length() != 10) {
                tilPhone.setError("Mobile number must be 10 digits");
                return;
            }

            int sp = spinnerService.getSelectedItemPosition();
            int stp = spinnerStylist.getSelectedItemPosition();
            String today = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
            WalkIn w = new WalkIn(name, phone,
                    sp >= 0 && sp < svcIds.size() ? svcIds.get(sp) : "", sp >= 0 && sp < svcNames.size() ? svcNames.get(sp) : "",
                    stp >= 0 && stp < currentStIds.size() ? currentStIds.get(stp) : "", stp >= 0 && stp < currentStNames.size() ? currentStNames.get(stp) : "",
                    today, spinnerTime.getSelectedItem() != null ? spinnerTime.getSelectedItem().toString() : "");

            db.collection(Constants.COLLECTION_WALKINS).add(w.toMap())
                    .addOnSuccessListener(r -> {
                        Toast.makeText(requireContext(), R.string.walkin_saved, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
        });
    }

    @Override public void onResume() { super.onResume(); loadAppointments(); }
    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
