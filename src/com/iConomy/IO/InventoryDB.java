package com.iConomy.IO;

import com.iConomy.Constants;
import com.iConomy.iConomy;
import com.iConomy.util.nbt.ByteTag;
import com.iConomy.util.nbt.CompoundTag;
import com.iConomy.util.nbt.ListTag;
import com.iConomy.util.nbt.NBTInputStream;
import com.iConomy.util.nbt.NBTOutputStream;
import com.iConomy.util.nbt.ShortTag;
import com.iConomy.util.nbt.Tag;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;

public class InventoryDB {
    
    private Server server;
    private File dataDir;
    
    public InventoryDB() {
        server = Bukkit.getServer();
        dataDir = new File(server.getWorlds().get(0).getName(), "players");
    }
    
    public List<String> getAllPlayers() {
        ArrayList<String> result = new ArrayList<String>();
        for (String file : dataDir.list()) {
            if (file.endsWith(".dat")) {
                result.add(file.substring(0, file.length() - 4));
            }
        }
        return result;
    }
    
    public boolean dataExists(String name) {
        return new File(dataDir, name + ".dat").exists();
    }
    
    public void setBalance(String name, double balance) {
        if (server.getPlayer(name) != null) {
            ItemStack[] stacks = server.getPlayer(name).getInventory().getContents();
            setBalance(stacks, balance);
            server.getPlayer(name).getInventory().setContents(stacks);
            server.getPlayer(name).updateInventory();
        } else {
            if (!dataExists(name)) {
                return;
            }
            
            ItemStack[] stacks = readInventory(name);
            if (stacks != null) {
                setBalance(stacks, balance);
                writeInventory(name, stacks);
            }
        }
    }
    
    public double getBalance(String name) {
        if (server.getPlayer(name) != null) {
            return getBalance(server.getPlayer(name).getInventory().getContents());
        } else {
            ItemStack[] stacks = readInventory(name);
            if (stacks != null) {
                return getBalance(stacks);
            } else {
                return Constants.Nodes.Balance.getDouble();
            }
        }
    }
    
    private ItemStack[] readInventory(String name) {
        try {
            NBTInputStream in = new NBTInputStream(new FileInputStream(new File(dataDir, name + ".dat")));
            CompoundTag tag = (CompoundTag) in.readTag();
            in.close();
            
            ListTag inventory = (ListTag) tag.getValue().get("Inventory");

            ItemStack[] stacks = new ItemStack[40];
            for (int i = 0; i < inventory.getValue().size(); ++i) {
                CompoundTag item = (CompoundTag) inventory.getValue().get(i);
                byte count = ((ByteTag) item.getValue().get("Count")).getValue();
                byte slot = ((ByteTag) item.getValue().get("Slot")).getValue();
                short damage = ((ShortTag) item.getValue().get("Damage")).getValue();
                short id = ((ShortTag) item.getValue().get("id")).getValue();
                
                stacks[slot] = new ItemStack(id, damage, count);
            }
            return stacks;
        } catch (IOException ex) {
            return null;
        }
    }
    
    private void writeInventory(String name, ItemStack[] stacks) {
        try {
            NBTInputStream in = new NBTInputStream(new FileInputStream(new File(dataDir, name + ".dat")));
            CompoundTag tag = (CompoundTag) in.readTag();
            in.close();
            
            ListTag inventory = (ListTag) tag.getValue().get("Inventory");
            inventory.getValue().clear();

            for (int i = 0; i < stacks.length; ++i) {
                if (stacks[i] == null) continue;
                
                ByteTag count = new ByteTag("Count", (byte) stacks[i].getAmount());
                ByteTag slot = new ByteTag("Slot", (byte) i);
                ShortTag damage = new ShortTag("Damage", stacks[i].getDurability());
                ShortTag id = new ShortTag("id", (short) stacks[i].getTypeId());
                
                HashMap<String, Tag> tagMap = new HashMap<String, Tag>();
                tagMap.put("Count", count);
                tagMap.put("Slot", slot);
                tagMap.put("Damage", damage);
                tagMap.put("id", id);
                
                inventory.getValue().add(new CompoundTag("", tagMap));
            }
            
            NBTOutputStream out = new NBTOutputStream(new FileOutputStream(new File(dataDir, name + ".dat")));
            out.writeTag(tag);
        } catch (IOException ex) {
        }
    }

    private void setBalance(ItemStack[] contents, double balance) {
        int major = Constants.Nodes.DatabaseMajorItem.getInteger();
        int minor = Constants.Nodes.DatabaseMinorItem.getInteger();
        
        // Remove all existing items
        for (int i = 0; i < contents.length; ++i) {
            ItemStack item = contents[i];
            if (item != null) {
                if (item.getTypeId() == major || item.getTypeId() == minor) {
                    contents[i] = null;
                }
            }
        }
        
        // Re-add balance to inventory
        for (int i = 0; i < contents.length; ++i) {
            if (contents[i] == null) {
                if (balance >= 1) {
                    int add = (int) balance;
                    if (add > Material.getMaterial(major).getMaxStackSize()) {
                        add = Material.getMaterial(major).getMaxStackSize();
                    }
                    contents[i] = new ItemStack(major, add);
                    balance -= add;
                } else if (balance > 0) {
                    int add = (int) ((balance - (int) balance) * 100);
                    if (add > Material.getMaterial(minor).getMaxStackSize()) {
                        add = Material.getMaterial(minor).getMaxStackSize();
                    }
                    contents[i] = new ItemStack(minor, add);
                    balance = 0;
                    break;
                }
            }
        }
    }
    
    private double getBalance(ItemStack[] contents) {
        double balance = 0;
        int major = Constants.Nodes.DatabaseMajorItem.getInteger();
        int minor = Constants.Nodes.DatabaseMinorItem.getInteger();
        
        for (ItemStack item : contents) {
            if (item != null) {
                if (item.getTypeId() == major) {
                    balance += item.getAmount();
                } else if (item.getTypeId() == minor) {
                    balance += 0.01 * item.getAmount();
                }
            }
        }
        
        return balance;
    }
    
}
