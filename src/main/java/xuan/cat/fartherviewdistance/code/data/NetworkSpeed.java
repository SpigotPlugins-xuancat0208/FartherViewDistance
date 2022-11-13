package xuan.cat.fartherviewdistance.code.data;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 網路速度監聽器
 */
public final class NetworkSpeed {
    /** 測速用時間戳 */
    public volatile long speedTimestamp = 0;
    /** 測速用數據量 */
    public volatile int speedConsume = 0;
    /** 測速用 ID */
    public volatile Long speedID = null;

    /** 延遲用時間戳 */
    public volatile long pingTimestamp = 0;
    /** 延遲用 ID */
    public volatile Long pingID = null;
    /** 最後一次的延遲 */
    public volatile int lastPing = 0;

    /** 寫入紀錄 */
    private volatile int[] writeArray = new int[50];
    /** 延遲紀錄 */
    private volatile int[] consumeArray = new int[50];
    /** 寫入累計 */
    private final AtomicInteger writeTotal = new AtomicInteger(0);
    /** 寫入累計 */
    private final AtomicInteger consumeTotal = new AtomicInteger(0);


    /**
     * 加入
     */
    public void add(int ping, int length) {
        synchronized (writeTotal) {
            writeTotal.addAndGet(length);
            consumeTotal.addAndGet(ping);
            writeArray[0] += length;
            consumeArray[0] += ping;
        }
    }


    /**
     * @return 平均速度
     */
    public int avg() {
        synchronized (writeTotal) {
            int writeGet = writeTotal.get();
            int consumeGet = Math.max(1, consumeTotal.get());
            if (writeGet == 0) {
                return 0;
            } else {
                return writeGet / consumeGet;
            }
        }
    }


    /**
     * 下一個 tick
     */
    public void next() {
        synchronized (writeTotal) {
            writeTotal.addAndGet(-writeArray[writeArray.length - 1]);
            consumeTotal.addAndGet(-consumeArray[consumeArray.length - 1]);
            int[] writeArrayClone = new int[writeArray.length];
            int[] consumeArrayClone = new int[consumeArray.length];
            System.arraycopy(writeArray, 0, writeArrayClone, 1, writeArray.length - 1);
            System.arraycopy(consumeArray, 0, consumeArrayClone, 1, consumeArray.length - 1);
            writeArray = writeArrayClone;
            consumeArray = consumeArrayClone;
        }
    }
}
