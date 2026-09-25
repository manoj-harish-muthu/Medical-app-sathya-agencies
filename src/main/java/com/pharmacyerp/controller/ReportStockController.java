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

public class ReportStockController implements Initializable {

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
        if (txtSearch != null) txtSearch.textProperty().addListener((obs, oldV, newV) -> loadDummyData());
        
        loadDummyData();
        tblHistory.setItems(records);
        updateTotals();
    }

    @FXML
    private void loadDummyData() {
        records.clear();
        String sql = "SELECT m.medicine_name, mb.batch_number, mb.current_quantity, mb.purchase_rate, (mb.current_quantity * mb.purchase_rate) as total_value " +
                     "FROM medicines m " +
                     "JOIN medicine_batches mb ON m.medicine_id = mb.medicine_id " +
                     "WHERE mb.current_quantity > 0 " +
                     "ORDER BY m.medicine_name";

        try (java.sql.Connection conn = com.pharmacyerp.database.DatabaseManager.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
             java.sql.ResultSet rs = stmt.executeQuery()) {
             
            String today = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            
            while (rs.next()) {
                String name = rs.getString("medicine_name");
                String batch = rs.getString("batch_number");
                int qty = rs.getInt("current_quantity");
                double value = rs.getDouble("total_value");
                records.add(new HistoryData(today, name, "Batch: " + batch + " (Qty: " + qty + ")", value));
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
