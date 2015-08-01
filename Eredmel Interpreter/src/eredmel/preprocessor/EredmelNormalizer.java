package eredmel.preprocessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eredmel.regex.Matcher;
import eredmel.regex.Pattern;
import eredmel.utils.math.MathUtils;

/**
 * This utility class contains methods that allow for the normalization of
 * Eredmel source files.
 * 
 * @author Kavi Gupta
 */
public final class EredmelNormalizer {
	private EredmelNormalizer() {}
	/**
	 * Matches a tabwidth statement at the top of the document, which has the
	 * form {@code ##tabwidth = <number>}, with any number of spaces
	 * permissible between segments
	 */
	private static final Pattern TABWIDTH = Pattern
			.compile("\\s*##\\s*tabwidth\\s*=\\s*(?<tabwidth>\\d+)");
	/**
	 * Matches a tabbed line, which contains any number of tabs and spaces and
	 * text at the end
	 */
	private static final Pattern TABBED_LINE = Pattern
			.compile("(?<indt>[\t ]*)(?<rest>.*)");
	/**
	 * Normalizes a given Eredmel file, represented as a list of lines.
	 * 
	 * Normalization consists of stripping out opening empty lines (or lines
	 * containing only whitespace), interpreting a tabwidth statement (which
	 * takes the form {@code ##tabwidth = <number>}.
	 * 
	 * The normalizer then goes through each line and counts the number of tabs
	 * and spaces which it contains. If tabwidth was not defined, it tries to
	 * infer it by taking the Greatest Common Factor of the number of spaces in
	 * each line. Note that this is not always accurate and will emit an error.
	 * 
	 * The normalizer then replaces the {@code n} spaces at the beginning of
	 * each line with {@code round(n/t)} spaces, where {@code t} is the
	 * tabwidth.
	 * 
	 * @param toNormalize
	 *        The text to normalize, in the form of a list of lines
	 * @return The normalized document, in the form of a list of lines
	 */
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
				gcf = MathUtils.gcf(gcf, spaces);
			if (gcf == 0) {
				// no spaces. Set to 1 to prevent divide by 0 errors and
				// continue
				tabwidth = 1;
			} else {
				// take a guess
				tabwidth = gcf;
				// TODO emit warning here
			}
		}
		for (int j = 0; j < norm.size(); j++) {
			int tabs = (spaceNumbers.get(j) + tabwidth / 2) / tabwidth
					+ tabNumbers.get(j);
			char[] tabsc = new char[tabs];
			Arrays.fill(tabsc, '\t');
			norm.set(j, new String(tabsc) + norm.get(j));
		}
		return norm;
	}
	/**
	 * Gets the tabwidth and consumes a tabwidth statement
	 * 
	 * @param norm
	 *        the pre-normalzied edmh file, which may or may not contain a
	 *        tabwidth statement to begin with
	 * @return if the file begins with a tabwidth statement, it strips it and
	 *         returns the specified width. Otherwise, it returns {@code -1}
	 */
	private static int processTabwidthHeader(List<String> norm) {
		int tabwidth = -1;
		Matcher mat = TABWIDTH.matcher(norm.get(0));
		if (mat.find()) {
			norm.remove(0);
			tabwidth = Integer.parseInt(mat.group("tabwidth"));
		}
		return tabwidth;
	}
	/**
	 * @param toNormalize
	 * @return a clone of {@code toNormalize} without opening empty lines
	 */
	private static List<String> stripOpeningEmptyLines(List<String> toNormalize) {
		int i = 0;
		while (i < toNormalize.size()
				&& toNormalize.get(i).trim().length() == 0)
			i++;
		return new ArrayList<>(toNormalize.subList(i, toNormalize.size()));
	}
}
