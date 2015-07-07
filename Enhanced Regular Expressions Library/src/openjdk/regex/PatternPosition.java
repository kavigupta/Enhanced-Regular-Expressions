package openjdk.regex;

import static openjdk.regex.Pattern.COMMENTS;
import static openjdk.regex.Pattern.UNIX_LINES;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PatternPosition {
	int[] temp;
	final Supplier<Integer> patternLengthS;
	final Consumer<Integer> patternLengthC;
	final Supplier<Integer> flags;
	final ErrorProvider errors;
	int cursor;
	public PatternPosition(Supplier<Integer> patternLengthS,
			Consumer<Integer> patternLengthC, Supplier<Integer> flags,
			int cursor, ErrorProvider errors) {
		this.patternLengthS = patternLengthS;
		this.patternLengthC = patternLengthC;
		this.flags = flags;
		this.cursor = cursor;
		this.errors = errors;
	}
	/**
	 * Preprocess any \Q...\E sequences in `temp', meta-quoting them.
	 * See the description of `quotemeta' in perlfunc(1).
	 */
	void RemoveQEQuoting() {
		final int pLen = patternLengthS.get();
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
		patternLengthC.accept(j);
		temp = Arrays.copyOf(newtemp, j + 2); // double zero termination
	}
	/**
	 * Mark the end of pattern with a specific character.
	 */
	void mark(int c) {
		temp[patternLengthS.get()] = c;
	}
	/**
	 * Peek the next character, and do not advance the cursor.
	 */
	int peek() {
		int ch = temp[cursor];
		if (has(COMMENTS)) ch = peekPastWhitespace(ch);
		return ch;
	}
	/**
	 * Read the next character, and advance the cursor by one.
	 */
	int read() {
		int ch = temp[cursor++];
		if (has(COMMENTS)) ch = parsePastWhitespace(ch);
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
		if (has(COMMENTS)) ch = peekPastWhitespace(ch);
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
		if (cursor < patternLengthS.get()) { return read() ^ 64; }
		throw errors.error("Illegal control escape sequence");
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
		throw errors.error("Illegal octal escape sequence");
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
					throw errors.error("Hexadecimal codepoint is too big");
			}
			if (n != '}')
				throw errors.error("Unclosed hexadecimal escape sequence");
			return ch;
		}
		throw errors.error("Illegal hexadecimal escape sequence");
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
			if (!ASCII.isHexDigit(ch)) { throw errors
					.error("Illegal Unicode escape sequence"); }
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
		if (has(COMMENTS)) testChar = parsePastWhitespace(testChar);
		if (ch != testChar) { throw errors.error(s); }
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
		if (has(UNIX_LINES)) {
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
	private boolean has(int f) {
		return Pattern.has(flags.get(), f);
	}
}
