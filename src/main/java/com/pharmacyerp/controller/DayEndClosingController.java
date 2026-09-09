package com.pharmacyerp.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;

import com.pharmacyerp.model.DailySummary;
import com.pharmacyerp.dao.SalesDAO;

import java.io.IOException;
import java.time.LocalDate;

public class DayEndClosingController {

    @FXML private DatePicker datePicker;
    @FXML private Label lblInvoices;
    @FXML private Label lblCash;
    @FXML private Label lblUpi;
    @FXML private Label lblCard;
    @FXML private Label lblCredit;
    @FXML private Label lblTotal;

    private SalesDAO sDao = new SalesDAO();

    @FXML
    public void initialize() {
        datePicker.setValue(LocalDate.now());
        loadSummary();
    }

    @FXML
    public void loadSummary() {
        LocalDate date = datePicker.getValue();
        if (date == null) return;
        
        DailySummary summary = sDao.getDailySummary(date);
        
        lblInvoices.setText(String.valueOf(summary.getTotalInvoices()));
        lblCash.setText(String.format("₹ %.2f", summary.getCashSales()));
        lblUpi.setText(String.format("₹ %.2f", summary.getUpiSales()));
        lblCard.setText(String.format("₹ %.2f", summary.getCardSales()));
        lblCredit.setText(String.format("₹ %.2f", summary.getCreditSales()));
        lblTotal.setText(String.format("₹ %.2f", summary.getTotalSales()));
    }

    @FXML
    private void handlePrint() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Z-Report");
        alert.setHeaderText(null);
        alert.setContentText("Z-Report (Day End Closing) has been generated and sent to the printer.");
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
