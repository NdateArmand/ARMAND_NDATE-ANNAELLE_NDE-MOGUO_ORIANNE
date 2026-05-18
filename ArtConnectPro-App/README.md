# ArtConnect Pro

Application de gestion d'une communauté artistique — Projet académique TI603  
**EFREI Paris** · NDE MAMA · NDATE ARMAND · MOGUO ARIANE  
Supervisé par Mme Lydia CHIBOUT

---

## Présentation

ArtConnect Pro est une application de bureau JavaFX connectée à une base de données MySQL.
Elle permet de gérer artistes, œuvres, galeries, expositions, ateliers et membres d'une
communauté artistique, avec un système de rôles complet (Organisateur, Artiste, Membre, Visiteur).

---

## Prérequis

| Outil | Version minimale |
|-------|-----------------|
| Java (JDK) | 17 |
| JavaFX | 17 |
| Maven | 3.8+ |
| MySQL | 8.0+ |
| IntelliJ IDEA | 2022+ (recommandé) |

---

## Installation de la base de données

### 1. Créer la base et les tables

Ouvrir MySQL Workbench et exécuter dans l'ordre :

```sql
-- 1. Création de la base et des tables
SOURCE bdartconnect.sql;

-- 2. Triggers, procédures stockées et vues
SOURCE artconnect_triggers_procedures.sql;

-- 3. Correction des statuts des œuvres (valeurs Java)
SET SQL_SAFE_UPDATES = 0;
UPDATE oeuvre SET statut = 'FOR_SALE'  WHERE statut = 'disponible';
UPDATE oeuvre SET statut = 'SOLD'      WHERE statut = 'vendu';
UPDATE oeuvre SET statut = 'EXHIBITED' WHERE statut IN ('exposé', 'expose');
SET SQL_SAFE_UPDATES = 1;
```

### 2. Migration des relations artiste (OBLIGATOIRE)

Ces scripts ajoutent `id_artiste` dans les tables `OEUVRE` et `ATELIER` pour corriger
l'affichage de toutes les œuvres et ateliers dans l'interface :

```sql
USE artconnect;
SET SQL_SAFE_UPDATES = 0;

-- Ajouter id_artiste dans OEUVRE
ALTER TABLE oeuvre
    ADD COLUMN id_artiste INT NULL,
    ADD CONSTRAINT fk_oeuvre_artiste
        FOREIGN KEY (id_artiste) REFERENCES artiste(id_artiste) ON DELETE SET NULL;

UPDATE oeuvre o
JOIN artiste a ON a.id_oeuvre = o.id_oeuvre
SET o.id_artiste = a.id_artiste;

-- Ajouter id_artiste dans ATELIER
ALTER TABLE atelier
    ADD COLUMN id_artiste INT NULL,
    ADD CONSTRAINT fk_atelier_artiste
        FOREIGN KEY (id_artiste) REFERENCES artiste(id_artiste) ON DELETE SET NULL;

UPDATE atelier at
JOIN artiste a ON a.id_atelier = at.id_atelier
SET at.id_artiste = a.id_artiste;

SET SQL_SAFE_UPDATES = 1;
```

### 3. Configurer les mots de passe

```sql
-- Organisateur
UPDATE organisateur SET mot_passe = SHA2('admin123', 256);

-- Artistes  (format : art_[nomminuscule])
UPDATE artiste SET mot_passe = SHA2(CONCAT('art_', LOWER(REPLACE(nom,' ',''))), 256);

-- Membres   (format : mem_[nomminuscule])
UPDATE membre_communaute SET mot_passe = SHA2(CONCAT('mem_', LOWER(REPLACE(nom,' ',''))), 256);
```

### 4. Configurer la connexion JDBC

Modifier le fichier :
```
src/main/java/com/project/artconnect/util/DatabaseConfig.java
```

```java
public static final String URL      = "jdbc:mysql://localhost:3306/artconnect";
public static final String USER     = "root";       // votre utilisateur MySQL
public static final String PASSWORD = "votre_mdp";  // votre mot de passe MySQL
```

---

## Lancement de l'application

### Via le terminal IntelliJ

```bash
mvn javafx:run
```

### Via le panneau Maven d'IntelliJ

`Maven` (panneau droit) → `Plugins` → `javafx` → double-clic sur `javafx:run`

### Via une configuration Run

`Add Configuration` → `+` → `Maven` → Command line : `javafx:run` → `OK` → ▶

---

## Comptes de test

| Rôle | Email | Mot de passe |
|------|-------|-------------|
| Organisateur | marie.dupont@artconnect.fr | admin123 |
| Artiste | julien.mercier@art.fr | art_julienmercier |
| Membre | sophie.martin@email.fr | mem_sophiemartin |
| Visiteur | *(aucune connexion requise)* | — |

---

## Fonctionnalités par rôle

### Visiteur (non connecté)
- Consultation de tous les onglets sauf Community
- Lecture seule — aucun formulaire CRUD visible

