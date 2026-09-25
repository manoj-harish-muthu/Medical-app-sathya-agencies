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

import com.pharmacyerp.model.Supplier;
import com.pharmacyerp.model.Medicine;
import com.pharmacyerp.dao.SupplierDAO;
import com.pharmacyerp.dao.MedicineDAO;

public class PurchaseReturnController {
    
    @FXML private ComboBox<Supplier> cmbSupplier;
    @FXML private TextField txtInvoiceNo;
    @FXML private DatePicker dpReturnDate;
    @FXML private ComboBox<String> cmbReason;
    
    @FXML private TextField medicineSearchField;
    @FXML private TableView<ReturnItem> tblCart;
    
    @FXML private TableColumn<ReturnItem, String> colMedicine;
    @FXML private TableColumn<ReturnItem, String> colBatch;
    @FXML private TableColumn<ReturnItem, String> colQty;
    @FXML private TableColumn<ReturnItem, String> colRate;
    @FXML private TableColumn<ReturnItem, String> colTotal;
    
    @FXML private Label lblTotalItems;
    @FXML private Label lblGrandTotal;
    
    private ObservableList<ReturnItem> cartItems = FXCollections.observableArrayList();
    private ContextMenu suggestionsPopup = new ContextMenu();

    public static class ReturnItem {
        private int medicineId;
        private SimpleStringProperty medicineName;
        private SimpleStringProperty batchNumber;
        private SimpleStringProperty quantity;
        private SimpleStringProperty purchaseRate;
        private SimpleStringProperty netAmount;
        
        public ReturnItem(int medicineId, String medicineName) {
            this.medicineId = medicineId;
            this.medicineName = new SimpleStringProperty(medicineName);
            this.batchNumber = new SimpleStringProperty("");
            this.quantity = new SimpleStringProperty("1");
            this.purchaseRate = new SimpleStringProperty("0.00");
            this.netAmount = new SimpleStringProperty("0.00");
        }
        
        public int getMedicineId() { return medicineId; }
        
        public String getMedicineName() { return medicineName.get(); }
        public SimpleStringProperty medicineNameProperty() { return medicineName; }
        
        public String getBatchNumber() { return batchNumber.get(); }
        public SimpleStringProperty batchNumberProperty() { return batchNumber; }
        public void setBatchNumber(String value) { this.batchNumber.set(value); }
        
        public String getQuantity() { return quantity.get(); }
        public SimpleStringProperty quantityProperty() { return quantity; }
        public void setQuantity(String value) { this.quantity.set(value); updateNetAmt(); }
        
