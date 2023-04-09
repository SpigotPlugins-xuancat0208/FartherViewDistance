package xuan.cat.fartherviewdistance.code.branch.v14;

import io.netty.buffer.Unpooled;
import net.minecraft.server.v1_14_R1.*;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import xuan.cat.fartherviewdistance.api.branch.BranchChunk;
import xuan.cat.fartherviewdistance.api.branch.BranchChunkLight;
import xuan.cat.fartherviewdistance.api.branch.BranchPacket;

import java.util.function.Consumer;

public final class Branch_14_Packet implements BranchPacket {
    private final Branch_14_PacketHandleChunk handleChunk         = new Branch_14_PacketHandleChunk();
    private final Branch_14_PacketHandleLightUpdate handleLightUpdate   = new Branch_14_PacketHandleLightUpdate();

    public void sendPacket(Player player, Packet<?> packet) {
        try {
            PlayerConnection container = ((CraftPlayer) player).getHandle().playerConnection;
            container.sendPacket(packet);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public void sendViewDistance(Player player, int viewDistance) {
        sendPacket(player, new PacketPlayOutViewDistance(viewDistance));
    }

    public void sendUnloadChunk(Player player, int chunkX, int chunkZ) {
        sendPacket(player, new PacketPlayOutUnloadChunk(chunkX, chunkZ));
    }

    public Consumer<Player> sendChunkAndLight(BranchChunk chunk, BranchChunkLight light, boolean needTile, Consumer<Integer> consumeTraffic) {
        PacketPlayOutMapChunk packetChunk = handleChunk.createMapChunkPacket(chunk.getChunk(), 65535, needTile);
        PacketPlayOutLightUpdate packetLight = handleLightUpdate.createLightUpdatePacket(chunk.getX(), chunk.getZ(), ((Branch_14_ChunkLight) light));
        try {
            // 適用於 paper
            packetChunk.setReady(true);
        } catch (NoSuchMethodError noSuchMethodError) {
            // 適用於 spigot (不推薦)
        }
        calculateConsume(packetChunk, consumeTraffic);
        calculateConsume(packetLight, consumeTraffic);
        return (player) -> {
            sendPacket(player, packetLight);
            sendPacket(player, packetChunk);
        };
    }

    public void sendKeepAlive(Player player, long id) {
        sendPacket(player, new PacketPlayOutKeepAlive(id));
    }

    private <T extends PacketListener> void calculateConsume(Packet<T> packet, Consumer<Integer> consumeTraffic) {
        PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer().writerIndex(0));
        try {
            packet.b(serializer);
        } catch (Exception ignored) {
        }
        consumeTraffic.accept(serializer.readableBytes());
    }
}
