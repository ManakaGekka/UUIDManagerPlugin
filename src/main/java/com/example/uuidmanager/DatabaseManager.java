package com.example.uuidmanager;

import org.bukkit.plugin.java.JavaPlugin;
import java.sql.*;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        initializeDatabase();
    }

    private void initializeDatabase() {
        String dbPath = plugin.getDataFolder() + "/player_uuids.db";
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            createTable();
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("数据库初始化失败: " + e.getMessage());
        }
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS player_uuids (\n" +
                     "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                     "    player_id TEXT NOT NULL,\n" +
                     "    old_uuid TEXT NOT NULL,\n" +
                     "    new_uuid TEXT,\n" +
                     "    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
                     ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            plugin.getLogger().info("玩家UUID表创建成功或已存在");
        } catch (SQLException e) {
            plugin.getLogger().severe("创建表失败: " + e.getMessage());
        }
    }

    public void insertPlayerUUID(String playerId, String uuid) {
        String sql = "INSERT INTO player_uuids (player_id, old_uuid) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            pstmt.setString(2, uuid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("插入记录失败: " + e.getMessage());
        }
    }

    public String getPlayerIdByUUID(String uuid) {
        String sql = "SELECT player_id FROM player_uuids WHERE old_uuid = ? OR new_uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.setString(2, uuid);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("player_id");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("查询记录失败: " + e.getMessage());
        }
        return null;
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().severe("关闭数据库连接失败: " + e.getMessage());
            }
        }
    }
}