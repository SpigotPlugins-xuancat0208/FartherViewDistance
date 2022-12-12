package xuan.cat.fartherviewdistance.code.branch.v19;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ProtoChunkTicks;
import xuan.cat.fartherviewdistance.api.branch.BranchChunk;
import xuan.cat.fartherviewdistance.api.branch.BranchChunkLight;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @see ChunkSerializer
 * 參考 XuanCatAPI.CodeExtendChunkLight
 */
public final class Branch_19_ChunkRegionLoader {
    private static final int        CURRENT_DATA_VERSION    = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
    private static final boolean    JUST_CORRUPT_IT         = Boolean.getBoolean("Paper.ignoreWorldDataVersion");


    public static BranchChunk.Status loadStatus(CompoundTag nbt) {
        try {
            // 適用於 paper
            return Branch_19_Chunk.ofStatus(ChunkStatus.getStatus(nbt.getString("Status")));
        } catch (NoSuchMethodError noSuchMethodError) {
            // 適用於 spigot (不推薦)
            return Branch_19_Chunk.ofStatus(ChunkStatus.byName(nbt.getString("Status")));
        }
    }

    private static Codec<PalettedContainerRO<Holder<Biome>>> makeBiomeCodec(Registry<Biome> biomeRegistry) {
        return PalettedContainer.codecRO(biomeRegistry.asHolderIdMap(), biomeRegistry.holderByNameCodec(), PalettedContainer.Strategy.SECTION_BIOMES, biomeRegistry.getHolderOrThrow(Biomes.PLAINS));
    }


    private static Method method_ChunkSerializer_makeBiomeCodecRW;
    static {
        try {
            method_ChunkSerializer_makeBiomeCodecRW = ChunkSerializer.class.getDeclaredMethod("makeBiomeCodecRW", Registry.class);
            method_ChunkSerializer_makeBiomeCodecRW.setAccessible(true);
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }

    }
    private static Codec<PalettedContainer<Holder<Biome>>> makeBiomeCodecRW(Registry<Biome> biomeRegistry) {
        try {
            return (Codec<PalettedContainer<Holder<Biome>>>) method_ChunkSerializer_makeBiomeCodecRW.invoke(null, biomeRegistry);
        } catch (InvocationTargetException | IllegalAccessException ex) {
            ex.printStackTrace();
            return null;
        }
    }


