package com.pharmacyerp.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import com.pharmacyerp.model.CartItem;
import com.pharmacyerp.dao.PosDAO;

import java.io.IOException;
import java.math.BigDecimal;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.Button;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;

public class PosController {

    @FXML private TextField medicineSearchField;
    @FXML private TableView<CartItem> cartTable;
    @FXML private TableColumn<CartItem, String> colMfr;
    @FXML private TableColumn<CartItem, String> colHsn;
    @FXML private TableColumn<CartItem, BigDecimal> colMrp;
    @FXML private TableColumn<CartItem, String> colBatch;
    @FXML private TableColumn<CartItem, String> colExp;
    @FXML private TableColumn<CartItem, String> colItem;
    @FXML private TableColumn<CartItem, String> colPack;
    @FXML private TableColumn<CartItem, Integer> colQty;
    @FXML private TableColumn<CartItem, BigDecimal> colRate;
    @FXML private TableColumn<CartItem, BigDecimal> colDisc;
    @FXML private TableColumn<CartItem, BigDecimal> colGst;
    @FXML private TableColumn<CartItem, BigDecimal> colAmount;
    @FXML private TableColumn<CartItem, Void> colAction;
    
    @FXML private TextField customerPhoneField;
    @FXML private TextField customerNameField;
    @FXML private TextField doctorNameField;
    
    @FXML private Label subtotalLabel;
    @FXML private Label discountLabel;
    @FXML private Label taxLabel;
    @FXML private Label totalLabel;
    
    @FXML private TextField paidAmountField;
    @FXML private Label balanceLabel;
    private BigDecimal currentGrandTotal = BigDecimal.ZERO;
    @FXML private Label dateLabel;
    
    @FXML private ComboBox<String> paymentModeCombo;

    private PosDAO posDAO = new PosDAO();
    private ObservableList<CartItem> cartItems = FXCollections.observableArrayList();
    private ContextMenu suggestionsPopup = new ContextMenu();
    private boolean requiresPrescription = false;

