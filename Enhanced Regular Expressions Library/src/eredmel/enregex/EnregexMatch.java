package eredmel.enregex;

import eredmel.regex.MatchResult;
import eredmel.regex.Matcher;

public class EnregexMatch {
	private MatchResult mat;
	private int offset;
	public EnregexMatch(int offset, Matcher mat) {
		this.offset = offset;
		this.mat = mat.toMatchResult();
	}
	public int start() {
		return offset + mat.start();
	}
	public int start(int group) {
		return offset + mat.start(group);
	}
	public int start(String group) {
		return offset + mat.start(group);
	}
	public int end() {
		return offset + mat.end();
	}
	public int end(int group) {
		return offset + mat.end(group);
	}
	public int end(String group) {
		return offset + mat.end(group);
	}
	public String group(int group) {
		return mat.group(group);
	}
	public String group(String group) {
		return mat.group(group);
	}
	public int groupCount(int group) {
		return mat.groupCount();
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mat == null) ? 0 : mat.hashCode());
		result = prime * result + offset;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		EnregexMatch other = (EnregexMatch) obj;
		if (mat == null) {
			if (other.mat != null) return false;
		} else if (!mat.equals(other.mat)) return false;
		if (offset != other.offset) return false;
		return true;
	}
	@Override
	public String toString() {
		return "EREMatch [mat=" + mat + ", offset=" + offset + "]";
	}
}
