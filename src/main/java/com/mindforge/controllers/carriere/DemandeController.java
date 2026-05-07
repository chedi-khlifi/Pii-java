package com.mindforge.controllers.carriere;

import com.mindforge.entities.carriere.Demande;
import com.mindforge.entities.carriere.OpportuniteCarriere;
import com.mindforge.services.carriere.DemandeService;
import com.mindforge.services.carriere.OpportuniteCarriereService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DemandeController {

    @FXML private TableView<Demande> tableView;
    @FXML private TableColumn<Demande, Integer> colId;
    @FXML private TableColumn<Demande, String> colOpportunity;
    @FXML private TableColumn<Demande, Integer> colUserId;
    @FXML private TableColumn<Demande, String> colStatus;
    @FXML private TableColumn<Demande, LocalDateTime> colAppliedAt;

    @FXML private VBox formPanel;
    @FXML private Label formTitle;
    @FXML private ComboBox<OpportuniteCarriere> fieldOpportunity;
    @FXML private TextField fieldUserId;
    @FXML private ComboBox<String> fieldStatus;
    @FXML private TextArea fieldCoverLetter;

    private final DemandeService service = new DemandeService();
    private final OpportuniteCarriereService oppService = new OpportuniteCarriereService();
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colOpportunity.setCellValueFactory(new PropertyValueFactory<>("opportunityTitle"));
        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAppliedAt.setCellValueFactory(new PropertyValueFactory<>("appliedAt"));
        colAppliedAt.setCellFactory(col -> new TableCell<>() {
            final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : fmt.format(item));
            }
        });

        fieldStatus.setItems(FXCollections.observableArrayList(
            "pending", "accepted", "rejected", "withdrawn"));

        List<OpportuniteCarriere> opps = oppService.getData();
        fieldOpportunity.setItems(FXCollections.observableArrayList(opps));
        fieldOpportunity.setConverter(new StringConverter<>() {
            @Override public String toString(OpportuniteCarriere o) { return o == null ? "" : o.getTitle(); }
            @Override public OpportuniteCarriere fromString(String s) { return null; }
        });

        refresh();
    }

    private void refresh() {
        tableView.setItems(FXCollections.observableArrayList(service.getData()));
    }

    @FXML
    private void showAddForm() {
        isEditMode = false;
        formTitle.setText("Add Demande");
        clearForm();
        showForm();
    }

    @FXML
    private void showEditForm() {
        Demande sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Please select an application to edit."); return; }
        isEditMode = true;
        formTitle.setText("Edit Demande");
        fieldOpportunity.getItems().stream()
            .filter(o -> o.getId() == sel.getOpportunityId())
            .findFirst().ifPresent(fieldOpportunity::setValue);
        fieldUserId.setText(String.valueOf(sel.getUserId()));
        fieldStatus.setValue(sel.getStatus());
        fieldCoverLetter.setText(sel.getCoverLetter());
        showForm();
    }

    @FXML
    private void deleteSelected() {
        Demande sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Please select an application to delete."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete this application?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) { service.deleteEntity(sel); refresh(); }
        });
    }

    @FXML
    private void saveEntity() {
        if (fieldOpportunity.getValue() == null) { alert("Opportunity is required."); return; }
        if (fieldUserId.getText().isBlank()) { alert("User ID is required."); return; }
        if (isEditMode) {
            Demande sel = tableView.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            fill(sel);
            service.updateEntity(sel.getId(), sel);
        } else {
            Demande d = new Demande();
            fill(d);
            service.addEntity(d);
        }
        refresh();
        cancelForm();
    }

    @FXML
    private void cancelForm() {
        formPanel.setVisible(false);
        formPanel.setManaged(false);
        clearForm();
    }

    private void showForm() {
        formPanel.setVisible(true);
        formPanel.setManaged(true);
    }

    private void fill(Demande d) {
        d.setOpportunityId(fieldOpportunity.getValue().getId());
        try { d.setUserId(Integer.parseInt(fieldUserId.getText().trim())); }
        catch (NumberFormatException ex) { d.setUserId(0); }
        d.setStatus(fieldStatus.getValue() != null ? fieldStatus.getValue() : "pending");
        d.setCoverLetter(fieldCoverLetter.getText().trim());
    }

    private void clearForm() {
        fieldOpportunity.setValue(null);
        fieldUserId.clear();
        fieldStatus.setValue(null);
        fieldCoverLetter.clear();
    }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
