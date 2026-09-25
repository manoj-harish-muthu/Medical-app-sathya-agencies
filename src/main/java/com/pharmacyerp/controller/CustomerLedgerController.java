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

public class CustomerLedgerController implements Initializable {

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
        
        String sql = "SELECT sale_date as date, customer_name, customer_phone, invoice_no as ref, grand_total as amount, 'SALE' as type " +
                     "FROM sales " +
                     "WHERE DATE(sale_date) BETWEEN ? AND ? AND (customer_name LIKE ? OR customer_phone LIKE ? OR invoice_no LIKE ?) " +
                     "UNION ALL " +
                     "SELECT payment_date as date, '' as customer_name, customer_phone, payment_mode as ref, amount_paid as amount, 'PAYMENT' as type " +
                     "FROM customer_payments " +
                     "WHERE DATE(payment_date) BETWEEN ? AND ? AND (customer_phone LIKE ? OR payment_mode LIKE ?) " +
                     "ORDER BY date DESC";
                     
        try (java.sql.Connection conn = com.pharmacyerp.database.DatabaseManager.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            java.sql.Date dFrom = java.sql.Date.valueOf(from);
            java.sql.Date dTo = java.sql.Date.valueOf(to);
            String likeSearch = "%" + search + "%";
            
            stmt.setDate(1, dFrom);
            stmt.setDate(2, dTo);
            stmt.setString(3, likeSearch);
            stmt.setString(4, likeSearch);
            stmt.setString(5, likeSearch);
            
            stmt.setDate(6, dFrom);
            stmt.setDate(7, dTo);
            stmt.setString(8, likeSearch);
            stmt.setString(9, likeSearch);
            
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    java.sql.Timestamp ts = rs.getTimestamp("date");
                    String dateStr = ts != null ? ts.toLocalDateTime().format(formatter) : "";
                    String cust = rs.getString("customer_name");
                    String phone = rs.getString("customer_phone");
                    String type = rs.getString("type");
                    String ref = rs.getString("ref");
                    double amount = rs.getDouble("amount");
                    
                    String name = (cust != null && !cust.isEmpty()) ? cust : (phone != null ? phone : "Unknown");
                    String desc = type.equals("SALE") ? "Invoice: " + ref : "Payment (" + ref + ")";
                    // For ledger, we'll show Sales as positive and Payments as negative, or just display them as is.
                    // Usually ledger balances reduce with payment. Let's make payment negative.
                    double displayAmount = type.equals("SALE") ? amount : -amount;
                    
                    records.add(new HistoryData(dateStr, name, desc, displayAmount));
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
