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
-- Table structure for table `activity_route_log`
--

DROP TABLE IF EXISTS `activity_route_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity_route_log` (
  `route_log_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `exercise_log_id` bigint unsigned NOT NULL,
  `start_place` varchar(255) DEFAULT NULL,
  `end_place` varchar(255) DEFAULT NULL,
  `start_lat` decimal(10,7) DEFAULT NULL,
  `start_lng` decimal(10,7) DEFAULT NULL,
  `end_lat` decimal(10,7) DEFAULT NULL,
  `end_lng` decimal(10,7) DEFAULT NULL,
  `route_distance_km` decimal(8,2) NOT NULL,
  `route_duration_min` int DEFAULT NULL,
  `map_provider` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`route_log_id`),
  UNIQUE KEY `uk_route_exercise_log` (`exercise_log_id`),
  CONSTRAINT `fk_route_log_exercise` FOREIGN KEY (`exercise_log_id`) REFERENCES `exercise_log` (`exercise_log_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `activity_route_log`
--

LOCK TABLES `activity_route_log` WRITE;
/*!40000 ALTER TABLE `activity_route_log` DISABLE KEYS */;
INSERT INTO `activity_route_log` VALUES (1,140,'시작 지점','종료 지점',35.1803116,129.1199522,35.1751159,129.1222976,3.88,30,'kakao'),(2,141,NULL,NULL,NULL,NULL,NULL,NULL,3.00,NULL,'manual'),(5,144,NULL,NULL,35.2019601,129.1177262,35.1689941,129.1363064,5.02,NULL,'kakao'),(7,146,NULL,NULL,35.2020962,129.1180564,35.1688093,129.1361836,5.25,NULL,'kakao'),(9,150,NULL,NULL,37.3222616,127.0223163,37.3213517,127.0215489,0.12,NULL,'kakao'),(11,153,NULL,NULL,35.1486403,129.0562018,35.1047431,129.0282125,5.50,NULL,'kakao'),(12,154,NULL,NULL,35.1471962,129.0592539,35.1939326,129.1805626,12.21,NULL,'kakao'),(13,156,NULL,NULL,35.2047236,129.1256042,35.1632008,129.1621402,5.69,NULL,'kakao'),(15,159,NULL,NULL,37.5695354,126.9745086,37.3221431,127.0289051,27.88,NULL,'kakao');
/*!40000 ALTER TABLE `activity_route_log` ENABLE KEYS */;
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

-- Dump completed on 2026-04-14 11:56:37
