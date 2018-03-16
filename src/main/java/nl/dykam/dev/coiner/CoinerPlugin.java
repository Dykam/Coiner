package nl.dykam.dev.coiner;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.apache.commons.lang.NotImplementedException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Logger;

public class CoinerPlugin extends JavaPlugin implements Listener {
    private static final Logger log = Logger.getLogger("Minecraft");
    public static final String COINER_PLUGIN_INVENTORY = "CoinerPluginInventory";
    public static final String EXCHANGE = "Drop currency to exchange";
    private static Plugin vault;
    private static Currency<Material> currency;

    @Override
    public void onEnable() {
        vault = getServer().getPluginManager().getPlugin("Vault");
        if (vault == null) {
            log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getServicesManager().register(Economy.class, new CoinerEconomy(), getServer().getPluginManager().getPlugin("Vault"), ServicePriority.Highest);

        currency = new Currency<Material>()
                .registerDenomination(Material.GOLD_BLOCK, 81, "Gold block", "Gold blocks")
                .registerDenomination(Material.GOLD_INGOT, 9, "Gold ingot", "Gold ingots")
                .registerDenomination(Material.GOLD_NUGGET, 1, "Gold nugget", "Gold nuggets");

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(label.equals("coiner")) {
            if (args.length == 1) {
                long amount = Long.parseLong(args[0]);
                sender.sendMessage(CoinMath.denominate(currency, amount).toString());
                return true;
            }
            if (args.length == 0 && sender instanceof Player) {
                sender.sendMessage(getWallet((Player) sender).toString());
                return true;
            }
            return false;
        }
        return false;
    }

    @EventHandler
    void onPlayerInteract(PlayerInteractEvent event) {
        if(event.getHand() != EquipmentSlot.HAND)
            return;

        ItemStack item = event.getItem();
        if(!currency.validDenimination(item.getType()))
            return;

        Inventory inventory = Bukkit.createInventory(event.getPlayer(), 54, EXCHANGE);
        Wallet<Material> exchangeWallet = currency.createWallet();
        updateInventory(exchangeWallet, inventory);
        event.getPlayer().openInventory(inventory);
        event.getPlayer().setMetadata(COINER_PLUGIN_INVENTORY, new FixedMetadataValue(this, exchangeWallet));
    }

    private void updateInventory(Wallet<Material> wallet, Inventory inventory) {
        long value = wallet.getValue();
        int index = 0;
        for (Denomination<Material> denomination : currency.descending()) {
            ItemStack itemStack = new ItemStack(denomination.getKey(), 1);
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.setDisplayName(ChatColor.RESET + denomination.getPluralName() + " x " + value / denomination.getValue());
            itemStack.setItemMeta(itemMeta);
            inventory.setItem(index, itemStack);

            if(index++ >= 8)
                break;
        }
    }

    @EventHandler
    void onInventory(InventoryClickEvent event) {
        if(event.getSlotType() != InventoryType.SlotType.CONTAINER)
            return;

        Inventory top = event.getView().getTopInventory();
        if(!top.getName().equals(EXCHANGE))
            return;

        Inventory clickedInventory = event.getClickedInventory();
        if (!clickedInventory.getName().equals(EXCHANGE)) {
            // clicked bottom inventory
            if(event.getAction() == InventoryAction.COLLECT_TO_CURSOR || event.getAction() == InventoryAction.UNKNOWN) {
                event.setCancelled(true);
            }
            if(event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
                int slot = event.getSlot();
                ItemStack item = clickedInventory.getItem(slot);
                getServer().getScheduler().runTask(this, () -> clickedInventory.setItem(slot, null));

                Player player = (Player) event.getWhoClicked();
                @SuppressWarnings("unchecked")
                Wallet<Material> wallet = (Wallet<Material>) player.getMetadata(COINER_PLUGIN_INVENTORY).get(0).value();
                wallet.add(item.getType(), item.getAmount());
                updateInventory(wallet, top);
            }

            return;
        }

        Set<InventoryAction> acceptList  = new HashSet<>();
        acceptList.add(InventoryAction.PLACE_ALL);
        acceptList.add(InventoryAction.PLACE_ONE);
        acceptList.add(InventoryAction.PLACE_SOME);

        if(acceptList.contains(event.getAction())) {
            Player player = (Player) event.getWhoClicked();
            @SuppressWarnings("unchecked")
            Wallet<Material> wallet = (Wallet<Material>) player.getMetadata(COINER_PLUGIN_INVENTORY).get(0).value();

            ItemStack item = event.getCursor();
            if(!currency.validDenimination(item.getType())) {
                event.setCancelled(true);
                return;
            }

            event.setCurrentItem(null);
            int slot = event.getSlot();
            getServer().getScheduler().runTask(this, () -> clickedInventory.setItem(slot, null));
            wallet.add(item.getType(), item.getAmount());
            updateInventory(wallet, clickedInventory);

            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    void onInventory(InventoryCloseEvent event) {
    }

    @EventHandler
    void onInventory(InventoryCreativeEvent event) {

    }

    @EventHandler
    void onInventory(InventoryDragEvent event) {
        Inventory clickedInventory = event.getInventory();
        if (!clickedInventory.getName().equals(EXCHANGE)) {
            return;
        }

        for (Integer slot : event.getRawSlots()) {
            if(slot < clickedInventory.getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    void onInventory(InventoryInteractEvent event) {

    }

    @EventHandler
    void onInventory(InventoryOpenEvent event) {

    }

    @EventHandler
    void onInventory(InventoryMoveItemEvent event) {
    }

    @EventHandler
    void onInventory(InventoryPickupItemEvent event) {
    }

    class CoinerEconomy implements Economy {
        public boolean isEnabled() {
            return true;
        }

        public String getName() {
            return "Coiner";
        }

        public boolean hasBankSupport() {
            return true;
        }

        public int fractionalDigits() {
            return 0;
        }

        public String format(double v) {
             return "" + (long)v;
        }

        public String currencyNamePlural() {
            return "Coins";
        }

        public String currencyNameSingular() {
            return "Coin";
        }

        public boolean hasAccount(String s) {
            @SuppressWarnings("deprecation")
            OfflinePlayer player = getServer().getOfflinePlayer(s);
            return hasAccount(player);
        }

        public boolean hasAccount(OfflinePlayer offlinePlayer) {
            return true;
        }

        public boolean hasAccount(String s, String s1) {
            @SuppressWarnings("deprecation")
            OfflinePlayer player = getServer().getOfflinePlayer(s);
            return hasAccount(player, s1);
        }

        public boolean hasAccount(OfflinePlayer offlinePlayer, String worldName) {
            return hasAccount(offlinePlayer);
        }

        public double getBalance(String s) {
            @SuppressWarnings("deprecation")
            OfflinePlayer player = getServer().getOfflinePlayer(s);
            return getBalance(player);
        }

        public double getBalance(OfflinePlayer offlinePlayer) {
            if(!offlinePlayer.isOnline()) {
                return 0;
            }

            Player player = (Player)offlinePlayer;

            Wallet<Material> wallet = getWallet(player);
            return wallet.getValue();
        }

        public double getBalance(String s, String s1) {
            @SuppressWarnings("deprecation")
            OfflinePlayer player = getServer().getOfflinePlayer(s);
            return getBalance(player, s1);
        }

        public double getBalance(OfflinePlayer offlinePlayer, String world) {
            return getBalance(offlinePlayer);
        }

        public boolean has(String s, double v) {
            @SuppressWarnings("deprecation")
            OfflinePlayer player = getServer().getOfflinePlayer(s);
            return has(player, v);
        }

        public boolean has(OfflinePlayer player, double amount) {
            return getBalance(player) >= amount;
        }

        public boolean has(String s, String s1, double v) {
            @SuppressWarnings("deprecation")
            OfflinePlayer player = getServer().getOfflinePlayer(s);
            return has(player, s1, v);
        }

        public boolean has(OfflinePlayer offlinePlayer, String world, double amount) {
            return has(offlinePlayer, amount);
        }

        public EconomyResponse withdrawPlayer(String s, double v) {
            @SuppressWarnings("deprecation")
            OfflinePlayer player = getServer().getOfflinePlayer(s);
            return withdrawPlayer(player, v);
        }

        public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, double amount) {
            if(!offlinePlayer.isOnline())
                return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Player offline");

            Player player = (Player)offlinePlayer;

            Wallet<Material> wallet = getWallet(player);

            long balance = wallet.getValue();
            if(balance < amount)
                return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, "Not enough coins");

            Wallet<Material> changes;
            try {
                changes = CoinMath.subtract(wallet, (long) amount);
            } catch (Exception ex) {
                getLogger().severe(ex.getMessage());
                return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, "Not enough coins");
            }

            applyWallet(player, changes);

            return new EconomyResponse(amount, getBalance(offlinePlayer), EconomyResponse.ResponseType.SUCCESS, "");
        }

        private void applyWallet(Player player, Wallet<Material> changes) {
            for (Map.Entry<Material, Long> change : changes.getContents().entrySet()) {
                if(change.getValue() < 0) {
                    player.getInventory().removeItem(new ItemStack(change.getKey(), (int) -change.getValue()));
                } else {
                    player.getInventory().addItem(new ItemStack(change.getKey(), (int) (long)change.getValue()));
                }
            }
        }

        public EconomyResponse withdrawPlayer(String s, String s1, double v) {
            @SuppressWarnings("deprecation")
            OfflinePlayer player = getServer().getOfflinePlayer(s);
            return withdrawPlayer(player, s1, v);
        }

        public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, String s, double v) {
            return withdrawPlayer(offlinePlayer, v);
        }

        public EconomyResponse depositPlayer(String s, double v) {
            @SuppressWarnings("deprecation")
            OfflinePlayer player = getServer().getOfflinePlayer(s);
            return depositPlayer(player, v);
        }

        public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, double amount) {
            if(!offlinePlayer.isOnline())
                return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Player offline");

            Player player = (Player)offlinePlayer;

            applyWallet(player, CoinMath.denominate(currency, (long) amount));

            return new EconomyResponse(amount, getBalance(offlinePlayer), EconomyResponse.ResponseType.SUCCESS, "");
        }

        public EconomyResponse depositPlayer(String s, String s1, double v) {
            @SuppressWarnings("deprecation")
            OfflinePlayer player = getServer().getOfflinePlayer(s);
            return depositPlayer(player, s1, v);
        }

        public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, String s, double v) {
            return depositPlayer(offlinePlayer, v);
        }

        public EconomyResponse createBank(String s, String s1) {
            throw new NotImplementedException();
        }

        public EconomyResponse createBank(String s, OfflinePlayer offlinePlayer) {
            return null;
        }

        public EconomyResponse deleteBank(String s) {
            return null;
        }

        public EconomyResponse bankBalance(String s) {
            return null;
        }

        public EconomyResponse bankHas(String s, double v) {
            return null;
        }

        public EconomyResponse bankWithdraw(String s, double v) {
            return null;
        }

        public EconomyResponse bankDeposit(String s, double v) {
            return null;
        }

        public EconomyResponse isBankOwner(String s, String s1) {
            @SuppressWarnings("deprecation")
            OfflinePlayer player = getServer().getOfflinePlayer(s);
            return isBankOwner(s, player);
        }

        public EconomyResponse isBankOwner(String s, OfflinePlayer offlinePlayer) {
            return null;
        }

        public EconomyResponse isBankMember(String s, String s1) {
            @SuppressWarnings("deprecation")
            OfflinePlayer player = getServer().getOfflinePlayer(s);
            return isBankMember(s, player);
        }

        public EconomyResponse isBankMember(String s, OfflinePlayer offlinePlayer) {
            return null;
        }

        public List<String> getBanks() {
            return null;
        }

        public boolean createPlayerAccount(String s) {
            @SuppressWarnings("deprecation")
            OfflinePlayer player = getServer().getOfflinePlayer(s);
            return createPlayerAccount(player);
        }

        public boolean createPlayerAccount(OfflinePlayer offlinePlayer) {
            return false;
        }

        public boolean createPlayerAccount(String s, String s1) {
            @SuppressWarnings("deprecation")
            OfflinePlayer player = getServer().getOfflinePlayer(s);
            return createPlayerAccount(player, s1);
        }

        public boolean createPlayerAccount(OfflinePlayer offlinePlayer, String world) {
            return false;
        }

        // private int addToExistingStacks(Inventory inventory, Material material, int amount) {
        //     HashMap<Integer, ? extends ItemStack> stacks = inventory.all(material);
        //     for (Map.Entry<Integer, ? extends ItemStack> stackEntry : stacks.entrySet()) {
        //         ItemStack stack = stackEntry.getValue();
        //         int space = stack.getMaxStackSize() - stack.getAmount();
        //         int amountToAdd = Math.max(space - amount, 0);
        //         amount -= amountToAdd;
        //         stack.setAmount(stack.getAmount() + amountToAdd);
        //         inventory.setItem(stackEntry.getKey(), stack);
        //     }
        //     return amount;
        // }
    }

    private Wallet<Material> getWallet(Player player) {
        Wallet<Material> wallet = currency.createWallet();

        for (ItemStack itemStack : player.getInventory().getContents()) {
            if(itemStack != null && currency.validDenimination(itemStack.getType())) {
                wallet.add(itemStack.getType(), itemStack.getAmount());
            }
        }
        return wallet;
    }
}
