package com.example.uuidmanager;

import org.bukkit.plugin.java.JavaPlugin;

public class UUIDManagerPlugin extends JavaPlugin {
    public DatabaseManager getDbManager() {
        return dbManager;
    }
    private DatabaseManager dbManager;

    @Override
    public void onEnable() {
        getLogger().info("UUID Manager Plugin 已启用！");
        // 初始化数据库连接
        this.dbManager = new DatabaseManager(this);
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("UUID Manager Plugin 已禁用！");
        // 关闭数据库连接
        if (dbManager != null) {
            dbManager.close();
        }
    }
}