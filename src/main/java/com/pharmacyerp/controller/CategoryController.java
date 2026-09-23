package com.pharmacyerp.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import com.pharmacyerp.model.Category;
import com.pharmacyerp.dao.CategoryDAO;

import java.io.IOException;

public class CategoryController {

    @FXML private TextField nameField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField searchField;
    
    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, Integer> colId;
    @FXML private TableColumn<Category, String> colName;
    @FXML private TableColumn<Category, String> colDesc;
    @FXML private TableColumn<Category, Void> colAction;
    
    @FXML private Label lblTotalItems;

    private CategoryDAO cDao = new CategoryDAO();
    private ObservableList<Category> masterList = FXCollections.observableArrayList();
    private Category selectedCategory = null;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("categoryId"));
        
        // Setup Action Column
        colAction.setCellFactory(param -> new TableCell<Category, Void>() {
            private final Button editBtn = new Button("Edit");
            {
                editBtn.getStyleClass().add("button-primary");
                editBtn.setOnAction(event -> {
                    selectedCategory = getTableView().getItems().get(getIndex());
                    nameField.setText(selectedCategory.getCategoryName());
                    descriptionArea.setText(selectedCategory.getDescription());
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
        masterList.setAll(cDao.getAllCategories());
        categoryTable.setItems(masterList);
        if (lblTotalItems != null) {
            lblTotalItems.setText(String.valueOf(masterList.size()));
        }
    }

    private void filterList(String query) {
        if (query == null || query.isEmpty()) {
            categoryTable.setItems(masterList);
            return;
        }
        String q = query.toLowerCase();
        ObservableList<Category> filtered = FXCollections.observableArrayList();
        for (Category c : masterList) {
            if (c.getCategoryName().toLowerCase().contains(q)) {
                filtered.add(c);
            }
        }
        categoryTable.setItems(filtered);
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Category Name is required!");
            return;
        }

        if (selectedCategory == null) {
            // Add new
            Category c = new Category();
            c.setCategoryName(name);
            c.setDescription(descriptionArea.getText().trim());
            
            if (cDao.addCategory(c)) {
                showAlert(Alert.AlertType.INFORMATION, "Category added successfully!");
                handleClear(null);
                loadData();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error adding category.");
            }
        } else {
            // Update
            selectedCategory.setCategoryName(name);
            selectedCategory.setDescription(descriptionArea.getText().trim());
            
            if (cDao.updateCategory(selectedCategory)) {
                showAlert(Alert.AlertType.INFORMATION, "Category updated successfully!");
                handleClear(null);
                loadData();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error updating category.");
            }
        }
    }

    @FXML
    public void handleClear(ActionEvent event) {
        selectedCategory = null;
        nameField.clear();
        descriptionArea.clear();
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(type.name());
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
