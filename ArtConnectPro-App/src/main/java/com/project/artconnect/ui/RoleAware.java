package com.project.artconnect.ui;

/**
 * Interface implémentée par tous les controllers d'onglet.
 * MainController l'appelle après chaque login/logout pour
 * que chaque controller adapte son interface au nouveau rôle.
 */
public interface RoleAware {
    void applyRole();
}
