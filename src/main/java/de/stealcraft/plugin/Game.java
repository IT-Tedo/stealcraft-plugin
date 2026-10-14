package de.stealcraft.plugin;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/** The StealCraft rules: what happens when a bench is placed, removed, or a player dies. */
public final class Game {

    private final StealCraftPlugin plugin;

    public Game(StealCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public static Component tag() {
        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text("StealCraft", NamedTextColor.GOLD))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY));
    }

    public static Component msg(String text, NamedTextColor color) {
        return tag().append(Component.text(text, color));
    }

    private static Component teamName(TeamData t) {
        return Component.text("Team " + t.id(), t.color()).decoration(TextDecoration.BOLD, true);
    }

    private static Sound sound(String key, float pitch) {
        return Sound.sound(Key.key(key), Sound.Source.MASTER, 1f, pitch);
    }

    private static Title title(Component main, Component sub) {
        return Title.title(main, sub, Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(800)));
    }

    private void toTeam(TeamData t, Component chat, Title title, Sound sound) {
        for (UUID uuid : t.members()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            if (chat != null) p.sendMessage(chat);
            if (title != null) p.showTitle(title);
            if (sound != null) p.playSound(sound, Sound.Emitter.self());
        }
    }

    private void toAll(Component chat, Title title, Sound sound) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (chat != null) p.sendMessage(chat);
            if (title != null) p.showTitle(title);
            if (sound != null) p.playSound(sound, Sound.Emitter.self());
        }
        if (chat != null) Bukkit.getConsoleSender().sendMessage(chat);
    }

    public void benchPlaced(TeamData t, Player placer, Location block) {
        boolean first = !t.activated();
        t.location(block.toBlockLocation());
        t.placed(true);
        t.activated(true);
        plugin.store().save();
        plugin.tab().refresh();

        if (first) {
            toAll(tag().append(teamName(t)).append(Component.text(" hat seine Werkbank aufgestellt. Ab jetzt gelten für sie die StealCraft-Regeln!", NamedTextColor.YELLOW)),
                    null, sound("minecraft:block.beacon.activate", 1f));
            toTeam(t, null,
                    title(Component.text("Werkbank steht", NamedTextColor.GREEN),
                            Component.text("Wird sie abgebaut, habt ihr nur noch ein Leben", NamedTextColor.GRAY)),
                    null);
            warnIfVisible(t, block);
        } else {
            warnIfVisible(t, block);
            toTeam(t, msg("Eure Werkbank steht wieder. Ihr könnt wieder unendlich oft sterben.", NamedTextColor.GREEN),
                    title(Component.text("Geschützt", NamedTextColor.GREEN),
                            Component.text("Eure Werkbank steht wieder", NamedTextColor.GRAY)),
                    sound("minecraft:block.beacon.power_select", 1.2f));
        }
    }

    private void warnIfVisible(TeamData t, Location block) {
        Reveal r = plugin.reveal();
        if (r == null || r.secondsLeft() < 0 || !r.outOfRange(block)) return;
        toTeam(t, msg("Achtung: Eure Werkbank steht unter Y " + r.minY() + " oder über Y " + r.maxY()
                + ". Alle " + r.minutes() + " Minuten sehen alle Spieler, wo sie steht.", NamedTextColor.GOLD), null, null);
    }

    public void benchRemoved(TeamData t, Player breaker) {
        t.placed(false);
        t.location(null);
        plugin.store().save();
        plugin.tab().refresh();

        boolean own = breaker != null && t.isMember(breaker.getUniqueId());
        Component chat;
        if (own) {
            chat = tag().append(teamName(t)).append(Component.text(" hat seine Werkbank abgebaut.", NamedTextColor.YELLOW));
        } else if (breaker != null && plugin.announceThief()) {
            chat = tag().append(Component.text(breaker.getName(), NamedTextColor.WHITE))
                    .append(Component.text(" hat die Werkbank von ", NamedTextColor.RED))
                    .append(teamName(t)).append(Component.text(" geklaut!", NamedTextColor.RED));
        } else {
            chat = tag().append(Component.text("Die Werkbank von ", NamedTextColor.RED))
                    .append(teamName(t)).append(Component.text(" wurde abgebaut!", NamedTextColor.RED));
        }
        toAll(chat, null, sound("minecraft:entity.elder_guardian.curse", 1f));
        toTeam(t, msg("Ohne Werkbank ist jeder Tod endgültig. Holt sie zurück und stellt sie wieder auf!", NamedTextColor.RED),
                title(Component.text("Werkbank weg!", NamedTextColor.RED),
                        Component.text("Ihr habt nur noch ein Leben", NamedTextColor.GRAY)),
                null);
    }

    /** Called from PlayerDeathEvent. Returns true if the player is out. */
    public boolean handleDeath(Player player) {
        TeamData t = plugin.store().teamOf(player.getUniqueId());
        if (t == null || !t.vulnerable() || plugin.store().isEliminated(player.getUniqueId())) return false;

        plugin.store().eliminate(player.getUniqueId());
        plugin.store().save();
        plugin.tab().refresh();

        toAll(tag().append(Component.text(player.getName(), NamedTextColor.WHITE))
                        .append(Component.text(" aus ", NamedTextColor.RED)).append(teamName(t))
                        .append(Component.text(" ist ohne Werkbank gestorben und scheidet aus!", NamedTextColor.RED)),
                title(Component.text("☠ " + player.getName() + " ist raus", NamedTextColor.RED),
                        Component.text("Team " + t.id() + " hatte keine Werkbank", NamedTextColor.GRAY)),
                sound("minecraft:entity.wither.death", 1f));

        boolean teamGone = t.members().stream().allMatch(plugin.store()::isEliminated);
        if (teamGone) {
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    toAll(tag().append(teamName(t)).append(Component.text(" ist komplett ausgeschieden.", NamedTextColor.DARK_RED)),
                            null, sound("minecraft:ui.toast.challenge_complete", 0.6f)), 80L);
        }

        UUID uuid = player.getUniqueId();
        if (plugin.banOnElimination()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Player p = Bukkit.getPlayer(uuid);
                String reason = "Du bist bei StealCraft ohne Werkbank gestorben und ausgeschieden.";
                if (p != null) {
                    p.ban(reason, (Date) null, "StealCraft", true);
                }
            }, 60L);
        }
        return true;
    }

    /** In spectator mode, eliminated players become spectators when they respawn or join. */
    public void applySpectator(Player p) {
        if (!plugin.banOnElimination() && plugin.store().isEliminated(p.getUniqueId())) {
            p.setGameMode(GameMode.SPECTATOR);
        }
    }
}
