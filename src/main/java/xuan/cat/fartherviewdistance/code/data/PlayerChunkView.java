package xuan.cat.fartherviewdistance.code.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import xuan.cat.fartherviewdistance.api.branch.BranchPacket;
import xuan.cat.fartherviewdistance.api.data.PlayerView;
import xuan.cat.fartherviewdistance.api.event.*;
import xuan.cat.fartherviewdistance.code.ChunkServer;
import xuan.cat.fartherviewdistance.code.data.viewmap.ViewMap;
import xuan.cat.fartherviewdistance.code.data.viewmap.ViewShape;

import java.util.Map;

/** 玩家視圖計算器 */
public final class PlayerChunkView {
    public  final PlayerView viewAPI;
    private final Player player;
    private final BranchPacket branchPacket;
    /** 視圖計算器 */
    private final ViewMap mapView;
    /** 強制視野距離 */
    public Integer forciblyMaxDistance = null;
    /** 強制每秒能傳輸多少數據 (單位 bytes) */
    public Integer forciblySendSecondMaxBytes = null;
    /** 最後的視野距離 */
    private int lastDistance = 0;
    private final ConfigData configData;
    /** 延遲時間戳 */
    private long delayTime;
    /** 已卸除 */
    private boolean isUnload = false;
    /** 最後世界 */
    private World lastWorld;
    /** 最後座標 */
    private Location oldLocation = null;
    /** 移動過快 */
    public volatile boolean moveTooFast = false;
    /** 網路流量 */
    public final NetworkTraffic networkTraffic = new NetworkTraffic();
    /** 網路速度 */
    public final NetworkSpeed networkSpeed = new NetworkSpeed();
    /** 等待發送 */
    public volatile boolean waitSend = false;
    /** 同步鑰匙 */
    public volatile long syncKey;
    /** 報告 */
    public final CumulativeReport cumulativeReport = new CumulativeReport();
    /** 檢查權限 */
    private Long permissionsCheck = null;
    /** 權限命中 */
    private Integer permissionsHit = null;
    /** 權限需要檢查 */
    public boolean permissionsNeed = true;


    public PlayerChunkView(Player player, ConfigData configData, ViewShape viewShape, BranchPacket branchPacket) {
        this.player = player;
        this.configData = configData;
        this.branchPacket = branchPacket;
        this.mapView = configData.viewDistanceMode.createMap(viewShape);
        this.lastWorld = player.getWorld();
        this.syncKey = ChunkServer.random.nextLong();

        updateDistance();
        delay();

        mapView.setCenter(player.getLocation());

        this.viewAPI = new PlayerView(this);
        Bukkit.getPluginManager().callEvent(new PlayerInitViewEvent(viewAPI));
    }


    private int serverDistance() {
        return configData.serverViewDistance <= -1 ? (Bukkit.getViewDistance() + 1) : configData.serverViewDistance;
    }


