package nl.dykam.dev.coiner

import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.economy.EconomyResponse
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.logging.Logger
import kotlin.math.absoluteValue

@Suppress("OverridingDeprecatedMember", "DEPRECATION")
internal class CoinerEconomy(
        private val server: Server,
        private val currency: Currency,
        private val mapper: DenominationMapper<Material>,
        private val logger: Logger
) : Economy {
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
    override fun hasAccount(offlinePlayer: OfflinePlayer):Boolean {
        return true
    }
    override fun hasAccount(s:String, s1:String):Boolean {
        val player = server.getOfflinePlayer(s)
        return hasAccount(player, s1)
    }
    override fun hasAccount(offlinePlayer: OfflinePlayer, worldName:String):Boolean {
        return hasAccount(offlinePlayer)
    }
    override fun getBalance(s:String):Double {
        val player = server.getOfflinePlayer(s)
        return getBalance(player)
    }
    override fun getBalance(offlinePlayer: OfflinePlayer):Double {
        if (!offlinePlayer.isOnline)
        {
            return 0.0
        }
        val player = offlinePlayer as Player
        val wallet = player.getWallet(currency, mapper)
        return wallet.value.toDouble()
    }
    override fun getBalance(s:String, s1:String):Double {
        val player = server.getOfflinePlayer(s)
        return getBalance(player, s1)
    }
    override fun getBalance(offlinePlayer: OfflinePlayer, world:String):Double {
        return getBalance(offlinePlayer)
    }
    override fun has(s:String, v:Double):Boolean {
        val player = server.getOfflinePlayer(s)
        return has(player, v)
    }
    override fun has(player: OfflinePlayer, amount:Double):Boolean {
        return getBalance(player) >= amount
    }
    override fun has(s:String, s1:String, v:Double):Boolean {
        val player = server.getOfflinePlayer(s)
        return has(player, s1, v)
    }
    override fun has(offlinePlayer: OfflinePlayer, world:String, amount:Double):Boolean {
        return has(offlinePlayer, amount)
    }
    override fun withdrawPlayer(s:String, v:Double): EconomyResponse {
        val player = server.getOfflinePlayer(s)
        return withdrawPlayer(player, v)
    }
    override fun withdrawPlayer(offlinePlayer: OfflinePlayer, amount:Double): EconomyResponse {
        if (!offlinePlayer.isOnline)
            return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Player offline")
        val player = offlinePlayer as Player
        val wallet = player.getWallet(currency, mapper)
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
    private fun applyWallet(player: Player, changes:Wallet) {
        for (change in changes.contents)
        {
            val itemStack = ItemStack(mapper[change.key], change.value.toInt().absoluteValue)
            if (change.value < 0)
                player.inventory.removeItem(itemStack)
            else
                player.inventory.addItem(itemStack)
        }
    }
    override fun withdrawPlayer(s:String, s1:String, v:Double): EconomyResponse {
        val player = server.getOfflinePlayer(s)
        return withdrawPlayer(player, s1, v)
    }
    override fun withdrawPlayer(offlinePlayer: OfflinePlayer, s:String, v:Double): EconomyResponse {
        return withdrawPlayer(offlinePlayer, v)
    }
    override fun depositPlayer(s:String, v:Double): EconomyResponse {
        val player = server.getOfflinePlayer(s)
        return depositPlayer(player, v)
    }
    override fun depositPlayer(offlinePlayer: OfflinePlayer, amount:Double): EconomyResponse {
        if (!offlinePlayer.isOnline)
            return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Player offline")
        val player = offlinePlayer as Player
        applyWallet(player, CoinMath.denominate(currency, amount.toLong()))
        return EconomyResponse(amount, getBalance(offlinePlayer), EconomyResponse.ResponseType.SUCCESS, "")
    }
    override fun depositPlayer(s:String, s1:String, v:Double): EconomyResponse {
        val player = server.getOfflinePlayer(s)
        return depositPlayer(player, s1, v)
    }
    override fun depositPlayer(offlinePlayer: OfflinePlayer, s:String, v:Double): EconomyResponse {
        return depositPlayer(offlinePlayer, v)
    }
    override fun createBank(s:String, s1:String): EconomyResponse {
        return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "")
    }
    override fun createBank(s:String, offlinePlayer: OfflinePlayer): EconomyResponse {
        return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "")
    }
    override fun deleteBank(s:String): EconomyResponse {
        return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "")
    }
    override fun bankBalance(s:String): EconomyResponse {
        return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "")
    }
    override fun bankHas(s:String, v:Double): EconomyResponse {
        return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "")
    }
    override fun bankWithdraw(s:String, v:Double): EconomyResponse {
        return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "")
    }
    override fun bankDeposit(s:String, v:Double): EconomyResponse {
        return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "")
    }
    override fun isBankOwner(s:String, s1:String): EconomyResponse {
        val player = server.getOfflinePlayer(s)
        return isBankOwner(s, player)
    }
    override fun isBankOwner(s:String, offlinePlayer: OfflinePlayer): EconomyResponse {
        return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "")
    }
    override fun isBankMember(s:String, s1:String): EconomyResponse {
        val player = server.getOfflinePlayer(s)
        return isBankMember(s, player)
    }
    override fun isBankMember(s:String, offlinePlayer: OfflinePlayer): EconomyResponse {
        return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "")
    }
    override fun createPlayerAccount(s:String):Boolean {
        val player = server.getOfflinePlayer(s)
        return createPlayerAccount(player)
    }
    override fun createPlayerAccount(offlinePlayer: OfflinePlayer):Boolean {
        return false
    }
    override fun createPlayerAccount(s:String, s1:String):Boolean {
        val player = server.getOfflinePlayer(s)
        return createPlayerAccount(player, s1)
    }
    override fun createPlayerAccount(offlinePlayer: OfflinePlayer, world:String):Boolean {
        return false
    }
}
