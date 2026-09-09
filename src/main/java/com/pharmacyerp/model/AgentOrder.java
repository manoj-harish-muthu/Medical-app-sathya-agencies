package com.pharmacyerp.model;

import java.util.ArrayList;
import java.util.List;

public class AgentOrder {
    private String customerId;
    private String customerName;
    private String phone;
    private String address;
    private boolean confirmed;
    private boolean isCheckedByAdmin;
    private String adminStatus;
    private List<OrderItem> items = new ArrayList<>();
    private String filePath;

    public static class OrderItem {
        private String medicine;
        private String quantity;
        private String unit;

        public OrderItem() {}

        public OrderItem(String medicine, String quantity, String unit) {
            this.medicine = medicine;
            this.quantity = quantity;
            this.unit = unit;
        }

        public String getMedicine() { return medicine; }
        public void setMedicine(String medicine) { this.medicine = medicine; }

        public String getQuantity() { return quantity; }
        public void setQuantity(String quantity) { this.quantity = quantity; }

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
    }

    public AgentOrder() {}

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }

    public boolean isCheckedByAdmin() { return isCheckedByAdmin; }
    public void setCheckedByAdmin(boolean checkedByAdmin) { isCheckedByAdmin = checkedByAdmin; }

    public String getAdminStatus() { return adminStatus; }
    public void setAdminStatus(String adminStatus) { this.adminStatus = adminStatus; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

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
            sb.append(")");
            if (i < items.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    public String getStatusDisplay() {
        if (confirmed) {
            return "Confirmed (" + (adminStatus != null ? adminStatus : "pending") + ")";
        }
        return "Draft / In Progress";
    }
}
