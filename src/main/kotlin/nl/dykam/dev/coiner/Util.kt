package nl.dykam.dev.coiner

import org.bukkit.Material
import org.bukkit.entity.Player


fun Player.getWallet(currency: Currency, mapper: DenominationMapper<Material>):Wallet {
    val wallet = currency.createWallet()
    player.inventory.contents
            .filter { it != null && it.type in mapper }
            .forEach { wallet[mapper[it.type]] += it.amount.toLong() }
    return wallet
}