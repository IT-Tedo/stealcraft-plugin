// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 IT-Tedo and StealCraft contributors
package de.stealcraft.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Shows each player's team and bench state in front of their name (tab list and above the head):
 * green heart = bench stands, red heart = only one life left, grey heart = rules not active yet,
 * skull = eliminated.
 */
public final class TabDisplay {

    private static final String PREFIX = "sc_";
    private final StealCraftPlugin plugin;

    public TabDisplay(StealCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void refresh() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        DataStore store = plugin.store();

        // Remove scoreboard teams of deleted StealCraft teams.
        for (Team sbTeam : new HashSet<>(board.getTeams())) {
            if (sbTeam.getName().startsWith(PREFIX) && store.team(sbTeam.getName().substring(PREFIX.length())) == null
                    && !sbTeam.getName().endsWith("_raus")) {
                sbTeam.unregister();
            }
        }

        Team out = board.getTeam(PREFIX + "raus");
        if (out == null) out = board.registerNewTeam(PREFIX + "raus");
        out.prefix(Component.text("☠ ", NamedTextColor.DARK_GRAY));
        out.color(NamedTextColor.DARK_GRAY);

        for (TeamData t : store.teams()) {
            Team sbTeam = board.getTeam(PREFIX + t.id());
            if (sbTeam == null) sbTeam = board.registerNewTeam(PREFIX + t.id());

            NamedTextColor heart = !t.activated() ? NamedTextColor.GRAY
                    : t.placed() ? NamedTextColor.GREEN : NamedTextColor.RED;
            sbTeam.prefix(Component.text("❤ ", heart));
            sbTeam.color(t.color());
            sbTeam.suffix(Component.text(" [" + t.id() + "]", NamedTextColor.DARK_GRAY));

            Set<String> wanted = new HashSet<>();
            for (UUID uuid : t.members()) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                String name = op.getName();
                if (name == null) continue;
                if (store.isEliminated(uuid)) {
                    out.addEntry(name);
                } else {
                    wanted.add(name);
                }
            }
            for (String entry : new HashSet<>(sbTeam.getEntries())) {
                if (!wanted.contains(entry)) sbTeam.removeEntry(entry);
            }
            for (String name : wanted) {
                if (!sbTeam.hasEntry(name)) sbTeam.addEntry(name);
            }
        }
    }
}
