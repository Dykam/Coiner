package nl.dykam.dev.coiner
import java.util.*
class Currency(denominations: Collection<Denomination>) {
    constructor(vararg denominations: Denomination) : this(denominations as Collection<Denomination>)

    private val byValueDesc:SortedSet<Denomination>
    private val byValueAsc:SortedSet<Denomination>

    init{
        byValueDesc = sortedSetOf(Comparator<Denomination> { o1, o2 -> (o1!!.value - o2!!.value).toInt() })
        byValueAsc = sortedSetOf(Comparator<Denomination> { o1, o2 -> (o2!!.value - o1!!.value).toInt() })

        byValueDesc.addAll(denominations)
        byValueAsc.addAll(denominations)
    }

    val descending:Iterable<Denomination> = byValueDesc
    val ascending:Iterable<Denomination> = byValueAsc

    operator fun contains(denomination: Denomination):Boolean {
        return byValueDesc.contains(denomination)
    }

    operator fun plusAssign(denomination: Denomination) {
        byValueDesc.add(denomination)
        byValueAsc.add(denomination)
    }
}