/*
 * Refactored out of the openjdk.regex.Matcher class.
 */
package eredmel.regex;

import java.util.HashMap;
import java.util.Map;

/**
 * A registry containing a relational database between group names and ids.
 */
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
	GroupRegistry() {
		capturingGroupCount = 1;
		namedGroups = new HashMap<>(2);
	}
	/**
	 * @return if the given group is defined
	 */
	boolean groupDefined(String name) {
		return namedGroups.containsKey(name);
	}
	/**
	 * Adds the given group name to the end
	 */
	void addToEnd(String name) {
		namedGroups.put(name, capturingGroupCount - 1);
	}
	/**
	 * Clears this registry
	 */
	void clear() {
		capturingGroupCount = 1;
		namedGroups.clear();;
	}
	/**
	 * Add one to the capturing group count
	 */
	int iterateCount() {
		return capturingGroupCount++;
	}
	/**
	 * The group id with the given name
	 */
	int groupNumber(String name) {
		return namedGroups.get(name);
	}
}
