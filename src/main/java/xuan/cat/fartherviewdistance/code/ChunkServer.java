package xuan.cat.fartherviewdistance.code;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import xuan.cat.fartherviewdistance.api.branch.*;
import xuan.cat.fartherviewdistance.api.branch.packet.PacketEvent;
import xuan.cat.fartherviewdistance.api.branch.packet.PacketMapChunkEvent;
import xuan.cat.fartherviewdistance.api.event.PlayerSendExtendChunkEvent;
import xuan.cat.fartherviewdistance.code.data.*;
import xuan.cat.fartherviewdistance.code.data.viewmap.ViewMap;
import xuan.cat.fartherviewdistance.code.data.viewmap.ViewShape;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 區塊伺服器
 */
public final class ChunkServer {
    private final ConfigData configData;
    private final Plugin plugin;
    private boolean running = true;
    public final BranchMinecraft branchMinecraft;
    public final BranchPacket branchPacket;
    private final Set<BukkitTask> bukkitTasks = ConcurrentHashMap.newKeySet();
    /** 隨機數產生器 */
    public static final Random random = new Random();
    /** 多執行緒服務 */
    private ScheduledExecutorService multithreadedService;
    /** 允許運行執行緒 */
    private AtomicBoolean multithreadedCanRun;
    /** 每個玩家的視圖計算器 */
    public final Map<Player, PlayerChunkView> playersViewMap = new ConcurrentHashMap<>();
    /** 伺服器網路流量 */
    private final NetworkTraffic serverNetworkTraffic = new NetworkTraffic();
    /** 每個世界網路流量 */
    private final Map<World, NetworkTraffic> worldsNetworkTraffic = new ConcurrentHashMap<>();
    /** 最後一次的全部世界 */
    private List<World> lastWorldList = new ArrayList<>();
    /** 伺服器已生成區塊數量 */
    private final AtomicInteger serverGeneratedChunk = new AtomicInteger(0);
    /** 伺服器報告 */
    public final CumulativeReport serverCumulativeReport = new CumulativeReport();
    /** 每個世界已生成區塊數量 */
    private final Map<World, AtomicInteger> worldsGeneratedChunk = new ConcurrentHashMap<>();
    /** 每個世界報告 */
    public final Map<World, CumulativeReport> worldsCumulativeReport = new ConcurrentHashMap<>();
    /** 等待前往主執行緒 */
    private final Set<Runnable> waitMoveSyncQueue = ConcurrentHashMap.newKeySet();
    /** 每個執行序耗時 */
    public final Map<Integer, CumulativeReport> threadsCumulativeReport = new ConcurrentHashMap<>();
    /** 全部執行緒 */
    public final Set<Thread> threadsSet = ConcurrentHashMap.newKeySet();
    /** 全局停止 */
    public volatile boolean globalPause = false;
    /** 語言 */
    public final LangFiles lang = new LangFiles();
    /** 視圖形狀 */
    private final ViewShape viewShape;


    public ChunkServer(ConfigData configData, Plugin plugin, ViewShape viewShape, BranchMinecraft branchMinecraft, BranchPacket branchPacket) {
        this.configData = configData;
        this.plugin = plugin;
        this.branchMinecraft = branchMinecraft;
        this.branchPacket = branchPacket;
        this.viewShape = viewShape;

        BukkitScheduler scheduler = Bukkit.getScheduler();
        bukkitTasks.add(scheduler.runTaskTimer(plugin, this::tickSync, 0, 1));
        bukkitTasks.add(scheduler.runTaskTimerAsynchronously(plugin, this::tickAsync, 0, 1));
        bukkitTasks.add(scheduler.runTaskTimerAsynchronously(plugin, this::tickReport, 0, 20));

        // 除錯用
//        scheduler.runTaskTimer(plugin, () -> {
//            Player p = Bukkit.getPlayer("xuancat0208");
//            if (p != null) {
//                PlayerChunkView v = getView(p);
//                if (v != null) {
//                    System.out.println(v.getMap().extendDistance);
//                }
//            }
//        }, 0, 20);

        reloadMultithreaded();
    }


