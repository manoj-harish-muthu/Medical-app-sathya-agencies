package com.pharmacyerp.controller;

import com.pharmacyerp.dao.GenericSaltDAO;
import com.pharmacyerp.model.GenericSalt;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.List;

public class MedGenericSaltController {

    @FXML private TableView<GenericSalt> tblData;
    @FXML private TableColumn<GenericSalt, Integer> colId;
    @FXML private TableColumn<GenericSalt, String> colName;
    @FXML private TableColumn<GenericSalt, String> colDetails;
    @FXML private TableColumn<GenericSalt, Void> colAction;
    @FXML private Label lblTotalItems;

    @FXML private TextField searchField;
    @FXML private TextField nameField;
    @FXML private TextArea detailsArea;

    private GenericSaltDAO saltDao = new GenericSaltDAO();
    private ObservableList<GenericSalt> saltList = FXCollections.observableArrayList();
    private GenericSalt selectedSalt = null;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("saltId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("saltName"));
        colDetails.setCellValueFactory(new PropertyValueFactory<>("description"));

        colAction.setCellFactory(param -> new TableCell<GenericSalt, Void>() {
            private final Button editBtn = new Button("Edit");
            {
                editBtn.getStyleClass().add("button-primary");
                editBtn.setOnAction(event -> {
                    selectedSalt = getTableView().getItems().get(getIndex());
                    nameField.setText(selectedSalt.getSaltName());
                    detailsArea.setText(selectedSalt.getDescription());
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

        tblData.setItems(saltList);
        loadData();
    }

    private void loadData() {
        saltList.clear();
        List<GenericSalt> salts = saltDao.getAllSalts();
        saltList.addAll(salts);
        lblTotalItems.setText(String.valueOf(salts.size()));
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String name = nameField.getText();
        String details = detailsArea.getText();

        if (name == null || name.trim().isEmpty()) {
            showAlert("Error", "Salt Name is required.");
            return;
        }

        boolean success;
        if (selectedSalt == null) {
            GenericSalt newSalt = new GenericSalt();
            newSalt.setSaltName(name.trim());
            newSalt.setDescription(details != null ? details.trim() : "");
            success = saltDao.addSalt(newSalt);
        } else {
            selectedSalt.setSaltName(name.trim());
            selectedSalt.setDescription(details != null ? details.trim() : "");
            success = saltDao.updateSalt(selectedSalt);
        }

        if (success) {
            handleClear(null);
            loadData();
            showAlert("Success", "Salt details saved.");
        } else {
            showAlert("Error", "Failed to save salt details.");
        }
    }

    @FXML
    private void handleClear(ActionEvent event) {
        selectedSalt = null;
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
