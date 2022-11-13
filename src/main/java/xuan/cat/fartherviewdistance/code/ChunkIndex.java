package xuan.cat.fartherviewdistance.code;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import xuan.cat.fartherviewdistance.api.branch.BranchMinecraft;
import xuan.cat.fartherviewdistance.api.branch.BranchPacket;
import xuan.cat.fartherviewdistance.code.branch.v14.Branch_14_Minecraft;
import xuan.cat.fartherviewdistance.code.branch.v14.Branch_14_Packet;
import xuan.cat.fartherviewdistance.code.branch.v15.Branch_15_Minecraft;
import xuan.cat.fartherviewdistance.code.branch.v15.Branch_15_Packet;
import xuan.cat.fartherviewdistance.code.branch.v16.Branch_16_Minecraft;
import xuan.cat.fartherviewdistance.code.branch.v16.Branch_16_Packet;
import xuan.cat.fartherviewdistance.code.branch.v17.Branch_17_Minecraft;
import xuan.cat.fartherviewdistance.code.branch.v17.Branch_17_Packet;
import xuan.cat.fartherviewdistance.code.branch.v18.Branch_18_Minecraft;
import xuan.cat.fartherviewdistance.code.branch.v18.Branch_18_Packet;
import xuan.cat.fartherviewdistance.code.branch.v19.Branch_19_Minecraft;
import xuan.cat.fartherviewdistance.code.branch.v19.Branch_19_Packet;
import xuan.cat.fartherviewdistance.code.command.Command;
import xuan.cat.fartherviewdistance.code.command.CommandSuggest;
import xuan.cat.fartherviewdistance.code.data.ConfigData;
import xuan.cat.fartherviewdistance.code.data.viewmap.ViewShape;

public final class ChunkIndex extends JavaPlugin {
//    private static ProtocolManager protocolManager;
    private static Plugin plugin;
    private static ChunkServer chunkServer;
    private static ConfigData configData;
    private static BranchPacket branchPacket;
    private static BranchMinecraft branchMinecraft;


    public void onEnable() {
        plugin          = this;
//        protocolManager = ProtocolLibrary.getProtocolManager();

        saveDefaultConfig();
        configData      = new ConfigData(this, getConfig());

        // 檢測版本
        String bukkitVersion = Bukkit.getBukkitVersion();
        if (bukkitVersion.matches("^1\\.14[^0-9].*$")) {
            // 1.14
            branchPacket    = new Branch_14_Packet();
            branchMinecraft = new Branch_14_Minecraft();
            chunkServer     = new ChunkServer(configData, this, ViewShape.SQUARE, branchMinecraft, branchPacket);
        } else if (bukkitVersion.matches("^1\\.15\\D.*$")) {
            // 1.15
            branchPacket    = new Branch_15_Packet();
            branchMinecraft = new Branch_15_Minecraft();
            chunkServer     = new ChunkServer(configData, this, ViewShape.SQUARE, branchMinecraft, branchPacket);
        } else if (bukkitVersion.matches("^1\\.16\\D.*$")) {
            // 1.16
            branchPacket    = new Branch_16_Packet();
            branchMinecraft = new Branch_16_Minecraft();
            chunkServer     = new ChunkServer(configData, this, ViewShape.SQUARE, branchMinecraft, branchPacket);
        } else if (bukkitVersion.matches("^1\\.17\\D.*$")) {
            // 1.17
            branchPacket    = new Branch_17_Packet();
            branchMinecraft = new Branch_17_Minecraft();
            chunkServer     = new ChunkServer(configData, this, ViewShape.SQUARE, branchMinecraft, branchPacket);
        } else if (bukkitVersion.matches("^1\\.18\\D.*$")) {
            // 1.18
            branchPacket    = new Branch_18_Packet();
            branchMinecraft = new Branch_18_Minecraft();
            chunkServer     = new ChunkServer(configData, this, ViewShape.ROUND, branchMinecraft, branchPacket);
        } else if (bukkitVersion.matches("^1\\.19\\D.*$")) {
            // 1.19
            branchPacket    = new Branch_19_Packet();
            branchMinecraft = new Branch_19_Minecraft();
            chunkServer     = new ChunkServer(configData, this, ViewShape.ROUND, branchMinecraft, branchPacket);
        } else {
            throw new IllegalArgumentException("Unsupported MC version: " + bukkitVersion);
        }

        // 初始化一些資料
        for (Player player : Bukkit.getOnlinePlayers())
            chunkServer.initView(player);
        for (World world : Bukkit.getWorlds())
            chunkServer.initWorld(world);

        Bukkit.getPluginManager().registerEvents(new ChunkEvent(chunkServer, branchPacket, branchMinecraft), this);
//        protocolManager.addPacketListener(new ChunkPacketEvent(plugin, chunkServer));

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            ChunkPlaceholder.registerPlaceholder();
        }

        // 指令
        PluginCommand command = getCommand("viewdistance");
        if (command != null) {
            command.setExecutor(new Command(chunkServer, configData));
            command.setTabCompleter(new CommandSuggest(chunkServer, configData));
        }
    }

    public void onDisable() {
//        ChunkPlaceholder.unregisterPlaceholder();
        if (chunkServer != null)
            chunkServer.close();
    }

    public static ChunkServer getChunkServer() {
        return chunkServer;
    }

    public static ConfigData getConfigData() {
        return configData;
    }

    public static Plugin getPlugin() {
        return plugin;
    }

}
