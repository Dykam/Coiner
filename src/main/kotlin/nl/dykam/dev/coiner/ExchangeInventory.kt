package nl.dykam.dev.coiner

import com.deanveloper.kbukkit.chat.plus
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.event.*
import org.bukkit.event.inventory.*
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager
import java.io.Closeable

inline fun <reified T : Event> PluginManager.registerEvent(listener: Listener, plugin: Plugin, priority: EventPriority = EventPriority.NORMAL, noinline executor: (T) -> Unit) {
    this.registerEvents(listener, plugin, priority) { register(executor) }
}

data class RegistrationContext(val pluginManager: PluginManager, val listener: Listener, val plugin: Plugin, val priority: EventPriority = EventPriority.NORMAL) {
    inline fun <reified T: Event> register(noinline executor: T.() -> Unit) {
        pluginManager.registerEvent(
                T::class.java,
                listener,
                EventPriority.NORMAL,
                { _, event -> (event as T).executor() },
                plugin
        )
    }

    inline fun highest(crossinline executor: RegistrationContext.() -> Unit) = copy(priority = EventPriority.HIGHEST).executor()
    inline fun high(crossinline executor: RegistrationContext.() -> Unit) = copy(priority = EventPriority.HIGH).executor()
    inline fun normal(crossinline executor: RegistrationContext.() -> Unit) = copy(priority = EventPriority.NORMAL).executor()
    inline fun low(crossinline executor: RegistrationContext.() -> Unit) = copy(priority = EventPriority.LOW).executor()
    inline fun lowest(crossinline executor: RegistrationContext.() -> Unit) = copy(priority = EventPriority.LOWEST).executor()
    inline fun monitor(crossinline executor: RegistrationContext.() -> Unit) = copy(priority = EventPriority.MONITOR).executor()
}

inline fun PluginManager.registerEvents(listener: Listener, plugin: Plugin, priority: EventPriority = EventPriority.NORMAL, crossinline executor: RegistrationContext.() -> Unit) {
    executor(RegistrationContext(this, listener, plugin, priority))
}

class ExchangeInventory(
        val currency: Currency,
        val mapping: DenominationMapper<Material>,
        val server: Server,
        val plugin: Plugin,
        openOnRightClickItem: Boolean = false
) : Closeable {
    private var listener : Listener = object : Listener {}

    init {
        server.pluginManager.registerEvents(listener, plugin) {
            if(openOnRightClickItem) {
                register<PlayerInteractEvent> { open(player) }
            }

            register<InventoryClickEvent> {
                if (slotType !== InventoryType.SlotType.CONTAINER)
                    return@register
                val top = view.topInventory
                if (top.name != CoinerPlugin.EXCHANGE)
                    return@register
                val clickedInventory = clickedInventory
                if (clickedInventory.name != CoinerPlugin.EXCHANGE)
                {
                    // clicked bottom inventory
                    if (action === InventoryAction.COLLECT_TO_CURSOR || action === InventoryAction.UNKNOWN)
                    {
                        isCancelled = true
                    }
                    if (action === InventoryAction.MOVE_TO_OTHER_INVENTORY)
                    {
                        isCancelled = true
                        val slot = slot
                        val item = clickedInventory.getItem(slot)
                        server.scheduler.runTask(plugin, { clickedInventory.setItem(slot, null) })
                        val player = whoClicked as Player
                        @Suppress("UNCHECKED_CAST")
                        val wallet = player.getMetadata(CoinerPlugin.COINER_PLUGIN_INVENTORY)[0].value() as Wallet
                        wallet[mapping[item.type]] += item.amount.toLong()
                        updateInventory(wallet, top)
                    }
                    return@register
                }
                val acceptList = mutableSetOf(
                        InventoryAction.PLACE_ALL,
                        InventoryAction.PLACE_ONE,
                        InventoryAction.PLACE_SOME
                )
                if (acceptList.contains(action))
                {
                    val player = whoClicked as Player
                    @Suppress("UNCHECKED_CAST")
                    val wallet = player.getMetadata(CoinerPlugin.COINER_PLUGIN_INVENTORY)[0].value() as Wallet
                    val item = cursor
                    if (item.type !in mapping)
                    {
                        isCancelled = true
                        return@register
                    }
                    currentItem = null
                    val slot = slot
                    server.scheduler.runTask(plugin, { clickedInventory.setItem(slot, null) })
                    wallet[mapping[item.type]] += item.amount.toLong()
                    updateInventory(wallet, clickedInventory)
                    return@register
                }
                isCancelled = true
            }

            register<InventoryDragEvent> {
                val clickedInventory = inventory
                if (clickedInventory.name != CoinerPlugin.EXCHANGE)
                {
                    return@register
                }
                for (slot in rawSlots)
                {
                    if (slot < clickedInventory.size)
                    {
                        isCancelled = true
                        return@register
                    }
                }
            }
        }
    }

    private fun updateInventory(wallet:Wallet, inventory: Inventory) {
        val value = wallet.value
        for ((index, denomination) in currency.descending.take(9).withIndex())
        {
            val itemStack = ItemStack(mapping[denomination], 1)
            val itemMeta = itemStack.itemMeta
            itemMeta.displayName = ChatColor.RESET + denomination.name.plural + " x " + value / denomination.value
            itemStack.itemMeta = itemMeta
            inventory.setItem(index, itemStack)
        }
    }

    fun open(player: Player) {
        val inventory = Bukkit.createInventory(player, 54, CoinerPlugin.EXCHANGE)
        val exchangeWallet = currency.createWallet()
        updateInventory(exchangeWallet, inventory)
        player.openInventory(inventory)
        player.setMetadata(CoinerPlugin.COINER_PLUGIN_INVENTORY, FixedMetadataValue(plugin, exchangeWallet))
    }

    override fun close() {
        HandlerList.unregisterAll(listener)
    }
}