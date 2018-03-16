package nl.dykam.dev.coiner;

import java.util.*;

public class Wallet<T> {
    private final Currency<T> currency;
    private final Map<T, Long> contents;

    public Wallet(Currency<T> currency) {
        this(currency, null);
    }

    public Wallet(Currency<T> currency, Map<T, Long> contents) {
        this.currency = currency;
        this.contents = contents == null ? new HashMap<>() : new HashMap<>(contents);
    }

    public Currency<T> getCurrency() {
        return currency;
    }

    public Map<T, Long> getContents() {
        return contents;
    }

    public long getValue() {
        long value = 0;
        for (Map.Entry<T, Long> objectLongEntry : contents.entrySet()) {
            value += currency.get(objectLongEntry.getKey()).getValue(objectLongEntry.getValue());
        }
        return value;
    }

    public void add(T denomination, long amount) {
        contents.putIfAbsent(denomination, 0L);
        contents.put(denomination, contents.get(denomination) + amount);
    }

    public long getValue(T denomination) {
        return contents.getOrDefault(denomination, 0L);
    }

    public Wallet<T> clone() {
        return new Wallet<>(currency, contents);
    }

    public void compress() {
        for (Map.Entry<T, Long> entry : new HashSet<>(contents.entrySet())) {
            if(entry.getValue() == 0)
                contents.remove(entry.getKey());
        }
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean showZeroDenominations) {
        StringBuilder sb = new StringBuilder();
        List<String> parts = new ArrayList<>();

        for (Denomination<T> denomination : currency.descending()) {
            if(!contents.containsKey(denomination.getKey()))
                continue;

            long amount = contents.get(denomination.getKey());
            if(!showZeroDenominations && amount == 0)
                continue;

            parts.add(denomination.toString(amount));
        }

        for (int i = 0; i < parts.size(); i++) {
            sb.append(parts.get(i));
            if(i == parts.size() - 2) {
                sb.append(" and ");
            } else if(i != parts.size() - 1) {
                sb.append(", ");
            }
        }

        return sb.toString();
    }
}
