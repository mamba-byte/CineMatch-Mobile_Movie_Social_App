# CineMatch – Mobile Movie Social App

## Overview

CineMatch is a production-ready Android social movie discovery app where users can discover movies, follow other users, see friends' recent activity, send direct messages, and interact socially around films. The app integrates with TMDB (The Movie Database) for movie data and uses Firebase for backend services including authentication, real-time data storage, and social features.

## Technologies Used

### Platform & Language
- **Platform**: Android (Java)
- **Min SDK**: 29 (Android 10)
- **Target SDK**: 36 (Android 15)
- **Compile SDK**: 36

### Architecture
- **Pattern**: Single-activity + multiple fragments
- **Main Activity**: `MainActivity` with bottom navigation
- **Fragments**: Home, For You, Timeline, Messages, Profile, Users
- **Activities**: Login, Register, Movie Detail, Chat, Edit Profile, User Detail

### UI Framework
- **Material Design 3**: `Theme.Material3.DayNight.NoActionBar`
- **Custom Dark Theme**: Dark background with accent colors
- **Components**:
  - RecyclerView with custom adapters
  - BottomNavigationView with notification badges
  - CollapsingToolbarLayout
  - CardView-based layouts
  - DialogFragment for followers/following lists
  - TabLayout for Popular/Top Rated tabs

### Backend Services

#### Firebase Services
- **Firebase Authentication**: Email/password authentication with session management
- **Cloud Firestore**: NoSQL database for all user data, messages, timeline events, and social interactions
- **Firebase Analytics**: User analytics and app performance tracking

#### Firebase Collections
1. **`users`**: User profiles (username, displayName, bio, profileImageUrl, passwordHash)
2. **`messages`**: Direct messages between users (fromUserId, toUserId, text, timestamp)
3. **`follows`**: Follow relationships (followerId, followedId)
4. **`timeline_events`**: User activity events (userId, movieId, type, timestamp)
5. **`movies`**: Movie data cached from TMDB (id, title, overview, posterPath, releaseYear, tmdbRating)
6. **`userMovies`**: User movie preferences (userId, movieId, isFavorite, isWatched, timestamp)

#### Firebase Security Rules
- Production-ready security rules in `firestore.rules`
- Users can only create/update their own data
- Messages are readable only by sender and receiver
- Timeline events are readable by authenticated users
- User movies are private to each user

### Networking
- **Retrofit 2.9.0**: HTTP client for TMDB API
- **OkHttp 5.0.0**: HTTP client with logging interceptor
- **Gson**: JSON serialization/deserialization
- **TMDB REST APIs**:
  - `GET /movie/popular` - Popular movies
  - `GET /movie/top_rated` - Top rated movies
  - `GET /search/movie` - Movie search
  - `GET /discover/movie` - Movie recommendations with genre filters
  - `GET /movie/{id}` - Movie details

### Local Persistence
- **Room Database 2.6.1**: Local SQLite database (legacy, being phased out in favor of Firebase)
- **SharedPreferences**: User session management, "Remember Me" preference

### Image Loading
- **Glide 4.16.0**: Image loading and caching for TMDB movie posters
- **Content URI**: Profile images stored as local URIs

### Utilities
- **PasswordUtil**: Password hashing using SHA-256 algorithm for secure authentication. Passwords are never stored in plain text - they are hashed before being saved to Firebase. The `hashPassword()` method uses Java's `MessageDigest` with SHA-256 to create a one-way hash, and `verifyPassword()` compares a plain text password against a stored hash.
- **NotificationBadgeManager**: Manages notification badges on bottom navigation
- **UiAnimations**: Click-scale animations for buttons and icons
- **Constants**: TMDB base URL and API key configuration

## Main Screens & Activities

### Authentication Screens

#### Login Screen
- **Activity**: `LoginActivity`
- **Purpose**: User authentication with username/password
- **Features**:
  - Username and password input fields
  - "Remember Me" checkbox for session persistence
  - Auto-login if "Remember Me" is enabled and session exists
  - Firebase Auth integration
  - Redirects to MainActivity on successful login
- **Key Methods**:
  - `attemptLogin()`: Validates input and initiates login
  - `performLogin()`: Authenticates with Firebase Auth after verifying credentials in Firestore
  - `checkExistingLogin()`: Checks for existing Firebase Auth session on app startup
  - `getCurrentUserId()`: Returns current authenticated user's Firebase UID
  - `isLoggedIn()`: Checks if user is currently logged in
  - `logout()`: Signs out and clears session data

#### Register Screen
- **Activity**: `RegisterActivity`
- **Purpose**: New user registration
- **Features**:
  - Username, display name, password, and bio input
  - "Remember Me" checkbox
  - Creates Firebase Auth account and Firestore user document
  - Auto-login after successful registration

### Main App Screens

#### Main Activity
- **Activity**: `MainActivity`
- **Purpose**: Main container with bottom navigation
- **Features**:
  - Bottom navigation with 5 tabs: Home, For You, Timeline, Messages, Profile
  - Notification badge system for new timeline events and messages
  - Background notification checker (runs every 90 seconds)
  - Lifecycle-aware notification checking (pauses when app is in background)
