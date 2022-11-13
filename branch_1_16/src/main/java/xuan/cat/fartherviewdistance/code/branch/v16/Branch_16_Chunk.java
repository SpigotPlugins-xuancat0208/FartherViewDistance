package xuan.cat.fartherviewdistance.code.branch.v16;

import net.minecraft.server.v1_16_R3.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_16_R3.block.data.CraftBlockData;
import org.bukkit.util.Vector;
import xuan.cat.fartherviewdistance.api.branch.BranchChunk;
import xuan.cat.fartherviewdistance.api.branch.BranchChunkLight;
import xuan.cat.fartherviewdistance.api.branch.BranchNBT;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class Branch_16_Chunk implements BranchChunk {
    private final IChunkAccess  chunkAccess;
    private final WorldServer   worldServer;


    public Branch_16_Chunk(WorldServer worldServer, IChunkAccess chunkAccess) {
        this.chunkAccess = chunkAccess;
        this.worldServer = worldServer;
    }


    public BranchNBT toNBT(BranchChunkLight light, List<Runnable> asyncRunnable) {
        return new Branch_16_NBT(Branch_16_ChunkRegionLoader.saveChunkQuick(worldServer, chunkAccess, (Branch_16_ChunkLight) light, asyncRunnable));
    }


    public org.bukkit.Chunk getChunk() {
        IChunkAccess chunk = chunkAccess;
        if (chunk instanceof ChunkEmpty)
            return ((ChunkEmpty) chunk).bukkitChunk;
        else if (chunk instanceof Chunk)
            return ((Chunk) chunk).bukkitChunk;
        else if (chunk instanceof ProtoChunk)
            return new Chunk(worldServer, ((ProtoChunk) chunk)).getBukkitChunk();
        else
            return null;
    }

    public org.bukkit.World getWorld() {
        return worldServer.getWorld();
    }


    public IBlockData getIBlockData(int x, int y, int z) {
        int indexY = (y >> 4);
        ChunkSection[] chunkSections = chunkAccess.getSections();
        if (indexY >= 0 && indexY < chunkSections.length) {
            ChunkSection chunkSection = chunkSections[indexY];
            if (chunkSection != null && !chunkSection.c()) // hasOnlyAir
                return chunkSection.getType(x & 15, y & 15, z & 15);
        }
        return Blocks.AIR.getBlockData();
    }
    public void setIBlockData(int x, int y, int z, IBlockData iBlockData) {
        int indexY = (y >> 4);
        ChunkSection[] chunkSections = chunkAccess.getSections();
        if (indexY >= 0 && indexY < chunkSections.length) {
            ChunkSection chunkSection = chunkSections[indexY];
            if (chunkSection == null)
                chunkSection = chunkSections[indexY] = new ChunkSection(indexY);
            chunkSection.setType(x & 15, y & 15, z & 15, iBlockData, false);
        }
    }


    public boolean equalsBlockData(int x, int y, int z, BlockData blockData) {
        return equalsBlockData(x, y, z, ((CraftBlockData) blockData).getState());
    }
    public boolean equalsBlockData(int x, int y, int z, IBlockData other) {
        IBlockData state = getIBlockData(x, y, z);
        return state != null && state.equals(other);
    }


    public BlockData getBlockData(int x, int y, int z) {
        IBlockData blockData = getIBlockData(x, y, z);
        return blockData != null ? CraftBlockData.fromData(blockData) : CraftBlockData.fromData(Blocks.AIR.getBlockData());
    }


    public void setBlockData(int x, int y, int z, BlockData blockData) {
        IBlockData iBlockData = ((CraftBlockData) blockData).getState();
        if (iBlockData != null)
            setIBlockData(x, y, z, iBlockData);
    }


    public Map<Vector, BlockData> getBlockDataMap() {
        Map<Vector, BlockData> vectorBlockDataMap = new HashMap<>();
        int maxHeight = 0;// worldServer.getMaxBuildHeight();
        int minHeight = 256;//worldServer.getMinBuildHeight();
        for (int x = 0; x < 16; x++) {
            for (int y = minHeight; y < maxHeight; y++) {
                for (int z = 0; z < 16; z++) {
                    BlockData blockData = this.getBlockData(x, y, z);
                    org.bukkit.Material material = blockData.getMaterial();
                    if (material != org.bukkit.Material.AIR && material != org.bukkit.Material.VOID_AIR && material != org.bukkit.Material.CAVE_AIR) {
                        vectorBlockDataMap.put(new Vector(x, y, z), blockData);
                    }
                }
            }
        }

        return vectorBlockDataMap;
    }


    public int getX() {
        return chunkAccess.getPos().x;
    }

    public int getZ() {
        return chunkAccess.getPos().z;
    }


    private static Field field_LevelChunkSection_nonEmptyBlockCount;
    static {
        try {
            field_LevelChunkSection_nonEmptyBlockCount = ChunkSection.class.getDeclaredField("nonEmptyBlockCount");
            field_LevelChunkSection_nonEmptyBlockCount.setAccessible(true);
        } catch (NoSuchFieldException exception) {
            exception.printStackTrace();
        }
    }


    public static void recalculateBlockCounts(ChunkSection section) {
        AtomicInteger counts = new AtomicInteger();
        DataPaletteBlock<IBlockData> blocks = section.getBlocks();
        DataPaletteBlock.a<IBlockData> forEachLocation = (state, location) -> {
            if (state == null)
                return;
            if (!state.isAir())
                counts.incrementAndGet();
            Fluid fluid = state.getFluid();
            if (!fluid.isEmpty())
                counts.incrementAndGet();
        };
        try {
            // 適用於 paper
            blocks.forEachLocation(forEachLocation);
        } catch (NoSuchMethodError noSuchMethodError) {
            // 適用於 spigot (不推薦)
            blocks.a(forEachLocation);
        }
        try {
            field_LevelChunkSection_nonEmptyBlockCount.set(section, counts.shortValue());
        } catch (IllegalAccessException exception) {
            exception.printStackTrace();
        }
    }

    public void replaceAllMaterial(BlockData[] target, BlockData to) {
        Map<Block, IBlockData> targetMap = new HashMap<>();
        for (BlockData targetData : target) {
            IBlockData targetState = ((CraftBlockData) targetData).getState();
            targetMap.put(targetState.getBlock(), targetState);
        }
        IBlockData toI = ((CraftBlockData) to).getState();
        for (ChunkSection section :chunkAccess.getSections()) {
            if (section != Chunk.a) {
                AtomicInteger counts = new AtomicInteger();
                DataPaletteBlock<IBlockData> blocks = section.getBlocks();
                List<Integer> conversionLocationList = new ArrayList<>();
                DataPaletteBlock.a<IBlockData> forEachLocation = (state, location) -> {
                    if (state == null)
                        return;
                    IBlockData targetState = targetMap.get(state.getBlock());
                    if (targetState != null) {
                        conversionLocationList.add(location);
                        state = toI;
                    }
                    if (!state.isAir())
                        counts.incrementAndGet();
                    Fluid fluid = state.getFluid();
                    if (!fluid.isEmpty())
                        counts.incrementAndGet();
                };
                try {
                    // 適用於 paper
                    blocks.forEachLocation(forEachLocation);
                } catch (NoSuchMethodError noSuchMethodError) {
                    // 適用於 spigot (不推薦)
                    blocks.a(forEachLocation);
                }
                conversionLocationList.forEach(location -> {
                    blocks.b(location & 15, location >> 8 & 15, location >> 4 & 15, toI);

                });
                try {
                    field_LevelChunkSection_nonEmptyBlockCount.set(section, counts.shortValue());
                } catch (IllegalAccessException exception) {
                    exception.printStackTrace();
                }
            }
        }
    }


    public org.bukkit.Material getMaterial(int x, int y, int z) {
        return getBlockData(x, y, z).getMaterial();
    }

    public void setMaterial(int x, int y, int z, org.bukkit.Material material) {
        setBlockData(x, y, z, material.createBlockData());
    }


    @Deprecated
    public org.bukkit.block.Biome getBiome(int x, int z) {
        return this.getBiome(x, 0, z);
    }

    public org.bukkit.block.Biome getBiome(int x, int y, int z) {
        BiomeStorage biomeStorage = chunkAccess.getBiomeIndex();
        return biomeStorage != null ? CraftBlock.biomeBaseToBiome(worldServer.r().b(IRegistry.ay), biomeStorage.getBiome(x, y, z)) : null;
    }

    @Deprecated
    public void setBiome(int x, int z, org.bukkit.block.Biome biome) {
        setBiome(x, 0, z, biome);
    }
    public void setBiome(int x, int y, int z, org.bukkit.block.Biome biome) {
        BiomeStorage biomeStorage = chunkAccess.getBiomeIndex();
        if (biomeStorage != null)
            biomeStorage.setBiome(x, y, z, CraftBlock.biomeToBiomeBase(worldServer.r().b(IRegistry.ay), biome));
    }

    public boolean hasFluid(int x, int y, int z) {
        return !getIBlockData(x, y, z).getFluid().isEmpty();
    }
    public boolean isAir(int x, int y, int z) {
        return getIBlockData(x, y, z).isAir();
    }

    public int getHighestY(int x, int z) {
        return chunkAccess.getHighestBlock(HeightMap.Type.MOTION_BLOCKING, x, z);
    }


    public static Status ofStatus(ChunkStatus chunkStatus) {
        if (chunkStatus == ChunkStatus.EMPTY) {
            return Status.EMPTY;
        } else if (chunkStatus == ChunkStatus.STRUCTURE_STARTS) {
            return Status.STRUCTURE_STARTS;
        } else if (chunkStatus == ChunkStatus.STRUCTURE_REFERENCES) {
            return Status.STRUCTURE_REFERENCES;
        } else if (chunkStatus == ChunkStatus.BIOMES) {
            return Status.BIOMES;
        } else if (chunkStatus == ChunkStatus.NOISE) {
            return Status.NOISE;
        } else if (chunkStatus == ChunkStatus.SURFACE) {
            return Status.SURFACE;
        } else if (chunkStatus == ChunkStatus.CARVERS) {
            return Status.CARVERS;
        } else if (chunkStatus == ChunkStatus.LIQUID_CARVERS) {
            return Status.LIQUID_CARVERS;
        } else if (chunkStatus == ChunkStatus.FEATURES) {
            return Status.FEATURES;
        } else if (chunkStatus == ChunkStatus.LIGHT) {
            return Status.LIGHT;
        } else if (chunkStatus == ChunkStatus.SPAWN) {
            return Status.SPAWN;
        } else if (chunkStatus == ChunkStatus.HEIGHTMAPS) {
            return Status.HEIGHTMAPS;
        } else if (chunkStatus == ChunkStatus.FULL) {
            return Status.FULL;
        }
        return Status.EMPTY;
    }
    public Status getStatus() {
        return ofStatus(chunkAccess.getChunkStatus());
    }
}
