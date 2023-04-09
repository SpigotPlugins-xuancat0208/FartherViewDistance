package xuan.cat.fartherviewdistance.code.branch.v15;

import org.bukkit.entity.Player;
import xuan.cat.fartherviewdistance.api.branch.BranchChunk;
import xuan.cat.fartherviewdistance.api.branch.BranchChunkLight;
import xuan.cat.fartherviewdistance.api.branch.BranchPacket;

import java.util.function.Consumer;

public final class Branch_15_Packet implements BranchPacket {
    @Override
    public void sendViewDistance(Player player, int viewDistance) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendUnloadChunk(Player player, int chunkX, int chunkZ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Consumer<Player> sendChunkAndLight(BranchChunk chunk, BranchChunkLight light, boolean needTile, Consumer<Integer> consumeTraffic) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendKeepAlive(Player player, long id) {
        throw new UnsupportedOperationException();
    }
}
