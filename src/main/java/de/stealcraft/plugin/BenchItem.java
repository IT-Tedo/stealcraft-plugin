package de.stealcraft.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/** Creates and recognises team benches. The team is stored invisibly inside the item. */
public final class BenchItem {

    private final NamespacedKey teamKey;
    private final NamespacedKey versionKey;

    public BenchItem(StealCraftPlugin plugin) {
        this.teamKey = new NamespacedKey(plugin, "team");
        this.versionKey = new NamespacedKey(plugin, "version");
    }

    public ItemStack create(TeamData team) {
        ItemStack item = new ItemStack(Material.CRAFTING_TABLE);
        item.editMeta(meta -> {
            meta.displayName(Component.text("Werkbank von Team " + team.id(), team.color())
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, true));
            meta.lore(List.of(
                    line("Steht sie, könnt ihr unendlich oft sterben.", NamedTextColor.GRAY),
                    line("Ist sie weg, hat jeder nur noch ein Leben.", NamedTextColor.GRAY),
                    line("Nur Team " + team.id() + " kann sie aufstellen.", NamedTextColor.DARK_GRAY)
            ));
            meta.setEnchantmentGlintOverride(true);
            meta.setMaxStackSize(1);
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(teamKey, PersistentDataType.STRING, team.id());
            pdc.set(versionKey, PersistentDataType.INTEGER, team.benchVersion());
        });
        return item;
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    public boolean isBench(ItemStack item) {
        return teamOf(item) != null;
    }

    public String teamOf(ItemStack item) {
        if (item == null || item.getType() != Material.CRAFTING_TABLE || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(teamKey, PersistentDataType.STRING);
    }

    public int versionOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return -1;
        Integer v = item.getItemMeta().getPersistentDataContainer().get(versionKey, PersistentDataType.INTEGER);
        return v == null ? -1 : v;
    }
}
