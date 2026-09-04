package com.pharmacyerp.controller;

import com.pharmacyerp.dao.SalesDAO;
import com.pharmacyerp.model.Sale;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableCell;
import javafx.scene.control.Button;
import javafx.scene.control.cell.PropertyValueFactory;
import com.pharmacyerp.util.InvoicePrinter;
import com.pharmacyerp.model.CartItem;

public class ReportDailySalesController {

    @FXML private TableView<Sale> salesTable;
    @FXML private TableColumn<Sale, String> colInvoiceNo;
    @FXML private TableColumn<Sale, String> colTime;
    @FXML private TableColumn<Sale, String> colCustomer;
    @FXML private TableColumn<Sale, String> colTotal;
    @FXML private TableColumn<Sale, String> colPaymentMode;
    @FXML private TableColumn<Sale, Void> colAction;

    private SalesDAO salesDAO = new SalesDAO();

    @FXML
    public void initialize() {
        colInvoiceNo.setCellValueFactory(new PropertyValueFactory<>("invoiceNo"));
        
        colTime.setCellValueFactory(cellData -> {
            java.sql.Timestamp ts = cellData.getValue().getSaleDate();
            if (ts != null) {
                return new SimpleStringProperty(new java.text.SimpleDateFormat("HH:mm:ss").format(ts));
            }
            return new SimpleStringProperty("");
        });
        
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("grandTotal"));
        colPaymentMode.setCellValueFactory(new PropertyValueFactory<>("paymentMode"));

        colAction.setCellFactory(param -> new TableCell<Sale, Void>() {
            private final Button viewBtn = new Button("View Bill");
            {
                viewBtn.getStyleClass().add("button-primary");
                viewBtn.setOnAction(event -> {
                    Sale sale = getTableView().getItems().get(getIndex());
                    java.util.List<CartItem> items = salesDAO.getSaleItemsBySaleId(sale.getSaleId());
                    String pdfPath = "invoice_" + sale.getInvoiceNo() + ".pdf";
                    InvoicePrinter.printInvoice(sale.getInvoiceNo(), sale.getCustomerName() != null ? sale.getCustomerName() : "", sale.getDoctorName() != null ? sale.getDoctorName() : "", items, pdfPath);
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
                    setGraphic(viewBtn);
                }
            }
        });

        salesTable.setItems(FXCollections.observableArrayList(salesDAO.getDailySales(java.time.LocalDate.now())));
    }
}
