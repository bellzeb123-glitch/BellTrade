package pl.bell.trade.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import pl.bell.trade.BellTrade;
import pl.bell.trade.config.LangManager;
import pl.bell.trade.model.TradeSession;

import java.util.ArrayList;
import java.util.List;

public class TradeGUI {

    public static final int[] SLOTS_SELF = {19, 20, 21, 22};
    public static final int[] SLOTS_PARTNER = {10, 11, 12, 13};

    public static final int SLOT_MONEY_MINUS_10 = 29;
    public static final int SLOT_MONEY_MINUS_1 = 30;
    public static final int SLOT_MONEY_DISPLAY = 31;
    public static final int SLOT_MONEY_PLUS_1 = 32;
    public static final int SLOT_MONEY_PLUS_10 = 33;
    public static final int SLOT_PARTNER_MONEY = 14;
    public static final int SLOT_CANCEL = 45;
    public static final int SLOT_PARTNER_STATUS = 48;
    public static final int SLOT_CONFIRM = 49;

    private final BellTrade plugin;

    public TradeGUI(BellTrade plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, TradeSession session) {
        LangManager lang = plugin.getLangManager();
        GuiHolder holder = new GuiHolder(GuiHolder.Type.TRADE, session.getSessionId());
        Inventory inv = Bukkit.createInventory(holder, 54, lang.colorize(lang.getRaw("trade.gui-title")));
        holder.setInventory(inv);
        buildStatic(inv, player, session, lang);
        syncPartnerItems(inv, player, session);
        syncSelfItemsFromSession(inv, player, session);
        updateMoneyAndStatus(inv, player, session, lang);
        player.openInventory(inv);
    }

    public void refresh(TradeSession session) {
        Player a = Bukkit.getPlayer(session.getPlayerA());
        Player b = Bukkit.getPlayer(session.getPlayerB());
        if (a != null && a.getOpenInventory().getTopInventory().getHolder() instanceof GuiHolder h
            && h.getType() == GuiHolder.Type.TRADE
            && session.getSessionId().equals(h.getSessionId())) {
            refreshPlayer(a, session);
        }
        if (b != null && b.getOpenInventory().getTopInventory().getHolder() instanceof GuiHolder h
            && h.getType() == GuiHolder.Type.TRADE
            && session.getSessionId().equals(h.getSessionId())) {
            refreshPlayer(b, session);
        }
    }

