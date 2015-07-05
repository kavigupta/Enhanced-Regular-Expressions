package eredmel.statement;

import java.util.function.Function;

import eredmel.enregex.EnregexMatch;
import eredmel.enregex.EnregexPattern;
import eredmel.enregex.EnregexType;

public class Replacer implements Function<String, String> {
	public static Replacer getInstance(String enregex, String replacement) {
		return new Replacer(EnregexPattern.compile(enregex,
				EnregexType.EREDMEL), replacement);
	}
	private final EnregexPattern enregex;
	private final String replacement;
	private Replacer(EnregexPattern enregex, String replacement) {
		this.enregex = enregex;
		this.replacement = replacement;
	}
	@Override
	public String apply(String toMatch) {
		EnregexMatch match = enregex.firstMatch(toMatch);
		if (match == null) return toMatch;
		return apply(toMatch.substring(0, match.start())
				+ match.replace(replacement)
				+ toMatch.substring(match.end()));
	}
}