- **Key Methods**:
  - `checkNotifications()`: Checks for new timeline events and messages
  - `markTimelineViewed()`: Marks timeline as viewed, removes badge
  - `markMessagesViewed()`: Marks messages as viewed, removes badge
  - `startNotificationChecker()`: Starts periodic notification checking
  - `stopNotificationChecker()`: Stops notification checking

#### Home Screen
- **Fragment**: `HomeFragment`
- **Purpose**: Browse popular and top-rated movies
- **Features**:
  - TabLayout with "Popular" and "Top Rated" tabs
  - Search icon that toggles dropdown search bar
  - RecyclerView with endless scroll pagination
  - Movie cards with poster, title, year, and rating
- **Key Methods**:
  - `loadPopularMovies(page)`: Loads popular movies from TMDB
  - `loadTopRatedMovies(page)`: Loads top-rated movies from TMDB
  - `performSearch(query)`: Searches TMDB movie catalog
- **Data Source**: TMDB API via `MovieRepository`

#### For You Screen
- **Fragment**: `ForYouFragment`
- **Purpose**: Personalized movie recommendations
- **Features**:
  - Recommendations based on user's favorite and watched movies
  - Filters out already watched/favorited titles
  - Endless scroll pagination
- **Key Methods**:
  - `loadRecommendationsPage(page)`: Loads recommendation page
  - Uses `SocialRepository.getFavoriteMovies()` and `getWatchedMovies()` to get user preferences
  - Uses `MovieRepository.discoverMovies()` with genre filters
- **Data Source**: TMDB API + Firebase user preferences

#### Timeline Screen
- **Fragment**: `TimelineFragment`
- **Purpose**: Social activity feed from followed users
- **Features**:
  - Shows timeline events from followed users and current user
  - Event types: WATCHED, FAVORITED, RATED
  - Displays movie poster, user name, action type, movie title, timestamp
  - Search icon navigates to Users screen
- **Key Methods**:
  - `loadTimelineEvents()`: Loads timeline events from Firebase
  - `loadMoviesForEvents()`: Fetches movie data for timeline events
- **Data Source**: Firebase `timeline_events` collection

#### Users Screen
- **Fragment**: `UsersFragment`
- **Purpose**: Search and discover users to follow
- **Features**:
  - Search bar to filter users by name/username
  - List of all users (excluding current user)
  - Follow/Unfollow buttons
  - Can be opened from Timeline screen
- **Key Methods**:
  - `loadUsers()`: Loads all users from Firebase
  - `filterUsers(query)`: Filters users by search query
- **Data Source**: Firebase `users` collection

#### Messages Screen
- **Fragment**: `MessagesFragment`
- **Purpose**: List of conversation partners
- **Features**:
  - Shows users with whom current user has conversations
  - Displays user profile information
  - Tapping a user opens ChatActivity
  - Marks messages as viewed when displayed
- **Key Methods**:
  - `loadConversationUsers()`: Loads users with conversations
  - `getConversationUserIds()`: Gets list of user IDs with conversations
- **Data Source**: Firebase `messages` collection

#### Chat Screen
- **Activity**: `ChatActivity`
- **Purpose**: One-on-one direct messaging
- **Features**:
  - Message bubbles (sent/received styling)
  - Message input with send button
  - Real-time message loading
  - Scrolls to bottom when new messages arrive
- **Key Methods**:
  - `loadMessages()`: Loads conversation messages
  - `sendMessage()`: Sends new message to Firebase
- **Data Source**: Firebase `messages` collection

#### Profile Screen
- **Fragment**: `ProfileFragment`
- **Purpose**: Current user's profile and activity
- **Features**:
  - Profile header image
  - Display name, username, bio
  - Followers and following counts (clickable)
  - Edit profile and Edit bio buttons
  - Logout button
  - Recent activity section (last 20 timeline events)
- **Key Methods**:
  - `loadUserProfile()`: Loads current user's profile
  - `loadFollowCounts()`: Loads follower/following counts
  - `loadRecentActivity()`: Loads recent timeline events
  - Opens `FollowersFollowingDialogFragment` when followers/following counts are clicked
- **Data Source**: Firebase `users`, `follows`, `timeline_events` collections

#### User Detail Screen
- **Activity**: `UserDetailActivity`
- **Purpose**: View another user's profile
- **Features**:
  - Similar to ProfileFragment but for other users
  - Follow/Unfollow button
  - User's recent activity
  - Cannot edit (read-only)
- **Data Source**: Firebase `users`, `follows`, `timeline_events` collections

#### Edit Profile Screen
- **Activity**: `EditProfileActivity`
- **Purpose**: Edit current user's profile
- **Features**:
  - Edit display name, bio
  - Change profile picture (image picker)
  - Save changes to Firebase
- **Key Methods**:
  - `updateProfile()`: Updates user profile in Firebase
- **Data Source**: Firebase `users` collection

#### Movie Detail Screen
- **Activity**: `MovieDetailActivity`
- **Purpose**: Movie details and user actions
- **Features**:
  - Movie poster, title, year, rating, overview
  - Favorite and Watched buttons
  - Creates timeline events when user favorites/watches
- **Key Methods**:
  - `loadMovieDetails()`: Loads movie from Firebase or TMDB
  - `toggleFavorite()`: Adds/removes from favorites
  - `toggleWatched()`: Marks as watched/unwatched
- **Data Source**: Firebase `movies` collection or TMDB API

