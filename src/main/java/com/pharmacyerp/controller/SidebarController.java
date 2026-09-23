package com.pharmacyerp.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.awt.Desktop;
import java.net.URI;

public class SidebarController {

    @FXML private VBox billingSubMenu;
    @FXML private VBox medicinesSubMenu;
    @FXML private VBox inventorySubMenu;
    @FXML private VBox purchasesSubMenu;
    @FXML private VBox customersSubMenu;
    @FXML private VBox doctorsSubMenu;
    @FXML private VBox suppliersSubMenu;
    @FXML private VBox onlineOrdersSubMenu;
    @FXML private VBox reportsSubMenu;
    @FXML private VBox accountsSubMenu;
    @FXML private VBox alertsSubMenu;
    @FXML private VBox staffSubMenu;
    @FXML private javafx.scene.control.ScrollPane scrollPane;

    private static class SidebarState {
        static boolean billingOpen = false;
        static boolean medicinesOpen = false;
        static boolean inventoryOpen = false;
        static boolean purchasesOpen = false;
        static boolean customersOpen = false;
        static boolean doctorsOpen = false;
        static boolean suppliersOpen = false;
        static boolean onlineOrdersOpen = false;
        static boolean reportsOpen = false;
        static boolean accountsOpen = false;
        static boolean alertsOpen = false;
        static boolean staffOpen = false;
        static double scrollVValue = 0.0;
    }

    @FXML
    public void initialize() {
        if (billingSubMenu != null) { billingSubMenu.setVisible(SidebarState.billingOpen); billingSubMenu.setManaged(SidebarState.billingOpen); }
        if (medicinesSubMenu != null) { medicinesSubMenu.setVisible(SidebarState.medicinesOpen); medicinesSubMenu.setManaged(SidebarState.medicinesOpen); }
        if (inventorySubMenu != null) { inventorySubMenu.setVisible(SidebarState.inventoryOpen); inventorySubMenu.setManaged(SidebarState.inventoryOpen); }
        if (purchasesSubMenu != null) { purchasesSubMenu.setVisible(SidebarState.purchasesOpen); purchasesSubMenu.setManaged(SidebarState.purchasesOpen); }
        if (customersSubMenu != null) { customersSubMenu.setVisible(SidebarState.customersOpen); customersSubMenu.setManaged(SidebarState.customersOpen); }
        if (doctorsSubMenu != null) { doctorsSubMenu.setVisible(SidebarState.doctorsOpen); doctorsSubMenu.setManaged(SidebarState.doctorsOpen); }
        if (suppliersSubMenu != null) { suppliersSubMenu.setVisible(SidebarState.suppliersOpen); suppliersSubMenu.setManaged(SidebarState.suppliersOpen); }
        if (onlineOrdersSubMenu != null) { onlineOrdersSubMenu.setVisible(SidebarState.onlineOrdersOpen); onlineOrdersSubMenu.setManaged(SidebarState.onlineOrdersOpen); }
        if (reportsSubMenu != null) { reportsSubMenu.setVisible(SidebarState.reportsOpen); reportsSubMenu.setManaged(SidebarState.reportsOpen); }
        if (accountsSubMenu != null) { accountsSubMenu.setVisible(SidebarState.accountsOpen); accountsSubMenu.setManaged(SidebarState.accountsOpen); }
        if (alertsSubMenu != null) { alertsSubMenu.setVisible(SidebarState.alertsOpen); alertsSubMenu.setManaged(SidebarState.alertsOpen); }
        if (staffSubMenu != null) { staffSubMenu.setVisible(SidebarState.staffOpen); staffSubMenu.setManaged(SidebarState.staffOpen); }
        
        if (scrollPane != null) {
            // Defer scroll value until layout is done
            javafx.application.Platform.runLater(() -> {
                scrollPane.setVvalue(SidebarState.scrollVValue);
            });
        }
    }

    @FXML
    private void toggleBilling(ActionEvent event) {
        boolean isVisible = billingSubMenu.isVisible();
        billingSubMenu.setVisible(!isVisible);
        billingSubMenu.setManaged(!isVisible);
        SidebarState.billingOpen = !isVisible;
    }

    @FXML
    private void toggleMedicines(ActionEvent event) {
        boolean isVisible = medicinesSubMenu.isVisible();
        medicinesSubMenu.setVisible(!isVisible);
        medicinesSubMenu.setManaged(!isVisible);
        SidebarState.medicinesOpen = !isVisible;
    }