    public void refreshPlayer(Player player, TradeSession session) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (!(inv.getHolder() instanceof GuiHolder)) return;
        LangManager lang = plugin.getLangManager();
        syncPartnerItems(inv, player, session);
        updateMoneyAndStatus(inv, player, session, lang);
    }

    public void syncSessionFromInventory(Player player, TradeSession session) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        ItemStack[] offer = session.getOffer(player.getUniqueId());
        for (int i = 0; i < SLOTS_SELF.length; i++) {
            ItemStack stack = inv.getItem(SLOTS_SELF[i]);
            offer[i] = stack != null && !stack.getType().isAir() ? stack.clone() : null;
        }
    }

    public ItemStack[] collectOfferItems(Player player, TradeSession session) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        ItemStack[] items = new ItemStack[SLOTS_SELF.length];
        for (int i = 0; i < SLOTS_SELF.length; i++) {
            ItemStack stack = inv.getItem(SLOTS_SELF[i]);
            if (stack != null && !stack.getType().isAir()) {
                items[i] = stack.clone();
                inv.setItem(SLOTS_SELF[i], null);
            }
        }
        return items;
    }

    public void returnItems(Player player, TradeSession session) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (inv.getHolder() instanceof GuiHolder h && h.getType() == GuiHolder.Type.TRADE) {
            for (int slot : SLOTS_SELF) {
                ItemStack stack = inv.getItem(slot);
                if (stack != null && !stack.getType().isAir()) {
                    giveOrDrop(player, stack);
                    inv.setItem(slot, null);
                }
            }
        } else {
            ItemStack[] offer = session.getOffer(player.getUniqueId());
            for (ItemStack stack : offer) {
                if (stack != null && !stack.getType().isAir()) {
                    giveOrDrop(player, stack.clone());
                }
            }
        }
        ItemStack[] offer = session.getOffer(player.getUniqueId());
        for (int i = 0; i < offer.length; i++) offer[i] = null;
    }

    public static boolean isSelfOfferSlot(int rawSlot) {
        for (int s : SLOTS_SELF) if (s == rawSlot) return true;
        return false;
    }

    public static boolean isPartnerOfferSlot(int rawSlot) {
        for (int s : SLOTS_PARTNER) if (s == rawSlot) return true;
        return false;
    }

    private void buildStatic(Inventory inv, Player player, TradeSession session, LangManager lang) {
        fillGlass(inv);
        Player partner = Bukkit.getPlayer(session.getPartner(player.getUniqueId()));
        String partnerName = partner != null ? partner.getName() : "?";

        inv.setItem(4, labeledItem(Material.PLAYER_HEAD, lang.getRaw("trade.gui-you"),
            List.of(lang.getRaw("trade.gui-your-offer"))));
        inv.setItem(13, partnerHead(partner, lang.getRaw("trade.gui-partner", "player", partnerName),
            List.of(lang.getRaw("trade.gui-partner-offer"))));

        inv.setItem(SLOT_MONEY_MINUS_10, labeledItem(Material.RED_CONCRETE, lang.getRaw("trade.gui-money-minus-10"), List.of()));
        inv.setItem(SLOT_MONEY_MINUS_1, labeledItem(Material.RED_WOOL, lang.getRaw("trade.gui-money-minus-1"), List.of()));
        inv.setItem(SLOT_MONEY_PLUS_1, labeledItem(Material.LIME_WOOL, lang.getRaw("trade.gui-money-plus-1"), List.of()));
        inv.setItem(SLOT_MONEY_PLUS_10, labeledItem(Material.LIME_CONCRETE, lang.getRaw("trade.gui-money-plus-10"), List.of()));
        inv.setItem(SLOT_CANCEL, labeledItem(Material.BARRIER, lang.getRaw("trade.gui-cancel"), List.of()));
        inv.setItem(SLOT_CONFIRM, labeledItem(Material.LIME_STAINED_GLASS_PANE, lang.getRaw("trade.gui-confirm"), List.of()));
    }

    private void syncPartnerItems(Inventory inv, Player player, TradeSession session) {
        ItemStack[] partnerOffer = session.getPartnerOffer(player.getUniqueId());
        for (int i = 0; i < SLOTS_PARTNER.length; i++) {
            ItemStack stack = partnerOffer[i];
            inv.setItem(SLOTS_PARTNER[i], stack != null ? stack.clone() : null);
        }
    }

    private void syncSelfItemsFromSession(Inventory inv, Player player, TradeSession session) {
        ItemStack[] offer = session.getOffer(player.getUniqueId());
        for (int i = 0; i < SLOTS_SELF.length; i++) {
            ItemStack current = inv.getItem(SLOTS_SELF[i]);
            if (current == null || current.getType().isAir()) {
                ItemStack fromSession = offer[i];
                inv.setItem(SLOTS_SELF[i], fromSession != null ? fromSession.clone() : null);
            }
        }
    }

    private void updateMoneyAndStatus(Inventory inv, Player player, TradeSession session, LangManager lang) {
        double myMoney = session.getMoney(player.getUniqueId());
        double partnerMoney = session.getMoney(session.getPartner(player.getUniqueId()));
        var eco = plugin.getCurrencyManager();

        inv.setItem(SLOT_MONEY_DISPLAY, labeledItem(Material.GOLD_INGOT,
            lang.getRaw("trade.gui-your-money", "amount", eco.format(myMoney)),
            List.of(lang.getRaw("trade.gui-money-hint"))));

        inv.setItem(SLOT_PARTNER_MONEY, labeledItem(Material.GOLD_NUGGET,
            lang.getRaw("trade.gui-partner-money", "amount", eco.format(partnerMoney)),
            List.of()));

        boolean partnerReady = session.isReady(session.getPartner(player.getUniqueId()));
        boolean selfReady = session.isReady(player.getUniqueId());

        Material partnerMat = partnerReady ? Material.LIME_DYE : Material.GRAY_DYE;
        Material selfMat = selfReady ? Material.LIME_DYE : Material.GRAY_DYE;

        inv.setItem(SLOT_PARTNER_STATUS, labeledItem(partnerMat,
            lang.getRaw(partnerReady ? "trade.gui-partner-ready" : "trade.gui-partner-not-ready"), List.of()));

        inv.setItem(SLOT_CONFIRM, labeledItem(selfReady ? Material.LIME_STAINED_GLASS : Material.YELLOW_STAINED_GLASS,
            lang.getRaw(selfReady ? "trade.gui-you-ready" : "trade.gui-confirm"),
            List.of(lang.getRaw("trade.gui-confirm-hint"))));
    }

    private void fillGlass(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.empty());
        glass.setItemMeta(meta);
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType().isAir()) {
                inv.setItem(i, glass);
            }
        }
        for (int s : SLOTS_SELF) inv.setItem(s, null);
        for (int s : SLOTS_PARTNER) inv.setItem(s, null);
    }

    private ItemStack labeledItem(Material mat, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(colorize(name));
        if (!loreLines.isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) lore.add(colorize(line));
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack partnerHead(Player partner, String name, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (partner != null) meta.setOwningPlayer(partner);
        meta.displayName(colorize(name));
        List<Component> loreComp = new ArrayList<>();
        for (String line : lore) loreComp.add(colorize(line));
        meta.lore(loreComp);
        head.setItemMeta(meta);
        return head;
    }

    private void giveOrDrop(Player player, ItemStack stack) {
        var leftover = player.getInventory().addItem(stack);
        leftover.values().forEach(s -> player.getWorld().dropItemNaturally(player.getLocation(), s));
    }

    private Component colorize(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
