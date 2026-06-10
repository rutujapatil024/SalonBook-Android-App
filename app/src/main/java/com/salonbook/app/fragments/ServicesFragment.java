package com.salonbook.app.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.salonbook.app.R;
import com.salonbook.app.activities.AddServiceActivity;
import com.salonbook.app.activities.BookAppointmentActivity;
import com.salonbook.app.adapters.ServiceAdapter;
import com.salonbook.app.databinding.FragmentServicesBinding;
import com.salonbook.app.models.Service;
import com.salonbook.app.utils.Constants;
import com.salonbook.app.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class ServicesFragment extends Fragment implements ServiceAdapter.OnServiceClickListener {

    private FragmentServicesBinding binding;
    private ServiceAdapter adapter;
    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private Service selectedService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentServicesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(requireContext());

        setupRecyclerView();
        setupFab();
        loadServices();
    }

    private void setupRecyclerView() {
        adapter = new ServiceAdapter(this, sessionManager.isOwner());
        binding.recyclerServices.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerServices.setAdapter(adapter);
    }

    private void setupFab() {
        if (sessionManager.isOwner()) {
            binding.fabAddService.setVisibility(View.VISIBLE);
            binding.fabAddService.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), AddServiceActivity.class));
            });
        }
    }

    public void loadServices() {
        if (binding == null) return;
        binding.progressServices.setVisibility(View.VISIBLE);
        binding.layoutEmpty.setVisibility(View.GONE);

        db.collection(Constants.COLLECTION_SERVICES)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (binding == null) return;
                    List<Service> services = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Service service = doc.toObject(Service.class);
                        service.setId(doc.getId());
                        services.add(service);
                    }
                    adapter.setServices(services);
                    binding.progressServices.setVisibility(View.GONE);
                    binding.layoutEmpty.setVisibility(services.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    binding.progressServices.setVisibility(View.GONE);
                    binding.layoutEmpty.setVisibility(View.VISIBLE);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onServiceClick(Service service) {
        if (sessionManager.isCustomer()) {
            Intent intent = new Intent(requireContext(), BookAppointmentActivity.class);
            intent.putExtra(Constants.EXTRA_SERVICE_ID, service.getId());
            intent.putExtra(Constants.EXTRA_SERVICE_NAME, service.getName());
            intent.putExtra(Constants.EXTRA_SERVICE_CATEGORY, service.getCategory());
            startActivity(intent);
        }
    }

    @Override
    public void onServiceEditClick(Service service) {
        if (sessionManager.isOwner()) {
            editService(service);
        }
    }

    @Override
    public void onServiceDeleteClick(Service service) {
        if (sessionManager.isOwner()) {
            confirmDeleteService(service);
        }
    }

    @Override
    public void onServiceLongClick(Service service, View view) {
        if (sessionManager.isOwner()) {
            selectedService = service;
            showContextMenu(view, service);
        }
    }

    private void showContextMenu(View view, Service service) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(requireContext(), view);
        popup.inflate(R.menu.menu_context);

        if (!sessionManager.isOwner()) {
            popup.getMenu().findItem(R.id.context_edit).setVisible(false);
            popup.getMenu().findItem(R.id.context_delete).setVisible(false);
        }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.context_edit) {
                editService(service);
                return true;
            } else if (id == R.id.context_delete) {
                confirmDeleteService(service);
                return true;
            } else if (id == R.id.context_share) {
                shareService(service);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void editService(Service service) {
        Intent intent = new Intent(requireContext(), AddServiceActivity.class);
        intent.putExtra(Constants.EXTRA_EDIT_MODE, true);
        intent.putExtra(Constants.EXTRA_SERVICE_ID, service.getId());
        intent.putExtra(Constants.EXTRA_SERVICE_NAME, service.getName());
        intent.putExtra(Constants.EXTRA_SERVICE_CATEGORY, service.getCategory());
        intent.putExtra(Constants.EXTRA_SERVICE_DURATION, service.getDuration());
        intent.putExtra(Constants.EXTRA_SERVICE_PRICE, service.getPrice());
        startActivity(intent);
    }

    private void confirmDeleteService(Service service) {
        new MaterialAlertDialogBuilder(requireContext(), R.style.SalonBook_Dialog)
                .setTitle(R.string.delete)
                .setMessage(R.string.confirm_delete_service)
                .setPositiveButton(R.string.yes, (dialog, which) -> deleteService(service))
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void deleteService(Service service) {
        db.collection(Constants.COLLECTION_SERVICES)
                .document(service.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), R.string.service_deleted, Toast.LENGTH_SHORT).show();
                    loadServices();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show());
    }

    private void shareService(Service service) {
        String shareText = getString(R.string.share_service_text,
                service.getName(),
                String.format("%.0f", service.getPrice()),
                String.valueOf(service.getDuration()));
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
    }

    public void filterServices(String query) {
        if (adapter != null) adapter.filter(query);
    }

    public void filterByCategory(String category) {
        if (adapter != null) adapter.filterByCategory(category);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadServices();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
