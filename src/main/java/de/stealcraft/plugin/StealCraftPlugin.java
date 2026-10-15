// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 IT-Tedo and StealCraft contributors
package de.stealcraft.plugin;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class StealCraftPlugin extends JavaPlugin {

    private DataStore store;
    private BenchItem benchItem;
    private TabDisplay tab;
    private Game game;
    private Reveal reveal;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        store = new DataStore(this);
        store.load();
        benchItem = new BenchItem(this);
        tab = new TabDisplay(this);
        game = new Game(this);
        reveal = new Reveal(this);
        reveal.start();

        getServer().getPluginManager().registerEvents(new BenchListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new DimensionListener(this), this);

        Commands commands = new Commands(this);
        PluginCommand cmd = getCommand("stealcraft");
        if (cmd != null) {
            cmd.setExecutor(commands);
            cmd.setTabCompleter(commands);
        }

        // Twice a second: rescue bench items from the void, check placed benches still exist.
        getServer().getScheduler().runTaskTimer(this, new Watchdog(this), 20L, 10L);

        tab.refresh();
        getLogger().info("StealCraft ist aktiv. Teams: " + store.teams().size());
    }

    @Override
    public void onDisable() {
        if (reveal != null) reveal.stop();
        if (store != null) store.save();
    }

    public Reveal reveal() { return reveal; }

    public DataStore store() { return store; }
    public BenchItem benchItem() { return benchItem; }
    public TabDisplay tab() { return tab; }
    public Game game() { return game; }

    public boolean banOnElimination() {
        return !"ZUSCHAUER".equalsIgnoreCase(getConfig().getString("ausscheiden", "BAN"));
    }

    public boolean announceThief() {
        return getConfig().getBoolean("dieb-verraten", false);
    }

    public boolean blockEnderChest() {
        return getConfig().getBoolean("enderkiste-sperren", true);
    }
}
