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

public class CreditOutstandingController implements Initializable {

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
        
        loadDummyData();
        tblHistory.setItems(records);
        updateTotals();
    }

    @FXML
    public void loadDummyData() {
        records.clear();
        String search = txtSearch.getText() == null ? "" : txtSearch.getText().trim();
        
        String sql = "SELECT MAX(sale_date) as last_date, customer_name, customer_phone, SUM(balance_amount) as outstanding " +
                     "FROM sales " +
                     "WHERE balance_amount > 0 AND (customer_name LIKE ? OR customer_phone LIKE ?) " +
                     "GROUP BY customer_name, customer_phone " +
                     "HAVING SUM(balance_amount) > 0 " +
                     "ORDER BY outstanding DESC";
                     
        try (java.sql.Connection conn = com.pharmacyerp.database.DatabaseManager.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + search + "%");
            stmt.setString(2, "%" + search + "%");
            
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    java.sql.Timestamp ts = rs.getTimestamp("last_date");
                    String dateStr = ts != null ? ts.toLocalDateTime().format(formatter) : "";
                    String cust = rs.getString("customer_name");
                    String phone = rs.getString("customer_phone");
                    double out = rs.getDouble("outstanding");
                    
                    records.add(new HistoryData(dateStr, cust != null && !cust.isEmpty() ? cust : "Walk-in", phone != null ? phone : "", out));
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
