package de.tyro.mcnetwork.util;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Collectors;

public class FixedFiFoQueue<V> extends AbstractQueue<V> {
    final V[] items;
    int count;

    @SuppressWarnings("unchecked")
    public FixedFiFoQueue(int capacity) {
        items = (V[]) new Object[capacity];
        count = 0;
    }

    @Override
    public @NotNull Iterator<V> iterator() {
        return Arrays.stream(items).filter(Objects::nonNull).iterator();
    }

    @Override
    public int size() {
        return count;
    }

    @Override
    public boolean offer(V v) {
        if (v == null) throw new NullPointerException();
        if (count == items.length) poll();
        this.items[count] = v;
        count++;
        return true;
    }

    @Override
    public V poll() {
        if (count <= 0) return null;

        var item = items[0];
        shift();
        count--;
        return item;
    }

    private void shift() {
        for (int i = 0; i < count - 1; i++) {
            items[i] = items[i + 1];
        }
    }

    @Override
    public V peek() {
        if (count <= 0) return null;
        return items[0];
    }

    @Override
    public String toString() {
        return "[" + Arrays.stream(items).filter(Objects::nonNull).map(Object::toString).collect(Collectors.joining(",")) + "]";
    }
}
