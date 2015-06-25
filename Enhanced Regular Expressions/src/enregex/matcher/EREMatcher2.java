package enregex.matcher;

import java.util.function.Function;
import java.util.regex.Matcher;

public class EREMatcher2 {
	public static String processERE(String enregex, String input,
			Function<EREMatch, String> processor) {
		EREMatcher2 matcher = new EREMatcher2(enregex, input);
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
	private EREMatch find(EnhancedRegexPattern enregex, ERESegment seg) {
		Matcher mat = enregex.regex.matcher(seg);
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
}
