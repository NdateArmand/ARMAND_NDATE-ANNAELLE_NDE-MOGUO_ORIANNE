-- ============================================================
-- ArtConnect Pro — Objets base de données
-- Triggers, Procédures stockées, Fonctions
-- À exécuter APRÈS bdartconnect.sql
-- ============================================================

USE artconnect;
SET SQL_SAFE_UPDATES = 0;

-- ============================================================
-- 1. CORRECTION DES STATUTS DES ŒUVRES
-- Le modèle Java utilise : FOR_SALE, SOLD, EXHIBITED
-- ============================================================

UPDATE oeuvre SET statut = 'SOLD'      WHERE statut = 'vendu';
UPDATE oeuvre SET statut = 'EXHIBITED' WHERE statut IN ('exposé', 'expose');
UPDATE oeuvre SET statut = 'FOR_SALE'  WHERE statut = 'disponible';

-- ============================================================
-- 2. MOTS DE PASSE (si pas déjà fait)
-- ============================================================

-- Artistes : format art_[nomminuscule]
UPDATE artiste
SET mot_passe = SHA2(CONCAT('art_', LOWER(REPLACE(nom, ' ', ''))), 256)
WHERE mot_passe IS NULL;

-- Membres : format mem_[nomminuscule]
UPDATE membre_communaute
SET mot_passe = SHA2(CONCAT('mem_', LOWER(REPLACE(nom, ' ', ''))), 256)
WHERE mot_passe IS NULL;

-- Organisateur : admin123
UPDATE organisateur
SET mot_passe = SHA2('admin123', 256);

SET SQL_SAFE_UPDATES = 1;

-- ============================================================
-- 3. TRIGGERS
-- ============================================================

DELIMITER $$

-- Trigger 1 : Vérification dates exposition (INSERT)
DROP TRIGGER IF EXISTS trg_verif_dates_exposition$$
CREATE TRIGGER trg_verif_dates_exposition
BEFORE INSERT ON exposition
FOR EACH ROW
BEGIN
    IF NEW.date_fin < NEW.date_debut THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Erreur : la date de fin doit être postérieure ou égale à la date de début.';
    END IF;
END$$

-- Trigger 2 : Vérification dates exposition (UPDATE)
DROP TRIGGER IF EXISTS trg_verif_dates_exposition_update$$
CREATE TRIGGER trg_verif_dates_exposition_update
BEFORE UPDATE ON exposition
FOR EACH ROW
BEGIN
    IF NEW.date_fin < NEW.date_debut THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Erreur : la date de fin doit être postérieure ou égale à la date de début.';
    END IF;
END$$

-- Trigger 3 : Vérification places atelier avant inscription
DROP TRIGGER IF EXISTS trg_verif_places_atelier$$
CREATE TRIGGER trg_verif_places_atelier
BEFORE INSERT ON inscription
FOR EACH ROW
BEGIN
    DECLARE places_prises INT;
    DECLARE places_max    INT;

    SELECT COALESCE(SUM(nb_places), 0) INTO places_prises
      FROM inscription
     WHERE id_atelier  = NEW.id_atelier
       AND statut_paie != 'annulé';

    SELECT participants_max INTO places_max
      FROM atelier
     WHERE id_atelier = NEW.id_atelier;

    IF (places_prises + NEW.nb_places) > places_max THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Erreur : nombre maximum de participants atteint.';
    END IF;
END$$

-- Trigger 4 : Statut œuvre → EXHIBITED quand associée à une exposition
DROP TRIGGER IF EXISTS trg_statut_oeuvre_exposition$$
CREATE TRIGGER trg_statut_oeuvre_exposition
AFTER INSERT ON exposer
FOR EACH ROW
BEGIN
    UPDATE oeuvre
       SET statut = 'EXHIBITED'
     WHERE id_oeuvre = NEW.id_oeuvre;
END$$

-- Trigger 5 : Statut œuvre → FOR_SALE quand retirée de toutes les expositions
DROP TRIGGER IF EXISTS trg_statut_oeuvre_retrait$$
CREATE TRIGGER trg_statut_oeuvre_retrait
AFTER DELETE ON exposer
FOR EACH ROW
BEGIN
    DECLARE nb_expos INT;

    SELECT COUNT(*) INTO nb_expos
      FROM exposer
     WHERE id_oeuvre = OLD.id_oeuvre;

    IF nb_expos = 0 THEN
        UPDATE oeuvre
           SET statut = 'FOR_SALE'
         WHERE id_oeuvre = OLD.id_oeuvre;
    END IF;
END$$

-- Trigger 6 : Audit des modifications d'exposition (titre + dates)
DROP TRIGGER IF EXISTS trg_audit_modification_exposition$$
CREATE TRIGGER trg_audit_modification_exposition
AFTER UPDATE ON exposition
FOR EACH ROW
BEGIN
    IF OLD.titre      != NEW.titre
    OR OLD.date_debut != NEW.date_debut
    OR OLD.date_fin   != NEW.date_fin
    THEN
        INSERT INTO audit_exposition
            (id_exposition, ancien_titre, nouveau_titre,
             ancienne_date_debut, nouvelle_date_debut,
             ancienne_date_fin,   nouvelle_date_fin)
        VALUES
            (OLD.id_exposition,
             OLD.titre,      NEW.titre,
             OLD.date_debut, NEW.date_debut,
             OLD.date_fin,   NEW.date_fin);
    END IF;
