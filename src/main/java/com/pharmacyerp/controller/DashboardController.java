package com.pharmacyerp.controller;

import com.pharmacyerp.security.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.Node;
import com.pharmacyerp.dao.DashboardDAO;

import java.io.IOException;

public class DashboardController {

    @FXML private Label userNameLabel;
    @FXML private Label salesLabel;
    @FXML private Label billsLabel;
    @FXML private Label lowStockLabel;
    @FXML private Label expiringLabel;
    @FXML private javafx.scene.layout.VBox recentBillsContainer;
    @FXML private javafx.scene.chart.LineChart<String, Number> salesTrendChart;
    @FXML private javafx.scene.control.Button alertsButton;
    
    private java.util.List<String> currentAlerts = new java.util.ArrayList<>();

    private DashboardDAO dashboardDAO = new DashboardDAO();
    private com.pharmacyerp.dao.SalesDAO salesDAO = new com.pharmacyerp.dao.SalesDAO();

    @FXML
    public void initialize() {
        if (AuthService.getCurrentUser() != null) {
            userNameLabel.setText(AuthService.getCurrentUser().getFullName());
        }
        
        loadDashboardMetrics();
        loadRecentBills();
        loadSalesTrend();
        loadAlerts();
    }
    
    private void loadDashboardMetrics() {
        salesLabel.setText(String.format("₹ %,.2f", dashboardDAO.getTodaysSalesTotal()));
        billsLabel.setText(String.valueOf(dashboardDAO.getTodaysBillsCount()));
        lowStockLabel.setText(String.valueOf(dashboardDAO.getLowStockCount()));
        expiringLabel.setText(String.valueOf(dashboardDAO.getExpiringSoonCount()));
    }
    
    private void loadAlerts() {
        if (alertsButton == null) return;
        currentAlerts = dashboardDAO.getAlertMessages();
        int count = currentAlerts.size();
        if (count > 0) {
            alertsButton.setText("🔔 Alerts (" + count + ")");
            alertsButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #DC2626; -fx-font-weight: bold;");
        } else {
            alertsButton.setText("🔔 Alerts");
            alertsButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #111827; -fx-font-weight: bold;");
        }
    }

    private void loadSalesTrend() {
        if (salesTrendChart == null) return;
        salesTrendChart.getData().clear();
        
        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        series.setName("Sales");
        
        java.util.Map<String, java.math.BigDecimal> trend = dashboardDAO.getSalesTrend(7);
        for (java.util.Map.Entry<String, java.math.BigDecimal> entry : trend.entrySet()) {
            series.getData().add(new javafx.scene.chart.XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        
        salesTrendChart.getData().add(series);
    }

    private void loadRecentBills() {
        if (recentBillsContainer == null) return;
        recentBillsContainer.getChildren().clear();
        
        java.util.List<com.pharmacyerp.model.Sale> recentSales = salesDAO.getAllSales();
        int count = 0;
        
        for (com.pharmacyerp.model.Sale sale : recentSales) {
            if (count >= 5) break; // show only top 5
            
            javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(10);
            hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(15, javafx.scene.paint.Color.web("#E2E8F0"));
            
            javafx.scene.layout.VBox infoBox = new javafx.scene.layout.VBox();
            Label invLabel = new Label(sale.getInvoiceNo());
            invLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            
            String custName = sale.getCustomerName();
            if (custName == null || custName.trim().isEmpty()) custName = "Walk-in Customer";
            Label custLabel = new Label(custName);
            custLabel.getStyleClass().add("secondary-text");
            infoBox.getChildren().addAll(invLabel, custLabel);
            
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            
            Label amtLabel = new Label(String.format("₹ %,.2f", sale.getGrandTotal()));
            amtLabel.setStyle("-fx-font-weight: bold;");
            
            hbox.getChildren().addAll(circle, infoBox, spacer, amtLabel);
            recentBillsContainer.getChildren().add(hbox);
            
            count++;
        }
        
        if (count == 0) {
            Label noBills = new Label("No recent bills found");
            noBills.getStyleClass().add("secondary-text");
            recentBillsContainer.getChildren().add(noBills);
        }
    }
    
    @FXML
    private void handleShowAlerts(ActionEvent event) {
        if (currentAlerts.isEmpty()) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Alerts");
            alert.setHeaderText(null);
            alert.setContentText("No new alerts at this time.");
            alert.showAndWait();
            return;
        }

        javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
        contextMenu.setStyle("-fx-font-size: 14px; -fx-padding: 5px;");
        for (String alertMsg : currentAlerts) {
            javafx.scene.control.MenuItem item = new javafx.scene.control.MenuItem(alertMsg);
            if (alertMsg.startsWith("Expiring")) {
                item.setStyle("-fx-text-fill: #F59E0B;");
            } else if (alertMsg.startsWith("Low Stock")) {
                item.setStyle("-fx-text-fill: #DC2626;");
            }
            contextMenu.getItems().add(item);
        }
        
        javafx.scene.Node source = (javafx.scene.Node) event.getSource();
        contextMenu.show(source, javafx.geometry.Side.BOTTOM, 0, 5);
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        AuthService.logout();
        navigateTo(event, "/fxml/Login.fxml");
    }

    @FXML
    private void handlePos(ActionEvent event) {
        navigateTo(event, "/fxml/Pos.fxml");
    }

    @FXML
    private void handleMedicines(ActionEvent event) {
        navigateTo(event, "/fxml/Medicines.fxml");
    }
    
    @FXML
    private void handleInventory(ActionEvent event) {
        navigateTo(event, "/fxml/Inventory.fxml");
    }

    @FXML
    private void handlePurchases(ActionEvent event) {
        navigateTo(event, "/fxml/Purchases.fxml");
    }

    private void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1366, 768));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
