package enregex.matcher;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EREMatcher {
	private Pattern regex;
	private ArrayList<EREBracket> matches;
	private int start;
	private String text;
	public EREMatcher(String enregex, String text) {
		StringBuffer regex = new StringBuffer();
		matches = new ArrayList<>();
		for (int i = 0; i < enregex.length(); i++) {
			if (enregex.charAt(i) == '@') {
				regex.append("(?<").append(encodeStart(matches.size()))
						.append(">)");
				matches.add(EREBracket.OPEN);
			} else if (enregex.charAt(i) == '#') {
				regex.append("(?<").append(encodeEnd(matches.size()))
						.append(">)");
				matches.add(EREBracket.CLOSE);
			} else regex.append(enregex.charAt(i));
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
}
