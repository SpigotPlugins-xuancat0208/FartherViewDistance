package xuan.cat.fartherviewdistance.code.data.viewmap;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.List;

public abstract class ViewMap {
    public final ViewShape viewShape;
    public int extendDistance = 1;
    public int serverDistance = 1;
    protected int centerX = 0;
    protected int centerZ = 0;

    protected ViewMap(ViewShape viewShape) {
        this.viewShape = viewShape;
    }


    public abstract List<Long> movePosition(Location location);
    /**
     * 移動到區塊位置 (中心點)
     * @param moveX 區塊座標X
     * @param moveZ 區塊座標Z
     * @return 如果有區塊被移除, 則會集中回傳在這
     */
    public abstract List<Long> movePosition(int moveX, int moveZ);


    /**
     * 取得下一個應該要處裡的區塊
     * @return positionKey, 若沒有需要處裡的區塊, 則回傳 null
     */
    public abstract Long get();

    public int getCenterX() {
        return centerX;
    }
    public int getCenterZ() {
        return centerZ;
    }

    public final void setCenter(Location location) {
        setCenter(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }
    public final void setCenter(int positionX, int positionZ) {
        setCenterX(positionX);
        setCenterZ(positionZ);
    }
    public void setCenterX(int centerX) {
        this.centerX = centerX;
    }
    public void setCenterZ(int centerZ) {
        this.centerZ = centerZ;
    }


    public static int getX(long positionKey) {
        return (int) (positionKey);
    }
    public static int getZ(long positionKey) {
        return (int) (positionKey >> 32);
    }
    public static long getPositionKey(int x, int z) {
        return ((long) z << 32) & 0b1111111111111111111111111111111100000000000000000000000000000000L | x & 0b0000000000000000000000000000000011111111111111111111111111111111L;
    }


    public abstract boolean inPosition(int positionX, int positionZ);


    public abstract boolean isWaitPosition(long positionKey);
    public abstract boolean isWaitPosition(int positionX, int positionZ);


    public abstract boolean isSendPosition(long positionKey);
    public abstract boolean isSendPosition(int positionX, int positionZ);


    public abstract void markWaitPosition(long positionKey);
    public abstract void markWaitPosition(int positionX, int positionZ);


    public abstract void markSendPosition(long positionKey);
    public abstract void markSendPosition(int positionX, int positionZ);


    /**
     * @param range 範圍外的區塊標記為等待中
     */
    public abstract void markOutsideWait(int range);
    /**
     * @param range 範圍外的區塊標記為以發送
     */
    public abstract void markOutsideSend(int range);


    /**
     * @param range 範圍內的區塊標記為等待中
     */
    public abstract void markInsideWait(int range);
    /**
     * @param range 範圍內的區塊標記為以發送
     */
    public abstract void markInsideSend(int range);


    public abstract List<Long> getAll();
    public abstract List<Long> getAllNotServer();


    public abstract boolean isWaitSafe(int pointerX, int pointerZ);
    public abstract boolean isSendSafe(int pointerX, int pointerZ);


    public abstract boolean markWaitSafe(int pointerX, int pointerZ);
    public abstract void markSendSafe(int pointerX, int pointerZ);


    public abstract void clear();


    public abstract void debug(CommandSender sender);
}
