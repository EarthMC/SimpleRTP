package dev.warriorrr.simplertp.collection;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractQueue;
import java.util.Iterator;

public class ImmutableQueue<E> extends AbstractQueue<E> {
    private static final ImmutableQueue<?> INSTANCE = new ImmutableQueue<>();

    @SuppressWarnings("unchecked")
    public static <T> ImmutableQueue<T> instance() {
        return (ImmutableQueue<T>) INSTANCE;
    }

    @Override
    public @NotNull Iterator<E> iterator() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public E next() {
                return null;
            }
        };
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean offer(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E poll() {
        return null;
    }

    @Override
    public E peek() {
        return null;
    }
}
