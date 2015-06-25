package openjdk.regex;

public class Range {
	public static final Range EMPTY = new Range(-1, -1);
	public final int start, end;
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
		return "Range [start=" + start + ", end=" + end + "]";
	}
	@Override
	public Object clone() throws CloneNotSupportedException {
		return this;
	}
	public boolean isEmpty() {
		return start < 0 || end < 0;
	}
	public static Range of(int first, int last) {
		return new Range(first, last);
	}
}
