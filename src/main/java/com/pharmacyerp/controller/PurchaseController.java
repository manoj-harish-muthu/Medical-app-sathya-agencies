package com.pharmacyerp.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import com.pharmacyerp.model.Supplier;
import com.pharmacyerp.model.Medicine;
import com.pharmacyerp.dao.SupplierDAO;
import com.pharmacyerp.dao.MedicineDAO;

public class PurchaseController {
    
    @FXML private ComboBox<Supplier> cmbSupplier;
    @FXML private TextField txtInvoiceNo;
    @FXML private ComboBox<String> cmbPaymentMode;
    @FXML private DatePicker dpPurchaseDate;
    
    @FXML private TextField medicineSearchField;
    @FXML private TableView<PurchaseItem> tblCart;
    
    @FXML private TableColumn<PurchaseItem, String> colMedicine;
    @FXML private TableColumn<PurchaseItem, String> colBatch;
    @FXML private TableColumn<PurchaseItem, String> colExpiry;
    @FXML private TableColumn<PurchaseItem, String> colQty;
    @FXML private TableColumn<PurchaseItem, String> colFree;
    @FXML private TableColumn<PurchaseItem, String> colPRate;
    @FXML private TableColumn<PurchaseItem, String> colMRP;
    @FXML private TableColumn<PurchaseItem, String> colTotal;
    
    @FXML private Label lblTotalItems;
    @FXML private Label lblGrandTotal;
    @FXML private TextField txtAmountPaid;
    
    private ObservableList<PurchaseItem> cartItems = FXCollections.observableArrayList();
    private ContextMenu suggestionsPopup = new ContextMenu();

    public static class PurchaseItem {
        private int medicineId;
        private SimpleStringProperty medicineName;
        private SimpleStringProperty batchNumber;
        private SimpleStringProperty expiryDate;
        private SimpleStringProperty quantity;
        private SimpleStringProperty freeQuantity;
        private SimpleStringProperty purchaseRate;
        private SimpleStringProperty mrp;
        private SimpleStringProperty netAmount;
        
        public PurchaseItem(int medicineId, String medicineName) {
            this.medicineId = medicineId;
            this.medicineName = new SimpleStringProperty(medicineName);
            this.batchNumber = new SimpleStringProperty("");
            this.expiryDate = new SimpleStringProperty("");
            this.quantity = new SimpleStringProperty("1");
            this.freeQuantity = new SimpleStringProperty("0");
            this.purchaseRate = new SimpleStringProperty("0.00");
            this.mrp = new SimpleStringProperty("0.00");
            this.netAmount = new SimpleStringProperty("0.00");
        }
        
        public int getMedicineId() { return medicineId; }
        
        public String getMedicineName() { return medicineName.get(); }
        public SimpleStringProperty medicineNameProperty() { return medicineName; }
        public void setMedicineName(String value) { this.medicineName.set(value); }
        
        public String getBatchNumber() { return batchNumber.get(); }
        public SimpleStringProperty batchNumberProperty() { return batchNumber; }
        public void setBatchNumber(String value) { this.batchNumber.set(value); }
        
        public String getExpiryDate() { return expiryDate.get(); }
        public SimpleStringProperty expiryDateProperty() { return expiryDate; }
        public void setExpiryDate(String value) { this.expiryDate.set(value); }
        
        public String getQuantity() { return quantity.get(); }
        public SimpleStringProperty quantityProperty() { return quantity; }
        public void setQuantity(String value) { this.quantity.set(value); updateNetAmt(); }
        
        public String getFreeQuantity() { return freeQuantity.get(); }
        public SimpleStringProperty freeQuantityProperty() { return freeQuantity; }
        public void setFreeQuantity(String value) { this.freeQuantity.set(value); }
        
        public String getPurchaseRate() { return purchaseRate.get(); }
        public SimpleStringProperty purchaseRateProperty() { return purchaseRate; }
        public void setPurchaseRate(String value) { this.purchaseRate.set(value); updateNetAmt(); }
        
        public String getMrp() { return mrp.get(); }
        public SimpleStringProperty mrpProperty() { return mrp; }
        public void setMrp(String value) { this.mrp.set(value); }
        
