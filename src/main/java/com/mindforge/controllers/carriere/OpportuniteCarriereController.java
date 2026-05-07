package com.mindforge.controllers.carriere;

import com.mindforge.entities.carriere.Entreprise;
import com.mindforge.entities.carriere.OpportuniteCarriere;
import com.mindforge.services.carriere.EntrepriseService;
import com.mindforge.services.carriere.OpportuniteCarriereService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;

public class OpportuniteCarriereController {

    @FXML private TableView<OpportuniteCarriere> tableView;
    @FXML private TableColumn<OpportuniteCarriere, Integer> colId;
    @FXML private TableColumn<OpportuniteCarriere, String> colTitle;
    @FXML private TableColumn<OpportuniteCarriere, String> colType;
    @FXML private TableColumn<OpportuniteCarriere, String> colLocation;
    @FXML private TableColumn<OpportuniteCarriere, String> colStatus;
    @FXML private TableColumn<OpportuniteCarriere, LocalDate> colDeadline;
    @FXML private TableColumn<OpportuniteCarriere, String> colCompany;

    @FXML private VBox formPanel;
    @FXML private Label formTitle;
    @FXML private TextField fieldTitle;
    @FXML private ComboBox<String> fieldType;
    @FXML private TextField fieldLocation;
    @FXML private TextField fieldDuration;
    @FXML private DatePicker fieldDeadline;
    @FXML private ComboBox<String> fieldStatus;
    @FXML private ComboBox<Entreprise> fieldCompany;
    @FXML private TextArea fieldDescription;

    private final OpportuniteCarriereService service = new OpportuniteCarriereService();
    private final EntrepriseService entrepriseService = new EntrepriseService();
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDeadline.setCellValueFactory(new PropertyValueFactory<>("deadline"));
        colCompany.setCellValueFactory(new PropertyValueFactory<>("companyName"));

        fieldType.setItems(FXCollections.observableArrayList(
            "internship", "apprenticeship", "fulltime", "parttime", "freelance"));
        fieldStatus.setItems(FXCollections.observableArrayList("active", "closed", "filled"));
        fieldCompany.setItems(FXCollections.observableArrayList(entrepriseService.getData()));
        fieldCompany.setConverter(new StringConverter<>() {
            @Override public String toString(Entreprise e) { return e == null ? "" : e.getName(); }
            @Override public Entreprise fromString(String s) { return null; }
        });

        fieldDeadline.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (!empty && date != null && date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #cbd5e1;");
                }
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
        formTitle.setText("Add Opportunite");
        clearForm();
        showForm();
    }

    @FXML
    private void showEditForm() {
        OpportuniteCarriere sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Please select an opportunity to edit."); return; }
        isEditMode = true;
        formTitle.setText("Edit Opportunite");
        fieldTitle.setText(sel.getTitle());
        fieldDescription.setText(sel.getDescription());
        fieldType.setValue(sel.getType());
        fieldLocation.setText(sel.getLocation());
        fieldDuration.setText(sel.getDuration());
        fieldDeadline.setValue(sel.getDeadline());
        fieldStatus.setValue(sel.getStatus());
        fieldCompany.getItems().stream()
            .filter(e -> e.getId() == sel.getCompanyId())
            .findFirst().ifPresent(fieldCompany::setValue);
        showForm();
    }

    @FXML
    private void deleteSelected() {
        OpportuniteCarriere sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Please select an opportunity to delete."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete \"" + sel.getTitle() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) { service.deleteEntity(sel); refresh(); }
        });
    }

    @FXML
    private void saveEntity() {
        if (fieldTitle.getText().isBlank()) { alert("Title is required."); return; }
        if (isEditMode) {
            OpportuniteCarriere sel = tableView.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            fill(sel);
            service.updateEntity(sel.getId(), sel);
        } else {
            OpportuniteCarriere o = new OpportuniteCarriere();
            fill(o);
            service.addEntity(o);
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

    private void fill(OpportuniteCarriere o) {
        o.setTitle(fieldTitle.getText().trim());
        o.setDescription(fieldDescription.getText().trim());
        o.setType(fieldType.getValue() != null ? fieldType.getValue() : "internship");
        o.setLocation(fieldLocation.getText().trim());
        o.setDuration(fieldDuration.getText().trim());
        o.setDeadline(fieldDeadline.getValue());
        o.setStatus(fieldStatus.getValue() != null ? fieldStatus.getValue() : "active");
        Entreprise company = fieldCompany.getValue();
        o.setCompanyId(company != null ? company.getId() : 0);
    }

    private void clearForm() {
        fieldTitle.clear(); fieldDescription.clear(); fieldLocation.clear(); fieldDuration.clear();
        fieldType.setValue(null); fieldStatus.setValue(null);
        fieldDeadline.setValue(null); fieldCompany.setValue(null);
    }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
