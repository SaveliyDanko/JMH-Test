### Задание:
Необходимо сравнить Java Hashmap и кастомную Hashmap, проверить через JMH, затем попробовать оптимизировать

### `OpenHashTable` - кастомная реализация `HashTable`
```java
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
```

### `OpenHashTable` - кастомная оптимизированная реализация `HashTable`
```java
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
```

### Оптимизации
##### Индексация без `%` — через маску (power-of-two capacity)
Было:
```java
Math.abs(Objects.hashCode(key)) % capacity
```

Стало:
```java
int idx = h & (table.length - 1);
```

##### SpreadHash
Было:
```java
int h = key == null ? 0 : key.hashCode();
return h ^ (h >>> 16);
```

Стало:
```java
int idx = h & (table.length - 1);
```

##### Сохранение hash в узле
```java
final int hash;
if (e.hash == h && keysEqual(e.key, key))
```

##### Убрано логирование

##### Более дешёвое сравнение ключей
Было:
```java
Objects.equals(a, b)
```

Стало:
```java
return a == b || (a != null && a.equals(b));
```


### Бенчмарк:
```java
package org.savadanko;  
  
import org.openjdk.jmh.annotations.*;  
import org.openjdk.jmh.infra.Blackhole;  
  
import java.util.HashMap;  
import java.util.concurrent.TimeUnit;  
  
@BenchmarkMode(Mode.Throughput)  
@OutputTimeUnit(TimeUnit.SECONDS)  
@Warmup(iterations = 5, time = 1)  
@Measurement(iterations = 8, time = 1)  
@Fork(value = 3, jvmArgsAppend = {"-Xms2g", "-Xmx2g"})  
@State(Scope.Thread)  
public class HashTableBenchmark {  
  
    private static final int OPS = 1024;  
  
    @Param({"100000"})  
    public int size;  
  
    @Param({"0.75"})  
    public String load;  
  
    private float targetLoad;  
    private Integer[] hitKeys;  
    private Integer[] missKeys;  
  
    private HashMap<Integer, Integer> jdk;  
    private OpenHashTable<Integer, Integer> custom;  
    private OpenHashTableOptimized<Integer, Integer> customOpt;  
  
    @Setup(Level.Iteration)  
    public void setup() {  
        targetLoad = Float.parseFloat(load);  
  
        int capacity = nextPowerOfTwo((int) Math.ceil(size / targetLoad));  
  
        jdk = new HashMap<>((int) Math.ceil(size / targetLoad) + 1, targetLoad);  
  
        custom = new OpenHashTable<>(capacity, false);  
  
        customOpt = new OpenHashTableOptimized<>(capacity);  
  
        for (int i = 0; i < size; i++) {  
            Integer k = i;  
            Integer v = i;  
            jdk.put(k, v);  
            custom.put(k, v);  
            customOpt.put(k, v);  
        }  
  
        hitKeys = new Integer[OPS];  
        missKeys = new Integer[OPS];  
  
        int x = 1;  
        for (int i = 0; i < OPS; i++) {  
            x = x * 1664525 + 1013904223;  
            int idx = (x & 0x7fffffff) % size;  
            hitKeys[i] = idx;  
            missKeys[i] = size + idx;  
        }  
    }  
  
    // -------------------- GET HIT --------------------  
  
    @Benchmark  
    @OperationsPerInvocation(OPS)  
    public void jdk_getHit(Blackhole bh) {  
        for (int i = 0; i < OPS; i++) bh.consume(jdk.get(hitKeys[i]));  
    }  
  
    @Benchmark  
    @OperationsPerInvocation(OPS)  
    public void custom_getHit(Blackhole bh) {  
        for (int i = 0; i < OPS; i++) bh.consume(custom.get(hitKeys[i]));  
    }  
  
    @Benchmark  
    @OperationsPerInvocation(OPS)  
    public void customOpt_getHit(Blackhole bh) {  
        for (int i = 0; i < OPS; i++) bh.consume(customOpt.get(hitKeys[i]));  
    }  
  
    // -------------------- GET MISS --------------------  
  
    @Benchmark  
    @OperationsPerInvocation(OPS)  
    public void jdk_getMiss(Blackhole bh) {  
        for (int i = 0; i < OPS; i++) bh.consume(jdk.get(missKeys[i]));  
    }  
  
    @Benchmark  
    @OperationsPerInvocation(OPS)  
    public void custom_getMiss(Blackhole bh) {  
        for (int i = 0; i < OPS; i++) bh.consume(custom.get(missKeys[i]));  
    }  
  
    @Benchmark  
    @OperationsPerInvocation(OPS)  
    public void customOpt_getMiss(Blackhole bh) {  
        for (int i = 0; i < OPS; i++) bh.consume(customOpt.get(missKeys[i]));  
    }  
  
    // -------------------- PUT UPDATE (existing keys) --------------------  
  
    @Benchmark  
    @OperationsPerInvocation(OPS)  
    public void jdk_putUpdate() {  
        for (int i = 0; i < OPS; i++) jdk.put(hitKeys[i], -i);  
    }  
  
    @Benchmark  
    @OperationsPerInvocation(OPS)  
    public void custom_putUpdate() {  
        for (int i = 0; i < OPS; i++) custom.put(hitKeys[i], -i);  
    }  
  
    @Benchmark  
    @OperationsPerInvocation(OPS)  
    public void customOpt_putUpdate() {  
        for (int i = 0; i < OPS; i++) customOpt.put(hitKeys[i], -i);  
    }  
  
    // -------------------- utils --------------------  
  
    private static int nextPowerOfTwo(int x) {  
        int n = 1;  
        while (n < x) n <<= 1;  
        return n;  
    }  
}
```

