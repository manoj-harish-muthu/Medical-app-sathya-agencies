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

public class PurchaseOrdersController {
    
    @FXML private ComboBox<Supplier> cmbSupplier;
    @FXML private DatePicker dpDeliveryDate;
    @FXML private ComboBox<String> cmbStatus;
    
    @FXML private TextField medicineSearchField;
    @FXML private TableView<OrderItem> tblCart;
    
    @FXML private TableColumn<OrderItem, String> colMedicine;
    @FXML private TableColumn<OrderItem, String> colQty;
    @FXML private TableColumn<OrderItem, String> colEstRate;
    @FXML private TableColumn<OrderItem, String> colTotal;
    
    @FXML private Label lblTotalItems;
    @FXML private Label lblGrandTotal;
    
    private ObservableList<OrderItem> cartItems = FXCollections.observableArrayList();
    private ContextMenu suggestionsPopup = new ContextMenu();

    public static class OrderItem {
        private int medicineId;
        private SimpleStringProperty medicineName;
        private SimpleStringProperty quantity;
        private SimpleStringProperty purchaseRate;
        private SimpleStringProperty netAmount;
        
        public OrderItem(int medicineId, String medicineName) {
            this.medicineId = medicineId;
            this.medicineName = new SimpleStringProperty(medicineName);
            this.quantity = new SimpleStringProperty("1");
            this.purchaseRate = new SimpleStringProperty("0.00");
            this.netAmount = new SimpleStringProperty("0.00");
        }
        
        public int getMedicineId() { return medicineId; }
        
        public String getMedicineName() { return medicineName.get(); }
        public SimpleStringProperty medicineNameProperty() { return medicineName; }
        
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
        if (cmbStatus != null) {
            cmbStatus.getItems().addAll("Pending", "Completed", "Cancelled");
            cmbStatus.getSelectionModel().selectFirst();
        }
        if (dpDeliveryDate != null) {
            dpDeliveryDate.setValue(LocalDate.now().plusDays(3));
        }
        
        tblCart.setItems(cartItems);
        tblCart.setEditable(true);
        tblCart.getSelectionModel().setCellSelectionEnabled(true);
        
        setupColumns();
        setupSearch();
    }
    
    private void setupColumns() {
        if (colQty == null) return;
        
        colQty.setCellFactory(TextFieldTableCell.forTableColumn());
        colQty.setOnEditCommit(e -> {
            e.getRowValue().setQuantity(e.getNewValue());
            updateTotals();
        });
        
        colEstRate.setCellFactory(TextFieldTableCell.forTableColumn());
        colEstRate.setOnEditCommit(e -> {
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
        String sql = "SELECT purchase_rate FROM medicine_batches WHERE medicine_id = ? ORDER BY batch_id DESC LIMIT 1";
        try (java.sql.Connection conn = com.pharmacyerp.database.DatabaseManager.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, m.getMedicineId());
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    lastPRate = String.format("%.2f", rs.getDouble("purchase_rate"));
                }
            }
        } catch (Exception e) {}
        
        OrderItem newItem = new OrderItem(m.getMedicineId(), m.getMedicineName());
        newItem.setPurchaseRate(lastPRate);
        cartItems.add(newItem);
        updateTotals();
    }
    
    private void updateTotals() {
        lblTotalItems.setText(String.valueOf(cartItems.size()));
        double grandTotal = 0.0;
        for (OrderItem item : cartItems) {
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
    private void handleSaveOrder(ActionEvent event) {
        if (cmbSupplier.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select a supplier!");
            return;
        }
        if (cartItems.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Order cart is empty!");
            return;
        }
        
        Supplier supplier = cmbSupplier.getValue();
        String status = cmbStatus.getValue() != null ? cmbStatus.getValue() : "Pending";
        java.sql.Date dDate = dpDeliveryDate.getValue() != null ? java.sql.Date.valueOf(dpDeliveryDate.getValue()) : null;
        
        double grandTotal = 0.0;
        for (OrderItem item : cartItems) {
            try { grandTotal += Double.parseDouble(item.getNetAmount()); } catch (Exception e) {}
        }
        
        String insertOrder = "INSERT INTO purchase_orders (supplier_id, expected_delivery_date, status, total_items, grand_total) VALUES (?, ?, ?, ?, ?)";
        String insertItem = "INSERT INTO purchase_order_items (po_id, medicine_id, quantity, est_rate, est_amount) VALUES (?, ?, ?, ?, ?)";
        
        try (java.sql.Connection conn = com.pharmacyerp.database.DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            
            try (java.sql.PreparedStatement pStmt = conn.prepareStatement(insertOrder, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                pStmt.setInt(1, supplier.getSupplierId());
                pStmt.setDate(2, dDate);
                pStmt.setString(3, status);
                pStmt.setInt(4, cartItems.size());
                pStmt.setDouble(5, grandTotal);
                pStmt.executeUpdate();
                
                int poId = -1;
                try (java.sql.ResultSet rs = pStmt.getGeneratedKeys()) {
                    if (rs.next()) poId = rs.getInt(1);
                }
                
                try (java.sql.PreparedStatement iStmt = conn.prepareStatement(insertItem)) {
                    for (OrderItem item : cartItems) {
                        int medId = item.getMedicineId();
                        int qty = 0;
                        double pRate = 0, netAmt = 0;
                        try { qty = Integer.parseInt(item.getQuantity()); } catch(Exception e) {}
                        try { pRate = Double.parseDouble(item.getPurchaseRate()); } catch(Exception e) {}
                        try { netAmt = Double.parseDouble(item.getNetAmount()); } catch(Exception e) {}
                        
                        iStmt.setInt(1, poId);
                        iStmt.setInt(2, medId);
                        iStmt.setInt(3, qty);
                        iStmt.setDouble(4, pRate);
                        iStmt.setDouble(5, netAmt);
                        iStmt.executeUpdate();
                    }
                }
                
                conn.commit();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Purchase Order saved successfully!");
                cartItems.clear();
                updateTotals();
                
            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to save order: " + e.getMessage());
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
