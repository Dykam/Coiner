package nl.dykam.dev.coiner

data class Denomination(val value: Long, val singularName: String, val pluralName: String) {
    fun getValue(amount:Long):Long {
        return value * amount
    }
    fun toString(amount:Long):String {
        return "$amount ${nameFor(amount)}"
    }

    private fun nameFor(amount: Long) = if (amount == 1L) singularName else pluralName
}