// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 IT-Tedo and StealCraft contributors
package de.stealcraft.plugin;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
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
    private final Set<World.Environment> gesperrt = EnumSet.noneOf(World.Environment.class);

    public DataStore(StealCraftPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "daten.yml");
    }

    public void load() {
        teams.clear();
        eliminated.clear();
        gesperrt.clear();
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
                for (String b : t.getStringList("betten")) {
                    String[] parts = b.split(";");
                    if (parts.length != 4) continue;
                    World bw = Bukkit.getWorld(parts[0]);
                    if (bw == null) continue;
                    try {
                        team.beds().add(new Location(bw, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3])));
                    } catch (NumberFormatException ignored) { }
                }
                if (team.placed() && team.location() == null) team.placed(false);
                teams.put(id, team);
            }
        }
        for (String s : yml.getStringList("ausgeschieden")) {
            try { eliminated.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) { }
        }
        for (String s : yml.getStringList("gesperrt")) {
            try { gesperrt.add(World.Environment.valueOf(s)); } catch (IllegalArgumentException ignored) { }
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
            yml.set(p + "betten", team.beds().stream()
                    .filter(b -> b.getWorld() != null)
                    .map(b -> b.getWorld().getName() + ";" + b.getBlockX() + ";" + b.getBlockY() + ";" + b.getBlockZ())
                    .toList());
            Location l = team.location();
            if (l != null && l.getWorld() != null) {
                yml.set(p + "ort.welt", l.getWorld().getName());
                yml.set(p + "ort.x", l.getBlockX());
                yml.set(p + "ort.y", l.getBlockY());
                yml.set(p + "ort.z", l.getBlockZ());
            }
        }
        yml.set("ausgeschieden", eliminated.stream().map(UUID::toString).toList());
        yml.set("gesperrt", gesperrt.stream().map(Enum::name).toList());
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

    /** Largest distance in blocks between a team bench and one of the team's beds. */
    public double bedRange() {
        return Math.max(0, plugin.getConfig().getDouble("bett-abstand", 50));
    }

    /** The two halves of a bed touch each other; anything within this distance counts as the same bed. */
    private static boolean sameBed(Location stored, Location block) {
        return stored.getWorld() != null && stored.getWorld().equals(block.getWorld()) && stored.distance(block) <= 1.01;
    }

    /** The team whose Stammbett (either half) is at this block, or null. */
    public TeamData bedOwner(Location block) {
        if (block == null || block.getWorld() == null) return null;
        for (TeamData t : teams.values()) {
            for (Location bed : t.beds()) if (sameBed(bed, block)) return t;
        }
        return null;
    }

    /** Forgets the Stammbett at this block right away, so it can not come back by accident. */
    public void forgetBed(TeamData team, Location block) {
        team.beds().removeIf(bed -> sameBed(bed, block));
    }

    /** Removes beds that no longer exist (broken, exploded), keeping the order of the rest. */
    public void pruneBeds(TeamData team) {
        Iterator<Location> it = team.beds().iterator();
        while (it.hasNext()) {
            Location bed = it.next();
            if (bed.getWorld() == null) { it.remove(); continue; }
            if (bed.isChunkLoaded() && !Tag.BEDS.isTagged(bed.getBlock().getType())) it.remove();
        }
    }

    /**
     * The team's Stammbett: the first bed it placed that still exists. The bench is bound to it.
     * If it is broken, the next bed placed by a team member takes its place.
     */
    public Location anchorBed(TeamData team) {
        pruneBeds(team);
        return team.beds().isEmpty() ? null : team.beds().get(0);
    }

    /** True if the spot is within bedRange() of the team's Stammbett. */
    public boolean bedInRange(TeamData team, Location spot) {
        Location anchor = anchorBed(team);
        if (anchor == null || anchor.getWorld() == null || !anchor.getWorld().equals(spot.getWorld())) return false;
        return anchor.distance(spot) <= bedRange();
    }

    /** Nether and End can be locked by an operator. Locked means nobody can enter that dimension. */
    public boolean isLocked(World.Environment env) { return gesperrt.contains(env); }

    public void setLocked(World.Environment env, boolean locked) {
        if (locked) gesperrt.add(env); else gesperrt.remove(env);
        save();
    }

    public boolean isEliminated(UUID uuid) { return eliminated.contains(uuid); }
    public void eliminate(UUID uuid) { eliminated.add(uuid); }
    public boolean revive(UUID uuid) { return eliminated.remove(uuid); }
}
