package nl.dykam.dev.coiner

class Exchange {
    var value: Long = 0
        private set

    fun deposit(denomination: Denomination, amount: Long) {
        value += denomination * amount
    }

    fun withdraw(denomination: Denomination, amount: Long) {
        value -= denomination * amount
    }

    fun potential(denomination: Denomination): Long {
        return value / denomination.value
    }
}