package com.pharmacyerp.controller;

import com.pharmacyerp.model.Medicine;
import com.pharmacyerp.repository.MedicineRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.Button;
import javafx.util.Callback;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class MedicineController {

    @FXML private TableView<Medicine> medicineTable;
    @FXML private TableColumn<Medicine, Void> actionColumn;
    @FXML private TextField searchField;
    
    private MedicineRepository repository;

    @FXML
    public void initialize() {
        repository = new MedicineRepository();
        setupActionColumn();
        loadMedicines();
    }

    private void setupActionColumn() {
        if (actionColumn == null) return;
        Callback<TableColumn<Medicine, Void>, TableCell<Medicine, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Medicine, Void> call(final TableColumn<Medicine, Void> param) {
                return new TableCell<>() {
                    private final Button editBtn = new Button("Edit");
                    private final Button deleteBtn = new Button("Delete");
                    private final javafx.scene.layout.HBox pane = new javafx.scene.layout.HBox(10, editBtn, deleteBtn);
                    {
                        editBtn.getStyleClass().add("button-primary");
                        editBtn.setOnAction((ActionEvent event) -> {
                            Medicine data = getTableView().getItems().get(getIndex());
                            openEditDialog(data);
                        });

                        deleteBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-cursor: hand;");
                        deleteBtn.setOnAction((ActionEvent event) -> {
                            Medicine data = getTableView().getItems().get(getIndex());
                            if (repository.deleteMedicine(data.getMedicineId())) {
                                refreshTable();
                            }
                        });
                    }
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(pane);
                        }
                    }
                };
            }
        };
        actionColumn.setCellFactory(cellFactory);
    }

    private void loadMedicines() {
        List<Medicine> medicines = repository.getAllActiveMedicines();
        ObservableList<Medicine> observableList = FXCollections.observableArrayList(medicines);
        medicineTable.setItems(observableList);
    }

    @FXML
    private void handleDashboard(ActionEvent event) {
        navigateTo(event, "/fxml/Dashboard.fxml");
    }

    @FXML
    private void handleAddMedicine(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddMedicineDialog.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Add New Medicine");
            stage.setScene(new Scene(root, 800, 500));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            
            // Pass reference to this controller so it can refresh the list
            AddMedicineController controller = loader.getController();
            controller.setMedicineController(this);
            
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void openEditDialog(Medicine data) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddMedicineDialog.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Edit Medicine: " + data.getMedicineName());
            stage.setScene(new Scene(root, 800, 500));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            
            AddMedicineController controller = loader.getController();
            controller.setMedicineController(this);
            controller.setEditMode(data);
            
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void refreshTable() {
        loadMedicines();
    }

    private void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1366, 768));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
