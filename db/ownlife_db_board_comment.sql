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
-- Table structure for table `board_comment`
--

DROP TABLE IF EXISTS `board_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `board_comment` (
  `comment_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `post_id` bigint unsigned NOT NULL,
  `member_id` bigint unsigned NOT NULL,
  `parent_comment_id` bigint unsigned DEFAULT NULL,
  `content` text NOT NULL,
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`comment_id`),
  KEY `idx_board_comment_post` (`post_id`),
  KEY `idx_board_comment_member` (`member_id`),
  KEY `idx_board_comment_parent` (`parent_comment_id`),
  CONSTRAINT `fk_board_comment_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_board_comment_parent` FOREIGN KEY (`parent_comment_id`) REFERENCES `board_comment` (`comment_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_board_comment_post` FOREIGN KEY (`post_id`) REFERENCES `board_post` (`post_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `board_comment`
--

LOCK TABLES `board_comment` WRITE;
/*!40000 ALTER TABLE `board_comment` DISABLE KEYS */;
INSERT INTO `board_comment` VALUES (6,8,3,NULL,'오옷 20000이라니 자네는 센스가 정말 남다르군!',0,'2026-04-01 03:16:45','2026-04-01 03:16:45'),(10,10,3,NULL,'땃쥐 ㄱ?',0,'2026-04-01 03:38:21','2026-04-01 03:38:21'),(14,10,3,NULL,'앗 설마 메뉴가 마음에 안들어서? ㅜㅜ',1,'2026-04-01 03:47:13','2026-04-01 03:47:24'),(15,10,3,NULL,'설마 메뉴가 마음에 안들어서? 다른것도 ㄱㄴㄱㄴ',0,'2026-04-01 03:47:34','2026-04-01 03:47:34'),(17,12,3,NULL,'시공의 폭풍',0,'2026-04-07 11:07:06','2026-04-07 11:07:06'),(18,12,14,NULL,'테스트',0,'2026-04-07 15:01:25','2026-04-07 15:01:25'),(19,17,3,NULL,'댓글댓를',0,'2026-04-08 16:06:50','2026-04-08 16:06:50'),(20,17,3,NULL,'이상하다 여기...\r\n',0,'2026-04-08 16:38:47','2026-04-08 16:38:47'),(21,13,3,NULL,'댓글을 단다 단ㄷ테',0,'2026-04-08 16:41:45','2026-04-08 16:41:45'),(22,18,3,NULL,'난 살안빼도 있는데 ㅎㅎ',0,'2026-04-08 17:28:09','2026-04-08 17:28:09'),(23,18,3,NULL,'헤이헤이헤이',0,'2026-04-08 17:41:40','2026-04-08 17:41:40');
/*!40000 ALTER TABLE `board_comment` ENABLE KEYS */;
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

-- Dump completed on 2026-04-14 11:56:34