        public String getNetAmount() { return netAmount.get(); }
        public SimpleStringProperty netAmountProperty() { return netAmount; }
        
        public void updateNetAmt() {
            try {
                double qty = Double.parseDouble(getQuantity());
                double prate = Double.parseDouble(getPurchaseRate());
                netAmount.set(String.format("%.2f", qty * prate));
            } catch (Exception e) {
                netAmount.set("0.00");
            }
        }
    }

    @FXML
    public void initialize() {
        if (cmbSupplier != null) {
            cmbSupplier.getItems().setAll(SupplierDAO.getAllSuppliers());
        }
        if (cmbPaymentMode != null) {
            cmbPaymentMode.getItems().addAll("Cash", "Credit", "UPI", "Bank Transfer");
            cmbPaymentMode.getSelectionModel().selectFirst();
        }
        if (dpPurchaseDate != null) {
            dpPurchaseDate.setValue(LocalDate.now());
        }
        
        tblCart.setItems(cartItems);
        tblCart.setEditable(true);
        tblCart.getSelectionModel().setCellSelectionEnabled(true);
        
        setupColumns();
        setupSearch();
    }
    
    private void setupColumns() {
        if (colBatch == null) return;
        
        colBatch.setCellFactory(TextFieldTableCell.forTableColumn());
        colBatch.setOnEditCommit(e -> e.getRowValue().setBatchNumber(e.getNewValue()));
        
        colExpiry.setCellFactory(TextFieldTableCell.forTableColumn());
        colExpiry.setOnEditCommit(e -> e.getRowValue().setExpiryDate(e.getNewValue()));
        
        colQty.setCellFactory(TextFieldTableCell.forTableColumn());
        colQty.setOnEditCommit(e -> {
            e.getRowValue().setQuantity(e.getNewValue());
            updateTotals();
        });
        
        colFree.setCellFactory(TextFieldTableCell.forTableColumn());
        colFree.setOnEditCommit(e -> e.getRowValue().setFreeQuantity(e.getNewValue()));
        
        colPRate.setCellFactory(TextFieldTableCell.forTableColumn());
        colPRate.setOnEditCommit(e -> {
            e.getRowValue().setPurchaseRate(e.getNewValue());
            updateTotals();
            tblCart.refresh();
        });
        
        colMRP.setCellFactory(TextFieldTableCell.forTableColumn());
        colMRP.setOnEditCommit(e -> e.getRowValue().setMrp(e.getNewValue()));
    }
    