#### Followers/Following Dialog
- **Fragment**: `FollowersFollowingDialogFragment`
- **Purpose**: Display list of followers or following users
- **Features**:
  - Dialog showing followers or following list
  - Follow/Unfollow buttons for each user
  - Opens from ProfileFragment
- **Data Source**: Firebase `follows` collection

## Key Classes & Methods

### FirebaseService
Singleton service for Firebase Firestore operations.

**User Operations**:
- `getUserById(userId, listener)`: Get user by ID
- `getUserByUsername(username, listener)`: Get user by username
- `getAllUsers(listener)`: Get all users
- `createUser(user, success, failure)`: Create new user
- `updateUser(userId, updates, success, failure)`: Update user profile

**Message Operations**:
- `getMessagesFromTo(fromUserId, toUserId, listener)`: Get messages in one direction
- `getMessagesForReceiver(userId, listener)`: Get messages received by user
- `getMessagesForReceiverLimited(userId, listener)`: Get newest received message (for notifications)
- `getMessagesForSender(userId, listener)`: Get messages sent by user
- `getMessagesForSenderLimited(userId, listener)`: Get newest sent message (for notifications)
- `sendMessage(message, success, failure)`: Send new message

**Follow Operations**:
- `getFollows(followerId, listener)`: Get users followed by user
- `getFollowers(followedId, listener)`: Get followers of user
- `followUser(followerId, followedId, success, failure)`: Create follow relationship
- `unfollowUser(followerId, followedId, success, failure)`: Remove follow relationship

**Timeline Operations**:
- `getTimelineEventsForUser(userId, listener)`: Get timeline events for user
- `getTimelineEventsForFollowedUsers(userIds, listener)`: Get timeline events for multiple users
- `getNewestTimelineEventForUsers(userIds, listener)`: Get newest timeline event (for notifications)
- `createTimelineEvent(event, success, failure)`: Create new timeline event

**Movie Operations**:
- `getMovieById(movieId, listener)`: Get movie by TMDB ID
- `getAllMovies(listener)`: Get all cached movies
- `createMovie(movie, success, failure)`: Cache movie in Firebase

**User Movie Operations**:
- `getUserMovies(userId, listener)`: Get user's movie preferences
- `getFavoriteMovies(userId, listener)`: Get user's favorite movies
- `getWatchedMovies(userId, listener)`: Get user's watched movies
- `addUserMovie(userId, movieId, isFavorite, isWatched, success, failure)`: Add/update user movie preference

### SocialRepository
Repository layer for social features, wraps FirebaseService.

**User Methods**:
- `getUserById(userId, callback)`: Get user profile
- `getAllUsers(callback)`: Get all users

**Follow Methods**:
- `followUser(followerId, followedId, callback)`: Follow a user
- `unfollowUser(followerId, followedId, callback)`: Unfollow a user
- `getFollows(userId, callback)`: Get users followed by user
- `getFollowers(userId, callback)`: Get followers of user

**Message Methods**:
- `getConversation(u1, u2, callback)`: Get conversation between two users
- `getConversationUserIds(userId, callback)`: Get list of user IDs with conversations
- `sendMessage(fromUserId, toUserId, text, callback)`: Send message

**Timeline Methods**:
- `getTimelineEventsForUser(userId, callback)`: Get user's timeline events
- `getTimelineEventsForFollowedUsers(userId, callback)`: Get timeline events from followed users
- `addTimelineEvent(userId, movieId, type, callback)`: Add timeline event

**User Movie Methods**:
- `getFavoriteMovies(userId, callback)`: Get favorite movie IDs
- `getWatchedMovies(userId, callback)`: Get watched movie IDs
- `addFavoriteMovie(userId, movieId, callback)`: Add to favorites
- `addWatchedMovie(userId, movieId, callback)`: Mark as watched

### MovieRepository
Repository for movie data from TMDB API.

**Methods**:
- `loadPopularMovies(page, callback)`: Load popular movies
- `loadTopRatedMovies(page, callback)`: Load top-rated movies
- `searchMovies(query, page, callback)`: Search movies
- `discoverMovies(genreIds, page, callback)`: Discover movies by genres
- `getMovieById(movieId, callback)`: Get movie details

### PasswordUtil
Utility class for secure password hashing using SHA-256 algorithm. This ensures user passwords are never stored in plain text, providing essential security for user authentication.

**Security Features**:
- Uses SHA-256 (Secure Hash Algorithm 256-bit) for one-way password hashing
- Passwords are hashed before being stored in Firebase Firestore
- Original passwords cannot be recovered from hashes (one-way function)
- Hash comparison is used for password verification during login

**Methods**:
- `hashPassword(password)`: Hashes a plain text password using SHA-256 algorithm
  - **Purpose**: Convert plain text password to secure hash for storage
  - **Input**: Plain text password (String)
  - **Process**: 
    - Uses Java's `MessageDigest.getInstance("SHA-256")` to create the hash
    - Processes password bytes through SHA-256 algorithm
    - Converts the byte array hash to a hexadecimal string representation
  - **Output**: SHA-256 hash as a hexadecimal string (64 characters)
  - **Error Handling**: Throws `RuntimeException` if SHA-256 algorithm is not available
  - **Security**: One-way function - original password cannot be derived from hash
  
