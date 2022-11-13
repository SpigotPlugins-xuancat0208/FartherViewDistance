package xuan.cat.fartherviewdistance.code.branch.v14;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import xuan.cat.fartherviewdistance.api.branch.BranchChunk;
import xuan.cat.fartherviewdistance.api.branch.BranchChunkLight;
import xuan.cat.fartherviewdistance.api.branch.BranchMinecraft;
import xuan.cat.fartherviewdistance.api.branch.BranchNBT;

import java.io.IOException;

public final class Branch_14_Minecraft implements BranchMinecraft {
    @Override
    public BranchNBT getChunkNBTFromDisk(World world, int chunkX, int chunkZ) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Chunk getChunkFromMemoryCache(World world, int chunkX, int chunkZ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BranchChunk fromChunk(World world, int chunkX, int chunkZ, BranchNBT nbt, boolean integralHeightmap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BranchChunkLight fromLight(World world, BranchNBT nbt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BranchChunkLight fromLight(World world) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BranchChunk fromChunk(World world, Chunk chunk) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BranchChunk.Status fromStatus(BranchNBT nbt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void injectPlayer(Player player) {
        throw new UnsupportedOperationException();
    }
}
