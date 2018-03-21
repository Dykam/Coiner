package nl.dykam.dev.coiner

import java.util.*
class Wallet @JvmOverloads constructor(val currency: Currency, contents:Map<Denomination, Long> = emptyMap()) {
    val contents:MutableMap<Denomination, Long> = HashMap(contents).withDefault { 0 }
    val value:Long
        get() = contents.entries.fold(0L) { s, (denomination, amount) -> s + denomination * amount }

    operator fun set(denomination: Denomination, amount: Long) {
        validateDenomination(denomination)

        contents[denomination] = amount
    }

    operator fun get(denomination: Denomination): Long {
        validateDenomination(denomination)

        return contents[denomination]!!
    }

    private fun validateDenomination(denomination: Denomination) {
        if (denomination !in currency)
            throw IllegalArgumentException("Denomination not found in currency")
    }

    fun clone(): Wallet = Wallet(currency, contents)

    fun compress() {
        for (entry in contents.entries)
        {
            if (entry.value == 0L)
                contents.remove(entry.key)
        }
    }

    override fun toString():String {
        return toString(false)
    }

    private fun toString(showZeroDenominations:Boolean):String {
        val sb = StringBuilder()
        val parts = ArrayList<String>()
        for (denomination in currency.descending)
        {
            val amount = contents[denomination] ?: continue

            if (!showZeroDenominations && amount == 0L)
                continue

            parts.add(denomination.toString(amount))
        }
        for (i in parts.indices)
        {
            sb.append(parts[i])
            if (i == parts.size - 2)
            {
                sb.append(" and ")
            }
            else if (i != parts.size - 1)
            {
                sb.append(", ")
            }
        }
        return sb.toString()
    }
}

fun Currency.createWallet():Wallet {
    return Wallet(this)
}