- `verifyPassword(password, hash)`: Verifies if a plain text password matches a stored hash
  - **Purpose**: Authenticate user by comparing input password with stored hash
  - **Input**: Plain text password (String) and stored hash (String)
  - **Process**: 
    - Hashes the plain text password using `hashPassword()` method
    - Compares the generated hash with the stored hash using string equality
  - **Output**: Returns `true` if passwords match, `false` otherwise
  - **Security**: Never compares plain text passwords directly - always compares hashes

**Usage Example**:
```java
// During registration
String passwordHash = PasswordUtil.hashPassword("userPassword123");
// Store passwordHash in Firebase (never store plain text)
// Example hash output: "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3"

// During login
String inputPassword = "userPassword123";
String storedHash = user.passwordHash;
boolean isValid = PasswordUtil.verifyPassword(inputPassword, storedHash);
if (isValid) {
    // Password is correct, proceed with authentication
} else {
    // Password is incorrect, deny access
}
```

**Why SHA-256?**:
- SHA-256 is a cryptographic hash function that produces a fixed-size (256-bit) output
- It's a one-way function, meaning it's computationally infeasible to reverse the hash to get the original password
- Even small changes in the input password produce completely different hash outputs
- This ensures that if the database is compromised, attackers cannot easily recover user passwords
- The hash is deterministic - the same password always produces the same hash, enabling password verification

### NotificationBadgeManager
Manages notification badges on bottom navigation.

**Methods**:
- `checkTimelineNotifications(userId, newestTimestamp)`: Check for new timeline events
- `checkMessageNotifications(userId, newestTimestamp)`: Check for new messages
- `markTimelineViewed(userId)`: Mark timeline as viewed
- `markMessagesViewed(userId)`: Mark messages as viewed
- `showTimelineBadge()`: Show badge on timeline tab
- `hideTimelineBadge()`: Hide badge on timeline tab
- `showMessagesBadge()`: Show badge on messages tab
- `hideMessagesBadge()`: Hide badge on messages tab

## Firebase Security Rules

The app uses production-ready Firestore security rules:

- **Users**: Anyone can read profiles, users can only create/update their own profile
- **Messages**: Users can only read messages where they are sender or receiver
- **Follows**: Authenticated users can read follows, users can only create/delete their own follows
- **Timeline Events**: Authenticated users can read, users can only create/update/delete their own events
- **User Movies**: Users can only read/create/update/delete their own movie preferences
- **Movies**: Anyone can read, authenticated users can create/update

## Data Flow

1. **Authentication Flow**:
   - User enters username/password → Verify in Firestore → Authenticate with Firebase Auth → Save session → Navigate to MainActivity

2. **Movie Discovery Flow**:
   - User browses → TMDB API call → Display results → User interacts → Save to Firebase (favorites/watched) → Create timeline event

3. **Social Interaction Flow**:
   - User follows another user → Create follow document in Firebase → Timeline shows their events
   - User sends message → Create message document in Firebase → Recipient sees notification badge

4. **Notification Flow**:
   - Background checker runs every 90 seconds → Query newest timeline events/messages → Compare with last viewed timestamp → Show/hide badges

## Key Features

- ✅ User authentication with Firebase Auth
- ✅ Session management with "Remember Me" option
- ✅ Real-time social timeline
- ✅ Direct messaging between users
- ✅ Follow/unfollow system
- ✅ Movie discovery and recommendations
- ✅ Favorite and watched movie tracking
- ✅ Notification badges for new activity
- ✅ Profile management
- ✅ Production-ready Firebase security rules
- ✅ TMDB integration for movie data
- ✅ Image loading and caching with Glide

## Project Structure

```
app/src/main/java/com/example/mobilprogfallproj/
├── ui/
│   ├── login/          # LoginActivity, RegisterActivity
│   ├── main/            # MainActivity, NotificationBadgeManager
│   ├── home/            # HomeFragment
│   ├── foryou/          # ForYouFragment
│   ├── timeline/        # TimelineFragment
│   ├── dm/              # MessagesFragment, ChatActivity
│   ├── profile/         # ProfileFragment, UserDetailActivity, EditProfileActivity, FollowersFollowingDialogFragment
│   ├── social/          # UsersFragment
│   └── detail/          # MovieDetailActivity
├── data/
│   ├── firebase/        # FirebaseService, models
│   ├── repo/            # SocialRepository, MovieRepository
│   └── db/              # Room entities and DAOs (legacy)
└── util/                # PasswordUtil, UiAnimations, Constants
```

## Setup Instructions

1. **Firebase Setup**:
   - Create Firebase project at https://console.firebase.google.com
   - Enable Authentication (Email/Password)
   - Enable Firestore Database
   - Download `google-services.json` and place in `app/` directory
   - Deploy security rules from `firestore.rules`

2. **TMDB API**:
   - Get API key from https://www.themoviedb.org/settings/api
   - Add to `Constants.java` or use environment variable

3. **Build**:
   - Sync Gradle files
   - Build and run on Android device/emulator (min SDK 29)

## Production Readiness

- ✅ Firebase security rules implemented
- ✅ User authentication and session management
- ✅ Error handling and validation
- ✅ Performance optimizations (lightweight queries for notifications)
- ✅ Lifecycle-aware components (notification checker pauses when app is in background)
- ✅ No automatic data seeding (production-ready)
- ✅ Proper null checks and error handling

