package com.example.mobilprogfallproj.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.Menu;

import com.example.mobilprogfallproj.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.badge.BadgeDrawable;

public class NotificationBadgeManager {
    private static final String PREFS_NAME = "NotificationPrefs";
    private static final String KEY_LAST_TIMELINE_VIEW = "last_timeline_view";
    private static final String KEY_LAST_MESSAGES_VIEW = "last_messages_view";
    
    private SharedPreferences prefs;
    private BottomNavigationView bottomNav;
    
    public NotificationBadgeManager(Context context, BottomNavigationView bottomNav) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.bottomNav = bottomNav;
    }
    
    /**
     * Yeni zaman çizelgesi etkinliklerini kontrol et ve rozeti güncelle
     */
    public void checkTimelineNotifications(String currentUserId, long newestEventTimestamp) {
        if (currentUserId == null) return;
        
        long lastViewTime = prefs.getLong(KEY_LAST_TIMELINE_VIEW + "_" + currentUserId, 0);
        
        if (newestEventTimestamp > lastViewTime) {
            showTimelineBadge();
        } else {
            hideTimelineBadge();
        }
    }
    
    /**
     * Yeni mesajları kontrol et ve rozeti güncelle
     */
    public void checkMessageNotifications(String currentUserId, long newestMessageTimestamp) {
        if (currentUserId == null) return;
        
        long lastViewTime = prefs.getLong(KEY_LAST_MESSAGES_VIEW + "_" + currentUserId, 0);
        
        if (newestMessageTimestamp > lastViewTime) {
            showMessagesBadge();
        } else {
            hideMessagesBadge();
        }
    }
    
    /**
     * Zaman çizelgesini görüntülendi olarak işaretle (rozeti temizle)
     */
    public void markTimelineViewed(String currentUserId) {
        if (currentUserId == null) return;
        prefs.edit().putLong(KEY_LAST_TIMELINE_VIEW + "_" + currentUserId, System.currentTimeMillis()).apply();
        hideTimelineBadge();
    }
    
    /**
     * Mesajları görüntülendi olarak işaretle (rozeti temizle)
     */
    public void markMessagesViewed(String currentUserId) {
        if (currentUserId == null) return;
        prefs.edit().putLong(KEY_LAST_MESSAGES_VIEW + "_" + currentUserId, System.currentTimeMillis()).apply();
        hideMessagesBadge();
    }
    
    private void showTimelineBadge() {
        Menu menu = bottomNav.getMenu();
        android.view.MenuItem timelineItem = menu.findItem(R.id.nav_timeline);
        if (timelineItem != null) {
            BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_timeline);
            badge.setVisible(true);
            badge.setNumber(0); // Sadece bir nokta göster
            // Koyu mavi renk ayarla
            badge.setBackgroundColor(0xFF1976D2); // Koyu mavi
        }
    }
    
    private void hideTimelineBadge() {
        Menu menu = bottomNav.getMenu();
        android.view.MenuItem timelineItem = menu.findItem(R.id.nav_timeline);
        if (timelineItem != null) {
            BadgeDrawable badge = bottomNav.getBadge(R.id.nav_timeline);
            if (badge != null) {
                badge.setVisible(false);
            }
        }
    }
    
    private void showMessagesBadge() {
        Menu menu = bottomNav.getMenu();
        android.view.MenuItem messagesItem = menu.findItem(R.id.nav_messages);
        if (messagesItem != null) {
            BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_messages);
            badge.setVisible(true);
            badge.setNumber(0); // Sadece bir nokta göster
            // Koyu mavi renk ayarla
            badge.setBackgroundColor(0xFF1976D2); // Koyu mavi
        }
    }
    
    private void hideMessagesBadge() {
        Menu menu = bottomNav.getMenu();
        android.view.MenuItem messagesItem = menu.findItem(R.id.nav_messages);
        if (messagesItem != null) {
            BadgeDrawable badge = bottomNav.getBadge(R.id.nav_messages);
            if (badge != null) {
                badge.setVisible(false);
            }
        }
    }
}

