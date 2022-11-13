package xuan.cat.fartherviewdistance.api.branch.packet;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public final class PacketViewDistanceEvent extends PacketEvent {
    private static final HandlerList handlers = new HandlerList();
    public HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }

    private final int viewDistance;

    public PacketViewDistanceEvent(Player player, int viewDistance) {
        super(player);
        this.viewDistance = viewDistance;
    }

    public int getViewDistance() {
        return viewDistance;
    }

}