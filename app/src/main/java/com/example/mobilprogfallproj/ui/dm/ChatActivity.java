package com.example.mobilprogfallproj.ui.dm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilprogfallproj.R;
import com.example.mobilprogfallproj.data.firebase.models.MessageModel;
import com.example.mobilprogfallproj.data.repo.SocialRepository;
import com.example.mobilprogfallproj.ui.login.LoginActivity;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    private static final String EXTRA_USER_ID = "user_id";

    private String otherUserId;
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private EditText messageEdit;
    private SocialRepository socialRepository;

    public static Intent newIntent(Context context, String userId) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        otherUserId = getIntent().getStringExtra(EXTRA_USER_ID);
        if (otherUserId == null) {
            finish();
            return;
        }

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        String currentUserId = LoginActivity.getCurrentUserId(this);
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Adaptör uyumluluğu için String'i long'a dönüştür (geçici)
        // Güvenli dönüştürme: long olarak parse etmeyi dene, başarısız olursa hash kodu kullan
        long currentUserIdLong;
        try {
            currentUserIdLong = Long.parseLong(currentUserId);
        } catch (NumberFormatException e) {
            // ID UUID veya sayısal değilse, hash kodu kullan
            currentUserIdLong = (long) currentUserId.hashCode();
        }
        adapter = new MessageAdapter(new ArrayList<>(), currentUserIdLong);
        recyclerView.setAdapter(adapter);

        messageEdit = findViewById(R.id.message_edit);
        findViewById(R.id.send_button).setOnClickListener(v -> sendMessage());

        socialRepository = new SocialRepository(this);
        loadMessages();
    }

    private void loadMessages() {
        String currentUserId = LoginActivity.getCurrentUserId(this);
        if (currentUserId == null || currentUserId.isEmpty() || otherUserId == null || otherUserId.isEmpty()) {
            return;
        }
        socialRepository.getConversation(currentUserId, otherUserId, 
            new SocialRepository.Callback<List<MessageModel>>() {
                @Override
                public void onSuccess(List<MessageModel> messages) {
                    // Adaptör uyumluluğu için MessageModel'i MessageEntity'ye dönüştür
                    List<com.example.mobilprogfallproj.data.db.entities.MessageEntity> messageEntities = new ArrayList<>();
                    for (MessageModel msg : messages) {
                        com.example.mobilprogfallproj.data.db.entities.MessageEntity entity = 
                            new com.example.mobilprogfallproj.data.db.entities.MessageEntity();
                        // Mesaj ID'si için güvenli dönüştürme
                        try {
                            entity.id = Long.parseLong(msg.id != null ? msg.id : "0");
                        } catch (NumberFormatException e) {
                            entity.id = (long) (msg.id != null ? msg.id.hashCode() : 0);
                        }
                        // Kullanıcı ID'leri için güvenli dönüştürme
                        try {
                            entity.fromUserId = Long.parseLong(msg.fromUserId);
                        } catch (NumberFormatException e) {
                            entity.fromUserId = (long) msg.fromUserId.hashCode();
                        }
                        try {
                            entity.toUserId = Long.parseLong(msg.toUserId);
                        } catch (NumberFormatException e) {
                            entity.toUserId = (long) msg.toUserId.hashCode();
                        }
                        entity.text = msg.text;
                        entity.timestamp = msg.timestamp;
                        messageEntities.add(entity);
                    }
                    adapter.updateMessages(messageEntities);
                    if (!messageEntities.isEmpty()) {
                        recyclerView.scrollToPosition(messageEntities.size() - 1);
                    }
                }

                @Override
                public void onError(String msg) {
                    Toast.makeText(ChatActivity.this, "Error: " + msg, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void sendMessage() {
        String text = messageEdit.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }

        // Firebase Auth UID kullandığımızdan emin ol (SharedPreferences yedeği değil)
        String currentUserId = null;
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Not authenticated. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        socialRepository.sendMessage(currentUserId, otherUserId, text,
            new SocialRepository.Callback<MessageModel>() {
                @Override
                public void onSuccess(MessageModel message) {
                    messageEdit.setText("");
                    loadMessages();
                }

                @Override
                public void onError(String msg) {
                    Toast.makeText(ChatActivity.this, "Error: " + msg, Toast.LENGTH_SHORT).show();
                }
            });
    }
}

