package xuan.cat.fartherviewdistance.code.branch.v18;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import org.bukkit.Chunk;
import org.bukkit.craftbukkit.v1_18_R2.CraftChunk;

import java.util.HashMap;
import java.util.Map;

public final class Branch_18_PacketHandleChunk {
    public Branch_18_PacketHandleChunk() {
    }

    public void write(FriendlyByteBuf serializer, Chunk chunk, boolean needTile) {
        this.write(serializer, ((CraftChunk) chunk).getHandle(), needTile);
    }

    public void write(FriendlyByteBuf serializer, LevelChunk chunk, boolean needTile) {
        CompoundTag heightmapsNBT = new CompoundTag();
        for (Map.Entry<Heightmap.Types, Heightmap> entry : chunk.getHeightmaps()) {
            Heightmap.Types heightType = entry.getKey();
            Heightmap heightMap = entry.getValue();
            if (heightType.sendToClient())
                heightmapsNBT.put(heightType.getSerializationKey(), new LongArrayTag(heightMap.getRawData()));
        }

        int chunkSize = 0;
        for(LevelChunkSection section : chunk.getSections()) {
            chunkSize += section.getSerializedSize();
        }
        byte[] bufferBytes = new byte[chunkSize];
        FriendlyByteBuf bufferByteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bufferBytes));
        bufferByteBuf.writerIndex(0);
        for(LevelChunkSection section : chunk.getSections()) {
            section.write(bufferByteBuf);
        }

        serializer.writeNbt(heightmapsNBT);
        serializer.writeVarInt(bufferBytes.length);
        serializer.writeBytes(bufferBytes);

        Map<BlockPos, BlockEntity> blockEntityMap = !needTile ? new HashMap<>(0) : chunk.getBlockEntities();
        serializer.writeCollection(blockEntityMap.entrySet(), (buf, entry) -> {
            BlockEntity blockEntity = entry.getValue();
            CompoundTag entityNBT = blockEntity.getUpdateTag();
            BlockPos blockPos = blockEntity.getBlockPos();
            buf.writeByte(SectionPos.sectionRelative(blockPos.getX()) << 4 | SectionPos.sectionRelative(blockPos.getZ()));
            buf.writeShort(blockPos.getY());
            buf.writeVarInt(Registry.BLOCK_ENTITY_TYPE.getId(blockEntity.getType()));
            buf.writeNbt(entityNBT.isEmpty() ? null : entityNBT);
        });
    }
}
