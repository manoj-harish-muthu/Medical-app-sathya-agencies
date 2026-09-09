package com.pharmacyerp.controller;

import com.pharmacyerp.model.Medicine;
import com.pharmacyerp.repository.MedicineRepository;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDate;

public class AddMedicineController {

    @FXML private ComboBox<String> statusCombo;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> hideCombo;
    @FXML private TextField nameField;
    @FXML private TextField packingField;
    @FXML private TextField unit1stField;
    @FXML private TextField unit2ndField;
    @FXML private ComboBox<String> decimalCombo;
    @FXML private ComboBox<String> colorTypeCombo;
    @FXML private ComboBox<String> itemTypeCombo;
    @FXML private TextField companyField;
    @FXML private TextField saltField;
    @FXML private TextField hsnField;
    @FXML private TextField batchField;
    @FXML private DatePicker expiryDatePicker;
    @FXML private TextField quantityField;
    
    @FXML private ComboBox<String> localTaxCombo;
    @FXML private ComboBox<String> centralTaxCombo;
    @FXML private TextField mrpField;
    @FXML private TextField rateAField;
    @FXML private TextField convStrField;
    
    @FXML private TextField sgstField;
    @FXML private TextField igstField;
    @FXML private TextField purchaseRateField;
    @FXML private TextField rateBField;
    @FXML private TextField convCasField;
    
    @FXML private TextField cgstField;
    @FXML private TextField costPcsField;
    @FXML private TextField rateCField;
    @FXML private ComboBox<String> negativeCombo;

    @FXML private Label errorLabel;

    private MedicineController parentController;
    private MedicineRepository repository = new MedicineRepository();
    private int editingMedicineId = -1;

    public void setMedicineController(MedicineController controller) {
        this.parentController = controller;
    }

    public void setEditMode(Medicine med) {
        this.editingMedicineId = med.getMedicineId();
        
        nameField.setText(med.getMedicineName());
        saltField.setText(med.getSaltName());
        packingField.setText(med.getPacking());
        unit1stField.setText(med.getUnit1st());
        unit2ndField.setText(med.getUnit2nd());
        decimalCombo.setValue(med.getDecimalAllowed());
        colorTypeCombo.setValue(med.getColorType());
        itemTypeCombo.setValue(med.getItemType());
        hsnField.setText(med.getHsnCode());
        
        cgstField.setText(String.valueOf(med.getCgst()));
        sgstField.setText(String.valueOf(med.getSgst()));
        igstField.setText(String.valueOf(med.getIgst()));
        
        localTaxCombo.setValue(med.getLocalTaxType());
        centralTaxCombo.setValue(med.getCentralTaxType());
        negativeCombo.setValue(med.getNegativeAllowed());
        
        batchField.setText(med.getBatchNumber());
        if (med.getExpiryDate() != null) {
            expiryDatePicker.setValue(med.getExpiryDate());
        }
        quantityField.setText(String.valueOf(med.getTotalStock())); // the batch quantity is summed up as total stock for the table view
        mrpField.setText(String.valueOf(med.getCurrentMrp()));
        purchaseRateField.setText(String.valueOf(med.getPurchaseRate()));
        rateAField.setText(String.valueOf(med.getRateA()));
        rateBField.setText(String.valueOf(med.getRateB()));
        rateCField.setText(String.valueOf(med.getRateC()));
        costPcsField.setText(String.valueOf(med.getCostPerPcs()));
        convStrField.setText(String.valueOf(med.getConvStr()));
        convCasField.setText(String.valueOf(med.getConvCas()));
    }

