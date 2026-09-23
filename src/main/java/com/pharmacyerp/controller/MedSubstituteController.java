package com.pharmacyerp.controller;

import com.pharmacyerp.dao.MedicineDAO;
import com.pharmacyerp.dao.SubstituteDAO;
import com.pharmacyerp.model.Medicine;
import com.pharmacyerp.model.Substitute;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import java.util.List;

public class MedSubstituteController {

    @FXML private TableView<Substitute> tblData;
    @FXML private TableColumn<Substitute, Integer> colId;
    @FXML private TableColumn<Substitute, String> colPrimary;
    @FXML private TableColumn<Substitute, String> colSubstitute;
    @FXML private TableColumn<Substitute, Void> colAction;
    @FXML private Label lblTotalItems;

    @FXML private ComboBox<Medicine> primaryCombo;
    @FXML private ComboBox<Medicine> substituteCombo;
    
    private SubstituteDAO subDao = new SubstituteDAO();
    private ObservableList<Substitute> subList = FXCollections.observableArrayList();
    private ObservableList<Medicine> allMedicines = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPrimary.setCellValueFactory(new PropertyValueFactory<>("medicineName"));
        colSubstitute.setCellValueFactory(new PropertyValueFactory<>("substituteName"));

        colAction.setCellFactory(param -> new TableCell<Substitute, Void>() {
            private final Button delBtn = new Button("Delete");
            {
                delBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white;");
                delBtn.setOnAction(event -> {
                    Substitute sub = getTableView().getItems().get(getIndex());
                    if (subDao.deleteSubstitute(sub.getId())) {
                        loadData();
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(delBtn);
                }
            }
        });

        tblData.setItems(subList);
        
        setupComboBox(primaryCombo);
        setupComboBox(substituteCombo);

        loadMedicines();
        loadData();
    }

    private void setupComboBox(ComboBox<Medicine> combo) {
        combo.setConverter(new StringConverter<Medicine>() {
            @Override
            public String toString(Medicine m) {
                return m == null ? "" : m.getMedicineName();
            }
            @Override
            public Medicine fromString(String string) {
                return null;
            }
        });
    }

    private void loadMedicines() {
        allMedicines.setAll(MedicineDAO.getAllMedicines());
        primaryCombo.setItems(allMedicines);
        substituteCombo.setItems(allMedicines);
    }

    private void loadData() {
        subList.setAll(subDao.getAllSubstitutes());
        lblTotalItems.setText(String.valueOf(subList.size()));
    }

    @FXML
    private void handleSave(ActionEvent event) {
        Medicine pMed = primaryCombo.getValue();
        Medicine sMed = substituteCombo.getValue();

        if (pMed == null || sMed == null) {
            showAlert("Error", "Please select both Primary and Substitute medicines.");
            return;
        }
        
        if (pMed.getMedicineId() == sMed.getMedicineId()) {
            showAlert("Error", "Primary and Substitute cannot be the same.");
            return;
        }

        Substitute sub = new Substitute();
        sub.setMedicineId(pMed.getMedicineId());
        sub.setSubstituteMedicineId(sMed.getMedicineId());
        
        if (subDao.addSubstitute(sub)) {
            handleClear(null);
            loadData();
        } else {
            showAlert("Error", "Failed to add substitute (Might already exist).");
        }
    }

    @FXML
    private void handleClear(ActionEvent event) {
        primaryCombo.setValue(null);
        substituteCombo.setValue(null);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleDashboard(ActionEvent event) {
        SidebarController sidebar = new SidebarController();
        sidebar.navigateTo(event, "/fxml/Dashboard.fxml");
    }
}
