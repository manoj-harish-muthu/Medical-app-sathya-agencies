package com.pharmacyerp.controller;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class AccountPayablesController implements Initializable {

    public static class HistoryData {
        private final SimpleStringProperty date;
        private final SimpleStringProperty name;
        private final SimpleStringProperty description;
        private final SimpleDoubleProperty amount;

        public HistoryData(String date, String name, String description, double amount) {
            this.date = new SimpleStringProperty(date);
            this.name = new SimpleStringProperty(name);
            this.description = new SimpleStringProperty(description);
            this.amount = new SimpleDoubleProperty(amount);
        }

        public String getDate() { return date.get(); }
        public String getName() { return name.get(); }
        public String getDescription() { return description.get(); }
        public double getAmount() { return amount.get(); }
    }
    
    @FXML private TextField txtSearch;
    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    
    @FXML private TableView<HistoryData> tblHistory;
    
    @FXML private Label lblTotalItems;
    @FXML private Label lblGrandTotal;

    private ObservableList<HistoryData> records = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dpFrom.setValue(LocalDate.now().minusDays(30));
        dpTo.setValue(LocalDate.now());
        
        loadDummyData();
        tblHistory.setItems(records);
        updateTotals();
    }

    private void loadDummyData() {
        // Dummy data removed as per user request
    }

    private void updateTotals() {
        double grandTotal = 0.0;
        for (HistoryData item : records) {
            grandTotal += item.getAmount();
        }
        lblTotalItems.setText(String.valueOf(records.size()));
        lblGrandTotal.setText(String.format("%.2f", grandTotal));
    }
}
