-- phpMyAdmin SQL Dump
-- version 5.2.3
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1:3306
-- Generation Time: Mar 01, 2026 at 07:50 PM
-- Server version: 8.4.7
-- PHP Version: 8.3.28

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `agrisense-360`
--

-- --------------------------------------------------------

--
-- Table structure for table `affectation_travail`
--

DROP TABLE IF EXISTS `affectation_travail`;
CREATE TABLE IF NOT EXISTS `affectation_travail` (
  `id_affectation` int NOT NULL AUTO_INCREMENT,
  `type_travail` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `date_debut` date NOT NULL,
  `date_fin` date NOT NULL,
  `zone_travail` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `statut` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id_affectation`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `affectation_travail`
--

INSERT INTO `affectation_travail` (`id_affectation`, `type_travail`, `date_debut`, `date_fin`, `zone_travail`, `statut`) VALUES
(2, '123', '2026-02-14', '2026-02-22', 'marsa', 'Terminée'),
(3, 'sdfgh', '2026-02-16', '2026-02-26', 'ariana', 'En attente');

-- --------------------------------------------------------

--
-- Table structure for table `animal`
--

DROP TABLE IF EXISTS `animal`;
CREATE TABLE IF NOT EXISTS `animal` (
  `id` int NOT NULL AUTO_INCREMENT,
  `earTag` int DEFAULT NULL,
  `type` enum('sheep','cow','goat','chicken') COLLATE utf8mb4_unicode_ci NOT NULL,
  `gender` enum('male','female') COLLATE utf8mb4_unicode_ci NOT NULL,
  `weight` double DEFAULT NULL,
  `healthStatus` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `birthDate` date DEFAULT NULL,
  `entryDate` date DEFAULT NULL,
  `origin` enum('born_in_farm','outside') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `vaccinated` tinyint(1) DEFAULT '0',
  `location` enum('barn1','barn2','barn3','chicken_coop1','chicken_coop2') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `earTag` (`earTag`)
) ENGINE=MyISAM AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `animal`
--

INSERT INTO `animal` (`id`, `earTag`, `type`, `gender`, `weight`, `healthStatus`, `birthDate`, `entryDate`, `origin`, `vaccinated`, `location`) VALUES
(1, 5001, 'sheep', 'male', 74, 'healthy', '2026-02-10', '2026-02-12', 'outside', 1, 'barn3');

-- --------------------------------------------------------

--
-- Table structure for table `animalhealthrecord`
--

DROP TABLE IF EXISTS `animalhealthrecord`;
CREATE TABLE IF NOT EXISTS `animalhealthrecord` (
  `id` int NOT NULL AUTO_INCREMENT,
  `animal` int NOT NULL,
  `recordDate` date NOT NULL,
  `weight` double DEFAULT NULL,
  `appetite` enum('low','normal','high','none') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `conditionStatus` enum('healthy','sick','injured','critical') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `milkYield` double DEFAULT NULL,
  `eggCount` int DEFAULT NULL,
  `woolLength` double DEFAULT NULL,
  `notes` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `animal` (`animal`)
) ENGINE=MyISAM AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `animalhealthrecord`
--

INSERT INTO `animalhealthrecord` (`id`, `animal`, `recordDate`, `weight`, `appetite`, `conditionStatus`, `milkYield`, `eggCount`, `woolLength`, `notes`) VALUES
(1, 1, '2026-02-20', 74, 'low', 'healthy', NULL, NULL, 21, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `culture`
--

DROP TABLE IF EXISTS `culture`;
CREATE TABLE IF NOT EXISTS `culture` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nom` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type_Culture` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `date_Plantation` date DEFAULT NULL,
  `date_Recolte` date DEFAULT NULL,
  `etat` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `surface` double DEFAULT NULL,
  `img` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `parcelle_Id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `parcelleId` (`parcelle_Id`)
) ENGINE=MyISAM AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `culture`
--

INSERT INTO `culture` (`id`, `nom`, `type_Culture`, `date_Plantation`, `date_Recolte`, `etat`, `surface`, `img`, `parcelle_Id`) VALUES
(20, 'Laurier-rose', 'Ornementales', '2025-12-27', '2026-03-27', 'Croissance', 150, 'laurier_rose.png', 12),
(18, 'Orange', 'Fruits', '2026-02-05', '2026-05-06', 'Semis', 50, 'orange.png', 11),
(21, 'Pêche', 'Fruits', '2026-01-27', '2026-07-06', 'Croissance', 50, 'peche.png', 10),
(13, 'Riz', 'Céréales', '2025-09-28', '2026-02-20', 'Récolte en Retard', 1000, 'riz.png', 3);

-- --------------------------------------------------------

--
-- Table structure for table `equipments`
--

DROP TABLE IF EXISTS `equipments`;
CREATE TABLE IF NOT EXISTS `equipments` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `purchase_date` date DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `equipments`
--

INSERT INTO `equipments` (`id`, `name`, `type`, `status`, `purchase_date`) VALUES
(2, 'aa', 'aa', 'aa', '2005-12-05');

-- --------------------------------------------------------

--
-- Table structure for table `evaluation_performance`
--

