package com.pharmacyerp.database;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConnectionTest {

    @Test
    public void testAllBackendDaos() {
        assertDoesNotThrow(() -> {
            DatabaseManager.initialize();
        });

        // 1. Check Tables
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW TABLES")) {
            System.out.println("=== TABLES IN DB ===");
            while (rs.next()) {
                System.out.println("  Table: " + rs.getString(1));
            }
        } catch (Exception e) {
            System.err.println("SHOW TABLES failed: " + e.getMessage());
        }

        // 2. Test SalesDAO
        try {
            com.pharmacyerp.dao.SalesDAO salesDAO = new com.pharmacyerp.dao.SalesDAO();
            java.util.List<com.pharmacyerp.model.Sale> sales = salesDAO.getAllSales();
            System.out.println("SalesDAO.getAllSales() count: " + sales.size());
            if (!sales.isEmpty()) {
                int sId = sales.get(0).getSaleId();
                java.util.List<com.pharmacyerp.model.CartItem> saleItems = salesDAO.getSaleItemsBySaleId(sId);
                System.out.println("SalesDAO.getSaleItemsBySaleId(" + sId + ") count: " + saleItems.size());
            }
        } catch (Exception e) {
            System.err.println("SalesDAO.getAllSales() FAILED: " + e.getMessage());
            e.printStackTrace();
        }

        // 3. Test DashboardDAO
        try {
            com.pharmacyerp.dao.DashboardDAO dashboardDAO = new com.pharmacyerp.dao.DashboardDAO();
            System.out.println("DashboardDAO.getTodaysBillsCount: " + dashboardDAO.getTodaysBillsCount());
            System.out.println("DashboardDAO.getTodaysSalesTotal: " + dashboardDAO.getTodaysSalesTotal());
            System.out.println("DashboardDAO.getLowStockCount: " + dashboardDAO.getLowStockCount());
            System.out.println("DashboardDAO.getExpiringSoonCount: " + dashboardDAO.getExpiringSoonCount());
            System.out.println("DashboardDAO.getAlertMessages count: " + dashboardDAO.getAlertMessages().size());
            System.out.println("DashboardDAO.getSalesTrend: " + dashboardDAO.getSalesTrend(7));
        } catch (Exception e) {
            System.err.println("DashboardDAO FAILED: " + e.getMessage());
            e.printStackTrace();
        }

        // 4. Test PosDAO
        try {
            com.pharmacyerp.dao.PosDAO posDAO = new com.pharmacyerp.dao.PosDAO();
            java.util.List<com.pharmacyerp.model.CartItem> items = posDAO.searchMedicines("Paracetamol");
            System.out.println("PosDAO.searchMedicines('Paracetamol') count: " + items.size());
            java.util.List<com.pharmacyerp.model.CartItem> itemsDolo = posDAO.searchMedicines("Dolo");
            System.out.println("PosDAO.searchMedicines('Dolo') count: " + itemsDolo.size());
        } catch (Exception e) {
            System.err.println("PosDAO FAILED: " + e.getMessage());
            e.printStackTrace();
        }

        // 5. Test MedicineRepository
        try {
            com.pharmacyerp.repository.MedicineRepository medRepo = new com.pharmacyerp.repository.MedicineRepository();
            java.util.List<com.pharmacyerp.model.Medicine> meds = medRepo.getAllActiveMedicines();
            System.out.println("MedicineRepository.getAllActiveMedicines() count: " + meds.size());
            for (com.pharmacyerp.model.Medicine m : meds) {
                System.out.println("  Med: " + m.getMedicineName() + " | Stock: " + m.getTotalStock() + " | MRP: " + m.getCurrentMrp());
            }
        } catch (Exception e) {
            System.err.println("MedicineRepository FAILED: " + e.getMessage());
            e.printStackTrace();
        }

        // 6. Test CategoryDAO, ManufacturerDAO, InventoryDAO, OutstandingDAO
        try {
            com.pharmacyerp.dao.CategoryDAO catDAO = new com.pharmacyerp.dao.CategoryDAO();
            System.out.println("CategoryDAO.getAllCategories() count: " + catDAO.getAllCategories().size());
            
            com.pharmacyerp.dao.ManufacturerDAO mfrDAO = new com.pharmacyerp.dao.ManufacturerDAO();
            System.out.println("ManufacturerDAO.getAllManufacturers() count: " + mfrDAO.getAllManufacturers().size());

            com.pharmacyerp.dao.InventoryDAO invDAO = new com.pharmacyerp.dao.InventoryDAO();
            System.out.println("InventoryDAO.getAllInventory() count: " + invDAO.getAllInventory().size());
            System.out.println("InventoryDAO.getNearExpiryStock() count: " + invDAO.getNearExpiryStock().size());
            System.out.println("InventoryDAO.getExpiredStock() count: " + invDAO.getExpiredStock().size());
            System.out.println("InventoryDAO.getZeroOrLowStock() count: " + invDAO.getZeroOrLowStock().size());

            com.pharmacyerp.dao.OutstandingDAO outDAO = new com.pharmacyerp.dao.OutstandingDAO();
            System.out.println("OutstandingDAO.getOutstandingBills() count: " + outDAO.getOutstandingBills().size());
        } catch (Exception e) {
            System.err.println("Other DAOs FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
