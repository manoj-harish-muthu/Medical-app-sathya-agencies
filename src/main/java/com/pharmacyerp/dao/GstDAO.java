package com.pharmacyerp.dao;

import com.pharmacyerp.database.DatabaseManager;
import com.pharmacyerp.model.GstSummaryRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class GstDAO {
    private static final Logger logger = LoggerFactory.getLogger(GstDAO.class);

    public List<GstSummaryRow> getMonthlySalesTaxSummary(String month, String year) {
        List<GstSummaryRow> rows = new ArrayList<>();
        
        // Ensure month is 2 digits for SQLite strftime
        if (month.length() == 1) {
            month = "0" + month;
        }

        String sql = "SELECT " +
                     "CAST(COALESCE(NULLIF(m.cgst + m.sgst, 0), m.gst_rate, 0) AS INTEGER) as tax_rate_int, " +
                     "SUM(si.net_amount - COALESCE(si.cgst_amount,0) - COALESCE(si.sgst_amount,0)) as taxable_value, " +
                     "SUM(COALESCE(si.cgst_amount,0)) as total_cgst, " +
                     "SUM(COALESCE(si.sgst_amount,0)) as total_sgst, " +
                     "SUM(si.net_amount) as total_amount " +
                     "FROM sales s " +
                     "JOIN sale_items si ON s.sale_id = si.sale_id " +
                     "JOIN medicines m ON si.medicine_id = m.medicine_id " +
                     "WHERE s.status != 'QUOTATION' " +
                     "AND TO_CHAR(s.sale_date, 'MM') = ? " +
                     "AND TO_CHAR(s.sale_date, 'YYYY') = ? " +
                     "GROUP BY tax_rate_int " +
                     "ORDER BY tax_rate_int ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setString(1, month);
            stmt.setString(2, year);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int rate = rs.getInt("tax_rate_int");
                    String rateStr = rate + "%";
                    BigDecimal taxable = rs.getBigDecimal("taxable_value");
                    BigDecimal cgst = rs.getBigDecimal("total_cgst");
                    BigDecimal sgst = rs.getBigDecimal("total_sgst");
                    BigDecimal total = rs.getBigDecimal("total_amount");
                    
                    rows.add(new GstSummaryRow(rateStr, 
                            taxable != null ? taxable : BigDecimal.ZERO, 
                            cgst != null ? cgst : BigDecimal.ZERO, 
                            sgst != null ? sgst : BigDecimal.ZERO, 
                            total != null ? total : BigDecimal.ZERO));
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching sales GST summary", e);
        }
        return rows;
    }

    public List<GstSummaryRow> getMonthlyPurchaseTaxSummary(String month, String year) {
        List<GstSummaryRow> rows = new ArrayList<>();
        
        if (month.length() == 1) {
            month = "0" + month;
        }

        String sql = "SELECT " +
                     "CAST(COALESCE(pi.tax_percentage, 0) AS INTEGER) as tax_rate_int, " +
                     "SUM(pi.net_amount - COALESCE(pi.tax_amount,0)) as taxable_value, " +
                     "SUM(COALESCE(pi.tax_amount,0) / 2.0) as total_cgst, " +
                     "SUM(COALESCE(pi.tax_amount,0) / 2.0) as total_sgst, " +
                     "SUM(pi.net_amount) as total_amount " +
                     "FROM purchases p " +
                     "JOIN purchase_items pi ON p.purchase_id = pi.purchase_id " +
                     "WHERE TO_CHAR(p.purchase_date, 'MM') = ? " +
                     "AND TO_CHAR(p.purchase_date, 'YYYY') = ? " +
                     "GROUP BY tax_rate_int " +
                     "ORDER BY tax_rate_int ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setString(1, month);
            stmt.setString(2, year);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int rate = rs.getInt("tax_rate_int");
                    String rateStr = rate + "%";
                    BigDecimal taxable = rs.getBigDecimal("taxable_value");
                    BigDecimal cgst = rs.getBigDecimal("total_cgst");
                    BigDecimal sgst = rs.getBigDecimal("total_sgst");
                    BigDecimal total = rs.getBigDecimal("total_amount");
                    
                    rows.add(new GstSummaryRow(rateStr, 
                            taxable != null ? taxable : BigDecimal.ZERO, 
                            cgst != null ? cgst : BigDecimal.ZERO, 
                            sgst != null ? sgst : BigDecimal.ZERO, 
                            total != null ? total : BigDecimal.ZERO));
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching purchase GST summary", e);
        }
        return rows;
    }
}
