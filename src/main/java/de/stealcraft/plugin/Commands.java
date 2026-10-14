// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 IT-Tedo and StealCraft contributors
package de.stealcraft.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /sc team erstellen <name> [farbe]   /sc team löschen <name>
 * /sc team hinzufügen <name> <spieler> /sc team entfernen <spieler>   /sc team liste
 * /sc werkbank <team> [spieler]        /sc status
 * /sc wiederbeleben <spieler>          /sc zurücksetzen <team>
 */
public final class Commands implements TabExecutor {

    private static final String ADMIN = "stealcraft.admin";
    private final StealCraftPlugin plugin;

    public Commands(StealCraftPlugin plugin) {
        this.plugin = plugin;
    }

    private static void ok(CommandSender s, String text) { s.sendMessage(Game.msg(text, NamedTextColor.GREEN)); }
    private static void err(CommandSender s, String text) { s.sendMessage(Game.msg(text, NamedTextColor.RED)); }
    private static void info(CommandSender s, String text) { s.sendMessage(Game.msg(text, NamedTextColor.GRAY)); }

    @Override
    public boolean onCommand(CommandSender s, Command command, String label, String[] a) {
        if (a.length == 0 || a[0].equalsIgnoreCase("hilfe")) {
            help(s);
            return true;
        }
        String sub = a[0].toLowerCase();
        if (sub.equals("status")) {
            status(s);
            return true;
        }
        if (!s.hasPermission(ADMIN)) {
            err(s, "Dafür brauchst du Admin-Rechte.");
            return true;
        }
        switch (sub) {
            case "team" -> team(s, a);
            case "werkbank" -> giveBench(s, a);
            case "wiederbeleben" -> revive(s, a);
            case "zurücksetzen", "zuruecksetzen" -> reset(s, a);
            case "anzeige" -> {
                int n = plugin.reveal().reveal();
                if (n == 0) info(s, "Gerade steht keine Werkbank unter Y " + plugin.reveal().minY() + " oder über Y " + plugin.reveal().maxY() + ".");
                else ok(s, n + " Werkbank(en) angezeigt.");
            }
            default -> help(s);
        }
        return true;
    }

    private void help(CommandSender s) {
        info(s, "Befehle:");
        s.sendMessage(Component.text("/sc status", NamedTextColor.YELLOW).append(Component.text(" – Teams und Werkbänke anzeigen", NamedTextColor.GRAY)));
        if (!s.hasPermission(ADMIN)) return;
        s.sendMessage(Component.text("/sc team erstellen <name> [farbe]", NamedTextColor.YELLOW));
        s.sendMessage(Component.text("/sc team hinzufügen <name> <spieler>", NamedTextColor.YELLOW));
        s.sendMessage(Component.text("/sc team entfernen <spieler>", NamedTextColor.YELLOW));
        s.sendMessage(Component.text("/sc team löschen <name>  ·  /sc team liste", NamedTextColor.YELLOW));
        s.sendMessage(Component.text("/sc werkbank <team> [spieler]", NamedTextColor.YELLOW).append(Component.text(" – Werkbank geben (alte wird ungültig)", NamedTextColor.GRAY)));
        s.sendMessage(Component.text("/sc wiederbeleben <spieler>", NamedTextColor.YELLOW).append(Component.text(" – Ausgeschiedenen zurückholen", NamedTextColor.GRAY)));
        s.sendMessage(Component.text("/sc zurücksetzen <team>", NamedTextColor.YELLOW).append(Component.text(" – Regeln für ein Team wieder auf Anfang", NamedTextColor.GRAY)));
        s.sendMessage(Component.text("/sc anzeige", NamedTextColor.YELLOW).append(Component.text(" – Werkbank-Anzeige sofort auslösen", NamedTextColor.GRAY)));
    }

    // ---------- /sc team ----------