        public String getPurchaseRate() { return purchaseRate.get(); }
        public SimpleStringProperty purchaseRateProperty() { return purchaseRate; }
        public void setPurchaseRate(String value) { this.purchaseRate.set(value); updateNetAmt(); }
        
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
        if (cmbReason != null) {
            cmbReason.getItems().addAll("Expired", "Damage", "Excess Stock", "Wrong Item");
            cmbReason.getSelectionModel().selectFirst();
        }
        if (dpReturnDate != null) {
            dpReturnDate.setValue(LocalDate.now());
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
        
        colQty.setCellFactory(TextFieldTableCell.forTableColumn());
        colQty.setOnEditCommit(e -> {
            e.getRowValue().setQuantity(e.getNewValue());
            updateTotals();
        });
        
        colRate.setCellFactory(TextFieldTableCell.forTableColumn());
        colRate.setOnEditCommit(e -> {
            e.getRowValue().setPurchaseRate(e.getNewValue());
            updateTotals();
            tblCart.refresh();
        });
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
        String lastBatch = "";
        String sql = "SELECT purchase_rate, batch_number FROM medicine_batches WHERE medicine_id = ? ORDER BY batch_id DESC LIMIT 1";
        try (java.sql.Connection conn = com.pharmacyerp.database.DatabaseManager.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, m.getMedicineId());
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    lastPRate = String.format("%.2f", rs.getDouble("purchase_rate"));
                    lastBatch = rs.getString("batch_number");
                }
            }
        } catch (Exception e) {}
        
        ReturnItem newItem = new ReturnItem(m.getMedicineId(), m.getMedicineName());
        newItem.setPurchaseRate(lastPRate);
        newItem.setBatchNumber(lastBatch);
        
        cartItems.add(newItem);
        updateTotals();
    }
    
    private void updateTotals() {
        lblTotalItems.setText(String.valueOf(cartItems.size()));
        double grandTotal = 0.0;
        for (ReturnItem item : cartItems) {
            try { grandTotal += Double.parseDouble(item.getNetAmount()); } catch (Exception e) {}
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
    private void handleSaveReturn(ActionEvent event) {
        if (cmbSupplier.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select a supplier!");
            return;
        }
        if (cartItems.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Return cart is empty!");
            return;
        }
        
        Supplier supplier = cmbSupplier.getValue();
        String invoice = txtInvoiceNo.getText() != null ? txtInvoiceNo.getText().trim() : "";
        String reason = cmbReason.getValue() != null ? cmbReason.getValue() : "Expired";
        java.sql.Date dDate = dpReturnDate.getValue() != null ? java.sql.Date.valueOf(dpReturnDate.getValue()) : null;
        
        double grandTotal = 0.0;
        for (ReturnItem item : cartItems) {
            try { grandTotal += Double.parseDouble(item.getNetAmount()); } catch (Exception e) {}
        }
        
        String insertReturn = "INSERT INTO purchase_returns (supplier_id, original_invoice_no, return_date, reason, total_items, grand_total) VALUES (?, ?, ?, ?, ?, ?)";
        String insertItem = "INSERT INTO purchase_return_items (pr_id, medicine_id, batch_number, quantity, purchase_rate, return_amount) VALUES (?, ?, ?, ?, ?, ?)";
        String updateBatch = "UPDATE medicine_batches SET current_quantity = current_quantity - ? WHERE medicine_id = ? AND batch_number = ?";
        
        try (java.sql.Connection conn = com.pharmacyerp.database.DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            
            try (java.sql.PreparedStatement pStmt = conn.prepareStatement(insertReturn, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                pStmt.setInt(1, supplier.getSupplierId());
                pStmt.setString(2, invoice);
                pStmt.setDate(3, dDate);
                pStmt.setString(4, reason);
                pStmt.setInt(5, cartItems.size());
                pStmt.setDouble(6, grandTotal);
                pStmt.executeUpdate();
                
                int prId = -1;
                try (java.sql.ResultSet rs = pStmt.getGeneratedKeys()) {
                    if (rs.next()) prId = rs.getInt(1);
                }
                
                try (java.sql.PreparedStatement iStmt = conn.prepareStatement(insertItem);
                     java.sql.PreparedStatement ubStmt = conn.prepareStatement(updateBatch)) {
                    for (ReturnItem item : cartItems) {
                        int medId = item.getMedicineId();
                        String batch = item.getBatchNumber();
                        int qty = 0;
                        double pRate = 0, netAmt = 0;
                        try { qty = Integer.parseInt(item.getQuantity()); } catch(Exception e) {}
                        try { pRate = Double.parseDouble(item.getPurchaseRate()); } catch(Exception e) {}
                        try { netAmt = Double.parseDouble(item.getNetAmount()); } catch(Exception e) {}
                        
                        iStmt.setInt(1, prId);
                        iStmt.setInt(2, medId);
                        iStmt.setString(3, batch);
                        iStmt.setInt(4, qty);
                        iStmt.setDouble(5, pRate);
                        iStmt.setDouble(6, netAmt);
                        iStmt.executeUpdate();
                        
                        // Deduct stock from inventory
                        if (batch != null && !batch.isEmpty()) {
                            ubStmt.setInt(1, qty);
                            ubStmt.setInt(2, medId);
                            ubStmt.setString(3, batch);
                            ubStmt.executeUpdate();
                        }
                    }
                }
                
                conn.commit();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Purchase Return saved successfully! Inventory deducted.");
                cartItems.clear();
                updateTotals();
                if (txtInvoiceNo != null) txtInvoiceNo.clear();
                
            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to save return: " + e.getMessage());
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
