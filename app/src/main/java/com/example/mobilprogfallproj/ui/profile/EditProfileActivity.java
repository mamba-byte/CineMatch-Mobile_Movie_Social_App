package com.example.mobilprogfallproj.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobilprogfallproj.R;
import com.example.mobilprogfallproj.data.firebase.FirebaseService;
import com.example.mobilprogfallproj.data.firebase.models.UserModel;
import com.example.mobilprogfallproj.ui.login.LoginActivity;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {
    
    private EditText displayNameEdit;
    private EditText bioEdit;
    private ImageView profileImagePreview;
    private String selectedImageUri;
    private FirebaseService firebaseService;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri.toString();
                    profileImagePreview.setImageURI(uri);
                }
            });

    public static Intent newIntent(Context context) {
        return new Intent(context, EditProfileActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        displayNameEdit = findViewById(R.id.display_name_edit);
        bioEdit = findViewById(R.id.bio_edit);
        profileImagePreview = findViewById(R.id.profile_image_preview);
        Button saveButton = findViewById(R.id.btn_save);
        Button changePhotoButton = findViewById(R.id.btn_change_photo);

        firebaseService = FirebaseService.getInstance();

        loadUserProfile();

        saveButton.setOnClickListener(v -> saveProfile());
        changePhotoButton.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
    }

    private void loadUserProfile() {
        String currentUserId = LoginActivity.getCurrentUserId(this);
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        firebaseService.getUserById(currentUserId, new com.google.android.gms.tasks.OnCompleteListener<com.google.firebase.firestore.DocumentSnapshot>() {
            @Override
            public void onComplete(com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    UserModel user = FirebaseService.documentToUser(task.getResult());
                    if (user != null) {
                        displayNameEdit.setText(user.displayName);
                        if (user.bio != null) {
                            bioEdit.setText(user.bio);
                        }
                        if (user.profileImageUrl != null) {
                            selectedImageUri = user.profileImageUrl;
                            try {
                                profileImagePreview.setImageURI(Uri.parse(user.profileImageUrl));
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        });
    }

    private void saveProfile() {
        String displayName = displayNameEdit.getText().toString().trim();
        String bio = bioEdit.getText().toString().trim();

        if (displayName.isEmpty()) {
            Toast.makeText(this, "Display name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = LoginActivity.getCurrentUserId(this);
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", displayName);
        updates.put("bio", bio);
        if (selectedImageUri != null) {
            updates.put("profileImageUrl", selectedImageUri);
        }

        firebaseService.updateUser(currentUserId, updates, 
            new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    Toast.makeText(EditProfileActivity.this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            },
            new com.google.android.gms.tasks.OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(EditProfileActivity.this, "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }
}

