// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 IT-Tedo and StealCraft contributors
package de.stealcraft.plugin;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** One StealCraft team and the state of its bench. */
public final class TeamData {

    private final String id;
    private NamedTextColor color;
    private final Set<UUID> members = new LinkedHashSet<>();

    /** True once the team placed its bench for the first time. Before that, normal Minecraft rules apply. */
    private boolean activated;
    /** True while the bench stands as a block in the world. */
    private boolean placed;
    private Location location;
    /** Raised when an admin hands out a replacement bench; older bench items stop working. */
    private int benchVersion = 1;

    public TeamData(String id, NamedTextColor color) {
        this.id = id;
        this.color = color;
    }

    public String id() { return id; }
    public NamedTextColor color() { return color; }
    public void color(NamedTextColor color) { this.color = color; }
    public Set<UUID> members() { return members; }
    public boolean isMember(UUID uuid) { return members.contains(uuid); }

    public boolean activated() { return activated; }
    public void activated(boolean activated) { this.activated = activated; }
    public boolean placed() { return placed; }
    public void placed(boolean placed) { this.placed = placed; }
    public Location location() { return location; }
    public void location(Location location) { this.location = location; }
    public int benchVersion() { return benchVersion; }
    public void benchVersion(int benchVersion) { this.benchVersion = benchVersion; }

    /** Deaths are only final once the rules are active and the bench is not standing. */
    public boolean vulnerable() {
        return activated && !placed;
    }
}
