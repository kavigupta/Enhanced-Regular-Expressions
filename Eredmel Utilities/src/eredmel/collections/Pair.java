package eredmel.collections;


import java.util.Map.Entry;

public class Pair<K, V> implements Entry<K, V>, Comparable<Entry<K, V>> {
	public final K key;
	public final V value;
	public static <K, V> Pair<K, V> getInstance(K key, V value) {
		return new Pair<>(key, value);
	}
	private Pair(K key, V value) {
		this.key = key;
		this.value = value;
	}
	@Override
	public K getKey() {
		return key;
	}
	@Override
	public V getValue() {
		return value;
	}
	@Override
	public V setValue(V value) {
		throw new UnsupportedOperationException();
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Pair<?, ?> other = (Pair<?, ?>) obj;
		if (key == null) {
			if (other.key != null) return false;
		} else if (!key.equals(other.key)) return false;
		if (value == null) {
			if (other.value != null) return false;
		} else if (!value.equals(other.value)) return false;
		return true;
	}
	@Override
	public int compareTo(Entry<K, V> o) {
		if (!(key instanceof Comparable<?>))
			throw new UnsupportedOperationException();
		@SuppressWarnings("unchecked")
		Comparable<K> cKey = (Comparable<K>) key;
		int comp = cKey.compareTo(o.getKey());
		if (comp != 0) return comp;
		if (!(value instanceof Comparable<?>)) return 0;
		@SuppressWarnings("unchecked")
		Comparable<V> cValue = (Comparable<V>) value;
		return cValue.compareTo(o.getValue());
	}
	@Override
	public String toString() {
		return "Pair [key=" + key + ", value=" + value + "]";
	}
}
