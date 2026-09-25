package com.pharmacyerp.dao;

import com.pharmacyerp.database.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AccountDAO {
    
    public static class AccountRow {
        public String date;
        public String name;
        public String description;
        public double amount;
        public AccountRow(String d, String n, String desc, double a) {
            date = d; name = n; description = desc; amount = a;
        }
    }

    public static List<AccountRow> getIncomingPayments(String mode, java.time.LocalDate from, java.time.LocalDate to) {
        List<AccountRow> list = new ArrayList<>();
        String sql = "SELECT TO_CHAR(sale_date, 'DD/MM/YYYY') as date, invoice_number as name, 'Direct Sale' as description, paid_amount as amount " +
                     "FROM sales WHERE UPPER(payment_mode) = UPPER(?) AND DATE(sale_date) BETWEEN ? AND ? " +
                     "UNION ALL " +
                     "SELECT TO_CHAR(p.payment_date, 'DD/MM/YYYY') as date, s.invoice_number as name, 'Partial Payment' as description, p.amount_paid as amount " +
                     "FROM customer_payments p JOIN sales s ON p.sale_id = s.sale_id " +
                     "WHERE UPPER(p.payment_mode) = UPPER(?) AND DATE(p.payment_date) BETWEEN ? AND ? " +
                     "ORDER BY date DESC";
                     
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            java.sql.Date sqlFrom = java.sql.Date.valueOf(from);
            java.sql.Date sqlTo = java.sql.Date.valueOf(to);
            stmt.setString(1, mode); stmt.setDate(2, sqlFrom); stmt.setDate(3, sqlTo);
            stmt.setString(4, mode); stmt.setDate(5, sqlFrom); stmt.setDate(6, sqlTo);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new AccountRow(rs.getString("date"), rs.getString("name"), rs.getString("description"), rs.getDouble("amount")));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }
    
    public static List<AccountRow> getReceivables() {
        List<AccountRow> list = new ArrayList<>();
        String sql = "SELECT TO_CHAR(sale_date, 'DD/MM/YYYY') as date, invoice_number as name, 'Pending Balance' as description, balance_amount as amount " +
                     "FROM sales WHERE balance_amount > 0 ORDER BY sale_date DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new AccountRow(rs.getString("date"), rs.getString("name"), rs.getString("description"), rs.getDouble("amount")));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }
    
    public static List<AccountRow> getPayables() {
        List<AccountRow> list = new ArrayList<>();
        String sql = "SELECT TO_CHAR(p.purchase_date, 'DD/MM/YYYY') as date, s.supplier_name as name, 'Pending Balance (Inv: ' || p.invoice_number || ')' as description, p.balance_amount as amount " +
                     "FROM purchases p JOIN suppliers s ON p.supplier_id = s.supplier_id WHERE p.balance_amount > 0 ORDER BY p.purchase_date DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new AccountRow(rs.getString("date"), rs.getString("name"), rs.getString("description"), rs.getDouble("amount")));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }
}
