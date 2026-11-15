package dev.aqua.client.util;

import java.util.ArrayDeque;
import java.util.Deque;

/** Считает клики мыши за последнюю секунду (заполняется из MouseMixin). */
public final class ClickTracker {
    private static final Deque<Long> LEFT = new ArrayDeque<>();
    private static final Deque<Long> RIGHT = new ArrayDeque<>();

    public static long totalLeft;

    private ClickTracker() {}

    public static synchronized void click(int button) {
        long now = System.currentTimeMillis();
        if (button == 0) { LEFT.addLast(now); totalLeft++; }
        else if (button == 1) RIGHT.addLast(now);
    }

    public static synchronized int left() { return count(LEFT); }
    public static synchronized int right() { return count(RIGHT); }

    private static int count(Deque<Long> q) {
        long cut = System.currentTimeMillis() - 1000;
        while (!q.isEmpty() && q.peekFirst() < cut) q.pollFirst();
        return q.size();
    }
}
