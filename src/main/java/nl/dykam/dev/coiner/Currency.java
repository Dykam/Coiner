package nl.dykam.dev.coiner;

import java.util.*;

public class Currency<T> {
    private Map<T, Denomination<T>> registrations;
    private SortedSet<Denomination<T>> byValueDesc;
    private SortedSet<Denomination<T>> byValueAsc;

    public Currency() {
        registrations = new HashMap<>();
        byValueDesc = new TreeSet<>(Comparator.comparingLong(o -> -o.getValue()));
        byValueAsc = new TreeSet<>(Comparator.comparingLong(o -> o.getValue()));
    }

    public Currency<T> registerDenomination(T key, long value, String singularName, String pluralName) {
        Denomination<T> denomination = new Denomination<>(key, value, singularName, pluralName);
        registrations.put(key, denomination);
        byValueDesc.add(denomination);
        byValueAsc.add(denomination);
        return this;
    }

    public Wallet<T> createWallet() {
        return new Wallet<T>(this);
    }

    public Iterable<Denomination<T>> descending() {
        return byValueDesc;
    }

    public Iterable<Denomination<T>> ascending() {
        return byValueAsc;
    }

    public Denomination<T> get(T key) {
        return registrations.get(key);
    }

    public boolean validDenimination(T key) {
        return registrations.containsKey(key);
    }
}