    @FXML
    private void toggleInventory(ActionEvent event) {
        boolean isVisible = inventorySubMenu.isVisible();
        inventorySubMenu.setVisible(!isVisible);
        inventorySubMenu.setManaged(!isVisible);
        SidebarState.inventoryOpen = !isVisible;
    }

    @FXML
    private void togglePurchases(ActionEvent event) {
        boolean isVisible = purchasesSubMenu.isVisible();
        purchasesSubMenu.setVisible(!isVisible);
        purchasesSubMenu.setManaged(!isVisible);
        SidebarState.purchasesOpen = !isVisible;
    }

    @FXML
    private void toggleCustomers(ActionEvent event) {
        boolean isVisible = customersSubMenu.isVisible();
        customersSubMenu.setVisible(!isVisible);
        customersSubMenu.setManaged(!isVisible);
        SidebarState.customersOpen = !isVisible;
    }

    @FXML
    private void toggleDoctors(ActionEvent event) {
        boolean isVisible = doctorsSubMenu.isVisible();
        doctorsSubMenu.setVisible(!isVisible);
        doctorsSubMenu.setManaged(!isVisible);
        SidebarState.doctorsOpen = !isVisible;
    }

    @FXML
    private void toggleSuppliers(ActionEvent event) {
        boolean isVisible = suppliersSubMenu.isVisible();
        suppliersSubMenu.setVisible(!isVisible);
        suppliersSubMenu.setManaged(!isVisible);
        SidebarState.suppliersOpen = !isVisible;
    }

    @FXML
    private void toggleOnlineOrders(ActionEvent event) {
        boolean isVisible = onlineOrdersSubMenu.isVisible();
        onlineOrdersSubMenu.setVisible(!isVisible);
        onlineOrdersSubMenu.setManaged(!isVisible);
        SidebarState.onlineOrdersOpen = !isVisible;
    }

    @FXML
    private void toggleReports(ActionEvent event) {
        boolean isVisible = reportsSubMenu.isVisible();
        reportsSubMenu.setVisible(!isVisible);
        reportsSubMenu.setManaged(!isVisible);
        SidebarState.reportsOpen = !isVisible;
    }

    @FXML
    private void toggleAccounts(ActionEvent event) {
        boolean isVisible = accountsSubMenu.isVisible();
        accountsSubMenu.setVisible(!isVisible);
        accountsSubMenu.setManaged(!isVisible);
        SidebarState.accountsOpen = !isVisible;
    }

    @FXML
    private void toggleAlerts(ActionEvent event) {
        boolean isVisible = alertsSubMenu.isVisible();
        alertsSubMenu.setVisible(!isVisible);
        alertsSubMenu.setManaged(!isVisible);
        SidebarState.alertsOpen = !isVisible;
    }

    @FXML
    private void toggleStaff(ActionEvent event) {
        boolean isVisible = staffSubMenu.isVisible();
        staffSubMenu.setVisible(!isVisible);
        staffSubMenu.setManaged(!isVisible);
        SidebarState.staffOpen = !isVisible;
    }



