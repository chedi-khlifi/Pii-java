package tn.esprit.services.guardian;

import tn.esprit.Entity.Guardian.Resource;
import tn.esprit.db.DatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ResourceService {

    public List<Resource> findAll() throws SQLException {
        String sql = "SELECT id, title, description, file_path, type, download_count, rating, created_at, updated_at, subject_id, uploader_id FROM resource";
        List<Resource> resources = new ArrayList<>();

        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resources.add(mapRow(rs));
            }
        }

        return resources;
    }

    public List<Resource> findByUploader(int uploaderId) throws SQLException {
        String sql = "SELECT id, title, description, file_path, type, download_count, rating, created_at, updated_at, subject_id, uploader_id FROM resource WHERE uploader_id = ?";
        List<Resource> resources = new ArrayList<>();

        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, uploaderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resources.add(mapRow(rs));
                }
            }
        }

        return resources;
    }

    public Resource findById(int id) throws SQLException {
        String sql = "SELECT id, title, description, file_path, type, download_count, rating, created_at, updated_at, subject_id, uploader_id FROM resource WHERE id = ?";

        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }

        return null;
    }

    public int insert(Resource resource) throws SQLException {
        String sql = "INSERT INTO resource (title, description, file_path, type, download_count, rating, created_at, updated_at, subject_id, uploader_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, resource.title());
            ps.setString(2, resource.description());
            ps.setString(3, resource.filePath());
            ps.setString(4, resource.type());
            ps.setInt(5, resource.downloadCount());
            ps.setInt(6, resource.rating());
            ps.setTimestamp(7, Timestamp.valueOf(resource.createdAt()));
            ps.setTimestamp(8, Timestamp.valueOf(resource.updatedAt()));
            setNullableInt(ps, 9, resource.subjectId());
            ps.setInt(10, resource.uploaderId());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        return -1;
    }

    public boolean update(Resource resource) throws SQLException {
        if (resource.id() == null) {
            throw new IllegalArgumentException("Resource id must not be null for update.");
        }

        String sql = "UPDATE resource SET title = ?, description = ?, file_path = ?, type = ?, download_count = ?, rating = ?, created_at = ?, updated_at = ?, subject_id = ?, uploader_id = ? WHERE id = ?";

        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, resource.title());
            ps.setString(2, resource.description());
            ps.setString(3, resource.filePath());
            ps.setString(4, resource.type());
            ps.setInt(5, resource.downloadCount());
            ps.setInt(6, resource.rating());
            ps.setTimestamp(7, Timestamp.valueOf(resource.createdAt()));
            ps.setTimestamp(8, Timestamp.valueOf(resource.updatedAt()));
            setNullableInt(ps, 9, resource.subjectId());
            ps.setInt(10, resource.uploaderId());
            ps.setInt(11, resource.id());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM resource WHERE id = ?";

        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Resource mapRow(ResultSet rs) throws SQLException {
        return new Resource(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("file_path"),
                rs.getString("type"),
                rs.getInt("download_count"),
                rs.getInt("rating"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at")),
                rs.getInt("subject_id"),
                rs.getInt("uploader_id")
        );
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }
}
