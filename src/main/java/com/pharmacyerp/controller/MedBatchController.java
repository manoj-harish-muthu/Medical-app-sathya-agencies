package com.pharmacyerp.controller;

import com.pharmacyerp.database.DatabaseManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;

public class MedBatchController implements Initializable {

    public static class MasterData {
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty name;
        private final SimpleStringProperty details;
        private final SimpleStringProperty extra1;
        private final SimpleStringProperty extra2;

        public MasterData(int id, String name, String details, String extra1, String extra2) {
            this.id = new SimpleIntegerProperty(id);
            this.name = new SimpleStringProperty(name);
            this.details = new SimpleStringProperty(details);
            this.extra1 = new SimpleStringProperty(extra1);
            this.extra2 = new SimpleStringProperty(extra2);
        }

        public int getId() { return id.get(); }
        public void setId(int id) { this.id.set(id); }
        
        public String getName() { return name.get(); }
        public void setName(String name) { this.name.set(name); }
        
        public String getDetails() { return details.get(); }
        public void setDetails(String details) { this.details.set(details); }
        
        public String getExtra1() { return extra1.get(); }
        public void setExtra1(String extra1) { this.extra1.set(extra1); }
        
        public String getExtra2() { return extra2.get(); }
        public void setExtra2(String extra2) { this.extra2.set(extra2); }
    }

    @FXML private TableView<MasterData> tblData;
    @FXML private TableColumn<MasterData, Integer> colId;
    @FXML private TableColumn<MasterData, String> colName;
    @FXML private TableColumn<MasterData, String> colDetails;
    @FXML private TableColumn<MasterData, String> colExtra1;
    @FXML private TableColumn<MasterData, String> colExtra2;
    @FXML private Label lblTotalItems;

    private ObservableList<MasterData> records = FXCollections.observableArrayList();
    private final String tableName = "";
    private final String col1 = ""; // Name
    private final String col2 = ""; // Details
    private final String col3 = ""; // Extra1
    private final String col4 = ""; // Extra2

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        tblData.setItems(records);
        tblData.setEditable(true);

        setupColumn(colName, md -> md.getName(), (md, val) -> md.setName(val));
        setupColumn(colDetails, md -> md.getDetails(), (md, val) -> md.setDetails(val));
        setupColumn(colExtra1, md -> md.getExtra1(), (md, val) -> md.setExtra1(val));
        setupColumn(colExtra2, md -> md.getExtra2(), (md, val) -> md.setExtra2(val));

        loadData();
    }

    private void setupColumn(TableColumn<MasterData, String> col, java.util.function.Function<MasterData, String> getter, java.util.function.BiConsumer<MasterData, String> setter) {
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(event -> {
            setter.accept(event.getRowValue(), event.getNewValue());
            checkAndAddEmptyRow();
        });
    }

    @FXML
    public void loadData() {
        records.clear();
        boolean success = false;
        if (!tableName.isEmpty()) {
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM " + tableName + " LIMIT 50")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt(1);
                        String name = col1.isEmpty() ? "N/A" : rs.getString(col1);
                        String details = col2.isEmpty() ? "" : rs.getString(col2);
                        String extra1 = col3.isEmpty() ? "" : rs.getString(col3);
                        String extra2 = col4.isEmpty() ? "" : rs.getString(col4);
                        records.add(new MasterData(id, name != null ? name : "", details != null ? details : "", extra1 != null ? extra1 : "", extra2 != null ? extra2 : ""));
                    }
                    success = true;
                }
            } catch (Exception e) {
                System.err.println("Error loading table " + tableName + ": " + e.getMessage());
            }
        }

        

        addEmptyRow();
        updateTotals();
    }

    private void addEmptyRow() {
        records.add(new MasterData(0, "", "", "", ""));
    }

    private void checkAndAddEmptyRow() {
        if (records.isEmpty()) {
            addEmptyRow();
            return;
        }
        MasterData last = records.get(records.size() - 1);
        if (last.getName() != null && !last.getName().trim().isEmpty()) {
            addEmptyRow();
        }
    }

    private void updateTotals() {
        int valid = 0;
        for (MasterData md : records) {
            if (md.getName() != null && !md.getName().trim().isEmpty()) valid++;
        }
        lblTotalItems.setText(String.valueOf(valid));
    }

    @FXML
    private void handleSave(ActionEvent event) {
        int saved = 0;
        if (!tableName.isEmpty() && !col1.isEmpty()) {
            StringBuilder cols = new StringBuilder(col1);
            StringBuilder vals = new StringBuilder("?");
            if (!col2.isEmpty()) { cols.append(", ").append(col2); vals.append(", ?"); }
            if (!col3.isEmpty()) { cols.append(", ").append(col3); vals.append(", ?"); }
            if (!col4.isEmpty()) { cols.append(", ").append(col4); vals.append(", ?"); }
            
            StringBuilder upds = new StringBuilder(col1 + " = ?");
            if (!col2.isEmpty()) upds.append(", ").append(col2).append(" = ?");
            if (!col3.isEmpty()) upds.append(", ").append(col3).append(" = ?");
            if (!col4.isEmpty()) upds.append(", ").append(col4).append(" = ?");

            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement insert = conn.prepareStatement("INSERT INTO " + tableName + " (" + cols + ") VALUES (" + vals + ")");
                 PreparedStatement update = conn.prepareStatement("UPDATE " + tableName + " SET " + upds + " WHERE " + tableName.substring(0, tableName.length()-1) + "_id = ?")) {
                 
                for (MasterData md : records) {
                    if (md.getName() == null || md.getName().trim().isEmpty()) continue;
                    
                    if (md.getId() == 0) {
                        int i = 1;
                        insert.setString(i++, md.getName());
                        if (!col2.isEmpty()) insert.setString(i++, md.getDetails());
                        if (!col3.isEmpty()) insert.setString(i++, md.getExtra1());
                        if (!col4.isEmpty()) insert.setString(i++, md.getExtra2());
                        insert.executeUpdate();
                        saved++;
                    } else {
                        int i = 1;
                        update.setString(i++, md.getName());
                        if (!col2.isEmpty()) update.setString(i++, md.getDetails());
                        if (!col3.isEmpty()) update.setString(i++, md.getExtra1());
                        if (!col4.isEmpty()) update.setString(i++, md.getExtra2());
                        update.setInt(i, md.getId());
                        update.executeUpdate();
                        saved++;
                    }
                }
            } catch (Exception e) {
                System.err.println("DB Save Error: " + e.getMessage());
            }
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Save Successful");
        alert.setHeaderText(null);
        alert.setContentText("Successfully synced " + saved + " records with the database!");
        alert.showAndWait();
        
        loadData();
    }
}
