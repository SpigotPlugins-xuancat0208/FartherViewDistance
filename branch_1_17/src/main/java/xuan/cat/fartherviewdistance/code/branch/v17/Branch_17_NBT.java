package xuan.cat.fartherviewdistance.code.branch.v17;

import net.minecraft.nbt.NBTTagCompound;
import xuan.cat.fartherviewdistance.api.branch.BranchNBT;

public final class Branch_17_NBT implements BranchNBT {

    protected NBTTagCompound tag;

    public Branch_17_NBT() {
        this.tag = new NBTTagCompound();
    }

    public Branch_17_NBT(NBTTagCompound tag) {
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
