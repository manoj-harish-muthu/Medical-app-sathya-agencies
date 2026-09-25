package com.pharmacyerp.controller;

import com.pharmacyerp.dao.UserDAO;
import com.pharmacyerp.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class UserManagementController {

    @FXML private TableView<User> tblUsers;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colFullName;
    @FXML private TableColumn<User, String> colRole;

    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label lblError;
    
    @FXML private CheckBox chkBilling;
    @FXML private CheckBox chkInventory;
    @FXML private CheckBox chkReports;
    @FXML private CheckBox chkMasters;
    @FXML private CheckBox chkAccounts;

    private UserDAO userDAO = new UserDAO();
    private ObservableList<User> userList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("ADMIN", "REP", "CASHIER"));
        roleCombo.getSelectionModel().selectFirst();

        colId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        tblUsers.setItems(userList);
        loadUsers();
    }

    private void loadUsers() {
        userList.clear();
        List<User> users = userDAO.getAllUsers();
        userList.addAll(users);
    }

    @FXML
    private void handleCreateUser(ActionEvent event) {
        lblError.setVisible(false);
        String name = fullNameField.getText();
        String uname = usernameField.getText();
        String pass = passwordField.getText();
        String role = roleCombo.getValue();

        if (name == null || name.trim().isEmpty() || 
            uname == null || uname.trim().isEmpty() || 
            pass == null || pass.trim().isEmpty()) {
            showError("All fields are required.");
            return;
        }
        
        StringBuilder perms = new StringBuilder();
        if (chkBilling.isSelected()) perms.append("BILLING,");
        if (chkInventory.isSelected()) perms.append("INVENTORY,");
        if (chkReports.isSelected()) perms.append("REPORTS,");
        if (chkMasters.isSelected()) perms.append("MASTERS,");
        if (chkAccounts.isSelected()) perms.append("ACCOUNTS,");
        
        String permissions = perms.toString();
        if (permissions.length() > 0) {
            permissions = permissions.substring(0, permissions.length() - 1);
        } else {
            permissions = "NONE";
        }

        boolean success = userDAO.createUser(uname.trim(), pass, name.trim(), role, permissions);
        if (success) {
            fullNameField.clear();
            usernameField.clear();
            passwordField.clear();
            loadUsers();
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("User account created successfully.");
            alert.showAndWait();
        } else {
            showError("Failed to create user. Username might already exist.");
        }
    }

    @FXML
    private void handleDeleteUser(ActionEvent event) {
        User selected = tblUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a user to delete.");
            return;
        }
        
        if ("admin".equalsIgnoreCase(selected.getUsername())) {
            showError("Cannot delete the default admin account.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to delete user: " + selected.getUsername() + "?");
        
        if (confirm.showAndWait().get() == ButtonType.OK) {
            boolean success = userDAO.deleteUser(selected.getUserId());
            if (success) {
                loadUsers();
            } else {
                showError("Failed to delete user.");
            }
        }
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
    }
}