---

# CineMatch – Mobil Film Sosyal Uygulaması (Türkçe)

## Genel Bakış

CineMatch, kullanıcıların filmleri keşfedebileceği, diğer kullanıcıları takip edebileceği, arkadaşlarının son etkinliklerini görebileceği, doğrudan mesaj gönderebileceği ve filmler etrafında sosyal olarak etkileşim kurabileceği, üretime hazır bir Android sosyal film keşif uygulamasıdır. Uygulama, film verileri için TMDB (The Movie Database) ile entegre olur ve kimlik doğrulama, gerçek zamanlı veri depolama ve sosyal özellikler dahil olmak üzere arka plan hizmetleri için Firebase kullanır.

## Kullanılan Teknolojiler

### Platform ve Dil
- **Platform**: Android (Java)
- **Min SDK**: 29 (Android 10)
- **Hedef SDK**: 36 (Android 15)
- **Derleme SDK**: 36

### Mimari
- **Desen**: Tek aktivite + çoklu fragment
- **Ana Aktivite**: Alt navigasyonlu `MainActivity`
- **Fragment'ler**: Ana Sayfa, Senin İçin, Zaman Çizelgesi, Mesajlar, Profil, Kullanıcılar
- **Aktiviteler**: Giriş, Kayıt, Film Detayı, Sohbet, Profil Düzenle, Kullanıcı Detayı

### UI Çerçevesi
- **Material Design 3**: `Theme.Material3.DayNight.NoActionBar`
- **Özel Karanlık Tema**: Vurgu renkleriyle karanlık arka plan
- **Bileşenler**:
  - Özel adaptörlerle RecyclerView
  - Bildirim rozetleriyle BottomNavigationView
  - CollapsingToolbarLayout
  - CardView tabanlı düzenler
  - Takipçi/takip edilen listeleri için DialogFragment
  - Popüler/En İyi Puanlı sekmeleri için TabLayout

### Arka Plan Hizmetleri

#### Firebase Hizmetleri
- **Firebase Authentication**: Oturum yönetimiyle e-posta/şifre kimlik doğrulaması
- **Cloud Firestore**: Tüm kullanıcı verileri, mesajlar, zaman çizelgesi etkinlikleri ve sosyal etkileşimler için NoSQL veritabanı
- **Firebase Analytics**: Kullanıcı analitiği ve uygulama performans takibi

#### Firebase Koleksiyonları
1. **`users`**: Kullanıcı profilleri (username, displayName, bio, profileImageUrl, passwordHash)
2. **`messages`**: Kullanıcılar arası doğrudan mesajlar (fromUserId, toUserId, text, timestamp)
3. **`follows`**: Takip ilişkileri (followerId, followedId)
4. **`timeline_events`**: Kullanıcı etkinlik olayları (userId, movieId, type, timestamp)
5. **`movies`**: TMDB'den önbelleğe alınan film verileri (id, title, overview, posterPath, releaseYear, tmdbRating)
6. **`userMovies`**: Kullanıcı film tercihleri (userId, movieId, isFavorite, isWatched, timestamp)

#### Firebase Güvenlik Kuralları
- `firestore.rules` içinde üretime hazır güvenlik kuralları
- Kullanıcılar yalnızca kendi verilerini oluşturabilir/güncelleyebilir
- Mesajlar yalnızca gönderen ve alıcı tarafından okunabilir
- Zaman çizelgesi etkinlikleri kimlik doğrulaması yapılmış kullanıcılar tarafından okunabilir
- Kullanıcı filmleri her kullanıcı için özeldir

### Ağ İşlemleri
- **Retrofit 2.9.0**: TMDB API için HTTP istemcisi
- **OkHttp 5.0.0**: Günlük kaydı interceptor'ü ile HTTP istemcisi
- **Gson**: JSON serileştirme/serileştirme kaldırma
- **TMDB REST API'leri**:
  - `GET /movie/popular` - Popüler filmler
  - `GET /movie/top_rated` - En iyi puanlı filmler
  - `GET /search/movie` - Film arama
  - `GET /discover/movie` - Tür filtreleriyle film önerileri
  - `GET /movie/{id}` - Film detayları

### Yerel Kalıcılık
- **Room Database 2.6.1**: Yerel SQLite veritabanı (eski, Firebase lehine aşamalı olarak kaldırılıyor)
- **SharedPreferences**: Kullanıcı oturum yönetimi, "Beni Hatırla" tercihi

### Görüntü Yükleme
- **Glide 4.16.0**: TMDB film posterleri için görüntü yükleme ve önbelleğe alma
- **Content URI**: Profil görüntüleri yerel URI'ler olarak saklanır

### Yardımcı Programlar
- **PasswordUtil**: Güvenli kimlik doğrulama için şifre hash'leme
- **NotificationBadgeManager**: Alt navigasyondaki bildirim rozetlerini yönetir
- **UiAnimations**: Butonlar ve simgeler için tıklama-ölçek animasyonları
- **Constants**: TMDB temel URL'si ve API anahtarı yapılandırması

## Ana Ekranlar ve Aktiviteler

### Kimlik Doğrulama Ekranları

