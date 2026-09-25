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
    @FXML private ComboBox<com.pharmacyerp.model.Manufacturer> cmbCompany;
    @FXML private ComboBox<com.pharmacyerp.model.Category> cmbCategory;
    @FXML private ComboBox<com.pharmacyerp.model.GenericSalt> cmbSalt;
    @FXML private ComboBox<com.pharmacyerp.model.Brand> cmbBrand;
    @FXML private ComboBox<com.pharmacyerp.model.Schedule> cmbSchedule;
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
    
    private int editingMedicineId = -1;
    private com.pharmacyerp.repository.MedicineRepository repository = new com.pharmacyerp.repository.MedicineRepository();
    
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
        
        // Add Edit & Delete Action Column
        javafx.util.Callback<TableColumn<MedicineData, Void>, TableCell<MedicineData, Void>> cellFactory = new javafx.util.Callback<TableColumn<MedicineData, Void>, TableCell<MedicineData, Void>>() {
            @Override
            public TableCell<MedicineData, Void> call(final TableColumn<MedicineData, Void> param) {
                return new TableCell<MedicineData, Void>() {
                    private final Button editBtn = new Button("Edit");
                    private final Button delBtn = new Button("Del");
                    private final javafx.scene.layout.HBox pane = new javafx.scene.layout.HBox(5, editBtn, delBtn);

                    {
                        editBtn.setStyle("-fx-background-color: #0d9488; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 11px;");
                        delBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 11px;");

                        editBtn.setOnAction((ActionEvent event) -> {
                            MedicineData data = getTableView().getItems().get(getIndex());
                            Medicine m = com.pharmacyerp.dao.MedicineDAO.getMedicineById(data.getId());
                            if (m != null) {
                                showFormView();
                                editingMedicineId = m.getMedicineId();
                                populateForm(m);
                            }
                        });

                        delBtn.setOnAction((ActionEvent event) -> {
                            MedicineData data = getTableView().getItems().get(getIndex());
                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                            alert.setTitle("Confirm Deletion");
                            alert.setHeaderText("Delete Medicine: " + data.getName());
                            alert.setContentText("Are you sure you want to delete this medicine?");
                            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                                if (MedicineDAO.deleteMedicine(data.getId())) {
                                    loadData();
                                }
                            }
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(pane);
                        }
                    }
                };
            }
        };
        colAction.setCellFactory(cellFactory);

        tblData.setItems(records);
    }
    
    private void populateForm(Medicine m) {
        txtProduct.setText(m.getMedicineName());
        txtPacking.setText(m.getPacking());
        txtUnit1.setText(m.getUnit1st());
        txtUnit2.setText(m.getUnit2nd());
        txtHsn.setText(m.getHsnCode());
        
        txtSgst.setText(String.format("%.2f", m.getSgst()));
        txtCgst.setText(String.format("%.2f", m.getCgst()));
        txtIgst.setText(String.format("%.2f", m.getIgst()));
        
        txtMrp.setText(String.format("%.2f", m.getMrp()));
        txtPRate.setText(String.format("%.2f", m.getPurchaseRate()));
        txtCost.setText(String.format("%.5f", m.getCostPerPcs()));
        txtRateA.setText(String.format("%.2f", m.getRateA()));
        txtRateB.setText(String.format("%.2f", m.getRateB()));
        txtRateC.setText(String.format("%.2f", m.getRateC()));
        
        txtConvStri.setText(String.valueOf(m.getConvStr()));
        txtConvCas.setText(String.valueOf(m.getConvCas()));
        
        if (m.getCompanyId() > 0) {
            for (com.pharmacyerp.model.Manufacturer c : cmbCompany.getItems()) {
                if (c.getCompanyId() == m.getCompanyId()) { cmbCompany.setValue(c); break; }
            }
        }
        if (m.getCategoryId() > 0) {
            for (com.pharmacyerp.model.Category c : cmbCategory.getItems()) {
                if (c.getCategoryId() == m.getCategoryId()) { cmbCategory.setValue(c); break; }
            }
        }
        if (m.getBrandId() > 0) {
            for (com.pharmacyerp.model.Brand b : cmbBrand.getItems()) {
                if (b.getBrandId() == m.getBrandId()) { cmbBrand.setValue(b); break; }
            }
        }
        if (m.getScheduleId() > 0) {
            for (com.pharmacyerp.model.Schedule s : cmbSchedule.getItems()) {
                if (s.getScheduleId() == m.getScheduleId()) { cmbSchedule.setValue(s); break; }
            }
        }
        if (m.getSaltName() != null && !m.getSaltName().isEmpty()) {
            for (com.pharmacyerp.model.GenericSalt s : cmbSalt.getItems()) {
                if (s.getSaltName().equalsIgnoreCase(m.getSaltName())) { cmbSalt.setValue(s); break; }
            }
        }
        
        if (m.getDecimalAllowed() != null) cmbDecimal.setValue(m.getDecimalAllowed());
        if (m.getColorType() != null) cmbColorType.setValue(m.getColorType());
        if (m.getItemType() != null) cmbItemType.setValue(m.getItemType());
        if (m.getLocalTaxType() != null) cmbLocal.setValue(m.getLocalTaxType());
        if (m.getCentralTaxType() != null) cmbCentral.setValue(m.getCentralTaxType());
        if (m.getNegativeAllowed() != null) cmbNegative.setValue(m.getNegativeAllowed());
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
        
        // Load Masters from DB
        com.pharmacyerp.dao.ManufacturerDAO mDao = new com.pharmacyerp.dao.ManufacturerDAO();
        cmbCompany.setItems(javafx.collections.FXCollections.observableArrayList(mDao.getAllManufacturers()));
        cmbCompany.setConverter(new javafx.util.StringConverter<com.pharmacyerp.model.Manufacturer>() {
            @Override public String toString(com.pharmacyerp.model.Manufacturer m) { return m == null ? "" : m.getCompanyName(); }
            @Override public com.pharmacyerp.model.Manufacturer fromString(String s) { return null; }
        });

        com.pharmacyerp.dao.CategoryDAO cDao = new com.pharmacyerp.dao.CategoryDAO();
        cmbCategory.setItems(javafx.collections.FXCollections.observableArrayList(cDao.getAllCategories()));
        cmbCategory.setConverter(new javafx.util.StringConverter<com.pharmacyerp.model.Category>() {
            @Override public String toString(com.pharmacyerp.model.Category c) { return c == null ? "" : c.getCategoryName(); }
            @Override public com.pharmacyerp.model.Category fromString(String s) { return null; }
        });

        com.pharmacyerp.dao.GenericSaltDAO sDao = new com.pharmacyerp.dao.GenericSaltDAO();
        cmbSalt.setItems(javafx.collections.FXCollections.observableArrayList(sDao.getAllSalts()));
        cmbSalt.setConverter(new javafx.util.StringConverter<com.pharmacyerp.model.GenericSalt>() {
            @Override public String toString(com.pharmacyerp.model.GenericSalt s) { return s == null ? "" : s.getSaltName(); }
            @Override public com.pharmacyerp.model.GenericSalt fromString(String s) { return null; }
        });

        com.pharmacyerp.dao.BrandDAO bDao = new com.pharmacyerp.dao.BrandDAO();
        cmbBrand.setItems(javafx.collections.FXCollections.observableArrayList(bDao.getAllBrands()));
        cmbBrand.setConverter(new javafx.util.StringConverter<com.pharmacyerp.model.Brand>() {
            @Override public String toString(com.pharmacyerp.model.Brand b) { return b == null ? "" : b.getBrandName(); }
            @Override public com.pharmacyerp.model.Brand fromString(String s) { return null; }
        });

        com.pharmacyerp.dao.ScheduleDAO schDao = new com.pharmacyerp.dao.ScheduleDAO();
        cmbSchedule.setItems(javafx.collections.FXCollections.observableArrayList(schDao.getAllSchedules()));
        cmbSchedule.setConverter(new javafx.util.StringConverter<com.pharmacyerp.model.Schedule>() {
            @Override public String toString(com.pharmacyerp.model.Schedule s) { return s == null ? "" : s.getScheduleName(); }
            @Override public com.pharmacyerp.model.Schedule fromString(String s) { return null; }
        });
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
            com.pharmacyerp.model.Medicine med = new com.pharmacyerp.model.Medicine();
            med.setMedicineName(product.trim().toUpperCase());
            
            if (cmbCompany.getValue() != null) med.setCompanyId(cmbCompany.getValue().getCompanyId());
            if (cmbCategory.getValue() != null) med.setCategoryId(cmbCategory.getValue().getCategoryId());
            if (cmbBrand.getValue() != null) med.setBrandId(cmbBrand.getValue().getBrandId());
            if (cmbSchedule.getValue() != null) med.setScheduleId(cmbSchedule.getValue().getScheduleId());
            if (cmbSalt.getValue() != null) med.setSaltName(cmbSalt.getValue().getSaltName());

            med.setPacking(txtPacking.getText().trim().toUpperCase());
            med.setUnit1st(txtUnit1.getText().trim().toUpperCase());
            med.setUnit2nd(txtUnit2.getText().trim().toUpperCase());
            med.setDecimalAllowed(cmbDecimal.getValue());
            med.setColorType(cmbColorType.getValue());
            med.setItemType(cmbItemType.getValue());
            med.setHsnCode(txtHsn.getText().trim());

            med.setCgst(parseDouble(txtCgst.getText()));
            med.setSgst(parseDouble(txtSgst.getText()));
            med.setIgst(parseDouble(txtIgst.getText()));

            med.setLocalTaxType(cmbLocal.getValue());
            med.setCentralTaxType(cmbCentral.getValue());
            med.setNegativeAllowed(cmbNegative.getValue());

            // Default Batch Details
            med.setBatchNumber("OPENING");
            med.setQuantity(0); // Assuming 0 for new ERP item opening stock
            med.setMrp(parseDouble(txtMrp.getText()));
            med.setPurchaseRate(parseDouble(txtPRate.getText()));
            med.setSellingRate(parseDouble(txtRateA.getText()));
            med.setRateA(parseDouble(txtRateA.getText()));
            med.setRateB(parseDouble(txtRateB.getText()));
            med.setRateC(parseDouble(txtRateC.getText()));
            med.setCostPerPcs(parseDouble(txtCost.getText()));
            med.setConvStr(parseInt(txtConvStri.getText()));
            med.setConvCas(parseInt(txtConvCas.getText()));

            boolean success;
            if (editingMedicineId != -1) {
                med.setMedicineId(editingMedicineId);
                success = repository.updateMedicineWithBatch(med);
            } else {
                success = repository.saveMedicineWithBatch(med);
            }

            if (success) {
                showListView();
            } else {
                lblStatus.setText("Failed to save product.");
                lblStatus.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            }
        } catch (Exception e) {
            lblStatus.setText("Error: " + e.getMessage());
            lblStatus.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            e.printStackTrace();
        }
    }

    @FXML
    private void clearForm() {
        editingMedicineId = -1;
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
        cmbCategory.getSelectionModel().clearSelection();
        cmbSalt.getSelectionModel().clearSelection();
        cmbBrand.getSelectionModel().clearSelection();
        cmbSchedule.getSelectionModel().clearSelection();
        
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
