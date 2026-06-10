package com.salonbook.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
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
import com.salonbook.app.activities.AddStylistActivity;
import com.salonbook.app.adapters.StylistAdapter;
import com.salonbook.app.databinding.FragmentStylistsBinding;
import com.salonbook.app.models.Stylist;
import com.salonbook.app.utils.Constants;
import com.salonbook.app.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class StylistsFragment extends Fragment implements StylistAdapter.OnStylistClickListener {

    private FragmentStylistsBinding binding;
    private StylistAdapter adapter;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStylistsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(requireContext());

        setupRecyclerView();
        setupFab();
        loadStylists();
    }

    private void setupRecyclerView() {
        adapter = new StylistAdapter(this, sessionManager.isOwner());
        binding.recyclerStylists.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerStylists.setAdapter(adapter);
    }

    private void setupFab() {
        if (sessionManager.isOwner()) {
            binding.fabAddStylist.setVisibility(View.VISIBLE);
            binding.fabAddStylist.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), AddStylistActivity.class));
            });
        }
    }

    private void loadStylists() {
        binding.progressStylists.setVisibility(View.VISIBLE);
        binding.layoutEmpty.setVisibility(View.GONE);

        db.collection(Constants.COLLECTION_STYLISTS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Stylist> stylists = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Stylist stylist = doc.toObject(Stylist.class);
                        stylist.setId(doc.getId());
                        stylists.add(stylist);
                    }
                    adapter.setStylists(stylists);
                    binding.progressStylists.setVisibility(View.GONE);
                    binding.layoutEmpty.setVisibility(stylists.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    binding.progressStylists.setVisibility(View.GONE);
                    binding.layoutEmpty.setVisibility(View.VISIBLE);
                    Toast.makeText(requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onStylistClick(Stylist stylist) {
        // No action for click on stylists (they are selected during booking)
    }

    @Override
    public void onStylistEditClick(Stylist stylist) {
        if (sessionManager.isOwner()) {
            editStylist(stylist);
        }
    }

    @Override
    public void onStylistDeleteClick(Stylist stylist) {
        if (sessionManager.isOwner()) {
            confirmDeleteStylist(stylist);
        }
    }

    @Override
    public void onStylistLongClick(Stylist stylist, View view) {
        if (sessionManager.isOwner()) {
            showContextMenu(view, stylist);
        }
    }

    private void showContextMenu(View view, Stylist stylist) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(requireContext(), view);
        popup.inflate(R.menu.menu_context);
        popup.getMenu().findItem(R.id.context_share).setVisible(false);

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.context_edit) {
                editStylist(stylist);
                return true;
            } else if (id == R.id.context_delete) {
                confirmDeleteStylist(stylist);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void editStylist(Stylist stylist) {
        Intent intent = new Intent(requireContext(), AddStylistActivity.class);
        intent.putExtra(Constants.EXTRA_EDIT_MODE, true);
        intent.putExtra(Constants.EXTRA_STYLIST_ID, stylist.getId());
        intent.putExtra(Constants.EXTRA_STYLIST_NAME, stylist.getName());
        intent.putExtra(Constants.EXTRA_STYLIST_SPEC, stylist.getSpecialization());
        intent.putExtra(Constants.EXTRA_STYLIST_EXP, stylist.getExperience());
        intent.putExtra(Constants.EXTRA_STYLIST_AVAILABLE, stylist.isAvailable());
        startActivity(intent);
    }

    private void confirmDeleteStylist(Stylist stylist) {
        new MaterialAlertDialogBuilder(requireContext(), R.style.SalonBook_Dialog)
                .setTitle(R.string.delete)
                .setMessage(R.string.confirm_delete_stylist)
                .setPositiveButton(R.string.yes, (dialog, which) -> deleteStylist(stylist))
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void deleteStylist(Stylist stylist) {
        db.collection(Constants.COLLECTION_STYLISTS)
                .document(stylist.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), R.string.stylist_deleted, Toast.LENGTH_SHORT).show();
                    loadStylists();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show());
    }

    public void filterStylists(String query) {
        if (adapter != null) adapter.filter(query);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStylists();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
