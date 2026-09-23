package com.pharmacyerp.controller;

import com.pharmacyerp.dao.InventoryDAO;
import com.pharmacyerp.model.ExpiryReportRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.util.List;

public class ReportExpiryController {

    @FXML private ComboBox<String> timeframeCombo;
    @FXML private TableView<ExpiryReportRow> tblExpiry;
    
    @FXML private TableColumn<ExpiryReportRow, String> colMedicine;
    @FXML private TableColumn<ExpiryReportRow, String> colBatch;
    @FXML private TableColumn<ExpiryReportRow, String> colExpiryDate;
    @FXML private TableColumn<ExpiryReportRow, Integer> colStock;
    @FXML private TableColumn<ExpiryReportRow, String> colCompany;
    @FXML private TableColumn<ExpiryReportRow, String> colStatus;

    private InventoryDAO inventoryDAO = new InventoryDAO();
    private ObservableList<ExpiryReportRow> expiryData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        timeframeCombo.setItems(FXCollections.observableArrayList(
                "Already Expired (0 Months)",
                "Next 1 Month",
                "Next 3 Months",
                "Next 6 Months"
        ));
        timeframeCombo.getSelectionModel().select("Next 3 Months");
        
        timeframeCombo.setOnAction(e -> loadData());

        colMedicine.setCellValueFactory(new PropertyValueFactory<>("medicineName"));
        colBatch.setCellValueFactory(new PropertyValueFactory<>("batchNumber"));
        colExpiryDate.setCellValueFactory(new PropertyValueFactory<>("expiryDateStr"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("currentQuantity"));
        colCompany.setCellValueFactory(new PropertyValueFactory<>("companyName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tblExpiry.setItems(expiryData);

        // Highlight rows based on status
        tblExpiry.setRowFactory(tv -> new TableRow<ExpiryReportRow>() {
            @Override
            protected void updateItem(ExpiryReportRow item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    if ("Expired".equals(item.getStatus())) {
                        setStyle("-fx-background-color: #fee2e2;"); // Light red
                    } else if ("Expiring Soon".equals(item.getStatus())) {
                        setStyle("-fx-background-color: #fef3c7;"); // Light yellow
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        loadData();
    }

    @FXML
    private void loadData() {
        expiryData.clear();
        String selection = timeframeCombo.getValue();
        int months = 3;
        if (selection.contains("0")) months = 0;
        else if (selection.contains("1")) months = 1;
        else if (selection.contains("3")) months = 3;
        else if (selection.contains("6")) months = 6;

        List<ExpiryReportRow> rows = inventoryDAO.getExpiringBatches(months);
        expiryData.addAll(rows);
    }

    @FXML
    private void handleExport() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export");
        alert.setHeaderText(null);
        alert.setContentText("Export to CSV feature pending.");
        alert.showAndWait();
    }
}
