package eredmel.enregex;

public class EREMetadata {
	private final EnregexType type;
	private final int[] parencounts;
	private final int quoteType;
	private final int slashcount;
	public static EREMetadata startOfString(EnregexType type) {
		return new EREMetadata(type, new int[type.parens.size()], -1, 0);
	}
	public EREMetadata next(char next) {
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
			if (pair.openMatches(next, slashcount)) return closeParen(i);
		}
		return this;
	}
	private EREMetadata openParen(int i) {
		int[] paren2 = parencounts.clone();
		paren2[i]++;
		return new EREMetadata(type, paren2, quoteType, slashcount);
	}
	private EREMetadata closeParen(int i) {
		int[] paren2 = parencounts.clone();
		paren2[i]--;
		return new EREMetadata(type, paren2, quoteType, slashcount);
	}
	private EREMetadata slash() {
		return new EREMetadata(type, parencounts, quoteType, slashcount + 1);
	}
	private EREMetadata closeQuote() {
		return new EREMetadata(type, parencounts, -1, 0);
	}
	private EREMetadata openQuote(int i) {
		return new EREMetadata(type, parencounts, i, 0);
	}
	private EREMetadata(EnregexType type, int[] parencounts, int quoteType,
			int slashcount) {
		this.type = type;
		this.parencounts = parencounts;
		this.quoteType = quoteType;
		this.slashcount = slashcount;
	}
	public boolean equalParenState(EREMetadata other) {
		for (int i = 0; i < parencounts.length; i++)
			if (parencounts[i] != other.parencounts[i]) return false;
		return true;
	}
	public boolean greaterOrEqualParenState(EREMetadata other) {
		for (int i = 0; i < parencounts.length; i++)
			if (parencounts[i] < other.parencounts[i]) return false;
		return true;
	}
	public boolean quoteTypeMatches(int quoteType) {
		return this.quoteType == quoteType;
	}
}
