package xuan.cat.fartherviewdistance.api.branch;

import org.bukkit.entity.Player;

import java.util.function.Consumer;

public interface BranchPacket {
    void sendViewDistance(Player player, int viewDistance);

    void sendUnloadChunk(Player player, int chunkX, int chunkZ);

    Consumer<Player> sendChunkAndLight(org.bukkit.Chunk chunk, BranchChunkLight light, boolean needTile, Consumer<Integer> consumeTraffic);

    void sendKeepAlive(Player player, long id);
}
