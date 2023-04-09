package xuan.cat.fartherviewdistance.api.branch;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.IOException;

public interface BranchMinecraft {
    BranchNBT getChunkNBTFromDisk(World world, int chunkX, int chunkZ) throws IOException;

    BranchChunk getChunkFromMemoryCache(World world, int chunkX, int chunkZ);

    BranchChunk fromChunk(World world, int chunkX, int chunkZ, BranchNBT nbt, boolean integralHeightmap);

    BranchChunkLight fromLight(World world, BranchNBT nbt);

    BranchChunkLight fromLight(World world);

    BranchChunk fromChunk(World world, Chunk chunk);

    BranchChunk.Status fromStatus(BranchNBT nbt);

    void injectPlayer(Player player);
}
