package openjdk.regex;

class CompiledPattern implements java.io.Serializable {
	transient Node root;
	/**
	 * The root of object tree for a match operation. The pattern is matched
	 * at the beginning. This may include a find that uses BnM or a First
	 * node.
	 */
	transient Node matchRoot;
	public GroupRegistry registry;
	public transient int localCount;
	public CompiledPattern(Node root, Node matchRoot, GroupRegistry registry,
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
