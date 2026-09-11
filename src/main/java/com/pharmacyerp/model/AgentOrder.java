package com.pharmacyerp.model;

import java.util.ArrayList;
import java.util.List;

public class AgentOrder {
    private Integer id;
    private String customerId;
    private String customerName;
    private String phone;
    private String address;
    private boolean confirmed;
    private boolean isCheckedByAdmin;
    private String adminStatus = "pending";
    private String orderSource = "WhatsApp AI Agent";
    private String orderDate = "";
    private List<OrderItem> items = new ArrayList<>();
    private String filePath;

    public static class OrderItem {
        private Integer itemId;
        private String medicine;
        private String quantity;
        private String unit;
        private String volumeMl;
        private Integer piecesPerUnit;

        public OrderItem() {}

        public OrderItem(String medicine, String quantity, String unit) {
            this(medicine, quantity, unit, null, null);
        }

        public OrderItem(String medicine, String quantity, String unit, String volumeMl, Integer piecesPerUnit) {
            this.medicine = medicine;
            this.quantity = quantity;
            this.unit = unit;
            this.volumeMl = volumeMl;
            this.piecesPerUnit = piecesPerUnit;
        }

        public Integer getItemId() { return itemId; }
        public void setItemId(Integer itemId) { this.itemId = itemId; }

        public String getMedicine() { return medicine != null ? medicine : ""; }
        public void setMedicine(String medicine) { this.medicine = medicine; }

        public String getQuantity() { return quantity != null ? quantity : "1"; }
        public void setQuantity(String quantity) { this.quantity = quantity; }

        public String getUnit() { return unit != null ? unit : ""; }
        public void setUnit(String unit) { this.unit = unit; }

        public String getVolumeMl() { return volumeMl != null ? volumeMl : "-"; }
        public void setVolumeMl(String volumeMl) { this.volumeMl = volumeMl; }

        public Integer getPiecesPerUnit() { return piecesPerUnit; }
        public void setPiecesPerUnit(Integer piecesPerUnit) { this.piecesPerUnit = piecesPerUnit; }

        public String getPiecesDisplay() {
            return (piecesPerUnit != null && piecesPerUnit > 0) ? String.valueOf(piecesPerUnit) : "-";
        }

        public String getDisplayText() {
            StringBuilder sb = new StringBuilder();
            sb.append(getMedicine()).append(" (").append(getQuantity());
            if (unit != null && !unit.isBlank()) {
                sb.append(" ").append(unit);
            }
            if (volumeMl != null && !volumeMl.isBlank() && !"-".equals(volumeMl)) {
                sb.append(" • ").append(volumeMl);
            }
            if (piecesPerUnit != null && piecesPerUnit > 0) {
                sb.append(" • ").append(piecesPerUnit).append(" pcs/pack");
            }
            sb.append(")");
            return sb.toString();
        }
    }

    public AgentOrder() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCustomerId() { return customerId != null ? customerId : ""; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName != null ? customerName : "Walk-in / Unknown"; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getPhone() { return phone != null ? phone : ""; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address != null ? address : "No address provided"; }
    public void setAddress(String address) { this.address = address; }

    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }

    public boolean isCheckedByAdmin() { return isCheckedByAdmin; }
    public void setCheckedByAdmin(boolean checkedByAdmin) { isCheckedByAdmin = checkedByAdmin; }

    public String getAdminStatus() { return adminStatus != null ? adminStatus : "pending"; }
    public void setAdminStatus(String adminStatus) { this.adminStatus = adminStatus; }

    public String getOrderSource() { return orderSource != null ? orderSource : "WhatsApp AI Agent"; }
    public void setOrderSource(String orderSource) { this.orderSource = orderSource; }

    public String getOrderDate() { return orderDate != null ? orderDate : ""; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items != null ? items : new ArrayList<>(); }

    public int getItemsCount() {
        return items != null ? items.size() : 0;
    }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getItemsSummary() {
        if (items == null || items.isEmpty()) return "No items";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            OrderItem it = items.get(i);
            sb.append(it.getMedicine()).append(" (").append(it.getQuantity());
            if (it.getUnit() != null && !it.getUnit().isEmpty()) {
                sb.append(" ").append(it.getUnit());
            }
            if (it.getVolumeMl() != null && !"-".equals(it.getVolumeMl()) && !it.getVolumeMl().isBlank()) {
                sb.append(" ").append(it.getVolumeMl());
            }
            sb.append(")");
            if (i < items.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    public String getStatusDisplay() {
        if (!confirmed) {
            return "Draft / In Progress";
        }
        String status = getAdminStatus().toLowerCase().trim();
        switch (status) {
            case "approved":
                return "Approved";
            case "packing_started":
            case "packing started":
            case "packed":
                return "Packing Started";
            case "shipping_started":
            case "shipping started":
            case "shipped":
                return "Shipped / Out for Delivery";
            case "completed":
            case "delivered":
                return "Completed";
            case "cancelled":
                return "Cancelled";
            default:
                return "Confirmed (Pending Review)";
        }
    }

    public String getPincode() {
        if (address == null) return "";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b([1-9][0-9]{5})\\b").matcher(address);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }
}
