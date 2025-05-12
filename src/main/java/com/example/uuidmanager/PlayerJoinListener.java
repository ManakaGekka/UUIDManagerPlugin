package com.example.uuidmanager;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PlayerJoinListener implements Listener {
    private final UUIDManagerPlugin plugin;
    private final DatabaseManager dbManager;

    public PlayerJoinListener(UUIDManagerPlugin plugin) {
        this.plugin = plugin;
        this.dbManager = plugin.getDbManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerId = player.getName();
        String newUuid = player.getUniqueId().toString();

        String existingPlayerId = dbManager.getPlayerIdByUUID(newUuid);
        if (existingPlayerId != null) {
            plugin.getLogger().info("玩家 " + playerId + " 使用新UUID登录，旧ID为 " + existingPlayerId);
            String oldUuid = findOldUuid(existingPlayerId, newUuid);
            if (oldUuid != null) {
                renameAndMoveFile(oldUuid, newUuid);
            }
        } else {
            dbManager.insertPlayerUUID(playerId, newUuid);
            plugin.getLogger().info("记录新玩家UUID：" + playerId + " - " + newUuid);
        }
    }

    private String findOldUuid(String playerId, String newUuid) {
        String sql = "SELECT old_uuid FROM player_uuids WHERE player_id = ? AND new_uuid IS NULL";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("old_uuid");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("查询旧UUID失败: " + e.getMessage());
        }
        return null;
    }

    private void renameAndMoveFile(String oldUuid, String newUuid) {
        File backupDir = new File(plugin.getDataFolder(), "backups");
        if (!backupDir.exists()) backupDir.mkdirs();

        File oldFile = new File(backupDir, oldUuid + ".dat");
        File newFile = new File(backupDir, newUuid + ".dat");
        File playerDataDir = new File(plugin.getServer().getWorldContainer(), "world/playerdata");
        if (!playerDataDir.exists()) playerDataDir.mkdirs();

        if (oldFile.exists()) {
            try {
                Files.move(oldFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Files.move(newFile.toPath(), new File(playerDataDir, newUuid + ".dat").toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("文件重命名并移动成功：" + oldUuid + ".dat -> " + newUuid + ".dat");
            } catch (Exception e) {
                plugin.getLogger().severe("文件操作失败: " + e.getMessage());
            }
        } else {
            plugin.getLogger().warning("旧UUID文件不存在：" + oldUuid + ".dat");
        }
    }
}