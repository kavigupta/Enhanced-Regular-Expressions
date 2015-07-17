package eredmel.regex;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EnregexMetadata {
	private final EnregexType type;
	private final int[] parencounts;
	private final int quoteType;
	private final int slashcount;
	public static EnregexMetadata startOfString(EnregexType type) {
		return new EnregexMetadata(type, new int[type.parens.size()], -1, 0);
	}
	public EnregexMetadata next(char next) {
		Logger.getGlobal().log(Level.FINE, this + "\t" + next);
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
			Logger.getGlobal().log(Level.FINE, pair.toString());
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
	public boolean equalParenState(EnregexMetadata other, int parenClose) {
		int parenType = type.parenType(parenClose);
		return parencounts[parenType] == other.parencounts[parenType];
	}
	public boolean greaterOrEqualParenState(EnregexMetadata other,
			int parenClose) {
		int parenType = type.parenType(parenClose);
		return parencounts[parenType] >= other.parencounts[parenType];
	}
	public boolean quoteTypeMatches(boolean positive, int quoteOpen) {
		int quoteType = type.quoteType(quoteOpen);
		if (positive)
			return this.quoteType == quoteType;
		else return this.quoteType != quoteType;
	}
	@Override
	public String toString() {
		return "[" + Arrays.toString(parencounts) + "," + quoteType + ","
				+ slashcount + "]";
	}
}
