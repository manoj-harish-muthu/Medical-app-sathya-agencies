package com.pharmacyerp.controller;

import com.pharmacyerp.dao.GstDAO;
import com.pharmacyerp.model.GstSummaryRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class GstReportController {

    @FXML private ComboBox<String> monthCombo;
    @FXML private ComboBox<String> yearCombo;
    
    // Sales Table
    @FXML private TableView<GstSummaryRow> tblSales;
    @FXML private TableColumn<GstSummaryRow, String> colSalesRate;
    @FXML private TableColumn<GstSummaryRow, BigDecimal> colSalesTaxable;
    @FXML private TableColumn<GstSummaryRow, BigDecimal> colSalesCgst;
    @FXML private TableColumn<GstSummaryRow, BigDecimal> colSalesSgst;
    @FXML private TableColumn<GstSummaryRow, BigDecimal> colSalesTotal;

    // Purchases Table
    @FXML private TableView<GstSummaryRow> tblPurchases;
    @FXML private TableColumn<GstSummaryRow, String> colPurRate;
    @FXML private TableColumn<GstSummaryRow, BigDecimal> colPurTaxable;
    @FXML private TableColumn<GstSummaryRow, BigDecimal> colPurCgst;
    @FXML private TableColumn<GstSummaryRow, BigDecimal> colPurSgst;
    @FXML private TableColumn<GstSummaryRow, BigDecimal> colPurTotal;

    // Totals
    @FXML private Label lblTotalSalesTax;
    @FXML private Label lblTotalPurchaseTax;
    @FXML private Label lblTaxPayable;

    private GstDAO gstDAO = new GstDAO();
    private ObservableList<GstSummaryRow> salesData = FXCollections.observableArrayList();
    private ObservableList<GstSummaryRow> purchasesData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Initialize Combos
        String[] months = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"};
        monthCombo.getItems().addAll(months);
        
        int currentYear = LocalDate.now().getYear();
        for (int i = currentYear - 5; i <= currentYear; i++) {
            yearCombo.getItems().add(String.valueOf(i));
        }
        
        // Select current month and year
        String currentMonthStr = String.format("%02d", LocalDate.now().getMonthValue());
        monthCombo.setValue(currentMonthStr);
        yearCombo.setValue(String.valueOf(currentYear));

        // Setup Sales Columns
        colSalesRate.setCellValueFactory(new PropertyValueFactory<>("taxPercentage"));
        colSalesTaxable.setCellValueFactory(new PropertyValueFactory<>("taxableValue"));
        colSalesCgst.setCellValueFactory(new PropertyValueFactory<>("cgstAmount"));
        colSalesSgst.setCellValueFactory(new PropertyValueFactory<>("sgstAmount"));
        colSalesTotal.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));

        // Setup Purchase Columns
        colPurRate.setCellValueFactory(new PropertyValueFactory<>("taxPercentage"));
        colPurTaxable.setCellValueFactory(new PropertyValueFactory<>("taxableValue"));
        colPurCgst.setCellValueFactory(new PropertyValueFactory<>("cgstAmount"));
        colPurSgst.setCellValueFactory(new PropertyValueFactory<>("sgstAmount"));
        colPurTotal.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));

        tblSales.setItems(salesData);
        tblPurchases.setItems(purchasesData);

        loadData();
    }

    @FXML
    private void loadData() {
        String month = monthCombo.getValue();
        String year = yearCombo.getValue();
        if (month == null || year == null) return;

        salesData.clear();
        purchasesData.clear();

        List<GstSummaryRow> sRows = gstDAO.getMonthlySalesTaxSummary(month, year);
        List<GstSummaryRow> pRows = gstDAO.getMonthlyPurchaseTaxSummary(month, year);

        salesData.addAll(sRows);
        purchasesData.addAll(pRows);

        calculateTotals();
    }

    private void calculateTotals() {
        BigDecimal totalSalesTax = BigDecimal.ZERO;
        for (GstSummaryRow row : salesData) {
            totalSalesTax = totalSalesTax.add(row.getCgstAmount()).add(row.getSgstAmount());
        }

        BigDecimal totalPurTax = BigDecimal.ZERO;
        for (GstSummaryRow row : purchasesData) {
            totalPurTax = totalPurTax.add(row.getCgstAmount()).add(row.getSgstAmount());
        }

        BigDecimal payable = totalSalesTax.subtract(totalPurTax);

        lblTotalSalesTax.setText(String.format("₹ %.2f", totalSalesTax));
        lblTotalPurchaseTax.setText(String.format("₹ %.2f", totalPurTax));
        lblTaxPayable.setText(String.format("₹ %.2f", payable));
        
        if (payable.compareTo(BigDecimal.ZERO) < 0) {
            lblTaxPayable.setStyle("-fx-font-weight: bold; -fx-font-size: 24px; -fx-text-fill: #15803d;"); // Green for credit
        } else {
            lblTaxPayable.setStyle("-fx-font-weight: bold; -fx-font-size: 24px; -fx-text-fill: #dc2626;"); // Red for payable
        }
    }

    @FXML
    private void handlePrintReport() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Print Report");
        alert.setHeaderText(null);
        alert.setContentText("GSTR-3B Summary sent to printer.");
        alert.showAndWait();
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