    @FXML
    public void initialize() {
        if (paidAmountField != null) {
            paidAmountField.textProperty().addListener((obs, oldVal, newVal) -> {
                updateBalance();
            });
        }
        
        paymentModeCombo.setItems(FXCollections.observableArrayList(
                "Cash", "UPI", "Card", "Credit"
        ));
        paymentModeCombo.getSelectionModel().selectFirst();
        
        // Initial values
        dateLabel.setText(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        
        setupTable();
        
        // Autocomplete logic
        medicineSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.trim().isEmpty()) {
                suggestionsPopup.hide();
            } else {
                java.util.List<CartItem> suggestions = posDAO.searchMedicines(newValue.trim());
                if (suggestions.isEmpty()) {
                    suggestionsPopup.hide();
                } else {
                    suggestionsPopup.getItems().clear();
                    for (CartItem item : suggestions) {
                        Label label = new Label(item.getMedicineName() + " (Batch: " + item.getBatchNumber() + " | MRP: ₹" + item.getMrp() + ")");
                        label.setStyle("-fx-font-size: 14px; -fx-padding: 5px;");
                        CustomMenuItem menuItem = new CustomMenuItem(label, true);
                        menuItem.setOnAction(e -> {
                            medicineSearchField.setText("");
                            suggestionsPopup.hide();
                            addItemToCart(item);
                        });
                        suggestionsPopup.getItems().add(menuItem);
                    }
                    if (!suggestionsPopup.isShowing()) {
                        // Position popup below the search field
                        suggestionsPopup.show(medicineSearchField, javafx.geometry.Side.BOTTOM, 0, 0);
                    }
                }
            }
        });
        
        // Barcode scanner acts as keyboard and hits ENTER
        medicineSearchField.setOnAction(e -> handleMedicineSearch());
    }

    private void setupTable() {
        cartTable.setEditable(true);

        colMfr.setCellValueFactory(new PropertyValueFactory<>("companyName"));
        colHsn.setCellValueFactory(new PropertyValueFactory<>("hsnCode"));
        colMrp.setCellValueFactory(new PropertyValueFactory<>("mrp"));
        colBatch.setCellValueFactory(new PropertyValueFactory<>("batchNumber"));
        colExp.setCellValueFactory(new PropertyValueFactory<>("expiryDateStr"));
        colItem.setCellValueFactory(new PropertyValueFactory<>("medicineName"));
        colPack.setCellValueFactory(new PropertyValueFactory<>("packing"));
        
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colQty.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colQty.setOnEditCommit(event -> {
            CartItem item = event.getRowValue();
            item.setQuantity(event.getNewValue());
            updateTotals();
            cartTable.refresh();
        });
        
        colRate.setCellValueFactory(new PropertyValueFactory<>("sellingRate"));
        colDisc.setCellValueFactory(new PropertyValueFactory<>("discountPercentage"));
        colGst.setCellValueFactory(new PropertyValueFactory<>("gstRate"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("netAmount"));
        
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("Delete");
            {
                deleteBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setOnAction(event -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    cartItems.remove(item);
                    updateTotals();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteBtn);
                }
            }
        });
        
        cartTable.setItems(cartItems);
    }

    private void handleMedicineSearch() {
        String query = medicineSearchField.getText().trim();
        if (query.isEmpty()) return;
        
        CartItem item = posDAO.getItemByBarcodeOrName(query);
        if (item != null) {
            addItemToCart(item);
            medicineSearchField.clear();
        }
    }
    
    private void addItemToCart(CartItem item) {
        boolean found = false;
        for (CartItem existing : cartItems) {
            if (existing.getBatchId() == item.getBatchId()) {
                existing.setQuantity(existing.getQuantity() + 1);
                found = true;
                break;
            }
        }
        if (!found) {
            cartItems.add(item);
            if ("H".equalsIgnoreCase(item.getScheduleType()) || "H1".equalsIgnoreCase(item.getScheduleType())) {
                requiresPrescription = true;
                doctorNameField.setStyle("-fx-border-color: red;");
                doctorNameField.setPromptText("Doctor Name (REQUIRED for Schedule H)");
            }
        }
        cartTable.refresh();
        updateTotals();
    }
    
    private void updateTotals() {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        
        for (CartItem item : cartItems) {
            subtotal = subtotal.add(item.getSellingRate().multiply(new BigDecimal(item.getQuantity())));
            discount = discount.add(item.getDiscountAmount());
            tax = tax.add(item.getCgstAmount()).add(item.getSgstAmount());
            total = total.add(item.getNetAmount());
        }
        
        currentGrandTotal = total;
        
        subtotalLabel.setText(String.format("₹ %.2f", subtotal));
        discountLabel.setText(String.format("₹ %.2f", discount));
        taxLabel.setText(String.format("₹ %.2f", tax));
        totalLabel.setText(String.format("₹ %.2f", total));
        
        updateBalance();
    }
    
    private void updateBalance() {
        try {
            String paidStr = paidAmountField.getText().trim();
            BigDecimal paid = paidStr.isEmpty() ? currentGrandTotal : new BigDecimal(paidStr);
            BigDecimal balance = currentGrandTotal.subtract(paid);
            // Optionally, allow negative balance as "change to return", but for ledger we store max(0) or just store what is owed.
            // Let's store actual balance (if < 0, they overpaid, usually return change, so balance owed is 0).
            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                balanceLabel.setText(String.format("Change: ₹ %.2f", balance.abs()));
            } else {
                balanceLabel.setText(String.format("₹ %.2f", balance));
            }
        } catch (NumberFormatException e) {
            balanceLabel.setText("₹ 0.00");
        }
    }

    @FXML
    private void handleCompleteSale(ActionEvent event) {
        if (cartItems.isEmpty()) {
            return;
        }
        
        if (requiresPrescription && doctorNameField.getText().trim().isEmpty()) {
            // Can't proceed without a doctor's name for Schedule H
            doctorNameField.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            return;
        }

        String invoiceNo = "INV-" + System.currentTimeMillis();
        String customer = customerNameField.getText().trim();
        String customerPhone = customerPhoneField.getText().trim();
        String doctor = doctorNameField.getText().trim();
        String paymentMode = paymentModeCombo.getValue();
        String pdfPath = "invoice_" + invoiceNo + ".pdf";

        String paidStr = paidAmountField.getText().trim();
        BigDecimal paidAmount = paidStr.isEmpty() ? currentGrandTotal : new BigDecimal(paidStr);
        BigDecimal balanceAmount = currentGrandTotal.subtract(paidAmount);
        if (balanceAmount.compareTo(BigDecimal.ZERO) < 0) balanceAmount = BigDecimal.ZERO;

        // Save to Database
        boolean saved = posDAO.saveSale(invoiceNo, customer, customerPhone, doctor, paymentMode, cartItems, paidAmount, balanceAmount);
        
        if (!saved) {
            System.err.println("Failed to save sale to database!");
            return; // Maybe show an alert in real app
        }

        com.pharmacyerp.util.InvoicePrinter.printInvoice(invoiceNo, customer, doctor, cartItems, pdfPath);
        
        System.out.println("Sale completed, saved to DB, and invoice saved to " + pdfPath);
        
        // Open the PDF automatically
        try {
            java.io.File pdfFile = new java.io.File(pdfPath);
            if (pdfFile.exists() && java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(pdfFile);
            }
        } catch (Exception e) {
            System.err.println("Failed to open PDF: " + e.getMessage());
        }
        
        // Reset POS
        cartItems.clear();
        cartTable.refresh();
        customerNameField.clear();
        customerPhoneField.clear();
        doctorNameField.clear();
        requiresPrescription = false;
        doctorNameField.setStyle("");
        doctorNameField.setPromptText("Doctor Name (Optional)");
        if (paidAmountField != null) paidAmountField.clear();
        if (balanceLabel != null) balanceLabel.setText("₹ 0.00");
        updateTotals();
    }

    @FXML
    private void handleDashboard(ActionEvent event) {
        navigateTo(event, "/fxml/Dashboard.fxml");
    }

    private void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
