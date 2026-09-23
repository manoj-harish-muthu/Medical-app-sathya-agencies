package com.pharmacyerp.controller;

import com.pharmacyerp.dao.MedicineDAO;
import com.pharmacyerp.model.Medicine;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MedicinesController implements Initializable {

    // Views
    @FXML private VBox listView;
    @FXML private VBox formView;

    // List View Table
    @FXML private TableView<MedicineData> tblData;
    @FXML private TableColumn<MedicineData, Integer> colId;
    @FXML private TableColumn<MedicineData, String> colName;
    @FXML private TableColumn<MedicineData, String> colDetails;
    @FXML private TableColumn<MedicineData, String> colExtra1;
    @FXML private TableColumn<MedicineData, String> colExtra2;
    @FXML private Label lblTotalItems;
    
    private ObservableList<MedicineData> records = FXCollections.observableArrayList();

    // Status Section
    @FXML private ComboBox<String> cmbStatus;
    @FXML private ComboBox<String> cmbType;
    @FXML private ComboBox<String> cmbHide;
    
    // Product Info
    @FXML private TextField txtProduct;
    @FXML private TextField txtPacking;
    @FXML private TextField txtUnit1;
    @FXML private TextField txtUnit2;
    @FXML private ComboBox<String> cmbDecimal;
    @FXML private ComboBox<String> cmbColorType;
    @FXML private ComboBox<String> cmbItemType;
    
    // Categorization
    @FXML private ComboBox<String> cmbCompany;
    @FXML private ComboBox<String> cmbSalt;
    @FXML private TextField txtHsn;
    
    // Taxes
    @FXML private ComboBox<String> cmbLocal;
    @FXML private ComboBox<String> cmbCentral;
    @FXML private TextField txtSgst;
    @FXML private TextField txtCgst;
    @FXML private TextField txtIgst;
    
    // Pricing
    @FXML private TextField txtMrp;
    @FXML private TextField txtPRate;
    @FXML private TextField txtCost;
    @FXML private TextField txtRateA;
    @FXML private TextField txtRateB;
    @FXML private TextField txtRateC;
    
    // Converstion & Options
    @FXML private TextField txtConvStri;
    @FXML private TextField txtConvCas;
    @FXML private ComboBox<String> cmbNegative;
    
    @FXML private Label lblStatus;
    
    public static class MedicineData {
        private int id;
        private String name;
        private String details;
        private String extra1;
        private String extra2;

        public MedicineData(int id, String name, String details, String extra1, String extra2) {
            this.id = id;
            this.name = name;
            this.details = details;
            this.extra1 = extra1;
            this.extra2 = extra2;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getDetails() { return details; }
        public String getExtra1() { return extra1; }
        public String getExtra2() { return extra2; }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initComboBoxes();
        setupTable();
        showListView();
    }
    
    @FXML private TableColumn<MedicineData, Void> colAction;

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
        colExtra1.setCellValueFactory(new PropertyValueFactory<>("extra1"));
        colExtra2.setCellValueFactory(new PropertyValueFactory<>("extra2"));
        
        // Add Edit Action Column
        javafx.util.Callback<TableColumn<MedicineData, Void>, TableCell<MedicineData, Void>> cellFactory = new javafx.util.Callback<TableColumn<MedicineData, Void>, TableCell<MedicineData, Void>>() {
            @Override
            public TableCell<MedicineData, Void> call(final TableColumn<MedicineData, Void> param) {
                return new TableCell<MedicineData, Void>() {
                    private final Button btn = new Button("Edit");

                    {
                        btn.setOnAction((ActionEvent event) -> {
                            MedicineData data = getTableView().getItems().get(getIndex());
                            System.out.println("Editing medicine: " + data.getName());
                            // TODO: Add logic to fetch Medicine by ID and populate the formView here
                        });
                        btn.setStyle("-fx-background-color: #0d9488; -fx-text-fill: white; -fx-cursor: hand;");
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
            }
        };
        colAction.setCellFactory(cellFactory);

        tblData.setItems(records);
    }

    @FXML
    private void showListView() {
        formView.setVisible(false);
        formView.setManaged(false);
        listView.setVisible(true);
        listView.setManaged(true);
        loadData();
    }

    @FXML
    private void showFormView() {
        listView.setVisible(false);
        listView.setManaged(false);
        formView.setVisible(true);
        formView.setManaged(true);
        clearForm();
    }

    private void initComboBoxes() {
        cmbStatus.setItems(FXCollections.observableArrayList("CONTINUE", "DISCONTINUED"));
        cmbStatus.getSelectionModel().selectFirst();
        
        cmbType.setItems(FXCollections.observableArrayList("NORMAL", "NARCOTIC", "SCHEDULE H"));
        cmbType.getSelectionModel().selectFirst();
        
        cmbHide.setItems(FXCollections.observableArrayList("NO", "YES"));
        cmbHide.getSelectionModel().selectFirst();
        
        cmbDecimal.setItems(FXCollections.observableArrayList("No", "Yes"));
        cmbDecimal.getSelectionModel().selectFirst();
        
        cmbColorType.setItems(FXCollections.observableArrayList("NORMAL", "RED", "BLUE"));
        cmbColorType.getSelectionModel().selectFirst();
        
        cmbItemType.setItems(FXCollections.observableArrayList("1 NORMAL", "2 RAW MATERIAL"));
        cmbItemType.getSelectionModel().selectFirst();
        
        cmbLocal.setItems(FXCollections.observableArrayList("Taxable", "Exempt"));
        cmbLocal.getSelectionModel().selectFirst();
        
        cmbCentral.setItems(FXCollections.observableArrayList("Taxable", "Exempt"));
        cmbCentral.getSelectionModel().selectFirst();
        
        cmbNegative.setItems(FXCollections.observableArrayList("No", "Yes"));
        cmbNegative.getSelectionModel().selectFirst();
        
        // Dummy data for Company and Salt for now
        cmbCompany.setItems(FXCollections.observableArrayList("AMRUTANJAN HEALTH CARE", "CIPLA", "SUN PHARMA"));
        cmbSalt.setItems(FXCollections.observableArrayList("AYURVEDIC*", "PARACETAMOL 500MG"));
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String product = txtProduct.getText();
        if (product == null || product.trim().isEmpty()) {
            lblStatus.setText("Error: PRODUCT name is required!");
            lblStatus.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            return;
        }

        try {
            boolean success = MedicineDAO.saveErpMedicine(
                cmbStatus.getValue(), cmbType.getValue(), cmbHide.getValue(),
                product, txtPacking.getText(), txtUnit1.getText(), txtUnit2.getText(), cmbDecimal.getValue(),
                cmbColorType.getValue(), cmbItemType.getValue(), cmbCompany.getValue(), cmbSalt.getValue(), txtHsn.getText(),
                cmbLocal.getValue(), cmbCentral.getValue(),
                parseDouble(txtSgst.getText()), parseDouble(txtIgst.getText()), parseDouble(txtCgst.getText()),
                parseDouble(txtMrp.getText()), parseDouble(txtPRate.getText()), parseDouble(txtCost.getText()),
                parseDouble(txtRateA.getText()), parseDouble(txtRateB.getText()), parseDouble(txtRateC.getText()),
                parseInt(txtConvStri.getText()), parseInt(txtConvCas.getText()), cmbNegative.getValue()
            );

            if (success) {
                // Success! Switch back to list view and refresh
                showListView();
            } else {
                lblStatus.setText("Failed to save product.");
                lblStatus.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            }
        } catch (Exception e) {
            lblStatus.setText("Invalid number format in pricing or taxes.");
            lblStatus.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void clearForm() {
        txtProduct.clear();
        txtPacking.clear();
        txtUnit1.clear();
        txtUnit2.clear();
        txtHsn.clear();
        
        txtSgst.setText("0.00");
        txtCgst.setText("0.00");
        txtIgst.setText("0.00");
        
        txtMrp.setText("0.00");
        txtPRate.setText("0.00");
        txtCost.setText("0.00000");
        txtRateA.setText("0.00");
        txtRateB.setText("0.00");
        txtRateC.setText("0.00");
        
        txtConvStri.setText("0");
        txtConvCas.setText("0");
        
        cmbCompany.getSelectionModel().clearSelection();
        cmbSalt.getSelectionModel().clearSelection();
        
        initComboBoxes();
        lblStatus.setText("");
    }
    
    @FXML
    private void loadData() {
        records.clear();
        List<Medicine> meds = MedicineDAO.searchMedicines(""); // Empty query gets all
        for (Medicine m : meds) {
            records.add(new MedicineData(
                m.getMedicineId(), 
                m.getMedicineName(), 
                m.getSaltName() != null ? m.getSaltName() : "", 
                m.getCompanyName() != null ? m.getCompanyName() : "", 
                m.getHsnCode() != null ? m.getHsnCode() : "" 
            ));
        }
        lblTotalItems.setText(String.valueOf(records.size()));
    }

    private double parseDouble(String val) {
        if (val == null || val.trim().isEmpty()) return 0.0;
        return Double.parseDouble(val.trim());
    }

    private int parseInt(String val) {
        if (val == null || val.trim().isEmpty()) return 0;
        return Integer.parseInt(val.trim());
    }
}
