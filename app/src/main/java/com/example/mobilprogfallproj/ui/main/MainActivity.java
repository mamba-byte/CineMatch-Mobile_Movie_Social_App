package com.example.mobilprogfallproj.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.example.mobilprogfallproj.R;
import com.example.mobilprogfallproj.data.firebase.models.MessageModel;
import com.example.mobilprogfallproj.data.firebase.models.TimelineEventModel;
import com.example.mobilprogfallproj.data.repo.SocialRepository;
import com.example.mobilprogfallproj.ui.dm.MessagesFragment;
import com.example.mobilprogfallproj.ui.foryou.ForYouFragment;
import com.example.mobilprogfallproj.ui.home.HomeFragment;
import com.example.mobilprogfallproj.ui.login.LoginActivity;
import com.example.mobilprogfallproj.ui.profile.ProfileFragment;
import com.example.mobilprogfallproj.ui.timeline.TimelineFragment;
import com.example.mobilprogfallproj.util.NotificationBadgeManager;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;
    private NotificationBadgeManager badgeManager;
    private SocialRepository socialRepository;
    private Handler handler;
    private Runnable notificationChecker;
    private boolean isCheckingNotifications = false;
    private boolean isPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Kullanıcının giriş yapıp yapmadığını kontrol et
        if (!LoginActivity.isLoggedIn(this)) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav);
        badgeManager = new NotificationBadgeManager(this, bottomNav);
        socialRepository = new SocialRepository(this);
        handler = new Handler(Looper.getMainLooper());
        
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(android.view.MenuItem item) {
                    Fragment selectedFragment = null;
                    int itemId = item.getItemId();
                    String currentUserId = LoginActivity.getCurrentUserId(MainActivity.this);
                    
                    if (itemId == R.id.nav_home) {
                        selectedFragment = new HomeFragment();
                    } else if (itemId == R.id.nav_foryou) {
                        selectedFragment = new ForYouFragment();
                    } else if (itemId == R.id.nav_timeline) {
                        selectedFragment = new TimelineFragment();
                        // Kullanıcı zaman çizelgesine gittiğinde görüntülendi olarak işaretle
                        badgeManager.markTimelineViewed(currentUserId);
                    } else if (itemId == R.id.nav_messages) {
                        selectedFragment = new MessagesFragment();
                        // Kullanıcı mesajlara gittiğinde görüntülendi olarak işaretle
                        badgeManager.markMessagesViewed(currentUserId);
                    } else if (itemId == R.id.nav_profile) {
                        selectedFragment = new ProfileFragment();
                    }

                    if (selectedFragment != null) {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, selectedFragment)
                                .commit();
                        return true;
                    }
                    return false;
                }
            });
        }
        
        // Bildirim kontrolünü başlat
        startNotificationChecker();

        // Varsayılan fragment'i ayarla
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }
    
    private void startNotificationChecker() {
        if (notificationChecker != null) {
            handler.removeCallbacks(notificationChecker);
        }
        notificationChecker = new Runnable() {
            @Override
            public void run() {
                // Sadece uygulama ön plandaysa kontrol et
                if (!isPaused && !isCheckingNotifications) {
                    checkNotifications();
                }
                // Her 90 saniyede bir kontrol et (CPU kullanımını önlemek için daha da azaltıldı)
                if (!isPaused) {
                    handler.postDelayed(this, 90000);
                }
            }
        };
        // İlk kontrol 10 saniye sonra (uygulamanın yüklenmesi için daha fazla zaman ver)
        if (!isPaused) {
            handler.postDelayed(notificationChecker, 10000);
        }
    }
    
    private void stopNotificationChecker() {
        if (handler != null && notificationChecker != null) {
            handler.removeCallbacks(notificationChecker);
        }
    }
    
    private void checkNotifications() {
        if (isCheckingNotifications) {
            return; // Çakışan kontrolleri önle
        }
        
        String currentUserId = LoginActivity.getCurrentUserId(this);
        if (currentUserId == null || currentUserId.isEmpty()) {
            return;
        }
        
        isCheckingNotifications = true;
        
        // Her iki kontrolün tamamlanmasını takip et
        final boolean[] timelineComplete = {false};
        final boolean[] messagesComplete = {false};
        
        // Her ikisi tamamlandığında bayrağı sıfırlamak için yardımcı
        Runnable checkCompletion = () -> {
            if (timelineComplete[0] && messagesComplete[0]) {
                isCheckingNotifications = false;
            }
        };
        
        // Hafif yöntem kullanarak yeni zaman çizelgesi etkinliklerini kontrol et (sadece 1 etkinlik)
        checkTimelineLightweight(currentUserId, () -> {
            timelineComplete[0] = true;
            checkCompletion.run();
        });
        
        // Yeni mesajları kontrol et - daha hafif bir sorgu kullan
        checkMessagesLightweight(currentUserId, () -> {
            messagesComplete[0] = true;
            checkCompletion.run();
        });
    }
    
    private void checkTimelineLightweight(String currentUserId, Runnable onComplete) {
        // Önce takip edilen kullanıcıları al, sonra en yeni etkinliği kontrol et
        socialRepository.getFollowedUsers(currentUserId, new SocialRepository.Callback<List<String>>() {
            @Override
            public void onSuccess(List<String> followedUserIds) {
                followedUserIds.add(currentUserId); // Kendi etkinliklerini dahil et
                
                // Sadece 1 en yeni etkinliği almak için hafif yöntem kullan
                com.example.mobilprogfallproj.data.firebase.FirebaseService firebaseService = 
                    com.example.mobilprogfallproj.data.firebase.FirebaseService.getInstance();
                
                // Önce sıralı sorguyu dene (indeks varsa daha verimli)
                firebaseService.getNewestTimelineEventForUsers(followedUserIds, 
                    new com.google.android.gms.tasks.OnCompleteListener<com.google.firebase.firestore.QuerySnapshot>() {
                        @Override
                        public void onComplete(com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task) {
                            long newestTimestamp = 0;
                            
                            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                                // İlk (en yeni) etkinliği al (zaten zaman damgasına göre DESC sıralı)
                                com.google.firebase.firestore.QueryDocumentSnapshot firstDoc = 
                                    (com.google.firebase.firestore.QueryDocumentSnapshot) task.getResult().getDocuments().get(0);
                                Long timestamp = firstDoc.getLong("timestamp");
                                if (timestamp != null) {
                                    newestTimestamp = timestamp;
                                }
                                android.util.Log.d("MainActivity", "En yeni zaman çizelgesi etkinliği zaman damgası bulundu: " + newestTimestamp);
                            } else {
                                // Sıralı sorgu başarısız olursa (örn. eksik indeks), tüm etkinlikleri alma yöntemine geri dön
                                if (task.getException() != null) {
                                    android.util.Log.w("MainActivity", "Sıralı zaman çizelgesi sorgusu başarısız (indeks gerekebilir), yedek kullanılıyor: " + task.getException().getMessage());
                                }
                                // Yedek: takip edilen kullanıcılar için tüm zaman çizelgesi etkinliklerini al ve istemci tarafında en yenisini bul
                                firebaseService.getTimelineEventsForFollowedUsers(followedUserIds, 
                                    new com.google.android.gms.tasks.OnCompleteListener<com.google.firebase.firestore.QuerySnapshot>() {
                                        @Override
                                        public void onComplete(com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> fallbackTask) {
                                            long newestTimestamp = 0;
                                            if (fallbackTask.isSuccessful() && fallbackTask.getResult() != null) {
                                                // İstemci tarafında en yeni zaman damgasını bul
                                                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : fallbackTask.getResult()) {
                                                    Long timestamp = doc.getLong("timestamp");
                                                    if (timestamp != null && timestamp > newestTimestamp) {
                                                        newestTimestamp = timestamp;
                                                    }
                                                }
                                                android.util.Log.d("MainActivity", "Yedek: En yeni zaman çizelgesi etkinliği zaman damgası bulundu: " + newestTimestamp);
                                            }
                                            badgeManager.checkTimelineNotifications(currentUserId, newestTimestamp);
                                            if (onComplete != null) onComplete.run();
                                        }
                                    });
                                return; // Yedek tamamlamayı ele alacak
                            }
                            
                            badgeManager.checkTimelineNotifications(currentUserId, newestTimestamp);
                            if (onComplete != null) onComplete.run();
                        }
                    });
            }
            
            @Override
            public void onError(String msg) {
                android.util.Log.e("MainActivity", "Error getting followed users for timeline check: " + msg);
                badgeManager.checkTimelineNotifications(currentUserId, 0);
                if (onComplete != null) onComplete.run();
            }
        });
    }
    
    private void checkMessagesLightweight(String currentUserId, Runnable onComplete) {
        // FirebaseService'i doğrudan kullan - alınan mesajları kontrol et (bildirimler için en önemli)
        // Daha basit bir yaklaşım kullanacağız: tüm alınan mesajları al ve istemci tarafında en yenisini bul
        // Bu indeks gereksinimlerinden kaçınır ve daha güvenilirdir
        com.example.mobilprogfallproj.data.firebase.FirebaseService firebaseService = 
            com.example.mobilprogfallproj.data.firebase.FirebaseService.getInstance();
        
        // Önce sıralı sorguyu dene (indeks varsa daha verimli)
        firebaseService.getMessagesForReceiverLimited(currentUserId, 
            new com.google.android.gms.tasks.OnCompleteListener<com.google.firebase.firestore.QuerySnapshot>() {
                @Override
                public void onComplete(com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task) {
                    long newestTimestamp = 0;
                    
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        // İlk (en yeni) mesajı al (zaten zaman damgasına göre DESC sıralı)
                        com.google.firebase.firestore.QueryDocumentSnapshot firstDoc = 
                            (com.google.firebase.firestore.QueryDocumentSnapshot) task.getResult().getDocuments().get(0);
                        Long timestamp = firstDoc.getLong("timestamp");
                        if (timestamp != null) {
                            newestTimestamp = timestamp;
                        }
                        android.util.Log.d("MainActivity", "En yeni alınan mesaj zaman damgası bulundu: " + newestTimestamp);
                    } else {
                        // Sıralı sorgu başarısız olursa (örn. eksik indeks), tüm mesajları alma yöntemine geri dön
                        if (task.getException() != null) {
                            android.util.Log.w("MainActivity", "Sıralı sorgu başarısız (indeks gerekebilir), yedek kullanılıyor: " + task.getException().getMessage());
                        }
                        // Yedek: tüm alınan mesajları al ve istemci tarafında en yenisini bul
                        firebaseService.getMessagesForReceiver(currentUserId, 
                            new com.google.android.gms.tasks.OnCompleteListener<com.google.firebase.firestore.QuerySnapshot>() {
                                @Override
                                public void onComplete(com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> fallbackTask) {
                                    long newestTimestamp = 0;
                                    if (fallbackTask.isSuccessful() && fallbackTask.getResult() != null) {
                                        // İstemci tarafında en yeni zaman damgasını bul
                                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : fallbackTask.getResult()) {
                                            Long timestamp = doc.getLong("timestamp");
                                            if (timestamp != null && timestamp > newestTimestamp) {
                                                newestTimestamp = timestamp;
                                            }
                                        }
                                        android.util.Log.d("MainActivity", "Yedek: En yeni alınan mesaj zaman damgası bulundu: " + newestTimestamp);
                                    }
                                    badgeManager.checkMessageNotifications(currentUserId, newestTimestamp);
                                    if (onComplete != null) onComplete.run();
                                }
                            });
                        return; // Yedek tamamlamayı ele alacak
                    }
                    
                    badgeManager.checkMessageNotifications(currentUserId, newestTimestamp);
                    if (onComplete != null) onComplete.run();
                }
            });
    }
    
    private void checkAllMessagesForUser(String currentUserId) {
        // Kullanıcı için tüm mesajları almak için FirebaseService'i doğrudan kullan
        com.example.mobilprogfallproj.data.firebase.FirebaseService firebaseService = 
            com.example.mobilprogfallproj.data.firebase.FirebaseService.getInstance();
        
        final long[] newestTimestamp = {0};
        final int[] completed = {0};
        final int totalQueries = 2;
        
        // Alınan mesajları kontrol et
        firebaseService.getMessagesForReceiver(currentUserId, 
            new com.google.android.gms.tasks.OnCompleteListener<com.google.firebase.firestore.QuerySnapshot>() {
                @Override
                public void onComplete(com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task) {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : task.getResult()) {
                            Long timestamp = doc.getLong("timestamp");
                            if (timestamp != null && timestamp > newestTimestamp[0]) {
                                newestTimestamp[0] = timestamp;
                            }
                        }
                    }
                    completed[0]++;
                    if (completed[0] == totalQueries) {
                        badgeManager.checkMessageNotifications(currentUserId, newestTimestamp[0]);
                    }
                }
            });
        
        // Gönderilen mesajları da kontrol et
        firebaseService.getMessagesForSender(currentUserId, 
            new com.google.android.gms.tasks.OnCompleteListener<com.google.firebase.firestore.QuerySnapshot>() {
                @Override
                public void onComplete(com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task) {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : task.getResult()) {
                            Long timestamp = doc.getLong("timestamp");
                            if (timestamp != null && timestamp > newestTimestamp[0]) {
                                newestTimestamp[0] = timestamp;
                            }
                        }
                    }
                    completed[0]++;
                    if (completed[0] == totalQueries) {
                        badgeManager.checkMessageNotifications(currentUserId, newestTimestamp[0]);
                    }
                }
            });
    }
    
    public void markTimelineViewed(String currentUserId) {
        if (badgeManager != null) {
            badgeManager.markTimelineViewed(currentUserId);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        isPaused = true;
        stopNotificationChecker();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        isPaused = false;
        startNotificationChecker();
        // Uygulama devam ettiğinde hemen bildirimleri kontrol et
        String currentUserId = LoginActivity.getCurrentUserId(this);
        if (currentUserId != null && !currentUserId.isEmpty() && !isCheckingNotifications) {
            checkNotifications();
        }
    }
    
    /**
     * Mesajları görüntülendi olarak işaretlemek için genel yöntem (MessagesFragment'ten çağrılır)
     */
    public void markMessagesViewed(String currentUserId) {
        if (badgeManager != null && currentUserId != null) {
            badgeManager.markMessagesViewed(currentUserId);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopNotificationChecker();
    }
}

