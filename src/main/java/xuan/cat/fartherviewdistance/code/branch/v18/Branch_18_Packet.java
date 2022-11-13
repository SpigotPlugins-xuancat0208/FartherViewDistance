package xuan.cat.fartherviewdistance.code.branch.v18;

import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import xuan.cat.fartherviewdistance.api.branch.BranchChunkLight;
import xuan.cat.fartherviewdistance.api.branch.BranchPacket;

import java.util.function.Consumer;

public final class Branch_18_Packet implements BranchPacket {
    @Override
    public void sendViewDistance(Player player, int viewDistance) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendUnloadChunk(Player player, int chunkX, int chunkZ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Consumer<Player> sendChunkAndLight(Chunk chunk, BranchChunkLight light, boolean needTile, Consumer<Integer> consumeTraffic) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendKeepAlive(Player player, long id) {
        throw new UnsupportedOperationException();
    }
}
