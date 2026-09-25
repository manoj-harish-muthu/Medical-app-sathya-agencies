package com.pharmacyerp.controller;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class ReportSupplierController implements Initializable {

    public static class HistoryData {
        private final SimpleStringProperty date;
        private final SimpleStringProperty name;
        private final SimpleStringProperty description;
        private final SimpleDoubleProperty amount;

        public HistoryData(String date, String name, String description, double amount) {
            this.date = new SimpleStringProperty(date);
            this.name = new SimpleStringProperty(name);
            this.description = new SimpleStringProperty(description);
            this.amount = new SimpleDoubleProperty(amount);
        }

        public String getDate() { return date.get(); }
        public String getName() { return name.get(); }
        public String getDescription() { return description.get(); }
        public double getAmount() { return amount.get(); }
    }
    
    @FXML private TextField txtSearch;
    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    
    @FXML private TableView<HistoryData> tblHistory;
    
    @FXML private Label lblTotalItems;
    @FXML private Label lblGrandTotal;

    private ObservableList<HistoryData> records = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dpFrom.setValue(LocalDate.now().minusDays(30));
        dpTo.setValue(LocalDate.now());
        if (dpFrom != null) dpFrom.valueProperty().addListener((obs, oldV, newV) -> loadDummyData());
        if (dpTo != null) dpTo.valueProperty().addListener((obs, oldV, newV) -> loadDummyData());
        
        loadDummyData();
        tblHistory.setItems(records);
        updateTotals();
    }

    @FXML
    private void loadDummyData() {
        records.clear();
        LocalDate from = dpFrom.getValue() != null ? dpFrom.getValue() : LocalDate.now().minusDays(30);
        LocalDate to = dpTo.getValue() != null ? dpTo.getValue() : LocalDate.now();

        String sql = "SELECT supplier_name, COUNT(purchase_id) as total_bills, SUM(grand_total) as total_purchases " +
                     "FROM purchases " +
                     "WHERE DATE(purchase_date) BETWEEN ? AND ? " +
                     "GROUP BY supplier_name " +
                     "ORDER BY total_purchases DESC";

        try (java.sql.Connection conn = com.pharmacyerp.database.DatabaseManager.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setDate(1, java.sql.Date.valueOf(from));
            stmt.setDate(2, java.sql.Date.valueOf(to));
            
            String today = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String supp = rs.getString("supplier_name");
                    int totalBills = rs.getInt("total_bills");
                    double amount = rs.getDouble("total_purchases");
                    records.add(new HistoryData(today, supp != null && !supp.isEmpty() ? supp : "Unknown Supplier", totalBills + " Purchase Invoices", amount));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateTotals() {
        double grandTotal = 0.0;
        for (HistoryData item : records) {
            grandTotal += item.getAmount();
        }
        lblTotalItems.setText(String.valueOf(records.size()));
        lblGrandTotal.setText(String.format("%.2f", grandTotal));
    }
}
