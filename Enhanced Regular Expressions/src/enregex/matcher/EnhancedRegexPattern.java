package enregex.matcher;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eredmel.enregex.EREMatch;
import eredmel.enregex.ERESegment;

public final class EnhancedRegexPattern {
	public Pattern regex;
	public ArrayList<EREBracket> matches;
	public ArrayList<Boolean> inString;
	public EnhancedRegexPattern(String enregex) {
		StringBuffer regex = new StringBuffer();
		matches = new ArrayList<>();
		int slashcount = 0;
		for (int i = 0; i < enregex.length(); i++) {
			if (slashcount % 2 == 0) {
				switch (enregex.charAt(i)) {
					case '@':
						regex.append("(?<")
								.append(encodeStart(matches.size()))
								.append(">)");
						matches.add(EREBracket.OPEN);
						continue;
					case '#':
						regex.append("(?<")
								.append(encodeEnd(matches.size()))
								.append(">)");
						matches.add(EREBracket.CLOSE);
						continue;
					case '`':
						regex.append("(?<")
								.append(encodeInStr(matches.size()))
								.append(">)");
						inString.add(true);
						continue;
					case '~':
						regex.append("(?<")
								.append(encodeOutStr(matches.size()))
								.append(">)");
						inString.add(false);
						continue;
				}
			}
			if (enregex.charAt(i) == '\\')
				slashcount++;
			else slashcount = 0;
			regex.append(enregex.charAt(i));
		}
		System.out.println(regex);
		this.regex = Pattern.compile(regex.toString());
	}
	public void process(ERESegment segment,
			Function<EREMatch, String> matchConsumer) {
		Matcher mat = regex.matcher(segment);
		if (!mat.find()) return;
	}
	private static String encodeStart(int i) {
		return "EREMatcheroINTERNALoSTART" + i;
	}
	private static String encodeEnd(int i) {
		return "EREMatcheroINTERNALoEND" + i;
	}
	private static String encodeOutStr(int i) {
		return "EREMatcheroINTERNALoINSTR" + i;
	}
	private static String encodeInStr(int i) {
		return "EREMatcheroINTERNALoOUTSTR" + i;
	}
}
