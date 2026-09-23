package com.pharmacyerp.controller;

import com.pharmacyerp.dao.BrandDAO;
import com.pharmacyerp.model.Brand;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.List;

public class MedBrandController {

    @FXML private TableView<Brand> tblData;
    @FXML private TableColumn<Brand, Integer> colId;
    @FXML private TableColumn<Brand, String> colName;
    @FXML private TableColumn<Brand, String> colDetails;
    @FXML private TableColumn<Brand, Void> colAction;
    @FXML private Label lblTotalItems;

    @FXML private TextField searchField;
    @FXML private TextField nameField;
    @FXML private TextArea detailsArea;

    private BrandDAO brandDao = new BrandDAO();
    private ObservableList<Brand> brandList = FXCollections.observableArrayList();
    private Brand selectedBrand = null;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("brandId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("brandName"));
        colDetails.setCellValueFactory(new PropertyValueFactory<>("description"));

        colAction.setCellFactory(param -> new TableCell<Brand, Void>() {
            private final Button editBtn = new Button("Edit");
            {
                editBtn.getStyleClass().add("button-primary");
                editBtn.setOnAction(event -> {
                    selectedBrand = getTableView().getItems().get(getIndex());
                    nameField.setText(selectedBrand.getBrandName());
                    detailsArea.setText(selectedBrand.getDescription());
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

        tblData.setItems(brandList);
        loadData();
    }

    private void loadData() {
        brandList.clear();
        List<Brand> brands = brandDao.getAllBrands();
        brandList.addAll(brands);
        lblTotalItems.setText(String.valueOf(brands.size()));
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String name = nameField.getText();
        String details = detailsArea.getText();

        if (name == null || name.trim().isEmpty()) {
            showAlert("Error", "Brand Name is required.");
            return;
        }

        boolean success;
        if (selectedBrand == null) {
            Brand newBrand = new Brand();
            newBrand.setBrandName(name.trim());
            newBrand.setDescription(details != null ? details.trim() : "");
            success = brandDao.addBrand(newBrand);
        } else {
            selectedBrand.setBrandName(name.trim());
            selectedBrand.setDescription(details != null ? details.trim() : "");
            success = brandDao.updateBrand(selectedBrand);
        }

        if (success) {
            handleClear(null);
            loadData();
            showAlert("Success", "Brand details saved.");
        } else {
            showAlert("Error", "Failed to save brand details.");
        }
    }

    @FXML
    private void handleClear(ActionEvent event) {
        selectedBrand = null;
        nameField.clear();
        detailsArea.clear();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleDashboard(ActionEvent event) {
        SidebarController sidebar = new SidebarController();
        sidebar.navigateTo(event, "/fxml/Dashboard.fxml");
    }
}
