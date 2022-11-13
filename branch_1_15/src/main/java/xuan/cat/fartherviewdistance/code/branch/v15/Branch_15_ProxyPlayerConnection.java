package xuan.cat.fartherviewdistance.code.branch.v15;

import net.minecraft.server.v1_15_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xuan.cat.fartherviewdistance.api.branch.packet.PacketKeepAliveEvent;
import xuan.cat.fartherviewdistance.api.branch.packet.PacketMapChunkEvent;
import xuan.cat.fartherviewdistance.api.branch.packet.PacketUnloadChunkEvent;
import xuan.cat.fartherviewdistance.api.branch.packet.PacketViewDistanceEvent;

import java.lang.reflect.Field;

public final class Branch_15_ProxyPlayerConnection {
    public static boolean read(Player player, Packet<?> packet) {
        if (packet instanceof PacketPlayInKeepAlive) {
            PacketKeepAliveEvent event = new PacketKeepAliveEvent(player, ((PacketPlayInKeepAlive) packet).b());
            Bukkit.getPluginManager().callEvent(event);
            return !event.isCancelled();
        } else {
            return true;
        }
    }


    private static Field field_PacketPlayOutUnloadChunk_chunkX;
    private static Field field_PacketPlayOutUnloadChunk_chunkZ;
    private static Field field_PacketPlayOutViewDistance_distance;
    private static Field field_PacketPlayOutMapChunk_chunkX;
    private static Field field_PacketPlayOutMapChunk_chunkZ;
    static {
        try {
            field_PacketPlayOutUnloadChunk_chunkX = PacketPlayOutUnloadChunk.class.getDeclaredField("a");
            field_PacketPlayOutUnloadChunk_chunkZ = PacketPlayOutUnloadChunk.class.getDeclaredField("b");
            field_PacketPlayOutViewDistance_distance = PacketPlayOutViewDistance.class.getDeclaredField("a");
            field_PacketPlayOutMapChunk_chunkX = PacketPlayOutMapChunk.class.getDeclaredField("a");
            field_PacketPlayOutMapChunk_chunkZ = PacketPlayOutMapChunk.class.getDeclaredField("b");
            field_PacketPlayOutUnloadChunk_chunkX.setAccessible(true);
            field_PacketPlayOutUnloadChunk_chunkZ.setAccessible(true);
            field_PacketPlayOutViewDistance_distance.setAccessible(true);
            field_PacketPlayOutMapChunk_chunkX.setAccessible(true);
            field_PacketPlayOutMapChunk_chunkZ.setAccessible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    public static boolean write(Player player, Packet<?> packet) {
        try {
            if (packet instanceof PacketPlayOutUnloadChunk) {
                PacketUnloadChunkEvent event = new PacketUnloadChunkEvent(player, field_PacketPlayOutUnloadChunk_chunkX.getInt(packet), field_PacketPlayOutUnloadChunk_chunkZ.getInt(packet));
                Bukkit.getPluginManager().callEvent(event);
                return !event.isCancelled();
            } else if (packet instanceof PacketPlayOutViewDistance) {
                PacketViewDistanceEvent event = new PacketViewDistanceEvent(player, field_PacketPlayOutViewDistance_distance.getInt(packet));
                Bukkit.getPluginManager().callEvent(event);
                return !event.isCancelled();
            } else if (packet instanceof PacketPlayOutMapChunk) {
                PacketMapChunkEvent event = new PacketMapChunkEvent(player, field_PacketPlayOutMapChunk_chunkX.getInt(packet), field_PacketPlayOutMapChunk_chunkZ.getInt(packet));
                Bukkit.getPluginManager().callEvent(event);
                return !event.isCancelled();
            } else {
                return true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return true;
        }
    }
}
