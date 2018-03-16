package nl.dykam.dev.coiner;

public class Denomination<T> {
    private T key;
    private final long value;
    private final String singularName;
    private final String pluralName;

    Denomination(T key, long value, String singularName, String pluralName) {
        this.key = key;
        this.value = value;
        this.singularName = singularName;
        this.pluralName = pluralName;
    }

    public T getKey() {
        return key;
    }

    public long getValue() {
        return value;
    }

    public long getValue(long amount) {
        return getValue() * amount;
    }

    public String getSingularName() {
        return singularName;
    }

    public String getPluralName() {
        return pluralName;
    }

    public String toString(long amount) {
        String name = amount == 1 ? getSingularName() : getPluralName();
        return String.format("%d %s", amount, name);
    }
}
