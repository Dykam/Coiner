package nl.dykam.dev.coiner
import com.deanveloper.kbukkit.KotlinPlugin
import com.deanveloper.kbukkit.chat.plus
import com.google.common.collect.BiMap
import com.google.common.collect.ImmutableBiMap
import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.economy.EconomyResponse
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.*
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.plugin.ServicePriority
import java.util.logging.Logger
import kotlin.math.absoluteValue

@Suppress("unused", "UNUSED_PARAMETER")
class CoinerPlugin: KotlinPlugin(), Listener {
    private lateinit var currency:Currency
    private lateinit var denominationMap: BiMap<Material, Denomination>
    override fun onEnable() {
        val vault = server.pluginManager.getPlugin("Vault")
        if (vault == null)
        {
            log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", description.name))
            server.pluginManager.disablePlugin(this)
            return
        }
        Bukkit.getServicesManager().register(Economy::class.java, CoinerEconomy(), vault, ServicePriority.Highest)
        denominationMap = ImmutableBiMap.copyOf(hashMapOf(
                Material.GOLD_BLOCK  to Denomination(81, "Gold block", "Gold blocks"),
                Material.GOLD_INGOT  to Denomination(9, "Gold ingot", "Gold ingots"),
                Material.GOLD_NUGGET to Denomination(1, "Gold nugget", "Gold nuggets")
        ))
        currency = Currency(denominationMap.values)

//        val coinerSession = CoinerSession(currency);
        server.pluginManager.registerEvents(this, this)
    }
    override fun onCommand(sender:CommandSender, command:Command, label:String, args:Array<String>):Boolean {
        if (label == "coiner")
        {
            if (args.size == 1)
            {
                val amount = java.lang.Long.parseLong(args[0])
                sender.sendMessage(CoinMath.denominate(currency, amount).toString())
                return true
            }
            if (args.isEmpty() && sender is Player)
            {
                sender.sendMessage(getWallet(sender).toString())
                return true
            }
            return false
        }
        return false
    }
    @EventHandler
    internal fun onPlayerInteract(event:PlayerInteractEvent) {
        if (event.hand !== EquipmentSlot.HAND)
            return

        val item = event.item
        if (item.type !in denominationMap)
            return

        val inventory = Bukkit.createInventory(event.player, 54, EXCHANGE)
        val exchangeWallet = currency.createWallet()
        updateInventory(exchangeWallet, inventory)
        event.player.openInventory(inventory)
        event.player.setMetadata(COINER_PLUGIN_INVENTORY, FixedMetadataValue(this, exchangeWallet))
    }
    private fun updateInventory(wallet:Wallet, inventory:Inventory) {
        val value = wallet.value
        for ((index, denomination) in currency.descending.take(9).withIndex())
        {
            val itemStack = ItemStack(denominationMap.inverse()[denomination], 1)
            val itemMeta = itemStack.itemMeta
            itemMeta.displayName = ChatColor.RESET + denomination.pluralName + " x " + value / denomination.value
            itemStack.itemMeta = itemMeta
            inventory.setItem(index, itemStack)
        }
    }
    @EventHandler
    internal fun onInventory(event:InventoryClickEvent) {
        if (event.slotType !== InventoryType.SlotType.CONTAINER)
            return
        val top = event.view.topInventory
        if (top.name != EXCHANGE)
            return
        val clickedInventory = event.clickedInventory
        if (clickedInventory.name != EXCHANGE)
        {
            // clicked bottom inventory
            if (event.action === InventoryAction.COLLECT_TO_CURSOR || event.action === InventoryAction.UNKNOWN)
            {
                event.isCancelled = true
            }
            if (event.action === InventoryAction.MOVE_TO_OTHER_INVENTORY)
            {
                event.isCancelled = true
                val slot = event.slot
                val item = clickedInventory.getItem(slot)
                server.scheduler.runTask(this, { clickedInventory.setItem(slot, null) })
                val player = event.whoClicked as Player
                @Suppress("UNCHECKED_CAST")
                val wallet = player.getMetadata(COINER_PLUGIN_INVENTORY)[0].value() as Wallet
                wallet[denominationMap[item.type]!!] += item.amount.toLong()
                updateInventory(wallet, top)
            }
            return
        }
        val acceptList = mutableSetOf(
                InventoryAction.PLACE_ALL,
                InventoryAction.PLACE_ONE,
                InventoryAction.PLACE_SOME
        )
        if (acceptList.contains(event.action))
        {
            val player = event.whoClicked as Player
            @Suppress("UNCHECKED_CAST")
            val wallet = player.getMetadata(COINER_PLUGIN_INVENTORY)[0].value() as Wallet
            val item = event.cursor
            if (item.type !in denominationMap)
            {
                event.isCancelled = true
                return
            }
            event.currentItem = null
            val slot = event.slot
            server.scheduler.runTask(this, { clickedInventory.setItem(slot, null) })
            wallet[denominationMap[item.type]!!] += item.amount.toLong()
            updateInventory(wallet, clickedInventory)
            return
        }
        event.isCancelled = true
    }
    @EventHandler
    internal fun onInventory(event:InventoryCloseEvent) {}
    @EventHandler
    internal fun onInventory(event:InventoryCreativeEvent) {
    }
    @EventHandler
    internal fun onInventory(event:InventoryDragEvent) {
        val clickedInventory = event.inventory
        if (clickedInventory.name != EXCHANGE)
        {
            return
        }
        for (slot in event.rawSlots)
        {
            if (slot < clickedInventory.size)
            {
                event.isCancelled = true
                return
            }
        }
    }
    @EventHandler
    internal fun onInventory(event:InventoryInteractEvent) {
    }
    @EventHandler
    internal fun onInventory(event:InventoryOpenEvent) {
    }
    @EventHandler
    internal fun onInventory(event:InventoryMoveItemEvent) {}
    @EventHandler
    internal fun onInventory(event:InventoryPickupItemEvent) {}

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    internal inner class CoinerEconomy:Economy {
        override fun isEnabled():Boolean {
            return true
        }
        override fun getName():String {
            return "Coiner"
        }
        override fun getBanks():List<String> {
            return emptyList()
        }
        override fun hasBankSupport():Boolean {
            return true
        }
        override fun fractionalDigits():Int {
            return 0
        }
        override fun format(v:Double):String {
            return "" + v.toLong()
        }
        override fun currencyNamePlural():String {
            return "Coins"
        }
        override fun currencyNameSingular():String {
            return "Coin"
        }
        override fun hasAccount(s:String):Boolean {
            val player = server.getOfflinePlayer(s)
            return hasAccount(player)
        }
        override fun hasAccount(offlinePlayer:OfflinePlayer):Boolean {
            return true
        }
        override fun hasAccount(s:String, s1:String):Boolean {
            val player = server.getOfflinePlayer(s)
            return hasAccount(player, s1)
        }
        override fun hasAccount(offlinePlayer:OfflinePlayer, worldName:String):Boolean {
            return hasAccount(offlinePlayer)
        }
        override fun getBalance(s:String):Double {
            val player = server.getOfflinePlayer(s)
            return getBalance(player)
        }
        override fun getBalance(offlinePlayer:OfflinePlayer):Double {
            if (!offlinePlayer.isOnline)
            {
                return 0.0
            }
            val player = offlinePlayer as Player
            val wallet = getWallet(player)
            return wallet.value.toDouble()
        }
        override fun getBalance(s:String, s1:String):Double {
            val player = server.getOfflinePlayer(s)
            return getBalance(player, s1)
        }
        override fun getBalance(offlinePlayer:OfflinePlayer, world:String):Double {
            return getBalance(offlinePlayer)
        }
        override fun has(s:String, v:Double):Boolean {
            val player = server.getOfflinePlayer(s)
            return has(player, v)
        }
        override fun has(player:OfflinePlayer, amount:Double):Boolean {
            return getBalance(player) >= amount
        }
        override fun has(s:String, s1:String, v:Double):Boolean {
            val player = server.getOfflinePlayer(s)
            return has(player, s1, v)
        }
        override fun has(offlinePlayer:OfflinePlayer, world:String, amount:Double):Boolean {
            return has(offlinePlayer, amount)
        }
        override fun withdrawPlayer(s:String, v:Double):EconomyResponse {
            val player = server.getOfflinePlayer(s)
            return withdrawPlayer(player, v)
        }
        override fun withdrawPlayer(offlinePlayer:OfflinePlayer, amount:Double):EconomyResponse {
            if (!offlinePlayer.isOnline)
                return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Player offline")
            val player = offlinePlayer as Player
            val wallet = getWallet(player)
            val balance = wallet.value
            if (balance < amount)
                return EconomyResponse(0.0, balance.toDouble(), EconomyResponse.ResponseType.FAILURE, "Not enough coins")
            val changes:Wallet
            try
            {
                changes = CoinMath.subtract(wallet, amount.toLong())
            }
            catch (ex:Exception) {
                logger.severe(ex.message)
                return EconomyResponse(0.0, balance.toDouble(), EconomyResponse.ResponseType.FAILURE, "Not enough coins")
            }
            applyWallet(player, changes)
            return EconomyResponse(amount, getBalance(offlinePlayer), EconomyResponse.ResponseType.SUCCESS, "")
        }
        private fun applyWallet(player:Player, changes:Wallet) {
            for (change in changes.contents)
            {
                val itemStack = ItemStack(denominationMap.inverse()[change.key], change.value.toInt().absoluteValue)
                if (change.value < 0)
                    player.inventory.removeItem(itemStack)
                else
                    player.inventory.addItem(itemStack)
            }
        }
        override fun withdrawPlayer(s:String, s1:String, v:Double):EconomyResponse {
            val player = server.getOfflinePlayer(s)
            return withdrawPlayer(player, s1, v)
        }
        override fun withdrawPlayer(offlinePlayer:OfflinePlayer, s:String, v:Double):EconomyResponse {
            return withdrawPlayer(offlinePlayer, v)
        }
        override fun depositPlayer(s:String, v:Double):EconomyResponse {
            val player = server.getOfflinePlayer(s)
            return depositPlayer(player, v)
        }
        override fun depositPlayer(offlinePlayer:OfflinePlayer, amount:Double):EconomyResponse {
            if (!offlinePlayer.isOnline)
                return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Player offline")
            val player = offlinePlayer as Player
            applyWallet(player, CoinMath.denominate(currency, amount.toLong()))
            return EconomyResponse(amount, getBalance(offlinePlayer), EconomyResponse.ResponseType.SUCCESS, "")
        }
        override fun depositPlayer(s:String, s1:String, v:Double):EconomyResponse {
            val player = server.getOfflinePlayer(s)
            return depositPlayer(player, s1, v)
        }
        override fun depositPlayer(offlinePlayer:OfflinePlayer, s:String, v:Double):EconomyResponse {
            return depositPlayer(offlinePlayer, v)
        }
        override fun createBank(s:String, s1:String):EconomyResponse {
            return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "")
        }
        override fun createBank(s:String, offlinePlayer:OfflinePlayer):EconomyResponse {
            return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "")
        }
        override fun deleteBank(s:String):EconomyResponse {
            return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "")
        }
        override fun bankBalance(s:String):EconomyResponse {
            return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "")
        }
        override fun bankHas(s:String, v:Double):EconomyResponse {
            return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "")
        }
        override fun bankWithdraw(s:String, v:Double):EconomyResponse {
            return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "")
        }
        override fun bankDeposit(s:String, v:Double):EconomyResponse {
            return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "")
        }
        override fun isBankOwner(s:String, s1:String):EconomyResponse {
            val player = server.getOfflinePlayer(s)
            return isBankOwner(s, player)
        }
        override fun isBankOwner(s:String, offlinePlayer:OfflinePlayer):EconomyResponse {
            return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "")
        }
        override fun isBankMember(s:String, s1:String):EconomyResponse {
            val player = server.getOfflinePlayer(s)
            return isBankMember(s, player)
        }
        override fun isBankMember(s:String, offlinePlayer:OfflinePlayer):EconomyResponse {
            return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "")
        }
        override fun createPlayerAccount(s:String):Boolean {
            val player = server.getOfflinePlayer(s)
            return createPlayerAccount(player)
        }
        override fun createPlayerAccount(offlinePlayer:OfflinePlayer):Boolean {
            return false
        }
        override fun createPlayerAccount(s:String, s1:String):Boolean {
            val player = server.getOfflinePlayer(s)
            return createPlayerAccount(player, s1)
        }
        override fun createPlayerAccount(offlinePlayer:OfflinePlayer, world:String):Boolean {
            return false
        }
    }
    private fun getWallet(player:Player):Wallet {
        val wallet = currency.createWallet()
        for (itemStack in player.inventory.contents)
        {
            if (itemStack != null && itemStack.type in denominationMap)
            {
                wallet[denominationMap[itemStack.type]!!] += itemStack.amount.toLong()
            }
        }
        return wallet
    }
    companion object {
        private val log = Logger.getLogger("Minecraft")
        const val COINER_PLUGIN_INVENTORY = "CoinerPluginInventory"
        const val EXCHANGE = "Drop currency to exchange"
    }
}