package de.stealcraft.plugin;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Every X minutes, benches standing very deep (below min-y) or very high (above max-y)
 * are revealed to everyone: chat message with coordinates, a sound, and a particle beam for a while.
 */
public final class Reveal {

    private final StealCraftPlugin plugin;
    private BukkitTask timer;
    private BukkitTask warning;
    private BukkitTask beam;
    private long nextAt;

    public Reveal(StealCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public int minutes() { return Math.max(1, plugin.getConfig().getInt("anzeige.intervall-minuten", 30)); }
    public int minY() { return plugin.getConfig().getInt("anzeige.unter-y", -30); }
    public int maxY() { return plugin.getConfig().getInt("anzeige.ueber-y", 95); }
    private int beamSeconds() { return Math.max(0, plugin.getConfig().getInt("anzeige.strahl-sekunden", 60)); }
    private boolean enabled() { return plugin.getConfig().getBoolean("anzeige.aktiv", true); }

    public void start() {
        stop();
        if (!enabled()) return;
        long period = minutes() * 60L * 20L;
        nextAt = System.currentTimeMillis() + minutes() * 60_000L;
        timer = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            nextAt = System.currentTimeMillis() + minutes() * 60_000L;
            reveal();
        }, period, period);
        scheduleWarning();
    }

    public void stop() {
        if (timer != null) timer.cancel();
        if (warning != null) warning.cancel();
        if (beam != null) beam.cancel();
        timer = warning = beam = null;
    }

    /** Seconds until the next reveal, or -1 if switched off. */
    public long secondsLeft() {
        if (timer == null) return -1;
        return Math.max(0, (nextAt - System.currentTimeMillis()) / 1000);
    }

    // One minute before each reveal, warn teams whose bench would be shown.
    private void scheduleWarning() {
        if (minutes() < 2) return;
        long period = minutes() * 60L * 20L;
        warning = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (TeamData t : hidden()) {
                for (var uuid : t.members()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) p.sendMessage(Game.msg("In 1 Minute wird allen angezeigt, wo eure Werkbank steht!", NamedTextColor.GOLD));
                }
            }
        }, period - 1200L, period);
    }

    public boolean outOfRange(Location l) {
        return l.getBlockY() < minY() || l.getBlockY() > maxY();
    }

    private List<TeamData> hidden() {
        List<TeamData> out = new ArrayList<>();
        for (TeamData t : plugin.store().teams()) {
            Location l = t.location();
            if (t.placed() && l != null && l.getWorld() != null && outOfRange(l)) out.add(t);
        }
        return out;
    }

    private static String worldName(World w) {
        return switch (w.getEnvironment()) {
            case NETHER -> "Nether";
            case THE_END -> "End";
            default -> "Oberwelt";
        };
    }

    /** Shows all hidden benches now. Returns how many were shown. */
    public int reveal() {
        List<TeamData> list = hidden();
        if (list.isEmpty()) return 0;

        List<Component> lines = new ArrayList<>();
        lines.add(Game.tag().append(Component.text("Werkbank-Anzeige! ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
                .append(Component.text("Diese Werkbänke stehen unter Y " + minY() + " oder über Y " + maxY() + ":", NamedTextColor.YELLOW)));
        for (TeamData t : list) {
            Location l = t.location();
            lines.add(Component.text("  ❤ Team " + t.id(), t.color()).decoration(TextDecoration.BOLD, true)
                    .append(Component.text("  " + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ(), NamedTextColor.WHITE))
                    .append(Component.text("  (" + worldName(l.getWorld()) + ")", NamedTextColor.GRAY)));
        }
        if (beamSeconds() > 0) {
            lines.add(Component.text("  Ein Lichtstrahl zeigt " + beamSeconds() + " Sekunden lang, wo sie stehen.", NamedTextColor.GRAY));
        }

        Sound sound = Sound.sound(Key.key("minecraft:block.bell.use"), Sound.Source.MASTER, 1f, 0.8f);
        for (Player p : Bukkit.getOnlinePlayers()) {
            lines.forEach(p::sendMessage);
            p.playSound(sound, Sound.Emitter.self());
        }
        lines.forEach(c -> Bukkit.getConsoleSender().sendMessage(c));

        startBeam(list);
        return list.size();
    }

    // A tall column of particles above (and below) each revealed bench, visible from far away.
    private void startBeam(List<TeamData> list) {
        if (beam != null) beam.cancel();
        int seconds = beamSeconds();
        if (seconds <= 0) return;
        List<Location> spots = new ArrayList<>();
        for (TeamData t : list) spots.add(t.location().clone().add(0.5, 0.5, 0.5));
        final int[] ticks = {0};
        beam = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ticks[0] += 5;
            if (ticks[0] > seconds * 20) {
                beam.cancel();
                beam = null;
                return;
            }
            for (Location base : spots) {
                World w = base.getWorld();
                if (w == null) continue;
                for (int dy = -40; dy <= 60; dy += 2) {
                    Location at = base.clone().add(0, dy, 0);
                    w.spawnParticle(Particle.END_ROD, at, 1, 0.05, 0.4, 0.05, 0.0, null, true);
                }
            }
        }, 0L, 5L);
    }
}