    private void team(CommandSender s, String[] a) {
        if (a.length < 2) { help(s); return; }
        String op = a[1].toLowerCase();
        switch (op) {
            case "erstellen" -> {
                if (a.length < 3) { err(s, "/sc team erstellen <name> [farbe]"); return; }
                String id = a[2].toLowerCase();
                if (!id.matches("[a-z0-9_]{1,16}") || id.equals("raus")) {
                    err(s, "Teamnamen: 1–16 Zeichen, nur a–z, 0–9 und _ (nicht „raus“).");
                    return;
                }
                if (plugin.store().team(id) != null) { err(s, "Team " + id + " gibt es schon."); return; }
                NamedTextColor color = NamedTextColor.WHITE;
                if (a.length >= 4) {
                    color = NamedTextColor.NAMES.value(a[3].toLowerCase());
                    if (color == null) { err(s, "Unbekannte Farbe. Möglich: " + String.join(", ", NamedTextColor.NAMES.keys())); return; }
                }
                plugin.store().addTeam(new TeamData(id, color));
                plugin.store().save();
                plugin.tab().refresh();
                ok(s, "Team " + id + " erstellt. Füge Spieler mit /sc team hinzufügen " + id + " <spieler> hinzu.");
            }
            case "löschen", "loeschen" -> {
                if (a.length < 3) { err(s, "/sc team löschen <name>"); return; }
                TeamData t = plugin.store().team(a[2]);
                if (t == null) { err(s, "Team " + a[2] + " gibt es nicht."); return; }
                plugin.store().removeTeam(t.id());
                plugin.store().save();
                plugin.tab().refresh();
                ok(s, "Team " + t.id() + " gelöscht.");
            }
            case "hinzufügen", "hinzufuegen" -> {
                if (a.length < 4) { err(s, "/sc team hinzufügen <name> <spieler>"); return; }
                TeamData t = plugin.store().team(a[2]);
                if (t == null) { err(s, "Team " + a[2] + " gibt es nicht."); return; }
                OfflinePlayer target = findPlayer(a[3]);
                if (target == null) { err(s, a[3] + " war noch nie auf dem Server. Der Spieler muss einmal joinen."); return; }
                for (TeamData other : plugin.store().teams()) other.members().remove(target.getUniqueId());
                t.members().add(target.getUniqueId());
                plugin.store().save();
                plugin.tab().refresh();
                ok(s, target.getName() + " ist jetzt in Team " + t.id() + ".");
            }
            case "entfernen" -> {
                if (a.length < 3) { err(s, "/sc team entfernen <spieler>"); return; }
                OfflinePlayer target = findPlayer(a[2]);
                TeamData t = target == null ? null : plugin.store().teamOf(target.getUniqueId());
                if (t == null) { err(s, a[2] + " ist in keinem Team."); return; }
                t.members().remove(target.getUniqueId());
                plugin.store().save();
                plugin.tab().refresh();
                ok(s, target.getName() + " ist nicht mehr in Team " + t.id() + ".");
            }
            case "liste" -> {
                if (plugin.store().teams().isEmpty()) { info(s, "Noch keine Teams."); return; }
                for (TeamData t : plugin.store().teams()) {
                    s.sendMessage(Component.text("Team " + t.id(), t.color())
                            .append(Component.text(": " + names(t), NamedTextColor.GRAY)));
                }
            }
            default -> help(s);
        }
    }

    // ---------- /sc werkbank ----------

    private void giveBench(CommandSender s, String[] a) {
        if (a.length < 2) { err(s, "/sc werkbank <team> [spieler]"); return; }
        TeamData t = plugin.store().team(a[1]);
        if (t == null) { err(s, "Team " + a[1] + " gibt es nicht."); return; }
        if (t.placed() && t.location() != null) {
            Location l = t.location();
            err(s, "Die Werkbank von Team " + t.id() + " steht schon bei " + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ() + ".");
            return;
        }
        Player target;
        if (a.length >= 3) {
            target = Bukkit.getPlayerExact(a[2]);
            if (target == null) { err(s, a[2] + " ist nicht online."); return; }
        } else if (s instanceof Player p) {
            target = p;
        } else {
            err(s, "Gib an, wer die Werkbank bekommen soll: /sc werkbank " + t.id() + " <spieler>");
            return;
        }
        boolean replacing = t.activated() || t.benchVersion() > 1;
        if (replacing) t.benchVersion(t.benchVersion() + 1);
        plugin.store().save();

        ItemStack bench = plugin.benchItem().create(t);
        Map<Integer, ItemStack> left = target.getInventory().addItem(bench);
        left.values().forEach(it -> target.getWorld().dropItem(target.getLocation(), it));
        ok(s, target.getName() + " hat die Werkbank von Team " + t.id() + " bekommen."
                + (replacing ? " Alle älteren Werkbänke dieses Teams sind jetzt ungültig." : ""));
    }

    // ---------- /sc wiederbeleben ----------

    private void revive(CommandSender s, String[] a) {
        if (a.length < 2) { err(s, "/sc wiederbeleben <spieler>"); return; }
        OfflinePlayer target = findPlayer(a[1]);
        if (target == null) { err(s, "Spieler " + a[1] + " nicht gefunden."); return; }
        UUID uuid = target.getUniqueId();
        boolean was = plugin.store().revive(uuid);
        plugin.store().save();

        ProfileBanList bans = Bukkit.getBanList(BanList.Type.PROFILE);
        bans.pardon(target.getPlayerProfile());

        Player online = target.getPlayer();
        if (online != null && online.getGameMode() == GameMode.SPECTATOR) online.setGameMode(GameMode.SURVIVAL);
        plugin.tab().refresh();
        ok(s, target.getName() + (was ? " ist wieder im Spiel." : " war nicht ausgeschieden, Bann wurde trotzdem aufgehoben."));
    }

