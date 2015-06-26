package eredmel.enregex;

import java.util.List;

import openjdk.regex.Pattern;

public class EnregexType {
	private static final String TILDE_PAREN = "(?<!\\\\\\\\)(\\\\\\\\\\\\\\\\)*(?<tilde>~((?<parenopen><)|(?<parenclose>>)%s))";
	public final List<SymbolPair> parens;
	public final List<SymbolPair> quotes;
	public final Pattern tildeMatcher;
	public EnregexType(List<SymbolPair> parens, List<SymbolPair> quotes) {
		this.parens = parens;
		this.quotes = quotes;
		tildeMatcher = produceTildeMatcher(quotes);
	}
	private static Pattern produceTildeMatcher(List<SymbolPair> quotes) {
		StringBuffer quoteGroups = new StringBuffer();
		for (int i = 0; i < quotes.size(); i++) {
			SymbolPair pair = quotes.get(i);
			quoteGroups.append("|(?<quotin").append(i).append(">")
					.append(openRegex(pair)).append(")");
			quoteGroups.append("|(?<quotout").append(i).append(">")
					.append(closeRegex(pair)).append(")");
		}
		return Pattern.compile(String.format(TILDE_PAREN, quoteGroups));
	}
	private static String openRegex(SymbolPair pair) {
		if (pair.same()) return quote(pair.open);
		return quote(pair.open) + "|(^" + quote(pair.close) + ")";
	}
	private static String closeRegex(SymbolPair pair) {
		if (pair.same()) return quote(pair.close);
		return quote(pair.close) + "|(^" + quote(pair.open) + ")";
	}
	private static String quote(char open) {
		return Pattern.quote(Character.toString(open));
	}
}
