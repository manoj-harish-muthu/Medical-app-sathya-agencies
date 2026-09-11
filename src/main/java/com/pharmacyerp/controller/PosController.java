package com.pharmacyerp.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import com.pharmacyerp.model.CartItem;
import com.pharmacyerp.dao.PosDAO;
import javafx.geometry.Insets;
import javafx.scene.layout.Region;

import java.math.BigDecimal;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.Button;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;

public class PosController {

    @FXML private Label itemsCountLabel;
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
    private com.pharmacyerp.model.AgentOrder activeAgentOrder;

    @FXML
    public void initialize() {
        if (paidAmountField != null) {
            paidAmountField.textProperty().addListener((obs, oldVal, newVal) -> {
                updateBalance();
            });
        }

        if (customerPhoneField != null) {
            customerPhoneField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    lookupCustomer();
                }
            });
            customerPhoneField.setOnAction(e -> {
                lookupCustomer();
                if (customerNameField != null && customerNameField.getText().trim().isEmpty()) {
                    customerNameField.requestFocus();
                } else if (medicineSearchField != null) {
                    medicineSearchField.requestFocus();
                }
            });
        }
        
        if (paymentModeCombo != null) {
            paymentModeCombo.setItems(FXCollections.observableArrayList(
                    "Cash", "UPI", "Card", "Credit"
            ));
            paymentModeCombo.getSelectionModel().selectFirst();
        }
        
        // Initial values
        if (dateLabel != null) {
            dateLabel.setText(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
        
        setupTable();
        updateTotals();
        
        // Autocomplete logic with rich high-contrast popup cards
        medicineSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                suggestionsPopup.hide();
            } else {
                java.util.List<CartItem> suggestions = posDAO.searchMedicines(newValue.trim());
                if (suggestions.isEmpty()) {
                    suggestionsPopup.hide();
                } else {
                    suggestionsPopup.getItems().clear();
                    for (CartItem item : suggestions) {
                        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(12);
                        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                        row.setPadding(new Insets(6, 12, 6, 12));
                        
                        Label nameLbl = new Label(item.getMedicineName());
                        nameLbl.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #111827;");
                        
                        Label packLbl = new Label(item.getPacking() != null ? item.getPacking() : "");
                        packLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #4B5563;");
                        
                        Label batchLbl = new Label("Batch: " + (item.getBatchNumber() != null ? item.getBatchNumber() : "-"));
                        batchLbl.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #6B7280;");
                        
                        Label expLbl = new Label("Exp: " + (item.getExpiryDateStr() != null ? item.getExpiryDateStr() : "-"));
                        expLbl.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #6B7280;");
                        
                        Region spacer = new Region();
                        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                        
                        Label priceLbl = new Label("₹ " + String.format("%.2f", item.getSellingRate()));
                        priceLbl.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #059669;");
                        
                        row.getChildren().addAll(nameLbl, packLbl, batchLbl, expLbl, spacer, priceLbl);
                        row.setStyle("-fx-cursor: hand;");
                        row.setOnMouseClicked(e -> {
                            medicineSearchField.setText("");
                            suggestionsPopup.hide();
                            addItemToCart(item);
                        });

                        CustomMenuItem menuItem = new CustomMenuItem(row, true);
                        menuItem.setOnAction(e -> {
                            medicineSearchField.setText("");
                            suggestionsPopup.hide();
                            addItemToCart(item);
                        });
                        suggestionsPopup.getItems().add(menuItem);
                    }
                    if (!suggestionsPopup.isShowing()) {
                        suggestionsPopup.show(medicineSearchField, javafx.geometry.Side.BOTTOM, 0, 0);
                    }
                }
            }
        });
        
        // Barcode scanner hits Enter
        medicineSearchField.setOnAction(e -> handleMedicineSearch());
        
        // Global Keyboard Shortcuts (F1, F2, F3, F12)
        javafx.application.Platform.runLater(() -> {
            if (medicineSearchField.getScene() != null) {
                medicineSearchField.getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == javafx.scene.input.KeyCode.F1) {
                        medicineSearchField.requestFocus();
                        medicineSearchField.selectAll();
                        event.consume();
                    } else if (event.getCode() == javafx.scene.input.KeyCode.F2) {
                        if (customerPhoneField != null) {
                            customerPhoneField.requestFocus();
                            customerPhoneField.selectAll();
                        }
                        event.consume();
                    } else if (event.getCode() == javafx.scene.input.KeyCode.F3) {
                        handleClearBill(null);
                        event.consume();
                    } else if (event.getCode() == javafx.scene.input.KeyCode.F12) {
                        handleCompleteSale(null);
                        event.consume();
                    } else if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                        suggestionsPopup.hide();
                    }
                });
            }
        });
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

    @FXML
    private void handleMedicineSearch() {
        String query = medicineSearchField.getText() != null ? medicineSearchField.getText().trim() : "";
        if (query.isEmpty()) return;
        
        CartItem item = posDAO.getItemByBarcodeOrName(query);
        if (item != null) {
            addItemToCart(item);
            medicineSearchField.clear();
            suggestionsPopup.hide();
            return;
        }

        // Fallback to search suggestions
        java.util.List<CartItem> suggestions = posDAO.searchMedicines(query);
        if (!suggestions.isEmpty()) {
            addItemToCart(suggestions.get(0));
            medicineSearchField.clear();
            suggestionsPopup.hide();
        } else {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING,
                    "No medicine found matching \"" + query + "\"",
                    javafx.scene.control.ButtonType.OK
            );
            alert.setHeaderText("Medicine Not Found");
            alert.setTitle("Search Result");
            alert.showAndWait();
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
        
        if (itemsCountLabel != null) {
            int count = cartItems.size();
            itemsCountLabel.setText(count + (count == 1 ? " item in cart" : " items in cart"));
        }
        
        updateBalance();
    }
    
    private void updateBalance() {
        try {
            String paidStr = paidAmountField.getText().trim();
            BigDecimal paid = paidStr.isEmpty() ? currentGrandTotal : new BigDecimal(paidStr);
            BigDecimal balance = currentGrandTotal.subtract(paid);
            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                balanceLabel.setText(String.format("Change: ₹ %.2f", balance.abs()));
                balanceLabel.setStyle("-fx-font-weight: 800; -fx-text-fill: #16A34A; -fx-font-size: 16px;");
            } else if (balance.compareTo(BigDecimal.ZERO) == 0) {
                balanceLabel.setText("₹ 0.00");
                balanceLabel.setStyle("-fx-font-weight: 800; -fx-text-fill: #16A34A; -fx-font-size: 16px;");
            } else {
                balanceLabel.setText(String.format("₹ %.2f", balance));
                balanceLabel.setStyle("-fx-font-weight: 800; -fx-text-fill: #DC2626; -fx-font-size: 16px;");
            }
        } catch (NumberFormatException e) {
            balanceLabel.setText("₹ 0.00");
            balanceLabel.setStyle("-fx-font-weight: 800; -fx-text-fill: #111827; -fx-font-size: 16px;");
        }
    }

    // Quick Tender Button Handlers
    @FXML
    private void handleExactTender(ActionEvent event) {
        if (paidAmountField != null) {
            paidAmountField.setText(currentGrandTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            updateBalance();
        }
    }

    @FXML
    private void handleTender100(ActionEvent event) {
        addTenderAmount(new BigDecimal("100"));
    }

    @FXML
    private void handleTender200(ActionEvent event) {
        addTenderAmount(new BigDecimal("200"));
    }

    @FXML
    private void handleTender500(ActionEvent event) {
        addTenderAmount(new BigDecimal("500"));
    }

    @FXML
    private void handleTender2000(ActionEvent event) {
        addTenderAmount(new BigDecimal("2000"));
    }

    private void addTenderAmount(BigDecimal addAmount) {
        if (paidAmountField == null) return;
        try {
            String cur = paidAmountField.getText().trim();
            BigDecimal val = cur.isEmpty() ? BigDecimal.ZERO : new BigDecimal(cur);
            val = val.add(addAmount);
            paidAmountField.setText(val.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        } catch (Exception e) {
            paidAmountField.setText(addAmount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        }
        updateBalance();
    }

    // Clear Bill with Confirmation
    @FXML
    private void handleClearBill(ActionEvent event) {
        if (cartItems.isEmpty() && (customerNameField == null || customerNameField.getText().trim().isEmpty())) {
            return;
        }
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION,
                "Are you sure you want to clear the current bill?",
                javafx.scene.control.ButtonType.YES,
                javafx.scene.control.ButtonType.NO
        );
        alert.setHeaderText("Clear Current Bill");
        alert.setTitle("Confirm Clear");
        alert.showAndWait().ifPresent(type -> {
            if (type == javafx.scene.control.ButtonType.YES) {
                resetBillingForm();
            }
        });
    }

    // Suspend Bill
    @FXML
    private void handleSuspendBill(ActionEvent event) {
        if (cartItems.isEmpty()) {
            return;
        }
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION,
                "Bill with " + cartItems.size() + " items suspended (Total: ₹ " + currentGrandTotal + ")",
                javafx.scene.control.ButtonType.OK
        );
        alert.setHeaderText("Bill Held / Suspended");
        alert.setTitle("Suspended Bill");
        alert.showAndWait();
        resetBillingForm();
    }

    private void lookupCustomer() {
        if (customerPhoneField == null || customerNameField == null) return;
        String phone = customerPhoneField.getText().trim();
        if (phone.length() >= 7) {
            String name = posDAO.getCustomerNameByPhone(phone);
            if (name != null && !name.trim().isEmpty()) {
                customerNameField.setText(name);
            }
        }
    }

    private void resetBillingForm() {
        activeAgentOrder = null;
        cartItems.clear();
        cartTable.refresh();
        if (customerNameField != null) customerNameField.clear();
        if (customerPhoneField != null) customerPhoneField.clear();
        if (doctorNameField != null) {
            doctorNameField.clear();
            doctorNameField.setStyle("");
            doctorNameField.setPromptText("Doctor Name (Optional)");
        }
        requiresPrescription = false;
        if (medicineSearchField != null) medicineSearchField.clear();
        if (paidAmountField != null) paidAmountField.clear();
        if (balanceLabel != null) balanceLabel.setText("₹ 0.00");
        updateTotals();
    }

    @FXML
    private void handleCompleteSale(ActionEvent event) {
        if (cartItems.isEmpty()) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING,
                    "Cart is empty! Please search and add medicines before completing the sale.",
                    javafx.scene.control.ButtonType.OK
            );
            alert.setHeaderText("Empty Cart");
            alert.setTitle("Cannot Complete Sale");
            alert.showAndWait();
            medicineSearchField.requestFocus();
            return;
        }
        
        if (requiresPrescription && (doctorNameField == null || doctorNameField.getText().trim().isEmpty())) {
            if (doctorNameField != null) {
                doctorNameField.setStyle("-fx-border-color: #DC2626; -fx-border-width: 2px;");
                doctorNameField.requestFocus();
            }
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING,
                    "Doctor name is required because this bill contains Schedule H / H1 / X medicines.",
                    javafx.scene.control.ButtonType.OK
            );
            alert.setHeaderText("Prescription Required");
            alert.setTitle("Doctor Name Required");
            alert.showAndWait();
            return;
        }

        String invoiceNo = "INV-" + System.currentTimeMillis();
        String customer = (customerNameField != null && !customerNameField.getText().trim().isEmpty()) 
                ? customerNameField.getText().trim() : "Walk-in Customer";
        String customerPhone = (customerPhoneField != null) ? customerPhoneField.getText().trim() : "";
        String doctor = (doctorNameField != null) ? doctorNameField.getText().trim() : "";
        String paymentMode = (paymentModeCombo != null && paymentModeCombo.getValue() != null) 
                ? paymentModeCombo.getValue() : "Cash";
        String pdfPath = "invoice_" + invoiceNo + ".pdf";

        BigDecimal paidAmount = currentGrandTotal;
        if (paidAmountField != null && !paidAmountField.getText().trim().isEmpty()) {
            try {
                paidAmount = new BigDecimal(paidAmountField.getText().trim());
            } catch (Exception ignored) {
                paidAmount = currentGrandTotal;
            }
        }
        BigDecimal balanceAmount = currentGrandTotal.subtract(paidAmount);
        if (balanceAmount.compareTo(BigDecimal.ZERO) < 0) balanceAmount = BigDecimal.ZERO;

        // Save to Database
        boolean saved = posDAO.saveSale(invoiceNo, customer, customerPhone, doctor, paymentMode, cartItems, paidAmount, balanceAmount);
        
        if (!saved) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR,
                    "Failed to save sale to the database! Please check database connection.",
                    javafx.scene.control.ButtonType.OK
            );
            alert.setHeaderText("Database Error");
            alert.setTitle("Save Failed");
            alert.showAndWait();
            return;
        }

        // If this sale was initiated from an Agent Order, mark it as completed & billed
        if (activeAgentOrder != null) {
            try {
                activeAgentOrder.setCheckedByAdmin(true);
                activeAgentOrder.setAdminStatus("completed");

                com.pharmacyerp.dao.AgentOrderDAO agentOrderDAO = new com.pharmacyerp.dao.AgentOrderDAO();
                agentOrderDAO.updateOrderStatus(activeAgentOrder.getFilePath(), activeAgentOrder.getCustomerId(), "completed", true);

                if (activeAgentOrder.getFilePath() != null) {
                    java.io.File f = new java.io.File(activeAgentOrder.getFilePath());
                    if (f.exists()) {
                        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                        com.fasterxml.jackson.databind.JsonNode root = om.readTree(f);
                        if (root.isObject()) {
                            ((com.fasterxml.jackson.databind.node.ObjectNode) root).put("is_checked_by_admin", true);
                            ((com.fasterxml.jackson.databind.node.ObjectNode) root).put("admin_status", "completed");
                            ((com.fasterxml.jackson.databind.node.ObjectNode) root).put("completed_at", java.time.LocalDateTime.now().toString());
                            ((com.fasterxml.jackson.databind.node.ObjectNode) root).put("invoice_no", invoiceNo);
                            om.writerWithDefaultPrettyPrinter().writeValue(f, root);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Notice: Could not update agent order status upon sale completion: " + e.getMessage());
            }
        }


        try {
            com.pharmacyerp.util.InvoicePrinter.printInvoice(invoiceNo, customer, doctor, cartItems, pdfPath);
            System.out.println("Sale completed, saved to DB, and invoice saved to " + pdfPath);
            
            java.io.File pdfFile = new java.io.File(pdfPath);
            if (pdfFile.exists() && java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(pdfFile);
            }
        } catch (Exception e) {
            System.err.println("Notice: Could not automatically open PDF: " + e.getMessage());
        }

        javafx.scene.control.Alert successAlert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION,
                String.format("Sale completed successfully!\n\nInvoice No: %s\nCustomer: %s\nGrand Total: ₹ %.2f\nPayment Mode: %s",
                        invoiceNo, customer, currentGrandTotal, paymentMode),
                javafx.scene.control.ButtonType.OK
        );
        successAlert.setHeaderText("Sale Completed Successfully");
        successAlert.setTitle("Sale Completed");
        successAlert.showAndWait();
        
        resetBillingForm();
    }

    public void loadAgentOrder(com.pharmacyerp.model.AgentOrder order) {
        if (order == null) return;
        this.activeAgentOrder = order;
        if (customerPhoneField != null && order.getPhone() != null && !order.getPhone().isBlank()) {
            customerPhoneField.setText(order.getPhone());
        }
        if (customerNameField != null && order.getCustomerName() != null && !order.getCustomerName().isBlank()) {
            customerNameField.setText(order.getCustomerName());
        }
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (com.pharmacyerp.model.AgentOrder.OrderItem it : order.getItems()) {
                String medName = it.getMedicine();
                if (medName != null && !medName.isBlank()) {
                    CartItem found = posDAO.getItemByBarcodeOrName(medName);
                    if (found != null) {
                        try {
                            int q = 1;
                            String qStr = it.getQuantity().replaceAll("[^0-9]", "");
                            if (!qStr.isEmpty()) {
                                q = Math.max(1, Integer.parseInt(qStr));
                            }
                            found.setQuantity(q);
                            found.calculateTotals();
                            cartItems.add(found);
                        } catch (Exception ignored) {
                            cartItems.add(found);
                        }
                    }
                }
            }
            updateTotals();
        }
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
        } catch (Throwable e) {
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Unable to open screen: " + fxmlPath);
            alert.setContentText(e.getMessage() != null ? e.getMessage() : e.toString());
            alert.showAndWait();
        }
    }
}
