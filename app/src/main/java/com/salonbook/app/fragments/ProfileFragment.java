package com.salonbook.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.salonbook.app.R;
import com.salonbook.app.databinding.FragmentProfileBinding;
import com.salonbook.app.utils.Constants;
import com.salonbook.app.utils.SessionManager;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(requireContext());
        loadProfile();
        binding.btnSaveProfile.setOnClickListener(v -> saveProfile());
        binding.btnLogout.setOnClickListener(v -> {
            com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.SalonBook_Dialog);
            builder.setTitle(R.string.logout).setMessage(R.string.confirm_logout)
                    .setPositiveButton(R.string.yes, (d, w) -> {
                        FirebaseAuth.getInstance().signOut();
                        sessionManager.logout();
                        requireActivity().finish();
                        startActivity(new android.content.Intent(requireContext(), com.salonbook.app.activities.LoginActivity.class));
                    }).setNegativeButton(R.string.no, null).show();
        });
    }

    private void loadProfile() {
        String uid = sessionManager.getFirebaseUid();
        if (uid == null || uid.isEmpty()) {
            // No valid UID — cannot fetch profile from Firestore
            Toast.makeText(requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressProfile.setVisibility(View.VISIBLE);
        binding.tvProfileRole.setText(sessionManager.getRole());

        // Pre-fill from cached session data while Firestore loads
        String cachedName = sessionManager.getUserName();
        String cachedPhone = sessionManager.getUserPhone();
        String cachedEmail = sessionManager.getUsername();
        if (!cachedName.isEmpty()) {
            binding.tvProfileName.setText(cachedName);
            binding.etProfileName.setText(cachedName);
        }
        if (!cachedPhone.isEmpty()) {
            binding.etProfilePhone.setText(cachedPhone);
        }
        if (!cachedEmail.isEmpty()) {
            binding.etProfileEmail.setText(cachedEmail);
        }

        db.collection(Constants.COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (binding == null) return; // Fragment was destroyed
                    binding.progressProfile.setVisibility(View.GONE);
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String phone = doc.getString("phone");
                        String email = doc.getString("email");
                        binding.tvProfileName.setText(name != null ? name : "");
                        binding.etProfileName.setText(name != null ? name : "");
                        binding.etProfilePhone.setText(phone != null ? phone : "");
                        binding.etProfileEmail.setText(email != null ? email : "");

                        // Update cached session data
                        if (name != null) sessionManager.setUserName(name);
                        if (phone != null) sessionManager.setUserPhone(phone);
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return; // Fragment was destroyed
                    binding.progressProfile.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
                });
    }

    private void saveProfile() {
        String name = binding.etProfileName.getText() != null ? binding.etProfileName.getText().toString().trim() : "";
        String phone = binding.etProfilePhone.getText() != null ? binding.etProfilePhone.getText().toString().trim() : "";
        binding.tilProfileName.setError(null);
        binding.tilProfilePhone.setError(null);

        if (name.isEmpty()) { binding.tilProfileName.setError(getString(R.string.field_required)); return; }
        if (phone.isEmpty()) { binding.tilProfilePhone.setError(getString(R.string.field_required)); return; }
        if (phone.length() != 10) { binding.tilProfilePhone.setError("Mobile number must be 10 digits"); return; }

        String uid = sessionManager.getFirebaseUid();
        if (uid == null || uid.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressProfile.setVisibility(View.VISIBLE);
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        db.collection(Constants.COLLECTION_USERS).document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (binding == null) return;
                    binding.progressProfile.setVisibility(View.GONE);
                    binding.tvProfileName.setText(name);
                    sessionManager.setUserName(name);
                    sessionManager.setUserPhone(phone);
                    Toast.makeText(requireContext(), R.string.profile_updated, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    binding.progressProfile.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