    /**
     * 初始化玩家區塊視圖
     */
    public PlayerChunkView initView(Player player) {
        PlayerChunkView view = new PlayerChunkView(player, configData, viewShape, branchPacket);
        playersViewMap.put(player, view);
        return view;
    }
    /**
     * 清除玩家區塊視圖
     */
    public void clearView(Player player) {
        playersViewMap.remove(player);
    }
    /**
     * @return 玩家區塊視圖
     */
    public PlayerChunkView getView(Player player) {
        return playersViewMap.get(player);
    }


    /**
     * 重新加載多執行緒
     */
    public synchronized void reloadMultithreaded() {
        // 先終止處理上一次的執行續組
        if (multithreadedCanRun != null)
            multithreadedCanRun.set(false);
        if (multithreadedService != null) {
            multithreadedService.shutdown();
        }
        threadsCumulativeReport.clear();
        threadsSet.clear();

        playersViewMap.values().forEach(view -> view.waitSend = false);

        // 創建新的執行續組
        AtomicBoolean canRun = new AtomicBoolean(true);
        multithreadedCanRun = canRun;
        multithreadedService = Executors.newScheduledThreadPool(configData.asyncThreadAmount + 1);

        multithreadedService.schedule(() -> {
            Thread thread = Thread.currentThread();
            thread.setName("FartherViewDistance View thread");
            thread.setPriority(3);
            threadsSet.add(thread);
            runView(canRun);
        }, 0, TimeUnit.MILLISECONDS);

        for (int index = 0 ; index < configData.asyncThreadAmount ; index++) {
            int threadNumber = index;
            CumulativeReport threadCumulativeReport = new CumulativeReport();
            threadsCumulativeReport.put(index, threadCumulativeReport)  ;
            // 每個執行續每 50 毫秒響應一次
            multithreadedService.schedule(() -> {
                Thread thread = Thread.currentThread();
                thread.setName("FartherViewDistance AsyncTick thread #" + threadNumber);
                thread.setPriority(2);
                threadsSet.add(thread);
                runThread(canRun, threadCumulativeReport);
            }, 0, TimeUnit.MILLISECONDS);
        }
    }


    /**
     * 初始化世界
     */
    public void initWorld(World world) {
        worldsNetworkTraffic.put(world, new NetworkTraffic());
        worldsCumulativeReport.put(world, new CumulativeReport());
        worldsGeneratedChunk.put(world, new AtomicInteger(0));
    }
    /**
     * 清除世界
     */
    public void clearWorld(World world) {
        worldsNetworkTraffic.remove(world);
        worldsCumulativeReport.remove(world);
        worldsGeneratedChunk.remove(world);
    }



