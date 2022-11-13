package xuan.cat.fartherviewdistance.code.branch.v16;

import com.google.common.collect.Lists;
import net.minecraft.server.v1_16_R3.PacketPlayOutLightUpdate;

import java.lang.reflect.Field;
import java.util.ArrayList;

public final class Branch_16_PacketHandleLightUpdate {

    private Field field_positionX           = null;
    private Field field_positionZ           = null;
    private Field field_skyMask             = null;
    private Field field_blockMask           = null;
    private Field field_emptySkyMask        = null;
    private Field field_emptyBlockMask      = null;
    private Field field_skyArrays           = null;
    private Field field_blockArrays         = null;
    private Field field_trustEdges          = null;

    private Field field_cleaner1            = null;
    private Field field_cleaner2            = null;


    public Branch_16_PacketHandleLightUpdate() {
        try {
            Class<PacketPlayOutLightUpdate> atClass = PacketPlayOutLightUpdate.class;
            field_positionX         = atClass.getDeclaredField("a");
            field_positionZ         = atClass.getDeclaredField("b");
            field_skyMask           = atClass.getDeclaredField("c");
            field_blockMask         = atClass.getDeclaredField("d");
            field_emptySkyMask      = atClass.getDeclaredField("e");
            field_emptyBlockMask    = atClass.getDeclaredField("f");
            field_skyArrays         = atClass.getDeclaredField("g");
            field_blockArrays       = atClass.getDeclaredField("h");
            field_trustEdges        = atClass.getDeclaredField("i");

            field_positionX         .setAccessible(true);
            field_positionZ         .setAccessible(true);
            field_skyMask           .setAccessible(true);
            field_blockMask         .setAccessible(true);
            field_emptySkyMask      .setAccessible(true);
            field_emptyBlockMask    .setAccessible(true);
            field_skyArrays         .setAccessible(true);
            field_blockArrays       .setAccessible(true);
            field_trustEdges        .setAccessible(true);

            try {
                field_cleaner1          = atClass.getDeclaredField("cleaner1");
                field_cleaner2          = atClass.getDeclaredField("cleaner2");

                field_cleaner1          .setAccessible(true);
                field_cleaner2          .setAccessible(true);
            } catch (Exception ignored) {
            }

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }


    public PacketPlayOutLightUpdate createLightUpdatePacket(int chunkX, int chunkZ, Branch_16_ChunkLight light) {
        try {
            PacketPlayOutLightUpdate    packet          = new PacketPlayOutLightUpdate();
            ArrayList<byte[]>           skyArrays       = Lists.newArrayList();
            ArrayList<byte[]>           blockArrays     = Lists.newArrayList();
            int                         skyMask         = 0;
            int                         blockMask       = 0;
            int                         emptySkyMask    = 0;
            int                         emptyBlockMask  = 0;

            for(int sectionY = 0; sectionY < 18; ++sectionY) {
                byte[]                      skyLight        = light.getSkyLights()[sectionY];
                byte[]                      blockLight      = light.getBlockLights()[sectionY];
                if (skyLight != Branch_16_ChunkLight.EMPTY) {
                    if (skyLight == null) {
                        emptySkyMask    |= 1 << sectionY;
                    } else {
                        skyMask         |= 1 << sectionY;
                        skyArrays.add(skyLight);
                    }
                }
                if (blockLight != Branch_16_ChunkLight.EMPTY) {
                    if (blockLight == null) {
                        emptyBlockMask  |= 1 << sectionY;
                    } else {
                        blockMask       |= 1 << sectionY;
                        blockArrays.add(blockLight);
                    }
                }
            }

            field_positionX     .setInt(packet, chunkX);
            field_positionZ     .setInt(packet, chunkZ);
            field_skyMask       .setInt(packet, skyMask);
            field_blockMask     .setInt(packet, blockMask);
            field_emptySkyMask  .setInt(packet, emptySkyMask);
            field_emptyBlockMask.setInt(packet, emptyBlockMask);
            field_skyArrays     .set(packet, skyArrays);
            field_blockArrays   .set(packet, blockArrays);
            field_trustEdges    .setBoolean(packet, true);

            if (field_cleaner1 != null)
                field_cleaner1.set(packet, (Runnable) () -> { });
            if (field_cleaner2 != null)
                field_cleaner2.set(packet, (Runnable) () -> { });

            return packet;

        } catch (Exception exception) {
            exception.printStackTrace();
            // 發生錯誤, 直接使用原版方式
            return null;
        }
    }
}