    public static BranchChunk loadChunk(ServerLevel world, int chunkX, int chunkZ, CompoundTag nbt, boolean integralHeightmap) {
        if (nbt.contains("DataVersion", 99)) {
            int dataVersion = nbt.getInt("DataVersion");
            if (!JUST_CORRUPT_IT && dataVersion > CURRENT_DATA_VERSION) {
                (new RuntimeException("Server attempted to load chunk saved with newer version of minecraft! " + dataVersion + " > " + CURRENT_DATA_VERSION)).printStackTrace();
                System.exit(1);
            }
        }

        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        UpgradeData upgradeData = nbt.contains("UpgradeData", 10) ? new UpgradeData(nbt.getCompound("UpgradeData"), world) : UpgradeData.EMPTY;
        boolean isLightOn = Objects.requireNonNullElse(ChunkStatus.byName(nbt.getString("Status")), ChunkStatus.EMPTY).isOrAfter(ChunkStatus.LIGHT) && (nbt.get("isLightOn") != null || nbt.getInt("starlight.light_version") == 6);
        ListTag sectionArrayNBT = nbt.getList("sections", 10);
        int sectionsCount = world.getSectionsCount();
        LevelChunkSection[] sections = new LevelChunkSection[sectionsCount];
        ServerChunkCache chunkSource = world.getChunkSource();
        LevelLightEngine lightEngine = chunkSource.getLightEngine();
        Registry<Biome> biomeRegistry = world.registryAccess().registryOrThrow(Registries.BIOME);
        Codec<PalettedContainer<Holder<Biome>>> paletteCodec = makeBiomeCodecRW(biomeRegistry);
        for(int sectionIndex = 0; sectionIndex < sectionArrayNBT.size(); ++sectionIndex) {
            CompoundTag sectionNBT = sectionArrayNBT.getCompound(sectionIndex);
            byte locationY = sectionNBT.getByte("Y");
            int sectionY = world.getSectionIndexFromSectionY(locationY);
            if (sectionY >= 0 && sectionY < sections.length) {
                // 方塊轉換器
                PalettedContainer<BlockState> paletteBlock;
                if (sectionNBT.contains("block_states", 10)) {
                    paletteBlock = ChunkSerializer.BLOCK_STATE_CODEC.parse(NbtOps.INSTANCE, sectionNBT.getCompound("block_states")).promotePartial((sx) -> {}).getOrThrow(false, (message) -> {});
                } else {
                    paletteBlock = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES);
                }

                // 生態轉換器
                PalettedContainer<Holder<Biome>> paletteBiome;
                if (sectionNBT.contains("biomes", 10)) {
                    paletteBiome = paletteCodec.parse(NbtOps.INSTANCE, sectionNBT.getCompound("biomes")).promotePartial((sx) -> {}).getOrThrow(false, (message) -> {});
                } else {
                    try {
                        // 適用於 paper
                        paletteBiome = new PalettedContainer<>(biomeRegistry.asHolderIdMap(), biomeRegistry.getHolderOrThrow(Biomes.PLAINS), PalettedContainer.Strategy.SECTION_BIOMES, null);
                    } catch (NoSuchMethodError noSuchMethodError) {
                        // 適用於 spigot (不推薦)
                        paletteBiome = new PalettedContainer<>(biomeRegistry.asHolderIdMap(), biomeRegistry.getHolderOrThrow(Biomes.PLAINS), PalettedContainer.Strategy.SECTION_BIOMES);
                    }
                }

                LevelChunkSection chunkSection = new LevelChunkSection(locationY, paletteBlock, paletteBiome);
                sections[sectionY] = chunkSection;
            }
        }

