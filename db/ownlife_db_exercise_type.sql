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
-- Table structure for table `exercise_type`
--

DROP TABLE IF EXISTS `exercise_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exercise_type` (
  `exercise_type_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `exercise_name` varchar(50) NOT NULL,
  `category` enum('SELF','COUNT_SET','TIME','ROUTE') DEFAULT NULL,
  `kcal_per_rep` decimal(8,4) DEFAULT NULL,
  `kcal_per_min` decimal(8,4) DEFAULT NULL,
  `kcal_per_km` decimal(8,4) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`exercise_type_id`),
  UNIQUE KEY `uk_exercise_name` (`exercise_name`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exercise_type`
--

LOCK TABLES `exercise_type` WRITE;
/*!40000 ALTER TABLE `exercise_type` DISABLE KEYS */;
INSERT INTO `exercise_type` VALUES (1,'직접 입력','SELF',NULL,NULL,NULL,'운동 이름과 소모 칼로리를 직접 입력하는 placeholder 타입',1),(2,'팔굽혀펴기','COUNT_SET',0.4500,NULL,NULL,'세트 x 횟수 기반',1),(3,'스쿼트','COUNT_SET',0.5000,NULL,NULL,'세트 x 횟수 기반',1),(4,'윗몸일으키기','COUNT_SET',0.3200,NULL,NULL,'세트 x 횟수 기반',1),(5,'턱걸이','COUNT_SET',1.0000,NULL,NULL,'세트 x 횟수 기반',1),(6,'버피 테스트','COUNT_SET',1.5000,NULL,NULL,'세트 x 횟수 기반',1),(7,'걷기','ROUTE',NULL,6.0000,50.0000,'분당 및 km당 칼로리 기반',1),(8,'달리기','ROUTE',NULL,12.0000,75.0000,'분당 및 km당 칼로리 기반',1),(9,'자전거','ROUTE',NULL,9.0000,25.0000,'분당 및 km당 칼로리 기반',1),(10,'줄넘기','TIME',NULL,12.0000,NULL,'분당 칼로리 기반',1),(11,'수영','TIME',NULL,11.0000,NULL,'분당 칼로리 기반',1);
/*!40000 ALTER TABLE `exercise_type` ENABLE KEYS */;
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
