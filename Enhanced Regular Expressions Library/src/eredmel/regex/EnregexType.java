package eredmel.regex;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EnregexType implements java.io.Serializable {
	public static final EnregexType EREDMEL_STANDARD = new EnregexType(
			Arrays.asList(new SymbolPair('(', ')', false, false),
					new SymbolPair('[', ']', false, false),
					new SymbolPair('<', '>', false, false),
					new SymbolPair('{', '}', false, false)),
			Arrays.asList(new SymbolPair('\'', '\'', false, true)));
	public final List<SymbolPair> parens;
	public final List<SymbolPair> quotes;
	public EnregexType(List<SymbolPair> parens, List<SymbolPair> quotes) {
		this.parens = parens;
		this.quotes = quotes;
	}
	static enum EnregexSymbol {
		OPEN_PAREN, CLOSE_PAREN, OPEN_QUOTE, CLOSE_QUOTE, CARET, ERROR;
	}
	EnregexSymbol classify(int c) {
		if (c == '^') return EnregexSymbol.CARET;
		for (SymbolPair ch : parens) {
			Logger.getGlobal().log(Level.FINE, ch + "\t" + (char) c);
			if (ch.open == c) return EnregexSymbol.OPEN_PAREN;
			if (ch.close == c) return EnregexSymbol.CLOSE_PAREN;
		}
		for (SymbolPair ch : quotes) {
			if (ch.open == c) return EnregexSymbol.OPEN_QUOTE;
			if (ch.close == c) return EnregexSymbol.CLOSE_QUOTE;
		}
		return EnregexSymbol.ERROR;
	}
	int matching(int q) {
		for (SymbolPair ch : parens) {
			if (ch.open == q) return ch.close;
			if (ch.close == q) return ch.open;
		}
		for (SymbolPair ch : quotes) {
			if (ch.open == q) return ch.close;
			if (ch.close == q) return ch.open;
		}
		return q;
	}
	int parenType(int parenClose) {
		for (int i = 0; i < parens.size(); i++)
			if (parens.get(i).close == parenClose) return i;
		return -1;
	}
	int quoteType(int quoteOpen) {
		for (int i = 0; i < quotes.size(); i++)
			if (quotes.get(i).open == quoteOpen) return i;
		return -1;
	}
}