#### Giriş Ekranı
- **Aktivite**: `LoginActivity`
- **Amaç**: Kullanıcı adı/şifre ile kullanıcı kimlik doğrulaması
- **Özellikler**:
  - Kullanıcı adı ve şifre giriş alanları
  - Oturum kalıcılığı için "Beni Hatırla" onay kutusu
  - "Beni Hatırla" etkinse ve oturum varsa otomatik giriş
  - Firebase Auth entegrasyonu
  - Başarılı girişte MainActivity'ye yönlendirme
- **Ana Yöntemler**:
  - `attemptLogin()`: Girdiyi doğrular ve girişi başlatır
  - `performLogin()`: Firestore'da kimlik bilgilerini doğruladıktan sonra Firebase Auth ile kimlik doğrular
  - `checkExistingLogin()`: Uygulama başlangıcında mevcut Firebase Auth oturumunu kontrol eder
  - `getCurrentUserId()`: Mevcut kimlik doğrulanmış kullanıcının Firebase UID'sini döndürür
  - `isLoggedIn()`: Kullanıcının şu anda giriş yapıp yapmadığını kontrol eder
  - `logout()`: Çıkış yapar ve oturum verilerini temizler

#### Kayıt Ekranı
- **Aktivite**: `RegisterActivity`
- **Amaç**: Yeni kullanıcı kaydı
- **Özellikler**:
  - Kullanıcı adı, görünen ad, şifre ve biyografi girişi
  - "Beni Hatırla" onay kutusu
  - Firebase Auth hesabı ve Firestore kullanıcı belgesi oluşturur
  - Başarılı kayıttan sonra otomatik giriş

### Ana Uygulama Ekranları

#### Ana Aktivite
- **Aktivite**: `MainActivity`
- **Amaç**: Alt navigasyonlu ana konteyner
- **Özellikler**:
  - 5 sekme ile alt navigasyon: Ana Sayfa, Senin İçin, Zaman Çizelgesi, Mesajlar, Profil
  - Yeni zaman çizelgesi etkinlikleri ve mesajlar için bildirim rozeti sistemi
  - Arka plan bildirim kontrolörü (90 saniyede bir çalışır)
  - Yaşam döngüsü farkında bildirim kontrolü (uygulama arka plandayken duraklar)
- **Ana Yöntemler**:
  - `checkNotifications()`: Yeni zaman çizelgesi etkinliklerini ve mesajları kontrol eder
  - `markTimelineViewed()`: Zaman çizelgesini görüntülendi olarak işaretler, rozeti kaldırır
  - `markMessagesViewed()`: Mesajları görüntülendi olarak işaretler, rozeti kaldırır
  - `startNotificationChecker()`: Periyodik bildirim kontrolünü başlatır
  - `stopNotificationChecker()`: Bildirim kontrolünü durdurur

#### Ana Sayfa Ekranı
- **Fragment**: `HomeFragment`
- **Amaç**: Popüler ve en iyi puanlı filmleri gözden geçir
- **Özellikler**:
  - "Popüler" ve "En İyi Puanlı" sekmeleriyle TabLayout
  - Açılır arama çubuğunu açan arama simgesi
  - Sonsuz kaydırma sayfalama ile RecyclerView
  - Poster, başlık, yıl ve puan ile film kartları
- **Ana Yöntemler**:
  - `loadPopularMovies(page)`: TMDB'den popüler filmleri yükler
  - `loadTopRatedMovies(page)`: TMDB'den en iyi puanlı filmleri yükler
  - `performSearch(query)`: TMDB film kataloğunu arar
- **Veri Kaynağı**: `MovieRepository` üzerinden TMDB API

#### Senin İçin Ekranı
- **Fragment**: `ForYouFragment`
- **Amaç**: Kişiselleştirilmiş film önerileri
- **Özellikler**:
  - Kullanıcının favori ve izlediği filmlerine dayalı öneriler
  - Zaten izlenen/favorilere eklenen başlıkları filtreler
  - Sonsuz kaydırma sayfalama
- **Ana Yöntemler**:
  - `loadRecommendationsPage(page)`: Öneri sayfasını yükler
  - Kullanıcı tercihlerini almak için `SocialRepository.getFavoriteMovies()` ve `getWatchedMovies()` kullanır
  - Tür filtreleriyle `MovieRepository.discoverMovies()` kullanır
- **Veri Kaynağı**: TMDB API + Firebase kullanıcı tercihleri

#### Zaman Çizelgesi Ekranı
- **Fragment**: `TimelineFragment`
- **Amaç**: Takip edilen kullanıcılardan sosyal etkinlik akışı
- **Özellikler**:
  - Takip edilen kullanıcılardan ve mevcut kullanıcıdan zaman çizelgesi etkinliklerini gösterir
  - Etkinlik türleri: İZLENDİ, FAVORİLERE EKLENDİ, PUAN VERİLDİ
  - Film posterini, kullanıcı adını, eylem türünü, film başlığını, zaman damgasını gösterir
  - Arama simgesi Kullanıcılar ekranına gider
- **Ana Yöntemler**:
  - `loadTimelineEvents()`: Firebase'den zaman çizelgesi etkinliklerini yükler
  - `loadMoviesForEvents()`: Zaman çizelgesi etkinlikleri için film verilerini getirir
- **Veri Kaynağı**: Firebase `timeline_events` koleksiyonu

