package com.example.mobilprogfallproj.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobilprogfallproj.R;
import com.example.mobilprogfallproj.data.firebase.FirebaseService;
import com.example.mobilprogfallproj.data.firebase.models.UserModel;
import com.example.mobilprogfallproj.ui.main.MainActivity;
import com.example.mobilprogfallproj.util.PasswordUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.QuerySnapshot;


public class RegisterActivity extends AppCompatActivity {
    private EditText usernameEdit;
    private EditText displayNameEdit;
    private EditText passwordEdit;
    private EditText bioEdit;
    private CheckBox rememberMeCheckbox;
    private Button registerButton;
    private Button backToLoginButton;
    private FirebaseService firebaseService;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        usernameEdit = findViewById(R.id.username_edit);
        displayNameEdit = findViewById(R.id.display_name_edit);
        passwordEdit = findViewById(R.id.password_edit);
        bioEdit = findViewById(R.id.bio_edit);
        rememberMeCheckbox = findViewById(R.id.remember_me_checkbox);
        registerButton = findViewById(R.id.btn_register);
        backToLoginButton = findViewById(R.id.btn_back_to_login);

        firebaseService = FirebaseService.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        registerButton.setOnClickListener(v -> attemptRegister());
        backToLoginButton.setOnClickListener(v -> {
            finish();
        });
    }

    private void attemptRegister() {
        String username = usernameEdit.getText().toString().trim();
        String displayName = displayNameEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString();
        String bio = bioEdit.getText().toString().trim();

        // Doğrulama
        if (username.isEmpty()) {
            Toast.makeText(this, "Please enter username", Toast.LENGTH_SHORT).show();
            return;
        }

        if (displayName.isEmpty()) {
            Toast.makeText(this, "Please enter display name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        registerButton.setEnabled(false);
        registerButton.setText("Creating account...");

        // Kullanıcı adının zaten var olup olmadığını kontrol et
        firebaseService.getUserByUsername(username, new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(Task<QuerySnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                    registerButton.setEnabled(true);
                    registerButton.setText("Create Account");
                    Toast.makeText(RegisterActivity.this, "Username already exists", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Kullanıcı adı müsait, hesap oluştur
                String email = username + "@cinematch.app";
                
                // Firebase Auth'da kullanıcı oluştur
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(Task<AuthResult> authTask) {
                                if (authTask.isSuccessful()) {
                                    // Auth kullanıcısı oluşturuldu, Firebase Auth UID'yi kullanıcı ID'si olarak kullan
                                    String userId = authTask.getResult().getUser().getUid();
                                    
                                    UserModel user = new UserModel();
                                    user.id = userId;
                                    user.username = username;
                                    user.displayName = displayName;
                                    user.bio = bio.isEmpty() ? "" : bio;
                                    user.passwordHash = PasswordUtil.hashPassword(password);
                                    user.profileImageUrl = null;

                                    firebaseService.createUser(user, 
                                        new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                // Beni hatırla tercihi ile giriş durumunu kaydet
                                                boolean rememberMe = rememberMeCheckbox.isChecked();
                                                LoginActivity.saveLoginState(RegisterActivity.this, userId, rememberMe);
                                                
                                                Toast.makeText(RegisterActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                                                
                                                // Ana aktiviteye git
                                                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                                finish();
                                            }
                                        },
                                        new OnFailureListener() {
                                            @Override
                                            public void onFailure(Exception e) {
                                                registerButton.setEnabled(true);
                                                registerButton.setText("Create Account");
                                                Toast.makeText(RegisterActivity.this, "Failed to create account: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                // Firestore oluşturma başarısız olursa auth kullanıcısını sil
                                                if (firebaseAuth.getCurrentUser() != null) {
                                                    firebaseAuth.getCurrentUser().delete();
                                                }
                                            }
                                        });
                                } else {
                                    registerButton.setEnabled(true);
                                    registerButton.setText("Create Account");
                                    String errorMsg = authTask.getException() != null ? authTask.getException().getMessage() : "Unknown error";
                                    Toast.makeText(RegisterActivity.this, "Failed to create account: " + errorMsg, Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });
    }
}