    @FXML
    public void initialize() {
        // Initialize ComboBoxes
        statusCombo.setItems(FXCollections.observableArrayList("CONTINUE", "STOPPED"));
        statusCombo.getSelectionModel().selectFirst();
        
        typeCombo.setItems(FXCollections.observableArrayList("NORMAL", "PROHIBITED"));
        typeCombo.getSelectionModel().selectFirst();
        
        hideCombo.setItems(FXCollections.observableArrayList("NO", "YES"));
        hideCombo.getSelectionModel().selectFirst();
        
        decimalCombo.setItems(FXCollections.observableArrayList("No", "Yes"));
        decimalCombo.getSelectionModel().selectFirst();
        
        colorTypeCombo.setItems(FXCollections.observableArrayList("NORMAL", "SPECIAL"));
        colorTypeCombo.getSelectionModel().selectFirst();
        
        itemTypeCombo.setItems(FXCollections.observableArrayList("1 NORMAL", "2 SCHEDULE H"));
        itemTypeCombo.getSelectionModel().selectFirst();
        
        localTaxCombo.setItems(FXCollections.observableArrayList("Taxable", "Exempt"));
        localTaxCombo.getSelectionModel().selectFirst();
        
        centralTaxCombo.setItems(FXCollections.observableArrayList("Taxable", "Exempt"));
        centralTaxCombo.getSelectionModel().selectFirst();
        
        negativeCombo.setItems(FXCollections.observableArrayList("No", "Yes"));
        negativeCombo.getSelectionModel().selectFirst();

        expiryDatePicker.setValue(LocalDate.now().plusYears(1));
        
        // Defaults
        convStrField.setText("0");
        convCasField.setText("0");
        sgstField.setText("0.00");
        cgstField.setText("0.00");
        igstField.setText("0.00");
    }

    @FXML
    private void handleSave(ActionEvent event) {
        errorLabel.setText("");
        try {
            if (nameField.getText().trim().isEmpty() || batchField.getText().trim().isEmpty() || quantityField.getText().trim().isEmpty()) {
                errorLabel.setText("PRODUCT, BATCH, and QUANTITY are required!");
                return;
            }

            Medicine med = new Medicine();
            med.setMedicineName(nameField.getText().trim().toUpperCase());
            med.setSaltName(saltField.getText().trim().toUpperCase());
            med.setPacking(packingField.getText().trim().toUpperCase());
            med.setUnit1st(unit1stField.getText().trim().toUpperCase());
            med.setUnit2nd(unit2ndField.getText().trim().toUpperCase());
            med.setDecimalAllowed(decimalCombo.getValue());
            med.setColorType(colorTypeCombo.getValue());
            med.setItemType(itemTypeCombo.getValue());
            med.setHsnCode(hsnField.getText().trim());
            
            med.setCgst(parseDoubleSafely(cgstField.getText()));
            med.setSgst(parseDoubleSafely(sgstField.getText()));
            med.setIgst(parseDoubleSafely(igstField.getText()));
            
            med.setLocalTaxType(localTaxCombo.getValue());
            med.setCentralTaxType(centralTaxCombo.getValue());
            med.setNegativeAllowed(negativeCombo.getValue());

            // Batch fields
            med.setBatchNumber(batchField.getText().trim());
            med.setExpiryDate(expiryDatePicker.getValue());
            med.setQuantity(Integer.parseInt(quantityField.getText().trim()));
            med.setMrp(parseDoubleSafely(mrpField.getText()));
            med.setPurchaseRate(parseDoubleSafely(purchaseRateField.getText()));
            med.setSellingRate(parseDoubleSafely(rateAField.getText())); // Mapping Rate-A to selling rate for POS
            med.setRateA(parseDoubleSafely(rateAField.getText()));
            med.setRateB(parseDoubleSafely(rateBField.getText()));
            med.setRateC(parseDoubleSafely(rateCField.getText()));
            med.setCostPerPcs(parseDoubleSafely(costPcsField.getText()));
            med.setConvStr(Integer.parseInt(convStrField.getText().trim().isEmpty() ? "0" : convStrField.getText().trim()));
            med.setConvCas(Integer.parseInt(convCasField.getText().trim().isEmpty() ? "0" : convCasField.getText().trim()));

            boolean success;
            if (editingMedicineId != -1) {
                med.setMedicineId(editingMedicineId);
                success = repository.updateMedicineWithBatch(med);
            } else {
                success = repository.saveMedicineWithBatch(med);
            }
            
            if (success) {
                if (parentController != null) {
                    parentController.refreshTable();
                }
                closeWindow();
            } else {
                errorLabel.setText("Database error. Could not save product.");
            }
            
        } catch (NumberFormatException e) {
            errorLabel.setText("Please enter valid numeric values for Rates/Taxes.");
        } catch (Exception e) {
            errorLabel.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private double parseDoubleSafely(String val) {
        if (val == null || val.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(val.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeWindow();
    }
    
    private void closeWindow() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}
