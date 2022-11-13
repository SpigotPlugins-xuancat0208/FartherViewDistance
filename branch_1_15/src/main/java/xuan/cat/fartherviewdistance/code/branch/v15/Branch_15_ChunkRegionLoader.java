package xuan.cat.fartherviewdistance.code.branch.v15;

import net.minecraft.server.v1_15_R1.*;
import xuan.cat.fartherviewdistance.api.branch.BranchChunk;
import xuan.cat.fartherviewdistance.api.branch.BranchChunkLight;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;

/**
 * @see ChunkRegionLoader
 */
public final class Branch_15_ChunkRegionLoader {

    private static final int        CURRENT_DATA_VERSION                    = SharedConstants.getGameVersion().getWorldVersion();
    private static final boolean    JUST_CORRUPT_IT                         = Boolean.getBoolean("Paper.ignoreWorldDataVersion");
    private static       Field      field_ChunkSection_nonEmptyBlockCount;
    static {
        try {
            field_ChunkSection_nonEmptyBlockCount   = ChunkSection.class.getDeclaredField("nonEmptyBlockCount");
            field_ChunkSection_nonEmptyBlockCount.setAccessible(true);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static BranchChunk.Status loadStatus(NBTTagCompound chunkNBT) {
        NBTTagCompound                  levelNBT            = chunkNBT.getCompound("Level");
        return Branch_15_Chunk.ofStatus(ChunkStatus.a(levelNBT.getString("Status")));

    }


    public static BranchChunk loadChunk(WorldServer worldServer, ChunkCoordIntPair chunkLocation, NBTTagCompound chunkNBT, boolean integralHeightmap) {
        ChunkGenerator<?>               chunkGenerator      = worldServer.getChunkProvider().getChunkGenerator();
        if (chunkNBT.hasKeyOfType("DataVersion", 99)) {
            int dataVersion = chunkNBT.getInt("DataVersion");
            if (!JUST_CORRUPT_IT && dataVersion > CURRENT_DATA_VERSION) {
                (new RuntimeException("Server attempted to load chunk saved with newer version of minecraft! " + dataVersion + " > " + CURRENT_DATA_VERSION)).printStackTrace();
                System.exit(1);
            }
        }

        WorldChunkManager               chunkManager        = chunkGenerator.getWorldChunkManager();
        NBTTagCompound                  levelNBT            = chunkNBT.getCompound("Level");
        ChunkCoordIntPair               locationNBT         = new ChunkCoordIntPair(levelNBT.getInt("xPos"), levelNBT.getInt("zPos"));
        if (!Objects.equals(chunkLocation, locationNBT)) {
//            LOGGER.error("Chunk file at {} is in the wrong location; relocating. (Expected {}, got {})", chunkLocation, chunkLocation, locationNBT);
        }

        BiomeStorage                    biomeStorage        = new BiomeStorage(chunkLocation, chunkManager, levelNBT.hasKeyOfType("Biomes", 11) ? levelNBT.getIntArray("Biomes") : null);
        ChunkConverter                  chunkConverter      = levelNBT.hasKeyOfType("UpgradeData", 10) ? new ChunkConverter(levelNBT.getCompound("UpgradeData")) : ChunkConverter.a;
        ProtoChunkTickList<Block>       tickListBlockProto  = new ProtoChunkTickList<>((block) -> block == null || block.getBlockData().isAir(), chunkLocation, levelNBT.getList("ToBeTicked", 9));
        ProtoChunkTickList<FluidType>   tickListFluidProto  = new ProtoChunkTickList<>((fluidType) -> fluidType == null || fluidType == FluidTypes.EMPTY, chunkLocation, levelNBT.getList("LiquidsToBeTicked", 9));
        boolean                         isLightOn           = levelNBT.getBoolean("isLightOn");
        NBTTagList                      sectionsListNBT     = levelNBT.getList("Sections", 10);
        ChunkSection[]                  sections            = new ChunkSection[16];
        ChunkProviderServer             providerServer      = worldServer.getChunkProvider();
        LightEngine                     lightEngine         = providerServer.getLightEngine();

        for(int index = 0; index < sectionsListNBT.size(); ++index) {
            NBTTagCompound                  sectionNBT          = sectionsListNBT.getCompound(index);
            byte                            sectionY            = sectionNBT.getByte("Y");
            if (sectionNBT.hasKeyOfType("Palette", 9) && sectionNBT.hasKeyOfType("BlockStates", 12)) {
                ChunkSection                    section;
                try {
                    // 適用於 paper
                    section = new ChunkSection(sectionY << 4, null, worldServer, false);
                } catch (NoSuchMethodError noSuchMethodError) {
                    // 適用於 spigot (不推薦)
                    section = new ChunkSection(sectionY << 4);
                }
                section.getBlocks().a(sectionNBT.getList("Palette", 10), sectionNBT.getLongArray("BlockStates"));
                Branch_15_Chunk.recalculateBlockCounts(section);
                if (!section.c()) {
                    sections[sectionY] = section;
                }
            }
        }

        long                            inhabitedTime       = levelNBT.getLong("InhabitedTime");
        ChunkStatus.Type                statusType          = getStatusType(chunkNBT);
        IChunkAccess                    chunkAccess;
        if (statusType == ChunkStatus.Type.LEVELCHUNK) {
            TickList<Block>                 tickListBlock       = levelNBT.hasKeyOfType("TileTicks", 9) ? TickListChunk.a(levelNBT.getList("TileTicks", 10), IRegistry.BLOCK::getKey, IRegistry.BLOCK::get) : tickListBlockProto;
            TickList<FluidType>             tickListFluid       = levelNBT.hasKeyOfType("LiquidTicks", 9) ? TickListChunk.a(levelNBT.getList("LiquidTicks", 10), IRegistry.FLUID::getKey, IRegistry.FLUID::get) : tickListFluidProto;
            Chunk                           chunk               = new Chunk(worldServer.getMinecraftWorld(), chunkLocation, biomeStorage, chunkConverter, tickListBlock, tickListFluid, inhabitedTime, sections, (asyncChunk) -> { });
            chunkAccess                                         = chunk;
            NBTTagList                      tileEntitiesNBT     = levelNBT.getList("TileEntities", 10);
            for(int index = 0, size = tileEntitiesNBT.size() ; index < size ; ++index) {
                NBTTagCompound                  tileEntityNBT       = tileEntitiesNBT.getCompound(index);
                boolean                         keepPacked          = tileEntityNBT.getBoolean("keepPacked");
                if (keepPacked) {
                    chunk.a(tileEntityNBT);
                } else {
                    // 阻止損壞的不正確數據資料
                    try {
                        TileEntity                      tileEntity          = TileEntity.create(tileEntityNBT);
                        if (tileEntity != null && chunkAccess.getType(tileEntity.getPosition()).getBlock() instanceof ITileEntity)
                            chunk.setTileEntity(tileEntity.getPosition(), tileEntity);
                    } catch (ClassCastException ignored) {
                    }
                }
            }
        } else {
            ProtoChunk                      protochunk;
            try {
                // 適用於 paper
                protochunk = new ProtoChunk(chunkLocation, chunkConverter, sections, tickListBlockProto, tickListFluidProto, worldServer);
            } catch (NoSuchMethodError noSuchMethodError) {
                // 適用於 spigot (不推薦)
                protochunk = new ProtoChunk(chunkLocation, chunkConverter, sections, tickListBlockProto, tickListFluidProto);
            }

            chunkAccess     = protochunk;
            protochunk.a(biomeStorage);
            protochunk.setInhabitedTime(inhabitedTime);
            protochunk.a(ChunkStatus.a(levelNBT.getString("Status")));
            if (protochunk.getChunkStatus().b(ChunkStatus.FEATURES)) {
                protochunk.a(lightEngine);
            }

            if (!isLightOn && protochunk.getChunkStatus().b(ChunkStatus.LIGHT)) {
                // 有光照
                for (BlockPosition blockPosition : BlockPosition.b(chunkLocation.d(), 0, chunkLocation.e(), chunkLocation.f(), 255, chunkLocation.g()))
                    if (chunkAccess.getType(blockPosition).h() != 0)
                        protochunk.j(blockPosition);
            }
        }
        chunkAccess.b(isLightOn);


        // 高度圖
        NBTTagCompound                  heightMapsNBT       = levelNBT.getCompound("Heightmaps");
        EnumSet<HeightMap.Type>         heightMapTypeSet    = EnumSet.noneOf(HeightMap.Type.class);
        for (HeightMap.Type heightMapType : chunkAccess.getChunkStatus().h()) {
            String                          heightMapName       = heightMapType.a();
            if (heightMapsNBT.hasKeyOfType(heightMapName, 12)) {
                chunkAccess.a(heightMapType, heightMapsNBT.getLongArray(heightMapName));
            } else {
                heightMapTypeSet.add(heightMapType);
            }
        }
        if (integralHeightmap) {
            HeightMap.a(chunkAccess, heightMapTypeSet);
        }


        return new Branch_15_Chunk(worldServer, chunkAccess);
    }

    public static BranchChunkLight loadLight(WorldServer worldServer, NBTTagCompound chunkNBT) {
        if (chunkNBT.hasKeyOfType("DataVersion", 99)) {
            int dataVersion = chunkNBT.getInt("DataVersion");
            if (!JUST_CORRUPT_IT && dataVersion > CURRENT_DATA_VERSION) {
                (new RuntimeException("Server attempted to load chunk saved with newer version of minecraft! " + dataVersion + " > " + CURRENT_DATA_VERSION)).printStackTrace();
                System.exit(1);
            }
        }

        NBTTagCompound                  levelNBT            = chunkNBT.getCompound("Level");
        boolean                         isLightOn           = levelNBT.get("isLightOn") != null;
        NBTTagList                      sectionsListNBT     = levelNBT.getList("Sections", 10);
        Branch_15_ChunkLight chunkLight          = new Branch_15_ChunkLight(worldServer);
        boolean                         hasSkyLight         = worldServer.getWorldProvider().g();

        for(int index = 0; index < sectionsListNBT.size(); ++index) {
            NBTTagCompound                  sectionNBT          = sectionsListNBT.getCompound(index);
            byte                            sectionY            = sectionNBT.getByte("Y");

            if (isLightOn) {
                if (sectionNBT.hasKeyOfType("BlockLight", 7)) {
                    chunkLight.setBlockLight(sectionY, sectionNBT.getByteArray("BlockLight"));
                }
                if (hasSkyLight && sectionNBT.hasKeyOfType("SkyLight", 7)) {
                    chunkLight.setSkyLight(sectionY, sectionNBT.getByteArray("SkyLight"));
                }
            }
        }

        return chunkLight;
    }



    public static NBTTagCompound saveChunkQuick(WorldServer worldserver, IChunkAccess chunkAccess, Branch_15_ChunkLight chunkLight, List<Runnable> asyncRunnable) {
        ChunkCoordIntPair               chunkLocation       = chunkAccess.getPos();
        NBTTagCompound                  chunkNBT            = new NBTTagCompound();
        NBTTagCompound                  levelNBT            = new NBTTagCompound();

        chunkNBT.setInt("DataVersion", SharedConstants.getGameVersion().getWorldVersion());
        chunkNBT.set("Level", levelNBT);
        levelNBT.setInt("xPos", chunkLocation.x);
        levelNBT.setInt("zPos", chunkLocation.z);
        levelNBT.setLong("LastUpdate", worldserver.getTime());
        levelNBT.setLong("InhabitedTime", chunkAccess.getInhabitedTime());
        levelNBT.setString("Status", chunkAccess.getChunkStatus().d());
        ChunkConverter                  chunkConverter      = chunkAccess.p();
        if (!chunkConverter.a()) {
            levelNBT.set("UpgradeData", chunkConverter.b());
        }

        ChunkSection[]                  sections            = chunkAccess.getSections();
        NBTTagList                      sectionListNBT      = new NBTTagList();
        LightEngineThreaded             lightenginethreaded = worldserver.getChunkProvider().getLightEngine();
        boolean                         isLightOn           = false;

        for(int index = -1; index < 17; ++index) {
            int                             sectionY            = index;
            ChunkSection                    section             = Arrays.stream(sections).filter((chunkSection) -> chunkSection != null && chunkSection.getYPosition() >> 4 == sectionY).findFirst().orElse(Chunk.a);
            NibbleArray                     blockLight          = lightenginethreaded.a(EnumSkyBlock.BLOCK).a(SectionPosition.a(chunkLocation, index));
            NibbleArray                     skyLight            = lightenginethreaded.a(EnumSkyBlock.SKY).a(SectionPosition.a(chunkLocation, index));

            if (section != Chunk.a || blockLight != null || skyLight != null) {
                NBTTagCompound                  sectionNBT          = new NBTTagCompound();
                sectionNBT.setByte("Y", (byte) (sectionY & 255));
                if (section != Chunk.a)
                    asyncRunnable.add(() -> section.getBlocks().a(sectionNBT, "Palette", "BlockStates"));

                if (blockLight != null && !blockLight.c()) {
                    isLightOn = true;
                    try {
                        // 適用於 paper
                        chunkLight.setBlockLight(sectionY, blockLight.getIfSet());
                    } catch (NoSuchMethodError noSuchMethodError) {
                        // 適用於 spigot (不推薦)
                        chunkLight.setBlockLight(sectionY, blockLight.asBytes());
                    }
                }
                if (skyLight   != null && !skyLight.c()) {
                    isLightOn = true;
                    try {
                        // 適用於 paper
                        chunkLight.setSkyLight(sectionY, skyLight.getIfSet());
                    } catch (NoSuchMethodError noSuchMethodError) {
                        // 適用於 spigot (不推薦)
                        chunkLight.setSkyLight(sectionY, skyLight.asBytes());
                    }
                }

                sectionListNBT.add(sectionNBT);
            }
        }

        levelNBT.set("Sections", sectionListNBT);
        if (isLightOn) {
            levelNBT.setBoolean("isLightOn", true);
        }

        BiomeStorage                    biomeStorage        = chunkAccess.getBiomeIndex();
        if (biomeStorage != null) {
            asyncRunnable.add(() -> levelNBT.setIntArray("Biomes", biomeStorage.a()));
        }

        NBTTagList                      tileEntitiesNBT     = new NBTTagList();
        for (BlockPosition blockPosition : chunkAccess.c()) {
            NBTTagCompound                  tileEntityNBT       = chunkAccess.f(blockPosition);
            if (tileEntityNBT != null) {
                tileEntitiesNBT.add(tileEntityNBT);
            }
        }
        levelNBT.set("TileEntities", tileEntitiesNBT);

        NBTTagCompound                  heightmapsNBT       = new NBTTagCompound();
        for (Map.Entry<HeightMap.Type, HeightMap> entry : chunkAccess.f()) {
            if (chunkAccess.getChunkStatus().h().contains(entry.getKey())) {
                heightmapsNBT.set(entry.getKey().a(), new NBTTagLongArray(entry.getValue().a()));
            }
        }
        levelNBT.set("Heightmaps", heightmapsNBT);

        return chunkNBT;
    }






    public static ChunkStatus.Type getStatusType(@Nullable NBTTagCompound nbttagcompound) {
        if (nbttagcompound != null) {
            ChunkStatus chunkstatus = ChunkStatus.a(nbttagcompound.getCompound("Level").getString("Status"));
            if (chunkstatus != null) {
                return chunkstatus.getType();
            }
        }

        return ChunkStatus.Type.PROTOCHUNK;
    }
}
