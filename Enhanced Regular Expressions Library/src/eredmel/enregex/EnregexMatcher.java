package eredmel.enregex;

import java.util.Arrays;

public class EnregexMatcher {
	private final EnregexType type;
	private final int[] parencounts;
	private final int quoteType;
	private final int slashcount;
	public static EnregexMatcher startOfString(EnregexType type) {
		return new EnregexMatcher(type, new int[type.parens.size()], -1, 0);
	}
	public EnregexMatcher next(char next) {
		if (next == '\\') return slash();
		if (quoteType >= 0) {
			SymbolPair pair = type.quotes.get(quoteType);
			if (pair.closeMatches(next, slashcount)) return closeQuote();
			return this;
		}
		for (int i = 0; i < type.quotes.size(); i++) {
			SymbolPair pair = type.quotes.get(i);
			if (pair.openMatches(next, slashcount)) return openQuote(i);
		}
		for (int i = 0; i < type.parens.size(); i++) {
			SymbolPair pair = type.parens.get(i);
			if (pair.openMatches(next, slashcount)) return openParen(i);
			if (pair.closeMatches(next, slashcount)) return closeParen(i);
		}
		return this;
	}
	private EnregexMatcher openParen(int i) {
		int[] paren2 = parencounts.clone();
		paren2[i]++;
		return new EnregexMatcher(type, paren2, quoteType, slashcount);
	}
	private EnregexMatcher closeParen(int i) {
		int[] paren2 = parencounts.clone();
		paren2[i]--;
		return new EnregexMatcher(type, paren2, quoteType, slashcount);
	}
	private EnregexMatcher slash() {
		return new EnregexMatcher(type, parencounts, quoteType, slashcount + 1);
	}
	private EnregexMatcher closeQuote() {
		return new EnregexMatcher(type, parencounts, -1, 0);
	}
	private EnregexMatcher openQuote(int i) {
		return new EnregexMatcher(type, parencounts, i, 0);
	}
	private EnregexMatcher(EnregexType type, int[] parencounts, int quoteType,
			int slashcount) {
		this.type = type;
		this.parencounts = parencounts;
		this.quoteType = quoteType;
		this.slashcount = slashcount;
	}
	public boolean equalParenState(EnregexMatcher other) {
		for (int i = 0; i < parencounts.length; i++)
			if (parencounts[i] != other.parencounts[i]) return false;
		return true;
	}
	public boolean greaterOrEqualParenState(EnregexMatcher other) {
		for (int i = 0; i < parencounts.length; i++)
			if (parencounts[i] < other.parencounts[i]) return false;
		return true;
	}
	public boolean quoteTypeMatches(int quoteType) {
		return this.quoteType == quoteType;
	}
	@Override
	public String toString() {
		return "[" + Arrays.toString(parencounts) + "," + quoteType + "]";
	}
}
