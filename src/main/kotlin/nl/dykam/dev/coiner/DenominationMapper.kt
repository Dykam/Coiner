package nl.dykam.dev.coiner

import com.google.common.collect.BiMap
import com.google.common.collect.ImmutableBiMap

interface DenominationMapper<T> {
    operator fun get(mapped: T): Denomination
    operator fun get(denomination: Denomination): T
    operator fun contains(mapped: T): Boolean
    operator fun contains(denomination: Denomination): Boolean

    companion object {
        fun <T> simple(currency: Currency, mapping: Map<T, Denomination>): DenominationMapper<T> {
            val hashMapper = HashMapper(ImmutableBiMap.copyOf(mapping))
            if (currency.descending.any { it !in hashMapper }) {
                throw IllegalArgumentException("mapping does not map all denominations in currency")
            }
            return hashMapper
        }
    }

    private class HashMapper<T>(val map: BiMap<T, Denomination>): DenominationMapper<T> {
        override fun contains(denomination: Denomination): Boolean {
            return denomination in map.inverse()
        }

        override fun contains(mapped: T): Boolean {
            return mapped in map
        }

        override fun get(mapped: T): Denomination {
            return map[mapped]!!
        }

        override fun get(denomination: Denomination): T {
            return map.inverse()[denomination]!!
        }
    }
}