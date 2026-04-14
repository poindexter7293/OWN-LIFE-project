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
-- Table structure for table `member`
--

DROP TABLE IF EXISTS `member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member` (
  `member_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(50) DEFAULT NULL,
  `password_hash` varchar(255) DEFAULT NULL,
  `nickname` varchar(50) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `gender` enum('M','F','OTHER') DEFAULT NULL,
  `birth_date` date DEFAULT NULL,
  `height_cm` decimal(5,2) DEFAULT NULL,
  `weight_kg` decimal(5,2) DEFAULT NULL,
  `goal_eat_kcal` int DEFAULT NULL,
  `goal_burned_kcal` int DEFAULT NULL,
  `goal_weight` decimal(5,2) DEFAULT NULL,
  `role` enum('USER','ADMIN') NOT NULL DEFAULT 'USER',
  `status` enum('ACTIVE','INACTIVE','SUSPENDED','DELETED') NOT NULL DEFAULT 'ACTIVE',
  `login_type` enum('LOCAL','NAVER','KAKAO','GOOGLE') NOT NULL DEFAULT 'LOCAL',
  `social_provider` varchar(20) DEFAULT NULL,
  `social_provider_id` varchar(100) DEFAULT NULL,
  `profile_image_url` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `uk_member_username` (`username`),
  UNIQUE KEY `uk_member_email` (`email`),
  UNIQUE KEY `uk_member_social` (`social_provider`,`social_provider_id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `member`
--

LOCK TABLES `member` WRITE;
/*!40000 ALTER TABLE `member` DISABLE KEYS */;
INSERT INTO `member` VALUES (2,'admin','pbkdf2$65536$DNnEKOKBRYvUrSzOw27NMg==$w55DBWxVj+ldidAfpQ8mfau81kRR8W9IOMr+pnAeBSo=','관리자','admin@test.com','M','1997-06-30',182.00,70.00,NULL,NULL,NULL,'ADMIN','ACTIVE','LOCAL',NULL,NULL,NULL,'2026-03-31 08:06:15','2026-03-31 08:06:43'),(3,'bbbb','pbkdf2$65536$/n2KF3QqEYupeYz0SdXmPA==$iIxmNziESv/woZQrsGU/uXzzB/fxeJ2NdeBYsS9dwzc=','비비비비','bbbbb@test.com','M','2001-10-25',177.00,75.00,1500,800,70.00,'USER','ACTIVE','LOCAL',NULL,NULL,NULL,'2026-03-31 08:15:51','2026-04-02 12:05:26'),(9,'kakao_4826936163',NULL,'건형','112tkwk@naver.com','M','2002-02-04',168.00,61.00,1700,500,62.00,'USER','ACTIVE','KAKAO','KAKAO','4826936163','http://k.kakaocdn.net/dn/8zzmV/btsJEO1V4gK/ZhYJosMVyQkUTytoMEKT9k/img_640x640.jpg','2026-04-02 18:07:08','2026-04-09 17:58:39'),(10,'google_109694647677054937045',NULL,'대깨히','poindexter7293@gmail.com','M','2026-03-31',170.00,64.00,1000,1000,65.00,'USER','ACTIVE','GOOGLE','GOOGLE','109694647677054937045','https://lh3.googleusercontent.com/a/ACg8ocJCCQZpUqIxfIPKe-9AB1swSWX0-do5b1ESno7TwY-UYzm81A=s96-c','2026-04-02 18:28:55','2026-04-13 03:15:47'),(11,'kakao_4826957129',NULL,'엄준식','kori6314@naver.com','M','1998-07-21',178.00,100.00,NULL,NULL,90.00,'USER','ACTIVE','KAKAO','KAKAO','4826957129','http://img1.kakaocdn.net/thumb/R640x640.q70/?fname=http://t1.kakaocdn.net/account_images/default_profile.jpeg','2026-04-03 17:44:16','2026-04-08 11:25:56'),(14,'deleted_14',NULL,'탈퇴한 회원 #14',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'USER','DELETED','LOCAL',NULL,NULL,NULL,'2026-04-07 15:01:06','2026-04-07 15:02:20'),(15,'naver_vdtnoejsl27wqjkev8ubvoiivaiqs0n5qm6rv20g',NULL,'네이버테스트','henry2506@naver.com','M',NULL,178.00,70.00,1000,NULL,NULL,'USER','ACTIVE','NAVER','NAVER','vdTnoE_JSl27W-QJKev8uBvOIiVAIqs0N5QM6RV-20g',NULL,'2026-04-08 11:28:53','2026-04-08 12:20:22'),(16,'cccc','pbkdf2$65536$YVjpt7zGI1QERuAEHTmeuQ==$wE7ioVP1+y6r9Bb46FRIrviovEJpWZ48ZldVXDsINA8=','띠띠띠띠','cccc@test.com','M','2006-07-07',175.00,75.00,NULL,NULL,NULL,'USER','ACTIVE','LOCAL',NULL,NULL,NULL,'2026-04-09 15:30:15','2026-04-09 15:30:15');
/*!40000 ALTER TABLE `member` ENABLE KEYS */;
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

-- Dump completed on 2026-04-14 11:56:36
