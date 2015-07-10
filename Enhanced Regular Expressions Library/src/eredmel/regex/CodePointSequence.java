/*
 * Refactored out of the original openjdk.regex.Pattern class.
 */
package eredmel.regex;

import static eredmel.regex.Pattern.*;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * A class for containing an array of codepoints along with a
 * {@code patternLength}
 * 
 * This was primarily refactored out of {@code openjdk.regex.Pattern}
 */
class CodePointSequence {
	/**
	 * A buffer containing codepoints used to parse a given pattern.
	 */
	int[] temp;
	/**
	 * Holds the length of the pattern string.
	 */
	transient int patternLength;
	/**
	 * Since flags can change, a simple lambda expression must be passed to
	 * {@code CodePointSequence} from {@code Pattern}.
	 */
	private final Supplier<Integer> flags;
	/**
	 * The current point in the pattern string of the parsing.
	 */
	int cursor;
	/**
	 * If the Start node might possibly match supplementary characters.
	 * It is set to true during compiling if
	 * (1) There is supplementary char in pattern, or
	 * (2) There is complement node of Category or Block
	 */
	boolean hasSupplementary;
	/**
	 * Constructs a code point sequence a pattern string and flag provision.
	 * 
	 * @param pattern
	 *        the pattern string to be used
	 * @param flags
	 *        the flag supplier to be used (this::flags)
	 */
	CodePointSequence(String pattern, Supplier<Integer> flags) {
		this.flags = flags;
		this.cursor = 0;
		String normalizedPattern;
		// Handle canonical equivalences
		if (has(flags.get(), CANON_EQ) && !has(flags.get(), LITERAL)) {
			normalizedPattern = normalize(pattern);
		} else {
			normalizedPattern = pattern;
		}
		patternLength = normalizedPattern.length();
		// Copy pattern to int array for convenience
		// Use double zero to terminate pattern
		temp = new int[patternLength + 2];
		int c, count = 0;
		// Convert all chars into code points
		for (int x = 0; x < patternLength; x += Character.charCount(c)) {
			c = normalizedPattern.codePointAt(x);
			if (isSupplementary(c)) {
				hasSupplementary = true;
			}
			temp[count++] = c;
		}
		patternLength = count; // patternLength now in code points
		if (!has(flags.get(), LITERAL)) RemoveQEQuoting();
	}
	/**
	 * 
	 * Converts a slice of this CodePointSequence to a String
	 * 
	 * @param start
	 *        the starting index
	 * @param len
	 *        the length of the subsequence
	 */
	String toString(int start, int len) {
		return new String(temp, start, len);
	}
	/**
	 * The pattern is converted to normalizedD form and then a pure group
	 * is constructed to match canonical equivalences of the characters.
	 */
	private String normalize(String pattern) {
		int lastCodePoint = -1;
		// Convert pattern into normalizedD form
		String normalizedPattern = Normalizer.normalize(pattern,
				Normalizer.Form.NFD);
		patternLength = normalizedPattern.length();
		// Modify pattern to match canonical equivalences
		StringBuilder newPattern = new StringBuilder(patternLength);
		for (int i = 0; i < patternLength;) {
			int c = normalizedPattern.codePointAt(i);
			StringBuilder sequenceBuffer;
			if ((Character.getType(c) == Character.NON_SPACING_MARK)
					&& (lastCodePoint != -1)) {
				sequenceBuffer = new StringBuilder();
				sequenceBuffer.appendCodePoint(lastCodePoint);
				sequenceBuffer.appendCodePoint(c);
				while (Character.getType(c) == Character.NON_SPACING_MARK) {
					i += Character.charCount(c);
					if (i >= patternLength) break;
					c = normalizedPattern.codePointAt(i);
					sequenceBuffer.appendCodePoint(c);
				}
				String ea = produceEquivalentAlternation(sequenceBuffer
						.toString());
				newPattern.setLength(newPattern.length()
						- Character.charCount(lastCodePoint));
				newPattern.append("(?:").append(ea).append(")");
			} else if (c == '[' && lastCodePoint != '\\') {
				i = normalizeCharClass(normalizedPattern, newPattern, i);
			} else {
				newPattern.appendCodePoint(c);
			}
			lastCodePoint = c;
			i += Character.charCount(c);
		}
		return newPattern.toString();
	}
	/**
	 * Complete the character class being parsed and add a set
	 * of alternations to it that will match the canonical equivalences
	 * of the characters within the class.
	 */
	private int normalizeCharClass(String normalizedPattern,
			StringBuilder newPattern, int i) {
		StringBuilder charClass = new StringBuilder();
		StringBuilder eq = null;
		int lastCodePoint = -1;
		String result;
		i++;
		charClass.append("[");
		while (true) {
			int c = normalizedPattern.codePointAt(i);
			StringBuilder sequenceBuffer;
			if (c == ']' && lastCodePoint != '\\') {
				charClass.append((char) c);
				break;
			} else if (Character.getType(c) == Character.NON_SPACING_MARK) {
				sequenceBuffer = new StringBuilder();
				sequenceBuffer.appendCodePoint(lastCodePoint);
				while (Character.getType(c) == Character.NON_SPACING_MARK) {
					sequenceBuffer.appendCodePoint(c);
					i += Character.charCount(c);
					if (i >= normalizedPattern.length()) break;
					c = normalizedPattern.codePointAt(i);
				}
				String ea = produceEquivalentAlternation(sequenceBuffer
						.toString());
				charClass.setLength(charClass.length()
						- Character.charCount(lastCodePoint));
				if (eq == null) eq = new StringBuilder();
				eq.append('|');
				eq.append(ea);
			} else {
				charClass.appendCodePoint(c);
				i++;
			}
			if (i == normalizedPattern.length())
				throw error("Unclosed character class");
			lastCodePoint = c;
		}
		if (eq != null) {
			result = "(?:" + charClass.toString() + eq.toString() + ")";
		} else {
			result = charClass.toString();
		}
		newPattern.append(result);
		return i;
	}
	/**
	 * Given a specific sequence composed of a regular character and
	 * combining marks that follow it, produce the alternation that will
	 * match all canonical equivalences of that sequence.
	 */
	private String produceEquivalentAlternation(String source) {
		int len = countChars(source, 0, 1);
		if (source.length() == len)
		// source has one character.
			return source;
		String base = source.substring(0, len);
		String combiningMarks = source.substring(len);
		String[] perms = producePermutations(combiningMarks);
		StringBuilder result = new StringBuilder(source);
		// Add combined permutations
		for (int x = 0; x < perms.length; x++) {
			String next = base + perms[x];
			if (x > 0) result.append("|" + next);
			next = composeOneStep(next);
			if (next != null)
				result.append("|" + produceEquivalentAlternation(next));
		}
		return result.toString();
	}
	/**
	 * Returns an array of strings that have all the possible
	 * permutations of the characters in the input string.
	 * This is used to get a list of all possible orderings
	 * of a set of combining marks. Note that some of the permutations
	 * are invalid because of combining class collisions, and these
	 * possibilities must be removed because they are not canonically
	 * equivalent.
	 */
	private String[] producePermutations(String input) {
		if (input.length() == countChars(input, 0, 1))
			return new String[] { input };
		if (input.length() == countChars(input, 0, 2)) {
			int c0 = Character.codePointAt(input, 0);
			int c1 = Character.codePointAt(input, Character.charCount(c0));
			if (getClass(c1) == getClass(c0)) { return new String[] { input }; }
			String[] result = new String[2];
			result[0] = input;
			StringBuilder sb = new StringBuilder(2);
			sb.appendCodePoint(c1);
			sb.appendCodePoint(c0);
			result[1] = sb.toString();
			return result;
		}
		int length = 1;
		int nCodePoints = countCodePoints(input);
		for (int x = 1; x < nCodePoints; x++)
			length = length * (x + 1);
		String[] temp = new String[length];
		int combClass[] = new int[nCodePoints];
		for (int x = 0, i = 0; x < nCodePoints; x++) {
			int c = Character.codePointAt(input, i);
			combClass[x] = getClass(c);
			i += Character.charCount(c);
		}
		// For each char, take it out and add the permutations
		// of the remaining chars
		int index = 0;
		int len;
		// offset maintains the index in code units.
		loop: for (int x = 0, offset = 0; x < nCodePoints; x++, offset += len) {
			len = countChars(input, offset, 1);
			for (int y = x - 1; y >= 0; y--) {
				if (combClass[y] == combClass[x]) {
					continue loop;
				}
			}
			StringBuilder sb = new StringBuilder(input);
			String otherChars = sb.delete(offset, offset + len).toString();
			String[] subResult = producePermutations(otherChars);
			String prefix = input.substring(offset, offset + len);
			for (int y = 0; y < subResult.length; y++)
				temp[index++] = prefix + subResult[y];
		}
		String[] result = new String[index];
		for (int x = 0; x < index; x++)
			result[x] = temp[x];
		return result;
	}
	private int getClass(int c) {
		return sun.text.Normalizer.getCombiningClass(c);
	}
	/**
	 * Attempts to compose input by combining the first character
	 * with the first combining mark following it. Returns a String
	 * that is the composition of the leading character with its first
	 * combining mark followed by the remaining combining marks. Returns
	 * null if the first two characters cannot be further composed.
	 */
	private String composeOneStep(String input) {
		int len = countChars(input, 0, 2);
		String firstTwoCharacters = input.substring(0, len);
		String result = Normalizer.normalize(firstTwoCharacters,
				Normalizer.Form.NFC);
		if (result.equals(firstTwoCharacters))
			return null;
		else {
			String remainder = input.substring(len);
			return result + remainder;
		}
	}
	/**
	 * Preprocess any \Q...\E sequences in `temp', meta-quoting them.
	 * See the description of `quotemeta' in perlfunc(1).
	 */
	void RemoveQEQuoting() {
		final int pLen = patternLength;
		int i = 0;
		while (i < pLen - 1) {
			if (temp[i] != '\\')
				i += 1;
			else if (temp[i + 1] != 'Q')
				i += 2;
			else break;
		}
		if (i >= pLen - 1) // No \Q sequence found
			return;
		int j = i;
		i += 2;
		int[] newtemp = new int[j + 3 * (pLen - i) + 2];
		System.arraycopy(temp, 0, newtemp, 0, j);
		boolean inQuote = true;
		boolean beginQuote = true;
		while (i < pLen) {
			int c = temp[i++];
			if (!ASCII.isAscii(c) || ASCII.isAlpha(c)) {
				newtemp[j++] = c;
			} else if (ASCII.isDigit(c)) {
				if (beginQuote) {
					/*
					 * A unicode escape \[0xu] could be before this quote,
					 * and we don't want this numeric char to processed as
					 * part of the escape.
					 */
					newtemp[j++] = '\\';
					newtemp[j++] = 'x';
					newtemp[j++] = '3';
				}
				newtemp[j++] = c;
			} else if (c != '\\') {
				if (inQuote) newtemp[j++] = '\\';
				newtemp[j++] = c;
			} else if (inQuote) {
				if (temp[i] == 'E') {
					i++;
					inQuote = false;
				} else {
					newtemp[j++] = '\\';
					newtemp[j++] = '\\';
				}
			} else {
				if (temp[i] == 'Q') {
					i++;
					inQuote = true;
					beginQuote = true;
					continue;
				} else {
					newtemp[j++] = c;
					if (i != pLen) newtemp[j++] = temp[i++];
				}
			}
			beginQuote = false;
		}
		patternLength = j;
		temp = Arrays.copyOf(newtemp, j + 2); // double zero termination
	}
	/**
	 * Mark the end of pattern with a specific character.
	 */
	void mark(int c) {
		temp[patternLength] = c;
	}
	/**
	 * Peek the next character, and do not advance the cursor.
	 */
	int peek() {
		int ch = temp[cursor];
		if (has(flags.get(), COMMENTS)) ch = peekPastWhitespace(ch);
		return ch;
	}
	/**
	 * Read the next character, and advance the cursor by one.
	 */
	int read() {
		int ch = temp[cursor++];
		if (has(flags.get(), COMMENTS)) ch = parsePastWhitespace(ch);
		return ch;
	}
	/**
	 * Read the next character, and advance the cursor by one,
	 * ignoring the COMMENTS setting
	 */
	@SuppressWarnings("unused")
	private int readEscaped() {
		int ch = temp[cursor++];
		return ch;
	}
	/**
	 * Advance the cursor by one, and peek the next character.
	 */
	int next() {
		int ch = temp[++cursor];
		if (has(flags.get(), COMMENTS)) ch = peekPastWhitespace(ch);
		return ch;
	}
	/**
	 * Advance the cursor by one, and peek the next character,
	 * ignoring the COMMENTS setting
	 */
	int nextEscaped() {
		int ch = temp[++cursor];
		return ch;
	}
	/**
	 * Utility method for parsing control escape sequences.
	 */
	int c() {
		if (cursor < patternLength) { return read() ^ 64; }
		throw error("Illegal control escape sequence");
	}
	/**
	 * Utility method for parsing octal escape sequences.
	 */
	int o() {
		int n = read();
		if (((n - '0') | ('7' - n)) >= 0) {
			int m = read();
			if (((m - '0') | ('7' - m)) >= 0) {
				int o = read();
				if ((((o - '0') | ('7' - o)) >= 0)
						&& (((n - '0') | ('3' - n)) >= 0)) { return (n - '0')
						* 64 + (m - '0') * 8 + (o - '0'); }
				unread();
				return (n - '0') * 8 + (m - '0');
			}
			unread();
			return (n - '0');
		}
		throw error("Illegal octal escape sequence");
	}
	/**
	 * Utility method for parsing hexadecimal escape sequences.
	 */
	int x() {
		int n = read();
		if (ASCII.isHexDigit(n)) {
			int m = read();
			if (ASCII.isHexDigit(m)) { return ASCII.toDigit(n) * 16
					+ ASCII.toDigit(m); }
		} else if (n == '{' && ASCII.isHexDigit(peek())) {
			int ch = 0;
			while (ASCII.isHexDigit(n = read())) {
				ch = (ch << 4) + ASCII.toDigit(n);
				if (ch > Character.MAX_CODE_POINT)
					throw error("Hexadecimal codepoint is too big");
			}
			if (n != '}')
				throw error("Unclosed hexadecimal escape sequence");
			return ch;
		}
		throw error("Illegal hexadecimal escape sequence");
	}
	/**
	 * Utility method for parsing unicode escape sequences.
	 */
	private int cursor() {
		return cursor;
	}
	private void setcursor(int pos) {
		cursor = pos;
	}
	private int uxxxx() {
		int n = 0;
		for (int i = 0; i < 4; i++) {
			int ch = read();
			if (!ASCII.isHexDigit(ch)) { throw error("Illegal Unicode escape sequence"); }
			n = n * 16 + ASCII.toDigit(ch);
		}
		return n;
	}
	int u() {
		int n = uxxxx();
		if (Character.isHighSurrogate((char) n)) {
			int cur = cursor();
			if (read() == '\\' && read() == 'u') {
				int n2 = uxxxx();
				if (Character.isLowSurrogate((char) n2))
					return Character.toCodePoint((char) n, (char) n2);
			}
			setcursor(cur);
		}
		return n;
	}
	/**
	 * Match next character, signal error if failed.
	 */
	void accept(int ch, String s) {
		int testChar = temp[cursor++];
		if (has(flags.get(), COMMENTS))
			testChar = parsePastWhitespace(testChar);
		if (ch != testChar) { throw error(s); }
	}
	/**
	 * If in xmode peek past whitespace and comments.
	 */
	private int peekPastWhitespace(int ch) {
		while (ASCII.isSpace(ch) || ch == '#') {
			while (ASCII.isSpace(ch))
				ch = temp[++cursor];
			if (ch == '#') {
				ch = peekPastLine();
			}
		}
		return ch;
	}
	/**
	 * If in xmode parse past whitespace and comments.
	 */
	private int parsePastWhitespace(int ch) {
		while (ASCII.isSpace(ch) || ch == '#') {
			while (ASCII.isSpace(ch))
				ch = temp[cursor++];
			if (ch == '#') ch = parsePastLine();
		}
		return ch;
	}
	/**
	 * xmode parse past comment to end of line.
	 */
	private int parsePastLine() {
		int ch = temp[cursor++];
		while (ch != 0 && !isLineSeparator(ch))
			ch = temp[cursor++];
		return ch;
	}
	/**
	 * xmode peek past comment to end of line.
	 */
	private int peekPastLine() {
		int ch = temp[++cursor];
		while (ch != 0 && !isLineSeparator(ch))
			ch = temp[++cursor];
		return ch;
	}
	/**
	 * Determines if character is a line separator in the current mode
	 */
	private boolean isLineSeparator(int ch) {
		if (has(flags.get(), UNIX_LINES)) {
			return ch == '\n';
		} else {
			return (ch == '\n' || ch == '\r' || (ch | 1) == '\u2029' || ch == '\u0085');
		}
	}
	/**
	 * Read the character after the next one, and advance the cursor by two.
	 */
	int skip() {
		int i = cursor;
		int ch = temp[i + 1];
		cursor = i + 2;
		return ch;
	}
	/**
	 * Unread one next character, and retreat cursor by one.
	 */
	void unread() {
		cursor--;
	}
	/** Check extra pattern characters */
	void confirmEnding() {
		if (patternLength == cursor) return;
		throw error(peek() == ')' ? "Unmatched closing ')'"
				: "Unexpected internal error");
	}
	/**
	 * Internal method used for handling all syntax The pattern is
	 * displayed with a pointer to aid in locating the syntax error.
	 */
	PatternSyntaxException error(String s) {
		return new PatternSyntaxException(s, new String(temp, 0,
				patternLength), cursor - 1);
	}
	/**
	 * Counts the code points in the given character sequence.
	 */
	static final int countCodePoints(CharSequence seq) {
		int length = seq.length();
		int n = 0;
		for (int i = 0; i < length;) {
			n++;
			if (Character.isHighSurrogate(seq.charAt(i++))) {
				if (i < length && Character.isLowSurrogate(seq.charAt(i))) {
					i++;
				}
			}
		}
		return n;
	}
}
