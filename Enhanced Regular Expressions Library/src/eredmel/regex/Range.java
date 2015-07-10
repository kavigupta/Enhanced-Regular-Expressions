/*
 * Added
 */
package eredmel.regex;

/**
 * A class representing a range of indices {@code [start, end)}
 */
public class Range implements Comparable<Range> {
	/**
	 * The starting index, inclusive
	 */
	public final int start;
	/**
	 * The ending index, exclusive.
	 */
	public final int end;
	private Range(int start, int end) {
		super();
		this.start = start;
		this.end = end;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + end;
		result = prime * result + start;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Range other = (Range) obj;
		if (end != other.end) return false;
		if (start != other.start) return false;
		return true;
	}
	@Override
	public String toString() {
		return "(" + start + ", " + end + ")";
	}
	@Override
	public Object clone() throws CloneNotSupportedException {
		return this;
	}
	/**
	 * Checks if this range is the empty range.
	 */
	public boolean isEmpty() {
		return start < 0 || end < 0;
	}
	/**
	 * Constructs a range.
	 */
	public static Range of(int first, int last) {
		return new Range(first, last);
	}
	@Override
	public int compareTo(Range o) {
		int compare = Integer.compare(this.start, o.start);
		if (compare != 0) return compare;
		return Integer.compare(this.end, o.end);
	}
}
