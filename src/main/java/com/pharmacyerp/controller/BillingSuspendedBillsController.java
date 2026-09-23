package com.pharmacyerp.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.cell.PropertyValueFactory;
import com.pharmacyerp.model.Sale;
import com.pharmacyerp.model.CartItem;
import com.pharmacyerp.dao.SalesDAO;

public class BillingSuspendedBillsController {

    @FXML private TableView<Sale> tblHistory;
    @FXML private TableColumn<Sale, java.sql.Timestamp> colDate;
    @FXML private TableColumn<Sale, String> colCustomer;
    @FXML private TableColumn<Sale, String> colInvoice;
    @FXML private TableColumn<Sale, java.math.BigDecimal> colAmount;
    @FXML private TableColumn<Sale, String> colStatus;
    @FXML private TableColumn<Sale, Void> colAction;
    
    @FXML private Label lblTotalItems;
    @FXML private Label lblGrandTotal;

    private SalesDAO salesDAO = new SalesDAO();
    private ObservableList<Sale> suspendedBills = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (colDate != null) colDate.setCellValueFactory(new PropertyValueFactory<>("saleDate"));
        if (colCustomer != null) colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        if (colInvoice != null) colInvoice.setCellValueFactory(new PropertyValueFactory<>("invoiceNo"));
        if (colAmount != null) colAmount.setCellValueFactory(new PropertyValueFactory<>("grandTotal"));
        if (colStatus != null) colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        if (colAction != null) {
            colAction.setCellFactory(param -> new TableCell<Sale, Void>() {
                private final Button resumeBtn = new Button("Resume");
                {
                    resumeBtn.getStyleClass().add("button-primary");
                    resumeBtn.setOnAction(event -> {
                        Sale sale = getTableView().getItems().get(getIndex());
                        System.out.println("Resuming suspended bill: " + sale.getInvoiceNo());
                        try {
                            java.util.List<CartItem> items = salesDAO.getSaleItemsBySaleId(sale.getSaleId());
                            
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Pos.fxml"));
                            Parent root = loader.load();
                            PosController posController = loader.getController();
                            
                            posController.loadSale(sale, items);
                            
                            // Delete the suspended bill so it isn't duplicated
                            salesDAO.deleteSale(sale.getSaleId());
                            
                            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                            stage.getScene().setRoot(root);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) setGraphic(null);
                    else setGraphic(resumeBtn);
                }
            });
        }
        
        loadData();
    }

    private void loadData() {
        suspendedBills.clear();
        suspendedBills.addAll(salesDAO.getSalesByStatus("SUSPENDED"));
        if (tblHistory != null) {
            tblHistory.setItems(suspendedBills);
        }
        updateTotals();
    }
    
    private void updateTotals() {
        if (lblTotalItems != null) lblTotalItems.setText(String.valueOf(suspendedBills.size()));
        
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (Sale s : suspendedBills) {
            if (s.getGrandTotal() != null) total = total.add(s.getGrandTotal());
        }
        if (lblGrandTotal != null) lblGrandTotal.setText(String.format("%.2f", total));
    }
}
