package com.toroidalworld.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;

public final class FoldedCopies {
    public static <T> List<T> of(List<T> source, UnaryOperator<T> fold) {
        return fold(source, fold, ArrayList::new);
    }

    public static <T> Set<T> of(Set<T> source, UnaryOperator<T> fold) {
        return fold(source, fold, LinkedHashSet::new);
    }

    public static <T> Collection<T> of(Collection<T> source, UnaryOperator<T> fold) {
        return fold(source, fold, ArrayList::new);
    }

    private static <T, C extends Collection<T>> C fold(C source, UnaryOperator<T> fold, IntFunction<C> copyOf) {
        C copy = null;
        int unmovedPrefix = 0;
        for (T element : source) {
            T folded = fold.apply(element);
            if (copy == null) {
                if (folded == element) {
                    unmovedPrefix++;
                    continue;
                }

                copy = prefixOf(source, unmovedPrefix, copyOf);
            }

            copy.add(folded);
        }

        return copy == null ? source : copy;
    }

    private static <T, C extends Collection<T>> C prefixOf(C source, int length, IntFunction<C> copyOf) {
        C copy = copyOf.apply(source.size());
        Iterator<T> elements = source.iterator();
        for (int index = 0; index < length; index++) {
            copy.add(elements.next());
        }

        return copy;
    }

    private FoldedCopies() {
    }
}
