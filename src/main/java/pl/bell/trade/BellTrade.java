package pl.bell.trade;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import pl.bell.trade.api.BellTradeAPI;
import pl.bell.trade.api.TransactionGuard;
import pl.bell.trade.command.BellTradeCommand;
import pl.bell.trade.command.EconomyCommand;
import pl.bell.trade.command.MarketCommand;
import pl.bell.trade.command.SellShopCommand;
import pl.bell.trade.command.TradeCommand;
import pl.bell.trade.config.CurrencyConfig;
import pl.bell.trade.config.LangManager;
import pl.bell.trade.config.ShopConfigManager;
import pl.bell.trade.economy.CurrencyManager;
import pl.bell.trade.engine.EconomyHealthMonitor;
import pl.bell.trade.engine.DemandTracker;
import pl.bell.trade.engine.PriceEngine;
import pl.bell.trade.engine.PriceHistoryRepository;
import pl.bell.trade.engine.RarityScorer;
import pl.bell.trade.engine.SupplySampleRepository;
import pl.bell.trade.engine.SupplyTracker;
import pl.bell.trade.gui.EconomyHealthGUI;
import pl.bell.trade.gui.EconomyHealthGuiListener;
import pl.bell.trade.gui.MainMenuGUI;
import pl.bell.trade.gui.MainMenuGuiListener;
import pl.bell.trade.gui.ShopPriceEditFlow;
import pl.bell.trade.gui.ShopPriceEditorGUI;
import pl.bell.trade.gui.ShopPriceEditorGuiListener;
import pl.bell.trade.gui.AdminGUI;
import pl.bell.trade.gui.MarketGUI;
import pl.bell.trade.gui.MarketGuiListener;
import pl.bell.trade.gui.SellShopGUI;
import pl.bell.trade.gui.SellShopGuiListener;
import pl.bell.trade.gui.TradeGUI;
import pl.bell.trade.gui.TradeGuiListener;
import pl.bell.trade.integration.BellLandsHook;
import pl.bell.trade.integration.PlaceholderHook;
import pl.bell.trade.listener.MarketChatListener;
import pl.bell.trade.listener.PlayerQuitListener;
import pl.bell.trade.market.ListingManager;
import pl.bell.trade.market.ListingRepository;
import pl.bell.trade.market.MarketSellFlow;
import pl.bell.trade.shop.ShopManager;
import pl.bell.trade.storage.Database;
import pl.bell.trade.trade.TradeManager;

public final class BellTrade extends JavaPlugin {

    private static BellTrade instance;

    private Database database;
    private LangManager langManager;
    private CurrencyConfig currencyConfig;
    private CurrencyManager currencyManager;
    private ShopConfigManager shopConfigManager;
    private AdminGUI adminGUI;
    private TradeGUI tradeGUI;
    private TradeManager tradeManager;
    private TransactionGuard transactionGuard;
    private ListingRepository listingRepository;
    private ListingManager listingManager;
    private MarketGUI marketGUI;
    private MarketSellFlow marketSellFlow;
    private PriceHistoryRepository priceHistoryRepository;
    private SupplySampleRepository supplySampleRepository;
    private SupplyTracker supplyTracker;
    private DemandTracker demandTracker;
    private RarityScorer rarityScorer;
    private PriceEngine priceEngine;
    private ShopManager shopManager;
    private SellShopGUI sellShopGUI;
    private EconomyHealthMonitor economyHealthMonitor;
    private EconomyHealthGUI economyHealthGUI;
    private ShopPriceEditorGUI shopPriceEditorGUI;
    private ShopPriceEditFlow shopPriceEditFlow;
    private MainMenuGUI mainMenuGUI;
    private BellLandsHook bellLandsHook;
    private PlaceholderHook placeholderHook;