### Результаты бенчмарка:
| Benchmark                                                 | load |   size | Mode  | Cnt |          Score |          Error | Units  |
| --------------------------------------------------------- | ---: | -----: | ----- | --: | -------------: | -------------: | ------ |
| HashTableBenchmark.customOpt_getHit                       | 0.75 | 100000 | thrpt |  24 |  454708218.347 | ± 10732430.388 | ops/s  |
| HashTableBenchmark.customOpt_getHit:gc.alloc.rate         | 0.75 | 100000 | thrpt |  24 |         14.354 |        ± 0.106 | MB/sec |
| HashTableBenchmark.customOpt_getHit:gc.alloc.rate.norm    | 0.75 | 100000 | thrpt |  24 |          0.033 |        ± 0.001 | B/op   |
| HashTableBenchmark.customOpt_getHit:gc.count              | 0.75 | 100000 | thrpt |  24 |          3.000 |                | counts |
| HashTableBenchmark.customOpt_getHit:gc.time               | 0.75 | 100000 | thrpt |  24 |         79.000 |                | ms     |
| HashTableBenchmark.customOpt_getMiss                      | 0.75 | 100000 | thrpt |  24 | 1480479957.708 | ± 95659582.785 | ops/s  |
| HashTableBenchmark.customOpt_getMiss:gc.alloc.rate        | 0.75 | 100000 | thrpt |  24 |         14.354 |        ± 0.098 | MB/sec |
| HashTableBenchmark.customOpt_getMiss:gc.alloc.rate.norm   | 0.75 | 100000 | thrpt |  24 |          0.010 |        ± 0.001 | B/op   |
| HashTableBenchmark.customOpt_getMiss:gc.count             | 0.75 | 100000 | thrpt |  24 |          3.000 |                | counts |
| HashTableBenchmark.customOpt_getMiss:gc.time              | 0.75 | 100000 | thrpt |  24 |         75.000 |                | ms     |
| HashTableBenchmark.customOpt_putUpdate                    | 0.75 | 100000 | thrpt |  24 |  169875455.249 |  ± 2811939.476 | ops/s  |
| HashTableBenchmark.customOpt_putUpdate:gc.alloc.rate      | 0.75 | 100000 | thrpt |  24 |       2270.255 |       ± 37.577 | MB/sec |
| HashTableBenchmark.customOpt_putUpdate:gc.alloc.rate.norm | 0.75 | 100000 | thrpt |  24 |         14.074 |        ± 0.001 | B/op   |
| HashTableBenchmark.customOpt_putUpdate:gc.time            | 0.75 | 100000 | thrpt |  24 |        853.000 |                | ms     |
| HashTableBenchmark.custom_getHit                          | 0.75 | 100000 | thrpt |  24 |  182849818.116 | ± 13791221.896 | ops/s  |
| HashTableBenchmark.custom_getHit:gc.alloc.rate            | 0.75 | 100000 | thrpt |  24 |         14.354 |        ± 0.097 | MB/sec |
| HashTableBenchmark.custom_getHit:gc.alloc.rate.norm       | 0.75 | 100000 | thrpt |  24 |          0.084 |        ± 0.011 | B/op   |
| HashTableBenchmark.custom_getHit:gc.count                 | 0.75 | 100000 | thrpt |  24 |          3.000 |                | counts |
| HashTableBenchmark.custom_getHit:gc.time                  | 0.75 | 100000 | thrpt |  24 |         75.000 |                | ms     |
| HashTableBenchmark.custom_getMiss                         | 0.75 | 100000 | thrpt |  24 |  718610429.817 |  ± 5094709.297 | ops/s  |
| HashTableBenchmark.custom_getMiss:gc.alloc.rate           | 0.75 | 100000 | thrpt |  24 |         14.355 |        ± 0.101 | MB/sec |
| HashTableBenchmark.custom_getMiss:gc.alloc.rate.norm      | 0.75 | 100000 | thrpt |  24 |          0.021 |        ± 0.001 | B/op   |
| HashTableBenchmark.custom_getMiss:gc.count                | 0.75 | 100000 | thrpt |  24 |          3.000 |                | counts |
| HashTableBenchmark.custom_getMiss:gc.time                 | 0.75 | 100000 | thrpt |  24 |         78.000 |                | ms     |
| HashTableBenchmark.custom_putUpdate                       | 0.75 | 100000 | thrpt |  24 |  123857664.587 |  ± 9675617.084 | ops/s  |
| HashTableBenchmark.custom_putUpdate:gc.alloc.rate         | 0.75 | 100000 | thrpt |  24 |       1659.066 |      ± 128.521 | MB/sec |
| HashTableBenchmark.custom_putUpdate:gc.alloc.rate.norm    | 0.75 | 100000 | thrpt |  24 |         14.109 |        ± 0.015 | B/op   |
| HashTableBenchmark.custom_putUpdate:gc.count              | 0.75 | 100000 | thrpt |  24 |         34.000 |                | counts |
| HashTableBenchmark.custom_putUpdate:gc.time               | 0.75 | 100000 | thrpt |  24 |        670.000 |                | ms     |
| HashTableBenchmark.jdk_getHit                             | 0.75 | 100000 | thrpt |  24 |  357804293.793 | ± 24351582.639 | ops/s  |
| HashTableBenchmark.jdk_getHit:gc.alloc.rate               | 0.75 | 100000 | thrpt |  24 |         14.355 |        ± 0.095 | MB/sec |
| HashTableBenchmark.jdk_getHit:gc.alloc.rate.norm          | 0.75 | 100000 | thrpt |  24 |          0.043 |        ± 0.005 | B/op   |
| HashTableBenchmark.jdk_getHit:gc.count                    | 0.75 | 100000 | thrpt |  24 |          3.000 |                | counts |
| HashTableBenchmark.jdk_getHit:gc.time                     | 0.75 | 100000 | thrpt |  24 |         74.000 |                | ms     |
| HashTableBenchmark.jdk_getMiss                            | 0.75 | 100000 | thrpt |  24 |  821902259.767 | ± 10273369.234 | ops/s  |
| HashTableBenchmark.jdk_getMiss:gc.alloc.rate              | 0.75 | 100000 | thrpt |  24 |         14.355 |        ± 0.100 | MB/sec |
| HashTableBenchmark.jdk_getMiss:gc.alloc.rate.norm         | 0.75 | 100000 | thrpt |  24 |          0.018 |        ± 0.001 | B/op   |
| HashTableBenchmark.jdk_getMiss:gc.count                   | 0.75 | 100000 | thrpt |  24 |          3.000 |                | counts |
| HashTableBenchmark.jdk_getMiss:gc.time                    | 0.75 | 100000 | thrpt |  24 |         77.000 |                | ms     |
| HashTableBenchmark.jdk_putUpdate                          | 0.75 | 100000 | thrpt |  24 |   96501880.247 |  ± 4805681.352 | ops/s  |
| HashTableBenchmark.jdk_putUpdate:gc.alloc.rate            | 0.75 | 100000 | thrpt |  24 |       1295.391 |       ± 64.134 | MB/sec |
| HashTableBenchmark.jdk_putUpdate:gc.alloc.rate.norm       | 0.75 | 100000 | thrpt |  24 |         14.142 |        ± 0.009 | B/op   |
| HashTableBenchmark.jdk_putUpdate:gc.count                 | 0.75 | 100000 | thrpt |  24 |         27.000 |                | counts |
| HashTableBenchmark.jdk_putUpdate:gc.time                  | 0.75 | 100000 | thrpt |  24 |        547.000 |                | ms     |

### Анализ результатов:
###### `getHit` (ключ найден)
- customOpt_getHit: `454,708,218 ops/s`
- jdk_getHit: `357,804,294 ops/s`
- custom_getHit: `182,849,818 ops/s`

###### `getMiss` (ключ не найден)
- customOpt_getMiss: `1,480,479,958 ops/s`
- jdk_getMiss: `821,902,260 ops/s`
- custom_getMiss: `718,610,430 ops/s`

###### `putUpdate` (обновление существующих ключей)
- customOpt_putUpdate: `169,875,455 ops/s`
- custom_putUpdate: `123,857,665 ops/s`
- jdk_putUpdate: `96,501,880 ops/s`
