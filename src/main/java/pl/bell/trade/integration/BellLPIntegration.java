package pl.bell.trade.integration;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import pl.bell.trade.BellTrade;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/** Registers BellTrade with BellLP group sync hub. */
public final class BellLPIntegration {

    private static boolean registered;

    private BellLPIntegration() {}

    public static void tryRegister(BellTrade plugin) {
        if (registered) return;

        Plugin bellLP = Bukkit.getPluginManager().getPlugin("BellLP");
        if (bellLP == null || !bellLP.isEnabled()) return;

        try {
            ClassLoader loader = bellLP.getClass().getClassLoader();
            Class<?> apiClass = Class.forName("pl.bell.belllp.api.BellLPAPI", true, loader);
            Class<?> handlerClass = Class.forName("pl.bell.belllp.api.GroupSyncHandler", true, loader);
            Object api = apiClass.getMethod("get").invoke(null);

            Object handler = Proxy.newProxyInstance(loader, new Class<?>[]{handlerClass},
                    new TradeHandler(plugin));

            apiClass.getMethod("registerSyncHandler", handlerClass).invoke(api, handler);
            registered = true;
            plugin.getLogger().info("BellLP ecosystem sync registered.");
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().warning("BellLP hook unavailable: " + ex.getMessage());
        }
    }

    private static final class TradeHandler implements InvocationHandler {
        private final BellTrade plugin;

        private TradeHandler(BellTrade plugin) {
            this.plugin = plugin;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "onGroupSynced", "onAllGroupsSynced" -> plugin.reloadConfigs();
                case "refreshPlayer" -> {
                    // Market listing limit is resolved from LP permissions on every
                    // call to ListingManager.getMaxListings(), so no cache to invalidate.
                }
                default -> { }
            }
            return null;
        }
    }
}
