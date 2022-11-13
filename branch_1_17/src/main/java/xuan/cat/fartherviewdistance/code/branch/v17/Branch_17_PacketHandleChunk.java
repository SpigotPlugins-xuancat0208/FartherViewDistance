package xuan.cat.fartherviewdistance.code.branch.v17;

import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.game.PacketPlayOutMapChunk;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkSection;
import net.minecraft.world.level.levelgen.HeightMap;
import org.bukkit.craftbukkit.v1_17_R1.CraftChunk;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class Branch_17_PacketHandleChunk {

    public PacketPlayOutMapChunk createMapChunkPacket(org.bukkit.Chunk chunk, boolean needTile, Consumer<Integer> consumeTraffic) {
        return createMapChunkPacket(((CraftChunk) chunk).getHandle(), needTile, consumeTraffic);
    }
    /**
     * 創建區塊數據包 (主要是用於跳過核心自帶的反礦物透視)
     * @param chunk 區塊資料
     * @return 區塊數據包
     */
    public PacketPlayOutMapChunk createMapChunkPacket(Chunk chunk, boolean needTile, Consumer<Integer> consumeTraffic) {
        PacketDataSerializer    serializer          = new PacketDataSerializer(Unpooled.buffer().writerIndex(0));

        ChunkCoordIntPair       chunkPos            = chunk.getPos();
        int                     positionX           = chunkPos.b;
        int                     positionZ           = chunkPos.c;
        NBTTagCompound          heightNBT           = new NBTTagCompound();

        // 保存高度圖資料
        HeightMap.Type          heightType;
        HeightMap               heightMap;
        for (Map.Entry<HeightMap.Type, HeightMap> entry : chunk.e()) {
            heightType          = entry.getKey();
            heightMap           = entry.getValue();
            if (heightType.b())
                heightNBT.set(heightType.a(), new NBTTagLongArray(heightMap.a()));
        }

        int[]                   biomeBytes          = chunk.getBiomeIndex().a();
        byte[]                  chunkBytes          = new byte[this.getExistDataSection(chunk)];
        BitSet                  existDataSection    = extractChunkData(new PacketDataSerializer(Unpooled.wrappedBuffer(chunkBytes).writerIndex(0)), chunk);
        List<NBTTagCompound>    titleEntityNBT      = new ArrayList<>();

        if (needTile) {
            for (TileEntity tileEntity : chunk.getTileEntities().values()) {
                titleEntityNBT.add(tileEntity.Z_());
            }
        }

        serializer.writeInt(positionX);
        serializer.writeInt(positionZ);
        serializer.a(existDataSection);
        serializer.a(heightNBT);
        serializer.a(biomeBytes);
        serializer.d(chunkBytes.length);
        serializer.writeBytes(chunkBytes);
        serializer.a(titleEntityNBT, PacketDataSerializer::a);

        consumeTraffic.accept(serializer.readableBytes());

        return new PacketPlayOutMapChunk(serializer);
    }

    private BitSet extractChunkData(PacketDataSerializer buf, Chunk chunk) {
        BitSet          bitSet      = new BitSet();
        ChunkSection[]  sections    = chunk.getSections();
        for(int index = 0, length = sections.length; index < length; ++index) {
            ChunkSection levelChunkSection = sections[index];
            if (levelChunkSection != Chunk.a && !levelChunkSection.c()) {
                bitSet.set(index);
                try {
                    // 適用於 paper
                    levelChunkSection.write(buf, null);
                } catch (NoSuchMethodError noSuchMethodError) {
                    // 適用於 spigot (不推薦)
                    levelChunkSection.b(buf);
                }
            }
        }
        return bitSet;
    }

    private int getExistDataSection(Chunk chunk) {
        int existDataSection = 0;
        for (ChunkSection levelChunkSection : chunk.getSections()) {
            if (levelChunkSection != Chunk.a && !levelChunkSection.c()) {
                existDataSection += levelChunkSection.j();
            }
        }
        return existDataSection;
    }


}
