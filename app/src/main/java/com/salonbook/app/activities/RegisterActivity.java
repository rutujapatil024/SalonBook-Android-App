package com.salonbook.app.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.salonbook.app.R;
import com.salonbook.app.database.SalonDBHelper;
import com.salonbook.app.databinding.ActivityRegisterBinding;
import com.salonbook.app.models.User;
import com.salonbook.app.utils.Constants;
import com.salonbook.app.utils.LocaleHelper;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;
    private SalonDBHelper dbHelper;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.attachBaseContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        dbHelper = new SalonDBHelper(this);

        // Role spinner
        String[] roles = {Constants.ROLE_CUSTOMER, Constants.ROLE_OWNER};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, roles);
        binding.spinnerRole.setAdapter(roleAdapter);

        binding.btnRegister.setOnClickListener(v -> registerUser());
        binding.btnGoLogin.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();
        String role = binding.spinnerRole.getSelectedItem().toString();

        // Validation
        if (TextUtils.isEmpty(name)) { binding.tilName.setError(getString(R.string.field_required)); return; }
        if (TextUtils.isEmpty(email)) { binding.tilEmail.setError(getString(R.string.field_required)); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.tilEmail.setError(getString(R.string.invalid_email)); return; }
        if (TextUtils.isEmpty(phone)) { binding.tilPhone.setError(getString(R.string.field_required)); return; }
        if (phone.length() != 10) { binding.tilPhone.setError("Mobile number must be 10 digits"); return; }
        if (TextUtils.isEmpty(password)) { binding.tilPassword.setError(getString(R.string.field_required)); return; }
        if (password.length() < 6) { binding.tilPassword.setError(getString(R.string.password_min_length)); return; }
        if (!password.equals(confirmPassword)) { binding.tilConfirmPassword.setError(getString(R.string.passwords_not_match)); return; }

        // Clear errors
        binding.tilName.setError(null); binding.tilEmail.setError(null);
        binding.tilPhone.setError(null); binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        binding.progressRegister.setVisibility(View.VISIBLE);
        binding.btnRegister.setEnabled(false);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                        String uid = firebaseAuth.getCurrentUser().getUid();

                        // Save to SQLite
                        dbHelper.insertUser(email, password, role, uid);

                        // Save to Firestore
                        User user = new User(name, phone, email, role);
                        db.collection(Constants.COLLECTION_USERS).document(uid)
                                .set(user.toMap())
                                .addOnSuccessListener(aVoid -> {
                                    binding.progressRegister.setVisibility(View.GONE);
                                    Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show();
                                    firebaseAuth.signOut();
                                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    binding.progressRegister.setVisibility(View.GONE);
                                    binding.btnRegister.setEnabled(true);
                                    Toast.makeText(this, R.string.register_failed, Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        binding.progressRegister.setVisibility(View.GONE);
                        binding.btnRegister.setEnabled(true);
                        String msg = task.getException() != null ? task.getException().getMessage() : getString(R.string.register_failed);
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
