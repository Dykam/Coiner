package nl.dykam.dev.coiner

data class Denomination(val value: Long, val name: Name) {
    fun toString(amount:Long):String {
        return "$amount ${name.of(amount)}"
    }

    operator fun times(amount: Long): Long {
        return value * amount
    }
}