package openjdk.regex;

import java.util.HashMap;
import java.util.Map;

class GroupRegistry implements java.io.Serializable {
	/**
	 * Map the "name" of the "named capturing group" to its group id
	 * node.
	 */
	private volatile Map<String, Integer> namedGroups;
	/**
	 * The number of capturing groups in this Pattern. Used by matchers to
	 * allocate storage needed to perform a match.
	 */
	int capturingGroupCount;
	public GroupRegistry() {
		capturingGroupCount = 1;
		namedGroups = new HashMap<>(2);
	}
	public boolean groupDefined(String name) {
		return namedGroups.containsKey(name);
	}
	public void addToEnd(String name) {
		namedGroups.put(name, capturingGroupCount - 1);
	}
	public void clear() {
		capturingGroupCount = 1;
		namedGroups.clear();;
	}
	public int iterateCount() {
		return capturingGroupCount++;
	}
	public int groupNumber(String name) {
		return namedGroups.get(name);
	}
}
