package eredmel.regex;

import java.util.HashMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The State of an Enregex matcher. This will contain all the open matched paren
 * assertions in existence.
 * 
 * @author Kavi Gupta
 *
 */
class EnregexSystem {
	/**
	 * A map mapping close parenthesis characters to the locations at which
	 * open assertions of matched parenthesis take place in the matcher string.
	 */
	private final HashMap<Integer, TreeSet<Integer>> openParenthesis;
	private final EnregexSegment matchingString;
	final EnregexType type;
	/**
	 * Creates an EnregexSystem with the given matching string suppplier and
	 * quote system.
	 */
	EnregexSystem(CharSequence text, EnregexType type) {
		this.openParenthesis = new HashMap<>();
		this.matchingString = EnregexSegment.getInstance(text, type);
		this.type = type;
	}
	private TreeSet<Integer> set(int close) {
		if (!openParenthesis.containsKey(close))
			openParenthesis.put(close, new TreeSet<>());
		return openParenthesis.get(close);
	}
	/**
	 * Adds a parenthesis to the record, along with a location in the string
	 * for the parenthesis.
	 */
	void addParen(int open, int location) {
		int close = type.matching(open);
		set(close).add(location);
	}
	int popParen(int close) {
		return set(close).pollLast();
	}
	boolean parenMatches(int close, int location) {
		Logger.getGlobal().log(Level.FINE, (char) close + "\t" + location);
		TreeSet<Integer> open = openParenthesis.get(close);
		if (open == null) return false;
		if (open.size() == 0) return false;
		return matchingString.parensMatch(open.last(), location, close);
	}
	boolean quoteMatches(int openQuote, boolean positive, int loc) {
		return matchingString.quoteTypeMatches(loc, positive, openQuote);
	}
	@Override
	public String toString() {
		return "EnregexSystem [openParenthesis=" + openParenthesis
				+ ", matchingString=" + matchingString + ", type=" + type
				+ "]";
	}
}
