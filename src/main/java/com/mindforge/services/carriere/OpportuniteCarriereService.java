package com.mindforge.services.carriere;

import com.mindforge.entities.carriere.OpportuniteCarriere;
import com.mindforge.interfaces.IService;
import com.mindforge.utils.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OpportuniteCarriereService implements IService<OpportuniteCarriere> {

    private Connection cnx;

    public OpportuniteCarriereService() {
        try {
            cnx = DBConnection.getConnection();
        } catch (SQLException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    @Override
    public void addEntity(OpportuniteCarriere o) {
        String query = "INSERT INTO opportunite_carriere (title, description, type, location, duration, deadline, status, created_at, company_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, o.getTitle());
            pst.setString(2, o.getDescription());
            pst.setString(3, o.getType());
            pst.setString(4, o.getLocation());
            pst.setString(5, o.getDuration());
            pst.setDate(6, o.getDeadline() != null ? Date.valueOf(o.getDeadline()) : null);
            pst.setString(7, o.getStatus());
            pst.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            pst.setInt(9, o.getCompanyId());
            pst.executeUpdate();
            try (ResultSet keys = pst.getGeneratedKeys()) {
                if (keys.next()) o.setId(keys.getInt(1));
            }
            System.out.println("OpportuniteCarriere added successfully.");
        } catch (SQLException ex) {
            System.out.println("Error adding opportunite: " + ex.getMessage());
        }
    }

    @Override
    public void deleteEntity(OpportuniteCarriere o) {
        String query = "DELETE FROM opportunite_carriere WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, o.getId());
            pst.executeUpdate();
            System.out.println("OpportuniteCarriere deleted successfully.");
        } catch (SQLException ex) {
            System.out.println("Error deleting opportunite: " + ex.getMessage());
        }
    }

    @Override
    public void updateEntity(int id, OpportuniteCarriere o) {
        String query = "UPDATE opportunite_carriere SET title=?, description=?, type=?, location=?, duration=?, deadline=?, status=?, company_id=? WHERE id=?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setString(1, o.getTitle());
            pst.setString(2, o.getDescription());
            pst.setString(3, o.getType());
            pst.setString(4, o.getLocation());
            pst.setString(5, o.getDuration());
            pst.setDate(6, o.getDeadline() != null ? Date.valueOf(o.getDeadline()) : null);
            pst.setString(7, o.getStatus());
            pst.setInt(8, o.getCompanyId());
            pst.setInt(9, id);
            pst.executeUpdate();
            System.out.println("OpportuniteCarriere updated successfully.");
        } catch (SQLException ex) {
            System.out.println("Error updating opportunite: " + ex.getMessage());
        }
    }

    @Override
    public List<OpportuniteCarriere> getData() {
        List<OpportuniteCarriere> list = new ArrayList<>();
        String query = "SELECT o.*, e.name AS company_name FROM opportunite_carriere o LEFT JOIN entreprise e ON o.company_id = e.id";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            System.out.println("Error fetching opportunites: " + ex.getMessage());
        }
        return list;
    }

    public List<OpportuniteCarriere> getByCompanyId(int companyId) {
        List<OpportuniteCarriere> list = new ArrayList<>();
        String query = "SELECT o.*, e.name AS company_name FROM opportunite_carriere o " +
                       "LEFT JOIN entreprise e ON o.company_id = e.id " +
                       "WHERE o.company_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, companyId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            System.out.println("Error fetching opportunites by company: " + ex.getMessage());
        }
        return list;
    }

    private OpportuniteCarriere mapRow(ResultSet rs) throws SQLException {
        OpportuniteCarriere o = new OpportuniteCarriere();
        o.setId(rs.getInt("id"));
        o.setTitle(rs.getString("title"));
        o.setDescription(rs.getString("description"));
        o.setType(rs.getString("type"));
        o.setLocation(rs.getString("location"));
        o.setDuration(rs.getString("duration"));
        Date d = rs.getDate("deadline");
        if (d != null) o.setDeadline(d.toLocalDate());
        o.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) o.setCreatedAt(ts.toLocalDateTime());
        o.setCompanyId(rs.getInt("company_id"));
        String compName = rs.getString("company_name");
        o.setCompanyName(compName != null ? compName : "—");
        return o;
    }
}
