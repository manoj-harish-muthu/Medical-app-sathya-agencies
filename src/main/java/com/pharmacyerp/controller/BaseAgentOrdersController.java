package com.pharmacyerp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacyerp.dao.AgentOrderDAO;
import com.pharmacyerp.model.AgentOrder;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class BaseAgentOrdersController {
    protected static final Logger logger = LoggerFactory.getLogger(BaseAgentOrdersController.class);

    // Top & Filter Controls
    @FXML protected Label countLabel;
    @FXML protected Label tableStatsLabel;
    @FXML protected TextField searchField;
    @FXML protected Button btnFilterAll;
    @FXML protected Button btnFilterPending;
    @FXML protected Button btnFilterApproved;
    @FXML protected Button btnFilterPacking;

    // Master Table
    @FXML protected TableView<AgentOrder> ordersTable;
    @FXML protected TableColumn<AgentOrder, String> colPhone;
    @FXML protected TableColumn<AgentOrder, String> colCustomer;
    @FXML protected TableColumn<AgentOrder, String> colAddress;
    @FXML protected TableColumn<AgentOrder, Integer> colItemsCount;
    @FXML protected TableColumn<AgentOrder, String> colItems;
    @FXML protected TableColumn<AgentOrder, String> colStatus;
    @FXML protected TableColumn<AgentOrder, Void> colAction;

    // Details Pane
    @FXML protected VBox detailContainer;
    @FXML protected VBox emptyPlaceholder;
    @FXML protected VBox detailsContent;

    @FXML protected Label detailCustomerName;
    @FXML protected Label detailPhone;
    @FXML protected Label detailCustomerId;
    @FXML protected Label detailConfirmedBadge;
    @FXML protected Label detailStatusBadge;
    @FXML protected Label detailAddress;
    @FXML protected Button btnCopyAddress;
    @FXML protected Label detailItemsCount;

    @FXML protected TableView<AgentOrder.OrderItem> detailItemsTable;
    @FXML protected TableColumn<AgentOrder.OrderItem, String> colDetailMed;
    @FXML protected TableColumn<AgentOrder.OrderItem, String> colDetailQty;
    @FXML protected TableColumn<AgentOrder.OrderItem, String> colDetailUnit;
    @FXML protected TableColumn<AgentOrder.OrderItem, String> colDetailVol;
    @FXML protected TableColumn<AgentOrder.OrderItem, String> colDetailPieces;

    @FXML protected Button btnStatusApprove;
    @FXML protected Button btnStatusPacking;
    @FXML protected Button btnStatusShipped;
    @FXML protected Button btnStatusCompleted;
    @FXML protected Button btnStatusCancel;
    @FXML protected Button btnDetailBillInPos;

    protected final ObservableList<AgentOrder> masterOrderList = FXCollections.observableArrayList();
    protected FilteredList<AgentOrder> filteredOrderList;
    protected final ObservableList<AgentOrder.OrderItem> selectedItemsList = FXCollections.observableArrayList();

    protected final ObjectMapper objectMapper = new ObjectMapper();
    protected final AgentOrderDAO agentOrderDAO = new AgentOrderDAO();

    protected String currentStatusFilter = "ALL";
    protected AgentOrder selectedOrder;

    /**
     * Define the operational mode for this view:
     * - "ALL": Displays all incoming agent orders with full tab filtering
     * - "PENDING_ONLY": Shows only orders awaiting admin review or POS billing
     * - "COMPLETED_ONLY": Shows orders billed in POS, fulfilled, or archived
     * - "FAILED_ONLY": Shows cancelled orders or incomplete/abandoned customer drafts
     */
    public abstract String getPageMode();

    @FXML
    public void initialize() {
        setupMasterTable();
        setupDetailTable();
        setupFiltering();
        loadOrders();
    }

    protected void setupMasterTable() {
        if (colPhone != null) colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        if (colCustomer != null) colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        if (colAddress != null) colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        if (colItemsCount != null) colItemsCount.setCellValueFactory(new PropertyValueFactory<>("itemsCount"));
        if (colItems != null) colItems.setCellValueFactory(new PropertyValueFactory<>("itemsSummary"));

        if (colStatus != null) {
            colStatus.setCellValueFactory(new PropertyValueFactory<>("statusDisplay"));
            colStatus.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        Label badge = new Label(item);
                        badge.setStyle("-fx-font-weight: 800; -fx-font-size: 11px; -fx-padding: 3 8; -fx-background-radius: 10px;");

                        String lower = item.toLowerCase();
                        if (lower.contains("draft")) {
                            badge.setStyle(badge.getStyle() + "; -fx-background-color: #FEF3C7; -fx-text-fill: #92400E;");
                        } else if (lower.contains("approved")) {
                            badge.setStyle(badge.getStyle() + "; -fx-background-color: #DEF7EC; -fx-text-fill: #03543F;");
                        } else if (lower.contains("packing")) {
                            badge.setStyle(badge.getStyle() + "; -fx-background-color: #E0F2FE; -fx-text-fill: #0369A1;");
                        } else if (lower.contains("shipped") || lower.contains("delivery")) {
                            badge.setStyle(badge.getStyle() + "; -fx-background-color: #EDE9FE; -fx-text-fill: #6D28D9;");
                        } else if (lower.contains("completed")) {
                            badge.setStyle(badge.getStyle() + "; -fx-background-color: #DEF7EC; -fx-text-fill: #03543F;");
                        } else if (lower.contains("cancelled") || lower.contains("failed")) {
                            badge.setStyle(badge.getStyle() + "; -fx-background-color: #FDE8E8; -fx-text-fill: #9B1C1C;");
                        } else {
                            badge.setStyle(badge.getStyle() + "; -fx-background-color: #FEF3C7; -fx-text-fill: #92400E;");
                        }
                        setGraphic(badge);
                        setText(null);
                    }
                }
            });
        }

        if (colAction != null) {
            colAction.setCellFactory(param -> new TableCell<>() {
                private final Button billBtn = new Button("Bill POS");
                {
                    billBtn.getStyleClass().addAll("button-action", "button-primary");
                    billBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-font-weight: 800;");
                    billBtn.setOnAction(e -> {
                        AgentOrder order = getTableView().getItems().get(getIndex());
                        openOrderInPos(e, order);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        AgentOrder o = getTableView().getItems().get(getIndex());
                        if ("completed".equalsIgnoreCase(o.getAdminStatus())) {
                            billBtn.setText("Billed ✓");
                            billBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-font-weight: 800; -fx-background-color: #DEF7EC; -fx-text-fill: #03543F;");
                        } else if ("cancelled".equalsIgnoreCase(o.getAdminStatus())) {
                            billBtn.setText("Cancelled");
                            billBtn.setDisable(true);
                            billBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-font-weight: 800; -fx-background-color: #FDE8E8; -fx-text-fill: #9B1C1C;");
                        } else {
                            billBtn.setText("Bill POS");
                            billBtn.setDisable(false);
                            billBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-font-weight: 800; -fx-background-color: #0D9488; -fx-text-fill: #FFFFFF;");
                        }
                        setGraphic(billBtn);
                    }
                }
            });
        }

        // Selection listener to show details
        ordersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            displayOrderDetails(newVal);
        });
    }

    protected void setupDetailTable() {
        if (colDetailMed != null) colDetailMed.setCellValueFactory(new PropertyValueFactory<>("medicine"));
        if (colDetailQty != null) colDetailQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        if (colDetailUnit != null) colDetailUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        if (colDetailVol != null) colDetailVol.setCellValueFactory(new PropertyValueFactory<>("volumeMl"));
        if (colDetailPieces != null) colDetailPieces.setCellValueFactory(new PropertyValueFactory<>("piecesDisplay"));

        if (detailItemsTable != null) {
            detailItemsTable.setItems(selectedItemsList);
        }
    }

    protected void setupFiltering() {
        filteredOrderList = new FilteredList<>(masterOrderList, p -> true);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        }

        if (ordersTable != null) {
            ordersTable.setItems(filteredOrderList);
        }
    }

    protected void applyFilter() {
        String mode = getPageMode();
        String query = searchField != null ? searchField.getText().trim().toLowerCase() : "";

        filteredOrderList.setPredicate(order -> {
            String adminStatus = order.getAdminStatus() != null ? order.getAdminStatus().toLowerCase().trim() : "pending";
            boolean isFileCompleted = order.getFilePath() != null && order.getFilePath().toLowerCase().contains("completed");

            // Mode-specific partitioning
            if ("PENDING_ONLY".equals(mode)) {
                // Pending orders: Customer confirmed, but admin has not billed/completed it yet
                boolean isCompleted = "completed".equals(adminStatus) || "delivered".equals(adminStatus) || isFileCompleted;
                boolean isCancelled = "cancelled".equals(adminStatus);
                if (isCompleted || isCancelled) return false;
                if (!order.isConfirmed()) return false;
            } else if ("COMPLETED_ONLY".equals(mode)) {
                // Completed orders: Successfully processed, fulfilled or billed
                boolean isCompleted = "completed".equals(adminStatus) || "delivered".equals(adminStatus) || "billed".equals(adminStatus) || isFileCompleted;
                if (!isCompleted) return false;
            } else if ("FAILED_ONLY".equals(mode)) {
                // Failed orders: Cancelled by admin/customer or incomplete draft abandoned by customer
                boolean isFailed = "cancelled".equals(adminStatus) || "failed".equals(adminStatus) || !order.isConfirmed();
                if (!isFailed) return false;
            } else {
                // "ALL" Mode: Filter tabs
                if ("PENDING".equals(currentStatusFilter)) {
                    if (!order.isCheckedByAdmin() || "pending".equals(adminStatus)) {
                        // match
                    } else {
                        return false;
                    }
                } else if ("APPROVED".equals(currentStatusFilter)) {
                    if (!"approved".equals(adminStatus)) return false;
                } else if ("PACKING".equals(currentStatusFilter)) {
                    if (!(adminStatus.contains("pack") || adminStatus.contains("ship"))) return false;
                }
            }

            // Search query match
            if (query.isEmpty()) return true;

            if (order.getCustomerName().toLowerCase().contains(query)) return true;
            if (order.getPhone().toLowerCase().contains(query)) return true;
            if (order.getAddress().toLowerCase().contains(query)) return true;
            if (order.getCustomerId().toLowerCase().contains(query)) return true;

            if (order.getItems() != null) {
                for (AgentOrder.OrderItem it : order.getItems()) {
                    if (it.getMedicine().toLowerCase().contains(query)) return true;
                }
            }

            return false;
        });

        updateStatsLabel();
    }

    protected void updateStatsLabel() {
        int total = masterOrderList.size();
        int showing = filteredOrderList.size();
        String mode = getPageMode();

        if (tableStatsLabel != null) {
            tableStatsLabel.setText("Showing " + showing + " orders");
        }
        if (countLabel != null) {
            if ("PENDING_ONLY".equals(mode)) {
                countLabel.setText(showing + " pending WhatsApp orders awaiting action");
            } else if ("COMPLETED_ONLY".equals(mode)) {
                countLabel.setText(showing + " completed & billed orders from WhatsApp agent");
            } else if ("FAILED_ONLY".equals(mode)) {
                countLabel.setText(showing + " cancelled or incomplete customer orders");
            } else {
                long pendingCount = masterOrderList.stream()
                        .filter(o -> !o.isCheckedByAdmin() || "pending".equalsIgnoreCase(o.getAdminStatus()))
                        .count();
                countLabel.setText(total + " orders loaded from WhatsApp agent • " + pendingCount + " pending review");
            }
        }
    }

    @FXML
    public void handleFilterAll(ActionEvent event) {
        currentStatusFilter = "ALL";
        setActiveFilterButton(btnFilterAll);
        applyFilter();
    }

    @FXML
    public void handleFilterPending(ActionEvent event) {
        currentStatusFilter = "PENDING";
        setActiveFilterButton(btnFilterPending);
        applyFilter();
    }

    @FXML
    public void handleFilterApproved(ActionEvent event) {
        currentStatusFilter = "APPROVED";
        setActiveFilterButton(btnFilterApproved);
        applyFilter();
    }

    @FXML
    public void handleFilterPacking(ActionEvent event) {
        currentStatusFilter = "PACKING";
        setActiveFilterButton(btnFilterPacking);
        applyFilter();
    }

    protected void setActiveFilterButton(Button activeBtn) {
        Button[] all = new Button[] { btnFilterAll, btnFilterPending, btnFilterApproved, btnFilterPacking };
        for (Button b : all) {
            if (b != null) {
                if (b == activeBtn) {
                    b.setStyle("-fx-background-color: #0D9488; -fx-text-fill: white; -fx-font-weight: 800;");
                } else {
                    b.setStyle("-fx-background-color: #E5E7EB; -fx-text-fill: black; -fx-font-weight: 700;");
                }
            }
        }
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        loadOrders();
    }

    public void loadOrders() {
        masterOrderList.clear();
        selectedItemsList.clear();
        displayOrderDetails(null);

        File ordersDir = resolveOrdersDirectory();

        if (ordersDir != null && ordersDir.exists() && ordersDir.isDirectory()) {
            // Read active orders
            File[] files = ordersDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File f : files) {
                    parseAndAddOrder(f);
                }
            }

            // Also check completed/ subfolder
            File completedDir = new File(ordersDir, "completed");
            if (completedDir.exists() && completedDir.isDirectory()) {
                File[] completedFiles = completedDir.listFiles((dir, name) -> name.endsWith(".json"));
                if (completedFiles != null) {
                    for (File f : completedFiles) {
                        parseAndAddOrder(f);
                    }
                }
            }
        }

        applyFilter();

        if (!filteredOrderList.isEmpty() && ordersTable != null) {
            ordersTable.getSelectionModel().selectFirst();
        }
    }

    protected void parseAndAddOrder(File f) {
        try {
            JsonNode root = objectMapper.readTree(f);
            AgentOrder order = new AgentOrder();
            order.setFilePath(f.getAbsolutePath());
            order.setCustomerId(root.path("customer_id").asText(""));
            order.setCustomerName(root.path("customer_name").asText("Walk-in / Unknown"));
            order.setPhone(root.path("phone").asText(""));
            order.setAddress(root.path("address").asText(""));
            order.setConfirmed(root.path("confirmed").asBoolean(false));
            order.setCheckedByAdmin(root.path("is_checked_by_admin").asBoolean(false));
            order.setAdminStatus(root.path("admin_status").asText("pending"));

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a");
            order.setOrderDate(sdf.format(new Date(f.lastModified())));

            List<AgentOrder.OrderItem> items = new ArrayList<>();
            JsonNode itemsNode = root.path("items");
            if (itemsNode.isArray()) {
                for (JsonNode itNode : itemsNode) {
                    String med = itNode.path("medicine").asText("");
                    String qty = itNode.path("quantity").asText("1");
                    String unit = itNode.path("unit").asText("");
                    String volumeMl = itNode.path("volume_ml").asText(null);
                    Integer pieces = itNode.hasNonNull("pieces_per_unit") ? itNode.path("pieces_per_unit").asInt() : null;

                    items.add(new AgentOrder.OrderItem(med, qty, unit, volumeMl, pieces));
                }
            }
            order.setItems(items);

            // Sync to Database
            agentOrderDAO.saveOrSyncOrder(order);

            masterOrderList.add(order);
        } catch (Exception e) {
            logger.error("Error reading order JSON file: {}", f.getName(), e);
        }
    }

    protected void displayOrderDetails(AgentOrder order) {
        this.selectedOrder = order;

        if (order == null) {
            if (emptyPlaceholder != null) {
                emptyPlaceholder.setVisible(true);
                emptyPlaceholder.setManaged(true);
            }
            if (detailsContent != null) {
                detailsContent.setVisible(false);
                detailsContent.setManaged(false);
            }
            selectedItemsList.clear();
            return;
        }

        if (emptyPlaceholder != null) {
            emptyPlaceholder.setVisible(false);
            emptyPlaceholder.setManaged(false);
        }
        if (detailsContent != null) {
            detailsContent.setVisible(true);
            detailsContent.setManaged(true);
        }

        if (detailCustomerName != null) detailCustomerName.setText(order.getCustomerName());
        if (detailPhone != null) detailPhone.setText("Phone: " + (order.getPhone().isEmpty() ? "N/A" : order.getPhone()));
        if (detailCustomerId != null) detailCustomerId.setText("WhatsApp ID: " + order.getCustomerId());

        if (detailConfirmedBadge != null) {
            if (order.isConfirmed()) {
                detailConfirmedBadge.setText("Customer Confirmed ✓");
                detailConfirmedBadge.setStyle("-fx-background-color: #DEF7EC; -fx-text-fill: #03543F; -fx-font-weight: 800; -fx-padding: 3 8; -fx-background-radius: 10px;");
            } else {
                detailConfirmedBadge.setText("Draft / In Progress");
                detailConfirmedBadge.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-font-weight: 800; -fx-padding: 3 8; -fx-background-radius: 10px;");
            }
        }

        if (detailStatusBadge != null) {
            detailStatusBadge.setText(order.getStatusDisplay());
            detailStatusBadge.setStyle("-fx-background-color: #0D9488; -fx-text-fill: white; -fx-font-weight: 800; -fx-padding: 3 10; -fx-background-radius: 10px;");
        }

        if (detailAddress != null) {
            detailAddress.setText(order.getAddress().isEmpty() ? "No delivery address provided yet." : order.getAddress());
        }

        selectedItemsList.clear();
        if (order.getItems() != null) {
            selectedItemsList.addAll(order.getItems());
        }
        if (detailItemsCount != null) {
            detailItemsCount.setText(selectedItemsList.size() + " medicines prescribed");
        }

        updateWorkflowButtons(order);
    }

    protected void updateWorkflowButtons(AgentOrder order) {
        String status = order.getAdminStatus().toLowerCase();
        boolean isApproved = "approved".equals(status);
        boolean isPacking = status.contains("pack");
        boolean isShipped = status.contains("ship");
        boolean isCompleted = "completed".equals(status);
        boolean isCancelled = "cancelled".equals(status);

        if (btnStatusApprove != null) {
            btnStatusApprove.setDisable(isApproved || isPacking || isShipped || isCompleted || isCancelled);
            btnStatusApprove.setStyle(isApproved ? "-fx-background-color: #DEF7EC; -fx-text-fill: #03543F; -fx-font-weight: 800;" : "");
        }
        if (btnStatusPacking != null) {
            btnStatusPacking.setDisable(isPacking || isShipped || isCompleted || isCancelled);
            btnStatusPacking.setStyle(isPacking ? "-fx-background-color: #E0F2FE; -fx-text-fill: #0369A1; -fx-font-weight: 800;" : "");
        }
        if (btnStatusShipped != null) {
            btnStatusShipped.setDisable(isShipped || isCompleted || isCancelled);
            btnStatusShipped.setStyle(isShipped ? "-fx-background-color: #EDE9FE; -fx-text-fill: #6D28D9; -fx-font-weight: 800;" : "");
        }
        if (btnStatusCompleted != null) {
            btnStatusCompleted.setDisable(isCompleted);
            btnStatusCompleted.setStyle(isCompleted ? "-fx-background-color: #DEF7EC; -fx-text-fill: #03543F; -fx-font-weight: 800;" : "");
        }
        if (btnDetailBillInPos != null) {
            if (isCompleted) {
                btnDetailBillInPos.setText("✓ Order Already Billed in POS");
                btnDetailBillInPos.setDisable(true);
            } else {
                btnDetailBillInPos.setText("🛒 Bill in POS (Auto-fill Customer & Medicines)");
                btnDetailBillInPos.setDisable(false);
            }
        }
    }

    @FXML
    public void handleCopyAddress(ActionEvent event) {
        if (selectedOrder != null && selectedOrder.getAddress() != null && !selectedOrder.getAddress().isEmpty()) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(selectedOrder.getAddress());
            clipboard.setContent(content);

            if (btnCopyAddress != null) {
                btnCopyAddress.setText("Copied! ✓");
                PauseTransition pt = new PauseTransition(Duration.seconds(2));
                pt.setOnFinished(e -> btnCopyAddress.setText("📋 Copy Address"));
                pt.play();
            }
        }
    }

    @FXML
    public void handleStatusApprove(ActionEvent event) {
        updateSelectedOrderStatus("approved", true);
    }

    @FXML
    public void handleStatusPacking(ActionEvent event) {
        updateSelectedOrderStatus("packing_started", true);
    }

    @FXML
    public void handleStatusShipped(ActionEvent event) {
        updateSelectedOrderStatus("shipping_started", true);
    }

    @FXML
    public void handleStatusCompleted(ActionEvent event) {
        updateSelectedOrderStatus("completed", true);
    }

    @FXML
    public void handleStatusCancel(ActionEvent event) {
        if (selectedOrder == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to cancel this order from " + selectedOrder.getCustomerName() + "?",
                ButtonType.YES, ButtonType.NO);
        alert.setHeaderText("Cancel Order");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                updateSelectedOrderStatus("cancelled", true);
            }
        });
    }

    protected void updateSelectedOrderStatus(String newStatus, boolean checkedByAdmin) {
        if (selectedOrder == null || selectedOrder.getFilePath() == null) return;

        try {
            File f = new File(selectedOrder.getFilePath());
            if (f.exists()) {
                JsonNode root = objectMapper.readTree(f);
                if (root.isObject()) {
                    ((com.fasterxml.jackson.databind.node.ObjectNode) root).put("is_checked_by_admin", checkedByAdmin);
                    ((com.fasterxml.jackson.databind.node.ObjectNode) root).put("admin_status", newStatus);
                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(f, root);
                }
            }

            selectedOrder.setCheckedByAdmin(checkedByAdmin);
            selectedOrder.setAdminStatus(newStatus);

            // Update in DB
            agentOrderDAO.updateOrderStatus(selectedOrder.getFilePath(), selectedOrder.getCustomerId(), newStatus, checkedByAdmin);

            ordersTable.refresh();
            displayOrderDetails(selectedOrder);
            applyFilter();

        } catch (Exception e) {
            logger.error("Error updating order file / database", e);
        }
    }

    @FXML
    public void handleDetailBillInPos(ActionEvent event) {
        if (selectedOrder != null) {
            openOrderInPos(event, selectedOrder);
        }
    }

    protected void openOrderInPos(ActionEvent event, AgentOrder order) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Pos.fxml"));
            Parent root = loader.load();

            PosController posController = loader.getController();
            if (posController != null) {
                posController.loadAgentOrder(order);
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            logger.error("Error navigating to POS", e);
        }
    }

    protected File resolveOrdersDirectory() {
        String userHome = System.getProperty("user.home");
        File[] candidates = new File[] {
                new File("medical-agent/data/orders"),
                new File("data/orders"),
                new File("../medical-agent/data/orders"),
                new File(userHome, "Desktop/agents/medical-agent/data/orders"),
                new File("C:/Users/BALAHARISH NATARAJAN/Desktop/agents/medical-agent/data/orders")
        };

        for (File candidate : candidates) {
            if (candidate.exists() && candidate.isDirectory()) {
                return candidate;
            }
        }
        return candidates[0];
    }

    @FXML
    protected void handleDashboard(ActionEvent event) {
        navigateTo(event, "/fxml/Dashboard.fxml");
    }

    protected void navigateTo(ActionEvent event, String fxmlPath) {
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
