package com.pharmacyerp.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import com.pharmacyerp.model.InventoryItem;
import com.pharmacyerp.dao.InventoryDAO;

import java.io.IOException;

public class StockAdjustmentController {

    @FXML private TextField searchField;
    @FXML private TextField selectedBatchField;
    @FXML private TextField currentQtyField;
    @FXML private TextField newQtyField;
    @FXML private ComboBox<String> reasonCombo;
    
    @FXML private TableView<InventoryItem> resultsTable;
    @FXML private TableColumn<InventoryItem, String> colName;
    @FXML private TableColumn<InventoryItem, String> colBatch;
    @FXML private TableColumn<InventoryItem, Integer> colStock;
    @FXML private TableColumn<InventoryItem, Void> colAction;

    private InventoryDAO inventoryDAO = new InventoryDAO();
    private ObservableList<InventoryItem> searchResults = FXCollections.observableArrayList();
    private InventoryItem selectedItem = null;

    @FXML
    public void initialize() {
        reasonCombo.setItems(FXCollections.observableArrayList(
                "Physical Discrepancy", "Damaged Goods", "Expired", "Internal Use", "Other"
        ));
        reasonCombo.getSelectionModel().selectFirst();

        colAction.setCellFactory(param -> new TableCell<InventoryItem, Void>() {
            private final Button selectBtn = new Button("Select");
            {
                selectBtn.getStyleClass().add("button-primary");
                selectBtn.setOnAction(event -> {
                    selectedItem = getTableView().getItems().get(getIndex());
                    selectedBatchField.setText(selectedItem.getBatchNumber());
                    currentQtyField.setText(String.valueOf(selectedItem.getCurrentStock()));
                    newQtyField.clear();
                    newQtyField.requestFocus();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(selectBtn);
                }
            }
        });

        searchField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV.trim().isEmpty()) {
                searchResults.clear();
            } else {
                searchResults.setAll(inventoryDAO.searchInventory(newV.trim()));
            }
        });
        
        resultsTable.setItems(searchResults);
    }

    @FXML
    private void handleUpdate(ActionEvent event) {
        if (selectedItem == null) {
            showAlert(Alert.AlertType.ERROR, "Please select a batch to adjust.");
            return;
        }
        
        try {
            int newQty = Integer.parseInt(newQtyField.getText().trim());
            if (newQty < 0) {
                showAlert(Alert.AlertType.ERROR, "Quantity cannot be negative.");
                return;
            }
            
            if (inventoryDAO.updateStock(selectedItem.getBatchNumber(), newQty)) {
                showAlert(Alert.AlertType.INFORMATION, "Stock updated successfully!");
                handleClear();
                // Refresh search results
                searchResults.setAll(inventoryDAO.searchInventory(searchField.getText().trim()));
            } else {
                showAlert(Alert.AlertType.ERROR, "Failed to update stock.");
            }
            
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid number for new quantity.");
        }
    }

    @FXML
    private void handleClear() {
        selectedItem = null;
        selectedBatchField.clear();
        currentQtyField.clear();
        newQtyField.clear();
    }

    @FXML
    private void handleDashboard(ActionEvent event) {
        navigateTo(event, "/fxml/Dashboard.fxml");
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
    
    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
