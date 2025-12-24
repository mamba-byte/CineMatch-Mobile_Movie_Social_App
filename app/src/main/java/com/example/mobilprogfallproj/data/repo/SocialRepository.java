package com.example.mobilprogfallproj.data.repo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.mobilprogfallproj.data.firebase.FirebaseService;
import com.example.mobilprogfallproj.data.firebase.models.MessageModel;
import com.example.mobilprogfallproj.data.firebase.models.TimelineEventModel;
import com.example.mobilprogfallproj.data.firebase.models.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SocialRepository {
    private final FirebaseService firebaseService;
    private final Handler mainHandler;

    public SocialRepository(Context context) {
        firebaseService = FirebaseService.getInstance();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void getAllUsers(Callback<List<UserModel>> callback) {
        firebaseService.getAllUsers(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    List<UserModel> users = FirebaseService.queryToUsers(task.getResult());
                    mainHandler.post(() -> callback.onSuccess(users));
                } else {
                    mainHandler.post(() -> callback.onError(task.getException() != null ? task.getException().getMessage() : "Failed to get users"));
                }
            }
        });
    }

    public void getUserById(String userId, Callback<UserModel> callback) {
        final String finalUserId = userId; // İç sınıf için etkin olarak final yap
        firebaseService.getUserById(finalUserId, new OnCompleteListener<com.google.firebase.firestore.DocumentSnapshot>() {
            @Override
            public void onComplete(Task<com.google.firebase.firestore.DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    UserModel user = FirebaseService.documentToUser(task.getResult());
                    mainHandler.post(() -> callback.onSuccess(user));
                } else {
                    mainHandler.post(() -> callback.onError(task.getException() != null ? task.getException().getMessage() : "User not found"));
                }
            }
        });
    }

    public void followUser(String followerId, String followedId, Callback<Boolean> callback) {
        final String finalFollowerId = followerId; // Etkin olarak final yap
        final String finalFollowedId = followedId; // Etkin olarak final yap
        firebaseService.followUser(finalFollowerId, finalFollowedId, new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                mainHandler.post(() -> callback.onSuccess(true));
            }
        }, new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void unfollowUser(String followerId, String followedId, Callback<Boolean> callback) {
        final String finalFollowerId = followerId; // Etkin olarak final yap
        final String finalFollowedId = followedId; // Etkin olarak final yap
        firebaseService.unfollowUser(finalFollowerId, finalFollowedId, new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                mainHandler.post(() -> callback.onSuccess(true));
            }
        }, new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void isFollowing(String followerId, String followedId, Callback<Boolean> callback) {
        final String finalFollowedId = followedId; // Make effectively final for inner class
        firebaseService.getFollows(followerId, new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    boolean isFollowing = false;
                    if (task.getResult() != null) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String followedIdFromDoc = doc.getString("followedId");
                            if (finalFollowedId.equals(followedIdFromDoc)) {
                                isFollowing = true;
                                break;
                            }
                        }
                    }
                    final boolean finalIsFollowing = isFollowing;
                    mainHandler.post(() -> callback.onSuccess(finalIsFollowing));
                } else {
                    mainHandler.post(() -> callback.onError(task.getException() != null ? task.getException().getMessage() : "Failed to check follow status"));
                }
            }
        });
    }

    public void getFollowedUsers(String userId, Callback<List<String>> callback) {
        final String finalUserId = userId; // İç sınıf için etkin olarak final yap
        firebaseService.getFollows(finalUserId, new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    List<String> userIds = new ArrayList<>();
                    if (task.getResult() != null) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String followedId = doc.getString("followedId");
                            if (followedId != null) {
                                userIds.add(followedId);
                            }
                        }
                    }
                    mainHandler.post(() -> callback.onSuccess(userIds));
                } else {
                    mainHandler.post(() -> callback.onError(task.getException() != null ? task.getException().getMessage() : "Failed to get followed users"));
                }
            }
        });
    }

    public void getFollowers(String userId, Callback<List<String>> callback) {
        final String finalUserId = userId; // İç sınıf için etkin olarak final yap
        firebaseService.getFollowers(finalUserId, new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    List<String> userIds = new ArrayList<>();
                    if (task.getResult() != null) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String followerId = doc.getString("followerId");
                            if (followerId != null) {
                                userIds.add(followerId);
                            }
                        }
                    }
                    mainHandler.post(() -> callback.onSuccess(userIds));
                } else {
                    mainHandler.post(() -> callback.onError(task.getException() != null ? task.getException().getMessage() : "Failed to get followers"));
                }
            }
        });
    }

    public void sendMessage(String fromUserId, String toUserId, String text, Callback<MessageModel> callback) {
        final String finalFromUserId = fromUserId; // Etkin olarak final yap
        final String finalToUserId = toUserId; // Etkin olarak final yap
        final String finalText = text; // Etkin olarak final yap
        MessageModel message = new MessageModel();
        message.fromUserId = finalFromUserId;
        message.toUserId = finalToUserId;
        message.text = finalText;
        message.timestamp = System.currentTimeMillis();

        firebaseService.sendMessage(message, new OnSuccessListener<com.google.firebase.firestore.DocumentReference>() {
            @Override
            public void onSuccess(com.google.firebase.firestore.DocumentReference documentReference) {
                message.id = documentReference.getId();
                mainHandler.post(() -> callback.onSuccess(message));
            }
        }, new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void getConversation(String u1, String u2, Callback<List<MessageModel>> callback) {
        final String finalU1 = u1; // Etkin olarak final yap
        final String finalU2 = u2; // Etkin olarak final yap
        
        // Güvenlik kurallarıyla çalışmak için whereIn yerine iki ayrı sorgu kullan
        // Sorgu 1: u1'den u2'ye mesajlar
        // Sorgu 2: u2'den u1'e mesajlar
        // Sonuçları istemci tarafında birleştir
        
        final List<MessageModel> allMessages = new java.util.ArrayList<>();
        final int[] completed = {0};
        final int totalQueries = 2;
        final Exception[] firstError = {null};
        
        // Sorgu 1: u1 -> u2
        firebaseService.getMessagesFromTo(finalU1, finalU2, new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(Task<QuerySnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    List<MessageModel> messages = FirebaseService.queryToMessages(task.getResult());
                    allMessages.addAll(messages);
                } else {
                    if (firstError[0] == null) {
                        firstError[0] = task.getException();
                    }
                }
                completed[0]++;
                if (completed[0] == totalQueries) {
                    // Her iki sorgu tamamlandı, sonuçları birleştir ve döndür
                    if (allMessages.isEmpty() && firstError[0] != null) {
                        mainHandler.post(() -> callback.onError(firstError[0].getMessage()));
                    } else {
                        // Zaman damgasına göre artan sırada sırala (en eski önce)
                        allMessages.sort((m1, m2) -> Long.compare(m1.timestamp, m2.timestamp));
                        mainHandler.post(() -> callback.onSuccess(allMessages));
                    }
                }
            }
        });
        
        // Sorgu 2: u2 -> u1 (ters yön)
        firebaseService.getMessagesFromTo(finalU2, finalU1, new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            List<MessageModel> messages = FirebaseService.queryToMessages(task.getResult());
                            allMessages.addAll(messages);
                        } else {
                            if (firstError[0] == null) {
                                firstError[0] = task.getException();
                            }
                        }
                        completed[0]++;
                        if (completed[0] == totalQueries) {
                            // Her iki sorgu tamamlandı, sonuçları birleştir ve döndür
                            if (allMessages.isEmpty() && firstError[0] != null) {
                                mainHandler.post(() -> callback.onError(firstError[0].getMessage()));
                            } else {
                                // Zaman damgasına göre artan sırada sırala (en eski önce)
                                allMessages.sort((m1, m2) -> Long.compare(m1.timestamp, m2.timestamp));
                                mainHandler.post(() -> callback.onSuccess(allMessages));
                            }
                        }
                    }
                });
    }

    public void getConversationUserIds(String userId, Callback<List<String>> callback) {
        final String finalUserId = userId; // İç sınıf için etkin olarak final yap
        final Set<String> userIds = new HashSet<>();
        final int[] completed = {0};
        final int totalQueries = 2;
        
        // Sorgu 1: Kullanıcının alıcı olduğu mesajlar
        firebaseService.getMessagesForReceiver(finalUserId, new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(Task<QuerySnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        String fromUserId = doc.getString("fromUserId");
                        if (fromUserId != null && !fromUserId.equals(finalUserId)) {
                            userIds.add(fromUserId);
                        }
                    }
                }
                completed[0]++;
                if (completed[0] == totalQueries) {
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>(userIds)));
                }
            }
        });
        
        // Sorgu 2: Kullanıcının gönderen olduğu mesajlar
        firebaseService.getMessagesForSender(finalUserId, new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(Task<QuerySnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        String toUserId = doc.getString("toUserId");
                        if (toUserId != null && !toUserId.equals(finalUserId)) {
                            userIds.add(toUserId);
                        }
                    }
                }
                completed[0]++;
                if (completed[0] == totalQueries) {
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>(userIds)));
                }
            }
        });
    }

    public void addTimelineEvent(String userId, int movieId, String type, Callback<TimelineEventModel> callback) {
        final String finalUserId = userId; // Etkin olarak final yap
        final int finalMovieId = movieId; // Etkin olarak final yap
        final String finalType = type; // Etkin olarak final yap
        TimelineEventModel event = new TimelineEventModel();
        event.userId = finalUserId;
        event.movieId = finalMovieId;
        event.type = finalType;
        event.timestamp = System.currentTimeMillis();

        firebaseService.createTimelineEvent(event, new OnSuccessListener<com.google.firebase.firestore.DocumentReference>() {
            @Override
            public void onSuccess(com.google.firebase.firestore.DocumentReference documentReference) {
                event.id = documentReference.getId();
                mainHandler.post(() -> callback.onSuccess(event));
            }
        }, new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void getAllTimelineEvents(Callback<List<TimelineEventModel>> callback) {
        // Tüm zaman çizelgesi etkinliklerini al - bu basitleştirilmiş bir versiyon
        // Üretimde bunu sınırlamak isteyebilirsiniz
        firebaseService.getTimelineEventsForFollowedUsers(new ArrayList<>(), new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    List<TimelineEventModel> events = FirebaseService.queryToTimelineEvents(task.getResult());
                    final List<TimelineEventModel> finalEvents = events; // Etkin olarak final yap
                    mainHandler.post(() -> callback.onSuccess(finalEvents));
                } else {
                    mainHandler.post(() -> callback.onError(task.getException() != null ? task.getException().getMessage() : "Failed to get timeline events"));
                }
            }
        });
    }

    public void getTimelineEventsForFollowedUsers(String userId, Callback<List<TimelineEventModel>> callback) {
        final String finalUserId = userId; // İç sınıf için etkin olarak final yap
        getFollowedUsers(finalUserId, new Callback<List<String>>() {
            @Override
            public void onSuccess(List<String> followedUserIds) {
                followedUserIds.add(finalUserId); // Kendi etkinliklerini dahil et
                firebaseService.getTimelineEventsForFollowedUsers(followedUserIds, new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<TimelineEventModel> events = FirebaseService.queryToTimelineEvents(task.getResult());
                            // Sorgudan orderBy'ı kaldırdığımız için zaman damgasına göre azalan sırada sırala (en yeni önce)
                            events.sort((e1, e2) -> Long.compare(e2.timestamp, e1.timestamp));
                            // En son 50 ile sınırla
                            final List<TimelineEventModel> finalEvents = events.size() > 50 
                                ? events.subList(0, 50) 
                                : events;
                            mainHandler.post(() -> callback.onSuccess(finalEvents));
                        } else {
                            mainHandler.post(() -> callback.onError(task.getException() != null ? task.getException().getMessage() : "Failed to get timeline events"));
                        }
                    }
                });
            }

            @Override
            public void onError(String msg) {
                mainHandler.post(() -> callback.onError(msg));
            }
        });
    }

    public void getTimelineEventsForUser(String userId, Callback<List<TimelineEventModel>> callback) {
        final String finalUserId = userId; // İç sınıf için etkin olarak final yap
        firebaseService.getTimelineEventsForUser(finalUserId, new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    List<TimelineEventModel> events = FirebaseService.queryToTimelineEvents(task.getResult());
                    // Sort by timestamp descending (most recent first) since we removed orderBy from query
                    events.sort((e1, e2) -> Long.compare(e2.timestamp, e1.timestamp));
                    final List<TimelineEventModel> finalEvents = events; // Etkin olarak final yap
                    mainHandler.post(() -> callback.onSuccess(finalEvents));
                } else {
                    mainHandler.post(() -> callback.onError(task.getException() != null ? task.getException().getMessage() : "Failed to get timeline events"));
                }
            }
        });
    }

    // ========== KULLANICI FİLM İŞLEMLERİ (Favoriler/İzlenenler) ==========

    public void addFavoriteMovie(String userId, int movieId, Callback<Boolean> callback) {
        final String finalUserId = userId; // Make effectively final
        final int finalMovieId = movieId; // Make effectively final
        
        // Önce userMovie'nin var olup olmadığını kontrol et
        firebaseService.getUserMovies(finalUserId, new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(Task<QuerySnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    boolean found = false;
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        Integer docMovieId = doc.getLong("movieId").intValue();
                        if (docMovieId == finalMovieId) {
                            found = true;
                            // Mevcut olanı güncelle
                            Boolean isWatched = doc.getBoolean("isWatched");
                            firebaseService.updateUserMovie(finalUserId, finalMovieId, true, 
                                isWatched != null && isWatched, 
                                new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        mainHandler.post(() -> callback.onSuccess(true));
                                    }
                                },
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(Exception e) {
                                        mainHandler.post(() -> callback.onError(e.getMessage()));
                                    }
                                });
                            break;
                        }
                    }
                    if (!found) {
                        // Yeni oluştur
                        firebaseService.addUserMovie(finalUserId, finalMovieId, true, false,
                            new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    mainHandler.post(() -> callback.onSuccess(true));
                                }
                            },
                            new OnFailureListener() {
                                @Override
                                public void onFailure(Exception e) {
                                    mainHandler.post(() -> callback.onError(e.getMessage()));
                                }
                            });
                    }
                } else {
                    // Sorgu başarısız olursa yeni oluştur
                    firebaseService.addUserMovie(finalUserId, finalMovieId, true, false,
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                mainHandler.post(() -> callback.onSuccess(true));
                            }
                        },
                        new OnFailureListener() {
                            @Override
                            public void onFailure(Exception e) {
                                mainHandler.post(() -> callback.onError(e.getMessage()));
                            }
                        });
                }
            }
        });
    }

    public void addWatchedMovie(String userId, int movieId, Callback<Boolean> callback) {
        final String finalUserId = userId; // Make effectively final
        final int finalMovieId = movieId; // Make effectively final
        
        // Önce userMovie'nin var olup olmadığını kontrol et
        firebaseService.getUserMovies(finalUserId, new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(Task<QuerySnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    boolean found = false;
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        Integer docMovieId = doc.getLong("movieId").intValue();
                        if (docMovieId == finalMovieId) {
                            found = true;
                            // Mevcut olanı güncelle
                            Boolean isFavorite = doc.getBoolean("isFavorite");
                            firebaseService.updateUserMovie(finalUserId, finalMovieId, 
                                isFavorite != null && isFavorite, true,
                                new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        mainHandler.post(() -> callback.onSuccess(true));
                                    }
                                },
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(Exception e) {
                                        mainHandler.post(() -> callback.onError(e.getMessage()));
                                    }
                                });
                            break;
                        }
                    }
                    if (!found) {
                        // Yeni oluştur
                        firebaseService.addUserMovie(finalUserId, finalMovieId, false, true,
                            new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    mainHandler.post(() -> callback.onSuccess(true));
                                }
                            },
                            new OnFailureListener() {
                                @Override
                                public void onFailure(Exception e) {
                                    mainHandler.post(() -> callback.onError(e.getMessage()));
                                }
                            });
                    }
                } else {
                    // Sorgu başarısız olursa yeni oluştur
                    firebaseService.addUserMovie(finalUserId, finalMovieId, false, true,
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                mainHandler.post(() -> callback.onSuccess(true));
                            }
                        },
                        new OnFailureListener() {
                            @Override
                            public void onFailure(Exception e) {
                                mainHandler.post(() -> callback.onError(e.getMessage()));
                            }
                        });
                }
            }
        });
    }

    public void getFavoriteMovies(String userId, Callback<List<Integer>> callback) {
        final String finalUserId = userId; // Make effectively final
        firebaseService.getFavoriteMovies(finalUserId, new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    List<Integer> movieIds = new ArrayList<>();
                    if (task.getResult() != null) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Long movieIdLong = doc.getLong("movieId");
                            if (movieIdLong != null) {
                                movieIds.add(movieIdLong.intValue());
                            }
                        }
                    }
                    mainHandler.post(() -> callback.onSuccess(movieIds));
                } else {
                    mainHandler.post(() -> callback.onError(task.getException() != null ? task.getException().getMessage() : "Failed to get favorite movies"));
                }
            }
        });
    }

    public void getWatchedMovies(String userId, Callback<List<Integer>> callback) {
        final String finalUserId = userId; // Make effectively final
        firebaseService.getWatchedMovies(finalUserId, new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    List<Integer> movieIds = new ArrayList<>();
                    if (task.getResult() != null) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Long movieIdLong = doc.getLong("movieId");
                            if (movieIdLong != null) {
                                movieIds.add(movieIdLong.intValue());
                            }
                        }
                    }
                    mainHandler.post(() -> callback.onSuccess(movieIds));
                } else {
                    mainHandler.post(() -> callback.onError(task.getException() != null ? task.getException().getMessage() : "Failed to get watched movies"));
                }
            }
        });
    }

    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String msg);
    }
}
