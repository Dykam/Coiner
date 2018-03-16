package nl.dykam.dev.coiner;

import java.util.*;

public class CoinMath {
    public static <T> Wallet<T> denominate(Currency<T> currency, long amount) {
        Map<T, Long> wallet = new HashMap<>();

        for (Denomination<T> denomination : currency.descending()) {
            long count = amount / denomination.getValue();
            amount -= count * denomination.getValue();
            wallet.put(denomination.getKey(), count);
        }

        return new Wallet<>(currency, wallet);
    }

//    /**
//     * Redistributes the currency in subWallet so it is composed using currency in sourceWallet.
//     * @param sourceWallet
//     * @param subWallet
//     * @return
//     */
//    public static Wallet<Material> subtract(Wallet<Material> sourceWallet, Wallet<Material> subWallet) {
//        return subtract(sourceWallet, subWallet, true);
//    }
//
//    /**
//     * Redistributes the currency in subWallet so it is composed using currency in sourceWallet.
//     * @param sourceWallet
//     * @param subWallet
//     * @param allowChange Whether to swap currency when the available denominations aren't sufficient
//     * @return
//     */
//    public static <T> Wallet<T> subtract(Wallet<T> sourceWallet, Wallet<T> subWallet, boolean allowChange) {
//        if(subWallet.getValue() > sourceWallet.getValue())
//            throw new IllegalArgumentException("Source wallet must have more value than the sub wallet");
//        if(sourceWallet.getCurrency() != subWallet.getCurrency())
//            throw new IllegalArgumentException("Currencies mismatch");
//
//        Currency<T> currency = subWallet.getCurrency();
//
//        Wallet<T> workingSource = sourceWallet.clone();
//        Wallet<T> delta = currency.createWallet();
//
//        // First do the easy work. Remove matching denominations
//        for (Denomination<T> denomination : currency.descending()) {
//            long intersection = Math.min(workingSource.getValue(denomination.getKey()), subWallet.getValue(denomination.getKey()));
//
//            if(intersection == 0)
//                continue;
//
//            workingSource.add(denomination.getKey(), -intersection);
//            delta.add(denomination.getKey(), -intersection);
//        }
//
//        long left = workingSource.getValue() - delta.getValue();
//
//        // Try to fill left using existing denominations
//        for (Denomination<T> denomination : currency.descending()) {
//            long sourceAmount = workingSource.getValue(denomination.getKey());
//
//            long amount = Math.min(left / denomination.getValue(), sourceAmount);
//            long value = amount * denomination.getValue();
//
//            left -= value;
//            workingSource.add(denomination.getKey(), -amount);
//            delta.add(denomination.getKey(), -amount);
//        }
//
//        if (left == 0) {
//            return delta;
//        }
//
//        if (!allowChange)
//            throw new IllegalArgumentException("sourceWallet requires change but allowChange is false");
//
//        if(currency.ascending().iterator().next().getValue() != 1)
//            throw new UnsupportedOperationException("Currency does not have a 1-valued denomination.");
//
//        // Now if there's still money left, we need to create change using lower denominations.
//        // For simplicity we require a 1-valued denomination as part of the currency.
//
//        // Go through the generated denominations. If there isn't enough of one denomination, there should be enough of
//        // a lower denomination
//
//        for (Denomination<T> denomination : currency.descending()) {
//
//        }
//
//
//        return null;
//    }

    /**
     * Redistributes the currency in subWallet so it is composed using currency in sourceWallet.
     * @param wallet
     * @param valueToRemove
     * @return
     */
    public static <T> Wallet<T> subtract(Wallet<T> wallet, long valueToRemove) {
        return subtract(wallet, valueToRemove, true);
    }

    /**
     * Redistributes the currency in subWallet so it is composed using currency in sourceWallet.
     * @param wallet
     * @param valueToRemove
     * @param allowChange Whether to swap currency when the available denominations aren't sufficient
     * @return
     */
    public static <T> Wallet<T> subtract(Wallet<T> wallet, long valueToRemove, boolean allowChange) {
        if(valueToRemove > wallet.getValue())
            throw new IllegalArgumentException("Not enough money in the wallet");

        Currency<T> currency = wallet.getCurrency();

        Wallet<T> workingWallet = wallet.clone();
        Wallet<T> delta = currency.createWallet();

        // Just take existing denominations in the wallet
        for (Denomination<T> denomination : currency.descending()) {
            long sourceAmount = workingWallet.getValue(denomination.getKey());

            long amount = Math.min(valueToRemove / denomination.getValue(), sourceAmount);
            long value = amount * denomination.getValue();

            valueToRemove -= value;
            workingWallet.add(denomination.getKey(), -amount);
            delta.add(denomination.getKey(), -amount);
        }

        if (valueToRemove == 0) {
            delta.compress();
            return delta;
        }

        if (!allowChange)
            throw new IllegalArgumentException("wallet requires change but allowChange is false");

        if(currency.ascending().iterator().next().getValue() != 1)
            throw new UnsupportedOperationException("Currency does not have a 1-valued denomination.");

        // Now if there's still money left, we need to create change using lower denominations.
        // For simplicity we require a 1-valued denomination as part of the currency.

        // Go through the generated denominations. If there isn't enough of one denomination, there should be enough of
        // a lower denomination

        long denominationValue = 0;
        for (Denomination<T> denomination : currency.ascending()) {
            long value = denomination.getValue();
            if(value > 1 && workingWallet.getValue(denomination.getKey()) > 0) {
                denominationValue = value;
                workingWallet.add(denomination.getKey(), -1);
                delta.add(denomination.getKey(), -1);
                break;
            }
        }

        if(denominationValue == 0) {
            throw new IllegalStateException();
        }

        denominationValue -= valueToRemove;

        // So one of the chosen denomination has been removed. After subtracting the required amount, give back the rest
        Wallet<T> change = denominate(currency, denominationValue);
        for (Map.Entry<T, Long> entry : change.getContents().entrySet()) {
            delta.add(entry.getKey(), entry.getValue());
        }

        delta.compress();

        return delta;
    }
}