DROP TABLE IF EXISTS `evaluation_performance`;
CREATE TABLE IF NOT EXISTS `evaluation_performance` (
  `id_evaluation` int NOT NULL AUTO_INCREMENT,
  `id_affectation` int NOT NULL,
  `note` int NOT NULL,
  `qualite` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `commentaire` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `date_evaluation` date DEFAULT NULL,
  PRIMARY KEY (`id_evaluation`),
  KEY `fk_eval_affectation` (`id_affectation`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `evaluation_performance`
--

INSERT INTO `evaluation_performance` (`id_evaluation`, `id_affectation`, `note`, `qualite`, `commentaire`, `date_evaluation`) VALUES
(3, 2, 2, 'Bonne', 'bien', '2026-01-26');

-- --------------------------------------------------------

--
-- Table structure for table `maintenance`
--

DROP TABLE IF EXISTS `maintenance`;
CREATE TABLE IF NOT EXISTS `maintenance` (
  `id` int NOT NULL AUTO_INCREMENT,
  `equipment_id` int NOT NULL,
  `maintenance_date` date NOT NULL,
  `maintenance_type` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `cost` decimal(10,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_maintenance_equipment` (`equipment_id`)
) ENGINE=MyISAM AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `maintenance`
--

INSERT INTO `maintenance` (`id`, `equipment_id`, `maintenance_date`, `maintenance_type`, `cost`) VALUES
(1, 2, '2026-02-11', 'aaze', 1500.00);

-- --------------------------------------------------------

--
-- Table structure for table `parcelle`
--

DROP TABLE IF EXISTS `parcelle`;
CREATE TABLE IF NOT EXISTS `parcelle` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nom` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `surface` double NOT NULL,
  `localisation` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type_sol` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `statut` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `surface_restant` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `parcelle`
--

INSERT INTO `parcelle` (`id`, `nom`, `surface`, `localisation`, `type_sol`, `statut`, `surface_restant`) VALUES
(3, 'Parcelle A', 5000, 'Tataouine', 'Sol Argileux', 'Libre', 4000),
(11, 'aaabbb', 50, 'Ariana', 'Sol Limoneux', 'Occupée', 0),
(12, 'parcelle dar habouba', 150, 'Tozeur', 'Sol Sablonneux', 'Occupée', 0),
(10, 'azza', 50, 'Mahdia', 'Sol Argileux', 'Occupée', 0);

-- --------------------------------------------------------

--
-- Table structure for table `parcelle_historique`
--

DROP TABLE IF EXISTS `parcelle_historique`;
CREATE TABLE IF NOT EXISTS `parcelle_historique` (
  `id` int NOT NULL AUTO_INCREMENT,
  `parcelle_id` int NOT NULL,
  `type_action` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'CULTURE_AJOUTEE | CULTURE_MODIFIEE | CULTURE_SUPPRIMEE | RECOLTE',
  `culture_id` int DEFAULT NULL,
  `culture_nom` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type_culture` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `surface` double DEFAULT NULL,
  `etat_avant` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `etat_apres` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `date_action` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `description` text COLLATE utf8mb4_unicode_ci,
  `quantite_recolte` double DEFAULT NULL COMMENT 'kg récoltés si récolte',
  PRIMARY KEY (`id`),
  KEY `idx_parcelle_id` (`parcelle_id`),
  KEY `idx_date_action` (`date_action`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `parcelle_historique`
--

INSERT INTO `parcelle_historique` (`id`, `parcelle_id`, `type_action`, `culture_id`, `culture_nom`, `type_culture`, `surface`, `etat_avant`, `etat_apres`, `date_action`, `description`, `quantite_recolte`) VALUES
(1, 3, 'CULTURE_AJOUTEE', 27, 'Lentille', 'Légumes', 100, NULL, 'Récolte Prévue', '2026-02-28 23:46:37', 'Culture \"Lentille\" (Légumes) ajoutee · 100.0 m2 · du 2025-12-03 au 2026-03-03', NULL),
(2, 3, 'CULTURE_MODIFIEE', 27, 'Lentille', 'Légumes', 150, 'Récolte Prévue', 'Récolte Prévue', '2026-02-28 23:47:01', 'Culture \"Lentille\" modifiee. Surface: 100.0 -> 150.0 m2.', NULL),
(3, 3, 'CULTURE_SUPPRIMEE', 27, 'Lentille', 'Légumes', 150, 'Récolte Prévue', NULL, '2026-02-28 23:48:21', 'Culture \"Lentille\" supprimee · 150.0 m2 liberes.', NULL),
(4, 3, 'CULTURE_AJOUTEE', 28, 'Jasmin', 'Ornementales', 12, NULL, 'Maturité', '2026-02-28 23:49:21', 'Culture \"Jasmin\" (Ornementales) ajoutee · 12.0 m2 · du 2025-12-02 au 2026-03-22', NULL),
(5, 3, 'CULTURE_SUPPRIMEE', 28, 'Jasmin', 'Ornementales', 12, 'Maturité', NULL, '2026-02-28 23:49:33', 'Culture \"Jasmin\" supprimee · 12.0 m2 liberes.', NULL),
(6, 3, 'RECOLTE', 23, 'Framboise', 'Fruits', 100, 'Récolte Prévue', 'Recoltee', '2026-02-28 23:57:41', 'Recolte de \"Framboise\" (Fruits) · Surface: 100.0 m2 liberee · Quantite ML: 2058,5 kg · Date recolte prevue: 2026-02-28', 2058.5);

-- --------------------------------------------------------

--
-- Table structure for table `password_reset`
--

DROP TABLE IF EXISTS `password_reset`;
CREATE TABLE IF NOT EXISTS `password_reset` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `code` varchar(6) COLLATE utf8mb4_general_ci NOT NULL,
  `expires_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `used` tinyint(1) DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `password_reset`
--

INSERT INTO `password_reset` (`id`, `user_id`, `code`, `expires_at`, `used`, `created_at`) VALUES
(4, 29, '534198', '2026-02-21 22:31:44', 1, '2026-02-21 22:31:15'),
(11, 10, '036032', '2026-02-26 23:24:03', 1, '2026-02-26 23:23:19');

-- --------------------------------------------------------

--
-- Table structure for table `produit`
--

DROP TABLE IF EXISTS `produit`;
CREATE TABLE IF NOT EXISTS `produit` (
  `id` int NOT NULL AUTO_INCREMENT,
  `agriculteur_id` int NOT NULL,
  `categorie` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `nom` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `fournisseur` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `prix_unitaire` decimal(10,2) NOT NULL DEFAULT '0.00',
  `photo_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_nom_agriculteur` (`agriculteur_id`,`nom`),
  KEY `idx_produit_categorie` (`categorie`),
  KEY `idx_produit_nom` (`nom`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `produit`
--

INSERT INTO `produit` (`id`, `agriculteur_id`, `categorie`, `nom`, `description`, `fournisseur`, `prix_unitaire`, `photo_url`, `created_at`, `updated_at`) VALUES
(2, 3, 'Céréales', 'Avoine', 'Récolte automatique depuis la culture. Surface: 20 m² | Type: Céréales', NULL, 0.00, '/images/cultures/avoine.png', '2026-02-28 02:19:57', '2026-02-28 03:19:57'),
(3, 3, 'Fruits', 'Framboise', 'Récolte automatique depuis la culture. Surface: 10 m² | Type: Fruits', NULL, 0.00, '/images/cultures/framboise.png', '2026-02-28 02:22:33', '2026-02-28 03:22:33'),
(4, 3, 'Légumes', 'Carottes', 'Récolte automatique depuis la culture. Surface: 1000 m² | Type: Légumes', NULL, 0.00, '/images/cultures/carottes.png', '2026-02-28 02:27:01', '2026-02-28 03:27:00'),
(5, 3, 'Fruits', 'Orange', 'Récolte automatique depuis la culture. Surface: 100 m² | Type: Fruits', NULL, 0.00, '/images/cultures/orange.png', '2026-02-28 03:19:44', '2026-02-28 04:19:44'),
(6, 3, 'Fruits', 'Pomme', 'Récolte automatique depuis la culture. Surface: 100 m² | Type: Fruits', NULL, 0.00, '/images/cultures/pomme.png', '2026-02-28 20:58:39', '2026-02-28 21:58:39'),
(7, 3, 'Légumes', 'Lentille', 'Récolte automatique depuis la culture. Surface: 150 m² | Type: Légumes', NULL, 0.00, '/images/cultures/lentille.png', '2026-02-28 21:48:22', '2026-02-28 22:48:21');

-- --------------------------------------------------------

--
-- Table structure for table `stock`
--

DROP TABLE IF EXISTS `stock`;
CREATE TABLE IF NOT EXISTS `stock` (
  `id` int NOT NULL AUTO_INCREMENT,
  `produit_id` int NOT NULL,
  `quantite_actuelle` decimal(12,3) NOT NULL DEFAULT '0.000',
  `seuil_alerte` decimal(12,3) NOT NULL DEFAULT '10.000',
  `unite_mesure` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `date_reception` date DEFAULT NULL,
  `date_expiration` date DEFAULT NULL,
  `emplacement` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stock_produit` (`produit_id`),
  KEY `idx_stock_quantite` (`quantite_actuelle`),
  KEY `idx_stock_alerte` (`seuil_alerte`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `stock`
--

INSERT INTO `stock` (`id`, `produit_id`, `quantite_actuelle`, `seuil_alerte`, `unite_mesure`, `date_reception`, `date_expiration`, `emplacement`, `created_at`, `updated_at`) VALUES
(3, 2, 2116.800, 50.000, 'kg', '2026-02-28', NULL, 'TEST', '2026-02-28 03:19:57', '2026-02-28 03:19:57'),
(4, 3, 4460.400, 50.000, 'kg', '2026-02-28', NULL, 'tii ey', '2026-02-28 03:22:33', '2026-02-28 22:57:41'),
(5, 4, 3975.900, 50.000, 'kg', '2026-02-28', NULL, 'TESSST WORKS', '2026-02-28 03:27:00', '2026-02-28 03:27:00'),
(6, 5, 3240.000, 50.000, 'kg', '2026-02-28', NULL, 'test_screen', '2026-02-28 04:19:44', '2026-02-28 04:19:44'),
(7, 6, 2058.500, 50.000, 'kg', '2026-02-28', NULL, 'testtt-ahd', '2026-02-28 21:58:39', '2026-02-28 21:58:39'),
(8, 7, 5132.200, 50.000, 'kg', '2026-02-28', NULL, 'haya barka', '2026-02-28 22:48:21', '2026-02-28 22:48:21');

-- --------------------------------------------------------

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
CREATE TABLE IF NOT EXISTS `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `phone` int NOT NULL,
  `roles` enum('ROLE_GERANT','ROLE_OUVRIER','ROLE_ADMIN') COLLATE utf8mb4_general_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `auth_provider` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'LOCAL',
  `google_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `profile_picture` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `user`
--

INSERT INTO `user` (`id`, `name`, `email`, `password`, `phone`, `roles`, `status`, `auth_provider`, `google_id`, `profile_picture`) VALUES
(3, 'ahd', 'aa@mail.com', '555', 69847988, 'ROLE_GERANT', 'ACTIVE', 'LOCAL', NULL, NULL),
(5, 'ENAWW', 'naw@mail.com', '1234', 0, 'ROLE_OUVRIER', 'ACTIVE', 'LOCAL', NULL, NULL),
(6, 'doudy', 'doudi@gmail.com', '555', 8965471, 'ROLE_OUVRIER', 'ACTIVE', 'LOCAL', NULL, NULL),
(7, 'kikooo', 'kiko@gmail.com', '1234', 55555555, 'ROLE_ADMIN', 'ACTIVE', 'LOCAL', NULL, NULL),
(8, 'ahd', 'ahd@gmail.com', '444', 58566, 'ROLE_ADMIN', 'ACTIVE', 'LOCAL', NULL, NULL),
(10, 'odyosss', 'ahmedhabouba.com@gmail.com', '171004%Ahmed', 55233216, 'ROLE_GERANT', 'ACTIVE', 'LOCAL', NULL, 'C:\\Users\\iamam\\Downloads\\dixco.jpg');

-- --------------------------------------------------------

--
-- Table structure for table `user_faces`
--

DROP TABLE IF EXISTS `user_faces`;
CREATE TABLE IF NOT EXISTS `user_faces` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `face_encoding` longtext COLLATE utf8mb4_general_ci NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `user_faces`
--

INSERT INTO `user_faces` (`id`, `user_id`, `face_encoding`, `created_at`) VALUES
(1, 3, '[-0.1565505713224411, 0.14471402764320374, 0.057755716145038605, -0.09847772121429443, -0.12359370291233063, -0.021624568849802017, -0.06989794969558716, -0.09564021229743958, 0.22870315611362457, -0.13830561935901642, 0.20723851025104523, 0.004140177275985479, -0.16998833417892456, 0.054696835577487946, -0.049229033291339874, 0.16088393330574036, -0.09741939604282379, -0.09196557104587555, -0.06685523688793182, -0.01736580580472946, 0.03371744230389595, -0.012821007519960403, 0.058186836540699005, 0.10293891280889511, -0.15722714364528656, -0.374167263507843, -0.08246453106403351, -0.10777762532234192, -0.016544410958886147, -0.07160085439682007, -0.007039135321974754, 0.04609876871109009, -0.2465982884168625, -0.048692189157009125, -0.01046292670071125, 0.09772835671901703, -0.028935419395565987, 0.006439288146793842, 0.14734923839569092, -0.022834328934550285, -0.24381890892982483, -0.03874842822551727, 0.0813024491071701, 0.18382713198661804, 0.1679341346025467, 0.06415683776140213, 0.08787952363491058, -0.09809834510087967, 0.04571937024593353, -0.17681530117988586, 0.10831433534622192, 0.06772728264331818, 0.08728715777397156, 0.07268931716680527, 0.043856944888830185, -0.18867677450180054, 0.014206433668732643, 0.16047872602939606, -0.1695721447467804, 0.03855033218860626, 0.04552105814218521, 0.02818652056157589, -0.07366661727428436, 0.01365174725651741, 0.3304027020931244, 0.18971820175647736, -0.15701812505722046, -0.07522401213645935, 0.20808181166648865, -0.11002504825592041, 0.008124522864818573, 0.062189504504203796, -0.15401628613471985, -0.21618854999542236, -0.2499111443758011, 0.05276261270046234, 0.45107463002204895, 0.15597155690193176, -0.1486898809671402, 0.02093001827597618, -0.09717976301908493, -0.003314108122140169, -0.010585954412817955, 0.13615447282791138, -0.08493854105472565, 0.0217985138297081, 0.03021370619535446, 0.09986425191164017, 0.1625390350818634, 0.043030403554439545, -0.04288375377655029, 0.19679656624794006, -0.008471829816699028, 0.016346579417586327, -0.04103682562708855, 0.07183016836643219, -0.1316455453634262, -0.0413057804107666, -0.14141418039798737, -0.08252999186515808, -0.0009265448898077011, 0.04431308060884476, -0.03616734594106674, 0.1628381907939911, -0.22994230687618256, 0.16906528174877167, -0.011695992201566696, -0.0323328971862793, 0.0005749613046646118, 0.13406626880168915, -0.016767630353569984, -0.09219583123922348, 0.09763514995574951, -0.20266835391521454, 0.20752675831317902, 0.15512540936470032, -0.01402379758656025, 0.17072542011737823, 0.07310798019170761, 0.07254765182733536, 0.10699249804019928, -0.0032976772636175156, -0.17642900347709656, -0.03320157900452614, 0.11657015979290009, -0.09803187847137451, 0.12092896550893784, 0.02382729761302471]', '2026-03-01 03:33:50'),
(2, 39, '[-0.1651492416858673, 0.10963467508554459, 0.061342474073171616, -0.07145260274410248, -0.10451018065214157, -0.03305938094854355, 0.0003878884017467499, -0.12392164766788483, 0.22571401298046112, -0.20130731165409088, 0.1862325221300125, -0.06785859167575836, -0.22575311362743378, -0.0783865749835968, -0.01630617491900921, 0.18603137135505676, -0.12556496262550354, -0.18939915299415588, 0.0007159821689128876, -0.042224541306495667, 0.13109417259693146, -0.06523166596889496, 0.01639234647154808, 0.10160477459430695, -0.24387690424919128, -0.33763396739959717, -0.08997923135757446, -0.12239302694797516, 0.049018844962120056, -0.07558772712945938, 0.026014015078544617, 0.06648652255535126, -0.22344636917114258, -0.04709290713071823, -0.007236096542328596, 0.10365605354309082, -0.008500938303768635, -0.005790539085865021, 0.15199759602546692, -0.07227850705385208, -0.29674267768859863, -0.12341057509183884, 0.15395481884479523, 0.20007233321666718, 0.15102042257785797, 0.017658187076449394, 0.015422102995216846, -0.07029032707214355, 0.06846380978822708, -0.23996660113334656, 0.04143921658396721, 0.07642800360918045, 0.06078094616532326, 0.07759318500757217, 0.12719184160232544, -0.2112928181886673, 0.046082332730293274, 0.0874590054154396, -0.20691558718681335, 0.0697309747338295, 0.018661487847566605, -0.07024439424276352, -0.05730104446411133, 0.01780356653034687, 0.23945170640945435, 0.06890963762998581, -0.1144077405333519, -0.11261218786239624, 0.25941139459609985, -0.16122077405452728, 0.04701944813132286, 0.15814712643623352, -0.11266513168811798, -0.18492192029953003, -0.3115915358066559, -0.05722392350435257, 0.4691429138183594, 0.17139503359794617, -0.09757310897111893, 0.07957375049591064, -0.08312787115573883, -0.014648528769612312, 0.039597995579242706, 0.12028098106384277, 0.0032908301800489426, 0.01623392477631569, -0.05492446944117546, 0.09204959869384766, 0.12823204696178436, -0.01994972489774227, -0.001025869743898511, 0.2015254944562912, -0.0020133275538682938, 0.017979085445404053, -0.02809234894812107, 0.06225834786891937, -0.18543097376823425, -0.04632222279906273, -0.15315775573253632, -0.1150154173374176, 0.0033773034811019897, 0.011999962851405144, 0.0052617439068853855, 0.15050512552261353, -0.1964344084262848, 0.1248973160982132, 0.04769853875041008, -0.030454356223344803, -0.04252792149782181, 0.11783001571893692, -0.07916057854890823, -0.06275105476379395, 0.06802518665790558, -0.2326965183019638, 0.16552948951721191, 0.1757512092590332, -0.020848995074629784, 0.12713640928268433, 0.08192969858646393, 0.027643907815217972, 0.06704352051019669, -0.08251677453517914, -0.14042925834655762, -0.030318789184093475, 0.03822408616542816, -0.1386374831199646, 0.06783979386091232, -0.0023574577644467354]', '2026-03-01 03:52:48');

-- --------------------------------------------------------

--
-- Table structure for table `user_sessions`
--

DROP TABLE IF EXISTS `user_sessions`;
CREATE TABLE IF NOT EXISTS `user_sessions` (
  `session_id` int DEFAULT NULL,
  `user_id` int NOT NULL,
  `session_token` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `ip_address` varchar(45) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `last_activity` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `expires_at` timestamp NOT NULL DEFAULT ((now() + interval 7 day)),
  `is_active` tinyint(1) DEFAULT '1',
  `device_info` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `user_id` (`user_id`),
  KEY `user_id_2` (`user_id`),
  KEY `user_id_3` (`user_id`),
  KEY `user_id_4` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `user_sessions`
--

INSERT INTO `user_sessions` (`session_id`, `user_id`, `session_token`, `created_at`, `ip_address`, `last_activity`, `expires_at`, `is_active`, `device_info`) VALUES
(NULL, 7, 'e9f2aaf7-d78f-42ff-8997-c46faba6c3bb-1771282674495', '2026-02-16 22:58:14', '172.24.72.1', '2026-02-16 22:58:14', '2026-03-18 21:57:54', 0, 'Windows 11 10.0'),
(NULL, 3, 'a1d504c5-058a-4001-b46f-1c4c5ab20c5c-1771282719043', '2026-02-16 22:59:41', '172.24.72.1', '2026-02-16 22:59:41', '2026-03-18 21:58:39', 0, 'Windows 11 10.0'),
(NULL, 5, 'b23909c0-7cd8-47f8-a3f4-24629b445516-1771282806061', '2026-02-16 23:00:43', '172.24.72.1', '2026-02-16 23:00:43', '2026-03-18 22:00:06', 0, 'Windows 11 10.0'),
(NULL, 3, '30fd367f-a6f7-4111-902d-246226945a1e-1771282852495', '2026-02-16 23:00:52', '172.24.72.1', '2026-02-16 23:00:52', '2026-03-18 22:00:52', 1, 'Windows 11 10.0'),
(NULL, 3, '320c0ae9-e2e6-483a-9084-b380d44ea552-1771283062435', '2026-02-16 23:05:32', '172.24.72.1', '2026-02-16 23:05:32', '2026-03-18 22:04:22', 0, 'Windows 11 10.0'),
(NULL, 5, 'bf91c9e7-7752-4bfb-b649-1fb31c291e28-1771283157179', '2026-02-16 23:05:57', '172.24.72.1', '2026-02-16 23:05:57', '2026-03-18 22:05:57', 1, 'Windows 11 10.0'),
(NULL, 3, '0b5fdd69-b6ba-4018-97ee-aece174758fd-1771283535951', '2026-02-16 23:12:15', '172.24.72.1', '2026-02-16 23:12:15', '2026-03-18 22:12:16', 1, 'Windows 11 10.0'),
(NULL, 3, '6dfb4b72-9425-438e-b9bb-5f2c2754df2b-1771284232964', '2026-02-16 23:23:52', '172.24.72.1', '2026-02-16 23:23:52', '2026-03-18 22:23:53', 1, 'Windows 11 10.0'),
(NULL, 3, 'a52fc8fa-c93d-45ce-8126-dd9ed56badd4-1771284295303', '2026-02-16 23:24:55', '172.24.72.1', '2026-02-16 23:24:55', '2026-03-18 22:24:55', 1, 'Windows 11 10.0'),
(NULL, 3, '638e2b6d-44e1-4ecc-8e44-e75986b357e5-1771284746384', '2026-02-16 23:32:26', '172.24.72.1', '2026-02-16 23:32:26', '2026-03-18 22:32:26', 1, 'Windows 11 10.0'),
(NULL, 3, '993f7804-e3a7-4f14-9cd2-a74e4550bde9-1771288650014', '2026-02-17 00:37:30', '172.24.72.1', '2026-02-17 00:37:30', '2026-03-18 23:37:30', 1, 'Windows 11 10.0'),
(NULL, 3, 'a84a5457-0a04-4b2b-a03c-f4da850eea46-1771288897626', '2026-02-17 00:41:37', '172.24.72.1', '2026-02-17 00:41:37', '2026-03-18 23:41:38', 1, 'Windows 11 10.0'),
(NULL, 3, '44cce015-ec87-40c4-beac-498072c6a1d9-1771289063414', '2026-02-17 00:44:23', '172.24.72.1', '2026-02-17 00:44:23', '2026-03-18 23:44:23', 1, 'Windows 11 10.0'),
(NULL, 3, '8f331bb5-0456-47a1-8d54-d74ebfd71dd1-1771289241386', '2026-02-17 00:47:21', '172.24.72.1', '2026-02-17 00:47:21', '2026-03-18 23:47:21', 1, 'Windows 11 10.0'),
(NULL, 3, '93c7fe7b-6e68-40f9-bacb-ea4bf33c9baa-1771289344102', '2026-02-17 00:49:04', '172.24.72.1', '2026-02-17 00:49:04', '2026-03-18 23:49:04', 1, 'Windows 11 10.0'),
(NULL, 3, 'a042293c-ca4d-4c03-b2cf-bac7a6246cc9-1771292070349', '2026-02-17 01:36:22', '172.24.72.1', '2026-02-17 01:36:22', '2026-03-19 00:34:30', 0, 'Windows 11 10.0'),
(NULL, 7, 'b81f7d96-b45f-4f95-a1b4-ef5badbd4a0f-1771292208093', '2026-02-17 01:36:48', '172.24.72.1', '2026-02-17 01:36:48', '2026-03-19 00:36:48', 1, 'Windows 11 10.0'),
(NULL, 7, '4f5f5529-2090-44cc-b93a-4d728f2bf2e6-1771292285458', '2026-02-17 01:38:05', '172.24.72.1', '2026-02-17 01:38:05', '2026-03-19 00:38:05', 1, 'Windows 11 10.0'),
(NULL, 3, 'b2f2a261-d0f1-4ddc-8d05-6b6044641715-1771318095933', '2026-02-17 08:48:15', '172.24.72.1', '2026-02-17 08:48:15', '2026-03-19 07:48:16', 1, 'Windows 11 10.0'),
(NULL, 3, 'b0e2f333-b8dc-45fc-9574-7af85ae48464-1771318477630', '2026-02-17 09:12:17', '172.24.72.1', '2026-02-17 09:12:17', '2026-03-19 07:54:38', 0, 'Windows 11 10.0'),
(NULL, 3, '2937f659-8ddd-4b74-927d-f1790d6c1262-1771319550818', '2026-02-17 09:12:30', '172.24.72.1', '2026-02-17 09:12:30', '2026-03-19 08:12:31', 1, 'Windows 11 10.0'),
(NULL, 3, 'd431673a-f4c2-4385-abf2-6c0dfade4056-1771321045686', '2026-02-17 09:37:25', '172.24.72.1', '2026-02-17 09:37:25', '2026-03-19 08:37:26', 1, 'Windows 11 10.0'),
(NULL, 3, 'd4756df4-5d75-4061-9457-71a6c734e921-1771542551214', '2026-02-19 23:09:11', '172.24.72.1', '2026-02-19 23:09:11', '2026-03-21 22:09:11', 1, 'Windows 11 10.0'),
(NULL, 3, '7b4102bc-30c6-4e70-8c50-2a339ead673a-1771545171431', '2026-02-19 23:52:51', '172.24.72.1', '2026-02-19 23:52:51', '2026-03-21 22:52:51', 1, 'Windows 11 10.0'),
(NULL, 3, '020b5196-d5d4-4466-b414-ed046aebc6ce-1771547621970', '2026-02-20 00:33:41', '172.24.72.1', '2026-02-20 00:33:41', '2026-03-21 23:33:42', 1, 'Windows 11 10.0'),
(NULL, 3, 'cbdc84e1-4f93-4120-b103-347ab2661358-1771548004528', '2026-02-20 00:40:04', '172.24.72.1', '2026-02-20 00:40:04', '2026-03-21 23:40:05', 1, 'Windows 11 10.0'),
(NULL, 3, 'c396de43-585e-4615-8a14-5370eeb38dc1-1771548735252', '2026-02-20 00:52:15', '172.24.72.1', '2026-02-20 00:52:15', '2026-03-21 23:52:15', 1, 'Windows 11 10.0'),
(NULL, 3, 'f158e63a-2e21-4bf4-b43e-83049e8492f9-1771548866959', '2026-02-20 00:54:26', '172.24.72.1', '2026-02-20 00:54:26', '2026-03-21 23:54:27', 1, 'Windows 11 10.0'),
(NULL, 3, '43a15765-4321-4f0f-a286-5fa0ce8bb885-1771549188693', '2026-02-20 00:59:48', '172.24.72.1', '2026-02-20 00:59:48', '2026-03-21 23:59:49', 1, 'Windows 11 10.0'),
(NULL, 3, '140343ad-46e9-456a-954a-f189c2d8d9f3-1771549746232', '2026-02-20 01:09:06', '172.24.72.1', '2026-02-20 01:09:06', '2026-03-22 00:09:06', 1, 'Windows 11 10.0'),
(NULL, 3, 'a8e92a73-5064-42e8-ac55-6d9feef6191e-1771620666476', '2026-02-20 20:51:06', '172.24.72.1', '2026-02-20 20:51:06', '2026-03-22 19:51:06', 1, 'Windows 11 10.0'),
(NULL, 3, '77f1ab7a-335e-4c7b-8650-e728fc2b1837-1771620971095', '2026-02-20 20:56:11', '172.24.72.1', '2026-02-20 20:56:11', '2026-03-22 19:56:11', 1, 'Windows 11 10.0'),
(NULL, 3, '39fbddd8-4ade-44e9-8076-b971c0309ac7-1771621603496', '2026-02-20 21:06:43', '172.24.72.1', '2026-02-20 21:06:43', '2026-03-22 20:06:43', 1, 'Windows 11 10.0'),
(NULL, 3, '7682003b-010a-46a2-966b-bcee80af1e06-1771622135538', '2026-02-20 21:15:35', '172.24.72.1', '2026-02-20 21:15:35', '2026-03-22 20:15:36', 1, 'Windows 11 10.0'),
(NULL, 3, '779d3c68-96e1-4907-83ce-affaf5ce93da-1771622650500', '2026-02-20 21:24:10', '172.24.72.1', '2026-02-20 21:24:10', '2026-03-22 20:24:11', 1, 'Windows 11 10.0'),
(NULL, 3, '97e9c7c7-5f6b-4024-a817-9689db10f90b-1771623343985', '2026-02-20 21:35:43', '172.24.72.1', '2026-02-20 21:35:43', '2026-03-22 20:35:44', 1, 'Windows 11 10.0'),
(NULL, 3, 'f03541c9-6be2-4ee4-b5e9-f7bb04418462-1771625378207', '2026-02-20 22:09:38', '172.24.72.1', '2026-02-20 22:09:38', '2026-03-22 21:09:38', 1, 'Windows 11 10.0'),
(NULL, 3, '33c552c4-9439-41f5-8930-84b621f1e7ec-1771626155031', '2026-02-20 22:22:35', '172.24.72.1', '2026-02-20 22:22:35', '2026-03-22 21:22:35', 1, 'Windows 11 10.0'),
(NULL, 3, '3a84a5dd-b70f-44c1-8d4b-acfcf178dcae-1771628118589', '2026-02-20 22:55:18', '172.24.72.1', '2026-02-20 22:55:18', '2026-03-22 21:55:19', 1, 'Windows 11 10.0'),
(NULL, 3, '506ae6df-23fb-4997-9610-92d96aeb6831-1771629178811', '2026-02-20 23:12:58', '172.24.72.1', '2026-02-20 23:12:58', '2026-03-22 22:12:59', 1, 'Windows 11 10.0'),
(NULL, 3, 'f5b8222d-440a-4444-9a6e-3b89c60ea32b-1771641962383', '2026-02-21 02:46:02', '172.24.72.1', '2026-02-21 02:46:02', '2026-03-23 01:46:02', 1, 'Windows 11 10.0'),
(NULL, 3, '84d64d2c-6faa-4ca0-8dad-8e73215ff4e0-1771642124698', '2026-02-21 02:48:44', '172.24.72.1', '2026-02-21 02:48:44', '2026-03-23 01:48:45', 1, 'Windows 11 10.0'),
(NULL, 3, 'f8219ad6-3a4e-4464-b6a5-7f734ef927de-1771643180864', '2026-02-21 03:06:20', '172.24.72.1', '2026-02-21 03:06:20', '2026-03-23 02:06:21', 1, 'Windows 11 10.0'),
(NULL, 3, 'eab2410b-dc8c-4e50-8e42-5fecc537d5c8-1771643331701', '2026-02-21 03:08:51', '172.24.72.1', '2026-02-21 03:08:51', '2026-03-23 02:08:52', 1, 'Windows 11 10.0'),
(NULL, 3, '60a2ea6b-4b06-4fbc-8b76-0b088c7d8aab-1771644196727', '2026-02-21 03:23:16', '172.24.72.1', '2026-02-21 03:23:16', '2026-03-23 02:23:17', 1, 'Windows 11 10.0'),
(NULL, 3, 'f9e64fb9-720f-4919-b1cc-2f77e1480223-1771644229645', '2026-02-21 03:23:49', '172.24.72.1', '2026-02-21 03:23:49', '2026-03-23 02:23:50', 1, 'Windows 11 10.0'),
(NULL, 3, '5f7101d0-c57f-4b0b-8f92-9d218387a254-1771706884534', '2026-02-21 20:48:04', '172.24.72.1', '2026-02-21 20:48:04', '2026-03-23 19:48:05', 1, 'Windows 11 10.0'),
(NULL, 3, '2f19b1b1-3e04-41ab-b392-8fc3a5185ad8-1771707079729', '2026-02-21 20:51:19', '172.24.72.1', '2026-02-21 20:51:19', '2026-03-23 19:51:20', 1, 'Windows 11 10.0'),
(NULL, 3, '7292ccdb-5aab-47cb-8d1f-704f63d166e9-1771708015371', '2026-02-21 21:06:55', '172.24.72.1', '2026-02-21 21:06:55', '2026-03-23 20:06:55', 1, 'Windows 11 10.0'),
(NULL, 3, '41ec2c7b-2165-4f5a-8cb1-67140e35bc9d-1771708598428', '2026-02-21 21:16:38', '172.24.72.1', '2026-02-21 21:16:38', '2026-03-23 20:16:38', 1, 'Windows 11 10.0'),
(NULL, 3, '7974ffe7-0c4b-423a-9ed4-114c7216064b-1771708881662', '2026-02-21 21:21:21', '172.24.72.1', '2026-02-21 21:21:21', '2026-03-23 20:21:22', 1, 'Windows 11 10.0'),
(NULL, 3, '5e1da9b7-f2f0-4c7f-8b9f-521c3742af37-1771708936817', '2026-02-21 21:22:16', '172.24.72.1', '2026-02-21 21:22:16', '2026-03-23 20:22:17', 1, 'Windows 11 10.0'),
(NULL, 3, 'd56b6367-4f35-4d65-b615-bb7a5c9a2197-1771709027213', '2026-02-21 21:23:47', '172.24.72.1', '2026-02-21 21:23:47', '2026-03-23 20:23:47', 1, 'Windows 11 10.0'),
(NULL, 3, '46e3674c-a813-44a6-a7ed-9c051551ae7b-1771709128058', '2026-02-21 21:25:28', '172.24.72.1', '2026-02-21 21:25:28', '2026-03-23 20:25:28', 1, 'Windows 11 10.0'),
(NULL, 3, '67b355be-396f-4480-83e2-f01dd475fffe-1771709805959', '2026-02-21 21:36:45', '172.24.72.1', '2026-02-21 21:36:45', '2026-03-23 20:36:46', 1, 'Windows 11 10.0'),
(NULL, 3, '991ebf09-1580-4661-bf15-55a7eaaa76cd-1771710334455', '2026-02-21 21:45:34', '172.24.72.1', '2026-02-21 21:45:34', '2026-03-23 20:45:34', 1, 'Windows 11 10.0'),
(NULL, 3, '34dae550-78ce-4448-93d8-0ba027d0a1b0-1771710887949', '2026-02-21 21:54:47', '172.24.72.1', '2026-02-21 21:54:47', '2026-03-23 20:54:48', 1, 'Windows 11 10.0'),
(NULL, 3, '8a74b7de-bda6-43b6-ab07-29195b788429-1771711100145', '2026-02-21 21:58:20', '172.24.72.1', '2026-02-21 21:58:20', '2026-03-23 20:58:20', 1, 'Windows 11 10.0'),
(NULL, 3, 'a94becb4-cb77-4c65-b33b-2296ae01331e-1771711985400', '2026-02-21 22:13:05', '172.24.72.1', '2026-02-21 22:13:05', '2026-03-23 21:13:05', 1, 'Windows 11 10.0'),
(NULL, 3, 'a55be98e-a721-499b-8bfe-0ca5243903e0-1771712271487', '2026-02-21 22:17:51', '172.24.72.1', '2026-02-21 22:17:51', '2026-03-23 21:17:51', 1, 'Windows 11 10.0'),
(NULL, 3, '120170c2-46a2-4155-b276-34fa1b4016c4-1771712358565', '2026-02-21 22:19:18', '172.24.72.1', '2026-02-21 22:19:18', '2026-03-23 21:19:19', 1, 'Windows 11 10.0'),
(NULL, 3, 'a423b659-29e5-4aed-8b83-09aec2c7e045-1771713142755', '2026-02-21 22:32:22', '172.24.72.1', '2026-02-21 22:32:22', '2026-03-23 21:32:23', 1, 'Windows 11 10.0'),
(NULL, 3, 'ff3e65b4-2883-4c5c-af2a-91e0b86d1305-1771713812272', '2026-02-21 22:43:32', '172.24.72.1', '2026-02-21 22:43:32', '2026-03-23 21:43:32', 1, 'Windows 11 10.0'),
(NULL, 3, 'be5da42d-985a-4be4-a3f9-0f48910d25a6-1771721824916', '2026-02-22 00:57:04', '192.168.137.1', '2026-02-22 00:57:04', '2026-03-23 23:57:05', 1, 'Windows 11 10.0'),
(NULL, 3, 'b763b62f-a57a-4777-b07b-0bb441e1d7cb-1771721934798', '2026-02-22 00:58:54', '192.168.137.1', '2026-02-22 00:58:54', '2026-03-23 23:58:55', 1, 'Windows 11 10.0'),
(NULL, 3, 'dcd0e797-0d9f-48cc-be7d-0aebb85bb2e0-1771722183794', '2026-02-22 01:03:03', '192.168.137.1', '2026-02-22 01:03:03', '2026-03-24 00:03:04', 1, 'Windows 11 10.0'),
(NULL, 3, '0eb9bce8-8eb8-440f-906f-4905f6814014-1771722254256', '2026-02-22 01:04:14', '192.168.137.1', '2026-02-22 01:04:14', '2026-03-24 00:04:14', 1, 'Windows 11 10.0'),
(NULL, 3, '4bf76a75-84dc-4b2c-9207-04f67d13cc7a-1771722426643', '2026-02-22 01:07:06', '192.168.137.1', '2026-02-22 01:07:06', '2026-03-24 00:07:07', 1, 'Windows 11 10.0'),
(NULL, 3, '7104f3ce-5f5f-44ab-94f7-537a002ad1ed-1771726313734', '2026-02-22 02:11:53', '172.24.72.1', '2026-02-22 02:11:53', '2026-03-24 01:11:54', 1, 'Windows 11 10.0'),
(NULL, 3, 'ca73d24b-33d5-4661-a40a-90dd98348fae-1771726493109', '2026-02-22 02:14:53', '172.24.72.1', '2026-02-22 02:14:53', '2026-03-24 01:14:53', 1, 'Windows 11 10.0'),
(NULL, 3, '8d0e77a7-3870-4845-ab7a-35ad6ef433b8-1771727792334', '2026-02-22 02:36:32', '172.24.72.1', '2026-02-22 02:36:32', '2026-03-24 01:36:32', 1, 'Windows 11 10.0'),
(NULL, 3, '32f067ca-7479-4319-af01-59c22b5fe7f7-1771728460807', '2026-02-22 02:47:40', '172.24.72.1', '2026-02-22 02:47:40', '2026-03-24 01:47:41', 1, 'Windows 11 10.0'),
(NULL, 3, '17627c36-afa5-4749-b195-bac7033ef9ed-1771730200804', '2026-02-22 03:16:40', '172.24.72.1', '2026-02-22 03:16:40', '2026-03-24 02:16:41', 1, 'Windows 11 10.0'),
(NULL, 3, '426992aa-7ee5-4f11-a7c4-73664baf3039-1771731137797', '2026-02-22 03:32:17', '172.24.72.1', '2026-02-22 03:32:17', '2026-03-24 02:32:18', 1, 'Windows 11 10.0'),
(NULL, 3, '4917036a-f0e5-40f5-bd9f-76b33aed9d30-1771731268839', '2026-02-22 03:34:28', '172.24.72.1', '2026-02-22 03:34:28', '2026-03-24 02:34:29', 1, 'Windows 11 10.0'),
(NULL, 3, '737ed2da-b283-4628-95cb-79dbf424d1ae-1771732102181', '2026-02-22 03:48:22', '172.24.72.1', '2026-02-22 03:48:22', '2026-03-24 02:48:22', 1, 'Windows 11 10.0'),
(NULL, 3, '16f9db54-dc54-407c-a5a6-a98065f90a8f-1771734397893', '2026-02-22 04:26:37', '172.24.72.1', '2026-02-22 04:26:37', '2026-03-24 03:26:38', 1, 'Windows 11 10.0'),
(NULL, 3, '800c1a2b-c2d8-4ae2-a709-d02e994171a2-1771734576614', '2026-02-22 04:29:36', '172.24.72.1', '2026-02-22 04:29:36', '2026-03-24 03:29:37', 1, 'Windows 11 10.0'),
(NULL, 3, '47e0a1fa-9bd6-44f8-bd20-8972b67c954c-1771735065145', '2026-02-22 04:37:45', '172.24.72.1', '2026-02-22 04:37:45', '2026-03-24 03:37:45', 1, 'Windows 11 10.0'),
(NULL, 3, '74e0319a-aba4-42fb-8ba4-695bbda06d94-1771735800543', '2026-02-22 04:50:00', '172.24.72.1', '2026-02-22 04:50:00', '2026-03-24 03:50:01', 1, 'Windows 11 10.0'),
(NULL, 3, '87c110c3-bf1e-4671-a593-9a15891db8ec-1771735935504', '2026-02-22 04:52:15', '172.24.72.1', '2026-02-22 04:52:15', '2026-03-24 03:52:16', 1, 'Windows 11 10.0'),
(NULL, 3, '40c34bde-ec32-403f-9d4a-a67dce9206dd-1771736946689', '2026-02-22 05:09:06', '172.24.72.1', '2026-02-22 05:09:06', '2026-03-24 04:09:07', 1, 'Windows 11 10.0'),
(NULL, 3, 'aee0b5d6-78be-4901-9612-8f09d3c37295-1771737133561', '2026-02-22 05:12:13', '172.24.72.1', '2026-02-22 05:12:13', '2026-03-24 04:12:14', 1, 'Windows 11 10.0'),
(NULL, 3, 'ae9fdb4c-149b-49fb-b029-51b55afc18ea-1771738256277', '2026-02-22 05:30:56', '172.24.72.1', '2026-02-22 05:30:56', '2026-03-24 04:30:56', 1, 'Windows 11 10.0'),
(NULL, 3, '9c2147fb-eedd-4023-9b88-ae65d848d554-1771738517079', '2026-02-22 05:35:17', '172.24.72.1', '2026-02-22 05:35:17', '2026-03-24 04:35:17', 1, 'Windows 11 10.0'),
(NULL, 3, '639b3cea-56d8-4b34-86d7-c3b8ff265f45-1771746049412', '2026-02-22 07:40:49', '172.24.72.1', '2026-02-22 07:40:49', '2026-03-24 06:40:49', 1, 'Windows 11 10.0'),
(NULL, 3, '449728a8-9d0a-49e2-9ae1-f0d92f5dd746-1771746390830', '2026-02-22 07:46:30', '172.24.72.1', '2026-02-22 07:46:30', '2026-03-24 06:46:31', 1, 'Windows 11 10.0'),
(NULL, 3, 'ff76e7fc-b49b-46bd-85dd-51c7343a1a2c-1771748460459', '2026-02-22 08:21:00', '172.24.72.1', '2026-02-22 08:21:00', '2026-03-24 07:21:00', 1, 'Windows 11 10.0'),
(NULL, 3, '3e6bd650-0ce5-452a-8ea9-35b8c537a6d4-1771748765725', '2026-02-22 08:26:05', '172.24.72.1', '2026-02-22 08:26:05', '2026-03-24 07:26:06', 1, 'Windows 11 10.0'),
(NULL, 3, '986fe32d-9f33-4afc-977a-f133b48c8230-1771750146792', '2026-02-22 08:49:06', '172.24.72.1', '2026-02-22 08:49:06', '2026-03-24 07:49:07', 1, 'Windows 11 10.0'),
(NULL, 3, 'e8c74266-7d6b-443b-8a4c-5d33d635faf5-1771750584174', '2026-02-22 08:56:24', '172.24.72.1', '2026-02-22 08:56:24', '2026-03-24 07:56:24', 1, 'Windows 11 10.0'),
(NULL, 3, '36fb7875-5402-4da3-a71a-ad7c6f81b66b-1771750806339', '2026-02-22 09:00:06', '172.24.72.1', '2026-02-22 09:00:06', '2026-03-24 08:00:06', 1, 'Windows 11 10.0'),
(NULL, 3, '6de84b48-7a41-421f-a689-414092228549-1771752264969', '2026-02-22 09:24:24', '172.24.72.1', '2026-02-22 09:24:24', '2026-03-24 08:24:25', 1, 'Windows 11 10.0'),
(NULL, 3, 'fd284cba-2ed8-4b63-a939-892052c2fd35-1771752371936', '2026-02-22 09:26:11', '172.24.72.1', '2026-02-22 09:26:11', '2026-03-24 08:26:12', 1, 'Windows 11 10.0'),
(NULL, 3, '742c2c8c-8cb2-46ac-aab2-720cfa738aad-1771752723871', '2026-02-22 09:32:03', '172.24.72.1', '2026-02-22 09:32:03', '2026-03-24 08:32:04', 1, 'Windows 11 10.0'),
(NULL, 3, '43cf9435-71bf-40ab-8ef4-7abe5b272f10-1771753963580', '2026-02-22 09:52:43', '172.24.72.1', '2026-02-22 09:52:43', '2026-03-24 08:52:44', 1, 'Windows 11 10.0'),
(NULL, 3, 'aa8f8782-9888-42bd-a2e7-78c0604f9209-1771754506760', '2026-02-22 10:01:46', '172.24.72.1', '2026-02-22 10:01:46', '2026-03-24 09:01:47', 1, 'Windows 11 10.0'),
(NULL, 3, '012129d7-eb88-4079-9769-5abfdf47e854-1771754718923', '2026-02-22 10:05:18', '172.24.72.1', '2026-02-22 10:05:18', '2026-03-24 09:05:19', 1, 'Windows 11 10.0'),
(NULL, 3, 'f76128ef-df7b-4bde-a5cd-b86be85ae8e9-1771754797158', '2026-02-22 10:06:37', '172.24.72.1', '2026-02-22 10:06:37', '2026-03-24 09:06:37', 1, 'Windows 11 10.0'),
(NULL, 3, 'a0bfb8cb-4d80-41fe-b3ad-f22db1ba6de8-1771754901913', '2026-02-22 10:08:21', '172.24.72.1', '2026-02-22 10:08:21', '2026-03-24 09:08:22', 1, 'Windows 11 10.0'),
(NULL, 3, '98d68d39-94e6-4e94-b8eb-8b3e69969a00-1771754931469', '2026-02-22 10:08:51', '172.24.72.1', '2026-02-22 10:08:51', '2026-03-24 09:08:51', 1, 'Windows 11 10.0'),
(NULL, 3, 'cace97ac-846e-48c3-b6fa-00395f3d259f-1771755138901', '2026-02-22 10:12:18', '172.24.72.1', '2026-02-22 10:12:18', '2026-03-24 09:12:19', 1, 'Windows 11 10.0'),
(NULL, 3, 'eff64077-28d5-40c0-988c-65e7bdc8a1f0-1771755211848', '2026-02-22 10:13:31', '172.24.72.1', '2026-02-22 10:13:31', '2026-03-24 09:13:32', 1, 'Windows 11 10.0'),
(NULL, 3, '438b878b-4677-4cc8-9720-e92845abd9a3-1771755426325', '2026-02-22 10:17:06', '172.24.72.1', '2026-02-22 10:17:06', '2026-03-24 09:17:06', 1, 'Windows 11 10.0'),
(NULL, 3, 'a82430ac-3afd-401a-8777-c95874984b33-1771755538856', '2026-02-22 10:18:58', '172.24.72.1', '2026-02-22 10:18:58', '2026-03-24 09:18:59', 1, 'Windows 11 10.0'),
(NULL, 3, '0a3db24b-6c65-4a12-b867-ec889b10f192-1771756075055', '2026-02-22 10:27:55', '172.24.72.1', '2026-02-22 10:27:55', '2026-03-24 09:27:55', 1, 'Windows 11 10.0'),
(NULL, 3, '85061510-7e94-4fab-a3cd-15023f59c724-1771757354372', '2026-02-22 10:49:14', '172.24.72.1', '2026-02-22 10:49:14', '2026-03-24 09:49:14', 1, 'Windows 11 10.0'),
(NULL, 3, '864c8821-77f7-4da6-9da4-9c48d43d471d-1771757968719', '2026-02-22 10:59:28', '172.24.72.1', '2026-02-22 10:59:28', '2026-03-24 09:59:29', 1, 'Windows 11 10.0'),
(NULL, 3, '41a792bb-e7ca-4dc9-8c1d-237e587782bb-1771782270263', '2026-02-22 17:44:30', '172.24.72.1', '2026-02-22 17:44:30', '2026-03-24 16:44:30', 1, 'Windows 11 10.0'),
(NULL, 3, '2a076d32-6aac-4578-b8d4-9d893e2e978a-1771784289238', '2026-02-22 18:18:09', '172.24.72.1', '2026-02-22 18:18:09', '2026-03-24 17:18:09', 1, 'Windows 11 10.0'),
(NULL, 3, '79614d20-4100-421b-a525-e8c967b074b5-1771811498833', '2026-02-23 01:51:38', '172.24.72.1', '2026-02-23 01:51:38', '2026-03-25 00:51:39', 1, 'Windows 11 10.0'),
(NULL, 3, 'fda72a69-4d1c-4fff-8df4-8c77f5a41054-1771811827026', '2026-02-23 01:57:07', '172.24.72.1', '2026-02-23 01:57:07', '2026-03-25 00:57:07', 1, 'Windows 11 10.0'),
(NULL, 3, '0f8ee319-6479-46f2-aab2-caa3cb7d2024-1771812188895', '2026-02-23 02:03:08', '172.24.72.1', '2026-02-23 02:03:08', '2026-03-25 01:03:09', 1, 'Windows 11 10.0'),
(NULL, 3, '8d788aba-1f1e-47d4-9e66-10ec33934bb6-1771894107429', '2026-02-24 00:53:14', '172.24.72.1', '2026-02-24 00:53:14', '2026-03-25 23:48:27', 0, 'Windows 11 10.0'),
(NULL, 3, 'c93e1f4f-5eea-4af2-9d9d-712737bd10ae-1771920186979', '2026-02-24 08:03:07', '172.24.72.1', '2026-02-24 08:03:07', '2026-03-26 07:03:07', 1, 'Windows 11 10.0'),
(NULL, 3, 'fcfbf60b-633d-4d62-9511-5bfe5d9b7dfa-1771920460556', '2026-02-24 08:09:52', '172.24.72.1', '2026-02-24 08:09:52', '2026-03-26 07:07:41', 0, 'Windows 11 10.0'),
(NULL, 3, '5f15fb7b-0d71-4aef-896e-4948554dad65-1771921132047', '2026-02-24 08:20:48', '172.24.72.1', '2026-02-24 08:20:48', '2026-03-26 07:18:52', 0, 'Windows 11 10.0'),
(NULL, 3, '2321681a-e839-45a3-be0a-53563a57e040-1771921472647', '2026-02-24 08:24:32', '172.24.72.1', '2026-02-24 08:24:32', '2026-03-26 07:24:33', 1, 'Windows 11 10.0'),
(NULL, 3, '3a12e53d-1dce-4976-be05-019f4b238695-1771921877329', '2026-02-24 08:31:17', '172.24.72.1', '2026-02-24 08:31:17', '2026-03-26 07:31:17', 1, 'Windows 11 10.0'),
(NULL, 3, 'dc535f68-1eb3-4d81-b86c-e26d65d69fd3-1771942616397', '2026-02-24 14:16:56', '172.24.72.1', '2026-02-24 14:16:56', '2026-03-26 13:16:56', 1, 'Windows 11 10.0'),
(NULL, 3, '501a286f-6c14-4a9f-a691-c960cc0aee65-1771942785234', '2026-02-24 14:19:45', '172.24.72.1', '2026-02-24 14:19:45', '2026-03-26 13:19:45', 1, 'Windows 11 10.0'),
(NULL, 3, 'f8a4c646-4de3-4d4e-819c-9354c1e00065-1772057081900', '2026-02-25 22:04:41', '172.24.72.1', '2026-02-25 22:04:41', '2026-03-27 21:04:42', 1, 'Windows 11 10.0'),
(NULL, 3, '2d1bf23c-50e7-4b70-b1dc-4969ad8d8b88-1772057469501', '2026-02-25 22:11:09', '172.24.72.1', '2026-02-25 22:11:09', '2026-03-27 21:11:10', 1, 'Windows 11 10.0'),
(NULL, 3, '5bbadfea-b3fa-4119-8056-3e72f345c1f0-1772057578559', '2026-02-25 22:12:58', '172.24.72.1', '2026-02-25 22:12:58', '2026-03-27 21:12:59', 1, 'Windows 11 10.0'),
(NULL, 3, '92b09e03-6302-41cd-82cd-bb7eac29cf38-1772057729232', '2026-02-25 22:15:29', '172.24.72.1', '2026-02-25 22:15:29', '2026-03-27 21:15:29', 1, 'Windows 11 10.0'),
(NULL, 3, 'd3b667b6-c4a8-4e9d-8366-0f73207283ab-1772057846220', '2026-02-25 22:17:26', '172.24.72.1', '2026-02-25 22:17:26', '2026-03-27 21:17:26', 1, 'Windows 11 10.0'),
(NULL, 3, '8e412be3-ff65-4e8a-bc26-657c7312e2a6-1772058317428', '2026-02-25 22:25:17', '172.24.72.1', '2026-02-25 22:25:17', '2026-03-27 21:25:17', 1, 'Windows 11 10.0'),
(NULL, 3, '2f603f12-e85f-457e-b13e-b905ece16aae-1772058608679', '2026-02-25 22:30:08', '172.24.72.1', '2026-02-25 22:30:08', '2026-03-27 21:30:09', 1, 'Windows 11 10.0'),
(NULL, 3, 'a0b79dd8-8f7f-4160-b9b5-8e6cc095eef2-1772059125054', '2026-02-25 22:38:45', '172.24.72.1', '2026-02-25 22:38:45', '2026-03-27 21:38:45', 1, 'Windows 11 10.0'),
(NULL, 3, 'edb66718-104f-49bf-88fc-298ad1ed84be-1772067900178', '2026-02-26 01:05:00', '172.24.72.1', '2026-02-26 01:05:00', '2026-03-28 00:05:00', 1, 'Windows 11 10.0'),
(NULL, 3, '16ebbfad-df53-4928-9f72-216437d49418-1772067939285', '2026-02-26 01:05:39', '172.24.72.1', '2026-02-26 01:05:39', '2026-03-28 00:05:39', 1, 'Windows 11 10.0'),
(NULL, 3, '99ab8b8f-342e-4584-b828-90552d9a4d30-1772068021403', '2026-02-26 01:07:01', '172.24.72.1', '2026-02-26 01:07:01', '2026-03-28 00:07:01', 1, 'Windows 11 10.0'),
(NULL, 3, 'd9b0c775-f440-424f-b60f-780324a6e76c-1772068374377', '2026-02-26 01:12:54', '172.24.72.1', '2026-02-26 01:12:54', '2026-03-28 00:12:54', 1, 'Windows 11 10.0'),
(NULL, 3, 'ddf74ecb-9dc0-4d01-a8b7-03e94b3910ea-1772068415501', '2026-02-26 01:13:35', '172.24.72.1', '2026-02-26 01:13:35', '2026-03-28 00:13:36', 1, 'Windows 11 10.0'),
(NULL, 3, 'bc47b55c-7b96-40aa-9c0a-a47abc24c52b-1772069447442', '2026-02-26 01:30:47', '172.24.72.1', '2026-02-26 01:30:47', '2026-03-28 00:30:47', 1, 'Windows 11 10.0'),
(NULL, 3, '51eecc06-2072-46f6-97de-b17b1c228889-1772070428794', '2026-02-26 01:47:08', '172.24.72.1', '2026-02-26 01:47:08', '2026-03-28 00:47:09', 1, 'Windows 11 10.0'),
(NULL, 3, '0ac48cd1-c3d8-4a68-9dba-c8f0804033be-1772107504459', '2026-02-26 12:05:04', '172.24.72.1', '2026-02-26 12:05:04', '2026-03-28 11:05:04', 1, 'Windows 11 10.0'),
(NULL, 3, 'af1fdea2-bcfd-4c27-ac5b-fbf12d3c7f7e-1772142685472', '2026-02-26 21:51:25', '172.24.72.1', '2026-02-26 21:51:25', '2026-03-28 20:51:25', 1, 'Windows 11 10.0'),
(NULL, 3, '0e19bf2d-e6ee-489f-b97c-a22c7b84148f-1772144020041', '2026-02-26 22:13:40', '172.24.72.1', '2026-02-26 22:13:40', '2026-03-05 21:13:40', 1, 'Windows 11 10.0'),
(NULL, 3, '1dbaeac0-b332-4a6a-955f-8a1da8495250-1772144020998', '2026-02-26 22:13:41', '172.24.72.1', '2026-02-26 22:13:41', '2026-03-05 21:13:41', 1, 'Windows 11 10.0'),
(NULL, 3, '9527cf94-39b2-4384-9be7-0e35bbbdf248-1772144021465', '2026-02-26 22:13:41', '172.24.72.1', '2026-02-26 22:13:41', '2026-03-05 21:13:41', 1, 'Windows 11 10.0'),
(NULL, 3, '8b35652b-4cd1-4072-b477-030dd2b19a17-1772144021787', '2026-02-26 22:13:41', '172.24.72.1', '2026-02-26 22:13:41', '2026-03-05 21:13:42', 1, 'Windows 11 10.0'),
(NULL, 3, 'a9d99e48-903b-4a23-b271-cff680eb9d84-1772144021990', '2026-02-26 22:16:46', '172.24.72.1', '2026-02-26 22:16:46', '2026-03-05 21:13:42', 0, 'Windows 11 10.0'),
(NULL, 3, '2f38d580-4dc5-4e77-9c7c-81138b8ce4bf-1772144215491', '2026-02-26 23:20:42', '172.24.72.1', '2026-02-26 23:20:42', '2026-03-05 21:16:55', 0, 'Windows 11 10.0'),
(NULL, 10, 'b5e8ebfc-b69f-45a6-827e-aa2294a4283d-1772148057976', '2026-02-26 23:21:16', '172.24.72.1', '2026-02-26 23:21:16', '2026-03-05 22:20:58', 0, 'Windows 11 10.0'),
(NULL, 10, '0c13ac18-1053-4285-874e-c19273c0e230-1772148263396', '2026-02-26 23:29:52', '172.24.72.1', '2026-02-26 23:29:52', '2026-03-05 22:24:23', 1, 'Windows 11 10.0'),
(NULL, 8, '52b9e1ce-d9ee-4f95-8e8c-977385f4f824-1772153740161', '2026-02-27 00:57:22', '172.24.72.1', '2026-02-27 00:57:22', '2026-03-05 23:55:40', 0, 'Windows 11 10.0'),
(NULL, 3, '4160fdec-b72d-425e-8feb-956b75062ae0-1772153890765', '2026-02-27 00:58:10', '172.24.72.1', '2026-02-27 00:58:10', '2026-03-05 23:58:11', 1, 'Windows 11 10.0'),
(NULL, 10, '47359678-4218-4ab5-a08a-6e9072ce8665-1772157101615', '2026-02-27 01:54:36', '172.24.72.1', '2026-02-27 01:54:36', '2026-03-06 00:51:42', 1, 'Windows 11 10.0'),
(NULL, 10, '0249612c-7973-4517-b36f-673eb8d1a2dc-1772237927028', '2026-02-28 00:18:47', '172.24.72.1', '2026-02-28 00:18:47', '2026-03-06 23:18:47', 1, 'Windows 11 10.0'),
(NULL, 10, 'a282791f-11d5-4a20-88c3-fb9eb75cc9f7-1772240558073', '2026-02-28 01:06:24', '172.24.72.1', '2026-02-28 01:06:24', '2026-03-07 00:02:38', 1, 'Windows 11 10.0'),
(NULL, 3, 'eb9e39f0-4737-4acd-bcf8-097f5cad745e-1772242607650', '2026-02-28 01:37:16', '172.24.72.1', '2026-02-28 01:37:16', '2026-03-07 00:36:48', 1, 'Windows 11 10.0'),
(NULL, 3, '5a2ab2eb-3877-42f7-a7cf-d1e9760d0874-1772245397452', '2026-02-28 02:41:58', '172.24.72.1', '2026-02-28 02:41:58', '2026-03-07 01:23:17', 1, 'Windows 11 10.0'),
(NULL, 3, '1db7ad26-8591-4a31-80bc-8f980e66a33f-1772246819662', '2026-02-28 03:32:32', '172.24.72.1', '2026-02-28 03:32:32', '2026-03-07 01:47:00', 1, 'Windows 11 10.0'),
(NULL, 3, '0ccf585d-e311-4568-a478-a643b915f698-1772251529685', '2026-02-28 04:17:15', '172.24.72.1', '2026-02-28 04:17:15', '2026-03-07 03:05:30', 0, 'Windows 11 10.0'),
(NULL, 10, 'b6e13a40-95f2-4c94-bdaf-b688a0c6582e-1772252263624', '2026-02-28 04:21:15', '172.24.72.1', '2026-02-28 04:21:15', '2026-03-07 03:17:44', 0, 'Windows 11 10.0'),
(NULL, 3, '4bc60ce1-8601-4f79-bda4-d4c7f4817b0b-1772252513127', '2026-02-28 04:21:53', '172.24.72.1', '2026-02-28 04:21:53', '2026-03-07 03:21:53', 1, 'Windows 11 10.0'),
(NULL, 3, 'aa18110d-9c34-4d87-a605-73f0c721a8cf-1772253310094', '2026-02-28 04:52:53', '172.24.72.1', '2026-02-28 04:52:53', '2026-03-07 03:35:10', 1, 'Windows 11 10.0'),
(NULL, 3, '68b395f9-cc3e-4f20-b3cb-989ac6b967d3-1772254718319', '2026-02-28 05:04:22', '172.24.72.1', '2026-02-28 05:04:22', '2026-03-07 03:58:38', 0, 'Windows 11 10.0'),
(NULL, 10, '26becc43-9fe6-4bed-b08e-cc529628ea64-1772255082248', '2026-02-28 05:04:42', '172.24.72.1', '2026-02-28 05:04:42', '2026-03-07 04:04:42', 1, 'Windows 11 10.0'),
(NULL, 10, '41ed13ab-980f-49ab-9ea6-7d150aed4c3c-1772255148666', '2026-02-28 05:05:48', '172.24.72.1', '2026-02-28 05:05:48', '2026-03-07 04:05:49', 1, 'Windows 11 10.0'),
(NULL, 7, '777a17cb-04b5-4d12-aaf7-569bb74ecde6-1772315814991', '2026-02-28 21:57:22', '172.24.72.1', '2026-02-28 21:57:22', '2026-03-07 20:56:55', 0, 'Windows 11 10.0'),
(NULL, 3, 'a01dc6cb-f2e9-4f1a-b4cd-21f87ccede38-1772315849266', '2026-02-28 22:56:58', '172.24.72.1', '2026-02-28 22:56:58', '2026-03-07 20:57:29', 1, 'Windows 11 10.0'),
(NULL, 3, 'a15a63f5-10f3-489b-a3c0-0c5a2dcc0f58-1772332767556', '2026-03-01 02:39:27', '172.24.72.1', '2026-03-01 02:39:27', '2026-03-08 01:39:28', 1, 'Windows 11 10.0'),
(NULL, 3, 'be2bb4a3-55a7-4ca3-ad3c-8e09a5fcfce8-1772335127499', '2026-03-01 03:33:07', '172.24.72.1', '2026-03-01 03:33:07', '2026-03-08 02:18:48', 0, 'Windows 11 10.0'),
(NULL, 3, 'e0713341-83ac-4e52-a743-cb40b739dd52-1772335995216', '2026-03-01 03:44:47', '172.24.72.1', '2026-03-01 03:44:47', '2026-03-08 02:33:15', 0, 'Windows 11 10.0'),
(NULL, 7, 'e9b82a7e-7e60-41d8-a801-3bc14890402d-1772336695852', '2026-03-01 03:45:04', '172.24.72.1', '2026-03-01 03:45:04', '2026-03-08 02:44:56', 0, 'Windows 11 10.0'),
(NULL, 3, 'abe8e794-08b3-4ec1-bed5-f01d45caa75e-1772337354179', '2026-03-01 05:21:17', '172.24.72.1', '2026-03-01 05:21:17', '2026-03-08 02:55:54', 1, 'Windows 11 10.0'),
(NULL, 3, '5c08e333-d32a-4ea5-ba3f-e90d26284724-1772343183040', '2026-03-01 05:40:02', '172.24.72.1', '2026-03-01 05:40:02', '2026-03-08 04:33:03', 1, 'Windows 11 10.0'),
(NULL, 3, '9c8023e7-34eb-4df7-a3fd-5717f4309a48-1772343610742', '2026-03-01 05:40:10', '172.24.72.1', '2026-03-01 05:40:10', '2026-03-08 04:40:11', 1, 'Windows 11 10.0'),
(NULL, 3, 'ec7dff16-fa22-434b-83e3-b4f24a7407ae-1772343614297', '2026-03-01 05:40:14', '172.24.72.1', '2026-03-01 05:40:14', '2026-03-08 04:40:14', 1, 'Windows 11 10.0'),
(NULL, 3, '9b2e71b5-56ae-4ce5-b07d-aa9b3ea758ac-1772343615026', '2026-03-01 05:41:01', '172.24.72.1', '2026-03-01 05:41:01', '2026-03-08 04:40:15', 1, 'Windows 11 10.0'),
(NULL, 3, 'ef106cbf-08d5-4017-90ab-60f18424914c-1772343671557', '2026-03-01 05:42:13', '172.24.72.1', '2026-03-01 05:42:13', '2026-03-08 04:41:12', 1, 'Windows 11 10.0'),
(NULL, 3, 'f9696c2b-ce7c-4df1-84ff-047ec108060a-1772382223149', '2026-03-01 16:57:43', '172.24.72.1', '2026-03-01 16:57:43', '2026-03-08 15:23:43', 1, 'Windows 11 10.0');

--
-- Constraints for dumped tables
--

--
-- Constraints for table `evaluation_performance`
--
ALTER TABLE `evaluation_performance`
  ADD CONSTRAINT `fk_eval_affectation` FOREIGN KEY (`id_affectation`) REFERENCES `affectation_travail` (`id_affectation`) ON DELETE CASCADE;

--
-- Constraints for table `produit`
--
ALTER TABLE `produit`
  ADD CONSTRAINT `fk_produit_user` FOREIGN KEY (`agriculteur_id`) REFERENCES `user` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `stock`
--
ALTER TABLE `stock`
  ADD CONSTRAINT `fk_stock_produit` FOREIGN KEY (`produit_id`) REFERENCES `produit` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `user_sessions`
--
ALTER TABLE `user_sessions`
  ADD CONSTRAINT `fk_user_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
