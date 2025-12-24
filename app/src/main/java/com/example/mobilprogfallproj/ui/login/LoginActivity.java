package com.example.mobilprogfallproj.ui.login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuth.AuthStateListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QuerySnapshot;

public class LoginActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "CineMatchPrefs";
    private static final String KEY_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_REMEMBER_ME = "remember_me";
    
    private EditText usernameEdit;
    private EditText passwordEdit;
    private CheckBox rememberMeCheckbox;
    private Button loginButton;
    private FirebaseService firebaseService;
    private FirebaseAuth firebaseAuth;
    private AuthStateListener authStateListener;
    private boolean isLoggingIn = false; // Flag to track if we're in the middle of a login

    public static Intent newIntent(Context context) {
        return new Intent(context, LoginActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameEdit = findViewById(R.id.username_edit);
        passwordEdit = findViewById(R.id.password_edit);
        rememberMeCheckbox = findViewById(R.id.remember_me_checkbox);
        loginButton = findViewById(R.id.btn_login);
        Button createAccountButton = findViewById(R.id.btn_create_account);

        firebaseService = FirebaseService.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        
        // Beni hatırla durumunu yükle
        loadRememberMeState();
        
        // Firebase Auth başlatıldığında mevcut girişi kontrol etmek için auth state listener'ı ayarla
        authStateListener = new AuthStateListener() {
            @Override
            public void onAuthStateChanged(FirebaseAuth firebaseAuth) {
                // Giriş işleminin ortasındaysak müdahale etme
                if (isLoggingIn) {
                    return;
                }
                
                FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                
                if (currentUser != null) {
                    // Kullanıcı Firebase Auth ile kimlik doğrulaması yapıldı
                    // Beni hatırla'nın etkin olup olmadığını kontrol et
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    boolean rememberMe = prefs.getBoolean(KEY_REMEMBER_ME, false);
                    
                    if (rememberMe) {
                        // Beni hatırla etkin, otomatik giriş
                        String userId = currentUser.getUid();
                        // Kaydedilen kullanıcı ID'sini Firebase Auth UID ile eşleştirmek için güncelle
                        saveLoginState(userId, true);
                        
                        // MainActivity'ye yönlendir
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                    // Beni hatırla etkin değilse, burada çıkış yapma
                    // Sadece uygulama başlangıcında mevcut oturumları kontrol et (checkExistingLogin'de)
                }
            }
        };
        
        // Listener'ı ekle
        firebaseAuth.addAuthStateListener(authStateListener);
        
        // Ayrıca hemen kontrol et (oturum zaten geri yüklenmiş olabilir)
        checkExistingLogin();

        loginButton.setOnClickListener(v -> attemptLogin());
        createAccountButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
    
    private void checkExistingLogin() {
        // Firebase Auth'un mevcut bir kullanıcısı olup olmadığını kontrol et (oturum kalıcı)
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        
        if (currentUser != null) {
            // Kullanıcı zaten Firebase Auth ile kimlik doğrulaması yapıldı
            // Beni hatırla'nın etkin olup olmadığını kontrol et
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            boolean rememberMe = prefs.getBoolean(KEY_REMEMBER_ME, false);
            
            if (rememberMe) {
                // Beni hatırla etkin, otomatik giriş
                String userId = currentUser.getUid();
                // Kaydedilen kullanıcı ID'sini Firebase Auth UID ile eşleştirmek için güncelle
                saveLoginState(userId, true);
                
                // MainActivity'ye yönlendir
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                // Remember me not enabled, sign out
                firebaseAuth.signOut();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Bellek sızıntılarını önlemek için auth state listener'ı kaldır
        if (authStateListener != null && firebaseAuth != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }

    private void attemptLogin() {
        String username = usernameEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString();

        if (username.isEmpty()) {
            Toast.makeText(this, "Please enter username", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show();
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");

        performLogin(username, password);
    }

    private void performLogin(String username, String password) {
        // AuthStateListener'ın müdahale etmesini önlemek için bayrağı ayarla
        isLoggingIn = true;
        
        // Kullanıcının Firestore'da olup olmadığını kontrol et
        firebaseService.getUserByUsername(username, new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(Task<QuerySnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                    // User exists, get the user document
                    UserModel user = FirebaseService.documentToUser(task.getResult().getDocuments().get(0));
                    
                    if (user != null && user.id != null) {
                        // Verify password hash
                        String passwordHash = PasswordUtil.hashPassword(password);
                        
                        if (user.passwordHash != null && user.passwordHash.equals(passwordHash)) {
                            // Password matches - sign in with Firebase Auth
                            String email = username + "@cinematch.app";
                            
                            // Sign in with Firebase Auth
                            firebaseAuth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(Task<AuthResult> authTask) {
                                            if (authTask.isSuccessful()) {
                                                // Successfully signed in
                                                // Use Firebase Auth UID (not Firestore user ID) for consistency
                                                FirebaseUser firebaseUser = authTask.getResult().getUser();
                                                String userId = firebaseUser != null ? firebaseUser.getUid() : user.id;
                                                
                                                boolean rememberMe = rememberMeCheckbox.isChecked();
                                                saveLoginState(userId, rememberMe);
                                                
                                                // Reset flag before navigating
                                                isLoggingIn = false;
                                                
                                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                isLoggingIn = false;
                                                loginButton.setEnabled(true);
                                                loginButton.setText("Login");
                                                String errorMsg = authTask.getException() != null ? 
                                                    authTask.getException().getMessage() : "Authentication failed";
                                                Toast.makeText(LoginActivity.this, "Login failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        } else {
                            isLoggingIn = false;
                            loginButton.setEnabled(true);
                            loginButton.setText("Login");
                            Toast.makeText(LoginActivity.this, "Invalid password", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        isLoggingIn = false;
                        loginButton.setEnabled(true);
                        loginButton.setText("Login");
                        Toast.makeText(LoginActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    isLoggingIn = false;
                    loginButton.setEnabled(true);
                    loginButton.setText("Login");
                    if (task.getException() != null) {
                        String errorMsg = task.getException().getMessage();
                        Toast.makeText(LoginActivity.this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "User not found. Please register first.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void saveLoginState(String userId, boolean rememberMe) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe);
        editor.apply();
    }
    
    private void loadRememberMeState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean rememberMe = prefs.getBoolean(KEY_REMEMBER_ME, false);
        rememberMeCheckbox.setChecked(rememberMe);
    }

    public static void saveLoginState(Context context, String userId) {
        saveLoginState(context, userId, true); // Default to remember me for registration
    }
    
    public static void saveLoginState(Context context, String userId, boolean rememberMe) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe);
        editor.apply();
    }

    public static boolean isLoggedIn(Context context) {
        // Firebase Auth automatically persists sessions, so if there's a current user, they're logged in
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return true;
        }
        
        // If no Firebase Auth user, check SharedPreferences (for backward compatibility)
        // Only use this if rememberMe is checked
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean rememberMe = prefs.getBoolean(KEY_REMEMBER_ME, false);
        
        // If rememberMe is not checked, don't use saved login state
        if (!rememberMe) {
            return false;
        }
        
        // If rememberMe is checked, check if there's a saved login state
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public static void logout(Context context) {
        FirebaseAuth.getInstance().signOut();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_LOGGED_IN, false);
        editor.putBoolean(KEY_REMEMBER_ME, false);
        editor.remove(KEY_USER_ID);
        editor.apply();
    }

    public static String getCurrentUserId(Context context) {
        // Always use Firebase Auth as the source of truth
        // This ensures we always get the currently authenticated user's ID
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String authUserId = currentUser.getUid();
            // Firebase Auth ile eşleşmesi için SharedPreferences'ı güncelle (geriye dönük uyumluluk için)
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String savedUserId = prefs.getString(KEY_USER_ID, null);
            if (!authUserId.equals(savedUserId)) {
                // Kullanıcı ID'si değişti (örn. çıkış/giriş sonrası), SharedPreferences'ı güncelle
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_USER_ID, authUserId);
                editor.apply();
            }
            return authUserId;
        }
        
        // Firebase Auth kullanıcısı yoksa, null döndür (eski SharedPreferences kullanma)
        // Bu, çıkış sonrası eski kullanıcı ID'lerinin kullanılmasını önler
        return null;
    }
}