#### Kullanıcılar Ekranı
- **Fragment**: `UsersFragment`
- **Amaç**: Takip etmek için kullanıcıları ara ve keşfet
- **Özellikler**:
  - Kullanıcıları ad/kullanıcı adına göre filtrelemek için arama çubuğu
  - Tüm kullanıcıların listesi (mevcut kullanıcı hariç)
  - Takip Et/Takipten Çık butonları
  - Zaman Çizelgesi ekranından açılabilir
- **Ana Yöntemler**:
  - `loadUsers()`: Firebase'den tüm kullanıcıları yükler
  - `filterUsers(query)`: Kullanıcıları arama sorgusuna göre filtreler
- **Veri Kaynağı**: Firebase `users` koleksiyonu

#### Mesajlar Ekranı
- **Fragment**: `MessagesFragment`
- **Amaç**: Konuşma ortaklarının listesi
- **Özellikler**:
  - Mevcut kullanıcının konuşmaları olan kullanıcıları gösterir
  - Kullanıcı profil bilgilerini gösterir
  - Bir kullanıcıya dokunmak ChatActivity'yi açar
  - Görüntülendiğinde mesajları görüntülendi olarak işaretler
- **Ana Yöntemler**:
  - `loadConversationUsers()`: Konuşmaları olan kullanıcıları yükler
  - `getConversationUserIds()`: Konuşmaları olan kullanıcı ID'lerinin listesini alır
- **Veri Kaynağı**: Firebase `messages` koleksiyonu

#### Sohbet Ekranı
- **Aktivite**: `ChatActivity`
- **Amaç**: Bire bir doğrudan mesajlaşma
- **Özellikler**:
  - Mesaj baloncukları (gönderilen/alınan stillendirme)
  - Gönder butonuyla mesaj girişi
  - Gerçek zamanlı mesaj yükleme
  - Yeni mesajlar geldiğinde alta kaydırır
- **Ana Yöntemler**:
  - `loadMessages()`: Konuşma mesajlarını yükler
  - `sendMessage()`: Firebase'e yeni mesaj gönderir
- **Veri Kaynağı**: Firebase `messages` koleksiyonu

#### Profil Ekranı
- **Fragment**: `ProfileFragment`
- **Amaç**: Mevcut kullanıcının profili ve etkinliği
- **Özellikler**:
  - Profil başlık görüntüsü
  - Görünen ad, kullanıcı adı, biyografi
  - Takipçi ve takip edilen sayıları (tıklanabilir)
  - Profil düzenle ve Biyografi düzenle butonları
  - Çıkış butonu
  - Son etkinlik bölümü (son 20 zaman çizelgesi etkinliği)
- **Ana Yöntemler**:
  - `loadUserProfile()`: Mevcut kullanıcının profilini yükler
  - `loadFollowCounts()`: Takipçi/takip edilen sayılarını yükler
  - `loadRecentActivity()`: Son zaman çizelgesi etkinliklerini yükler
  - Takipçi/takip edilen sayılarına tıklandığında `FollowersFollowingDialogFragment`'i açar
- **Veri Kaynağı**: Firebase `users`, `follows`, `timeline_events` koleksiyonları

#### Kullanıcı Detay Ekranı
- **Aktivite**: `UserDetailActivity`
- **Amaç**: Başka bir kullanıcının profilini görüntüle
- **Özellikler**:
  - ProfileFragment'e benzer ancak diğer kullanıcılar için
  - Takip Et/Takipten Çık butonu
  - Kullanıcının son etkinliği
  - Düzenlenemez (salt okunur)
- **Veri Kaynağı**: Firebase `users`, `follows`, `timeline_events` koleksiyonları

#### Profil Düzenle Ekranı
- **Aktivite**: `EditProfileActivity`
- **Amaç**: Mevcut kullanıcının profilini düzenle
- **Özellikler**:
  - Görünen ad, biyografi düzenle
  - Profil resmini değiştir (görüntü seçici)
  - Değişiklikleri Firebase'e kaydet
- **Ana Yöntemler**:
  - `updateProfile()`: Firebase'de kullanıcı profilini günceller
- **Veri Kaynağı**: Firebase `users` koleksiyonu

#### Film Detay Ekranı
- **Aktivite**: `MovieDetailActivity`
- **Amaç**: Film detayları ve kullanıcı eylemleri
- **Özellikler**:
  - Film poster, başlık, yıl, puan, özet metni
  - Favori ve İzlendi butonları
  - Kullanıcı favorilere eklediğinde/izlediğinde zaman çizelgesi etkinlikleri oluşturur
- **Ana Yöntemler**:
  - `loadMovieDetails()`: Film'i Firebase'den veya TMDB'den yükler
  - `toggleFavorite()`: Favorilere ekler/kaldırır
  - `toggleWatched()`: İzlendi/izlenmedi olarak işaretler
- **Veri Kaynağı**: Firebase `movies` koleksiyonu veya TMDB API

#### Takipçiler/Takip Edilenler Diyalogu
- **Fragment**: `FollowersFollowingDialogFragment`
- **Amaç**: Takipçi veya takip edilen kullanıcıların listesini göster
- **Özellikler**:
  - Takipçi veya takip edilen listesini gösteren diyalog
  - Her kullanıcı için Takip Et/Takipten Çık butonları
  - ProfileFragment'ten açılır