    public void updateDistance() {
        updateDistance(false);
    }
    private void updateDistance(boolean forcibly) {
        int newDistance = max();
        synchronized (mapView) {
            mapView.serverDistance = serverDistance();
            if (newDistance < mapView.serverDistance) {
                newDistance = mapView.serverDistance;
            }
        }
        if (forcibly || lastDistance != newDistance) {
//            mapView.markOutsideWait(newDistance);
            lastDistance = newDistance;
            mapView.extendDistance = newDistance;
            PlayerSendViewDistanceEvent event = new PlayerSendViewDistanceEvent(viewAPI, newDistance);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled())
                branchPacket.sendViewDistance(player, event.getDistance());
        }
    }


    private double square(double num) {
        return num * num;
    }


    public boolean overSpeed() {
        return overSpeed(player.getLocation());
    }
    public boolean overSpeed(Location location) {
        ConfigData.World configWorld = configData.getWorld(lastWorld.getName());
        if (configWorld.speedingNotSend == -1) {
            return false;
        } else {
            double speed = 0;

            if (oldLocation != null && oldLocation.getWorld() == location.getWorld())
                speed = Math.sqrt(square(oldLocation.getX() - location.getX()) + square(oldLocation.getZ() - location.getZ()));
            oldLocation = location;

            // 檢查速度是否太快 (水平飛行速度 > ? 方塊)
            return speed > configWorld.speedingNotSend;
        }
    }


    public synchronized boolean move() {
        return move(player.getLocation());
    }
    public synchronized boolean move(Location location) {
        return move(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }
    public synchronized boolean move(int chunkX, int chunkZ) {
        if (isUnload)
            return false;

        if (player.getWorld() != lastWorld) {
            unload();
            return false;
        }

        int hitX;
        int hitZ;
        PlayerSendUnloadChunkEvent event;
        for (long chunkKey : mapView.movePosition(chunkX, chunkZ)) {
            hitX = ViewMap.getX(chunkKey);
            hitZ = ViewMap.getZ(chunkKey);
            event = new PlayerSendUnloadChunkEvent(viewAPI, hitX, hitZ);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled())
                branchPacket.sendUnloadChunk(player, hitX, hitZ);
        }

        return true;
    }


    public void delay() {
        delay(System.currentTimeMillis() + configData.getWorld(lastWorld.getName()).delayBeforeSend);
    }
    public void delay(long delayTime) {
        this.delayTime = delayTime;
    }

    public Long next() {
        if (player.getWorld() != lastWorld) {
            unload();
            return null;
        }

        if (isUnload)
            return null;

        if (delayTime >= System.currentTimeMillis())
            return null;

        Long        chunkKey    = mapView.get();
        if (chunkKey == null)
            return null;

        WorldBorder worldBorder         = lastWorld.getWorldBorder();
        int         chunkX              = ViewMap.getX(chunkKey);
        int         chunkZ              = ViewMap.getZ(chunkKey);
        Location    borderCenter        = worldBorder.getCenter();
        int         borderSizeRadius    = (int) worldBorder.getSize() / 2;
        int         borderMinX          = ((borderCenter.getBlockX() - borderSizeRadius) >> 4) - 1;
        int         borderMaxX          = ((borderCenter.getBlockX() + borderSizeRadius) >> 4) + 1;
        int         borderMinZ          = ((borderCenter.getBlockZ() - borderSizeRadius) >> 4) - 1;
        int         borderMaxZ          = ((borderCenter.getBlockZ() + borderSizeRadius) >> 4) + 1;

        return borderMinX <= chunkX && chunkX <= borderMaxX && borderMinZ <= chunkZ && chunkZ <= borderMaxZ ? chunkKey : null;
    }


    public void unload() {
        if (!isUnload) {
            delay();
            syncKey = ChunkServer.random.nextLong();
            isUnload = true;
            branchPacket.sendViewDistance(player, 0);
            branchPacket.sendViewDistance(player, mapView.extendDistance);
            mapView.clear();
        }
    }


    public boolean install() {
        if (isUnload) {
            delay();
            mapView.clear();
            updateDistance(true);

            lastWorld   = player.getWorld();
            isUnload    = false;
            return true;
        }
        return false;
    }


    public void send(int x, int z) {
        PlayerViewMarkSendChunkEvent event = new PlayerViewMarkSendChunkEvent(viewAPI, x, z);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled())
            mapView.markSendPosition(x, z);
    }


    public void remove(int x, int z) {
        PlayerViewMarkWaitChunkEvent event = new PlayerViewMarkWaitChunkEvent(viewAPI, x, z);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled())
            mapView.markWaitPosition(x, z);
    }


    public int max() {
        ConfigData.World configWorld = configData.getWorld(lastWorld.getName());
        int viewDistance = configWorld.maxViewDistance;
        int clientViewDistance = player.getClientViewDistance();
        Integer forciblyViewDistance = forciblyMaxDistance;

        PlayerCheckViewDistanceEvent event = new PlayerCheckViewDistanceEvent(viewAPI, serverDistance(), clientViewDistance, viewDistance);
        Bukkit.getPluginManager().callEvent(event);

        if (event.getForciblyDistance() != null) {
            viewDistance = event.getForciblyDistance();
        } else if (forciblyViewDistance != null) {
            viewDistance = forciblyViewDistance;
        } else if (permissionsNeed || (configData.permissionsPeriodicMillisecondCheck != -1 && (permissionsCheck == null || permissionsCheck <= System.currentTimeMillis() - configData.permissionsPeriodicMillisecondCheck))) {
            permissionsNeed = false;
            permissionsCheck = System.currentTimeMillis();
            permissionsHit = null;
            // 檢查權限節點
            for (Map.Entry<String, Integer> permissionsNodeEntry : configData.permissionsNodeList) {
                int permissionViewDistance = permissionsNodeEntry.getValue();
                if (permissionViewDistance <= configWorld.maxViewDistance && (permissionsHit == null || permissionViewDistance > permissionsHit) && player.hasPermission(permissionsNodeEntry.getKey())) {
                    permissionsHit = permissionViewDistance;
                }
            }
        }

        if (permissionsHit != null)
            viewDistance = permissionsHit;

        if (viewDistance > clientViewDistance)
            viewDistance = clientViewDistance;
        if (viewDistance < 1)
            viewDistance = 1;

        return viewDistance;
    }

    public void clear() {
        mapView.clear();
    }


    public void recalculate() {
        mapView.markOutsideWait(mapView.serverDistance);
    }

    public ViewMap getMap() {
        return mapView;
    }

    public World getLastWorld() {
        return lastWorld;
    }

    public Player getPlayer() {
        return player;
    }

    public long getDelayTime() {
        return delayTime;
    }
}
