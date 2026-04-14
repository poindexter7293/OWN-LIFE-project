-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: ls-4968413b922797c9b85ff5fb698072c071b6a562.czgiqoag813n.ap-northeast-2.rds.amazonaws.com    Database: ownlife_db
-- ------------------------------------------------------
-- Server version	8.4.8

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ '';

--
-- Table structure for table `social_account`
--

DROP TABLE IF EXISTS `social_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `social_account` (
  `social_account_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `member_id` bigint unsigned NOT NULL,
  `provider` enum('NAVER','KAKAO','GOOGLE') NOT NULL,
  `provider_user_id` varchar(100) NOT NULL,
  `provider_email` varchar(100) DEFAULT NULL,
  `provider_name` varchar(100) DEFAULT NULL,
  `profile_image_url` varchar(500) DEFAULT NULL,
  `connected_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_login_at` datetime DEFAULT NULL,
  PRIMARY KEY (`social_account_id`),
  UNIQUE KEY `uk_social_provider_user` (`provider`,`provider_user_id`),
  KEY `idx_social_member` (`member_id`),
  CONSTRAINT `fk_social_account_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `social_account`
--

LOCK TABLES `social_account` WRITE;
/*!40000 ALTER TABLE `social_account` DISABLE KEYS */;
INSERT INTO `social_account` VALUES (7,9,'KAKAO','4826936163','112tkwk@naver.com','건형','http://k.kakaocdn.net/dn/8zzmV/btsJEO1V4gK/ZhYJosMVyQkUTytoMEKT9k/img_640x640.jpg','2026-04-02 18:07:08','2026-04-09 17:57:22'),(8,10,'GOOGLE','109694647677054937045','poindexter7293@gmail.com','대깨히','https://lh3.googleusercontent.com/a/ACg8ocJCCQZpUqIxfIPKe-9AB1swSWX0-do5b1ESno7TwY-UYzm81A=s96-c','2026-04-02 18:28:55','2026-04-14 11:27:43'),(9,11,'KAKAO','4826957129','kori6314@naver.com','배현우','http://img1.kakaocdn.net/thumb/R640x640.q70/?fname=http://t1.kakaocdn.net/account_images/default_profile.jpeg','2026-04-03 17:44:16','2026-04-08 15:28:06'),(17,15,'NAVER','vdTnoE_JSl27W-QJKev8uBvOIiVAIqs0N5QM6RV-20g',NULL,NULL,NULL,'2026-04-08 11:28:53','2026-04-08 15:28:09');
/*!40000 ALTER TABLE `social_account` ENABLE KEYS */;
UNLOCK TABLES;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-14 11:56:35
