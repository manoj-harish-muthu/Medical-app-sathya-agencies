package com.pharmacyerp.dao;

import com.pharmacyerp.database.DatabaseManager;
import com.pharmacyerp.model.Manufacturer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ManufacturerDAO {
    private static final Logger logger = LoggerFactory.getLogger(ManufacturerDAO.class);

    public List<Manufacturer> getAllManufacturers() {
        List<Manufacturer> list = new ArrayList<>();
        String sql = "SELECT * FROM medicine_companies ORDER BY company_name ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Manufacturer m = new Manufacturer();
                m.setCompanyId(rs.getInt("company_id"));
                m.setCompanyName(rs.getString("company_name"));
                m.setContactPerson(rs.getString("contact_person"));
                m.setPhone(rs.getString("phone"));
                m.setEmail(rs.getString("email"));
                list.add(m);
            }
        } catch (Exception e) {
            logger.error("Error fetching manufacturers", e);
        }
        return list;
    }

    public boolean addManufacturer(Manufacturer m) {
        String sql = "INSERT INTO medicine_companies (company_name, contact_person, phone, email) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, m.getCompanyName());
            stmt.setString(2, m.getContactPerson());
            stmt.setString(3, m.getPhone());
            stmt.setString(4, m.getEmail());
            
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Error adding manufacturer", e);
            return false;
        }
    }

    public boolean updateManufacturer(Manufacturer m) {
        String sql = "UPDATE medicine_companies SET company_name=?, contact_person=?, phone=?, email=? WHERE company_id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, m.getCompanyName());
            stmt.setString(2, m.getContactPerson());
            stmt.setString(3, m.getPhone());
            stmt.setString(4, m.getEmail());
            stmt.setInt(5, m.getCompanyId());
            
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Error updating manufacturer", e);
            return false;
        }
    }
}
