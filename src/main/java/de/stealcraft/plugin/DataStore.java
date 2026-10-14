// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 IT-Tedo and StealCraft contributors
package de.stealcraft.plugin;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps teams and eliminated players in plugins/StealCraft/daten.yml. */
public final class DataStore {

    private final StealCraftPlugin plugin;
    private final File file;
    private final Map<String, TeamData> teams = new LinkedHashMap<>();
    private final Set<UUID> eliminated = ConcurrentHashMap.newKeySet();

    public DataStore(StealCraftPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "daten.yml");
    }

    public void load() {
        teams.clear();
        eliminated.clear();
        if (!file.exists()) return;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = yml.getConfigurationSection("teams");
        if (sec != null) {
            for (String id : sec.getKeys(false)) {
                ConfigurationSection t = sec.getConfigurationSection(id);
                if (t == null) continue;
                NamedTextColor color = NamedTextColor.NAMES.value(t.getString("farbe", "white"));
                TeamData team = new TeamData(id, color != null ? color : NamedTextColor.WHITE);
                for (String s : t.getStringList("mitglieder")) {
                    try { team.members().add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) { }
                }
                team.activated(t.getBoolean("aktiv"));
                team.placed(t.getBoolean("platziert"));
                team.benchVersion(t.getInt("werkbank-version", 1));
                String world = t.getString("ort.welt");
                if (world != null) {
                    World w = Bukkit.getWorld(world);
                    if (w != null) {
                        team.location(new Location(w, t.getInt("ort.x"), t.getInt("ort.y"), t.getInt("ort.z")));
                    }
                }
                if (team.placed() && team.location() == null) team.placed(false);
                teams.put(id, team);
            }
        }
        for (String s : yml.getStringList("ausgeschieden")) {
            try { eliminated.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) { }
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (TeamData team : teams.values()) {
            String p = "teams." + team.id() + ".";
            yml.set(p + "farbe", NamedTextColor.NAMES.key(team.color()));
            yml.set(p + "mitglieder", team.members().stream().map(UUID::toString).toList());
            yml.set(p + "aktiv", team.activated());
            yml.set(p + "platziert", team.placed());
            yml.set(p + "werkbank-version", team.benchVersion());
            Location l = team.location();
            if (l != null && l.getWorld() != null) {
                yml.set(p + "ort.welt", l.getWorld().getName());
                yml.set(p + "ort.x", l.getBlockX());
                yml.set(p + "ort.y", l.getBlockY());
                yml.set(p + "ort.z", l.getBlockZ());
            }
        }
        yml.set("ausgeschieden", eliminated.stream().map(UUID::toString).toList());
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Konnte daten.yml nicht speichern: " + e.getMessage());
        }
    }

    public Collection<TeamData> teams() { return teams.values(); }
    public TeamData team(String id) { return id == null ? null : teams.get(id.toLowerCase()); }
    public void addTeam(TeamData team) { teams.put(team.id(), team); }
    public void removeTeam(String id) { teams.remove(id.toLowerCase()); }

    public TeamData teamOf(UUID player) {
        for (TeamData t : teams.values()) if (t.isMember(player)) return t;
        return null;
    }

    /** The team whose bench stands at this block, or null. */
    public TeamData teamAt(Location block) {
        if (block == null || block.getWorld() == null) return null;
        for (TeamData t : teams.values()) {
            Location l = t.location();
            if (!t.placed() || l == null || l.getWorld() == null) continue;
            if (l.getWorld().equals(block.getWorld())
                    && l.getBlockX() == block.getBlockX()
                    && l.getBlockY() == block.getBlockY()
                    && l.getBlockZ() == block.getBlockZ()) {
                return t;
            }
        }
        return null;
    }

    public boolean isEliminated(UUID uuid) { return eliminated.contains(uuid); }
    public void eliminate(UUID uuid) { eliminated.add(uuid); }
    public boolean revive(UUID uuid) { return eliminated.remove(uuid); }
}
