//package xuan.cat.fartherviewdistance.code;
//
//import com.comphenix.protocol.PacketType;
//import com.comphenix.protocol.events.ListenerPriority;
//import com.comphenix.protocol.events.PacketAdapter;
//import com.comphenix.protocol.events.PacketEvent;
//import org.bukkit.entity.Player;
//import org.bukkit.plugin.Plugin;
//import xuan.cat.fartherviewdistance.code.data.PlayerChunkView;
//
//public final class ChunkPacketEvent extends PacketAdapter {
//
//    private final ChunkServer chunkServer;
//
//    public ChunkPacketEvent(Plugin plugin, ChunkServer chunkServer) {
//        super(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.UNLOAD_CHUNK, PacketType.Play.Server.VIEW_DISTANCE, PacketType.Play.Server.MAP_CHUNK, PacketType.Play.Server.LIGHT_UPDATE, PacketType.Play.Server.KEEP_ALIVE);
//        this.chunkServer    = chunkServer;
//    }
//
//    public void onPacketSending(PacketEvent event) {
//        if (event.isCancelled() || event.isReadOnly())
//            return;
//
//        Player          player      = event.getPlayer();
//        PacketType      packetType  = event.getPacketType();
//
//        if (packetType == PacketType.Play.Server.UNLOAD_CHUNK) {
////            // 區塊卸除
//            int[] chunkLoc = chunkServer.branchPacket.readChunkLocation(event.getPacket());
//            int chunkX = chunkLoc[0];
//            int chunkZ = chunkLoc[1];
//            PlayerChunkView view = chunkServer.getView(player);
//            if (view != null) {
////                Location nowLoc = view.getPlayer().getLocation();
////                int nowChunkX = nowLoc.getBlockX() >> 4;
////                int nowChunkZ = nowLoc.getBlockZ() >> 4;
////                ViewMap viewMap = view.getMap();
////                int serverMinX = nowChunkX - viewMap.serverDistance;
////                int serverMinZ = nowChunkZ - viewMap.serverDistance;
////                int serverMaxX = nowChunkX + viewMap.serverDistance;
////                int serverMaxZ = nowChunkZ + viewMap.serverDistance;
//                if (view.viewAPI.isChunkSend(chunkX, chunkZ)/* || !(chunkX >= serverMinX && chunkZ >= serverMinZ && chunkX <= serverMaxX && chunkZ <= serverMaxZ)*/)
//                    event.setCancelled(true);
//            }
//
//        } else if (packetType == PacketType.Play.Server.VIEW_DISTANCE) {
//            // 視野距離
//            PlayerChunkView view = chunkServer.getView(player);
//            int viewDistance = chunkServer.branchPacket.readViewDistance(event.getPacket());
//            if (view != null && view.getMap().extendDistance != viewDistance && viewDistance != 0)
//                event.setCancelled(true);
//
//        } else if (packetType == PacketType.Play.Server.MAP_CHUNK) {
////            // 區塊
//            chunkServer.packetEvent(player, event);
//
//        } else if (packetType == PacketType.Play.Server.LIGHT_UPDATE) {
////            // 更新光照
//            chunkServer.packetEvent(player, event);
//
//        } else if (packetType == PacketType.Play.Server.KEEP_ALIVE) {
//            // 保持活躍
//            long id = chunkServer.branchPacket.readKeepAlive(event.getPacket());
//            PlayerChunkView view = chunkServer.getView(player);
//            if (view != null) {
//                synchronized (view.networkSpeed) {
//                    if (view.networkSpeed.pingID != null && view.networkSpeed.pingID == id) {
//                        view.networkSpeed.lastPing = Math.max(1, (int) (System.currentTimeMillis() - view.networkSpeed.pingTimestamp));
//                        view.networkSpeed.pingID = null;
//                        event.setCancelled(true);
//                    } else if (view.networkSpeed.speedID != null && view.networkSpeed.speedID == id) {
//                        view.networkSpeed.add(Math.max(1, (int) (System.currentTimeMillis() - view.networkSpeed.speedTimestamp) - view.networkSpeed.lastPing), view.networkSpeed.speedConsume);
//                        view.networkSpeed.speedConsume = 0;
//                        view.networkSpeed.speedID = null;
//                        event.setCancelled(true);
//                    }
//                }
//            }
//        }
//    }
//
//    public void onPacketReceiving(PacketEvent event) {
//    }
//}