    @Override
    public void onEnable() {
        instance = this;
        printBanner();

        saveDefaultConfig();

        try {
            database = new Database();
            database.init(new java.io.File(getDataFolder(), "data.db"));
        } catch (Exception e) {
            getLogger().severe("Failed to init database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.langManager = new LangManager(this);
        this.currencyConfig = new CurrencyConfig(this);
        this.currencyManager = new CurrencyManager(this, database);
        this.shopConfigManager = new ShopConfigManager(this);
        this.shopConfigManager.reload();
        this.adminGUI = new AdminGUI(this);
        this.tradeGUI = new TradeGUI(this);
        this.tradeManager = new TradeManager(this, tradeGUI);
        this.transactionGuard = new TransactionGuard();
        this.listingRepository = new ListingRepository(database, getLogger());
        this.listingManager = new ListingManager(this, listingRepository, transactionGuard);
        this.marketGUI = new MarketGUI(this);
        this.marketSellFlow = new MarketSellFlow(this);

        this.priceHistoryRepository = new PriceHistoryRepository(database, getLogger());
        this.supplySampleRepository = new SupplySampleRepository(database, getLogger());
        this.supplyTracker = new SupplyTracker(this, supplySampleRepository);
        this.demandTracker = new DemandTracker(this, priceHistoryRepository, listingRepository);
        this.rarityScorer = new RarityScorer();
        this.priceEngine = new PriceEngine(this, shopConfigManager, supplyTracker, demandTracker, rarityScorer, priceHistoryRepository);
        this.shopManager = new ShopManager(this);
        this.sellShopGUI = new SellShopGUI(this);
        this.economyHealthMonitor = new EconomyHealthMonitor(this);
        this.economyHealthGUI = new EconomyHealthGUI(this);
        this.shopPriceEditorGUI = new ShopPriceEditorGUI(this);
        this.shopPriceEditFlow = new ShopPriceEditFlow(this);
        this.mainMenuGUI = new MainMenuGUI(this);

        supplyTracker.start();
        demandTracker.reload();
        economyHealthMonitor.start();

        this.bellLandsHook = new BellLandsHook(this);
        this.placeholderHook = new PlaceholderHook(this);
        placeholderHook.register();

        BellTradeAPI.init(this);

        BellTradeCommand btCmd = new BellTradeCommand(this);
        PluginCommand belltrade = getCommand("belltrade");
        if (belltrade != null) {
            belltrade.setExecutor(btCmd);
            belltrade.setTabCompleter(btCmd);
        }

        TradeCommand tradeCmd = new TradeCommand(this);
        PluginCommand trade = getCommand("trade");
        if (trade != null) {
            trade.setExecutor(tradeCmd);
            trade.setTabCompleter(tradeCmd);
        }

        MarketCommand marketCmd = new MarketCommand(this);
        PluginCommand market = getCommand("market");
        if (market != null) {
            market.setExecutor(marketCmd);
            market.setTabCompleter(marketCmd);
        }

        SellShopCommand sellShopCmd = new SellShopCommand(this);
        PluginCommand sellshop = getCommand("sellshop");
        if (sellshop != null) {
            sellshop.setExecutor(sellShopCmd);
            sellshop.setTabCompleter(sellShopCmd);
        }

        if (getConfig().getBoolean("commands.register-economy", true)) {
            EconomyCommand ecoCmd = new EconomyCommand(this);
            registerEcoCommand("balance", ecoCmd);
            registerEcoCommand("pay", ecoCmd);
            registerEcoCommand("baltop", ecoCmd);
        }

        getServer().getPluginManager().registerEvents(new MarketChatListener(this, adminGUI), this);
        getServer().getPluginManager().registerEvents(new TradeGuiListener(this, tradeManager), this);
        getServer().getPluginManager().registerEvents(new MarketGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new SellShopGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopPriceEditorGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new EconomyHealthGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new MainMenuGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this, tradeManager), this);

        getLogger().info("BellTrade enabled.");
    }

    @Override
    public void onDisable() {
        if (placeholderHook != null) {
            placeholderHook.unregister();
        }
        if (economyHealthMonitor != null) {
            economyHealthMonitor.shutdown();
        }
        if (supplyTracker != null) {
            supplyTracker.shutdown();
        }
        if (listingManager != null) {
            listingManager.shutdown();
        }
        if (tradeManager != null) {
            tradeManager.shutdown();
        }
        if (currencyManager != null) {
            currencyManager.flush();
        }
        if (database != null) {
            database.shutdown();
        }
        getLogger().info("BellTrade disabled.");
    }

    public void reload() {
        reloadConfig();
        if (currencyConfig != null) currencyConfig.reload();
        if (langManager != null) langManager.reload();
        if (currencyManager != null) currencyManager.reload();
        if (shopConfigManager != null) shopConfigManager.reload();
        if (supplyTracker != null) supplyTracker.reload();
        if (demandTracker != null) demandTracker.reload();
        if (economyHealthMonitor != null) economyHealthMonitor.reload();
        getServer().getPluginManager().callEvent(new pl.bell.trade.event.BellTradeReloadEvent());
    }

    private void registerEcoCommand(String name, EconomyCommand executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }
    }

