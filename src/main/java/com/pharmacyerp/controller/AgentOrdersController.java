package com.pharmacyerp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacyerp.model.AgentOrder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AgentOrdersController {
    private static final Logger logger = LoggerFactory.getLogger(AgentOrdersController.class);

    @FXML private TableView<AgentOrder> ordersTable;
    @FXML private TableColumn<AgentOrder, String> colPhone;
    @FXML private TableColumn<AgentOrder, String> colCustomer;
    @FXML private TableColumn<AgentOrder, String> colAddress;
    @FXML private TableColumn<AgentOrder, String> colItems;
    @FXML private TableColumn<AgentOrder, String> colStatus;
    @FXML private TableColumn<AgentOrder, Void> colAction;
    @FXML private Label countLabel;

    private ObservableList<AgentOrder> orderList = FXCollections.observableArrayList();
    private ObjectMapper objectMapper = new ObjectMapper();

    @FXML
    public void initialize() {
        setupTable();
        loadOrders();
    }

    private void setupTable() {
        if (colPhone != null) colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        if (colCustomer != null) colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        if (colAddress != null) colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        if (colItems != null) colItems.setCellValueFactory(new PropertyValueFactory<>("itemsSummary"));
        if (colStatus != null) colStatus.setCellValueFactory(new PropertyValueFactory<>("statusDisplay"));

        if (colAction != null) {
            colAction.setCellFactory(param -> new TableCell<>() {
                private final Button billBtn = new Button("Bill in POS");
                private final Button approveBtn = new Button("Approve");
                private final HBox pane = new HBox(6, billBtn, approveBtn);

                {
                    pane.setAlignment(javafx.geometry.Pos.CENTER);
                    billBtn.getStyleClass().addAll("button-action", "button-primary");
                    billBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
                    billBtn.setOnAction(e -> {
                        AgentOrder order = getTableView().getItems().get(getIndex());
                        openOrderInPos(e, order);
                    });

                    approveBtn.getStyleClass().addAll("button-action", "button-outline");
                    approveBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
                    approveBtn.setOnAction(e -> {
                        AgentOrder order = getTableView().getItems().get(getIndex());
                        markOrderApproved(order);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        AgentOrder order = getTableView().getItems().get(getIndex());
                        if ("approved".equalsIgnoreCase(order.getAdminStatus())) {
                            approveBtn.setText("Approved ✓");
                            approveBtn.setDisable(true);
                        } else {
                            approveBtn.setText("Approve");
                            approveBtn.setDisable(false);
                        }
                        setGraphic(pane);
                    }
                }
            });
        }
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        loadOrders();
    }

    private void loadOrders() {
        orderList.clear();
        File ordersDir = resolveOrdersDirectory();

        if (ordersDir != null && ordersDir.exists() && ordersDir.isDirectory()) {
            File[] files = ordersDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File f : files) {
                    try {
                        JsonNode root = objectMapper.readTree(f);
                        AgentOrder order = new AgentOrder();
                        order.setFilePath(f.getAbsolutePath());
                        order.setCustomerId(root.path("customer_id").asText(""));
                        order.setCustomerName(root.path("customer_name").asText(""));
                        order.setPhone(root.path("phone").asText(""));
                        order.setAddress(root.path("address").asText(""));
                        order.setConfirmed(root.path("confirmed").asBoolean(false));
                        order.setCheckedByAdmin(root.path("is_checked_by_admin").asBoolean(false));
                        order.setAdminStatus(root.path("admin_status").asText("pending"));

                        List<AgentOrder.OrderItem> items = new ArrayList<>();
                        JsonNode itemsNode = root.path("items");
                        if (itemsNode.isArray()) {
                            for (JsonNode itNode : itemsNode) {
                                String med = itNode.path("medicine").asText("");
                                String qty = itNode.path("quantity").asText("1");
                                String unit = itNode.path("unit").asText("");
                                items.add(new AgentOrder.OrderItem(med, qty, unit));
                            }
                        }
                        order.setItems(items);
                        orderList.add(order);
                    } catch (Exception e) {
                        logger.error("Error reading order JSON file: {}", f.getName(), e);
                    }
                }
            }
        }

        if (ordersTable != null) {
            ordersTable.setItems(orderList);
        }
        if (countLabel != null) {
            countLabel.setText(orderList.size() + " orders loaded from WhatsApp agent");
        }
    }

    private File resolveOrdersDirectory() {
        String userHome = System.getProperty("user.home");
        File[] candidates = new File[] {
                new File(userHome, "Desktop/agents/medical-agent/data/orders"),
                new File("C:/Users/BALAHARISH NATARAJAN/Desktop/agents/medical-agent/data/orders"),
                new File("../medical-agent/data/orders"),
                new File("data/orders")
        };

        for (File candidate : candidates) {
            if (candidate.exists() && candidate.isDirectory()) {
                return candidate;
            }
        }
        return candidates[0];
    }

    private void markOrderApproved(AgentOrder order) {
        if (order.getFilePath() == null) return;
        try {
            File f = new File(order.getFilePath());
            if (f.exists()) {
                JsonNode root = objectMapper.readTree(f);
                if (root.isObject()) {
                    ((com.fasterxml.jackson.databind.node.ObjectNode) root).put("is_checked_by_admin", true);
                    ((com.fasterxml.jackson.databind.node.ObjectNode) root).put("admin_status", "approved");
                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(f, root);
                    order.setCheckedByAdmin(true);
                    order.setAdminStatus("approved");
                    ordersTable.refresh();
                }
            }
        } catch (Exception e) {
            logger.error("Error updating order file", e);
        }
    }

    private void openOrderInPos(ActionEvent event, AgentOrder order) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Pos.fxml"));
            Parent root = loader.load();
            
            PosController posController = loader.getController();
            if (posController != null) {
                // If there are medicines in the order, pre-populate in POS
                // We will navigate to POS and the cashier can proceed
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            logger.error("Error navigating to POS", e);
        }
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
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
