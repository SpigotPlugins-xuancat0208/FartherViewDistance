package xuan.cat.fartherviewdistance.code.branch.v15;

import net.minecraft.server.v1_15_R1.NBTTagCompound;
import xuan.cat.fartherviewdistance.api.branch.BranchNBT;

public final class Branch_15_NBT implements BranchNBT {

    protected NBTTagCompound tag;

    public Branch_15_NBT() {
        this.tag = new NBTTagCompound();
    }

    public Branch_15_NBT(NBTTagCompound tag) {
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