    public static BellTrade getInstance() {
        return instance;
    }

    public Database getDatabase() {
        return database;
    }

    public LangManager getLangManager() {
        return langManager;
    }

    public CurrencyConfig getCurrencyConfig() {
        return currencyConfig;
    }

    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }

    public ShopConfigManager getShopConfigManager() {
        return shopConfigManager;
    }

    public AdminGUI getAdminGUI() {
        return adminGUI;
    }

    public TradeGUI getTradeGUI() {
        return tradeGUI;
    }

    public TradeManager getTradeManager() {
        return tradeManager;
    }

    public ListingManager getListingManager() {
        return listingManager;
    }

    public MarketGUI getMarketGUI() {
        return marketGUI;
    }

    public MarketSellFlow getMarketSellFlow() {
        return marketSellFlow;
    }

    public PriceEngine getPriceEngine() {
        return priceEngine;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public SellShopGUI getSellShopGUI() {
        return sellShopGUI;
    }

    public EconomyHealthMonitor getEconomyHealthMonitor() {
        return economyHealthMonitor;
    }

    public EconomyHealthGUI getEconomyHealthGUI() {
        return economyHealthGUI;
    }

    public ShopPriceEditorGUI getShopPriceEditorGUI() {
        return shopPriceEditorGUI;
    }

    public ShopPriceEditFlow getShopPriceEditFlow() {
        return shopPriceEditFlow;
    }

    public MainMenuGUI getMainMenuGUI() {
        return mainMenuGUI;
    }

    public BellLandsHook getBellLandsHook() {
        return bellLandsHook;
    }

    public TransactionGuard getTransactionGuard() {
        return transactionGuard;
    }

    private void printBanner() {
        var c = org.bukkit.Bukkit.getConsoleSender();
        c.sendMessage("§r");
        c.sendMessage("§6  ██████╗ ███████╗██╗     ██╗          ");
        c.sendMessage("§6  ██╔══██╗██╔════╝██║     ██║          ");
        c.sendMessage("§6  ██████╔╝█████╗  ██║     ██║          ");
        c.sendMessage("§6  ██╔══██╗██╔══╝  ██║     ██║          ");
        c.sendMessage("§6  ██████╔╝███████╗███████╗███████╗§r§f Trade");
        c.sendMessage("§6  ╚═════╝ ╚══════╝╚══════╝╚══════╝     ");
        c.sendMessage("§r");
        c.sendMessage("§7  Version §f" + getDescription().getVersion() + "  §7│  Author §bBellzeb");
        c.sendMessage("§7  Status  §aFree §7│ §7Pro §5Coming Soon");
        c.sendMessage("§r");
    }
}