### Membre
- Tout ce que voit le visiteur
- Onglet Community : modification de son propre profil uniquement (tableau masqué)
- Onglet Exhibitions : réservation / annulation, colonne statut `✅ Réservé / ⬜ Non réservé`
- Onglet Workshops : inscription / désinscription, colonne statut `✅ Inscrit / ⬜ Non inscrit`

### Artiste
- Onglet Artists : formulaire CRUD pré-sélectionné sur son propre profil
- Onglet Artworks : CRUD limité à ses propres œuvres (artiste verrouillé)
- Onglet Workshops : CRUD limité à ses propres ateliers + label inscrits au clic
- Onglet Community : masqué

### Organisateur
- CRUD complet sur tous les onglets (Artists, Artworks, Galleries, Exhibitions, Workshops, Community)
- Onglet Exhibitions : colonne "Réservés" + liste nominative des membres au clic
- Onglet Workshops : colonne "Inscrits" + liste nominative des membres au clic
- Onglet Community : gestion complète + modification des noms des membres

---

## Architecture technique

```
src/main/java/com/project/artconnect/
├── MainApp.java                    ← Point d'entrée JavaFX
├── model/                          ← Entités (Artist, Artwork, Gallery…)
│   ├── Session.java                ← Singleton de session utilisateur
│   └── UserRole.java               ← Enum PUBLIC / MEMBRE / ARTISTE / ORGANISATEUR
├── dao/                            ← Interfaces DAO (ArtistDao, ArtworkDao…)
│   └── impl/DaoFactory.java        ← Fabrique des DAO JDBC
├── persistence/                    ← Implémentations JDBC (JdbcXxxDao)
├── service/                        ← Interfaces Service
│   ├── AuthService.java            ← Authentification (3 tables : artiste, membre, organisateur)
│   └── impl/JdbcXxxService.java    ← Implémentations JDBC
├── ui/                             ← Controllers JavaFX
│   ├── RoleAware.java              ← Interface applyRole() — appelée après chaque login/logout
│   ├── MainController.java         ← Navigation + gestion login/logout
│   ├── LoginController.java        ← Formulaire de connexion (popup modale)
│   └── [Xxx]Controller.java        ← Un controller par onglet
└── util/
    ├── ConnectionManager.java      ← Connexion JDBC
    ├── DatabaseConfig.java         ← Paramètres de connexion
    └── ServiceProvider.java        ← Injection des services
```

---

## Objets base de données utilisés dans l'interface

| Objet | Type | Onglet | Rôle |
|-------|------|--------|------|
| `trg_verif_dates_exposition` | Trigger BEFORE INSERT | Exhibitions | Bloque date fin < date début |
| `trg_verif_dates_exposition_update` | Trigger BEFORE UPDATE | Exhibitions | Même vérification à la modification |
| `trg_audit_modification_exposition` | Trigger AFTER UPDATE | Exhibitions | Traçabilité dans AUDIT_EXPOSITION |
| `trg_statut_oeuvre_exposition` | Trigger AFTER INSERT | Artworks | Statut → EXHIBITED automatiquement |
| `trg_statut_oeuvre_retrait` | Trigger AFTER DELETE | Artworks | Statut → FOR_SALE automatiquement |
| `trg_verif_places_atelier` | Trigger BEFORE INSERT | Workshops | Bloque si atelier complet |
| `vue_oeuvres_artistes` | Vue | Artists | Jointure artiste + disciplines |
| `vue_programme_expositions` | Vue | Galleries | Expositions par galerie |
| `vue_membres_sans_mdp` | Vue | Community | Audit comptes incomplets |
| `inscrire_membre` | Procédure stockée | Workshops | Inscription depuis MySQL |
| `exposition_en_cours` | Fonction | Exhibitions | Vérifie si exposition active |
| `nb_participants_exposition` | Fonction | Exhibitions | Compte les réservations |
| `places_disponibles_atelier` | Fonction | Workshops | Places restantes |
| Transactions commit/rollback | — | Tous | Atomicité de toutes les écritures JDBC |

---

## Structure des fichiers FXML

```
src/main/resources/com/project/artconnect/ui/
├── MainView.fxml          ← Fenêtre principale + TabPane + boutons connexion
├── LoginView.fxml         ← Popup de connexion modale
├── DiscoverTab.fxml
├── ArtistsTab.fxml
├── ArtworksTab.fxml
├── GalleriesTab.fxml
├── ExhibitionsTab.fxml
├── WorkshopsTab.fxml
└── CommunityTab.fxml
```

---

## Scripts SQL fournis

| Fichier | Contenu |
|---------|---------|
| `bd_artconnect.sql` | Création de la base, tables et données initiales |
| `artconnect_triggers_procedures.sql` | Triggers, procédures stockées, fonctions et vues |

---

*Projet TI603 — Base de données 2 — EFREI Paris 2025-2026*
