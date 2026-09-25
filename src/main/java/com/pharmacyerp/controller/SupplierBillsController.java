package com.pharmacyerp.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.pharmacyerp.model.Supplier;
import com.pharmacyerp.dao.SupplierDAO;

public class SupplierBillsController {

    @FXML private ComboBox<Supplier> cmbSupplier;
    @FXML private TableView<BillItem> tblBills;
    @FXML private TableColumn<BillItem, String> colBillNo;
    @FXML private TableColumn<BillItem, String> colDate;
    @FXML private TableColumn<BillItem, String> colTotal;
    @FXML private TableColumn<BillItem, String> colStatus;
    @FXML private TableColumn<BillItem, Void> colAction;

    private ObservableList<BillItem> billsList = FXCollections.observableArrayList();

    public static class BillItem {
        private int purchaseId;
        private SimpleStringProperty billNo;
        private SimpleStringProperty date;
        private SimpleStringProperty total;
        private SimpleStringProperty status;

        public BillItem(int purchaseId, String billNo, String date, String total, String status) {
            this.purchaseId = purchaseId;
            this.billNo = new SimpleStringProperty(billNo);
            this.date = new SimpleStringProperty(date);
            this.total = new SimpleStringProperty(total);
            this.status = new SimpleStringProperty(status);
        }

        public int getPurchaseId() { return purchaseId; }
        public String getBillNo() { return billNo.get(); }
        public String getDate() { return date.get(); }
        public String getTotal() { return total.get(); }
        public String getStatus() { return status.get(); }
    }

    @FXML
    public void initialize() {
        if (cmbSupplier != null) {
            cmbSupplier.getItems().setAll(SupplierDAO.getAllSuppliers());
            cmbSupplier.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    loadSupplierBills(newVal.getSupplierId());
                }
            });
        }

        if (colBillNo != null) colBillNo.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("billNo"));
        if (colDate != null) colDate.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("date"));
        if (colTotal != null) colTotal.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("total"));
        if (colStatus != null) colStatus.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("status"));

        setupActionColumn();

        if (tblBills != null) {
            tblBills.setItems(billsList);
        }
    }

    private void loadSupplierBills(int supplierId) {
        billsList.clear();
        String sql = "SELECT purchase_id, invoice_no, purchase_date, grand_total, status FROM purchases WHERE supplier_id = ? ORDER BY purchase_id DESC";
        try (java.sql.Connection conn = com.pharmacyerp.database.DatabaseManager.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, supplierId);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
                while (rs.next()) {
                    int pId = rs.getInt("purchase_id");
                    String inv = rs.getString("invoice_no");
                    java.sql.Timestamp ts = rs.getTimestamp("purchase_date");
                    String dt = ts != null ? sdf.format(ts) : "";
                    String tot = String.format("%.2f", rs.getDouble("grand_total"));
                    String st = rs.getString("status");
                    billsList.add(new BillItem(pId, inv, dt, tot, st));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupActionColumn() {
        if (colAction == null) return;

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnView = new Button("View Items");

            {
                btnView.setStyle("-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-cursor: hand;");
                btnView.setOnAction(event -> {
                    BillItem item = getTableView().getItems().get(getIndex());
                    showBillItemsDialog(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnView);
                }
            }
        });
    }

    private void showBillItemsDialog(BillItem bill) {
        Stage stage = new Stage();
        stage.setTitle("Bill Items - " + bill.getBillNo());

        TableView<String[]> tblItems = new TableView<>();
        
        TableColumn<String[], String> colMed = new TableColumn<>("Medicine");
        colMed.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[0]));
        colMed.setPrefWidth(200);

        TableColumn<String[], String> colBatch = new TableColumn<>("Batch");
        colBatch.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[1]));
        
        TableColumn<String[], String> colQty = new TableColumn<>("Qty");
        colQty.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[2]));
        
        TableColumn<String[], String> colRate = new TableColumn<>("Rate");
        colRate.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[3]));
        
        TableColumn<String[], String> colNet = new TableColumn<>("Net Amount");
        colNet.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[4]));

        tblItems.getColumns().addAll(colMed, colBatch, colQty, colRate, colNet);

        ObservableList<String[]> data = FXCollections.observableArrayList();
        String sql = "SELECT m.medicine_name, pi.batch_number, pi.quantity, pi.purchase_rate, pi.net_amount " +
                     "FROM purchase_items pi " +
                     "JOIN medicines m ON pi.medicine_id = m.medicine_id " +
                     "WHERE pi.purchase_id = ?";
        try (java.sql.Connection conn = com.pharmacyerp.database.DatabaseManager.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bill.getPurchaseId());
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    data.add(new String[]{
                        rs.getString("medicine_name"),
                        rs.getString("batch_number"),
                        String.valueOf(rs.getInt("quantity")),
                        String.format("%.2f", rs.getDouble("purchase_rate")),
                        String.format("%.2f", rs.getDouble("net_amount"))
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        tblItems.setItems(data);

        VBox vbox = new VBox(15, new Label("Items for Bill: " + bill.getBillNo()), tblItems);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: white;");
        
        Scene scene = new Scene(vbox, 600, 400);
        stage.setScene(scene);
        stage.show();
    }
}