END$$

-- ============================================================
-- 4. PROCÉDURES STOCKÉES
-- ============================================================

-- Procédure 1 : Inscrire un membre à un atelier
DROP PROCEDURE IF EXISTS inscrire_membre$$
CREATE PROCEDURE inscrire_membre(
    IN  p_id_membre  INT,
    IN  p_id_atelier INT,
    OUT p_message    VARCHAR(255)
)
BEGIN
    DECLARE v_existe INT DEFAULT 0;

    SELECT COUNT(*) INTO v_existe
      FROM inscription
     WHERE id_membre  = p_id_membre
       AND id_atelier = p_id_atelier
       AND statut_paie != 'annulé';

    IF v_existe > 0 THEN
        SET p_message = 'Membre déjà inscrit à cet atelier.';
    ELSE
        -- trg_verif_places_atelier vérifie automatiquement les places
        INSERT INTO inscription
            (id_membre, id_atelier, date_inscription, nb_places, statut_paie)
        VALUES
            (p_id_membre, p_id_atelier, CURDATE(), 1, 'en attente');
        SET p_message = 'Inscription réussie.';
    END IF;
END$$

-- Procédure 2 : Annuler l'inscription d'un membre
DROP PROCEDURE IF EXISTS annuler_inscription$$
CREATE PROCEDURE annuler_inscription(
    IN  p_id_membre  INT,
    IN  p_id_atelier INT,
    OUT p_message    VARCHAR(255)
)
BEGIN
    DECLARE v_existe INT DEFAULT 0;

    SELECT COUNT(*) INTO v_existe
      FROM inscription
     WHERE id_membre  = p_id_membre
       AND id_atelier = p_id_atelier
       AND statut_paie != 'annulé';

    IF v_existe = 0 THEN
        SET p_message = 'Aucune inscription active trouvée.';
    ELSE
        UPDATE inscription
           SET statut_paie = 'annulé'
         WHERE id_membre  = p_id_membre
           AND id_atelier = p_id_atelier;
        SET p_message = 'Inscription annulée.';
    END IF;
END$$

-- Procédure 3 : Créer une exposition avec sa galerie
DROP PROCEDURE IF EXISTS creer_exposition_avec_galerie$$
CREATE PROCEDURE creer_exposition_avec_galerie(
    IN p_titre   VARCHAR(150),
    IN p_debut   DATE,
    IN p_fin     DATE,
    IN p_desc    TEXT,
    IN p_galerie VARCHAR(100),
    IN p_adresse VARCHAR(255)
)
BEGIN
    DECLARE v_id INT;

    START TRANSACTION;
        INSERT INTO exposition (titre, date_debut, date_fin, description)
        VALUES (p_titre, p_debut, p_fin, p_desc);

        SET v_id = LAST_INSERT_ID();

        INSERT INTO galerie (nom, adresse, heure_ouverture, heure_fermeture, id_exposition)
        VALUES (p_galerie, p_adresse, '09:00:00', '19:00:00', v_id);
    COMMIT;
END$$

-- ============================================================
-- 5. FONCTIONS
-- ============================================================

-- Fonction 1 : Vérifier si une exposition est en cours
DROP FUNCTION IF EXISTS exposition_en_cours$$
CREATE FUNCTION exposition_en_cours(p_id_exposition INT)
RETURNS BOOLEAN
DETERMINISTIC
BEGIN
    DECLARE v_result BOOLEAN;
    SELECT (date_debut <= CURDATE() AND date_fin >= CURDATE())
      INTO v_result
      FROM exposition
     WHERE id_exposition = p_id_exposition;
    RETURN COALESCE(v_result, FALSE);
END$$

-- Fonction 2 : Nombre de participants à une exposition
DROP FUNCTION IF EXISTS nb_participants_exposition$$
CREATE FUNCTION nb_participants_exposition(p_id_exposition INT)
RETURNS INT
DETERMINISTIC
BEGIN
    DECLARE v_count INT;
    SELECT COUNT(*) INTO v_count
      FROM reservation
     WHERE id_exposition = p_id_exposition;
    RETURN COALESCE(v_count, 0);
END$$

-- Fonction 3 : Places disponibles dans un atelier
DROP FUNCTION IF EXISTS places_disponibles_atelier$$
CREATE FUNCTION places_disponibles_atelier(p_id_atelier INT)
RETURNS INT
DETERMINISTIC
BEGIN
    DECLARE v_max    INT;
    DECLARE v_prises INT;

    SELECT participants_max INTO v_max
      FROM atelier WHERE id_atelier = p_id_atelier;

    SELECT COALESCE(SUM(nb_places), 0) INTO v_prises
      FROM inscription
     WHERE id_atelier  = p_id_atelier
       AND statut_paie != 'annulé';

    RETURN COALESCE(v_max - v_prises, 0);
END$$

DELIMITER ;

-- ============================================================
-- 6. VÉRIFICATIONS FINALES
-- ============================================================

-- Triggers créés
SHOW TRIGGERS FROM artconnect;

-- Procédures créées
SHOW PROCEDURE STATUS WHERE Db = 'artconnect';

-- Fonctions créées
SHOW FUNCTION STATUS WHERE Db = 'artconnect';

-- Statuts des œuvres corrigés
SELECT titre, statut FROM oeuvre ORDER BY titre;
