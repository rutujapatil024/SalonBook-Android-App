package com.salonbook.app.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.salonbook.app.R;
import com.salonbook.app.database.SalonDBHelper;
import com.salonbook.app.databinding.ActivityLoginBinding;
import com.salonbook.app.utils.Constants;
import com.salonbook.app.utils.LocaleHelper;
import com.salonbook.app.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestoreDb;
    private SalonDBHelper dbHelper;
    private SessionManager sessionManager;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.attachBaseContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        firestoreDb = FirebaseFirestore.getInstance();
        dbHelper = new SalonDBHelper(this);
        sessionManager = new SessionManager(this);

        binding.btnLogin.setOnClickListener(v -> loginUser());
        binding.btnGoRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void loginUser() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError(getString(R.string.field_required));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.invalid_email));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError(getString(R.string.field_required));
            return;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError(getString(R.string.password_min_length));
            return;
        }

        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.progressLogin.setVisibility(View.VISIBLE);
        binding.btnLogin.setEnabled(false);

        // Firebase Auth sign-in
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                        String uid = firebaseAuth.getCurrentUser().getUid();

                        // Fetch user profile from Firestore to get the correct role
                        firestoreDb.collection(Constants.COLLECTION_USERS).document(uid).get()
                                .addOnSuccessListener(doc -> {
                                    String role = Constants.ROLE_CUSTOMER; // default fallback
                                    String name = "";
                                    String phone = "";

                                    if (doc.exists()) {
                                        String firestoreRole = doc.getString("role");
                                        if (firestoreRole != null && !firestoreRole.isEmpty()) {
                                            role = firestoreRole;
                                        }
                                        name = doc.getString("name") != null ? doc.getString("name") : "";
                                        phone = doc.getString("phone") != null ? doc.getString("phone") : "";
                                    }

                                    // Update or insert local SQLite with correct role
                                    if (!dbHelper.userExists(email)) {
                                        dbHelper.insertUser(email, password, role, uid);
                                    } else {
                                        // Update the role in SQLite to match Firestore
                                        android.content.ContentValues values = new android.content.ContentValues();
                                        values.put("role", role);
                                        dbHelper.updateUser(email, values);
                                    }

                                    // Create session with correct role and cache user profile
                                    sessionManager.createLoginSession(email, role, uid);
                                    sessionManager.setUserName(name);
                                    sessionManager.setUserPhone(phone);

                                    binding.progressLogin.setVisibility(View.GONE);
                                    Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    // Firestore fetch failed — fall back to local SQLite data
                                    String role;
                                    if (!dbHelper.userExists(email)) {
                                        role = Constants.ROLE_CUSTOMER;
                                        dbHelper.insertUser(email, password, role, uid);
                                    } else {
                                        role = dbHelper.getUserRole(email);
                                    }

                                    sessionManager.createLoginSession(email, role, uid);

                                    binding.progressLogin.setVisibility(View.GONE);
                                    Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                });
                    } else {
                        binding.progressLogin.setVisibility(View.GONE);
                        binding.btnLogin.setEnabled(true);
                        Toast.makeText(this, R.string.login_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
