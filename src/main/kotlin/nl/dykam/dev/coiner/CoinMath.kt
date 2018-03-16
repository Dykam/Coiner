package nl.dykam.dev.coiner

object CoinMath {
    fun denominate(currency: Currency, amount: Long): Wallet {
        val wallet = currency.createWallet()
        var amountLeft = amount
        for (denomination in currency.descending) {
            val count = amountLeft / denomination.value
            amountLeft -= count * denomination.value
            wallet[denomination] = count
        }
        return wallet
    }

    /**
     * Redistributes the currency in subWallet so it is composed using currency in sourceWallet.
     * @param wallet
     * @param valueToRemove
     * @param allowChange Whether to swap currency when the available denominations aren't sufficient
     * @return
     */
    @JvmOverloads
    fun subtract(wallet: Wallet, valueToRemove: Long, allowChange: Boolean = true): Wallet {
        if (valueToRemove > wallet.value)
            throw IllegalArgumentException("Not enough money in the wallet")

        var valueToRemoveLeft = valueToRemove

        val currency = wallet.currency
        val workingWallet = wallet.clone()
        val delta = currency.createWallet()
        // Just take existing denominations in the wallet
        for (denomination in currency.descending) {
            val sourceAmount = workingWallet[denomination]
            val amount = Math.min(valueToRemoveLeft / denomination.value, sourceAmount)
            val value = amount * denomination.value
            valueToRemoveLeft -= value
            workingWallet[denomination] -= amount
            delta[denomination] -= amount
        }
        if (valueToRemoveLeft == 0L) {
            delta.compress()
            return delta
        }
        if (!allowChange)
            throw IllegalArgumentException("wallet requires change but allowChange is false")
        if (currency.ascending.first().value != 1L)
            throw UnsupportedOperationException("Currency does not have a 1-valued denomination.")
        // Now if there's still money left, we need to create change using lower denominations.
        // For simplicity we require a 1-valued denomination as part of the currency.
        // Go through the generated denominations. If there isn't enough of one denomination, there should be enough of
        // a lower denomination
        var denominationValue: Long = 0
        for (denomination in currency.ascending) {
            val value = denomination.value
            if (value > 1 && workingWallet[denomination] > 0) {
                denominationValue = value
                workingWallet[denomination] -= 1L
                delta[denomination] -= 1L
                break
            }
        }
        if (denominationValue == 0L) {
            throw IllegalStateException()
        }
        denominationValue -= valueToRemoveLeft
        // So one of the chosen denomination has been removed. After subtracting the required amount, give back the rest
        val change = denominate(currency, denominationValue)
        for (entry in change.contents) {
            delta[entry.key] += entry.value
        }
        delta.compress()
        return delta
    }
}