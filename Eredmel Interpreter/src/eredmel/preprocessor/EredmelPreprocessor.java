package eredmel.preprocessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import eredmel.collections.Pair;
import eredmel.regex.Matcher;
import eredmel.regex.Pattern;
import eredmel.utils.math.MathUtils;

/**
 * This utility class contains methods that allow for the normalization and
 * preprocessing of Eredmel source files.
 * 
 * @author Kavi Gupta
 */
public final class EredmelPreprocessor {
	private EredmelPreprocessor() {}
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
	 * @return The normalized document, in the form of a list of lines. The
	 *         tabwidth returned is either one explicitly declared, implicitly
	 *         calculated, or (when there are no declaring spaces), 4.
	 *         (normalizedDocument, tabwidth)
	 */
	public static Pair<List<String>, Integer> normalize(
			List<String> toNormalize) {
		Pair<List<String>, Optional<Integer>> twProc = processTabwidth(toNormalize);
		// (restOfLine, (spaces, tabs))
		List<Pair<String, Pair<Integer, Integer>>> countedStart = countWhitespace(twProc.key);
		int tabwidth;
		if (!twProc.value.isPresent()) {
			int gcf = 0;
			for (Pair<String, Pair<Integer, Integer>> line : countedStart)
				gcf = MathUtils.gcf(gcf, line.value.key);
			if (gcf == 0) {
				// no spaces. Set to 4 to prevent divide by 0 errors and
				// allow for standard conversion to spaces
				tabwidth = 4;
			} else {
				// take a guess
				tabwidth = gcf;
				// TODO emit warning here
			}
		} else {
			tabwidth = twProc.value.get();
		}
		List<String> normalized = new ArrayList<>(countedStart.size());
		for (Pair<String, Pair<Integer, Integer>> line : countedStart) {
			int tabs = (line.value.key + tabwidth / 2) / tabwidth
					+ line.value.value;
			char[] tabsc = new char[tabs];
			Arrays.fill(tabsc, '\t');
			normalized.add(new String(tabsc) + line.key);
		}
		return Pair.getInstance(normalized, tabwidth);
	}
	/**
	 * 
	 * Converts tabs to spaces at the beginning of lines. This method is not
	 * used by the preprocessor, but may be run at the end of a preprocessor
	 * cycle to repatriate spaces into a non-Eredmel source file.
	 * 
	 * @param lines
	 *        the file lines
	 * @param tabwidth
	 *        the number of spaces to use in replacing each tab
	 * @return
	 *         the modified lines; the original list is not modified.
	 */
	public static List<String> leadingTabsToSpaces(List<String> lines,
			int tabwidth) {
		char[] spacesC = new char[tabwidth];
		Arrays.fill(spacesC, ' ');
		String spaces = new String(spacesC);
		return lines
				.stream()
				.map(TABBED_LINE::matcher)
				.map(mat -> {
					mat.find(); // assumes find successful
					return mat.group("indt").replace("\t", spaces)
							+ mat.group("rest");
				}).collect(Collectors.toList());
	}
	private static List<Pair<String, Pair<Integer, Integer>>> countWhitespace(
			List<String> norm) {
		List<Pair<String, Pair<Integer, Integer>>> countedStart = new ArrayList<>();
		for (String line : norm) {
			Matcher mat = TABBED_LINE.matcher(line);
			mat.find();
			int spaces = 0;
			String indt = mat.group("indt");
			for (char c : indt.toCharArray())
				if (c == ' ') spaces++;
			countedStart.add(Pair.getInstance(mat.group("rest"),
					Pair.getInstance(spaces, indt.length() - spaces)));
		}
		return countedStart;
	}
	/**
	 * Gets the tabwidth and consumes a tabwidth statement
	 * 
	 * @param original
	 *        the pre-normalzied edmh file, which may or may not contain a
	 *        tabwidth statement to begin with
	 * @return if the file begins with a tabwidth statement, it strips it and
	 *         returns the specified Optional.of(width). Otherwise, it returns
	 *         Optional.none()
	 */
	private static Pair<List<String>, Optional<Integer>> processTabwidth(
			List<String> original) {
		int i = 0;
		while (i < original.size() && original.get(i).trim().length() == 0)
			i++;
		ArrayList<String> stripped = new ArrayList<>(original.subList(i,
				original.size()));
		if (stripped.size() == 0)
			return Pair.getInstance(stripped, Optional.empty());
		Matcher mat = TABWIDTH.matcher(stripped.get(0));
		if (!mat.find()) return Pair.getInstance(stripped, Optional.empty());
		stripped.remove(0);
		return Pair.getInstance(stripped,
				Optional.of(Integer.parseInt(mat.group("tabwidth"))));
	}
}
