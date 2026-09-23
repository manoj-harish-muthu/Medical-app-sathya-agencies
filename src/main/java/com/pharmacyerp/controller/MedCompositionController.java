package com.pharmacyerp.controller;

import com.pharmacyerp.dao.CompositionDAO;
import com.pharmacyerp.dao.GenericSaltDAO;
import com.pharmacyerp.dao.MedicineDAO;
import com.pharmacyerp.model.Composition;
import com.pharmacyerp.model.GenericSalt;
import com.pharmacyerp.model.Medicine;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import java.util.List;

public class MedCompositionController {

    @FXML private TableView<Composition> tblData;
    @FXML private TableColumn<Composition, Integer> colId;
    @FXML private TableColumn<Composition, String> colMedicine;
    @FXML private TableColumn<Composition, String> colSalt;
    @FXML private TableColumn<Composition, String> colStrength;
    @FXML private TableColumn<Composition, Void> colAction;
    @FXML private Label lblTotalItems;

    @FXML private ComboBox<Medicine> medicineCombo;
    @FXML private ComboBox<GenericSalt> saltCombo;
    @FXML private TextField strengthField;
    
    private CompositionDAO compDao = new CompositionDAO();
    private ObservableList<Composition> compList = FXCollections.observableArrayList();
    
    private ObservableList<Medicine> allMedicines = FXCollections.observableArrayList();
    private ObservableList<GenericSalt> allSalts = FXCollections.observableArrayList();
    private GenericSaltDAO saltDao = new GenericSaltDAO();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colMedicine.setCellValueFactory(new PropertyValueFactory<>("medicineName"));
        colSalt.setCellValueFactory(new PropertyValueFactory<>("saltName"));
        colStrength.setCellValueFactory(new PropertyValueFactory<>("strength"));

        colAction.setCellFactory(param -> new TableCell<Composition, Void>() {
            private final Button delBtn = new Button("Delete");
            {
                delBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white;");
                delBtn.setOnAction(event -> {
                    Composition comp = getTableView().getItems().get(getIndex());
                    if (compDao.deleteComposition(comp.getId())) {
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

        tblData.setItems(compList);
        
        setupMedicineCombo(medicineCombo);
        setupSaltCombo(saltCombo);

        loadDropdowns();
        loadData();
    }

    private void setupMedicineCombo(ComboBox<Medicine> combo) {
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

    private void setupSaltCombo(ComboBox<GenericSalt> combo) {
        combo.setConverter(new StringConverter<GenericSalt>() {
            @Override
            public String toString(GenericSalt s) {
                return s == null ? "" : s.getSaltName();
            }
            @Override
            public GenericSalt fromString(String string) {
                return null;
            }
        });
    }

    private void loadDropdowns() {
        allMedicines.setAll(MedicineDAO.getAllMedicines());
        medicineCombo.setItems(allMedicines);
        
        allSalts.setAll(saltDao.getAllSalts());
        saltCombo.setItems(allSalts);
    }

    private void loadData() {
        compList.setAll(compDao.getAllCompositions());
        lblTotalItems.setText(String.valueOf(compList.size()));
    }

    @FXML
    private void handleSave(ActionEvent event) {
        Medicine med = medicineCombo.getValue();
        GenericSalt salt = saltCombo.getValue();
        String strength = strengthField.getText();

        if (med == null || salt == null) {
            showAlert("Error", "Please select both Medicine and Salt.");
            return;
        }

        Composition comp = new Composition();
        comp.setMedicineId(med.getMedicineId());
        comp.setSaltId(salt.getSaltId());
        comp.setStrength(strength != null ? strength.trim() : "");
        
        if (compDao.addComposition(comp)) {
            handleClear(null);
            loadData();
        } else {
            showAlert("Error", "Failed to add composition (Might already exist).");
        }
    }

    @FXML
    private void handleClear(ActionEvent event) {
        medicineCombo.setValue(null);
        saltCombo.setValue(null);
        strengthField.clear();
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
