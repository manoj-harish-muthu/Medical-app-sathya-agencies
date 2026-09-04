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

import com.pharmacyerp.model.Manufacturer;
import com.pharmacyerp.dao.ManufacturerDAO;

import java.io.IOException;

public class ManufacturerController {

    @FXML private TextField nameField;
    @FXML private TextField contactField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField searchField;
    
    @FXML private TableView<Manufacturer> manufacturerTable;
    @FXML private TableColumn<Manufacturer, String> colName;
    @FXML private TableColumn<Manufacturer, String> colContact;
    @FXML private TableColumn<Manufacturer, String> colPhone;
    @FXML private TableColumn<Manufacturer, String> colEmail;
    @FXML private TableColumn<Manufacturer, Void> colAction;

    private ManufacturerDAO mDao = new ManufacturerDAO();
    private ObservableList<Manufacturer> masterList = FXCollections.observableArrayList();
    private Manufacturer selectedManufacturer = null;

    @FXML
    public void initialize() {
        // Setup Action Column
        colAction.setCellFactory(param -> new TableCell<Manufacturer, Void>() {
            private final Button editBtn = new Button("Edit");
            {
                editBtn.getStyleClass().add("button-primary");
                editBtn.setOnAction(event -> {
                    selectedManufacturer = getTableView().getItems().get(getIndex());
                    nameField.setText(selectedManufacturer.getCompanyName());
                    contactField.setText(selectedManufacturer.getContactPerson());
                    phoneField.setText(selectedManufacturer.getPhone());
                    emailField.setText(selectedManufacturer.getEmail());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(editBtn);
                }
            }
        });

        searchField.textProperty().addListener((obs, oldV, newV) -> filterList(newV));
        
        loadData();
    }

    private void loadData() {
        masterList.setAll(mDao.getAllManufacturers());
        manufacturerTable.setItems(masterList);
    }

    private void filterList(String query) {
        if (query == null || query.isEmpty()) {
            manufacturerTable.setItems(masterList);
            return;
        }
        String q = query.toLowerCase();
        ObservableList<Manufacturer> filtered = FXCollections.observableArrayList();
        for (Manufacturer m : masterList) {
            if (m.getCompanyName().toLowerCase().contains(q) ||
                (m.getContactPerson() != null && m.getContactPerson().toLowerCase().contains(q))) {
                filtered.add(m);
            }
        }
        manufacturerTable.setItems(filtered);
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Company Name is required!");
            return;
        }

        if (selectedManufacturer == null) {
            // Add new
            Manufacturer m = new Manufacturer();
            m.setCompanyName(name);
            m.setContactPerson(contactField.getText().trim());
            m.setPhone(phoneField.getText().trim());
            m.setEmail(emailField.getText().trim());
            
            if (mDao.addManufacturer(m)) {
                showAlert(Alert.AlertType.INFORMATION, "Manufacturer added successfully!");
                handleClear();
                loadData();
            } else {
                showAlert(Alert.AlertType.ERROR, "Failed to add manufacturer. Name might be duplicate.");
            }
        } else {
            // Update
            selectedManufacturer.setCompanyName(name);
            selectedManufacturer.setContactPerson(contactField.getText().trim());
            selectedManufacturer.setPhone(phoneField.getText().trim());
            selectedManufacturer.setEmail(emailField.getText().trim());
            
            if (mDao.updateManufacturer(selectedManufacturer)) {
                showAlert(Alert.AlertType.INFORMATION, "Manufacturer updated successfully!");
                handleClear();
                loadData();
            } else {
                showAlert(Alert.AlertType.ERROR, "Failed to update manufacturer.");
            }
        }
    }

    @FXML
    private void handleClear() {
        selectedManufacturer = null;
        nameField.clear();
        contactField.clear();
        phoneField.clear();
        emailField.clear();
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
            stage.setScene(new Scene(root, 1366, 768));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
