package xuan.cat.fartherviewdistance.code.branch.v16;

import net.minecraft.server.v1_16_R3.NBTTagCompound;
import xuan.cat.fartherviewdistance.api.branch.BranchNBT;

public final class Branch_16_NBT implements BranchNBT {
    protected NBTTagCompound tag;


    public Branch_16_NBT() {
        this.tag = new NBTTagCompound();
    }

    public Branch_16_NBT(NBTTagCompound tag) {
        this.tag = tag;
    }


    public NBTTagCompound getNMSTag() {
        return tag;
    }

    @Override
    public String toString() {
        return tag.toString();
    }
}
