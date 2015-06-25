package enregex.matcher;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EREMatcher {
	private Pattern regex;
	private ArrayList<EREBracket> matches;
	private ArrayList<Boolean> inString;
	private int start;
	private String text;
	public static String processERE(String enregex, String input,
			Function<EREMatch, String> processor) {
		EREMatcher matcher = new EREMatcher(enregex, input);
		int end = 0;
		StringBuffer buff = new StringBuffer();
		EREMatch match;
		while ((match = matcher.find()) != null) {
			buff.append(input.substring(end, match.start()));
			buff.append(processor.apply(match));
			end = match.end();
		}
		return buff.append(input.substring(end)).toString();
	}
	public EREMatcher(String enregex, String text) {
		StringBuffer regex = new StringBuffer();
		matches = new ArrayList<>();
		int slashcount = 0;
		for (int i = 0; i < enregex.length(); i++) {
			if (slashcount % 2 == 0) {
				if (enregex.charAt(i) == '@') {
					regex.append("(?<")
							.append(encodeStart(matches.size()))
							.append(">)");
					matches.add(EREBracket.OPEN);
					continue;
				} else if (enregex.charAt(i) == '#') {
					regex.append("(?<").append(encodeEnd(matches.size()))
							.append(">)");
					matches.add(EREBracket.CLOSE);
					continue;
				} else if (enregex.charAt(i) == '`') {
					regex.append("(?<")
							.append(encodeInStr(matches.size()))
							.append(">)");
					inString.add(true);
				} else if (enregex.charAt(i) == '~') {
					regex.append("(?<")
							.append(encodeOutStr(matches.size()))
							.append(">)");
					inString.add(false);
				}
			}
			if (enregex.charAt(i) == '\\')
				slashcount++;
			else slashcount = 0;
			regex.append(enregex.charAt(i));
		}
		System.out.println(regex);
		this.regex = Pattern.compile(regex.toString());
		this.start = 0;
		this.text = text;
	}
	public EREMatch find() {
		return find(text.substring(start));
	}
	private EREMatch find(String in) {
		Matcher mat = regex.matcher(in);
		if (!mat.find()) return null;
		System.out.println(mat);
		boolean realMatch = true;
		for (int i = 0; i < matches.size(); i++) {
			if (matches.get(i) == EREBracket.CLOSE) continue;
			int pos1 = mat.start(encodeStart(i));
			int j = EREUtil.matchingEREB(matches, i);
			if (j == -1
					|| EREUtil.parensMatch(in.substring(pos1,
							mat.start(encodeEnd(j))))) continue;
			realMatch = false;
			break;
		}
		System.out.println(realMatch);
		if (realMatch) {
			EREMatch match = new EREMatch(start, mat);
			start = match.end();
			return match;
		}
		if (mat.start() == mat.end()) return null;
		return find(in.substring(mat.start(), mat.end() - 1));
	}
	private String encodeStart(int i) {
		return "EREMatcheroINTERNALoSTART" + i;
	}
	private String encodeEnd(int i) {
		return "EREMatcheroINTERNALoEND" + i;
	}
	private String encodeOutStr(int i) {
		return "EREMatcheroINTERNALoINSTR" + i;
	}
	private String encodeInStr(int i) {
		return "EREMatcheroINTERNALoOUTSTR" + i;
	}
}
