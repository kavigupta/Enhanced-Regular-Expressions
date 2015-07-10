/*
 * Added.
 */
package eredmel.regex;

/**
 * This class contains the relevant information necessary to produce
 * matches from a regex. The {@code Pattern} class should just point to this one
 * 
 * @author Kavi Gupta
 * 
 */
class CompiledPattern implements java.io.Serializable {
	/**
	 * The "root" of the match operation.
	 */
	transient Node root;
	/**
	 * The root of object tree for a match operation. The pattern is matched
	 * at the beginning. This may include a find that uses BnM or a First
	 * node.
	 */
	transient Node matchRoot;
	/**
	 * The group registry that allows one to look up groups by name.
	 */
	GroupRegistry registry;
	transient int localCount;
	CompiledPattern(Node root, Node matchRoot, GroupRegistry registry,
			int localCount) {
		this.root = root;
		this.matchRoot = matchRoot;
		this.registry = registry;
		this.localCount = localCount;
	}
	private void readObject(java.io.ObjectInputStream s)
			throws java.io.IOException, ClassNotFoundException {
		s.defaultReadObject();
		localCount = 0;
	}
}
