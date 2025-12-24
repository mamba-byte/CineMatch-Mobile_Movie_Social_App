package com.example.mobilprogfallproj.data.firebase;

import android.util.Log;

import com.example.mobilprogfallproj.data.firebase.models.FollowModel;
import com.example.mobilprogfallproj.data.firebase.models.MessageModel;
import com.example.mobilprogfallproj.data.firebase.models.MovieModel;
import com.example.mobilprogfallproj.data.firebase.models.TimelineEventModel;
import com.example.mobilprogfallproj.data.firebase.models.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseService {
    private static final String TAG = "FirebaseService";
    private static FirebaseService instance;
    private FirebaseFirestore db;

    private FirebaseService() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirebaseService getInstance() {
        if (instance == null) {
            instance = new FirebaseService();
        }
        return instance;
    }

    // ========== KULLANICI İŞLEMLERİ ==========

    public void getUserById(String userId, OnCompleteListener<DocumentSnapshot> listener) {
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(listener);
    }

    public void getUserByUsername(String username, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnCompleteListener(listener);
    }

    public void getAllUsers(OnCompleteListener<QuerySnapshot> listener) {
        db.collection("users")
                .get()
                .addOnCompleteListener(listener);
    }

    public void createUser(UserModel user, OnSuccessListener<Void> successListener, OnFailureListener failureListener) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", user.username);
        userData.put("displayName", user.displayName);
        userData.put("bio", user.bio);
        userData.put("profileImageUrl", user.profileImageUrl);
        userData.put("passwordHash", user.passwordHash);

        db.collection("users").document(user.id)
                .set(userData)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void updateUser(String userId, Map<String, Object> updates, OnSuccessListener<Void> successListener, OnFailureListener failureListener) {
        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    // ========== MESAJ İŞLEMLERİ ==========

    public void getMessagesBetweenUsers(String userId1, String userId2, OnCompleteListener<QuerySnapshot> listener) {
        // whereIn kullanarak her iki yöndeki mesajları al
        // Güvenlik kuralları artık kimlik doğrulaması yapılmış kullanıcılar için liste sorgularına izin veriyor
        // ve get işlemleri bireysel belge izinlerini kontrol ediyor
        db.collection("messages")
                .whereIn("fromUserId", java.util.Arrays.asList(userId1, userId2))
                .whereIn("toUserId", java.util.Arrays.asList(userId1, userId2))
                .get()
                .addOnCompleteListener(listener);
    }
    
    public void getMessagesFromTo(String fromUserId, String toUserId, OnCompleteListener<QuerySnapshot> listener) {
        // Bir kullanıcıdan diğerine mesajları al (sadece bir yön)
        // Her sorgu ayrı ayrı doğrulandığı için bu güvenlik kurallarıyla çalışır
        db.collection("messages")
                .whereEqualTo("fromUserId", fromUserId)
                .whereEqualTo("toUserId", toUserId)
                .get()
                .addOnCompleteListener(listener);
    }

    public void getMessagesForReceiver(String userId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("messages")
                .whereEqualTo("toUserId", userId)
                .get()
                .addOnCompleteListener(listener);
    }
    
    public void getMessagesForReceiverLimited(String userId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("messages")
                .whereEqualTo("toUserId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING) // En yeni önce sırala
                .limit(1) // Hafif bildirim kontrolleri için 1 ile sınırla
                .get()
                .addOnCompleteListener(listener);
    }

    public void getMessagesForSender(String userId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("messages")
                .whereEqualTo("fromUserId", userId)
                .get()
                .addOnCompleteListener(listener);
    }
    
    public void getMessagesForSenderLimited(String userId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("messages")
                .whereEqualTo("fromUserId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING) // En yeni önce sırala
                .limit(1) // Hafif bildirim kontrolleri için 1 ile sınırla
                .get()
                .addOnCompleteListener(listener);
    }

    public void sendMessage(MessageModel message, OnSuccessListener<DocumentReference> successListener, OnFailureListener failureListener) {
        // Kimlik doğrulamayı doğrula
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            failureListener.onFailure(new Exception("Kullanıcı kimlik doğrulaması yapılmadı"));
            return;
        }
        
        String authUid = currentUser.getUid();
        
        // Gerekli alanları doğrula
        if (message.toUserId == null || message.toUserId.trim().isEmpty()) {
            failureListener.onFailure(new Exception("toUserId null veya boş olamaz"));
            return;
        }
        
        if (message.text == null || message.text.trim().isEmpty()) {
            failureListener.onFailure(new Exception("Mesaj metni null veya boş olamaz"));
            return;
        }
        
        // fromUserId'nin Firebase Auth UID ile eşleştiğinden emin ol (uyumsuzluk varsa geçersiz kıl)
        message.fromUserId = authUid;
        
        // Zaman damgasının ayarlandığından emin ol
        if (message.timestamp <= 0) {
            message.timestamp = System.currentTimeMillis();
        }
        
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("fromUserId", message.fromUserId);
        messageData.put("toUserId", message.toUserId.trim());
        messageData.put("text", message.text.trim());
        messageData.put("timestamp", message.timestamp);
        
        android.util.Log.d("FirebaseService", "Sending message - fromUserId: " + message.fromUserId + ", toUserId: " + message.toUserId + ", text length: " + message.text.length());

        db.collection("messages")
                .add(messageData)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    // ========== TAKİP İŞLEMLERİ ==========

    public void getFollows(String followerId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("follows")
                .whereEqualTo("followerId", followerId)
                .get()
                .addOnCompleteListener(listener);
    }

    public void getFollowers(String followedId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("follows")
                .whereEqualTo("followedId", followedId)
                .get()
                .addOnCompleteListener(listener);
    }

    public void followUser(String followerId, String followedId, OnSuccessListener<Void> successListener, OnFailureListener failureListener) {
        Map<String, Object> followData = new HashMap<>();
        followData.put("followerId", followerId);
        followData.put("followedId", followedId);

        String followId = followerId + "_" + followedId;
        db.collection("follows").document(followId)
                .set(followData)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void unfollowUser(String followerId, String followedId, OnSuccessListener<Void> successListener, OnFailureListener failureListener) {
        String followId = followerId + "_" + followedId;
        db.collection("follows").document(followId)
                .delete()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    // ========== FİLM İŞLEMLERİ ==========

    public void getAllMovies(OnCompleteListener<QuerySnapshot> listener) {
        db.collection("movies")
                .get()
                .addOnCompleteListener(listener);
    }

    public void getMovieById(int movieId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("movies")
                .whereEqualTo("id", movieId)
                .limit(1)
                .get()
                .addOnCompleteListener(listener);
    }

    public void createMovie(MovieModel movie, OnSuccessListener<Void> successListener, OnFailureListener failureListener) {
        Map<String, Object> movieData = new HashMap<>();
        movieData.put("id", movie.id);
        movieData.put("title", movie.title);
        movieData.put("posterPath", movie.posterPath);
        movieData.put("overview", movie.overview);
        movieData.put("releaseYear", movie.releaseYear);
        movieData.put("tmdbRating", movie.tmdbRating);

        db.collection("movies").document(String.valueOf(movie.id))
                .set(movieData)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    // ========== ZAMAN ÇİZELGESİ İŞLEMLERİ ==========

    public void getTimelineEventsForUser(String userId, OnCompleteListener<QuerySnapshot> listener) {
        // İndeks gereksiniminden kaçınmak için orderBy olmadan al, istemci tarafında sıralayacağız
        db.collection("timeline_events")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(listener);
    }

    public void getNewestTimelineEventForUsers(List<String> userIds, OnCompleteListener<QuerySnapshot> listener) {
        // Bildirim kontrolü için hafif yöntem - sadece 1 en yeni etkinliği al
        // En yeni etkinliği önce almak için zaman damgasına göre azalan sırada sırala
        if (userIds.isEmpty()) {
            db.collection("timeline_events")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1) // Bildirim kontrolü için sadece 1'e ihtiyaç var
                    .get()
                    .addOnCompleteListener(listener);
            return;
        }

        if (userIds.size() <= 10) {
            db.collection("timeline_events")
                    .whereIn("userId", userIds)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1) // Bildirim kontrolü için sadece 1'e ihtiyaç var
                    .get()
                    .addOnCompleteListener(listener);
        } else {
            // 10'dan fazla kullanıcı için, tümünden sadece 1 etkinlik al (en yeniye göre sıralı)
            db.collection("timeline_events")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnCompleteListener(listener);
        }
    }
    
    public void getTimelineEventsForFollowedUsers(List<String> userIds, OnCompleteListener<QuerySnapshot> listener) {
        if (userIds.isEmpty()) {
            // Boş sorgu döndür - sadece tüm etkinlikleri al ve gerekirse istemci tarafında filtrele
            // İndeks gereksiniminden kaçınmak için orderBy olmadan al
            db.collection("timeline_events")
                    .limit(100) // Daha fazla al ve istemci tarafında sırala
                    .get()
                    .addOnCompleteListener(listener);
            return;
        }

        // Firestore 'in' sorgu limiti 10'dur, bu yüzden 10'dan fazlaysa toplu işlem yapmalıyız
        // İndeks gereksiniminden kaçınmak için orderBy olmadan al, istemci tarafında sırala
        if (userIds.size() <= 10) {
            db.collection("timeline_events")
                    .whereIn("userId", userIds)
                    .limit(100) // Daha fazla al ve istemci tarafında sırala
                    .get()
                    .addOnCompleteListener(listener);
        } else {
            // 10'dan fazla için, ilk 10'u al
            List<String> firstBatch = userIds.subList(0, Math.min(10, userIds.size()));
            db.collection("timeline_events")
                    .whereIn("userId", firstBatch)
                    .limit(100) // Daha fazla al ve istemci tarafında sırala
                    .get()
                    .addOnCompleteListener(listener);
        }
    }

    public void createTimelineEvent(TimelineEventModel event, OnSuccessListener<DocumentReference> successListener, OnFailureListener failureListener) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userId", event.userId);
        eventData.put("movieId", event.movieId);
        eventData.put("type", event.type);
        eventData.put("timestamp", event.timestamp);

        db.collection("timeline_events")
                .add(eventData)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    // ========== KULLANICI FİLM İŞLEMLERİ (Favoriler/İzlenenler) ==========

    public void addUserMovie(String userId, int movieId, boolean isFavorite, boolean isWatched, OnSuccessListener<Void> successListener, OnFailureListener failureListener) {
        String userMovieId = userId + "_" + movieId;
        Map<String, Object> userMovieData = new HashMap<>();
        userMovieData.put("userId", userId);
        userMovieData.put("movieId", movieId);
        userMovieData.put("isFavorite", isFavorite);
        userMovieData.put("isWatched", isWatched);
        userMovieData.put("timestamp", System.currentTimeMillis());

        db.collection("userMovies").document(userMovieId)
                .set(userMovieData)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void updateUserMovie(String userId, int movieId, boolean isFavorite, boolean isWatched, OnSuccessListener<Void> successListener, OnFailureListener failureListener) {
        String userMovieId = userId + "_" + movieId;
        Map<String, Object> updates = new HashMap<>();
        updates.put("isFavorite", isFavorite);
        updates.put("isWatched", isWatched);
        updates.put("timestamp", System.currentTimeMillis());

        db.collection("userMovies").document(userMovieId)
                .update(updates)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void getUserMovies(String userId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("userMovies")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(listener);
    }

    public void getFavoriteMovies(String userId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("userMovies")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isFavorite", true)
                .get()
                .addOnCompleteListener(listener);
    }

    public void getWatchedMovies(String userId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("userMovies")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isWatched", true)
                .get()
                .addOnCompleteListener(listener);
    }

    // ========== YARDIMCI YÖNTEMLER ==========

    public static UserModel documentToUser(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        UserModel user = new UserModel();
        user.id = doc.getId();
        user.username = doc.getString("username");
        user.displayName = doc.getString("displayName");
        user.bio = doc.getString("bio");
        user.profileImageUrl = doc.getString("profileImageUrl");
        user.passwordHash = doc.getString("passwordHash");
        return user;
    }

    public static List<UserModel> queryToUsers(QuerySnapshot querySnapshot) {
        List<UserModel> users = new ArrayList<>();
        if (querySnapshot != null && !querySnapshot.isEmpty()) {
            for (QueryDocumentSnapshot doc : querySnapshot) {
                UserModel user = documentToUser(doc);
                if (user != null) users.add(user);
            }
        }
        return users;
    }

    public static MessageModel documentToMessage(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        MessageModel msg = new MessageModel();
        msg.id = doc.getId();
        msg.fromUserId = doc.getString("fromUserId");
        msg.toUserId = doc.getString("toUserId");
        msg.text = doc.getString("text");
        Long timestamp = doc.getLong("timestamp");
        msg.timestamp = timestamp != null ? timestamp : 0;
        return msg;
    }

    public static List<MessageModel> queryToMessages(QuerySnapshot querySnapshot) {
        List<MessageModel> messages = new ArrayList<>();
        if (querySnapshot != null && !querySnapshot.isEmpty()) {
            for (QueryDocumentSnapshot doc : querySnapshot) {
                MessageModel msg = documentToMessage(doc);
                if (msg != null) messages.add(msg);
            }
        }
        return messages;
    }

    public static TimelineEventModel documentToTimelineEvent(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        TimelineEventModel event = new TimelineEventModel();
        event.id = doc.getId();
        event.userId = doc.getString("userId");
        Long movieId = doc.getLong("movieId");
        event.movieId = movieId != null ? movieId.intValue() : 0;
        event.type = doc.getString("type");
        Long timestamp = doc.getLong("timestamp");
        event.timestamp = timestamp != null ? timestamp : 0;
        return event;
    }

    public static List<TimelineEventModel> queryToTimelineEvents(QuerySnapshot querySnapshot) {
        List<TimelineEventModel> events = new ArrayList<>();
        if (querySnapshot != null && !querySnapshot.isEmpty()) {
            for (QueryDocumentSnapshot doc : querySnapshot) {
                TimelineEventModel event = documentToTimelineEvent(doc);
                if (event != null) events.add(event);
            }
        }
        return events;
    }

    public static MovieModel documentToMovie(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        MovieModel movie = new MovieModel();
        Long id = doc.getLong("id");
        movie.id = id != null ? id.intValue() : 0;
        movie.title = doc.getString("title");
        movie.posterPath = doc.getString("posterPath");
        movie.overview = doc.getString("overview");
        Long releaseYear = doc.getLong("releaseYear");
        movie.releaseYear = releaseYear != null ? releaseYear.intValue() : 0;
        Double tmdbRating = doc.getDouble("tmdbRating");
        movie.tmdbRating = tmdbRating != null ? tmdbRating : 0.0;
        return movie;
    }

    public static List<MovieModel> queryToMovies(QuerySnapshot querySnapshot) {
        List<MovieModel> movies = new ArrayList<>();
        if (querySnapshot != null && !querySnapshot.isEmpty()) {
            for (QueryDocumentSnapshot doc : querySnapshot) {
                MovieModel movie = documentToMovie(doc);
                if (movie != null) movies.add(movie);
            }
        }
        return movies;
    }
}

