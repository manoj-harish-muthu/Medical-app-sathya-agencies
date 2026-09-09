package com.pharmacyerp;

import com.pharmacyerp.dao.PosDAO;
import com.pharmacyerp.database.DatabaseManager;
import com.pharmacyerp.model.CartItem;
import com.pharmacyerp.util.InvoicePrinter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PosBackendTest {

    private static PosDAO posDAO;

    @BeforeAll
    public static void setUp() {
        DatabaseManager.initialize();
        posDAO = new PosDAO();
    }

    @Test
    public void testSearchMedicines() {
        List<CartItem> items = posDAO.searchMedicines("Paracetamol");
        assertNotNull(items, "Items list should not be null");
        assertFalse(items.isEmpty(), "Should find at least 1 medicine matching Paracetamol");

        CartItem item = items.get(0);
        assertTrue(item.getMedicineName().toLowerCase().contains("paracetamol"));
        assertNotNull(item.getBatchNumber());
        assertTrue(item.getSellingRate().compareTo(BigDecimal.ZERO) > 0);
        System.out.println("Found item: " + item.getMedicineName() + " Batch: " + item.getBatchNumber() + " Rate: " + item.getSellingRate() + " GST: " + item.getGstRate());
    }

    @Test
    public void testGetItemByBarcodeOrName() {
        CartItem item = posDAO.getItemByBarcodeOrName("BAT-P-001");
        assertNotNull(item, "Should find medicine by batch BAT-P-001");
        assertEquals("BAT-P-001", item.getBatchNumber());
        assertEquals("Paracetamol 500mg", item.getMedicineName());
    }

    @Test
    public void testSaveSaleAndGenerateInvoice() {
        CartItem item = posDAO.getItemByBarcodeOrName("BAT-P-001");
        assertNotNull(item);
        item.setQuantity(2);
        item.calculateTotals();

        String testInvoice = "TEST-INV-" + System.currentTimeMillis();
        boolean saved = posDAO.saveSale(
                testInvoice,
                "Test Customer",
                "9876543210",
                "Dr. Test",
                "Cash",
                Collections.singletonList(item),
                item.getNetAmount(),
                BigDecimal.ZERO
        );

        assertTrue(saved, "Sale should be saved to database successfully");

        // Verify PDF invoice generation
        String testPdf = "target/test_invoice_" + testInvoice + ".pdf";
        InvoicePrinter.printInvoice(testInvoice, "Test Customer", "Dr. Test", Collections.singletonList(item), testPdf);
        File f = new File(testPdf);
        assertTrue(f.exists(), "Invoice PDF file should be generated");
        assertTrue(f.length() > 0, "Invoice PDF should not be empty");
        System.out.println("Test invoice generated at " + f.getAbsolutePath() + " size=" + f.length());
    }
}
