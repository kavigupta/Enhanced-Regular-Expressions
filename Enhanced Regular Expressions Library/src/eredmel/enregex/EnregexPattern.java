package eredmel.enregex;

import java.util.Stack;

import openjdk.regex.Matcher;
import openjdk.regex.PatternSyntaxException;

/**
 * An Enhanced Regex Pattern is basically a regex pattern with two additional
 * factors.
 * <ol>
 * <li>The ability to match parenthesis</li>
 * <li>The ability to match areas in and out of string-like constructs.</li>
 * </ol>
 * <br>
 * Matched parenthesis work as follows: {@code ~<.+~>} matches any region with
 * matched parenthesis (whatever parentheses are defined). This means that any
 * parentheses that aren't in quotes must be balanced between the locations of
 * the {@code ~(} and the {@code ~)}.
 * 
 * <br>
 * 
 * Note that parentheses are not actually matched. To match an actual pair of
 * matched parenthesis, use {@code (~~.+~~).
 * 
 * <br>
 * 
 * Quote contents work as follows. {@code ~'} matches anywhere that is between
 * two single quotes. Escapes can be set so that {@code 'ABC\'DEF'} counts as a
 * quoted region. Quoted regions cannot overlap, so the first one overrides.
 * {@code ~^'} matches anywhere that is not in a quote. If the start and end of
 * a quoted region have different symbols (e.g., {@code []}), both {@code ~[}
 * and {@code ~^]} match an area within square brackets, and both {@code ~^[}
 * and {@code ~]} match an area not in close brackets.
 */
public class EnregexPattern {
	public static EnregexPattern getInstance(String enregex, EnregexType type) {
		Matcher mat = type.tildeMatcher.matcher(enregex);
		int parencount = 0;
		int[] quoteincount = new int[type.quotes.size()];
		int[] quoteoutcount = new int[type.quotes.size()];
		Stack<Integer> opens = new Stack<>();
		StringBuffer repl = new StringBuffer();
		int end = 0;
		tildeMatch: for (; mat.find(); end = mat.end("tilde")) {
			repl.append(enregex.substring(end, mat.start("tilde")));
			if (mat.group("parenopen") != null) {
				opens.add(parencount);
				repl.append(groupNameOpenParen(parencount));
				parencount++;
				continue tildeMatch;
			}
			if (mat.group("parenclose") != null) {
				if (opens.empty())
					throw new PatternSyntaxException(
							"This tilde parenthesis has no matching open",
							enregex, mat.start("parenclose"));
				int openmatch = opens.pop();
				repl.append(groupNameCloseParen(openmatch));
				continue tildeMatch;
			}
			for (int i = 0; i < type.quotes.size(); i++) {
				if (mat.group("quotin" + i) != null) {
					repl.append(groupNameInQuot(i, quoteincount[i]));
					quoteincount[i]++;
					continue tildeMatch;
				}
				if (mat.group("quotout" + i) != null) {
					repl.append(groupNameOutQuot(i, quoteoutcount[i]));
					quoteoutcount[i]++;
					continue tildeMatch;
				}
			}
		}
		if (!opens.empty())
			throw new PatternSyntaxException(
					"This tilde parenthesis has no matching open",
					enregex, opens.pop());
		repl.append(enregex.substring(end));
		// TODO
		return null;
	}
	private static String groupNameOpenParen(int count) {
		return "EREINTuOPARENu" + count;
	}
	private static String groupNameCloseParen(int count) {
		return "EREINTuCPARENu" + count;
	}
	private static String groupNameInQuot(int quoteType, int count) {
		return "EREINTuINQUOTu" + quoteType + "u" + count;
	}
	private static String groupNameOutQuot(int quoteType, int count) {
		return "EREINTuOUTQUOTu" + quoteType + "u" + count;
	}
}
