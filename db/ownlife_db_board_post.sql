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
-- Table structure for table `board_post`
--

DROP TABLE IF EXISTS `board_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `board_post` (
  `post_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `member_id` bigint unsigned NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `view_count` int NOT NULL DEFAULT '0',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`post_id`),
  KEY `idx_board_post_member` (`member_id`),
  KEY `idx_board_post_created` (`created_at`),
  CONSTRAINT `fk_board_post_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `board_post`
--

LOCK TABLES `board_post` WRITE;
/*!40000 ALTER TABLE `board_post` DISABLE KEYS */;
INSERT INTO `board_post` VALUES (8,3,'나는 비비비비다','그러니 잘부탁한다 그럼 20000~',39,0,'2026-04-01 03:16:28','2026-04-11 02:36:55'),(10,3,'오늘 점심메뉴 추천','1. 맘터(외 햄버거)\r\n2. 칼만둣국(외 제육 돈까스 가능)',79,0,'2026-04-01 03:32:14','2026-04-07 12:03:28'),(11,9,'글쓰기','테스트',42,0,'2026-04-02 18:08:30','2026-04-07 15:29:02'),(12,10,'test','1234',18,0,'2026-04-06 11:49:52','2026-04-09 13:55:52'),(13,14,'테스트','테스트임',20,0,'2026-04-07 15:01:17','2026-04-09 12:26:45'),(14,3,'오운라이프 최고!!!','최고최고',1,1,'2026-04-08 14:30:15','2026-04-08 14:30:27'),(15,3,'ㅇㅇㅇㅇ','ㅇㅇㅇㅇ',5,1,'2026-04-08 14:31:01','2026-04-08 14:50:28'),(16,3,'테스트','테스트',5,1,'2026-04-08 14:31:58','2026-04-08 14:50:32'),(17,3,'테스트','테스트',61,0,'2026-04-08 14:44:38','2026-04-09 14:25:13'),(18,3,'살빠지고 몸만조아짐 ㅅㅂ','하아 야발 ㅜㅜ',26,0,'2026-04-08 17:27:34','2026-04-09 16:07:24'),(19,9,'ㅌㅅㅌ','ㅌㅅㅌ',1,1,'2026-04-09 09:54:20','2026-04-09 09:54:25'),(20,3,'비포 애프터','ㅇㅇㅇ',28,0,'2026-04-09 12:26:27','2026-04-09 16:07:08'),(21,3,'테슷흐','테슷흐',1,1,'2026-04-09 12:30:14','2026-04-09 12:30:19'),(22,3,'ㄴㅇㄻㄴ','ㅁㅇㅁㄴㅇ',2,1,'2026-04-09 12:33:48','2026-04-09 12:33:56'),(23,9,'테스트','1234',27,0,'2026-04-09 12:48:01','2026-04-09 16:07:20'),(24,2,'테스트','12344',23,0,'2026-04-09 15:03:04','2026-04-11 02:36:26'),(25,3,'1234','123456',16,0,'2026-04-09 15:22:07','2026-04-09 17:21:04'),(26,16,'안녕하세요 꾸벅','ㅎㅇㅎㅇ',30,0,'2026-04-09 15:31:10','2026-04-10 14:11:39'),(27,9,'업로드','업로드',9,0,'2026-04-09 15:41:50','2026-04-09 16:08:19'),(28,9,'업로드다시','222',16,0,'2026-04-09 16:05:55','2026-04-09 16:21:46'),(29,3,'test','test',8,0,'2026-04-09 16:18:52','2026-04-09 17:53:23'),(30,9,'테스트','23211',12,0,'2026-04-09 16:20:17','2026-04-11 02:36:22'),(31,2,'야이야이','21313',10,0,'2026-04-09 16:21:50','2026-04-11 02:36:18');
/*!40000 ALTER TABLE `board_post` ENABLE KEYS */;
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
