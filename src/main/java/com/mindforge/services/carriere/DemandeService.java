package com.mindforge.services.carriere;

import com.mindforge.entities.carriere.Demande;
import com.mindforge.interfaces.IService;
import com.mindforge.utils.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DemandeService implements IService<Demande> {

    private Connection cnx;

    public DemandeService() {
        try {
            cnx = DBConnection.getConnection();
        } catch (SQLException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    @Override
    public void addEntity(Demande d) {
        String query = "INSERT INTO demande (user_id, cover_letter, status, applied_at, opportunity_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, d.getUserId());
            pst.setString(2, d.getCoverLetter());
            pst.setString(3, d.getStatus() != null ? d.getStatus() : "pending");
            pst.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            pst.setInt(5, d.getOpportunityId());
            pst.executeUpdate();
            System.out.println("Demande added successfully.");
        } catch (SQLException ex) {
            System.out.println("Error adding demande: " + ex.getMessage());
        }
    }

    @Override
    public void deleteEntity(Demande d) {
        String query = "DELETE FROM demande WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, d.getId());
            pst.executeUpdate();
            System.out.println("Demande deleted successfully.");
        } catch (SQLException ex) {
            System.out.println("Error deleting demande: " + ex.getMessage());
        }
    }

    @Override
    public void updateEntity(int id, Demande d) {
        String query = "UPDATE demande SET cover_letter=?, status=?, opportunity_id=? WHERE id=?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setString(1, d.getCoverLetter());
            pst.setString(2, d.getStatus());
            pst.setInt(3, d.getOpportunityId());
            pst.setInt(4, id);
            pst.executeUpdate();
            System.out.println("Demande updated successfully.");
        } catch (SQLException ex) {
            System.out.println("Error updating demande: " + ex.getMessage());
        }
    }

    @Override
    public List<Demande> getData() {
        List<Demande> list = new ArrayList<>();
        String query = "SELECT d.*, o.title AS opportunity_title FROM demande d LEFT JOIN opportunite_carriere o ON d.opportunity_id = o.id";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            System.out.println("Error fetching demandes: " + ex.getMessage());
        }
        return list;
    }

    public List<Demande> getByUserId(int userId) {
        List<Demande> list = new ArrayList<>();
        String query = "SELECT d.*, o.title AS opportunity_title FROM demande d " +
                       "LEFT JOIN opportunite_carriere o ON d.opportunity_id = o.id " +
                       "WHERE d.user_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            System.out.println("Error fetching demandes by user: " + ex.getMessage());
        }
        return list;
    }

    public List<Demande> getByOpportunityId(int opportunityId) {
        List<Demande> list = new ArrayList<>();
        String query = "SELECT d.*, o.title AS opportunity_title FROM demande d " +
                       "LEFT JOIN opportunite_carriere o ON d.opportunity_id = o.id " +
                       "WHERE d.opportunity_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, opportunityId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            System.out.println("Error fetching demandes by opportunity: " + ex.getMessage());
        }
        return list;
    }

    public void updateStatus(int id, String status) {
        String query = "UPDATE demande SET status = ? WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setString(1, status);
            pst.setInt(2, id);
            pst.executeUpdate();
            System.out.println("Demande status updated to: " + status);
        } catch (SQLException ex) {
            System.out.println("Error updating demande status: " + ex.getMessage());
        }
    }

    public boolean hasApplied(int userId, int opportunityId) {
        String query = "SELECT COUNT(*) FROM demande WHERE user_id = ? AND opportunity_id = ? AND status <> 'withdrawn'";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, opportunityId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException ex) {
            System.out.println("Error checking application: " + ex.getMessage());
        }
        return false;
    }

    private Demande mapRow(ResultSet rs) throws SQLException {
        Demande d = new Demande();
        d.setId(rs.getInt("id"));
        d.setUserId(rs.getInt("user_id"));
        d.setCoverLetter(rs.getString("cover_letter"));
        d.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("applied_at");
        if (ts != null) d.setAppliedAt(ts.toLocalDateTime());
        d.setOpportunityId(rs.getInt("opportunity_id"));
        String oppTitle = rs.getString("opportunity_title");
        d.setOpportunityTitle(oppTitle != null ? oppTitle : "—");
        return d;
    }
}
