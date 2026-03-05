package org.savadanko;

public class OpenHashTableOptimized<K, V> {

    private static final class Node<K, V> {
        final int hash;
        final K key;
        V value;
        Node<K, V> next;

        Node(int hash, K key, V value, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private static final float LOAD_FACTOR = 0.75f;

    private Node<K, V>[] table;
    private int size;
    private int threshold;

    @SuppressWarnings("unchecked")
    public OpenHashTableOptimized(int capacityPowerOfTwo) {
        if (Integer.bitCount(capacityPowerOfTwo) != 1) {
            throw new IllegalArgumentException("capacity must be power of two");
        }
        this.table = (Node<K, V>[]) new Node[capacityPowerOfTwo];
        this.threshold = (int) (capacityPowerOfTwo * LOAD_FACTOR);
    }

    public V get(K key) {
        int h = spreadHash(key);
        Node<K, V> e = table[h & (table.length - 1)];
        while (e != null) {
            if (e.hash == h && keysEqual(e.key, key)) return e.value;
            e = e.next;
        }
        return null;
    }

    public void put(K key, V value) {
        int h = spreadHash(key);
        int idx = h & (table.length - 1);

        Node<K, V> e = table[idx];
        while (e != null) {
            if (e.hash == h && keysEqual(e.key, key)) {
                e.value = value;
                return;
            }
            e = e.next;
        }

        table[idx] = new Node<>(h, key, value, table[idx]);
        if (++size > threshold) resize();
    }

    public void remove(K key) {
        int h = spreadHash(key);
        int idx = h & (table.length - 1);

        Node<K, V> prev = null;
        Node<K, V> e = table[idx];

        while (e != null) {
            Node<K, V> next = e.next;
            if (e.hash == h && keysEqual(e.key, key)) {
                if (prev == null) table[idx] = next;
                else prev.next = next;
                size--;
                return;
            }
            prev = e;
            e = next;
        }
    }

    private static boolean keysEqual(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    private static int spreadHash(Object key) {
        int h = (key == null) ? 0 : key.hashCode();
        return h ^ (h >>> 16);
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        Node<K, V>[] oldTab = table;
        int newCap = oldTab.length << 1;
        Node<K, V>[] newTab = (Node<K, V>[]) new Node[newCap];
        int newMask = newCap - 1;

        for (Node<K, V> head : oldTab) {
            Node<K, V> e = head;
            while (e != null) {
                Node<K, V> next = e.next;
                int idx = e.hash & newMask;
                e.next = newTab[idx];
                newTab[idx] = e;
                e = next;
            }
        }

        table = newTab;
        threshold = (int) (newCap * LOAD_FACTOR);
    }
}
