package openjdk.regex;

import java.util.ArrayList;
import java.util.Objects;

public class MatchCache implements MatchResult {
	private final ArrayList<Range>[] groupsr;
	private final int first, last;
	private final Pattern parentPattern;
	private final CharSequence text;
	public MatchCache(Matcher matcher) {
		@SuppressWarnings("unchecked")
		ArrayList<Range>[] groupsr = new ArrayList[matcher.groupsr.length];
		for (int i = 0; i < groupsr.length; i++) {
			groupsr[i] = new ArrayList<Range>(matcher.groupsr[i]);
		}
		this.first = matcher.first;
		this.last = matcher.last;
		this.groupsr = groupsr;
		this.parentPattern = matcher.parentPattern;
		this.text = matcher.text;
	}
	public Range range(int group) {
		return range(group, iterations(group) - 1);
	}
	public int iterations(int group) {
		return groupsr[group].size();
	}
	public int iterations(String group) {
		return iterations(getMatchedGroupIndex(group));
	}
	public Range range(int group, int iteration) {
		if (iteration >= groupsr[group].size() || iteration < 0) return null;
		return groupsr[group].get(iteration);
	}
	public Range range(String groupName, int iteration) {
		return range(getMatchedGroupIndex(groupName), iteration);
	}
	@Override
	public int start() {
		if (first < 0) throw new IllegalStateException("No match available");
		return first;
	}
	@Override
	public int start(int group) {
		if (first < 0) throw new IllegalStateException("No match available");
		if (group < 0 || group > groupCount())
			throw new IndexOutOfBoundsException("No group " + group);
		return range(group).start;
	}
	public int start(String name) {
		return start(getMatchedGroupIndex(name));
	}
	@Override
	public int end() {
		if (first < 0) throw new IllegalStateException("No match available");
		return last;
	}
	@Override
	public int end(int group) {
		if (first < 0) throw new IllegalStateException("No match available");
		if (group < 0 || group > groupCount())
			throw new IndexOutOfBoundsException("No group " + group);
		return range(group).end;
	}
	public int end(String name) {
		return end(getMatchedGroupIndex(name));
	}
	@Override
	public String group() {
		return group(0);
	}
	@Override
	public String group(int group) {
		if (first < 0) throw new IllegalStateException("No match found");
		if (group < 0 || group > groupCount())
			throw new IndexOutOfBoundsException("No group " + group);
		Range range = range(group);
		if (range == null) return null;
		return getRange(range).toString();
	}
	public String group(String name) {
		int group = getMatchedGroupIndex(name);
		return group(group);
	}
	@Override
	public int groupCount() {
		return parentPattern.registry.capturingGroupCount - 1;
	}
	public CharSequence getRange(Range range) {
		return getSubSequence(range.start, range.end);
	}
	CharSequence getSubSequence(int beginIndex, int endIndex) {
		return text.subSequence(beginIndex, endIndex);
	}
	int getMatchedGroupIndex(String name) {
		Objects.requireNonNull(name, "Group name");
		if (first < 0) throw new IllegalStateException("No match found");
		if (!parentPattern.registry.groupDefined(name))
			throw new IllegalArgumentException("No group with name <" + name
					+ ">");
		return parentPattern.registry.groupNumber(name);
	}
	@Override
	public String toString() {
		return group();
	}
}
