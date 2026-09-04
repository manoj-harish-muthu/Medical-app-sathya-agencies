package com.pharmacyerp.controller;

import com.pharmacyerp.dao.OutstandingDAO;
import com.pharmacyerp.model.Sale;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import java.math.BigDecimal;
import java.util.Optional;

public class OutstandingBillsController {
    
    @FXML private TableView<Sale> outstandingTable;
    @FXML private TableColumn<Sale, String> colInvoiceNo;
    @FXML private TableColumn<Sale, String> colDate;
    @FXML private TableColumn<Sale, String> colCustomer;
    @FXML private TableColumn<Sale, String> colPhone;
    @FXML private TableColumn<Sale, String> colTotal;
    @FXML private TableColumn<Sale, String> colPaid;
    @FXML private TableColumn<Sale, String> colBalance;
    
    @FXML private VBox paymentPane;
    @FXML private Label selectedInvoiceLabel;
    @FXML private Label selectedBalanceLabel;
    @FXML private TextField paymentAmountField;
    @FXML private ComboBox<String> paymentModeCombo;
    @FXML private TextField paymentNotesField;

    private OutstandingDAO outstandingDAO = new OutstandingDAO();
    private Sale selectedSale = null;

    @FXML
    public void initialize() {
        colInvoiceNo.setCellValueFactory(new PropertyValueFactory<>("invoiceNo"));
        colDate.setCellValueFactory(cellData -> {
            java.sql.Timestamp ts = cellData.getValue().getSaleDate();
            if (ts != null) {
                return new SimpleStringProperty(new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(ts));
            }
            return new SimpleStringProperty("");
        });
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("customerPhone"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("grandTotal"));
        colPaid.setCellValueFactory(new PropertyValueFactory<>("paidAmount"));
        colBalance.setCellValueFactory(new PropertyValueFactory<>("balanceAmount"));

        paymentModeCombo.setItems(FXCollections.observableArrayList("Cash", "UPI", "Card", "Bank Transfer"));
        paymentModeCombo.setValue("Cash");

        outstandingTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedSale = newVal;
                selectedInvoiceLabel.setText(newVal.getInvoiceNo());
                selectedBalanceLabel.setText(String.format("₹ %.2f", newVal.getBalanceAmount()));
                paymentAmountField.setText(newVal.getBalanceAmount().toString());
                paymentPane.setDisable(false);
            } else {
                selectedSale = null;
                selectedInvoiceLabel.setText("None");
                selectedBalanceLabel.setText("₹ 0.00");
                paymentAmountField.clear();
                paymentPane.setDisable(true);
            }
        });

        loadOutstandingBills();
    }

    private void loadOutstandingBills() {
        outstandingTable.setItems(FXCollections.observableArrayList(outstandingDAO.getOutstandingBills()));
        outstandingTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleProcessPayment(ActionEvent event) {
        if (selectedSale == null) return;
        
        try {
            BigDecimal amount = new BigDecimal(paymentAmountField.getText().trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0 || amount.compareTo(selectedSale.getBalanceAmount()) > 0) {
                showAlert("Invalid Amount", "Payment amount must be greater than 0 and less than or equal to the balance.");
                return;
            }
            
            String mode = paymentModeCombo.getValue();
            String notes = paymentNotesField.getText().trim();
            
            boolean success = outstandingDAO.addPayment(selectedSale.getSaleId(), selectedSale.getCustomerPhone(), amount, mode, notes);
            
            if (success) {
                showAlert("Success", "Payment recorded successfully.");
                loadOutstandingBills();
            } else {
                showAlert("Error", "Failed to record payment.");
            }
        } catch (NumberFormatException e) {
            showAlert("Invalid Amount", "Please enter a valid number for payment amount.");
        }
    }
    
    @FXML
    private void handleWhatsAppReminder(ActionEvent event) {
        if (selectedSale == null) return;
        
        String phone = selectedSale.getCustomerPhone();
        if (phone == null || phone.trim().isEmpty()) {
            showAlert("Error", "No phone number available for this customer.");
            return;
        }
        
        try {
            String cleanPhone = phone.replaceAll("[^0-9]", "");
            if (cleanPhone.length() == 10) {
                cleanPhone = "91" + cleanPhone; 
            }
            
            String customerName = selectedSale.getCustomerName() != null ? selectedSale.getCustomerName() : "Customer";
            String balance = String.format("₹%.2f", selectedSale.getBalanceAmount());
            String invoice = selectedSale.getInvoiceNo();
            
            String message = "Dear " + customerName + ", this is a gentle reminder that your bill (" + invoice + ") has a pending balance of " + balance + ". Please make the payment at your earliest convenience. Thank you!";
            String encodedMessage = java.net.URLEncoder.encode(message, "UTF-8").replace("+", "%20");
            
            String url = "https://wa.me/" + cleanPhone + "?text=" + encodedMessage;
            
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } else {
                showAlert("Error", "Desktop browsing is not supported on this system.");
            }
            
        } catch (Exception e) {
            showAlert("Error", "Failed to open WhatsApp: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