    @FXML
    private void handleDashboard(ActionEvent event) {
        navigateTo(event, "/fxml/Dashboard.fxml");
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

    @FXML
    private void handleBillingCounterSale(ActionEvent event) {
        navigateTo(event, "/fxml/Pos.fxml");
    }

    @FXML
    private void handleBillingWholesale(ActionEvent event) {
        navigateTo(event, "/fxml/Pos.fxml");
    }

    @FXML
    private void handleBillingCreditSale(ActionEvent event) {
        navigateTo(event, "/fxml/Pos.fxml");
    }

    @FXML
    private void handleBillingCashSale(ActionEvent event) {
        navigateTo(event, "/fxml/Pos.fxml");
    }

    @FXML
    private void handleBillingQuotation(ActionEvent event) {
        navigateTo(event, "/fxml/BillingQuotation.fxml");
    }

    @FXML
    private void handleBillingSalesOrder(ActionEvent event) {
        navigateTo(event, "/fxml/Pos.fxml");
    }

    @FXML
    private void handleBillingDeliveryChallan(ActionEvent event) {
        navigateTo(event, "/fxml/Pos.fxml");
    }

    @FXML
    private void handleBillingSalesReturn(ActionEvent event) {
        navigateTo(event, "/fxml/Pos.fxml");
    }

    @FXML
    private void handleBillingExchange(ActionEvent event) {
        navigateTo(event, "/fxml/Pos.fxml");
    }

    @FXML
    private void handleBillingHoldBills(ActionEvent event) {
        navigateTo(event, "/fxml/BillingHoldBills.fxml");
    }

    @FXML
    private void handleBillingSuspendedBills(ActionEvent event) {
        navigateTo(event, "/fxml/BillingSuspendedBills.fxml");
    }

    @FXML
    private void handleMedDatabase(ActionEvent event) {
        navigateTo(event, "/fxml/MedDatabase.fxml");
    }

    @FXML
    private void handleMedGenericSalt(ActionEvent event) {
        navigateTo(event, "/fxml/MedGenericSalt.fxml");
    }

    @FXML
    private void handleMedBrand(ActionEvent event) {
        navigateTo(event, "/fxml/MedBrand.fxml");
    }

    @FXML
    private void handleMedSchedule(ActionEvent event) {
        navigateTo(event, "/fxml/MedSchedule.fxml");
    }

    @FXML
    private void handleMedSubstitute(ActionEvent event) {
        navigateTo(event, "/fxml/MedSubstitute.fxml");
    }

    @FXML
    private void handleMedComposition(ActionEvent event) {
        navigateTo(event, "/fxml/MedComposition.fxml");
    }

    @FXML
    private void handleMedRackShelf(ActionEvent event) {
        navigateTo(event, "/fxml/MedRackShelf.fxml");
    }

    @FXML
    private void handleMedBarcode(ActionEvent event) {
        navigateTo(event, "/fxml/MedBarcode.fxml");
    }

    @FXML
    private void handleMedBatch(ActionEvent event) {
        navigateTo(event, "/fxml/MedBatch.fxml");
    }

    @FXML
    private void handleMedMRP(ActionEvent event) {
        navigateTo(event, "/fxml/MedMRP.fxml");
    }

    @FXML
    private void handleMedPurchaseRate(ActionEvent event) {
        navigateTo(event, "/fxml/MedPurchaseRate.fxml");
    }

    @FXML
    private void handleMedSaleRate(ActionEvent event) {
        navigateTo(event, "/fxml/MedSaleRate.fxml");
    }

    @FXML
    private void handleMedInformation(ActionEvent event) {
        navigateTo(event, "/fxml/MedInformation.fxml");
    }

    @FXML
    private void handlePreviousBills(ActionEvent event) {
        navigateTo(event, "/fxml/PreviousBills.fxml");
    }

    @FXML
    private void handleDayEnd(ActionEvent event) {
        navigateTo(event, "/fxml/DayEndClosing.fxml");
    }

    @FXML
    private void handleStockAdjustment(ActionEvent event) {
        navigateTo(event, "/fxml/StockAdjustment.fxml");
    }

    @FXML
    private void handleManufacturer(ActionEvent event) {
        navigateTo(event, "/fxml/ManufacturerMaster.fxml");
    }

    @FXML
    private void handleCategory(ActionEvent event) {
        navigateTo(event, "/fxml/CategoryMaster.fxml");
    }

    @FXML
    private void handlePurchaseOrders(ActionEvent event) {
        navigateTo(event, "/fxml/PurchaseOrders.fxml");
    }

    @FXML
    private void handlePurchaseReturn(ActionEvent event) {
        navigateTo(event, "/fxml/PurchaseReturn.fxml");
    }

    @FXML
    private void handleSupplierBills(ActionEvent event) {
        navigateTo(event, "/fxml/SupplierBills.fxml");
    }

    @FXML
    private void handlePurchaseHistory(ActionEvent event) {
        navigateTo(event, "/fxml/PurchaseHistory.fxml");
    }



    @FXML private void handleInventoryItemMaster(ActionEvent event) { navigateTo(event, "/fxml/InventoryItemMaster.fxml"); }
    @FXML private void handleInventoryBatchDetails(ActionEvent event) { navigateTo(event, "/fxml/InventoryBatchDetails.fxml"); }
    @FXML private void handleInventoryShortageItems(ActionEvent event) { navigateTo(event, "/fxml/InventoryShortageItems.fxml"); }
    @FXML private void handleInventoryExpiredItems(ActionEvent event) { navigateTo(event, "/fxml/InventoryExpiredItems.fxml"); }
    @FXML private void handleInventoryStockJournal(ActionEvent event) { navigateTo(event, "/fxml/InventoryStockJournal.fxml"); }
    @FXML private void handleInventoryPhysicalStock(ActionEvent event) { navigateTo(event, "/fxml/InventoryPhysicalStock.fxml"); }
    @FXML private void handleInventoryCategoryMaster(ActionEvent event) { navigateTo(event, "/fxml/InventoryCategoryMaster.fxml"); }
    @FXML private void handleInventorySaltMaster(ActionEvent event) { navigateTo(event, "/fxml/InventorySaltMaster.fxml"); }

    @FXML
    private void handleCustomers(ActionEvent event) {
        navigateTo(event, "/fxml/Customers.fxml");
    }

    @FXML
    private void handlePatients(ActionEvent event) {
        navigateTo(event, "/fxml/Patients.fxml");
    }

    @FXML
    private void handlePrescriptionHistory(ActionEvent event) {
        navigateTo(event, "/fxml/PrescriptionHistory.fxml");
    }

    @FXML
    private void handleCreditOutstanding(ActionEvent event) {
        navigateTo(event, "/fxml/OutstandingBills.fxml");
    }

    @FXML
    private void handleCustomerLedger(ActionEvent event) {
        navigateTo(event, "/fxml/CustomerLedger.fxml");
    }

    @FXML
    private void handleGstReports(ActionEvent event) {
        navigateTo(event, "/fxml/GstReport.fxml");
    }

    @FXML
    private void handleDoctorMaster(ActionEvent event) {
        navigateTo(event, "/fxml/DoctorMaster.fxml");
    }

    @FXML
    private void handleDoctorPrescription(ActionEvent event) {
        navigateTo(event, "/fxml/DoctorPrescription.fxml");
    }

    @FXML
    private void handleDoctorWiseSales(ActionEvent event) {
        navigateTo(event, "/fxml/DoctorWiseSales.fxml");
    }

    @FXML
    private void handleSupplierMaster(ActionEvent event) {
        navigateTo(event, "/fxml/SupplierMaster.fxml");
    }

    @FXML
    private void handleSupplierLedger(ActionEvent event) {
        navigateTo(event, "/fxml/SupplierLedger.fxml");
    }

    @FXML
    private void handleSupplierOutstanding(ActionEvent event) {
        navigateTo(event, "/fxml/SupplierOutstanding.fxml");
    }

    @FXML
    private void handleSupplierPayment(ActionEvent event) {
        navigateTo(event, "/fxml/SupplierPayment.fxml");
    }

    @FXML
    private void handleAgentOrders(ActionEvent event) {
        navigateTo(event, "/fxml/AgentOrders.fxml");
    }

    @FXML
    private void handlePendingOrders(ActionEvent event) {
        navigateTo(event, "/fxml/PendingOrders.fxml");
    }

    @FXML
    private void handleCompletedOrders(ActionEvent event) {
        navigateTo(event, "/fxml/CompletedOrders.fxml");
    }

    @FXML
    private void handleFailedOrders(ActionEvent event) {
        navigateTo(event, "/fxml/FailedOrders.fxml");
    }

    @FXML
    private void handleSyncStatus(ActionEvent event) {
        navigateTo(event, "/fxml/SyncStatus.fxml");
    }

    @FXML
    private void handleReportDailySales(ActionEvent event) {
        navigateTo(event, "/fxml/ReportDailySales.fxml");
    }

    @FXML
    private void handleReportMonthlySales(ActionEvent event) {
        navigateTo(event, "/fxml/ReportMonthlySales.fxml");
    }

    @FXML
    private void handleReportPurchase(ActionEvent event) {
        navigateTo(event, "/fxml/ReportPurchase.fxml");
    }

    @FXML
    private void handleReportStock(ActionEvent event) {
        navigateTo(event, "/fxml/ReportStock.fxml");
    }

    @FXML
    private void handleReportExpiry(ActionEvent event) {
        navigateTo(event, "/fxml/ReportExpiry.fxml");
    }

    @FXML
    private void handleReportProfit(ActionEvent event) {
        navigateTo(event, "/fxml/ReportProfit.fxml");
    }

    @FXML
    private void handleReportGST(ActionEvent event) {
        navigateTo(event, "/fxml/GstReport.fxml");
    }

    @FXML
    private void handleReportCustomer(ActionEvent event) {
        navigateTo(event, "/fxml/ReportCustomer.fxml");
    }

    @FXML
    private void handleReportSupplier(ActionEvent event) {
        navigateTo(event, "/fxml/ReportSupplier.fxml");
    }

    @FXML
    private void handleReportCashier(ActionEvent event) {
        navigateTo(event, "/fxml/ReportCashier.fxml");
    }

    @FXML
    private void handleAccountCash(ActionEvent event) {
        navigateTo(event, "/fxml/AccountCash.fxml");
    }

    @FXML
    private void handleAccountUPI(ActionEvent event) {
        navigateTo(event, "/fxml/AccountUPI.fxml");
    }

    @FXML
    private void handleAccountCard(ActionEvent event) {
        navigateTo(event, "/fxml/AccountCard.fxml");
    }

    @FXML
    private void handleAccountExpenses(ActionEvent event) {
        navigateTo(event, "/fxml/AccountExpenses.fxml");
    }

    @FXML
    private void handleAccountReceivables(ActionEvent event) {
        navigateTo(event, "/fxml/AccountReceivables.fxml");
    }

    @FXML
    private void handleAccountPayables(ActionEvent event) {
        navigateTo(event, "/fxml/AccountPayables.fxml");
    }

    @FXML
    private void handleAccountDailyClosing(ActionEvent event) {
        navigateTo(event, "/fxml/AccountDailyClosing.fxml");
    }

    @FXML
    private void handleAlertLowStock(ActionEvent event) {
        navigateTo(event, "/fxml/AlertLowStock.fxml");
    }

    @FXML
    private void handleAlertNearExpiry(ActionEvent event) {
        navigateTo(event, "/fxml/AlertNearExpiry.fxml");
    }

    @FXML
    private void handleAlertExpired(ActionEvent event) {
        navigateTo(event, "/fxml/AlertExpired.fxml");
    }

    @FXML
    private void handleAlertOutstanding(ActionEvent event) {
        navigateTo(event, "/fxml/AlertOutstanding.fxml");
    }

    @FXML
    private void handleAlertPendingOrders(ActionEvent event) {
        navigateTo(event, "/fxml/AlertPendingOrders.fxml");
    }

    @FXML
    public void handleOpenGuttBrains(ActionEvent event) {
        openUrl("https://www.guttbrains.com");
    }

    @FXML
    public void handleSettings(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("About Software & System Settings");
        alert.setHeaderText(null);
        alert.setGraphic(null);

        VBox content = new VBox(12);
        content.setStyle("-fx-padding: 16 22 10 22; -fx-background-color: #FFFFFF;");
        content.setPrefWidth(420);
        
        Label titleLabel = new Label("Sathya Agencies");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: #0D9488;");

        Label subtitleLabel = new Label("Premium Pharmacy ERP • v1.0.0");
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #4B5563;");

        javafx.scene.control.Separator sep1 = new javafx.scene.control.Separator();

        Label descLabel = new Label("High-performance pharmaceutical billing, Marg ERP data sync & WhatsApp AI-integrated order fulfillment.");
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151; -fx-font-weight: 600; -fx-line-spacing: 3px;");

        javafx.scene.control.Separator sep2 = new javafx.scene.control.Separator();

        HBox creditBox = new HBox(6);
        creditBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label engLabel = new Label("Engineered by");
        engLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #111827;");
        Hyperlink guttLink = new Hyperlink("GuttBrains");
        guttLink.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #0D9488; -fx-padding: 0; -fx-border-width: 0; -fx-underline: false; -fx-cursor: hand;");
        guttLink.setOnAction(e -> openUrl("https://www.guttbrains.com"));
        creditBox.getChildren().addAll(engLabel, guttLink);

        content.getChildren().addAll(titleLabel, subtitleLabel, sep1, descLabel, sep2, creditBox);

        DialogPane pane = alert.getDialogPane();
        pane.setContent(content);
        pane.getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        pane.setStyle("-fx-background-color: #FFFFFF;");
        pane.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        alert.showAndWait();
    }

    public static void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            }
        } catch (Exception e) {
            System.err.println("Could not open URL: " + e.getMessage());
        }
    }

    @FXML
    private void handleComingSoon(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Feature in Development");
        alert.setHeaderText(null);
        alert.setContentText("This feature is currently under development and will be available soon.");
        alert.showAndWait();
    }

    public void navigateTo(ActionEvent event, String fxmlPath) {
        if (scrollPane != null) {
            SidebarState.scrollVValue = scrollPane.getVvalue();
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Throwable e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Unable to open screen: " + fxmlPath);
            alert.setContentText(e.getMessage() != null ? e.getMessage() : e.toString());
            alert.showAndWait();
        }
    }
}
