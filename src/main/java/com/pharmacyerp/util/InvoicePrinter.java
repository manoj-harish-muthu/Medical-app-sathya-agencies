package com.pharmacyerp.util;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pharmacyerp.model.CartItem;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvoicePrinter {

    private static Font boldFont = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static Font normalFont = new Font(Font.HELVETICA, 8, Font.NORMAL);
    private static Font smallFont = new Font(Font.HELVETICA, 7, Font.NORMAL);
    private static Font titleFont = new Font(Font.HELVETICA, 14, Font.BOLD);

    public static void printInvoice(String invoiceNo, String customerName, String doctorName, List<CartItem> items, String filePath) {
        Document document = new Document(PageSize.A4, 20, 20, 30, 30);
        try {
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            // Header Section
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{7f, 3f});

            PdfPCell companyCell = new PdfPCell();
            companyCell.setBorder(Rectangle.NO_BORDER);
            companyCell.addElement(new Paragraph("SATHYA AGENCIES", titleFont));
            companyCell.addElement(new Paragraph("6/540/7-7A, East 1st Floor,\nMeenakshi Nagar,\nVirudhunagar - 626001\nPhone : 7010573593, 9025001496\nD.L.No. : TN/TUT/20B/21/21B/00998\nGSTIN : 33JDDP50145H1ZM\nE-Mail : sathyaagenciesvnr@gmail.com", normalFont));
            headerTable.addCell(companyCell);

            PdfPCell invoiceLabelCell = new PdfPCell();
            invoiceLabelCell.setBorder(Rectangle.NO_BORDER);
            invoiceLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph gstInvoiceText = new Paragraph("GST INVOICE", new Font(Font.HELVETICA, 12, Font.BOLD));
            gstInvoiceText.setAlignment(Element.ALIGN_RIGHT);
            invoiceLabelCell.addElement(gstInvoiceText);
            headerTable.addCell(invoiceLabelCell);

            document.add(headerTable);
            document.add(new Paragraph("\n"));

            // Buyer & Invoice Details Section
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{6f, 4f});

            PdfPCell buyerCell = new PdfPCell();
            buyerCell.addElement(new Phrase("M/s " + (customerName.isEmpty() ? "CASH PATIENT" : customerName.toUpperCase()), boldFont));
            buyerCell.addElement(new Phrase("Virudhunagar\nDoctor: " + (doctorName.isEmpty() ? "Self" : doctorName), normalFont));
            infoTable.addCell(buyerCell);

            PdfPCell invoiceDetailsCell = new PdfPCell();
            
            PdfPTable innerDetails = new PdfPTable(2);
            innerDetails.setWidthPercentage(100);
            innerDetails.addCell(createNoBorderCell("Original for Buyer", normalFont));
            innerDetails.addCell(createNoBorderCell("Page No : 1/1", normalFont));
            innerDetails.addCell(createNoBorderCell("Invoice No : " + invoiceNo, normalFont));
            innerDetails.addCell(createNoBorderCell("Time : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), normalFont));
            innerDetails.addCell(createNoBorderCell("Inv.Date : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), normalFont));
            innerDetails.addCell(createNoBorderCell("Inv.Type : CREDIT", boldFont));
            
            invoiceDetailsCell.addElement(innerDetails);
            infoTable.addCell(invoiceDetailsCell);
            
            document.add(infoTable);
            
            // Gap before main table
            document.add(new Paragraph(" "));

            // Main Items Table
            PdfPTable table = new PdfPTable(14);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1f, 2f, 2.5f, 1.8f, 2.5f, 1.5f, 4.5f, 1.5f, 1.2f, 1f, 1.8f, 1.2f, 1.2f, 2f});

            String[] headers = {"S.No", "MFR", "HSN Code", "M.R.P", "BATCH", "EXP", "ITEM DESCRIPTION", "PACK", "QTY", "F/R", "RATE", "Dis%", "GST%", "Amount"};
            for (String header : headers) {
                PdfPCell hCell = new PdfPCell(new Phrase(header, boldFont));
                hCell.setBackgroundColor(new java.awt.Color(230, 230, 230));
                hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(hCell);
            }

            BigDecimal totalAmount = BigDecimal.ZERO;
            int sno = 1;
            
            // Tax accumulation map (GST Rate -> Taxable Amount)
            Map<BigDecimal, BigDecimal> taxSalesValueMap = new HashMap<>();
            Map<BigDecimal, BigDecimal> taxDiscValueMap = new HashMap<>();

            for (CartItem item : items) {
                table.addCell(createCenteredCell(String.valueOf(sno++), normalFont));
                table.addCell(createCell(item.getCompanyName(), smallFont));
                table.addCell(createCenteredCell(item.getHsnCode(), smallFont));
                table.addCell(createRightCell(String.format("%.2f", item.getMrp()), normalFont));
                table.addCell(createCenteredCell(item.getBatchNumber(), normalFont));
                table.addCell(createCenteredCell(item.getExpiryDateStr(), normalFont));
                table.addCell(createCell(item.getMedicineName(), normalFont));
                table.addCell(createCenteredCell(item.getPacking(), normalFont));
                table.addCell(createCenteredCell(String.valueOf(item.getQuantity()), normalFont));
                table.addCell(createCenteredCell("-", normalFont)); // F/R
                table.addCell(createRightCell(String.format("%.2f", item.getSellingRate()), normalFont));
                table.addCell(createRightCell(String.format("%.2f", item.getDiscountPercentage()), normalFont));
                table.addCell(createRightCell(String.format("%.2f", item.getGstRate()), normalFont));
                table.addCell(createRightCell(String.format("%.2f", item.getNetAmount()), normalFont));
                
                totalAmount = totalAmount.add(item.getNetAmount());
                
                // Group tax calculations
                BigDecimal rate = item.getGstRate();
                BigDecimal salesVal = item.getSellingRate().multiply(new BigDecimal(item.getQuantity()));
                
                taxSalesValueMap.put(rate, taxSalesValueMap.getOrDefault(rate, BigDecimal.ZERO).add(salesVal));
                taxDiscValueMap.put(rate, taxDiscValueMap.getOrDefault(rate, BigDecimal.ZERO).add(item.getDiscountAmount()));
            }

            document.add(table);
            document.add(new Paragraph(" "));

            // Tax Summary Grid
            PdfPTable taxTable = new PdfPTable(6);
            taxTable.setWidthPercentage(100);
            
            String[] taxHeaders = {"GST %", "Sales Value", "Discount Value", "Taxable Value", "SGST Value", "CGST Value"};
            for (String header : taxHeaders) {
                PdfPCell hCell = new PdfPCell(new Phrase(header, boldFont));
                hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                taxTable.addCell(hCell);
            }
            
            BigDecimal[] standardRates = {new BigDecimal("0.0"), new BigDecimal("5.0"), new BigDecimal("12.0"), new BigDecimal("18.0"), new BigDecimal("28.0")};
            
            BigDecimal grandSales = BigDecimal.ZERO;
            BigDecimal grandDisc = BigDecimal.ZERO;
            BigDecimal grandTaxable = BigDecimal.ZERO;
            BigDecimal grandSgst = BigDecimal.ZERO;
            BigDecimal grandCgst = BigDecimal.ZERO;
            
            for (BigDecimal r : standardRates) {
                BigDecimal sVal = taxSalesValueMap.getOrDefault(r, BigDecimal.ZERO);
                BigDecimal dVal = taxDiscValueMap.getOrDefault(r, BigDecimal.ZERO);
                BigDecimal tVal = sVal.subtract(dVal);
                BigDecimal halfTax = tVal.multiply(r).divide(new BigDecimal(200), 2, RoundingMode.HALF_UP); // 200 because half of rate %
                
                if (sVal.compareTo(BigDecimal.ZERO) > 0 || r.compareTo(BigDecimal.ZERO) == 0) { // always show 0% row or others if they have value
                    taxTable.addCell(createCenteredCell(String.format("GST %.0f%%", r), normalFont));
                    taxTable.addCell(createRightCell(sVal.compareTo(BigDecimal.ZERO) == 0 ? "-" : String.format("%.2f", sVal), normalFont));
                    taxTable.addCell(createRightCell(sVal.compareTo(BigDecimal.ZERO) == 0 ? "-" : String.format("%.2f", dVal), normalFont));
                    taxTable.addCell(createRightCell(sVal.compareTo(BigDecimal.ZERO) == 0 ? "-" : String.format("%.2f", tVal), normalFont));
                    taxTable.addCell(createRightCell(sVal.compareTo(BigDecimal.ZERO) == 0 ? "-" : String.format("%.2f", halfTax), normalFont));
                    taxTable.addCell(createRightCell(sVal.compareTo(BigDecimal.ZERO) == 0 ? "-" : String.format("%.2f", halfTax), normalFont));
                    
                    grandSales = grandSales.add(sVal);
                    grandDisc = grandDisc.add(dVal);
                    grandTaxable = grandTaxable.add(tVal);
                    grandSgst = grandSgst.add(halfTax);
                    grandCgst = grandCgst.add(halfTax);
                }
            }
            
            // Sub Total Row in Tax Table
            taxTable.addCell(createCenteredCell("Sub Total", boldFont));
            taxTable.addCell(createRightCell(String.format("%.2f", grandSales), boldFont));
            taxTable.addCell(createRightCell(String.format("%.2f", grandDisc), boldFont));
            taxTable.addCell(createRightCell(String.format("%.2f", grandTaxable), boldFont));
            taxTable.addCell(createRightCell(String.format("%.2f", grandSgst), boldFont));
            taxTable.addCell(createRightCell(String.format("%.2f", grandCgst), boldFont));
            
            document.add(taxTable);

            // Grand Total block
            document.add(new Paragraph(" "));
            
            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(100);
            totalTable.setWidths(new float[]{7f, 3f});
            
            // Terms and Conditions Cell
            PdfPCell termsCell = new PdfPCell();
            termsCell.addElement(new Phrase("Bank Details", boldFont));
            termsCell.addElement(new Phrase("Bank Name : BANK OF BARODA\nA/C No : 41530200000154\nIFSC Code : BARB0ROSALP\n\n", smallFont));
            termsCell.addElement(new Phrase("Terms and Conditions", boldFont));
            termsCell.addElement(new Phrase("1. Goods once sold will not be taken back or exchanged.\n2. Bills not paid due date will attract 24% interest.\n3. All disputes subject to Jurisdiction only.", smallFont));
            totalTable.addCell(termsCell);
            
            // Totals Cell
            PdfPCell totalsCell = new PdfPCell();
            
            PdfPTable tInner = new PdfPTable(2);
            tInner.setWidthPercentage(100);
            
            BigDecimal roundOff = totalAmount.setScale(0, RoundingMode.HALF_UP).subtract(totalAmount);
            BigDecimal finalAmount = totalAmount.add(roundOff);
            
            tInner.addCell(createNoBorderCell("Round Off", normalFont));
            tInner.addCell(createNoBorderRightCell(String.format("%.2f", roundOff), normalFont));
            
            PdfPCell grandTotalLbl = new PdfPCell(new Phrase("Grand Total", titleFont));
            grandTotalLbl.setBorder(Rectangle.NO_BORDER);
            tInner.addCell(grandTotalLbl);
            
            PdfPCell grandTotalVal = new PdfPCell(new Phrase(String.format("%.2f", finalAmount), titleFont));
            grandTotalVal.setBorder(Rectangle.NO_BORDER);
            grandTotalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tInner.addCell(grandTotalVal);
            
            totalsCell.addElement(tInner);
            
            // Signature space
            totalsCell.addElement(new Paragraph("\n\nFor SATHYA AGENCIES\n\n\nAuthorised Signatory", boldFont));
            
            totalTable.addCell(totalsCell);
            document.add(totalTable);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }
    
    private static PdfPCell createCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        return cell;
    }
    
    private static PdfPCell createCenteredCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }
    
    private static PdfPCell createRightCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    private static PdfPCell createNoBorderCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }
    
    private static PdfPCell createNoBorderRightCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }
}
