package xuan.cat.fartherviewdistance.code.branch.v17;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.network.PlayerConnection;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import xuan.cat.fartherviewdistance.api.branch.BranchChunk;
import xuan.cat.fartherviewdistance.api.branch.BranchChunkLight;
import xuan.cat.fartherviewdistance.api.branch.BranchPacket;

import java.util.function.Consumer;

public final class Branch_17_Packet implements BranchPacket {
    private final Branch_17_PacketHandleChunk           handleChunk         = new Branch_17_PacketHandleChunk();
    private final Branch_17_PacketHandleLightUpdate     handleLightUpdate   = new Branch_17_PacketHandleLightUpdate();

    public void sendPacket(Player player, Packet<?> packet) {
        try {
            PlayerConnection container = ((CraftPlayer) player).getHandle().b;
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
        PacketPlayOutMapChunk packetChunk = handleChunk.createMapChunkPacket(chunk.getChunk(), needTile, consumeTraffic);
        PacketPlayOutLightUpdate packetLight = handleLightUpdate.createLightUpdatePacket(chunk.getX(), chunk.getZ(), (Branch_17_ChunkLight) light, true, consumeTraffic);
        try {
            // 適用於 paper
            packetChunk.setReady(true);
        } catch (NoSuchMethodError noSuchMethodError) {
            // 適用於 spigot (不推薦)
        }
        return (player) -> {
            sendPacket(player, packetLight);
            sendPacket(player, packetChunk);
        };
    }

    public void sendKeepAlive(Player player, long id) {
        sendPacket(player, new PacketPlayOutKeepAlive(id));
    }

}
