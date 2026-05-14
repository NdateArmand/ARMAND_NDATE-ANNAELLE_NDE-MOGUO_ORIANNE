package com.project.artconnect.ui;

import com.project.artconnect.dao.ExhibitionDao;
import com.project.artconnect.dao.impl.DaoFactory;
import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.service.WorkshopService;
import com.project.artconnect.util.ServiceProvider;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.geometry.Insets;

import java.util.List;

/**
 * DiscoverController corrigé :
 * - Les expositions sont chargées via ExhibitionDao (jointure SQL)
 *   et non via g.getExhibitions() qui retournait toujours vide.
 * - Les ateliers sont chargés via WorkshopService.
 */
public class DiscoverController {

    @FXML private FlowPane discoverPane;

    // ExhibitionDao fait la jointure SQL EXPOSITION ↔ GALERIE
    private final ExhibitionDao  exhibitionDao  = DaoFactory.getExhibitionDao();
    private final WorkshopService workshopService = ServiceProvider.getWorkshopService();

    @FXML
    public void initialize() {
        discoverPane.getChildren().clear();

        List<Exhibition> exhibitions = exhibitionDao.findAll();
        exhibitions.stream().limit(3).forEach(this::addExhibitionCard);

        List<Workshop> workshops = workshopService.getAllWorkshops();
        workshops.stream().limit(3).forEach(this::addWorkshopCard);

        if (exhibitions.isEmpty() && workshops.isEmpty()) {
            Label empty = new Label("Aucun événement disponible pour le moment.");
            empty.setStyle("-fx-font-size:14px;-fx-text-fill:#888;");
            discoverPane.getChildren().add(empty);
        }
    }

    private void addExhibitionCard(Exhibition e) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setPrefWidth(250);
        card.setStyle("-fx-background-color:#e3f2fd;-fx-border-color:#2196f3;" +
                      "-fx-border-radius:5;-fx-background-radius:5;");

        Label badge = new Label("FEATURED EXHIBITION");
        badge.setStyle("-fx-font-size:10px;-fx-text-fill:#1565c0;-fx-font-weight:bold;");

        Label title = new Label(e.getTitle());
        title.setStyle("-fx-font-weight:bold;");
        title.setWrapText(true);

        String theme = e.getTheme() != null && !e.getTheme().isBlank()
                ? "Theme: " + e.getTheme() : "";
        String gallery = e.getGallery() != null ? e.getGallery().getName() : "—";

        card.getChildren().addAll(badge, title);
        if (!theme.isBlank()) card.getChildren().add(new Label(theme));
        card.getChildren().add(new Label("Gallery: " + gallery));
        discoverPane.getChildren().add(card);
    }

    private void addWorkshopCard(Workshop w) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setPrefWidth(250);
        card.setStyle("-fx-background-color:#f1f8e9;-fx-border-color:#4caf50;" +
                      "-fx-border-radius:5;-fx-background-radius:5;");

        Label badge = new Label("UPCOMING WORKSHOP");
        badge.setStyle("-fx-font-size:10px;-fx-text-fill:#2e7d32;-fx-font-weight:bold;");

        Label title = new Label(w.getTitle());
        title.setStyle("-fx-font-weight:bold;");
        title.setWrapText(true);

        String instructor = w.getInstructor() != null ? w.getInstructor().getName() : "Unknown";

        card.getChildren().addAll(
                badge, title,
                new Label("Instructor: " + instructor),
                new Label("Price: " + w.getPrice() + " €"));
        discoverPane.getChildren().add(card);
    }
}