        long inhabitedTime = nbt.getLong("InhabitedTime");
        ChunkStatus.ChunkType chunkType = ChunkSerializer.getChunkTypeFromTag(nbt);
        BlendingData blendingData;
        if (nbt.contains("blending_data", 10)) {
            blendingData = BlendingData.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, nbt.getCompound("blending_data"))).resultOrPartial((sx) -> {}).orElse(null);
        } else {
            blendingData = null;
        }

        ChunkAccess chunk;
        if (chunkType == ChunkStatus.ChunkType.LEVELCHUNK) {
            LevelChunkTicks<Block> ticksBlock = LevelChunkTicks.load(nbt.getList("block_ticks", 10), (sx) -> BuiltInRegistries.BLOCK.getOptional(ResourceLocation.tryParse(sx)), chunkPos);
            LevelChunkTicks<Fluid> ticksFluid = LevelChunkTicks.load(nbt.getList("fluid_ticks", 10), (sx) -> BuiltInRegistries.FLUID.getOptional(ResourceLocation.tryParse(sx)), chunkPos);
            LevelChunk levelChunk = new LevelChunk(world.getLevel(), chunkPos, upgradeData, ticksBlock, ticksFluid, inhabitedTime, sections, null, blendingData);
            chunk = levelChunk;

            // 實體方塊
            ListTag blockEntities = nbt.getList("block_entities", 10);
            for(int entityIndex = 0; entityIndex < blockEntities.size(); ++entityIndex) {
                CompoundTag entityNBT = blockEntities.getCompound(entityIndex);
                boolean keepPacked = entityNBT.getBoolean("keepPacked");
                if (keepPacked) {
                    chunk.setBlockEntityNbt(entityNBT);
                } else {
                    BlockPos blockPos = BlockEntity.getPosFromTag(entityNBT);
                    BlockEntity blockEntity = BlockEntity.loadStatic(blockPos, chunk.getBlockState(blockPos), entityNBT);
                    if (blockEntity != null) {
                        levelChunk.getBlockEntities().put(blockPos, blockEntity);
                    }
                }
            }
        } else {
            ProtoChunkTicks<Block> ticksBlock = ProtoChunkTicks.load(nbt.getList("block_ticks", 10), (sx) -> BuiltInRegistries.BLOCK.getOptional(ResourceLocation.tryParse(sx)), chunkPos);
            ProtoChunkTicks<Fluid> ticksFluid = ProtoChunkTicks.load(nbt.getList("fluid_ticks", 10), (sx) -> BuiltInRegistries.FLUID.getOptional(ResourceLocation.tryParse(sx)), chunkPos);
            ProtoChunk protochunk = new ProtoChunk(chunkPos, upgradeData, sections, ticksBlock, ticksFluid, world, biomeRegistry, blendingData);
            chunk = protochunk;
            protochunk.setInhabitedTime(inhabitedTime);
            if (nbt.contains("below_zero_retrogen", 10)) {
                BelowZeroRetrogen.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, nbt.getCompound("below_zero_retrogen"))).resultOrPartial((sx) -> {}).ifPresent(protochunk::setBelowZeroRetrogen);
            }

            ChunkStatus chunkStatus = ChunkStatus.byName(nbt.getString("Status"));
            protochunk.setStatus(chunkStatus);
            if (chunkStatus.isOrAfter(ChunkStatus.FEATURES)) {
                protochunk.setLightEngine(lightEngine);
            }
        }
        chunk.setLightCorrect(isLightOn);

        // 高度圖
        CompoundTag heightmapsNBT = nbt.getCompound("Heightmaps");
        EnumSet<Heightmap.Types> enumHeightmapType = EnumSet.noneOf(Heightmap.Types.class);
        for (Heightmap.Types heightmapTypes : chunk.getStatus().heightmapsAfter()) {
            String serializationKey = heightmapTypes.getSerializationKey();
            if (heightmapsNBT.contains(serializationKey, 12)) {
                chunk.setHeightmap(heightmapTypes, heightmapsNBT.getLongArray(serializationKey));
            } else {
                enumHeightmapType.add(heightmapTypes);
            }
        }
        if (integralHeightmap) {
            Heightmap.primeHeightmaps(chunk, enumHeightmapType);
        }

        ListTag processListNBT = nbt.getList("PostProcessing", 9);
        for(int indexList = 0; indexList < processListNBT.size(); ++indexList) {
            ListTag processNBT = processListNBT.getList(indexList);
            for (int index = 0; index < processNBT.size(); ++index) {
                chunk.addPackedPostProcess(processNBT.getShort(index), indexList);
            }
        }

        if (chunkType == ChunkStatus.ChunkType.LEVELCHUNK) {
            return new Branch_19_Chunk(world, chunk);
        } else {
            ProtoChunk protoChunk = (ProtoChunk) chunk;
            return new Branch_19_Chunk(world, protoChunk);
        }
    }

    public static BranchChunkLight loadLight(ServerLevel world, CompoundTag nbt) {
        // 檢查資料版本
        if (nbt.contains("DataVersion", 99)) {
            int dataVersion = nbt.getInt("DataVersion");
            if (!JUST_CORRUPT_IT && dataVersion > CURRENT_DATA_VERSION) {
                (new RuntimeException("Server attempted to load chunk saved with newer version of minecraft! " + dataVersion + " > " + CURRENT_DATA_VERSION)).printStackTrace();
                System.exit(1);
            }
        }

        boolean isLightOn = Objects.requireNonNullElse(ChunkStatus.byName(nbt.getString("Status")), ChunkStatus.EMPTY).isOrAfter(ChunkStatus.LIGHT) && (nbt.get("isLightOn") != null || nbt.getInt("starlight.light_version") == 6);
        boolean hasSkyLight = world.dimensionType().hasSkyLight();
        ListTag sectionArrayNBT = nbt.getList("sections", 10);
        Branch_19_ChunkLight chunkLight = new Branch_19_ChunkLight(world);
        for(int sectionIndex = 0; sectionIndex < sectionArrayNBT.size(); ++sectionIndex) {
            CompoundTag sectionNBT = sectionArrayNBT.getCompound(sectionIndex);
            byte locationY = sectionNBT.getByte("Y");
            if (isLightOn) {
                if (sectionNBT.contains("BlockLight", 7)) {
                    chunkLight.setBlockLight(locationY, sectionNBT.getByteArray("BlockLight"));
                }
                if (hasSkyLight) {
                    if (sectionNBT.contains("SkyLight", 7)) {
                        chunkLight.setSkyLight(locationY, sectionNBT.getByteArray("SkyLight"));
                    }
                }
            }
        }

        return chunkLight;
    }



    public static CompoundTag saveChunk(ServerLevel world, ChunkAccess chunk, Branch_19_ChunkLight light, List<Runnable> asyncRunnable) {
        int minSection = world.getMinSection() - 1;//WorldUtil.getMinLightSection();
        ChunkPos chunkPos = chunk.getPos();
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());
        nbt.putInt("xPos", chunkPos.x);
        nbt.putInt("yPos", chunk.getMinSection());
        nbt.putInt("zPos", chunkPos.z);
        nbt.putLong("LastUpdate", world.getGameTime());
        nbt.putLong("InhabitedTime", chunk.getInhabitedTime());
        nbt.putString("Status", chunk.getStatus().getName());
        BlendingData blendingData = chunk.getBlendingData();
        if (blendingData != null) {
            BlendingData.CODEC.encodeStart(NbtOps.INSTANCE, blendingData).resultOrPartial((sx) -> {}).ifPresent((nbtData) -> nbt.put("blending_data", nbtData));
        }

        BelowZeroRetrogen belowZeroRetrogen = chunk.getBelowZeroRetrogen();
        if (belowZeroRetrogen != null) {
            BelowZeroRetrogen.CODEC.encodeStart(NbtOps.INSTANCE, belowZeroRetrogen).resultOrPartial((sx) -> {}).ifPresent((nbtData) -> nbt.put("below_zero_retrogen", nbtData));
        }

        LevelChunkSection[] chunkSections = chunk.getSections();
        ListTag sectionArrayNBT = new ListTag();
        ThreadedLevelLightEngine lightEngine = world.getChunkSource().getLightEngine();

        // 生態解析器
        Registry<Biome> biomeRegistry = world.registryAccess().registryOrThrow(Registries.BIOME);
        Codec<PalettedContainerRO<Holder<Biome>>> paletteCodec = makeBiomeCodec(biomeRegistry);
        boolean lightCorrect = false;

        for(int locationY = lightEngine.getMinLightSection(); locationY < lightEngine.getMaxLightSection(); ++locationY) {
            int sectionY = chunk.getSectionIndexFromSectionY(locationY);
            boolean inSections = sectionY >= 0 && sectionY < chunkSections.length;
            ThreadedLevelLightEngine lightEngineThreaded = world.getChunkSource().getLightEngine();
            DataLayer blockNibble;
            DataLayer skyNibble;
            try {
                // 適用於 paper
                blockNibble = chunk.getBlockNibbles()[locationY - minSection].toVanillaNibble();
                skyNibble = chunk.getSkyNibbles()[locationY - minSection].toVanillaNibble();
            } catch (NoSuchMethodError noSuchMethodError) {
                // 適用於 spigot (不推薦)
                blockNibble = lightEngineThreaded.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(chunkPos, locationY));
                skyNibble = lightEngineThreaded.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(chunkPos, locationY));
            }

            if (inSections || blockNibble != null || skyNibble != null) {
                CompoundTag sectionNBT = new CompoundTag();
                if (inSections) {
                    LevelChunkSection chunkSection = chunkSections[sectionY];
                    asyncRunnable.add(() -> {
                        sectionNBT.put("block_states", ChunkSerializer.BLOCK_STATE_CODEC.encodeStart(NbtOps.INSTANCE, chunkSection.getStates()).getOrThrow(false, (message) -> {}));
                        sectionNBT.put("biomes", paletteCodec.encodeStart(NbtOps.INSTANCE, chunkSection.getBiomes()).getOrThrow(false, (message) -> {}));
                    });
                }

                if (blockNibble != null) {
                    if (!blockNibble.isEmpty()) {
                        if (light != null) {
                            light.setBlockLight(locationY, blockNibble.getData());
                        } else {
                            sectionNBT.putByteArray("BlockLight", blockNibble.getData());
                            lightCorrect = true;
                        }
                    }
                }

                if (skyNibble != null) {
                    if (!skyNibble.isEmpty()) {
                        if (light != null) {
                            light.setSkyLight(locationY, skyNibble.getData());
                        } else {
                            sectionNBT.putByteArray("SkyLight", skyNibble.getData());
                            lightCorrect = true;
                        }
                    }
                }

                // 增加 inSections 確保 asyncRunnable 不會出資料錯誤
                if (!sectionNBT.isEmpty() || inSections) {
                    sectionNBT.putByte("Y", (byte) locationY);
                    sectionArrayNBT.add(sectionNBT);
                }
            }
        }
        nbt.put("sections", sectionArrayNBT);

        if (lightCorrect) {
            nbt.putInt("starlight.light_version", 6);
            nbt.putBoolean("isLightOn", true);
        }

        // 實體方塊
        ListTag blockEntitiesNBT = new ListTag();
        for (BlockPos blockPos : chunk.getBlockEntitiesPos()) {
            CompoundTag blockEntity = chunk.getBlockEntityNbtForSaving(blockPos);
            if (blockEntity != null) {
                blockEntitiesNBT.add(blockEntity);
            }
        }
        nbt.put("block_entities", blockEntitiesNBT);

        if (chunk.getStatus().getChunkType() == ChunkStatus.ChunkType.PROTOCHUNK) {
        }

        ChunkAccess.TicksToSave tickSchedulers = chunk.getTicksForSerialization();
        long gameTime = world.getLevelData().getGameTime();
        nbt.put("block_ticks", tickSchedulers.blocks().save(gameTime, (block) -> BuiltInRegistries.BLOCK.getKey(block).toString()));
        nbt.put("fluid_ticks", tickSchedulers.fluids().save(gameTime, (fluid) -> BuiltInRegistries.FLUID.getKey(fluid).toString()));

        ShortList[] packOffsetList = chunk.getPostProcessing();
        ListTag packOffsetsNBT = new ListTag();
        for (ShortList shortlist : packOffsetList) {
            ListTag packsNBT = new ListTag();
            if (shortlist != null) {
                for (Short shortData : shortlist) {
                    packsNBT.add(ShortTag.valueOf(shortData));
                }
            }
            packOffsetsNBT.add(packsNBT);
        }
        nbt.put("PostProcessing", packOffsetsNBT);

        // 高度圖
        CompoundTag heightmapsNBT = new CompoundTag();
        for (Map.Entry<Heightmap.Types, Heightmap> entry : chunk.getHeightmaps()) {
            if (chunk.getStatus().heightmapsAfter().contains(entry.getKey())) {
                heightmapsNBT.put(entry.getKey().getSerializationKey(), new LongArrayTag(entry.getValue().getRawData()));
            }
        }
        nbt.put("Heightmaps", heightmapsNBT);

        return nbt;
    }
}
