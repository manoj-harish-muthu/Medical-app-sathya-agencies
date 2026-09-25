package com.pharmacyerp.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Stage;
import java.io.IOException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import com.pharmacyerp.model.InventoryItem;
import com.pharmacyerp.dao.InventoryDAO;

public class InventoryController {
    
    @FXML private TableView<InventoryItem> inventoryTable;
    @FXML private TableColumn<InventoryItem, String> colName;
    @FXML private TableColumn<InventoryItem, String> colBatch;
    @FXML private TableColumn<InventoryItem, java.sql.Date> colExpiry;
    @FXML private TableColumn<InventoryItem, Integer> colStock;
    @FXML private TableColumn<InventoryItem, Integer> colMin;
    @FXML private TableColumn<InventoryItem, java.math.BigDecimal> colMrp;
    @FXML private TableColumn<InventoryItem, String> colStatus;

    private InventoryDAO inventoryDAO = new InventoryDAO();
    private ObservableList<InventoryItem> inventoryList = FXCollections.observableArrayList();
    public static String currentFilter = "All";

    @FXML
    public void initialize() {
        // Custom styling for Status column based on value
        colStatus.setCellFactory(column -> {
            return new TableCell<InventoryItem, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        if ("Expired".equals(item)) {
                            setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        } else if ("Near Expiry".equals(item)) {
                            setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                        } else if ("Zero Stock".equals(item) || "Low Stock".equals(item)) {
                            setStyle("-fx-text-fill: #EAB308; -fx-font-weight: bold;"); // Yellow
                        } else {
                            setStyle("-fx-text-fill: green;");
                        }
                    }
                }
            };
        });

        if ("NearExpiry".equals(currentFilter)) {
            handleShowNearExpiry();
        } else {
            handleShowAll();
        }
        currentFilter = "All"; // Reset after applying
    }

    @FXML
    private void handleShowAll() {
        inventoryList.setAll(inventoryDAO.getAllInventory());
        inventoryTable.setItems(inventoryList);
    }

    @FXML
    private void handleShowLowStock() {
        inventoryList.setAll(inventoryDAO.getZeroOrLowStock());
        inventoryTable.setItems(inventoryList);
    }

    @FXML
    private void handleShowNearExpiry() {
        inventoryList.setAll(inventoryDAO.getNearExpiryStock());
        inventoryTable.setItems(inventoryList);
    }

    @FXML
    private void handleShowExpired() {
        inventoryList.setAll(inventoryDAO.getExpiredStock());
        inventoryTable.setItems(inventoryList);
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
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