    /**
     * 同步滴答
     *  主要用於處裡一些不可異步的操作
     */
    private void tickSync() {
        List<World> worldList = Bukkit.getWorlds();
        Collections.shuffle(worldList);
        lastWorldList = worldList;
        waitMoveSyncQueue.removeIf(runnable -> {
            try {
                runnable.run();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return true;
        });
    }


    /**
     * 異步滴答
     */
    private void tickAsync() {
        // 將所有網路流量歸零
        serverNetworkTraffic.next();
        worldsNetworkTraffic.values().forEach(NetworkTraffic::next);
        playersViewMap.values().forEach(view -> {
            view.networkTraffic.next();
            view.networkSpeed.next();
        });
        serverGeneratedChunk.set(0);
        worldsGeneratedChunk.values().forEach(generatedChunk -> generatedChunk.set(0));
    }


    /**
     * 同步報告滴答
     */
    private void tickReport() {
        serverCumulativeReport.next();
        worldsCumulativeReport.values().forEach(CumulativeReport::next);
        playersViewMap.values().forEach(view -> view.cumulativeReport.next());
        threadsCumulativeReport.values().forEach(CumulativeReport::next);
    }


    /**
     * 穩定保持每 50 毫秒執行一次
     */
    private void runView(AtomicBoolean canRun) {
        // 主循環
        while (canRun.get()) {
            // 開始時間
            long startTime = System.currentTimeMillis();

            try {
                // 處裡每個玩家的視圖
                playersViewMap.forEach((player, view) -> {
                    if (!view.install())
                        view.updateDistance();
                    view.moveTooFast = view.overSpeed();
                });
            } catch (Exception exception) {
                exception.printStackTrace();
            }

            // 結束時間
            long endTime = System.currentTimeMillis();
            // 最大耗時 50 毫秒
            long needSleep = 50 - (endTime - startTime);
            if (needSleep > 0) {
                try {
                    Thread.sleep(needSleep);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    /**
     * 多執行續滴答
     */
    private void runThread(AtomicBoolean canRun, CumulativeReport threadCumulativeReport) {
        // 主循環
        while (canRun.get()) {
            // 開始時間
            long startTime = System.currentTimeMillis();
            // 有效時間
            long effectiveTime = startTime + 50;

            if (!globalPause) {
                try {
                    // 全部世界
                    List<World> worldList = lastWorldList;
                    // 全部玩家視圖
                    List<PlayerChunkView> viewList = Arrays.asList(playersViewMap.values().toArray(new PlayerChunkView[0]));
                    Collections.shuffle(viewList);
                    // 移動
                    for (PlayerChunkView view : viewList) {
                        view.move();
                    }
                    // 每個世界的每個玩家視圖
                    Map<World, List<PlayerChunkView>> worldsViews = new HashMap<>();
                    for (PlayerChunkView view : viewList) {
                        worldsViews.computeIfAbsent(view.getLastWorld(), key -> new ArrayList<>()).add(view);
                    }

                    handleServer: {
                        for (World world : worldList) {
                            // 世界配置
                            ConfigData.World configWorld = configData.getWorld(world.getName());
                            if (!configWorld.enable)
                                continue;
                            // 世界報告
                            CumulativeReport worldCumulativeReport = worldsCumulativeReport.get(world);
                            if (worldCumulativeReport == null)
                                continue;
                            // 世界網路流量
                            NetworkTraffic worldNetworkTraffic = worldsNetworkTraffic.get(world);
                            if (worldNetworkTraffic == null)
                                continue;
                            if (serverNetworkTraffic.exceed(configData.getServerSendTickMaxBytes()))
                                break handleServer;
                            if (worldNetworkTraffic.exceed(configWorld.getWorldSendTickMaxBytes()))
                                continue;

                            /// 世界已生成的區塊數量
                            AtomicInteger worldGeneratedChunk = worldsGeneratedChunk.getOrDefault(world, new AtomicInteger(Integer.MAX_VALUE));

                            handleWorld: {
                                // 所有玩家都網路流量都已滿載
                                boolean playersFull = false;
                                while (!playersFull && effectiveTime >= System.currentTimeMillis()) {
                                    playersFull = true;
                                    for (PlayerChunkView view : worldsViews.getOrDefault(world, new ArrayList<>(0))) {
                                        if (serverNetworkTraffic.exceed(configData.getServerSendTickMaxBytes()))
                                            break handleServer;
                                        if (worldNetworkTraffic.exceed(configWorld.getWorldSendTickMaxBytes()))
                                            break handleWorld;
                                        synchronized (view.networkTraffic) {
                                            Integer forciblySendSecondMaxBytes = view.forciblySendSecondMaxBytes;
                                            if (view.networkTraffic.exceed(forciblySendSecondMaxBytes != null ? (int) (forciblySendSecondMaxBytes * configData.playerNetworkSpeedUseDegree) / 20 : configWorld.getPlayerSendTickMaxBytes()))
                                                continue;
                                            if (configData.autoAdaptPlayerNetworkSpeed && view.networkTraffic.exceed(Math.max(1, view.networkSpeed.avg() * 50)))
                                                continue;
                                        }
                                        if (view.waitSend) {
                                            playersFull = false;
                                            continue;
                                        }
                                        if (view.moveTooFast)
                                            continue;
                                        view.waitSend = true;
                                        long syncKey = view.syncKey;
                                        Long chunkKey = view.next();
                                        if (chunkKey == null) {
                                            view.waitSend = false;
                                            continue;
                                        }
                                        playersFull = false;
                                        int chunkX = ViewMap.getX(chunkKey);
                                        int chunkZ = ViewMap.getZ(chunkKey);

                                        handlePlayer: {
                                            if (!configData.disableFastProcess) {
                                                // 讀取最新
                                                try {
                                                    if (configWorld.readServerLoadedChunk) {
                                                        BranchChunk chunk = branchMinecraft.getChunkFromMemoryCache(world, chunkX, chunkZ);
                                                        if (chunk != null) {
                                                            // 讀取快取
                                                            serverCumulativeReport.increaseLoadFast();
                                                            worldCumulativeReport.increaseLoadFast();
                                                            view.cumulativeReport.increaseLoadFast();
                                                            threadCumulativeReport.increaseLoadFast();
                                                            List<Runnable> asyncRunnable = new ArrayList<>();
                                                            BranchChunkLight chunkLight = branchMinecraft.fromLight(world);
                                                            BranchNBT chunkNBT = chunk.toNBT(chunkLight, asyncRunnable);
                                                            asyncRunnable.forEach(Runnable::run);
                                                            sendChunk(world, configWorld, worldNetworkTraffic, view, chunkX, chunkZ, chunkNBT, chunkLight, syncKey, worldCumulativeReport, threadCumulativeReport);
                                                            break handlePlayer;
                                                        }
                                                    }
                                                } catch (NullPointerException | NoClassDefFoundError | NoSuchMethodError | NoSuchFieldError exception) {
                                                    exception.printStackTrace();
                                                } catch (Exception ignored) {
                                                }

                                                // 讀取最快
                                                try {
                                                    BranchNBT chunkNBT = branchMinecraft.getChunkNBTFromDisk(world, chunkX, chunkZ);
                                                    if (chunkNBT != null && branchMinecraft.fromStatus(chunkNBT).isAbove(BranchChunk.Status.FULL)) {
                                                        // 讀取區域文件
                                                        serverCumulativeReport.increaseLoadFast();
                                                        worldCumulativeReport.increaseLoadFast();
                                                        view.cumulativeReport.increaseLoadFast();
                                                        threadCumulativeReport.increaseLoadFast();
                                                        sendChunk(world, configWorld, worldNetworkTraffic, view, chunkX, chunkZ, chunkNBT, branchMinecraft.fromLight(world, chunkNBT), syncKey, worldCumulativeReport, threadCumulativeReport);
                                                        break handlePlayer;
                                                    }
                                                } catch (NullPointerException | NoClassDefFoundError | NoSuchMethodError | NoSuchFieldError exception) {
                                                    exception.printStackTrace();
                                                } catch (Exception ignored) {
                                                }
                                            }

                                            boolean canGenerated = serverGeneratedChunk.get() < configData.serverTickMaxGenerateAmount && worldGeneratedChunk.get() < configWorld.worldTickMaxGenerateAmount;
                                            if (canGenerated) {
                                                serverGeneratedChunk.incrementAndGet();
                                                worldGeneratedChunk.incrementAndGet();
                                            }

                                            // 生成
                                            try {
                                                // paper
                                                Chunk chunk = world.getChunkAtAsync(chunkX, chunkZ, canGenerated, true).get();
                                                if (chunk != null) {
                                                    serverCumulativeReport.increaseLoadSlow();
                                                    worldCumulativeReport.increaseLoadSlow();
                                                    view.cumulativeReport.increaseLoadSlow();
                                                    threadCumulativeReport.increaseLoadSlow();
                                                    try {
                                                        List<Runnable> asyncRunnable = new ArrayList<>();
                                                        BranchChunkLight chunkLight = branchMinecraft.fromLight(world);
                                                        BranchNBT chunkNBT = branchMinecraft.fromChunk(world, chunk).toNBT(chunkLight, asyncRunnable);
                                                        asyncRunnable.forEach(Runnable::run);
                                                        sendChunk(world, configWorld, worldNetworkTraffic, view, chunkX, chunkZ, chunkNBT, chunkLight, syncKey, worldCumulativeReport, threadCumulativeReport);
                                                        break handlePlayer;
                                                    } catch (NullPointerException | NoClassDefFoundError | NoSuchMethodError | NoSuchFieldError exception) {
                                                        exception.printStackTrace();
                                                    } catch (Exception ignored) {
                                                    }
                                                } else if (configData.serverTickMaxGenerateAmount > 0 && configWorld.worldTickMaxGenerateAmount > 0) {
                                                    view.remove(chunkX, chunkZ);
                                                    break handlePlayer;
                                                }
                                            } catch (ExecutionException ignored) {
                                                view.remove(chunkX, chunkZ);
                                                break handlePlayer;
                                            } catch (NoSuchMethodError methodError) {
                                                // spigot (不推薦)
                                                if (canGenerated) {
                                                    serverCumulativeReport.increaseLoadSlow();
                                                    worldCumulativeReport.increaseLoadSlow();
                                                    view.cumulativeReport.increaseLoadSlow();
                                                    threadCumulativeReport.increaseLoadSlow();
                                                    try {
                                                        List<Runnable> asyncRunnable = new ArrayList<>();
                                                        BranchChunkLight chunkLight = branchMinecraft.fromLight(world);
                                                        CompletableFuture<BranchNBT> syncNBT = new CompletableFuture<>();
                                                        waitMoveSyncQueue.add(() -> syncNBT.complete(branchMinecraft.fromChunk(world, world.getChunkAt(chunkX, chunkZ)).toNBT(chunkLight, asyncRunnable)));
                                                        BranchNBT chunkNBT = syncNBT.get();
                                                        asyncRunnable.forEach(Runnable::run);
                                                        sendChunk(world, configWorld, worldNetworkTraffic, view, chunkX, chunkZ, chunkNBT, chunkLight, syncKey, worldCumulativeReport, threadCumulativeReport);
                                                        break handlePlayer;
                                                    } catch (NullPointerException | NoClassDefFoundError | NoSuchMethodError | NoSuchFieldError exception) {
                                                        exception.printStackTrace();
                                                    } catch (Exception ignored) {
                                                    }
                                                }
                                            } catch (InterruptedException ignored) {
                                            } catch (Exception ex) {
                                                ex.printStackTrace();
                                            }
                                        }

                                        view.waitSend = false;
                                    }

                                    try {
                                        Thread.sleep(0L);
                                    } catch (InterruptedException ignored) {
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }

            // 結束時間
            long endTime = System.currentTimeMillis();
            // 最大耗時 50 毫秒
            long needSleep = 50 - (endTime - startTime);
            if (needSleep > 0) {
                try {
                    Thread.sleep(needSleep);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
    private void sendChunk(World world, ConfigData.World configWorld, NetworkTraffic worldNetworkTraffic, PlayerChunkView view, int chunkX, int chunkZ, BranchNBT chunkNBT, BranchChunkLight chunkLight, long syncKey, CumulativeReport worldCumulativeReport, CumulativeReport threadCumulativeReport) {
        BranchChunk chunk = branchMinecraft.fromChunk(world, chunkX, chunkZ, chunkNBT, configData.calculateMissingHeightMap);
        // 呼叫發送區塊事件
        PlayerSendExtendChunkEvent event = new PlayerSendExtendChunkEvent(view.viewAPI, chunk, world);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;

        // 防透視礦物作弊
        if (configWorld.preventXray != null && configWorld.preventXray.size() > 0) {
            // 替換全部指定材質
            for (Map.Entry<BlockData, BlockData[]> conversionMap : configWorld.preventXray.entrySet())
                chunk.replaceAllMaterial(conversionMap.getValue(), conversionMap.getKey());
        }

        AtomicInteger consumeTraffic = new AtomicInteger(0);
        Consumer<Player> chunkAndLightPacket = branchPacket.sendChunkAndLight(chunk, chunkLight, configWorld.sendTitleData, consumeTraffic::addAndGet);

        // 需要測量速度 (最短每 1 秒一次, 30 秒超時)
        synchronized (view.networkSpeed) {
            // 檢查當前是否在伺服器區塊內
            Location nowLoc = view.getPlayer().getLocation();
            int nowChunkX = nowLoc.getBlockX() >> 4;
            int nowChunkZ = nowLoc.getBlockZ() >> 4;
            ViewMap viewMap = view.getMap();
            if (world != nowLoc.getWorld()) {
                view.getMap().markWaitPosition(chunkX, chunkZ);
                return;
            }
            if (view.getMap().isWaitPosition(chunkX, chunkZ))
                return;
            if (viewShape.isInsideEdge(nowChunkX, nowChunkZ, chunkX, chunkZ, viewMap.serverDistance))
                return;
            if (view.syncKey != syncKey)
                return;
            if (!running)
                return;

            boolean needMeasure = configData.autoAdaptPlayerNetworkSpeed && ((view.networkSpeed.speedID == null && view.networkSpeed.speedTimestamp + 1000 <= System.currentTimeMillis()) || view.networkSpeed.speedTimestamp + 30000 <= System.currentTimeMillis());
            // 測量 PING
            if (needMeasure) {
                if (view.networkSpeed.speedID != null) {
                    view.networkSpeed.add(30000, 0);
                }
                long pingID = random.nextLong();
                view.networkSpeed.pingID = pingID;
                view.networkSpeed.pingTimestamp = System.currentTimeMillis();
                branchPacket.sendKeepAlive(view.getPlayer(), pingID);
            }

            // 因為 ProtocolLib 無法實現的特性導致需要每次都安全的發送一次視野距離
//            branchPacket.sendViewDistance(view.getPlayer(), view.getMap().extendDistance);

            // 正式發送
            chunkAndLightPacket.accept(view.getPlayer());
            serverNetworkTraffic.use(consumeTraffic.get());
            worldNetworkTraffic.use(consumeTraffic.get());
            view.networkTraffic.use(consumeTraffic.get());
            serverCumulativeReport.addConsume(consumeTraffic.get());
            worldCumulativeReport.addConsume(consumeTraffic.get());
            view.cumulativeReport.addConsume(consumeTraffic.get());
            threadCumulativeReport.addConsume(consumeTraffic.get());

            // 測量速度
            if (needMeasure) {
                long speedID = random.nextLong();
                view.networkSpeed.speedID = speedID;
                view.networkSpeed.speedConsume = consumeTraffic.get();
                view.networkSpeed.speedTimestamp = System.currentTimeMillis();
                branchPacket.sendKeepAlive(view.getPlayer(), speedID);
            }
        }
    }


    /**
     * 區塊數據包事件
     */
    public void packetEvent(Player player, PacketEvent event) {
        PlayerChunkView view = getView(player);
        if (view == null)
            return;
        if (event instanceof PacketMapChunkEvent) {
            PacketMapChunkEvent chunkEvent = (PacketMapChunkEvent) event;
            view.send(chunkEvent.getChunkX(), chunkEvent.getChunkZ());
        }
    }


    /**
     * 玩家重生
     */
    public void respawnView(Player player) {
        PlayerChunkView view = getView(player);
        if (view == null)
            return;
        view.delay();
        // 因為 ProtocolLib 無法實現的特性導致需要清除
//        view.clear();
        waitMoveSyncQueue.add(() -> branchPacket.sendViewDistance(player, view.getMap().extendDistance));
    }
    /**
     * 切換世界/長距離傳送/死亡重生, 則等待一段時間
     */
    public void unloadView(Player player, Location from, Location move) {
        PlayerChunkView view = getView(player);
        if (view == null)
            return;
        int blockDistance = view.getMap().extendDistance << 4;
        if (from.getWorld() != move.getWorld())
            view.unload();
        else if (Math.abs(from.getX() - move.getX()) >= blockDistance || Math.abs(from.getZ() - move.getZ()) >= blockDistance)
            view.unload();
    }


    /**
     * 結束伺服器運行
     */
    void close() {
        running = false;
        for (BukkitTask task : bukkitTasks)
            task.cancel();
        multithreadedService.shutdown();
    }
}