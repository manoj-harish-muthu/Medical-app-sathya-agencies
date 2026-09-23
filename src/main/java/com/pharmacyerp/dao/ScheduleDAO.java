package com.pharmacyerp.dao;

import com.pharmacyerp.database.DatabaseManager;
import com.pharmacyerp.model.Schedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ScheduleDAO {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleDAO.class);

    public List<Schedule> getAllSchedules() {
        List<Schedule> list = new ArrayList<>();
        String sql = "SELECT * FROM medicine_schedules ORDER BY schedule_name ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Schedule s = new Schedule();
                s.setScheduleId(rs.getInt("schedule_id"));
                s.setScheduleName(rs.getString("schedule_name"));
                s.setRequiresPrescription(rs.getBoolean("requires_prescription"));
                s.setDescription(rs.getString("description"));
                list.add(s);
            }
        } catch (Exception e) {
            logger.error("Error fetching schedules", e);
        }
        return list;
    }

    public boolean addSchedule(Schedule s) {
        String sql = "INSERT INTO medicine_schedules (schedule_name, requires_prescription, description) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, s.getScheduleName());
            stmt.setBoolean(2, s.isRequiresPrescription());
            stmt.setString(3, s.getDescription());
            
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Error adding schedule", e);
            return false;
        }
    }

    public boolean updateSchedule(Schedule s) {
        String sql = "UPDATE medicine_schedules SET schedule_name=?, requires_prescription=?, description=? WHERE schedule_id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, s.getScheduleName());
            stmt.setBoolean(2, s.isRequiresPrescription());
            stmt.setString(3, s.getDescription());
            stmt.setInt(4, s.getScheduleId());
            
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Error updating schedule", e);
            return false;
        }
    }
}
