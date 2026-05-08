DROP DATABASE IF EXISTS scriptly_auth;
DROP DATABASE IF EXISTS scriptly_posts;
DROP DATABASE IF EXISTS scriptly_comments;
DROP DATABASE IF EXISTS scriptly_media;
DROP DATABASE IF EXISTS scriptly_newsletter;
DROP DATABASE IF EXISTS scriptly_notifications;

CREATE DATABASE IF NOT EXISTS scriptly_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS scriptly_posts CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS scriptly_comments CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS scriptly_media CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS scriptly_newsletter CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS scriptly_notifications CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
