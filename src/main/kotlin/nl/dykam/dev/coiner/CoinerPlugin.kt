package nl.dykam.dev.coiner
import com.deanveloper.kbukkit.KotlinPlugin
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.ServicePriority
import java.util.logging.Logger

@Suppress("unused", "UNUSED_PARAMETER")
class CoinerPlugin: KotlinPlugin() {
    private var currency:Currency
    private var mapper: DenominationMapper<Material>
    private lateinit var exchangeInventory: ExchangeInventory
    init {
        val map = hashMapOf(
                Material.GOLD_BLOCK to Denomination(81, Name("Gold block", "Gold blocks")),
                Material.GOLD_INGOT to Denomination(9, Name("Gold ingot", "Gold ingots")),
                Material.GOLD_NUGGET to Denomination(1, Name("Gold nugget", "Gold nuggets"))
        )
        currency = Currency(Name("Coin", "Coins"), map.values)
        mapper = DenominationMapper.simple(currency, map)
    }
    override fun onEnable() {
        val vault = server.pluginManager.getPlugin("Vault")
        if (vault == null)
        {
            logger.severe(String.format("[%s] - Disabled due to no Vault dependency found!", description.name))
            server.pluginManager.disablePlugin(this)
            return
        }

        Bukkit.getServicesManager().register(Economy::class.java, CoinerEconomy(server, currency, mapper, logger), vault, ServicePriority.Highest)

        exchangeInventory = ExchangeInventory(currency, mapper, server, this, true)

//        val coinerSession = CoinerSession(currency);
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
                sender.sendMessage(sender.getWallet(currency, mapper).toString())
                return true
            }
            return false
        }
        return false
    }
    companion object {
        const val COINER_PLUGIN_INVENTORY = "CoinerPluginInventory"
        const val EXCHANGE = "Drop currency to exchange"
    }
}