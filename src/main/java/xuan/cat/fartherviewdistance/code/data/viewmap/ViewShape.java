package xuan.cat.fartherviewdistance.code.data.viewmap;

/**
 * 視圖形狀
 */
public enum ViewShape {
    /** 方形 */
    SQUARE((int aX, int aZ, int bX, int bZ, int viewDistance) -> {
        int minX = bX - viewDistance;
        int minZ = bZ - viewDistance;
        int maxX = bX + viewDistance;
        int maxZ = bZ + viewDistance;
        return aX >= minX && aZ >= minZ && aX <= maxX && aZ <= maxZ;
    }),
    /** 圓形 */
    ROUND((int aX, int aZ, int bX, int bZ, int viewDistance) -> {
        int viewDiameter = viewDistance * viewDistance + viewDistance;
        int distanceX = aX - bX;
        int distanceZ = aZ - bZ;
        int distance = distanceX * distanceX + distanceZ * distanceZ;
        return distance <= viewDiameter;
    }, (int aX, int aZ, int bX, int bZ, int viewDistance) -> {
        JudgeInside inside = (int _aX, int _aZ, int _bX, int _bZ, int viewDiameter) -> {
            int distanceX = _aX - _bX;
            int distanceZ = _aZ - _bZ;
            int distance = distanceX * distanceX + distanceZ * distanceZ;
            return distance <= viewDiameter;
        };
        int viewDiameter = viewDistance * viewDistance + viewDistance;
        return inside.test(aX, aZ, bX, bZ, viewDiameter) && !(!inside.test(aX + 1, aZ, bX, bZ, viewDiameter) || !inside.test(aX - 1, aZ, bX, bZ, viewDiameter) || !inside.test(aX, aZ + 1, bX, bZ, viewDiameter) || !inside.test(aX, aZ - 1, bX, bZ, viewDiameter));
    }),
    ;

    /**
     * 許可計算
     */
    interface JudgeInside {
        boolean test(int aX, int aZ, int bX, int bZ, int viewDistance);
    }
    private final JudgeInside judgeInside;
    private final JudgeInside judgeInsideEdge;

    ViewShape(JudgeInside judgeInside) {
        this(judgeInside, judgeInside);
    }
    ViewShape(JudgeInside judgeInside, JudgeInside judgeInsideEdge) {
        this.judgeInside        = judgeInside;
        this.judgeInsideEdge    = judgeInsideEdge;
    }

    public boolean isInside(int aX, int aZ, int bX, int bZ, int viewDistance) {
        return judgeInside.test(aX, aZ, bX, bZ, viewDistance);
    }
    public boolean isInsideEdge(int aX, int aZ, int bX, int bZ, int viewDistance) {
        return judgeInsideEdge.test(aX, aZ, bX, bZ, viewDistance);
    }
}
