package nl.dykam.dev.coiner

data class Name(val singular: String, val plural: String) {
    fun of(amount: Number) = if (amount == 1) singular else plural
}