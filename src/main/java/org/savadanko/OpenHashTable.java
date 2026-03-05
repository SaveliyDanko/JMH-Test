    package org.savadanko;

    import java.util.ArrayList;
    import java.util.List;
    import java.util.Objects;

    public class OpenHashTable<K, V> {

        private static class HashNode<K, V> {
            K key;
            V value;
            HashNode<K, V> next;

            public HashNode(K key, V value) {
                this.key = key;
                this.value = value;
                this.next = null;
            }
        }

        private final int capacity;
        private final HashNode<K, V>[] table;
        private final List<String> executionTrace;
        private final boolean traceEnabled;

        @SuppressWarnings("unchecked")
        public OpenHashTable(int capacity, boolean traceEnabled) {
            this.capacity = capacity;
            this.table = (HashNode<K, V>[]) new HashNode[capacity];
            this.traceEnabled = traceEnabled;
            this.executionTrace = traceEnabled ? new ArrayList<>() : null;
        }

        private void record(String point) {
            if (traceEnabled) executionTrace.add(point);
        }

        public List<String> getTrace() {
            return traceEnabled ? new ArrayList<>(executionTrace) : List.of();
        }

        public void clearTrace() {
            if (traceEnabled) executionTrace.clear();
        }

        private int hashFunction(K key) {
            return Math.abs(Objects.hashCode(key)) % capacity;
        }

        public void put(K key, V value) {
            record("PUT_START");
            int index = hashFunction(key);
            record("CALC_HASH");

            HashNode<K, V> current = table[index];
            while (current != null) {
                record("ITERATE_CHAIN");
                if (Objects.equals(current.key, key)) {
                    current.value = value;
                    record("REPLACE_VALUE");
                    return;
                }
                current = current.next;
            }

            record("INSERT_NEW");
            HashNode<K, V> newNode = new HashNode<>(key, value);
            newNode.next = table[index];
            table[index] = newNode;
        }

        public V get(K key) {
            record("GET_START");
            int index = hashFunction(key);
            record("CALC_HASH");

            HashNode<K, V> current = table[index];
            while (current != null) {
                record("ITERATE_CHAIN");
                if (Objects.equals(current.key, key)) {
                    record("FOUND_KEY");
                    return current.value;
                }
                current = current.next;
            }
            record("NOT_FOUND");
            return null;
        }

        public void remove(K key) {
            record("REMOVE_START");
            int index = hashFunction(key);
            record("CALC_HASH");

            HashNode<K, V> current = table[index];
            HashNode<K, V> prev = null;

            while (current != null) {
                record("ITERATE_CHAIN");
                if (Objects.equals(current.key, key)) {
                    record("REMOVE_FOUND");
                    if (prev == null) {
                        record("REMOVE_HEAD");
                        table[index] = current.next;
                    } else {
                        record("REMOVE_INNER");
                        prev.next = current.next;
                    }
                    return;
                }
                prev = current;
                current = current.next;
            }
            record("REMOVE_NOT_FOUND");
        }
    }