package com.pharmacyerp.controller;

import com.pharmacyerp.dao.ScheduleDAO;
import com.pharmacyerp.model.Schedule;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.List;

public class MedScheduleController {

    @FXML private TableView<Schedule> tblData;
    @FXML private TableColumn<Schedule, Integer> colId;
    @FXML private TableColumn<Schedule, String> colName;
    @FXML private TableColumn<Schedule, Boolean> colPrescription;
    @FXML private TableColumn<Schedule, String> colDetails;
    @FXML private TableColumn<Schedule, Void> colAction;
    @FXML private Label lblTotalItems;

    @FXML private TextField searchField;
    @FXML private TextField nameField;
    @FXML private CheckBox prescriptionCheck;
    @FXML private TextArea detailsArea;

    private ScheduleDAO scheduleDao = new ScheduleDAO();
    private ObservableList<Schedule> scheduleList = FXCollections.observableArrayList();
    private Schedule selectedSchedule = null;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("scheduleId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("scheduleName"));
        colDetails.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        colPrescription.setCellValueFactory(new PropertyValueFactory<>("requiresPrescription"));
        colPrescription.setCellFactory(column -> new TableCell<Schedule, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item ? "Yes" : "No");
                }
            }
        });

        colAction.setCellFactory(param -> new TableCell<Schedule, Void>() {
            private final Button editBtn = new Button("Edit");
            {
                editBtn.getStyleClass().add("button-primary");
                editBtn.setOnAction(event -> {
                    selectedSchedule = getTableView().getItems().get(getIndex());
                    nameField.setText(selectedSchedule.getScheduleName());
                    prescriptionCheck.setSelected(selectedSchedule.isRequiresPrescription());
                    detailsArea.setText(selectedSchedule.getDescription());
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

        tblData.setItems(scheduleList);
        loadData();
    }

    private void loadData() {
        scheduleList.clear();
        List<Schedule> schedules = scheduleDao.getAllSchedules();
        scheduleList.addAll(schedules);
        lblTotalItems.setText(String.valueOf(schedules.size()));
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String name = nameField.getText();
        boolean requiresPrescription = prescriptionCheck.isSelected();
        String details = detailsArea.getText();

        if (name == null || name.trim().isEmpty()) {
            showAlert("Error", "Schedule Name is required.");
            return;
        }

        boolean success;
        if (selectedSchedule == null) {
            Schedule newSchedule = new Schedule();
            newSchedule.setScheduleName(name.trim());
            newSchedule.setRequiresPrescription(requiresPrescription);
            newSchedule.setDescription(details != null ? details.trim() : "");
            success = scheduleDao.addSchedule(newSchedule);
        } else {
            selectedSchedule.setScheduleName(name.trim());
            selectedSchedule.setRequiresPrescription(requiresPrescription);
            selectedSchedule.setDescription(details != null ? details.trim() : "");
            success = scheduleDao.updateSchedule(selectedSchedule);
        }

        if (success) {
            handleClear(null);
            loadData();
            showAlert("Success", "Schedule details saved.");
        } else {
            showAlert("Error", "Failed to save schedule details.");
        }
    }

    @FXML
    private void handleClear(ActionEvent event) {
        selectedSchedule = null;
        nameField.clear();
        prescriptionCheck.setSelected(false);
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
