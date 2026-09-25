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

public class PrescriptionHistoryController implements Initializable {

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
        
        
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, oldV, newV) -> loadDummyData());
        }
        if (dpFrom != null) {
            dpFrom.valueProperty().addListener((obs, oldV, newV) -> loadDummyData());
        }
        if (dpTo != null) {
            dpTo.valueProperty().addListener((obs, oldV, newV) -> loadDummyData());
        }

        loadDummyData();
        tblHistory.setItems(records);
        updateTotals();
    }

    @FXML
    public void loadDummyData() {
        records.clear();
        String search = txtSearch.getText() == null ? "" : txtSearch.getText().trim();
        LocalDate from = dpFrom.getValue() != null ? dpFrom.getValue() : LocalDate.now().minusDays(30);
        LocalDate to = dpTo.getValue() != null ? dpTo.getValue() : LocalDate.now();
        
        String sql = "SELECT sale_date, customer_name, doctor_name, invoice_no, grand_total " +
                     "FROM sales " +
                     "WHERE doctor_name IS NOT NULL AND doctor_name != '' " +
                     "  AND DATE(sale_date) BETWEEN ? AND ? " +
                     "  AND (customer_name LIKE ? OR doctor_name LIKE ? OR invoice_no LIKE ?) " +
                     "ORDER BY sale_date DESC";
                     
        try (java.sql.Connection conn = com.pharmacyerp.database.DatabaseManager.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(from));
            stmt.setDate(2, java.sql.Date.valueOf(to));
            stmt.setString(3, "%" + search + "%");
            stmt.setString(4, "%" + search + "%");
            stmt.setString(5, "%" + search + "%");
            
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    java.sql.Timestamp ts = rs.getTimestamp("sale_date");
                    String dateStr = ts != null ? ts.toLocalDateTime().format(formatter) : "";
                    String cust = rs.getString("customer_name");
                    String doc = rs.getString("doctor_name");
                    String name = (cust != null ? cust : "Walk-in") + " (Dr. " + doc + ")";
                    String inv = rs.getString("invoice_no");
                    double total = rs.getDouble("grand_total");
                    
                    records.add(new HistoryData(dateStr, name, "Invoice: " + inv, total));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateTotals();
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
