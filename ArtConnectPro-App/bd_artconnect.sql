CREATE DATABASE  IF NOT EXISTS `artconnect` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `artconnect`;
-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: localhost    Database: artconnect
-- ------------------------------------------------------
-- Server version	8.0.44

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

--
-- Table structure for table `artiste`
--

DROP TABLE IF EXISTS `artiste`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `artiste` (
  `id_artiste` int NOT NULL AUTO_INCREMENT,
  `nom` varchar(100) DEFAULT NULL,
  `bio` text,
  `annee_naissance` int DEFAULT NULL,
  `email_contact` varchar(150) DEFAULT NULL,
  `mot_passe` varchar(255) DEFAULT NULL,
  `telephone` varchar(20) DEFAULT NULL,
  `ville` varchar(100) DEFAULT NULL,
  `website` varchar(255) DEFAULT NULL,
  `social_media` varchar(255) DEFAULT NULL,
  `id_oeuvre` int DEFAULT NULL,
  `id_atelier` int DEFAULT NULL,
  PRIMARY KEY (`id_artiste`),
  KEY `id_oeuvre` (`id_oeuvre`),
  KEY `id_atelier` (`id_atelier`),
  KEY `idx_artiste_ville` (`ville`),
  CONSTRAINT `artiste_ibfk_1` FOREIGN KEY (`id_oeuvre`) REFERENCES `oeuvre` (`id_oeuvre`),
  CONSTRAINT `artiste_ibfk_2` FOREIGN KEY (`id_atelier`) REFERENCES `atelier` (`id_atelier`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `artiste`
--

LOCK TABLES `artiste` WRITE;
/*!40000 ALTER TABLE `artiste` DISABLE KEYS */;
INSERT INTO `artiste` VALUES (1,'Julien Mercier',NULL,1980,'julien.mercier@art.fr','64d004cd7494b9f82625c85dfcf37c298dc6c2c2ac571ae683e529b4fcfc4080','0700000001','Paris',NULL,NULL,13,6),(2,'Amandine Koch',NULL,1975,'amandine.koch@art.fr','1197db1182952817c677e5729674c8a52bd5b7bb1091825b5d045e82445fe70e','0700000002','Strasbourg',NULL,NULL,2,2),(3,'Karim Benali',NULL,1988,'karim.benali@art.fr','f6939facc8151233c52d79198b4f2bb7d4e2c6604ed43dc8bc05d5243c89ad23','0700000003','Marseille',NULL,NULL,3,3),(4,'Lucie Chartier',NULL,1992,'lucie.chartier@art.fr','1639c4ce5ff7c5bfc653d480a47ed87072aba0942a0d14d1d16672ed2429158e','0700000004','Lyon',NULL,NULL,4,5),(5,'Étienne Vasseur',NULL,1970,'etienne.vasseur@art.fr','cbdf640c54035f57f9abe8f7b0b6938ee6f633e9a3e08edc47db5eb5504012fe','0700000005','Paris',NULL,NULL,5,2),(6,'Naomi Tremblay',NULL,1995,'naomi.tremblay@art.fr','06d08dc82ade92dbbf47f496b9e1babed203eaf388c86df965add1765fc687b7','0700000006','Bordeaux',NULL,NULL,6,3),(7,'Hugo Lacroix',NULL,1983,'hugo.lacroix@art.fr','1e544168ba1e818a1606c9802f6102f93bf4f78588ae32164bd702cdd53bd371','0700000007','Paris',NULL,NULL,7,1),(8,'Sara Oliveira',NULL,1990,'sara.oliveira@art.fr','077ebc337fac41477575ed1d0f8b4863412b75caa9aa8aa8c935271519e35953','0700000008','Paris',NULL,NULL,8,4);
/*!40000 ALTER TABLE `artiste` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `artiste_discipline`
--

DROP TABLE IF EXISTS `artiste_discipline`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `artiste_discipline` (
  `id_artiste` int NOT NULL,
  `id_discipline` int NOT NULL,
  PRIMARY KEY (`id_artiste`,`id_discipline`),
  KEY `id_discipline` (`id_discipline`),
  CONSTRAINT `artiste_discipline_ibfk_1` FOREIGN KEY (`id_artiste`) REFERENCES `artiste` (`id_artiste`) ON DELETE CASCADE,
  CONSTRAINT `artiste_discipline_ibfk_2` FOREIGN KEY (`id_discipline`) REFERENCES `discipline` (`id_discipline`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `artiste_discipline`
--

LOCK TABLES `artiste_discipline` WRITE;
/*!40000 ALTER TABLE `artiste_discipline` DISABLE KEYS */;
INSERT INTO `artiste_discipline` VALUES (1,1),(4,1),(7,1),(2,2),(5,2),(3,3),(6,3),(8,4),(8,5);
/*!40000 ALTER TABLE `artiste_discipline` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `atelier`
--

DROP TABLE IF EXISTS `atelier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `atelier` (
  `id_atelier` int NOT NULL AUTO_INCREMENT,
  `titre` varchar(150) DEFAULT NULL,
  `date_atelier` datetime DEFAULT NULL,
  `duree` int DEFAULT NULL,
  `participants_max` int DEFAULT NULL,
  `prix` decimal(10,2) DEFAULT NULL,
  `description` text,
  `niveau` varchar(50) DEFAULT NULL,
  `lieu` varchar(150) DEFAULT NULL,
  `id_artiste` int DEFAULT NULL,
  PRIMARY KEY (`id_atelier`),
  KEY `fk_atelier_artiste` (`id_artiste`),
  CONSTRAINT `fk_atelier_artiste` FOREIGN KEY (`id_artiste`) REFERENCES `artiste` (`id_artiste`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `atelier`
--

LOCK TABLES `atelier` WRITE;
/*!40000 ALTER TABLE `atelier` DISABLE KEYS */;
INSERT INTO `atelier` VALUES (1,'Initiation à l\'aquarelle','2024-04-15 10:00:00',180,12,45.00,'Découverte des techniques de base de l\'aquarelle pour débutants.','intermédiaire','Studio Lumière, Paris',7),(2,'Sculpture sur argile','2024-05-20 14:00:00',240,8,60.00,'Modelage libre et découverte des formes tridimensionnelles.','intermédiaire','Espace Sculptura, Paris',2),(3,'Photographie urbaine','2024-06-10 09:00:00',300,10,55.00,'Sortie photo guidée dans les rues de la ville.','tous niveaux','Centre-ville',3),(4,'Improvisation musicale','2024-07-05 18:00:00',120,15,35.00,'Atelier de jam session ouvert à tous niveaux.','tous niveaux','Centre Culturel Bastille',8),(5,'Peinture abstraite expressive','2024-08-12 10:00:00',180,10,50.00,'Exploration des émotions à travers la peinture abstraite.','avancé','Galerie du Marais, Paris',4),(6,'poterie','2022-07-23 10:00:00',12,12,0.00,NULL,NULL,'lyon',1),(7,'art pinceau','2022-03-24 10:00:00',234,23,2323.00,NULL,NULL,'villejuif',1);
/*!40000 ALTER TABLE `atelier` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `audit_exposition`
--

DROP TABLE IF EXISTS `audit_exposition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_exposition` (
  `id_audit` int NOT NULL AUTO_INCREMENT,
  `id_exposition` int DEFAULT NULL,
  `ancien_titre` varchar(150) DEFAULT NULL,
  `nouveau_titre` varchar(150) DEFAULT NULL,
  `ancienne_date_debut` date DEFAULT NULL,
  `nouvelle_date_debut` date DEFAULT NULL,
  `ancienne_date_fin` date DEFAULT NULL,
  `nouvelle_date_fin` date DEFAULT NULL,
  `date_modification` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_audit`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_exposition`
--

LOCK TABLES `audit_exposition` WRITE;
/*!40000 ALTER TABLE `audit_exposition` DISABLE KEYS */;
/*!40000 ALTER TABLE `audit_exposition` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `discipline`
--

DROP TABLE IF EXISTS `discipline`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `discipline` (
  `id_discipline` int NOT NULL AUTO_INCREMENT,
  `nom` varchar(100) NOT NULL,
  PRIMARY KEY (`id_discipline`),
  UNIQUE KEY `nom` (`nom`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `discipline`
--

LOCK TABLES `discipline` WRITE;
/*!40000 ALTER TABLE `discipline` DISABLE KEYS */;
INSERT INTO `discipline` VALUES (7,'Céramique'),(6,'Dessin'),(5,'Installation'),(4,'Musique'),(1,'Peinture'),(3,'Photographie'),(2,'Sculpture');
/*!40000 ALTER TABLE `discipline` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exposer`
--

DROP TABLE IF EXISTS `exposer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exposer` (
  `id_exposition` int NOT NULL,
  `id_oeuvre` int NOT NULL,
  `emplacement` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id_exposition`,`id_oeuvre`),
  KEY `id_oeuvre` (`id_oeuvre`),
  KEY `idx_exposer_exposition` (`id_exposition`),
  CONSTRAINT `exposer_ibfk_1` FOREIGN KEY (`id_exposition`) REFERENCES `exposition` (`id_exposition`),
  CONSTRAINT `exposer_ibfk_2` FOREIGN KEY (`id_oeuvre`) REFERENCES `oeuvre` (`id_oeuvre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exposer`
--

LOCK TABLES `exposer` WRITE;
/*!40000 ALTER TABLE `exposer` DISABLE KEYS */;
INSERT INTO `exposer` VALUES (1,1,'Salle A - Mur Nord'),(1,4,'Salle B - Mur Est'),(1,7,'Salle A - Mur Ouest'),(2,2,'Salle principale - Centre'),(2,5,'Salle principale - Entrée'),(3,3,'Galerie photo - Panneau 1'),(3,6,'Galerie photo - Panneau 3'),(4,5,'Couloir d\'entrée'),(4,8,'Grande salle - Centre'),(5,1,'Salle des horizons - Mur Sud'),(5,4,'Salle des horizons - Mur Nord'),(6,3,'Couloir principal'),(6,6,'Salle panoramique');
/*!40000 ALTER TABLE `exposer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exposition`
--

DROP TABLE IF EXISTS `exposition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exposition` (
  `id_exposition` int NOT NULL AUTO_INCREMENT,
  `titre` varchar(150) DEFAULT NULL,
  `theme` varchar(150) DEFAULT NULL,
  `date_debut` date DEFAULT NULL,
  `date_fin` date DEFAULT NULL,
  `description` text,
  PRIMARY KEY (`id_exposition`),
  KEY `idx_exposition_dates` (`date_debut`,`date_fin`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exposition`
--

LOCK TABLES `exposition` WRITE;
/*!40000 ALTER TABLE `exposition` DISABLE KEYS */;
INSERT INTO `exposition` VALUES (1,'Lumières de Paris','Lumière et couleur','2024-03-01','2024-03-31','Exposition de peintures contemporaines inspirées des lumières parisiennes.'),(2,'Formes & Matières','Abstraction matérielle','2024-05-10','2024-06-15','Sculptures abstraites explorant la relation entre forme et matière.'),(3,'L\'Œil Numérique','Art numérique','2024-07-20','2024-08-20','Photographies numériques et installations interactives.'),(4,'Mémoires Vivantes','Mémoire collective','2024-09-05','2024-10-05','Art pluridisciplinaire autour de la mémoire collective.'),(5,'Horizons Colorés','Voyage chromatique','2025-01-15','2025-02-28','Voyage chromatique à travers différentes cultures artistiques.'),(6,'Nuit Blanche 2025','Art nocturne','2025-10-04','2025-10-05','Événement culturel nocturne réunissant artistes locaux.');
/*!40000 ALTER TABLE `exposition` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `galerie`
--

DROP TABLE IF EXISTS `galerie`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `galerie` (
  `id_galerie` int NOT NULL AUTO_INCREMENT,
  `nom` varchar(100) DEFAULT NULL,
  `adresse` varchar(255) DEFAULT NULL,
  `nom_proprietaire` varchar(100) DEFAULT NULL,
  `heure_ouverture` time DEFAULT NULL,
  `heure_fermeture` time DEFAULT NULL,
  `id_exposition` int DEFAULT NULL,
  PRIMARY KEY (`id_galerie`),
  KEY `id_exposition` (`id_exposition`),
  CONSTRAINT `galerie_ibfk_1` FOREIGN KEY (`id_exposition`) REFERENCES `exposition` (`id_exposition`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `galerie`
--

LOCK TABLES `galerie` WRITE;
/*!40000 ALTER TABLE `galerie` DISABLE KEYS */;
INSERT INTO `galerie` VALUES (1,'Galerie du Marais','12 Rue de Bretagne, 75003 Paris','Hélène Vidal','10:00:00','19:00:00',1),(2,'Espace Sculptura','8 Avenue de la République, 75011 Paris','Marc Lejeune','11:00:00','20:00:00',2),(3,'Studio Lumière','34 Rue Oberkampf, 75011 Paris','Sandrine Bloch','10:00:00','18:30:00',3),(4,'Centre Culturel Bastille','5 Place de la Bastille, 75012 Paris','Paul Renard','09:00:00','21:00:00',4),(5,'Galerie Arc-en-Ciel','22 Rue du Faubourg Saint-Antoine, 75012 Paris','Nadia Cohn','10:00:00','19:30:00',5),(6,'Galerie République','45 Av. de la République, 75011 Paris','François Leduc','20:00:00','06:00:00',6);
/*!40000 ALTER TABLE `galerie` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inscription`
--

DROP TABLE IF EXISTS `inscription`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inscription` (
  `id_membre` int NOT NULL,
  `id_atelier` int NOT NULL,
  `date_inscription` date DEFAULT NULL,
  `nb_places` int DEFAULT NULL,
  `statut_paie` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id_membre`,`id_atelier`),
  KEY `id_atelier` (`id_atelier`),
  KEY `idx_inscription_membre` (`id_membre`),
  CONSTRAINT `inscription_ibfk_1` FOREIGN KEY (`id_membre`) REFERENCES `membre_communaute` (`id_membre`),
  CONSTRAINT `inscription_ibfk_2` FOREIGN KEY (`id_atelier`) REFERENCES `atelier` (`id_atelier`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inscription`
--

LOCK TABLES `inscription` WRITE;
/*!40000 ALTER TABLE `inscription` DISABLE KEYS */;
INSERT INTO `inscription` VALUES (1,1,'2024-02-20',1,'payé'),(1,2,'2026-05-12',1,'en attente'),(1,4,'2024-07-01',1,'payé'),(1,5,'2026-05-12',1,'en attente'),(2,3,'2024-06-01',1,'payé'),(3,2,'2024-04-15',1,'payé'),(3,5,'2024-03-05',1,'payé'),(4,5,'2024-03-10',1,'en attente'),(5,1,'2024-02-25',1,'payé'),(6,4,'2024-07-05',2,'payé'),(7,3,'2024-06-15',1,'annulé'),(8,2,'2024-04-20',1,'payé'),(8,5,'2026-05-04',1,'en attente');
/*!40000 ALTER TABLE `inscription` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `membre_communaute`
--

DROP TABLE IF EXISTS `membre_communaute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `membre_communaute` (
  `id_membre` int NOT NULL AUTO_INCREMENT,
  `nom` varchar(100) DEFAULT NULL,
  `email` varchar(150) DEFAULT NULL,
  `mot_passe` varchar(255) DEFAULT NULL,
  `annee_naissance` int DEFAULT NULL,
  `telephone` varchar(20) DEFAULT NULL,
  `ville` varchar(100) DEFAULT NULL,
  `type_adhesion` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id_membre`),
  KEY `idx_membre_ville` (`ville`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `membre_communaute`
--

LOCK TABLES `membre_communaute` WRITE;
/*!40000 ALTER TABLE `membre_communaute` DISABLE KEYS */;
INSERT INTO `membre_communaute` VALUES (1,'Sophie Martin','sophie.martin@email.fr','e78c89cb9a0e91c499620d9ce9ef0aa1d2508537ebe000daebbfa98236cd0529',1990,'0612345678','Paris','premium'),(2,'Lucas Dubois','lucas.dubois@email.fr','ee1b384bf1efc1c70652779bdd085734b1e67c0e0115c5b27753ff9a4340da20',1985,'0623456789','Lyon','standard'),(3,'Emma Bernard','emma.bernard@email.fr','a122d664e70d4a0f95be2a4e9a80d89436be56dbb79a9d942d895942a739832d',1995,'0634567890','Marseille','premium'),(4,'Nathan Leroy','nathan.leroy@email.fr','0b71f95feb2043841d8bcdf45f4ebd6b60ce6594cd052bc285bbf6e280c4fc69',1988,'0645678901','Paris','standard'),(5,'Chloé Moreau','chloe.moreau@email.fr','8ad2e947134f9c8b2acd728088c7cde030a33486daf40e767adf7713743067bd',1992,'0656789012','Bordeaux','étudiant'),(6,'Thomas Petit','thomas.petit@email.fr','d519ee909b4e511108724423d93e6ed19d748085a997db97a3d3028fe50ae6a2',1979,'0667890123','Paris','premium'),(7,'Léa Simon','lea.simon@email.fr','65aee19dc82d67dd3077491625c5e5b3f4d78f957f5896eebca5b2c5d0d5c9c1',1998,'0678901234','Nantes','étudiant'),(8,'Antoine Roux','antoine.roux@email.fr','0e191383f71aaf31c4ae8adf32e2d2608d79c9f48062abfde198ac346a67d5c4',1983,'0689012345','Toulouse','standard');
/*!40000 ALTER TABLE `membre_communaute` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oeuvre`
--

DROP TABLE IF EXISTS `oeuvre`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oeuvre` (
  `id_oeuvre` int NOT NULL AUTO_INCREMENT,
  `titre` varchar(150) DEFAULT NULL,
  `annee_creation` int DEFAULT NULL,
  `type` varchar(100) DEFAULT NULL,
  `support` varchar(100) DEFAULT NULL,
  `description` text,
  `prix` decimal(10,2) DEFAULT NULL,
  `statut` varchar(50) DEFAULT NULL,
  `id_artiste` int DEFAULT NULL,
  PRIMARY KEY (`id_oeuvre`),
  KEY `idx_oeuvre_statut` (`statut`),
  KEY `fk_oeuvre_artiste` (`id_artiste`),
  CONSTRAINT `fk_oeuvre_artiste` FOREIGN KEY (`id_artiste`) REFERENCES `artiste` (`id_artiste`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oeuvre`
--

LOCK TABLES `oeuvre` WRITE;
/*!40000 ALTER TABLE `oeuvre` DISABLE KEYS */;
INSERT INTO `oeuvre` VALUES (1,'Le Pont au Crépuscule',2020,'Peinture','Huile sur toile',NULL,1300.00,'SOLD',NULL),(2,'Fragment #3',2023,'Sculpture','Bronze',NULL,3500.00,'EXHIBITED',2),(3,'Ruelle Oubliée',2021,'Photographie','Tirage argentique','Photographie en noir et blanc d\'une ruelle parisienne.',450.00,'exposé',3),(4,'Symphonie Bleue',2023,'Peintur','Acrylique sur toile',NULL,800.00,'SOLD',4),(5,'Éclats de Mémoire',2020,'Sculpture','Verre soufflé','Installation de fragments de verre colorés suspendus.',2800.00,'exposé',5),(6,'Visages de la Ville',2022,'Photographie','Impression numérique',NULL,600.00,'SOLD',6),(7,'L\'Arbre Rouge',2023,'Peinture','Huile sur toile','Arbre solitaire dans un paysage hivernal.',950.00,'disponible',7),(8,'Résonance',2024,'Installation','Métal et sons','Installation sonore et visuelle interactive.',5000.00,'exposé',8),(10,'sss',12,'sss','dee',NULL,13.00,'FOR_SALE',NULL),(11,'ded',23,'dd','dwd',NULL,100.00,'FOR_SALE',NULL),(12,'2df32',43,'fwfe','dd',NULL,23.00,'FOR_SALE',NULL),(13,'tour de verre',2023,'fleche','oile',NULL,23.00,'FOR_SALE',1);
/*!40000 ALTER TABLE `oeuvre` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `organisateur`
--

DROP TABLE IF EXISTS `organisateur`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `organisateur` (
  `id_organisateur` int NOT NULL AUTO_INCREMENT,
  `nomO` varchar(100) DEFAULT NULL,
  `mot_passe` varchar(255) DEFAULT NULL,
  `email` varchar(150) DEFAULT NULL,
  `contact` varchar(20) DEFAULT NULL,
  `id_exposition` int DEFAULT NULL,
  PRIMARY KEY (`id_organisateur`),
  KEY `id_exposition` (`id_exposition`),
  CONSTRAINT `organisateur_ibfk_1` FOREIGN KEY (`id_exposition`) REFERENCES `exposition` (`id_exposition`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `organisateur`
--

LOCK TABLES `organisateur` WRITE;
/*!40000 ALTER TABLE `organisateur` DISABLE KEYS */;
INSERT INTO `organisateur` VALUES (1,'Marie Dupont','240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9','marie.dupont@artconnect.fr','0611223344',1),(2,'Jean Rousseau','240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9','jean.rousseau@artconnect.fr','0622334455',2),(3,'Claire Fontaine','240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9','claire.fontaine@artconnect.fr','0633445566',3),(4,'Pierre Garnier','240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9','pierre.garnier@artconnect.fr','0644556677',4),(5,'Isabelle Morin','240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9','isabelle.morin@artconnect.fr','0655667788',5),(6,'Camille Besson','240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9','camille.besson@artconnect.fr','0699887766',6);
/*!40000 ALTER TABLE `organisateur` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reservation`
--

DROP TABLE IF EXISTS `reservation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reservation` (
  `id_exposition` int NOT NULL,
  `id_membre` int NOT NULL,
  `date` date DEFAULT NULL,
  `est_present` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id_exposition`,`id_membre`),
  KEY `idx_reservation_membre` (`id_membre`),
  CONSTRAINT `reservation_ibfk_1` FOREIGN KEY (`id_exposition`) REFERENCES `exposition` (`id_exposition`),
  CONSTRAINT `reservation_ibfk_2` FOREIGN KEY (`id_membre`) REFERENCES `membre_communaute` (`id_membre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reservation`
--

LOCK TABLES `reservation` WRITE;
/*!40000 ALTER TABLE `reservation` DISABLE KEYS */;
INSERT INTO `reservation` VALUES (1,1,'2024-03-05',1),(1,2,'2024-03-10',1),(1,4,'2024-03-12',0),(2,1,'2024-05-15',1),(2,3,'2024-05-20',1),(2,5,'2024-05-22',1),(3,1,'2026-05-13',0),(3,2,'2024-07-25',0),(3,6,'2024-07-28',1),(3,7,'2024-08-01',1),(4,1,'2024-09-15',0),(4,3,'2024-09-10',1),(4,4,'2026-05-04',0),(4,8,'2024-09-12',1),(5,1,'2026-05-11',0),(5,5,'2025-01-22',1),(5,6,'2025-02-01',1),(5,8,'2026-05-04',0);
/*!40000 ALTER TABLE `reservation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `vue_ateliers_inscrits`
--

DROP TABLE IF EXISTS `vue_ateliers_inscrits`;
/*!50001 DROP VIEW IF EXISTS `vue_ateliers_inscrits`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `vue_ateliers_inscrits` AS SELECT 
 1 AS `id_atelier`,
 1 AS `titre_atelier`,
 1 AS `participants_max`,
 1 AS `prix`,
 1 AS `places_prises`,
 1 AS `places_restantes`,
 1 AS `nom_artiste_directeur`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `vue_membres_sans_mdp`
--

DROP TABLE IF EXISTS `vue_membres_sans_mdp`;
/*!50001 DROP VIEW IF EXISTS `vue_membres_sans_mdp`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `vue_membres_sans_mdp` AS SELECT 
 1 AS `id_membre`,
 1 AS `nom`,
 1 AS `email`,
 1 AS `ville`,
 1 AS `type_adhesion`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `vue_oeuvres_artistes`
--

DROP TABLE IF EXISTS `vue_oeuvres_artistes`;
/*!50001 DROP VIEW IF EXISTS `vue_oeuvres_artistes`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `vue_oeuvres_artistes` AS SELECT 
 1 AS `id_oeuvre`,
 1 AS `titre_oeuvre`,
 1 AS `annee_creation`,
 1 AS `type`,
 1 AS `support`,
 1 AS `prix`,
 1 AS `statut`,
 1 AS `id_artiste`,
 1 AS `nom_artiste`,
 1 AS `ville_artiste`,
 1 AS `email_contact`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `vue_participation_expositions`
--

DROP TABLE IF EXISTS `vue_participation_expositions`;
/*!50001 DROP VIEW IF EXISTS `vue_participation_expositions`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `vue_participation_expositions` AS SELECT 
 1 AS `id_exposition`,
 1 AS `titre_exposition`,
 1 AS `date_debut`,
 1 AS `date_fin`,
 1 AS `nb_reservations`,
 1 AS `nb_presents`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `vue_programme_expositions`
--

DROP TABLE IF EXISTS `vue_programme_expositions`;
/*!50001 DROP VIEW IF EXISTS `vue_programme_expositions`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `vue_programme_expositions` AS SELECT 
 1 AS `id_exposition`,
 1 AS `titre_exposition`,
 1 AS `date_debut`,
 1 AS `date_fin`,
 1 AS `description_exposition`,
 1 AS `nom_galerie`,
 1 AS `adresse_galerie`,
 1 AS `heure_ouverture`,
 1 AS `heure_fermeture`*/;
SET character_set_client = @saved_cs_client;

--
-- Final view structure for view `vue_ateliers_inscrits`
--

/*!50001 DROP VIEW IF EXISTS `vue_ateliers_inscrits`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `vue_ateliers_inscrits` AS select `at`.`id_atelier` AS `id_atelier`,`at`.`titre` AS `titre_atelier`,`at`.`participants_max` AS `participants_max`,`at`.`prix` AS `prix`,coalesce(sum(`i`.`nb_places`),0) AS `places_prises`,(`at`.`participants_max` - coalesce(sum(`i`.`nb_places`),0)) AS `places_restantes`,`ar`.`nom` AS `nom_artiste_directeur` from ((`atelier` `at` left join `inscription` `i` on(((`i`.`id_atelier` = `at`.`id_atelier`) and (`i`.`statut_paie` <> 'annulé')))) left join `artiste` `ar` on((`ar`.`id_atelier` = `at`.`id_atelier`))) group by `at`.`id_atelier`,`at`.`titre`,`at`.`participants_max`,`at`.`prix`,`ar`.`nom` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `vue_membres_sans_mdp`
--

/*!50001 DROP VIEW IF EXISTS `vue_membres_sans_mdp`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `vue_membres_sans_mdp` AS select `membre_communaute`.`id_membre` AS `id_membre`,`membre_communaute`.`nom` AS `nom`,`membre_communaute`.`email` AS `email`,`membre_communaute`.`ville` AS `ville`,`membre_communaute`.`type_adhesion` AS `type_adhesion` from `membre_communaute` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `vue_oeuvres_artistes`
--

/*!50001 DROP VIEW IF EXISTS `vue_oeuvres_artistes`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `vue_oeuvres_artistes` AS select `o`.`id_oeuvre` AS `id_oeuvre`,`o`.`titre` AS `titre_oeuvre`,`o`.`annee_creation` AS `annee_creation`,`o`.`type` AS `type`,`o`.`support` AS `support`,`o`.`prix` AS `prix`,`o`.`statut` AS `statut`,`a`.`id_artiste` AS `id_artiste`,`a`.`nom` AS `nom_artiste`,`a`.`ville` AS `ville_artiste`,`a`.`email_contact` AS `email_contact` from (`oeuvre` `o` left join `artiste` `a` on((`a`.`id_oeuvre` = `o`.`id_oeuvre`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `vue_participation_expositions`
--

/*!50001 DROP VIEW IF EXISTS `vue_participation_expositions`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `vue_participation_expositions` AS select `e`.`id_exposition` AS `id_exposition`,`e`.`titre` AS `titre_exposition`,`e`.`date_debut` AS `date_debut`,`e`.`date_fin` AS `date_fin`,count(`r`.`id_membre`) AS `nb_reservations`,sum((case when (`r`.`est_present` = true) then 1 else 0 end)) AS `nb_presents` from (`exposition` `e` left join `reservation` `r` on((`r`.`id_exposition` = `e`.`id_exposition`))) group by `e`.`id_exposition`,`e`.`titre`,`e`.`date_debut`,`e`.`date_fin` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `vue_programme_expositions`
--

/*!50001 DROP VIEW IF EXISTS `vue_programme_expositions`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `vue_programme_expositions` AS select `e`.`id_exposition` AS `id_exposition`,`e`.`titre` AS `titre_exposition`,`e`.`date_debut` AS `date_debut`,`e`.`date_fin` AS `date_fin`,`e`.`description` AS `description_exposition`,`g`.`nom` AS `nom_galerie`,`g`.`adresse` AS `adresse_galerie`,`g`.`heure_ouverture` AS `heure_ouverture`,`g`.`heure_fermeture` AS `heure_fermeture` from (`exposition` `e` left join `galerie` `g` on((`g`.`id_exposition` = `e`.`id_exposition`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-14 12:45:33
