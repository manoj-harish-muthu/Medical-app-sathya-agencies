package com.pharmacyerp.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class PurchaseHistoryController {
    
    @FXML private TableView<HistoryItem> tblPurchaseHistory;
    @FXML private TableColumn<HistoryItem, String> colInvoice;
    @FXML private TableColumn<HistoryItem, String> colDate;
    @FXML private TableColumn<HistoryItem, String> colSupplier;
    @FXML private TableColumn<HistoryItem, String> colAmount;
    @FXML private TableColumn<HistoryItem, String> colStatus;
    @FXML private TableColumn<HistoryItem, String> colPayment;
    
    private ObservableList<HistoryItem> dataList = FXCollections.observableArrayList();
    
    public static class HistoryItem {
        private SimpleStringProperty invoice;
        private SimpleStringProperty date;
        private SimpleStringProperty supplier;
        private SimpleStringProperty amount;
        private SimpleStringProperty status;
        private SimpleStringProperty payment;
        
        public HistoryItem(String inv, String dt, String sup, String amt, String stat, String pay) {
            this.invoice = new SimpleStringProperty(inv);
            this.date = new SimpleStringProperty(dt);
            this.supplier = new SimpleStringProperty(sup);
            this.amount = new SimpleStringProperty(amt);
            this.status = new SimpleStringProperty(stat);
            this.payment = new SimpleStringProperty(pay);
        }
        
        public String getInvoice() { return invoice.get(); }
        public String getDate() { return date.get(); }
        public String getSupplier() { return supplier.get(); }
        public String getAmount() { return amount.get(); }
        public String getStatus() { return status.get(); }
        public String getPayment() { return payment.get(); }
    }
    
    @FXML
    public void initialize() {
        if (colInvoice != null) colInvoice.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("invoice"));
        if (colDate != null) colDate.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("date"));
        if (colSupplier != null) colSupplier.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("supplier"));
        if (colAmount != null) colAmount.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("amount"));
        if (colStatus != null) colStatus.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("status"));
        if (colPayment != null) colPayment.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("payment"));
        
        loadData();
    }
    
    private void loadData() {
        dataList.clear();
        String sql = "SELECT invoice_no, purchase_date, supplier_name, grand_total, status, payment_mode FROM purchases ORDER BY purchase_id DESC";
        try (java.sql.Connection conn = com.pharmacyerp.database.DatabaseManager.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {
             
             java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
             
             while (rs.next()) {
                 String inv = rs.getString("invoice_no");
                 java.sql.Timestamp ts = rs.getTimestamp("purchase_date");
                 String dateStr = ts != null ? sdf.format(ts) : "";
                 String sup = rs.getString("supplier_name");
                 String amt = String.format("%.2f", rs.getDouble("grand_total"));
                 String stat = rs.getString("status");
                 String pay = rs.getString("payment_mode");
                 
                 dataList.add(new HistoryItem(inv, dateStr, sup, amt, stat, pay));
             }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (tblPurchaseHistory != null) {
            tblPurchaseHistory.setItems(dataList);
        }
    }
}