    // ---------- /sc zurücksetzen ----------

    private void reset(CommandSender s, String[] a) {
        if (a.length < 2) { err(s, "/sc zurücksetzen <team>"); return; }
        TeamData t = plugin.store().team(a[1]);
        if (t == null) { err(s, "Team " + a[1] + " gibt es nicht."); return; }
        t.activated(false);
        t.placed(false);
        t.location(null);
        plugin.store().save();
        plugin.tab().refresh();
        ok(s, "Team " + t.id() + " ist zurückgesetzt. Bis die Werkbank wieder steht, gelten normale Regeln.");
    }

    // ---------- /sc status ----------

    private void status(CommandSender s) {
        if (plugin.store().teams().isEmpty()) { info(s, "Noch keine Teams."); return; }
        boolean admin = s.hasPermission(ADMIN);
        s.sendMessage(Game.tag().append(Component.text("Status", NamedTextColor.WHITE)));
        long left = plugin.reveal().secondsLeft();
        if (left >= 0) {
            s.sendMessage(Component.text("Nächste Werkbank-Anzeige in " + (left / 60) + " Min " + (left % 60) + " Sek"
                    + " (nur Werkbänke unter Y " + plugin.reveal().minY() + " oder über Y " + plugin.reveal().maxY() + ")", NamedTextColor.GOLD));
        }
        for (TeamData t : plugin.store().teams()) {
            Component state;
            if (!t.activated()) {
                state = Component.text("❤ noch nicht aufgestellt, normale Regeln", NamedTextColor.GRAY);
            } else if (t.placed()) {
                state = Component.text("❤ Werkbank steht", NamedTextColor.GREEN);
                if (admin && t.location() != null) {
                    Location l = t.location();
                    state = state.append(Component.text(" (" + l.getWorld().getName() + " " + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ() + ")", NamedTextColor.DARK_GRAY));
                }
            } else {
                state = Component.text("❤ Werkbank weg, nur noch ein Leben", NamedTextColor.RED);
            }
            s.sendMessage(Component.text("Team " + t.id() + " ", t.color()).append(state));
            s.sendMessage(Component.text("   " + names(t), NamedTextColor.GRAY));
        }
    }

    // ---------- helpers ----------

    private String names(TeamData t) {
        if (t.members().isEmpty()) return "(keine Mitglieder)";
        List<String> out = new ArrayList<>();
        for (UUID uuid : t.members()) {
            String n = Bukkit.getOfflinePlayer(uuid).getName();
            if (n == null) n = uuid.toString().substring(0, 8);
            out.add(plugin.store().isEliminated(uuid) ? "☠" + n : n);
        }
        return String.join(", ", out);
    }

    private OfflinePlayer findPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        if (cached != null && cached.hasPlayedBefore()) return cached;
        return null;
    }

    // ---------- tab completion ----------

    @Override
    public List<String> onTabComplete(CommandSender s, Command command, String label, String[] a) {
        List<String> opts = new ArrayList<>();
        boolean admin = s.hasPermission(ADMIN);
        if (a.length == 1) {
            opts.add("status");
            opts.add("hilfe");
            if (admin) opts.addAll(List.of("team", "werkbank", "wiederbeleben", "zurücksetzen", "anzeige"));
        } else if (admin && a[0].equalsIgnoreCase("team")) {
            if (a.length == 2) opts.addAll(List.of("erstellen", "hinzufügen", "entfernen", "löschen", "liste"));
            else if (a.length == 3 && !a[1].equalsIgnoreCase("erstellen") && !a[1].equalsIgnoreCase("entfernen")) teamIds(opts);
            else if (a.length == 3 && a[1].equalsIgnoreCase("entfernen")) onlineNames(opts);
            else if (a.length == 4 && a[1].equalsIgnoreCase("erstellen")) opts.addAll(NamedTextColor.NAMES.keys());
            else if (a.length == 4 && a[1].toLowerCase().startsWith("hinzuf")) onlineNames(opts);
        } else if (admin && (a[0].equalsIgnoreCase("werkbank") || a[0].toLowerCase().startsWith("zur"))) {
            if (a.length == 2) teamIds(opts);
            else if (a.length == 3 && a[0].equalsIgnoreCase("werkbank")) onlineNames(opts);
        } else if (admin && a[0].equalsIgnoreCase("wiederbeleben") && a.length == 2) {
            onlineNames(opts);
        }
        String last = a[a.length - 1].toLowerCase();
        opts.removeIf(o -> !o.toLowerCase().startsWith(last));
        return opts;
    }

    private void teamIds(List<String> opts) {
        for (TeamData t : plugin.store().teams()) opts.add(t.id());
    }

    private void onlineNames(List<String> opts) {
        for (Player p : Bukkit.getOnlinePlayers()) opts.add(p.getName());
    }
}
