package xuan.cat.fartherviewdistance.code.branch.v15;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.server.v1_15_R1.*;
import org.bukkit.craftbukkit.v1_15_R1.CraftChunk;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Branch_15_PacketHandleChunk {


    private Field field_positionX           = null;
    private Field field_positionZ           = null;
    private Field field_existDataSection    = null;
    private Field field_heightNBT           = null;
    private Field field_biomeBytes          = null;
    private Field field_chunkBytes          = null;
    private Field field_titleEntityNBT      = null;
    private Field field_isFullChunk         = null;


    public Branch_15_PacketHandleChunk() {
        try {
            Class<PacketPlayOutMapChunk> chunkClass = PacketPlayOutMapChunk.class;
            field_positionX         = chunkClass.getDeclaredField("a");
            field_positionZ         = chunkClass.getDeclaredField("b");
            field_existDataSection  = chunkClass.getDeclaredField("c");
            field_heightNBT         = chunkClass.getDeclaredField("d");
            field_biomeBytes        = chunkClass.getDeclaredField("e");
            field_chunkBytes        = chunkClass.getDeclaredField("f");
            field_titleEntityNBT    = chunkClass.getDeclaredField("g");
            field_isFullChunk       = chunkClass.getDeclaredField("h");

            field_positionX         .setAccessible(true);
            field_positionZ         .setAccessible(true);
            field_existDataSection  .setAccessible(true);
            field_heightNBT         .setAccessible(true);
            field_biomeBytes        .setAccessible(true);
            field_chunkBytes        .setAccessible(true);
            field_titleEntityNBT    .setAccessible(true);
            field_isFullChunk       .setAccessible(true);

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }



    public PacketPlayOutMapChunk createMapChunkPacket(org.bukkit.Chunk chunk, int existChunkSection, boolean needTile) {
        return createMapChunkPacket(((CraftChunk) chunk).getHandle(), existChunkSection, needTile);
    }
    /**
     * ????????????????????? (???????????????????????????????????????????????????)
     * @param chunk ????????????
     * @param existChunkSection ???????????????
     *                          ?????? 16 ?????????, 1111111111111111 = 65535, ??????????????? 0 ?????????
     * @return ???????????????
     */
    public PacketPlayOutMapChunk createMapChunkPacket(Chunk chunk, int existChunkSection, boolean needTile) {
        try {
            ChunkCoordIntPair       chunkPos            = chunk.getPos();
            PacketPlayOutMapChunk   packet              = new PacketPlayOutMapChunk();
            int                     positionX           = chunkPos.x;
            int                     positionZ           = chunkPos.z;
            int                     existDataSection;
            NBTTagCompound          heightNBT           = new NBTTagCompound();
            int[]                   biomeBytes          = null;
            byte[]                  chunkBytes;
            List<NBTTagCompound>    titleEntityNBT      = new ArrayList<>();
            boolean                 isFullChunk         = existChunkSection == 65535;
            BiomeStorage            biomeStorage        = chunk.getBiomeIndex();


            // ?????????????????????
            HeightMap.Type          heightType;
            HeightMap               heightMap;
            for (Map.Entry<HeightMap.Type, HeightMap> entry : chunk.f()) {
                heightType          = entry.getKey();
                heightMap           = entry.getValue();
                if (heightType.b())
                    heightNBT.set(heightType.a(), new NBTTagLongArray(heightMap.a()));
            }


            // ???????????????
            if (isFullChunk && biomeStorage != null)
                biomeBytes          = biomeStorage.a();


            // ??????????????????????????????
            chunkBytes          = new byte[this.getTotalByteLength(chunk, existChunkSection, isFullChunk)];
            existDataSection    = this.getExistDataSection(new PacketDataSerializer(this.createChunkByteBuf(chunkBytes)), chunk, existChunkSection, isFullChunk);


            if (needTile) {
                // ????????????????????????
                TileEntity              tileEntity;
                int                     sectionY;
                BlockPosition           blockPosition;
                for (Map.Entry<BlockPosition, TileEntity> entry : chunk.getTileEntities().entrySet()) {
                    blockPosition       = entry.getKey();
                    tileEntity          = entry.getValue();
                    sectionY            = blockPosition.getY() >> 4;
                    if (!isFullChunk && (existChunkSection & 1 << sectionY) == 0)
                        continue;
                    titleEntityNBT.add(tileEntity.b());
                }
            }


            // ???????????????????????????
            field_positionX         .set(packet, positionX);
            field_positionZ         .set(packet, positionZ);
            field_existDataSection  .set(packet, existDataSection);
            field_heightNBT         .set(packet, heightNBT);
            field_biomeBytes        .set(packet, biomeBytes);
            field_chunkBytes        .set(packet, chunkBytes);
            field_titleEntityNBT    .set(packet, titleEntityNBT);
            field_isFullChunk       .set(packet, isFullChunk);


            return packet;

        } catch (Exception exception) {
            exception.printStackTrace();
            // ????????????, ????????????????????????
            return new PacketPlayOutMapChunk(chunk, existChunkSection);
        }
    }



    private ByteBuf createChunkByteBuf(byte[] chunkBytes) {
        ByteBuf         byteBuf             = Unpooled.wrappedBuffer(chunkBytes);
        byteBuf.writerIndex(0);
        return byteBuf;
    }



    private int getExistDataSection(PacketDataSerializer dataSerializer, Chunk chunk, int existChunkSection, boolean isFullChunk) {
        int             existDataSection    = 0;                    // ??? existChunkSection ????????????????????????
        ChunkSection[]  chunkSections       = chunk.getSections();
        ChunkSection    chunkSection;

        for(int length = chunkSections.length, sectionY = 0 ; sectionY < length; ++sectionY) {
            chunkSection = chunkSections[sectionY];
            if (chunkSection != Chunk.a && (!isFullChunk || !chunkSection.c()) && (existChunkSection & 1 << sectionY) != 0) {
                existDataSection |= 1 << sectionY;
                chunkSection.b(dataSerializer);
            }
        }

        return existDataSection;
    }



    private int getTotalByteLength(Chunk chunk, int existChunkSection, boolean isFullChunk) {
        int             byteLength          = 0;
        ChunkSection[]  chunkSections       = chunk.getSections();
        ChunkSection    chunkSection;

        for(int length = chunkSections.length, sectionY = 0 ; sectionY < length; ++sectionY) {
            chunkSection = chunkSections[sectionY];
            if (chunkSection != Chunk.a && (!isFullChunk || !chunkSection.c()) && (existChunkSection & 1 << sectionY) != 0)
                byteLength += chunkSection.j();
        }

        return byteLength;
    }

}
