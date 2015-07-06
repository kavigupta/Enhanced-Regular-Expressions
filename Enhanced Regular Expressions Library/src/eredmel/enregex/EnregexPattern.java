package eredmel.enregex;

import java.util.ArrayList;
import java.util.Stack;
import java.util.function.Function;

import openjdk.regex.Matcher;
import openjdk.regex.Pattern;
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
	public static EnregexPattern compile(String enregex, EnregexType type) {
		Matcher mat = type.tildeMatcher.matcher(enregex);
		System.out.println("tildeMatcher " + type.tildeMatcher);
		System.out.println(enregex);
		int parencount = 0;
		int[] quoteincount = new int[type.quotes.size()];
		int generaloutcount = 0;
		int[] quoteoutcount = new int[type.quotes.size()];
		Stack<Integer> opens = new Stack<>();
		StringBuffer repl = new StringBuffer();
		int end = 0;
		tildeMatch: for (; mat.find(); end = mat.end("tilde")) {
			System.out.println("Group: " + mat.group());
			repl.append(enregex.substring(end, mat.start("tilde")));
			if (mat.group("parenopen") != null) {
				opens.add(parencount);
				repl.append(group(groupNameOpenParen(parencount)));
				parencount++;
				continue tildeMatch;
			}
			if (mat.group("parenclose") != null) {
				if (opens.empty())
					throw new PatternSyntaxException(
							"This tilde parenthesis has no matching open",
							enregex, mat.start("parenclose"));
				int openmatch = opens.pop();
				repl.append(group(groupNameCloseParen(openmatch)));
				continue tildeMatch;
			}
			if (mat.group("quotout") != null) {
				repl.append(group(groupNameGeneralOut(generaloutcount)));
				generaloutcount++;
				continue tildeMatch;
			}
			for (int i = 0; i < type.quotes.size(); i++) {
				if (mat.group("quotin" + i) != null) {
					repl.append(group(groupNameInQuot(i, quoteincount[i])));
					quoteincount[i]++;
					continue tildeMatch;
				}
				if (mat.group("quotout" + i) != null) {
					repl.append(group(groupNameOutQuot(i, quoteoutcount[i])));
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
		System.out.println(repl);
		return new EnregexPattern(Pattern.compile(repl.toString()), type,
				parencount, quoteincount, quoteoutcount, generaloutcount);
	}
	private final Pattern regex;
	private final EnregexType type;
	private final int parencount;
	private final int[] quotInCount, quotOutCount;
	private final int generalOutCount;
	public EnregexPattern(Pattern regex, EnregexType type, int parencount,
			int[] quotInCount, int[] quotOutCount, int generalOutCount) {
		this.regex = regex;
		this.type = type;
		this.parencount = parencount;
		this.quotInCount = quotInCount;
		this.quotOutCount = quotOutCount;
		this.generalOutCount = generalOutCount;
	}
	public String process(String str,
			Function<EnregexMatch, String> matchConsumer) {
		int end = 0;
		StringBuffer buff = new StringBuffer();
		for (EnregexMatch match : process(str)) {
			buff.append(str.substring(end, match.start()));
			buff.append(matchConsumer.apply(match));
		}
		return buff.append(str.substring(end)).toString();
	}
	public ArrayList<EnregexMatch> process(String str) {
		EnregexSegment segment = EnregexSegment.getInstance(str, type);
		ArrayList<EnregexMatch> matches = new ArrayList<EnregexMatch>();
		int end = 0;
		while (end >= 0) {
			end = process(end, segment.subSequence(end, segment.length()),
					matches, true);
		}
		return matches;
	}
	private int process(int offset, EnregexSegment segment,
			ArrayList<EnregexMatch> matches, boolean start) {
		System.out.println(offset + "\t" + segment);
		Matcher mat = regex.matcher(segment);
		if (!mat.find()) return start ? -1 : offset + 1;
		if (actualMatch(mat, segment)) {
			matches.add(new EnregexMatch(offset, mat));
			return mat.end() + offset;
		}
		return process(offset + mat.start(),
				segment.subSequence(mat.start(), segment.length() - 1),
				matches, false);
	}
	private boolean actualMatch(Matcher mat, EnregexSegment segment) {
		for (int i = 0; i < parencount; i++) {
			int openloc = mat.start(groupNameOpenParen(i));
			int closeloc = mat.start(groupNameCloseParen(i));
			if (openloc == -1 && closeloc == -1) continue;
			if (openloc == -1 || closeloc == -1) return false;
			System.out.println("Paseed starting");
			if (!segment.parensMatch(openloc, closeloc)) return false;
		}
		System.out.println("Passed Paren Test");
		for (int quoteType = 0; quoteType < quotInCount.length; quoteType++) {
			for (int i = 0; i < quotInCount[quoteType]; i++) {
				int loc = mat.start(groupNameInQuot(quoteType, i));
				if (loc == -1) continue;
				if (!segment.quoteTypeMatches(loc, quoteType))
					return false;
			}
		}
		for (int quoteType = 0; quoteType < quotOutCount.length; quoteType++) {
			for (int i = 0; i < quotOutCount[quoteType]; i++) {
				int loc = mat.start(groupNameOutQuot(quoteType, i));
				if (loc == -1) continue;
				if (segment.quoteTypeMatches(loc, quoteType)) return false;
			}
		}
		for (int i = 0; i < generalOutCount; i++) {
			int loc = mat.start(groupNameGeneralOut(i));
			if (loc == -1) continue;
			if (!segment.quoteTypeMatches(loc, -1)) return false;
		}
		return true;
	}
	private static String group(String name) {
		return "(?<" + name + ">)";
	}
	private static String groupNameOpenParen(int count) {
		return "EREINTuOPARENu" + count;
	}
	private static String groupNameCloseParen(int count) {
		return "EREINTuCPARENu" + count;
	}
	private static String groupNameGeneralOut(int count) {
		return "EREINTuNOQUOTu" + count;
	}
	private static String groupNameInQuot(int quoteType, int count) {
		return "EREINTuINQUOTu" + quoteType + "u" + count;
	}
	private static String groupNameOutQuot(int quoteType, int count) {
		return "EREINTuOUTQUOTu" + quoteType + "u" + count;
	}
}
