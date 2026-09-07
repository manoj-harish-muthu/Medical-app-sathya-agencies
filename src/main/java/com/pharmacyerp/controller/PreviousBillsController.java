package com.pharmacyerp.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import com.pharmacyerp.model.Sale;
import com.pharmacyerp.dao.SalesDAO;

import java.io.IOException;

public class PreviousBillsController {

    @FXML private TextField searchField;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    
    @FXML private TableView<Sale> billsTable;
    @FXML private TableColumn<Sale, String> colInvoice;
    @FXML private TableColumn<Sale, java.sql.Timestamp> colDate;
    @FXML private TableColumn<Sale, String> colCustomer;
    @FXML private TableColumn<Sale, String> colPhone;
    @FXML private TableColumn<Sale, java.math.BigDecimal> colAmount;
    @FXML private TableColumn<Sale, String> colStatus;
    @FXML private TableColumn<Sale, Void> colAction;

    private SalesDAO salesDAO = new SalesDAO();
    private ObservableList<Sale> salesList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Setup table cell factories
        colAction.setCellFactory(param -> new TableCell<Sale, Void>() {
            private final Button printBtn = new Button("Re-Print");
            {
                printBtn.getStyleClass().add("button-primary");
                printBtn.setOnAction(event -> {
                    Sale sale = getTableView().getItems().get(getIndex());
                    java.util.List<com.pharmacyerp.model.CartItem> items = salesDAO.getSaleItemsBySaleId(sale.getSaleId());
                    String pdfPath = "invoice_" + sale.getInvoiceNo() + ".pdf";
                    com.pharmacyerp.util.InvoicePrinter.printInvoice(sale.getInvoiceNo(), sale.getCustomerName() != null ? sale.getCustomerName() : "", sale.getDoctorName() != null ? sale.getDoctorName() : "", items, pdfPath);
                    try {
                        java.io.File pdfFile = new java.io.File(pdfPath);
                        if (pdfFile.exists() && java.awt.Desktop.isDesktopSupported()) {
                            java.awt.Desktop.getDesktop().open(pdfFile);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(printBtn);
                }
            }
        });

        loadData();
    }

    private void loadData() {
        salesList.clear();
        salesList.addAll(salesDAO.getAllSales());
        billsTable.setItems(salesList);
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        // Simple search filtering
        String query = searchField.getText().toLowerCase();
        ObservableList<Sale> filteredList = FXCollections.observableArrayList();
        for (Sale sale : salesDAO.getAllSales()) {
            if (sale.getInvoiceNo().toLowerCase().contains(query) ||
                (sale.getCustomerName() != null && sale.getCustomerName().toLowerCase().contains(query)) ||
                (sale.getCustomerPhone() != null && sale.getCustomerPhone().contains(query))) {
                filteredList.add(sale);
            }
        }
        billsTable.setItems(filteredList);
    }

    @FXML
    private void handleClear(ActionEvent event) {
        searchField.clear();
        fromDate.setValue(null);
        toDate.setValue(null);
        loadData();
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
