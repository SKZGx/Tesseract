package com.supermartijn642.tesseract.manager;

import com.supermartijn642.tesseract.EnumChannelType;
import com.supermartijn642.tesseract.TesseractTile;
import com.supermartijn642.tesseract.capabilities.CombinedEnergyStorage;
import com.supermartijn642.tesseract.capabilities.CombinedFluidHandler;
import com.supermartijn642.tesseract.capabilities.CombinedItemHandler;
import net.minecraft.nbt.CompoundNBT;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Created 3/20/2020 by SuperMartijn642
 */
public class Channel {

    public final int id;
    public final EnumChannelType type;
    public UUID creator;
    public boolean isPrivate = false;

    public String name;

    private final List<TesseractLocation> tesseracts = new LinkedList<>();

    public Channel(int id, EnumChannelType type, UUID creator, boolean isPrivate, String name){
        this.id = id;
        this.type = type;
        this.creator = creator;
        this.isPrivate = isPrivate;
        this.name = name;
    }

    public Channel(int id, EnumChannelType type, CompoundNBT compound){
        this.id = id;
        this.type = type;
        this.read(compound);
    }

    public String getName(){
        return this.name;
    }

    public void addTesseract(TesseractTile tesseract){
        TesseractLocation location = new TesseractLocation(tesseract.getWorld(), tesseract.getPos());
        if(!this.tesseracts.contains(location)){
            this.tesseracts.add(location);
            tesseract.setChannel(this.type, this.id);
        }
    }

    public void removeTesseract(TesseractTile tesseract){
        TesseractLocation location = new TesseractLocation(tesseract.getWorld(), tesseract.getPos());
        this.tesseracts.remove(location);
    }

    public CompoundNBT write(){
        CompoundNBT compound = new CompoundNBT();
        compound.putUniqueId("creator", this.creator);
        compound.putBoolean("private", this.isPrivate);
        compound.putString("name", this.name);
        CompoundNBT tesseractCompound = new CompoundNBT();
        for(int a = 0; a < this.tesseracts.size(); a++){
            if(this.tesseracts.get(a).isValid())
                tesseractCompound.put(Integer.toString(a), this.tesseracts.get(a).write());
        }
        compound.put("tesseracts", tesseractCompound);
        return compound;
    }

    public void read(CompoundNBT compound){
        this.creator = compound.getUniqueId("creator");
        this.isPrivate = compound.getBoolean("private");
        this.name = compound.getString("name");
        this.tesseracts.clear();
        CompoundNBT tesseractCompound = compound.getCompound("tesseracts");
        for(String key : tesseractCompound.keySet()){
            TesseractLocation location = new TesseractLocation(tesseractCompound.getCompound(key));
            this.tesseracts.add(location);
        }
    }

    public CombinedItemHandler getItemHandler(TesseractTile self){
        return new CombinedItemHandler(this.tesseracts, self);
    }

    public CombinedFluidHandler getFluidHandler(TesseractTile self){
        return new CombinedFluidHandler(this.tesseracts, self);
    }

    public CombinedEnergyStorage getEnergyStorage(TesseractTile self){
        return new CombinedEnergyStorage(this.tesseracts, self);
    }

    @Override
    public int hashCode(){
        return this.id + 31 * this.type.hashCode();
    }

    public void delete(){
        for(TesseractLocation location : this.tesseracts)
            if(location.isValid())
                location.getTesseract().setChannel(this.type, -1);
    }
}
