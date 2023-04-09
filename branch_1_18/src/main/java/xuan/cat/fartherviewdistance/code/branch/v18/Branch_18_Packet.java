package xuan.cat.fartherviewdistance.code.branch.v18;

import io.netty.buffer.Unpooled;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import xuan.cat.fartherviewdistance.api.branch.BranchChunk;
import xuan.cat.fartherviewdistance.api.branch.BranchChunkLight;
import xuan.cat.fartherviewdistance.api.branch.BranchPacket;

import java.util.function.Consumer;

public final class Branch_18_Packet implements BranchPacket {
    private final Branch_18_PacketHandleChunk handleChunk = new Branch_18_PacketHandleChunk();
    private final Branch_18_PacketHandleLightUpdate handleLightUpdate = new Branch_18_PacketHandleLightUpdate();

    public void sendPacket(Player player, Packet<?> packet) {
        try {
            Connection container = ((CraftPlayer) player).getHandle().connection.connection;
            container.send(packet);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public void sendViewDistance(Player player, int viewDistance) {
        sendPacket(player, new ClientboundSetChunkCacheRadiusPacket(viewDistance));
    }

    public void sendUnloadChunk(Player player, int chunkX, int chunkZ) {
        sendPacket(player, new ClientboundForgetLevelChunkPacket(chunkX, chunkZ));
    }

    public Consumer<Player> sendChunkAndLight(BranchChunk chunk, BranchChunkLight light, boolean needTile, Consumer<Integer> consumeTraffic) {
        FriendlyByteBuf serializer = new FriendlyByteBuf(Unpooled.buffer().writerIndex(0));
        serializer.writeInt(chunk.getX());
        serializer.writeInt(chunk.getZ());
        this.handleChunk.write(serializer, chunk.getChunk(), needTile);
        this.handleLightUpdate.write(serializer, (Branch_18_ChunkLight) light, true);
        consumeTraffic.accept(serializer.readableBytes());
        ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(serializer);
        try {
            // 適用於 paper
            packet.setReady(true);
        } catch (NoSuchMethodError noSuchMethodError) {
            // 適用於 spigot (不推薦)
        }
        return (player) -> sendPacket(player, packet);
    }

    public void sendKeepAlive(Player player, long id) {
        sendPacket(player, new ClientboundKeepAlivePacket(id));
    }
}
