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
		return parentPattern.capturingGroupCount - 1;
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
		if (!parentPattern.namedGroups().containsKey(name))
			throw new IllegalArgumentException("No group with name <" + name
					+ ">");
		return parentPattern.namedGroups().get(name);
	}
	/**
	 * Implements a non-terminal append-and-replace step.
	 * <p>
	 * This method performs the following actions:
	 * </p>
	 * <ol>
	 * <li>
	 * <p>
	 * It reads characters from the input sequence, starting at the append
	 * position, and appends them to the given string buffer. It stops after
	 * reading the last character preceding the previous match, that is, the
	 * character at index {@link #start()}&nbsp;<tt>-</tt>&nbsp;<tt>1</tt>.
	 * </p>
	 * </li>
	 * <li>
	 * <p>
	 * It appends the given replacement string to the string buffer.
	 * </p>
	 * </li>
	 * <li>
	 * <p>
	 * It sets the append position of this matcher to the index of the last
	 * character matched, plus one, that is, to {@link #end()}.
	 * </p>
	 * </li>
	 * </ol>
	 * <p>
	 * The replacement string may contain references to subsequences captured
	 * during the previous match: Each occurrence of <tt>${</tt><i>name</i>
	 * <tt>}</tt> or <tt>$</tt><i>g</i> will be replaced by the result of
	 * evaluating the corresponding {@link #group(String) group(name)} or
	 * {@link #group(int) group(g)} respectively. For <tt>$</tt><i>g</i>, the
	 * first number after the <tt>$</tt> is always treated as part of the group
	 * reference. Subsequent numbers are incorporated into g if they would form
	 * a legal group reference. Only the numerals '0' through '9' are
	 * considered as potential components of the group reference. If the second
	 * group matched the string <tt>"foo"</tt>, for example, then passing the
	 * replacement string <tt>"$2bar"</tt> would cause <tt>"foobar"</tt> to be
	 * appended to the string buffer. A dollar sign (<tt>$</tt>) may be
	 * included as a literal in the replacement string by preceding it with a
	 * backslash (<tt>\$</tt>).
	 * <p>
	 * Note that backslashes (<tt>\</tt>) and dollar signs (<tt>$</tt>) in the
	 * replacement string may cause the results to be different than if it were
	 * being treated as a literal replacement string. Dollar signs may be
	 * treated as references to captured subsequences as described above, and
	 * backslashes are used to escape literal characters in the replacement
	 * string.
	 * <p>
	 * This method is intended to be used in a loop together with the
	 * {@link #appendTail appendTail} and {@link #find find} methods. The
	 * following code, for example, writes <tt>one dog two dogs in the
	 * yard</tt> to the standard-output stream:
	 * </p>
	 * <blockquote>
	 * 
	 * <pre>
	 * Pattern p = Pattern.compile(&quot;cat&quot;);
	 * Matcher m = p.matcher(&quot;one cat two cats in the yard&quot;);
	 * StringBuffer sb = new StringBuffer();
	 * while (m.find()) {
	 * 	m.appendReplacement(sb, &quot;dog&quot;);
	 * }
	 * m.appendTail(sb);
	 * System.out.println(sb.toString());
	 * </pre>
	 * 
	 * </blockquote>
	 *
	 * @param sb
	 *        The target string buffer
	 * @param replacement
	 *        The replacement string
	 * @return This matcher
	 * @throws IllegalStateException
	 *         If no match has yet been attempted,
	 *         or if the previous match operation failed
	 * @throws IllegalArgumentException
	 *         If the replacement string refers to a named-capturing
	 *         group that does not exist in the pattern
	 * @throws IndexOutOfBoundsException
	 *         If the replacement string refers to a capturing group
	 *         that does not exist in the pattern
	 */
	public String interpretReplacement(String replacement) {
		// If no match, return error
		if (first < 0) throw new IllegalStateException("No match available");
		// Process substitution string to replace group references with groups
		int cursor = 0;
		StringBuilder result = new StringBuilder();
		while (cursor < replacement.length()) {
			char nextChar = replacement.charAt(cursor);
			if (nextChar == '\\') {
				cursor++;
				if (cursor == replacement.length())
					throw new IllegalArgumentException(
							"character to be escaped is missing");
				nextChar = replacement.charAt(cursor);
				result.append(nextChar);
				cursor++;
			} else if (nextChar == '$') {
				// Skip past $
				cursor++;
				// Throw IAE if this "$" is the last character in
				// replacement
				if (cursor == replacement.length())
					throw new IllegalArgumentException(
							"Illegal group reference: group index is missing");
				nextChar = replacement.charAt(cursor);
				int refNum = -1;
				if (nextChar == '{') {
					cursor++;
					StringBuilder gsb = new StringBuilder();
					while (cursor < replacement.length()) {
						nextChar = replacement.charAt(cursor);
						if (ASCII.isLower(nextChar)
								|| ASCII.isUpper(nextChar)
								|| ASCII.isDigit(nextChar)) {
							gsb.append(nextChar);
							cursor++;
						} else {
							break;
						}
					}
					if (gsb.length() == 0)
						throw new IllegalArgumentException(
								"named capturing group has 0 length name");
					if (nextChar != '}')
						throw new IllegalArgumentException(
								"named capturing group is missing trailing '}'");
					String gname = gsb.toString();
					if (ASCII.isDigit(gname.charAt(0)))
						throw new IllegalArgumentException(
								"capturing group name {"
										+ gname
										+ "} starts with digit character");
					if (!parentPattern.namedGroups().containsKey(gname))
						throw new IllegalArgumentException(
								"No group with name {" + gname + "}");
					refNum = parentPattern.namedGroups().get(gname);
					cursor++;
				} else {
					// The first number is always a group
					refNum = nextChar - '0';
					if ((refNum < 0) || (refNum > 9))
						throw new IllegalArgumentException(
								"Illegal group reference");
					cursor++;
					// Capture the largest legal group string
					boolean done = false;
					while (!done) {
						if (cursor >= replacement.length()) {
							break;
						}
						int nextDigit = replacement.charAt(cursor) - '0';
						if ((nextDigit < 0) || (nextDigit > 9)) { // not a
															// number
							break;
						}
						int newRefNum = (refNum * 10) + nextDigit;
						if (groupCount() < newRefNum) {
							done = true;
						} else {
							refNum = newRefNum;
							cursor++;
						}
					}
				}
				// Append group
				if (start(refNum) != -1 && end(refNum) != -1)
					result.append(text, start(refNum), end(refNum));
			} else {
				result.append(nextChar);
				cursor++;
			}
		}
		return result.toString();
	}
	@Override
	public String toString() {
		return group();
	}
}