    private void setupSearch() {
        if (medicineSearchField == null) return;
        
        javafx.animation.PauseTransition searchDebounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
        medicineSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchDebounce.setOnFinished(event -> {
                if (newValue == null || newValue.trim().isEmpty()) {
                    suggestionsPopup.hide();
                } else {
                    String query = newValue.trim();
                    java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        return MedicineDAO.searchMedicines(query);
                    }).thenAccept(suggestions -> {
                        javafx.application.Platform.runLater(() -> {
                            if (suggestions.isEmpty()) {
                                suggestionsPopup.hide();
                            } else {
                                suggestionsPopup.getItems().clear();
                                for (Medicine item : suggestions) {
                                    Label nameLbl = new Label(item.getMedicineName() + " (" + item.getCompanyName() + ")");
                                    nameLbl.setStyle("-fx-font-size: 14px; -fx-cursor: hand;");
                                    
                                    CustomMenuItem menuItem = new CustomMenuItem(nameLbl, true);
                                    menuItem.setOnAction(e -> {
                                        medicineSearchField.setText("");
                                        suggestionsPopup.hide();
                                        addItemToCart(item);
                                    });
                                    suggestionsPopup.getItems().add(menuItem);
                                }
                                if (!suggestionsPopup.isShowing() && medicineSearchField.getScene() != null && medicineSearchField.isFocused()) {
                                    suggestionsPopup.show(medicineSearchField, javafx.geometry.Side.BOTTOM, 0, 0);
                                }
                            }
                        });
                    });
                }
            });
            searchDebounce.playFromStart();
        });
    }
    
    private void addItemToCart(Medicine m) {
        String lastPRate = "0.00";
        String lastMrp = "0.00";
        
        String sql = "SELECT purchase_rate, mrp FROM medicine_batches WHERE medicine_id = ? ORDER BY batch_id DESC LIMIT 1";
        try (java.sql.Connection conn = com.pharmacyerp.database.DatabaseManager.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, m.getMedicineId());
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    lastPRate = String.format("%.2f", rs.getDouble("purchase_rate"));
                    lastMrp = String.format("%.2f", rs.getDouble("mrp"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        PurchaseItem newItem = new PurchaseItem(m.getMedicineId(), m.getMedicineName());
        newItem.setPurchaseRate(lastPRate);
        newItem.setMrp(lastMrp);
        
        cartItems.add(newItem);
        updateTotals();
    }
    
    private void updateTotals() {
        lblTotalItems.setText(String.valueOf(cartItems.size()));
        double grandTotal = 0.0;
        for (PurchaseItem item : cartItems) {
            try {
                grandTotal += Double.parseDouble(item.getNetAmount());
            } catch (Exception e) {}
        }
        lblGrandTotal.setText(String.format("%.2f", grandTotal));
    }

    @FXML
    private void handleDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSavePurchase(ActionEvent event) {
        if (cmbSupplier.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select a supplier!");
            return;
        }
        if (txtInvoiceNo.getText() == null || txtInvoiceNo.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please enter an invoice number!");
            return;
        }
        if (cartItems.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Purchase cart is empty!");
            return;
        }
        
        Supplier supplier = cmbSupplier.getValue();
        String invoice = txtInvoiceNo.getText().trim();
        String payment = cmbPaymentMode.getValue() != null ? cmbPaymentMode.getValue() : "Cash";
        java.sql.Date pDate = dpPurchaseDate.getValue() != null ? java.sql.Date.valueOf(dpPurchaseDate.getValue()) : new java.sql.Date(System.currentTimeMillis());
        
        double grandTotal = 0.0;
        for (PurchaseItem item : cartItems) {
            try { grandTotal += Double.parseDouble(item.getNetAmount()); } catch (Exception e) {}
        }
        double amtPaid = 0.0;
        try {
            if (txtAmountPaid != null && !txtAmountPaid.getText().isEmpty()) {
                amtPaid = Double.parseDouble(txtAmountPaid.getText());
            }
        } catch (Exception e) {}
        
        double balance = grandTotal - amtPaid;
        
        String insertPurchase = "INSERT INTO purchases (invoice_no, purchase_date, supplier_id, supplier_name, payment_mode, subtotal, grand_total, paid_amount, balance_amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertItem = "INSERT INTO purchase_items (purchase_id, medicine_id, batch_number, expiry_date, quantity, free_quantity, purchase_rate, mrp, selling_rate, net_amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String checkBatch = "SELECT batch_id FROM medicine_batches WHERE medicine_id = ? AND batch_number = ?";
        String insertBatch = "INSERT INTO medicine_batches (medicine_id, batch_number, expiry_date, purchase_rate, mrp, selling_rate, current_quantity) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String updateBatch = "UPDATE medicine_batches SET current_quantity = current_quantity + ?, purchase_rate = ?, mrp = ? WHERE batch_id = ?";
        
        try (java.sql.Connection conn = com.pharmacyerp.database.DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            
            try (java.sql.PreparedStatement pStmt = conn.prepareStatement(insertPurchase, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                pStmt.setString(1, invoice);
                pStmt.setDate(2, pDate);
                pStmt.setInt(3, supplier.getSupplierId());
                pStmt.setString(4, supplier.getSupplierName());
                pStmt.setString(5, payment);
                pStmt.setDouble(6, grandTotal);
                pStmt.setDouble(7, grandTotal);
                pStmt.setDouble(8, amtPaid);
                pStmt.setDouble(9, balance);
                pStmt.executeUpdate();
                
                int purchaseId = -1;
                try (java.sql.ResultSet rs = pStmt.getGeneratedKeys()) {
                    if (rs.next()) purchaseId = rs.getInt(1);
                }
                
                try (java.sql.PreparedStatement iStmt = conn.prepareStatement(insertItem);
                     java.sql.PreparedStatement cbStmt = conn.prepareStatement(checkBatch);
                     java.sql.PreparedStatement ibStmt = conn.prepareStatement(insertBatch);
                     java.sql.PreparedStatement ubStmt = conn.prepareStatement(updateBatch)) {
                     
                    for (PurchaseItem item : cartItems) {
                        int medId = item.getMedicineId();
                        String batch = item.getBatchNumber();
                        String expiry = item.getExpiryDate();
                        if (expiry == null || expiry.isEmpty()) expiry = "2099-12-31"; // Fallback expiry
                        java.sql.Date expDate = null;
                        try {
                            if (expiry.contains("/")) {
                                String[] parts = expiry.split("/");
                                if (parts.length == 2) expiry = "20" + parts[1] + "-" + parts[0] + "-01";
                                else if (parts.length == 3) expiry = parts[2] + "-" + parts[1] + "-" + parts[0];
                            }
                            expDate = java.sql.Date.valueOf(expiry);
                        } catch (Exception e) {
                            expDate = java.sql.Date.valueOf("2099-12-31");
                        }
                        
                        int qty = 0, free = 0;
                        double pRate = 0, mrp = 0, netAmt = 0;
                        try { qty = Integer.parseInt(item.getQuantity()); } catch(Exception e) {}
                        try { free = Integer.parseInt(item.getFreeQuantity()); } catch(Exception e) {}
                        try { pRate = Double.parseDouble(item.getPurchaseRate()); } catch(Exception e) {}
                        try { mrp = Double.parseDouble(item.getMrp()); } catch(Exception e) {}
                        try { netAmt = Double.parseDouble(item.getNetAmount()); } catch(Exception e) {}
                        
                        // Insert Purchase Item
                        iStmt.setInt(1, purchaseId);
                        iStmt.setInt(2, medId);
                        iStmt.setString(3, batch);
                        iStmt.setDate(4, expDate);
                        iStmt.setInt(5, qty);
                        iStmt.setInt(6, free);
                        iStmt.setDouble(7, pRate);
                        iStmt.setDouble(8, mrp);
                        iStmt.setDouble(9, mrp); // default selling rate to MRP
                        iStmt.setDouble(10, netAmt);
                        iStmt.executeUpdate();
                        
                        // Handle Batch Inventory
                        cbStmt.setInt(1, medId);
                        cbStmt.setString(2, batch);
                        int batchId = -1;
                        try (java.sql.ResultSet rs = cbStmt.executeQuery()) {
                            if (rs.next()) batchId = rs.getInt(1);
                        }
                        
                        int totalQty = qty + free;
                        if (batchId != -1) {
                            ubStmt.setInt(1, totalQty);
                            ubStmt.setDouble(2, pRate);
                            ubStmt.setDouble(3, mrp);
                            ubStmt.setInt(4, batchId);
                            ubStmt.executeUpdate();
                        } else {
                            ibStmt.setInt(1, medId);
                            ibStmt.setString(2, batch);
                            ibStmt.setDate(3, expDate);
                            ibStmt.setDouble(4, pRate);
                            ibStmt.setDouble(5, mrp);
                            ibStmt.setDouble(6, mrp);
                            ibStmt.setInt(7, totalQty);
                            ibStmt.executeUpdate();
                        }
                    }
                }
                
                if (amtPaid > 0) {
                    String insertPayment = "INSERT INTO supplier_payments (purchase_id, supplier_id, amount_paid, payment_mode) VALUES (?, ?, ?, ?)";
                    try (java.sql.PreparedStatement payStmt = conn.prepareStatement(insertPayment)) {
                        payStmt.setInt(1, purchaseId);
                        payStmt.setInt(2, supplier.getSupplierId());
                        payStmt.setDouble(3, amtPaid);
                        payStmt.setString(4, payment);
                        payStmt.executeUpdate();
                    }
                }
                
                conn.commit();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Purchase saved successfully! Batch stocks updated.");
                cartItems.clear();
                updateTotals();
                if (txtInvoiceNo != null) txtInvoiceNo.clear();
                if (txtAmountPaid != null) txtAmountPaid.clear();
                
            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to save purchase: " + e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
