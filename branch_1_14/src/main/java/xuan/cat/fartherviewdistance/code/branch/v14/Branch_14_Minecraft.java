package xuan.cat.fartherviewdistance.code.branch.v14;

import io.netty.channel.*;
import net.minecraft.server.v1_14_R1.*;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_14_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import xuan.cat.fartherviewdistance.api.branch.BranchChunk;
import xuan.cat.fartherviewdistance.api.branch.BranchChunkLight;
import xuan.cat.fartherviewdistance.api.branch.BranchMinecraft;
import xuan.cat.fartherviewdistance.api.branch.BranchNBT;

import java.io.IOException;

public final class Branch_14_Minecraft implements BranchMinecraft {
    public BranchNBT getChunkNBTFromDisk(World world, int chunkX, int chunkZ) throws IOException {
        WorldServer         worldServer     = ((CraftWorld) world).getHandle();
        ChunkProviderServer providerServer  = worldServer.getChunkProvider();
        PlayerChunkMap      playerChunkMap  = providerServer.playerChunkMap;
        NBTTagCompound      nbtTagCompound  = playerChunkMap.read(new ChunkCoordIntPair(chunkX, chunkZ));
        return nbtTagCompound != null ? new Branch_14_NBT(nbtTagCompound) : null;
    }

    /**
     * @deprecated 由於 NMS 沒有實作異步, 所以不使用
     */
    @Deprecated
    public BranchChunk getChunkFromMemoryCache(World world, int chunkX, int chunkZ) {
        return null;
    }

    public BranchChunk fromChunk(World world, int chunkX, int chunkZ, BranchNBT nbt, boolean integralHeightmap) {
        return Branch_14_ChunkRegionLoader.loadChunk(((CraftWorld) world).getHandle(), new ChunkCoordIntPair(chunkX, chunkZ), ((Branch_14_NBT) nbt).getNMSTag(), integralHeightmap);
    }

    public BranchChunkLight fromLight(World world, BranchNBT nbt) {
        return Branch_14_ChunkRegionLoader.loadLight(((CraftWorld) world).getHandle(), ((Branch_14_NBT) nbt).getNMSTag());
    }
    public BranchChunkLight fromLight(World world) {
        return new Branch_14_ChunkLight(((CraftWorld) world).getHandle());
    }

    public BranchChunk.Status fromStatus(BranchNBT nbt) {
        return Branch_14_ChunkRegionLoader.loadStatus(((Branch_14_NBT) nbt).getNMSTag());
    }

    public BranchChunk fromChunk(World world, org.bukkit.Chunk chunk) {
        return new Branch_14_Chunk(((CraftChunk) chunk).getCraftWorld().getHandle(), ((CraftChunk) chunk).getHandle());
    }

    public int getPlayerPing(Player player) {
        return  ((CraftPlayer) player).getHandle().ping;
    }

    public void injectPlayer(Player player) {
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        PlayerConnection connection = entityPlayer.playerConnection;
        NetworkManager networkManager = connection.networkManager;
        Channel channel = networkManager.channel;
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addAfter("packet_handler", "farther_view_distance_write", new ChannelDuplexHandler() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                if (msg instanceof Packet) {
                    if (!Branch_14_ProxyPlayerConnection.write(player, (Packet<?>) msg))
                        return;
                }
                super.write(ctx, msg, promise);
            }
        });
        pipeline.addAfter("encoder", "farther_view_distance_read", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                if (msg instanceof Packet) {
                    if (!Branch_14_ProxyPlayerConnection.read(player, (Packet<?>) msg))
                        return;
                }
                super.channelRead(ctx, msg);
            }
        });
    }
}
