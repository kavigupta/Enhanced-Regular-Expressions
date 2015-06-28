package eredmel.enregex;

import java.util.Arrays;

public class EnregexMetadata {
	private final EnregexType type;
	private final int[] parencounts;
	private final int quoteType;
	private final int slashcount;
	public static EnregexMetadata startOfString(EnregexType type) {
		return new EnregexMetadata(type, new int[type.parens.size()], -1, 0);
	}
	public EnregexMetadata next(char next) {
		System.out.println(this + "\t" + next);
		if (next == '\\') return slash();
		if (quoteType >= 0) {
			SymbolPair pair = type.quotes.get(quoteType);
			if (pair.closeMatches(next, slashcount)) return closeQuote();
			return unslash();
		}
		for (int i = 0; i < type.quotes.size(); i++) {
			SymbolPair pair = type.quotes.get(i);
			if (pair.openMatches(next, slashcount)) return openQuote(i);
		}
		for (int i = 0; i < type.parens.size(); i++) {
			SymbolPair pair = type.parens.get(i);
			System.out.println(pair);
			if (pair.openMatches(next, slashcount)) return openParen(i);
			if (pair.closeMatches(next, slashcount)) return closeParen(i);
		}
		return unslash();
	}
	private EnregexMetadata unslash() {
		return new EnregexMetadata(type, parencounts, quoteType, 0);
	}
	private EnregexMetadata openParen(int i) {
		int[] paren2 = parencounts.clone();
		paren2[i]++;
		return new EnregexMetadata(type, paren2, quoteType, 0);
	}
	private EnregexMetadata closeParen(int i) {
		int[] paren2 = parencounts.clone();
		paren2[i]--;
		return new EnregexMetadata(type, paren2, quoteType, 0);
	}
	private EnregexMetadata slash() {
		return new EnregexMetadata(type, parencounts, quoteType,
				slashcount + 1);
	}
	private EnregexMetadata closeQuote() {
		return new EnregexMetadata(type, parencounts, -1, 0);
	}
	private EnregexMetadata openQuote(int i) {
		return new EnregexMetadata(type, parencounts, i, 0);
	}
	private EnregexMetadata(EnregexType type, int[] parencounts,
			int quoteType, int slashcount) {
		this.type = type;
		this.parencounts = parencounts;
		this.quoteType = quoteType;
		this.slashcount = slashcount;
	}
	public boolean equalParenState(EnregexMetadata other) {
		for (int i = 0; i < parencounts.length; i++)
			if (parencounts[i] != other.parencounts[i]) return false;
		return true;
	}
	public boolean greaterOrEqualParenState(EnregexMetadata other) {
		for (int i = 0; i < parencounts.length; i++)
			if (parencounts[i] < other.parencounts[i]) return false;
		return true;
	}
	public boolean quoteTypeMatches(int quoteType) {
		return this.quoteType == quoteType;
	}
	@Override
	public String toString() {
		return "[" + Arrays.toString(parencounts) + "," + quoteType + ","
				+ slashcount + "]";
	}
}
