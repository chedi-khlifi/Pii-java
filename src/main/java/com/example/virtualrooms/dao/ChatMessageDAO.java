package com.example.virtualrooms.dao;

import com.example.util.DBConnection;
import com.example.entity.ChatMessage;
import com.example.entity.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatMessageDAO {
    private final String chatTable;
    private final String userTable;

    public ChatMessageDAO() {
        this.chatTable = resolveFirstExistingTable("chat_message", "chat_messages");
        this.userTable = resolveFirstExistingTable("user", "users");
    }

    public List<ChatMessage> findByRoom(int roomId) {
        String sql = "SELECT m.id, m.virtual_room_id, m.sender_id, m.content, m.created_at, u.username AS sender_name " +
                "FROM " + chatTable + " m " +
                "JOIN " + userTable + " u ON u.id = m.sender_id " +
                "WHERE m.virtual_room_id = ? " +
                "ORDER BY m.created_at ASC";
        List<ChatMessage> messages = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapMessage(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to load room messages: " + e.getMessage(), e);
        }
        return messages;
    }

    public boolean insert(int roomId, int senderId, String content) {
        String sql = "INSERT INTO " + chatTable + " (virtual_room_id, sender_id, content, created_at, is_edited) VALUES (?, ?, ?, CURRENT_TIMESTAMP, 0)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setInt(2, senderId);
            ps.setString(3, content);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to send message: " + e.getMessage(), e);
        }
    }

    private ChatMessage mapMessage(ResultSet rs) throws SQLException {
        ChatMessage m = new ChatMessage();
        m.setId(rs.getInt("id"));
        User sender = new User();
        sender.setId(rs.getInt("sender_id"));
        sender.setUsername(rs.getString("sender_name"));
        m.setSender(sender);
        m.setContent(rs.getString("content"));
        Timestamp ts = rs.getTimestamp("created_at");
        m.setCreatedAt(ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
        return m;
    }

    private String resolveFirstExistingTable(String... candidates) {
        try (Connection conn = DBConnection.getConnection()) {
            for (String table : candidates) {
                if (tableExists(conn, table)) return table;
            }
        } catch (SQLException ignored) {}
        return candidates[0];
    }

    private boolean tableExists(Connection conn, String table) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, table, new String[]{"TABLE"})) {
            return rs.next();
        }
    }
}
