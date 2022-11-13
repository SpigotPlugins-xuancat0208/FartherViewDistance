package xuan.cat.fartherviewdistance.code.branch.v14;

import net.minecraft.server.v1_14_R1.NBTTagCompound;
import xuan.cat.fartherviewdistance.api.branch.BranchNBT;

public final class Branch_14_NBT implements BranchNBT {

    protected NBTTagCompound tag;

    public Branch_14_NBT() {
        this.tag = new NBTTagCompound();
    }

    public Branch_14_NBT(NBTTagCompound tag) {
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
