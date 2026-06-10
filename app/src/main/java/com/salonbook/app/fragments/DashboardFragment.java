package com.salonbook.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.salonbook.app.R;
import com.salonbook.app.activities.AddServiceActivity;
import com.salonbook.app.activities.AddStylistActivity;
import com.salonbook.app.activities.AppointmentDetailActivity;
import com.salonbook.app.adapters.AppointmentAdapter;
import com.salonbook.app.databinding.FragmentDashboardBinding;
import com.salonbook.app.models.Appointment;
import com.salonbook.app.models.WalkIn;
import com.salonbook.app.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment implements AppointmentAdapter.OnAppointmentActionListener {

    private FragmentDashboardBinding binding;
    private FirebaseFirestore db;
    private AppointmentAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        adapter = new AppointmentAdapter(this, true);
        binding.recyclerRecentAppointments.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerRecentAppointments.setAdapter(adapter);

        binding.btnQuickAddService.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddServiceActivity.class)));
        binding.btnQuickAddStylist.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddStylistActivity.class)));
        binding.btnQuickAddWalkin.setOnClickListener(v -> showWalkInDialog());
        loadDashboard();
    }

    private void loadDashboard() {
        if (binding == null) return;
        binding.progressDashboard.setVisibility(View.VISIBLE);

        db.collection(Constants.COLLECTION_APPOINTMENTS)
                .get()
                .addOnSuccessListener(snap -> {
                    if (binding == null) return;
                    int total = 0, pending = 0, confirmed = 0, completed = 0;
                    List<Appointment> recent = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Appointment a = doc.toObject(Appointment.class);
                        a.setId(doc.getId());
                        total++;
                        switch (a.getStatus()) {
                            case Constants.STATUS_PENDING: pending++; break;
                            case Constants.STATUS_CONFIRMED: confirmed++; break;
                            case Constants.STATUS_COMPLETED: completed++; break;
                        }
                        recent.add(a);
                    }
                    binding.tvTotalToday.setText(String.valueOf(total));
                    binding.tvPendingCount.setText(String.valueOf(pending));
                    binding.tvConfirmedCount.setText(String.valueOf(confirmed));
                    binding.tvCompletedCount.setText(String.valueOf(completed));
                    adapter.setAppointments(recent);
                    binding.progressDashboard.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    binding.progressDashboard.setVisibility(View.GONE);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
                    }
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
                        loadDashboard();
                    });
        });
    }

    @Override public void onViewDetails(Appointment a) {
        Intent i = new Intent(requireContext(), AppointmentDetailActivity.class);
        i.putExtra(Constants.EXTRA_APPOINTMENT_ID, a.getId());
        startActivity(i);
    }
    @Override public void onConfirm(Appointment a) { updateStatus(a, Constants.STATUS_CONFIRMED); }
    @Override public void onCancel(Appointment a) { updateStatus(a, Constants.STATUS_CANCELLED); }
    @Override public void onComplete(Appointment a) { updateStatus(a, Constants.STATUS_COMPLETED); }
    @Override public void onCall(Appointment a) {}

    private void updateStatus(Appointment a, String status) {
        db.collection(Constants.COLLECTION_APPOINTMENTS).document(a.getId())
                .update("status", status)
                .addOnSuccessListener(v -> loadDashboard());
    }

    @Override public void onResume() { super.onResume(); loadDashboard(); }
    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
