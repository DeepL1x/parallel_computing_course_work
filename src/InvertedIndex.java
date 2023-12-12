import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.HashMap;
import java.util.HashSet;

public class InvertedIndex {
    private static final int DEF_INIT_CAP = 16;
    private static final float DEF_LOAD_FACTOR = 0.75f;

    private List<Entry>[] buckets;
    private int size;
    private float loadFactor;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public InvertedIndex() {
        this(DEF_INIT_CAP, DEF_LOAD_FACTOR);
    }

    public InvertedIndex(int capacity) {
        this(capacity, DEF_LOAD_FACTOR);
    }

    public InvertedIndex(float loadFactor) {
        this(DEF_INIT_CAP, loadFactor);
    }

    public InvertedIndex(int capacity, float loadFactor) {
        this.buckets = new List[capacity];
        for (int i = 0; i < capacity; i++) {
            this.buckets[i] = new ArrayList<>();
        }
        this.loadFactor = loadFactor;
    }

    private void resizeIfNeeded() {
        if ((float) size / buckets.length > loadFactor) {
            int newCapacity = buckets.length * 2;
            List<Entry>[] newBuckets = new List[newCapacity];
            for (int i = 0; i < newCapacity; i++) {
                newBuckets[i] = new ArrayList<>();
            }

            for (List<Entry> bucket : buckets) {
                for (Entry entry : bucket) {
                    int hash = hash(entry.getWord());
                    newBuckets[hash % newCapacity].add(entry);
                }
            }

            buckets = newBuckets;
        }
    }

    public void put(String word, String filename, Integer wordPosition) {
        try {
            lock.writeLock().lock();
            resizeIfNeeded();
            int hash = hash(word);
            for (Entry entry : buckets[hash % buckets.length]) {
                if (entry.getWord().equals(word)) {
                    entry.addWordPosition(filename, wordPosition);
                    return;
                }
            }
            buckets[hash % buckets.length].add(new Entry(word, filename, wordPosition));

        } finally {
            lock.writeLock().unlock();
        }
    }

    public Map<String, Set<Integer>> get(String[] keys) {
        try {
            lock.readLock().lock();
            Map<String, Set<Integer>> result = new HashMap<>();

            if (keys.length > 0) {
                int hash = hash(keys[0]);
                for (Entry entry : buckets[hash % buckets.length]) {
                    if (entry.getWord().equals(keys[0])) {
                        result.putAll(entry.getFiles());
                    }
                }
                for (int i = 1; i < keys.length; i++) {
                    hash = hash(keys[i]);
                    for (Entry entry : buckets[hash % buckets.length]) {
                        if (entry.getWord().equals(keys[i])) {
                            processWord(result, entry.getFiles());
                        }
                    }
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }

    }

    private void processWord(Map<String, Set<Integer>> prevRes, Map<String, Set<Integer>> curFiles) {
        Set<String> commonKeys = prevRes.keySet();
        commonKeys.retainAll(curFiles.keySet());
        Map<String, Set<Integer>> newRes = new HashMap<>();
        for (String file : commonKeys) {
            Set<Integer> curWordsPositions = new HashSet<>();
            for (Integer pos : curFiles.get(file)) {
                if (prevRes.get(file).contains(pos - 1)) {
                    curWordsPositions.add(pos);
                }
            }
            if (curWordsPositions.size() > 0) {
                newRes.put(file, curWordsPositions);
            }
        }
        prevRes.clear();
        prevRes.putAll(newRes);
    }

    private int hash(String key) {
        return Math.abs(key.hashCode());
    }

    private static class Entry {
        private final String word;
        private Map<String, Set<Integer>> files;

        public Entry(String word, String filename, Integer wordPosition) {
            this.word = word;
            this.files = new HashMap<>();
            Set<Integer> positions = new HashSet<Integer>();
            positions.add(wordPosition);
            this.files.put(filename, positions);
        }

        public String getWord() {
            return word;
        }

        public Map<String, Set<Integer>> getFiles() {
            return files;
        }

        public void addWordPosition(String filename, Integer wordPosition) {
            Set<Integer> postions = this.files.get(filename);
            if (postions == null) {
                postions = new HashSet<>();
                postions.add(wordPosition);
                this.files.put(filename, postions);
            } else {
                postions.add(wordPosition);
            }
        }
    }
}
