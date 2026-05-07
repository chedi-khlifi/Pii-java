package com.mindforge.controllers.carriere;

import com.mindforge.entities.carriere.Entreprise;
import com.mindforge.services.carriere.EntrepriseService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EntrepriseController {

    @FXML private TableView<Entreprise> tableView;
    @FXML private TableColumn<Entreprise, Integer> colId;
    @FXML private TableColumn<Entreprise, String> colName;
    @FXML private TableColumn<Entreprise, String> colIndustry;
    @FXML private TableColumn<Entreprise, String> colEmail;
    @FXML private TableColumn<Entreprise, Integer> colPhone;
    @FXML private TableColumn<Entreprise, String> colWebsite;
    @FXML private TableColumn<Entreprise, LocalDateTime> colCreatedAt;

    @FXML private VBox formPanel;
    @FXML private Label formTitle;
    @FXML private TextField fieldName;
    @FXML private TextField fieldIndustry;
    @FXML private TextField fieldEmail;
    @FXML private TextField fieldPhone;
    @FXML private TextField fieldWebsite;
    @FXML private TextArea fieldDescription;

    private final EntrepriseService service = new EntrepriseService();
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colIndustry.setCellValueFactory(new PropertyValueFactory<>("industry"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("contactEmail"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("contactPhone"));
        colWebsite.setCellValueFactory(new PropertyValueFactory<>("website"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colCreatedAt.setCellFactory(col -> new TableCell<>() {
            final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : fmt.format(item));
            }
        });
        refresh();
    }

    private void refresh() {
        tableView.setItems(FXCollections.observableArrayList(service.getData()));
    }

    @FXML
    private void showAddForm() {
        isEditMode = false;
        formTitle.setText("Add Entreprise");
        clearForm();
        showForm();
    }

    @FXML
    private void showEditForm() {
        Entreprise sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Please select a company to edit."); return; }
        isEditMode = true;
        formTitle.setText("Edit Entreprise");
        fieldName.setText(sel.getName());
        fieldDescription.setText(sel.getDescription());
        fieldIndustry.setText(sel.getIndustry());
        fieldEmail.setText(sel.getContactEmail());
        fieldPhone.setText(String.valueOf(sel.getContactPhone()));
        fieldWebsite.setText(sel.getWebsite());
        showForm();
    }

    @FXML
    private void deleteSelected() {
        Entreprise sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Please select a company to delete."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete \"" + sel.getName() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) { service.deleteEntity(sel); refresh(); }
        });
    }

    @FXML
    private void saveEntity() {
        if (fieldName.getText().isBlank()) { alert("Name is required."); return; }
        if (isEditMode) {
            Entreprise sel = tableView.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            fill(sel);
            service.updateEntity(sel.getId(), sel);
        } else {
            Entreprise e = new Entreprise();
            fill(e);
            service.addEntity(e);
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

    private void fill(Entreprise e) {
        e.setName(fieldName.getText().trim());
        e.setDescription(fieldDescription.getText().trim());
        e.setIndustry(fieldIndustry.getText().trim());
        e.setContactEmail(fieldEmail.getText().trim());
        try { e.setContactPhone(Integer.parseInt(fieldPhone.getText().trim())); }
        catch (NumberFormatException ex) { e.setContactPhone(0); }
        e.setWebsite(fieldWebsite.getText().trim());
    }

    private void clearForm() {
        fieldName.clear(); fieldDescription.clear(); fieldIndustry.clear();
        fieldEmail.clear(); fieldPhone.clear(); fieldWebsite.clear();
    }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
