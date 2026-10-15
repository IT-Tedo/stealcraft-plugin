// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 IT-Tedo and StealCraft contributors
package de.stealcraft.plugin;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Everything that happens to the bench as a block or as an item. */
public final class BenchListener implements Listener {

    private final StealCraftPlugin plugin;
    private final BenchItem items;

    public BenchListener(StealCraftPlugin plugin) {
        this.plugin = plugin;
        this.items = plugin.benchItem();
    }

    // ---------- placing ----------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (Tag.BEDS.isTagged(e.getBlockPlaced().getType())) {
            registerBed(e);
            return;
        }
        ItemStack hand = e.getItemInHand();
        String id = items.teamOf(hand);
        if (id == null) return;

        Player p = e.getPlayer();
        TeamData t = plugin.store().team(id);
        if (t == null) {
            e.setCancelled(true);
            p.sendMessage(Game.msg("Diese Werkbank gehört zu keinem Team mehr.", NamedTextColor.RED));
            return;
        }
        if (items.versionOf(hand) != t.benchVersion()) {
            e.setCancelled(true);
            p.sendMessage(Game.msg("Diese Werkbank ist ungültig, weil Team " + t.id() + " eine neue bekommen hat. Sie wird entfernt.", NamedTextColor.RED));
            plugin.getServer().getScheduler().runTask(plugin, () -> p.getInventory().removeItem(hand));
            return;
        }
        if (!t.isMember(p.getUniqueId())) {
            e.setCancelled(true);
            p.sendMessage(Game.msg("Nur Team " + t.id() + " kann diese Werkbank aufstellen. Du kannst sie behalten, verstecken oder gegen etwas eintauschen.", NamedTextColor.YELLOW));
            return;
        }
        if (t.placed()) {
            e.setCancelled(true);
            p.sendMessage(Game.msg("Eure Werkbank steht schon.", NamedTextColor.YELLOW));
            return;
        }
        Location spot = e.getBlockPlaced().getLocation();
        if (!spot.getWorld().getWorldBorder().isInside(spot.clone().add(0.5, 0, 0.5))) {
            e.setCancelled(true);
            plugin.game().disqualify(t, p);
            return;
        }
        if (!plugin.store().bedInRange(t, spot)) {
            e.setCancelled(true);
            Location anchor = plugin.store().anchorBed(t);
            int range = (int) Math.round(plugin.store().bedRange());
            String text = anchor == null
                    ? "Euer Team hat noch kein Bett. Das erste Bett, das ihr aufstellt, wird das Stammbett der Werkbank."
                    : "Die Werkbank muss im Umkreis von " + range + " Blöcken vom Stammbett stehen (Bett bei "
                      + anchor.getBlockX() + " " + anchor.getBlockY() + " " + anchor.getBlockZ() + ").";
            p.sendMessage(Game.msg(text, NamedTextColor.RED));
            return;
        }
        plugin.game().benchPlaced(t, p, spot);
    }

    /**
     * A bed placed by a team member becomes the team's Stammbett, but only if there is none yet.
     * Any further beds are ordinary beds and do not change where the bench may stand.
     */
    private void registerBed(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        TeamData t = plugin.store().teamOf(p.getUniqueId());
        if (t == null || plugin.store().anchorBed(t) != null) return;
        t.beds().add(e.getBlockPlaced().getLocation());
        plugin.store().save();
        p.sendMessage(Game.msg("Dieses Bett ist jetzt das Stammbett von Team " + t.id()
                + ". Die Werkbank darf im Umkreis von " + (int) Math.round(plugin.store().bedRange())
                + " Blöcken davon stehen.", NamedTextColor.GREEN));
    }

    // ---------- breaking ----------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        TeamData t = plugin.store().teamAt(b.getLocation());
        if (t == null) return;

        e.setDropItems(false);
        e.setExpToDrop(0);
        Location center = b.getLocation().add(0.5, 0.5, 0.5);
        b.getWorld().dropItem(center, items.create(t));
        plugin.game().benchRemoved(t, e.getPlayer());
    }

    // The bench can only be removed by a player mining it.

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> plugin.store().teamAt(b.getLocation()) != null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> plugin.store().teamAt(b.getLocation()) != null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (anyBench(e.getBlocks())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (anyBench(e.getBlocks())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        if (plugin.store().teamAt(e.getBlock().getLocation()) != null) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        if (plugin.store().teamAt(e.getBlock().getLocation()) != null) e.setCancelled(true);
    }

    private boolean anyBench(List<Block> blocks) {
        for (Block b : blocks) if (plugin.store().teamAt(b.getLocation()) != null) return true;
        return false;
    }

    // ---------- the bench as a dropped item cannot be destroyed ----------

    @EventHandler(ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent e) {
        protect(e.getEntity());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDrop(PlayerDropItemEvent e) {
        protect(e.getItemDrop());
    }

    private void protect(Item item) {
        if (!items.isBench(item.getItemStack())) return;
        item.setUnlimitedLifetime(true);
        item.setInvulnerable(true);
        item.setGlowing(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Item item && items.isBench(item.getItemStack())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDespawn(ItemDespawnEvent e) {
        if (items.isBench(e.getEntity().getItemStack())) e.setCancelled(true);
    }

    // ---------- the bench cannot be burned or crafted away ----------

    @EventHandler(ignoreCancelled = true)
    public void onFuel(FurnaceBurnEvent e) {
        if (items.isBench(e.getFuel())) e.setCancelled(true);
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent e) {
        for (ItemStack it : e.getInventory().getMatrix()) {
            if (items.isBench(it)) {
                e.getInventory().setResult(null);
                return;
            }
        }
    }

    // ---------- no hiding in the ender chest (others could never steal it) ----------

    @EventHandler(ignoreCancelled = true)
    public void onEnderClick(InventoryClickEvent e) {
        if (!plugin.blockEnderChest()) return;
        if (e.getView().getTopInventory().getType() != InventoryType.ENDER_CHEST) return;

        boolean intoTop = e.getClickedInventory() == e.getView().getTopInventory();
        boolean blocked = false;
        if (intoTop && items.isBench(e.getCursor())) blocked = true;
        if (!intoTop && e.isShiftClick() && items.isBench(e.getCurrentItem())) blocked = true;
        if (intoTop && e.getClick() == ClickType.NUMBER_KEY && e.getHotbarButton() >= 0
                && items.isBench(e.getWhoClicked().getInventory().getItem(e.getHotbarButton()))) blocked = true;
        if (intoTop && e.getClick() == ClickType.SWAP_OFFHAND
                && items.isBench(e.getWhoClicked().getInventory().getItemInOffHand())) blocked = true;

        if (blocked) {
            e.setCancelled(true);
            e.getWhoClicked().sendMessage(Game.msg("Team-Werkbänke dürfen nicht in die Enderkiste.", NamedTextColor.RED));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnderDrag(InventoryDragEvent e) {
        if (!plugin.blockEnderChest()) return;
        if (e.getView().getTopInventory().getType() != InventoryType.ENDER_CHEST) return;
        if (!items.isBench(e.getOldCursor())) return;
        int topSize = e.getView().getTopInventory().getSize();
        for (int slot : e.getRawSlots()) {
            if (slot < topSize) {
                e.setCancelled(true);
                return;
            }
        }
    }
}
