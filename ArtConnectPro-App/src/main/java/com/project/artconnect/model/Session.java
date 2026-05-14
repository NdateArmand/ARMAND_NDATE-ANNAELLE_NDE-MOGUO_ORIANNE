package com.project.artconnect.model;

/**
 * Session singleton — stocke l'utilisateur connecté.
 * Par défaut (non connecté) le rôle est PUBLIC.
 */
public class Session {

    private static Session instance;

    private UserRole role        = UserRole.PUBLIC;
    private String   email       = null;
    private String   displayName = null;
    private Integer  idArtiste   = null;
    private Integer  idMembre    = null;
    private Integer  idOrga      = null;

    private Session() {}

    public static Session getInstance() {
        if (instance == null) instance = new Session();
        return instance;
    }

    // --- Login methods ---

    public void loginMembre(int idMembre, String email, String nom) {
        this.role = UserRole.MEMBRE;
        this.email = email;
        this.displayName = nom;
        this.idMembre = idMembre;
        this.idArtiste = null;
        this.idOrga = null;
    }

    public void loginArtiste(int idArtiste, String email, String nom) {
        this.role = UserRole.ARTISTE;
        this.email = email;
        this.displayName = nom;
        this.idArtiste = idArtiste;
        this.idMembre = null;
        this.idOrga = null;
    }

    public void loginOrganisateur(int idOrga, String email, String nom) {
        this.role = UserRole.ORGANISATEUR;
        this.email = email;
        this.displayName = nom;
        this.idOrga = idOrga;
        this.idArtiste = null;
        this.idMembre = null;
    }

    public void logout() {
        role = UserRole.PUBLIC;
        email = null;
        displayName = null;
        idArtiste = null;
        idMembre = null;
        idOrga = null;
    }

    // --- Checks ---
    public boolean isLoggedIn()       { return role != UserRole.PUBLIC; }
    public boolean isPublic()         { return role == UserRole.PUBLIC; }
    public boolean isMembre()         { return role == UserRole.MEMBRE; }
    public boolean isArtiste()        { return role == UserRole.ARTISTE; }
    public boolean isOrganisateur()   { return role == UserRole.ORGANISATEUR; }

    // --- Getters ---
    public UserRole getRole()         { return role; }
    public String   getEmail()        { return email; }
    public String   getDisplayName()  { return displayName; }
    public Integer  getIdArtiste()    { return idArtiste; }
    public Integer  getIdMembre()     { return idMembre; }
    public Integer  getIdOrga()       { return idOrga; }
}
