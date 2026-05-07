package com.mindforge.services.carriere;

import com.mindforge.entities.carriere.Entreprise;
import com.mindforge.interfaces.IService;
import com.mindforge.utils.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EntrepriseService implements IService<Entreprise> {

    private Connection cnx;

    public EntrepriseService() {
        try {
            cnx = DBConnection.getConnection();
        } catch (SQLException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    @Override
    public void addEntity(Entreprise e) {
        String query = "INSERT INTO entreprise (name, description, industry, contact_email, contact_phone, website, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setString(1, e.getName());
            pst.setString(2, e.getDescription());
            pst.setString(3, e.getIndustry());
            pst.setString(4, e.getContactEmail());
            pst.setInt(5, e.getContactPhone());
            pst.setString(6, e.getWebsite());
            pst.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            pst.executeUpdate();
            System.out.println("Entreprise added successfully.");
        } catch (SQLException ex) {
            System.out.println("Error adding entreprise: " + ex.getMessage());
        }
    }

    @Override
    public void deleteEntity(Entreprise e) {
        String query = "DELETE FROM entreprise WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, e.getId());
            pst.executeUpdate();
            System.out.println("Entreprise deleted successfully.");
        } catch (SQLException ex) {
            System.out.println("Error deleting entreprise: " + ex.getMessage());
        }
    }

    @Override
    public void updateEntity(int id, Entreprise e) {
        String query = "UPDATE entreprise SET name = ?, description = ?, industry = ?, contact_email = ?, contact_phone = ?, website = ? WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setString(1, e.getName());
            pst.setString(2, e.getDescription());
            pst.setString(3, e.getIndustry());
            pst.setString(4, e.getContactEmail());
            pst.setInt(5, e.getContactPhone());
            pst.setString(6, e.getWebsite());
            pst.setInt(7, id);
            pst.executeUpdate();
            System.out.println("Entreprise updated successfully.");
        } catch (SQLException ex) {
            System.out.println("Error updating entreprise: " + ex.getMessage());
        }
    }

    @Override
    public List<Entreprise> getData() {
        List<Entreprise> list = new ArrayList<>();
        String query = "SELECT * FROM entreprise";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            System.out.println("Error fetching entreprises: " + ex.getMessage());
        }
        return list;
    }

    public List<Entreprise> getByUserId(int userId) {
        List<Entreprise> list = new ArrayList<>();
        String query = "SELECT e.* FROM entreprise e " +
                       "JOIN entreprise_user eu ON e.id = eu.entreprise_id " +
                       "WHERE eu.user_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            System.out.println("Error fetching entreprises by user: " + ex.getMessage());
        }
        return list;
    }

    private Entreprise mapRow(ResultSet rs) throws SQLException {
        Entreprise e = new Entreprise();
        e.setId(rs.getInt("id"));
        e.setName(rs.getString("name"));
        e.setDescription(rs.getString("description"));
        e.setIndustry(rs.getString("industry"));
        e.setContactEmail(rs.getString("contact_email"));
        e.setContactPhone(rs.getInt("contact_phone"));
        e.setWebsite(rs.getString("website"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) e.setCreatedAt(ts.toLocalDateTime());
        return e;
    }
}
