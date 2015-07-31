package eredmel.preprocessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eredmel.regex.Matcher;
import eredmel.regex.Pattern;

public class EredmelPreprocessor {
	private static final Pattern TABWIDTH = Pattern
			.compile("\\s*##\\s*tabwidth\\s*=\\s*(?<tabwidth>\\d+)");
	private static final Pattern TABBED_LINE = Pattern
			.compile("(?<indt>[\t ]*)(?<rest>.*)");
	private static final int DEFAULT_TAB_WIDTH = 4;
	public static String preprocess(String input) {
		// TODO auto-generated method stub
		return null;
	}
	public static List<String> normalize(List<String> toNormalize) {
		List<String> norm = stripOpeningEmptyLines(toNormalize);
		int tabwidth = processTabwidthHeader(norm);
		List<Integer> tabNumbers = new ArrayList<>();
		List<Integer> spaceNumbers = new ArrayList<>();
		for (int j = 0; j < norm.size(); j++) {
			String line = norm.get(j);
			Matcher mat = TABBED_LINE.matcher(line);
			mat.find();
			norm.set(j, mat.group("rest"));
			int spaces = 0;
			String indt = mat.group("indt");
			for (char c : indt.toCharArray())
				if (c == ' ') spaces++;
			tabNumbers.add(indt.length() - spaces);
			spaceNumbers.add(spaces);
		}
		if (tabwidth < 0) {
			int gcf = 0;
			for (int spaces : spaceNumbers)
				gcf = gcf(gcf, spaces);
			if (gcf == 0) {
				// no spaces. Set to default and continue
				tabwidth = DEFAULT_TAB_WIDTH;
			} else {
				// take a guess
				tabwidth = gcf;
				// TODO emit warning here
			}
		}
		for (int j = 0; j < norm.size(); j++) {
			int tabs = spaceNumbers.get(j) / tabwidth + tabNumbers.get(j);
			char[] tabsc = new char[tabs];
			Arrays.fill(tabsc, '\t');
			norm.set(j, new String(tabsc) + norm.get(j));
		}
		return norm;
	}
	private static int processTabwidthHeader(List<String> norm) {
		int tabwidth = -1;
		Matcher mat = TABWIDTH.matcher(norm.get(0));
		if (mat.find()) {
			norm.remove(0);
			tabwidth = Integer.parseInt(mat.group("tabwidth"));
		}
		return tabwidth;
	}
	private static List<String> stripOpeningEmptyLines(List<String> toNormalize) {
		int i = 0;
		while (i < toNormalize.size()
				&& toNormalize.get(i).trim().length() == 0)
			i++;
		return new ArrayList<String>(toNormalize.subList(i,
				toNormalize.size()));
	}
	public static int gcf(int a, int b) {
		a = Math.abs(a);
		b = Math.abs(b);
		int t;
		while (b != 0) {
			t = b;
			b = a % b;
			a = t;
		}
		return a;
	}
}
