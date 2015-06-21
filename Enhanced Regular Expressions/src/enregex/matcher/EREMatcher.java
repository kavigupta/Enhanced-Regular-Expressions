package enregex.matcher;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EREMatcher {
	private static final Pattern PAREN_COUNT = Pattern
			.compile("(?<=\\)||\\()#(?<index>\\d+)#");
	private Pattern regex;
	private ArrayList<Integer> constraints;
	private int start;
	private String text;
	public EREMatcher(String enregex, String text) {
		Matcher parenCounts = PAREN_COUNT.matcher(enregex);
		StringBuffer regex = new StringBuffer();
		constraints = new ArrayList<>();
		int end = 0;
		while (parenCounts.find()) {
			System.out.println(parenCounts);
			regex.append(enregex.substring(end, parenCounts.start()))
					.append("(?<").append(encode(constraints.size()))
					.append(">)");
			constraints.add(Integer.parseInt(parenCounts.group("index")));
			end = parenCounts.end();
		}
		regex.append(enregex.substring(end));
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
		boolean realMatch = true;
		for (int i = 0; i < (constraints.size() + 1) / 2; i++) {
			int pos1 = mat.start(encode(i)) - 1;
			int pos2 = mat.start(encode(constraints.size() - 1 - i));
			System.out.println(in + "\t" + pos1 + "\t" + pos2);
			System.out.println(in.substring(pos1, pos2));
			if (EREUtil.parensMatch(in.substring(pos1, pos2))) continue;
			realMatch = false;
			break;
		}
		System.out.println(realMatch);
		if (realMatch) {
			EREMatch match = new EREMatch(start, mat);
			start = match.end();
			System.out.println(start);
			return match;
		}
		if (mat.start() == mat.end()) return null;
		return find(in.substring(mat.start(), mat.end() - 1));
	}
	private String encode(int i) {
		return "X" + i;
	}
}
