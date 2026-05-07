package com.mindforge.controllers;

import com.mindforge.entities.carriere.Demande;
import com.mindforge.entities.carriere.OpportuniteCarriere;
import com.mindforge.services.carriere.DemandeService;
import com.mindforge.services.carriere.EntrepriseService;
import com.mindforge.services.carriere.OpportuniteCarriereService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HomeController {

    @FXML private Label lblEntreprisesCount;
    @FXML private Label lblOpportunitesCount;
    @FXML private Label lblDemandesCount;

    @FXML private TableView<OpportuniteCarriere> tblRecentOpp;
    @FXML private TableColumn<OpportuniteCarriere, String> colOppTitle;
    @FXML private TableColumn<OpportuniteCarriere, String> colOppType;
    @FXML private TableColumn<OpportuniteCarriere, String> colOppLocation;
    @FXML private TableColumn<OpportuniteCarriere, String> colOppStatus;
    @FXML private TableColumn<OpportuniteCarriere, LocalDate> colOppDeadline;

    @FXML private TableView<Demande> tblRecentDemandes;
    @FXML private TableColumn<Demande, Integer> colDemId;
    @FXML private TableColumn<Demande, String> colDemOpportunity;
    @FXML private TableColumn<Demande, String> colDemStatus;
    @FXML private TableColumn<Demande, LocalDateTime> colDemApplied;

    @FXML
    public void initialize() {
        setupColumns();
        loadData();
    }

    private void setupColumns() {
        colOppTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colOppType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colOppLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colOppStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colOppDeadline.setCellValueFactory(new PropertyValueFactory<>("deadline"));

        colDemId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDemOpportunity.setCellValueFactory(new PropertyValueFactory<>("opportunityTitle"));
        colDemStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDemApplied.setCellValueFactory(new PropertyValueFactory<>("appliedAt"));
        colDemApplied.setCellFactory(col -> new TableCell<>() {
            final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : fmt.format(item));
            }
        });
    }

    private void loadData() {
        List<OpportuniteCarriere> opps = new OpportuniteCarriereService().getData();
        List<Demande> demandes = new DemandeService().getData();
        int entCount = new EntrepriseService().getData().size();

        lblEntreprisesCount.setText(String.valueOf(entCount));
        lblOpportunitesCount.setText(String.valueOf(opps.size()));
        lblDemandesCount.setText(String.valueOf(demandes.size()));

        tblRecentOpp.setItems(FXCollections.observableArrayList(opps.stream().limit(5).toList()));
        tblRecentDemandes.setItems(FXCollections.observableArrayList(demandes.stream().limit(5).toList()));
    }
}