- **Veri Kaynağı**: Firebase `follows` koleksiyonu

## Ana Sınıflar ve Yöntemler

### PasswordUtil
SHA-256 algoritması kullanarak güvenli şifre hash'leme için yardımcı sınıf. Bu, kullanıcı şifrelerinin asla düz metin olarak saklanmamasını sağlar ve kullanıcı kimlik doğrulaması için temel güvenlik sağlar.

**Güvenlik Özellikleri**:
- Tek yönlü şifre hash'leme için SHA-256 (Güvenli Hash Algoritması 256-bit) kullanır
- Şifreler Firebase Firestore'a kaydedilmeden önce hash'lenir
- Orijinal şifreler hash'lerden geri kazanılamaz (tek yönlü fonksiyon)
- Giriş sırasında şifre doğrulaması için hash karşılaştırması kullanılır
- Şifrelerin asla düz metin formatında saklanmamasını sağlayarak kullanıcı güvenliği sağlar

**Yöntemler**:
- `hashPassword(password)`: SHA-256 algoritması kullanarak düz metin şifreyi hash'ler
  - **Amaç**: Düz metin şifreyi saklama için güvenli hash'e dönüştür
  - **Girdi**: Düz metin şifre (String)
  - **İşlem**: 
    - Hash oluşturmak için Java'nın `MessageDigest.getInstance("SHA-256")` kullanır
    - Şifre byte'larını SHA-256 algoritmasından geçirir
    - Byte dizisi hash'ini onaltılık string temsiline dönüştürür
  - **Çıktı**: Onaltılık string olarak SHA-256 hash'i (64 karakter)
  - **Hata İşleme**: SHA-256 algoritması mevcut değilse `RuntimeException` fırlatır
  - **Güvenlik**: Tek yönlü fonksiyon - orijinal şifre hash'den türetilemez
  
- `verifyPassword(password, hash)`: Düz metin şifrenin saklanan hash ile eşleşip eşleşmediğini doğrular
  - **Amaç**: Saklanan hash ile girdi şifresini karşılaştırarak kullanıcıyı kimlik doğrula
  - **Girdi**: Düz metin şifre (String) ve saklanan hash (String)
  - **İşlem**: 
    - Düz metin şifreyi `hashPassword()` yöntemini kullanarak hash'ler
    - Oluşturulan hash'i saklanan hash ile string eşitliği kullanarak karşılaştırır
  - **Çıktı**: Şifreler eşleşirse `true`, aksi halde `false` döndürür
  - **Güvenlik**: Asla düz metin şifreleri doğrudan karşılaştırmaz - her zaman hash'leri karşılaştırır

**Kullanım Örneği**:
```java
// Kayıt sırasında
String passwordHash = PasswordUtil.hashPassword("userPassword123");
// passwordHash'i Firebase'e kaydet (asla düz metin saklama)
// Örnek hash çıktısı: "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3"

// Giriş sırasında
String inputPassword = "userPassword123";
String storedHash = user.passwordHash;
boolean isValid = PasswordUtil.verifyPassword(inputPassword, storedHash);
if (isValid) {
    // Şifre doğru, kimlik doğrulamaya devam et
} else {
    // Şifre yanlış, erişimi reddet
}
```

**Neden SHA-256?**:
- SHA-256, sabit boyutlu (256-bit) çıktı üreten kriptografik bir hash fonksiyonudur
- Tek yönlü bir fonksiyondur, yani hash'i tersine çevirerek orijinal şifreyi elde etmek hesaplama açısından olanaksızdır
- Girdi şifresindeki küçük değişiklikler bile tamamen farklı hash çıktıları üretir
- Bu, veritabanı ele geçirilse bile saldırganların kullanıcı şifrelerini kolayca geri kazanamayacağını garanti eder
- Hash deterministiktir - aynı şifre her zaman aynı hash'i üretir, bu da şifre doğrulamasını mümkün kılar

## Kurulum Talimatları

1. **Firebase Kurulumu**:
   - https://console.firebase.google.com adresinde Firebase projesi oluşturun
   - Authentication'ı etkinleştirin (E-posta/Şifre)
   - Firestore Database'i etkinleştirin
   - `google-services.json` dosyasını indirin ve `app/` dizinine yerleştirin
   - `firestore.rules` dosyasından güvenlik kurallarını dağıtın

2. **TMDB API**:
   - https://www.themoviedb.org/settings/api adresinden API anahtarı alın
   - `Constants.java` dosyasına ekleyin veya ortam değişkeni kullanın

3. **Derleme**:
   - Gradle dosyalarını senkronize edin
   - Android cihaz/emülatörde derleyin ve çalıştırın (min SDK 29)

## Üretime Hazırlık

- ✅ Firebase güvenlik kuralları uygulandı
- ✅ Kullanıcı kimlik doğrulaması ve oturum yönetimi
- ✅ Hata işleme ve doğrulama
- ✅ Performans optimizasyonları (bildirimler için hafif sorgular)
- ✅ Yaşam döngüsü farkında bileşenler (bildirim kontrolörü uygulama arka plandayken duraklar)
- ✅ Otomatik veri tohumlama yok (üretime hazır)
- ✅ Uygun null kontrolleri ve hata işleme
