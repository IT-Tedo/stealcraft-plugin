// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 IT-Tedo and StealCraft contributors
package de.stealcraft.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class PlayerListener implements Listener {

    private final StealCraftPlugin plugin;

    public PlayerListener(StealCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        plugin.game().handleDeath(e.getEntity());
    }

    /** Backup for the ban: eliminated players stay out even if the ban list is edited. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        if (plugin.banOnElimination() && plugin.store().isEliminated(e.getUniqueId())) {
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    Component.text("Du bist bei StealCraft ausgeschieden.", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.tab().refresh();
        plugin.game().applySpectator(e.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.game().applySpectator(e.getPlayer()));
    }
}
