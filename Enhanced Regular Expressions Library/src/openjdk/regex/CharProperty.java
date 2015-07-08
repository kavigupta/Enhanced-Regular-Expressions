package openjdk.regex;

import static openjdk.regex.Pattern.CASE_INSENSITIVE;
import static openjdk.regex.Pattern.UNICODE_CASE;
import openjdk.regex.Pattern.TreeInfo;

/**
 * Abstract node class to match one character satisfying some
 * boolean property.
 */
abstract class CharProperty extends Node {
	abstract boolean isSatisfiedBy(int ch);
	CharProperty complement() {
		return new CharProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				return !CharProperty.this.isSatisfiedBy(ch);
			}
		};
	}
	@Override
	boolean match(Matcher matcher, int i, CharSequence seq) {
		if (i < matcher.to) {
			int ch = Character.codePointAt(seq, i);
			return isSatisfiedBy(ch)
					&& next.match(matcher, i + Character.charCount(ch),
							seq);
		} else {
			matcher.hitEnd = true;
			return false;
		}
	}
	@Override
	boolean study(TreeInfo info) {
		info.minLength++;
		info.maxLength++;
		return next.study(info);
	}
	/**
	 * Optimized version of CharProperty that works only for
	 * properties never satisfied by Supplementary characters.
	 */
	static abstract class BmpCharProperty extends CharProperty {
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			if (i < matcher.to) {
				return isSatisfiedBy(seq.charAt(i))
						&& next.match(matcher, i + 1, seq);
			} else {
				matcher.hitEnd = true;
				return false;
			}
		}
	}
	/**
	 * Node class that matches a Supplementary Unicode character
	 */
	static final class SingleS extends CharProperty {
		final int c;
		SingleS(int c) {
			this.c = c;
		}
		@Override
		boolean isSatisfiedBy(int ch) {
			return ch == c;
		}
	}
	/**
	 * Optimization -- matches a given BMP character
	 */
	static final class Single extends BmpCharProperty {
		final int c;
		Single(int c) {
			this.c = c;
		}
		@Override
		boolean isSatisfiedBy(int ch) {
			return ch == c;
		}
	}
	/**
	 * Case insensitive matches a given BMP character
	 */
	static final class SingleI extends BmpCharProperty {
		final int lower;
		final int upper;
		SingleI(int lower, int upper) {
			this.lower = lower;
			this.upper = upper;
		}
		@Override
		boolean isSatisfiedBy(int ch) {
			return ch == lower || ch == upper;
		}
	}
	/**
	 * Unicode case insensitive matches a given Unicode character
	 */
	static final class SingleU extends CharProperty {
		final int lower;
		SingleU(int lower) {
			this.lower = lower;
		}
		@Override
		boolean isSatisfiedBy(int ch) {
			return lower == ch
					|| lower == Character.toLowerCase(Character
							.toUpperCase(ch));
		}
	}
	/**
	 * Node class that matches a Unicode block.
	 */
	static final class Block extends CharProperty {
		final Character.UnicodeBlock block;
		Block(Character.UnicodeBlock block) {
			this.block = block;
		}
		@Override
		boolean isSatisfiedBy(int ch) {
			return block == Character.UnicodeBlock.of(ch);
		}
	}
	/**
	 * Node class that matches a Unicode script
	 */
	static final class Script extends CharProperty {
		final Character.UnicodeScript script;
		Script(Character.UnicodeScript script) {
			this.script = script;
		}
		@Override
		boolean isSatisfiedBy(int ch) {
			return script == Character.UnicodeScript.of(ch);
		}
	}
	/**
	 * Node class that matches a Unicode category.
	 */
	static final class Category extends CharProperty {
		final int typeMask;
		Category(int typeMask) {
			this.typeMask = typeMask;
		}
		@Override
		boolean isSatisfiedBy(int ch) {
			return (typeMask & (1 << Character.getType(ch))) != 0;
		}
	}
	/**
	 * Node class that matches a Unicode "type"
	 */
	static final class Utype extends CharProperty {
		final UnicodeProp uprop;
		Utype(UnicodeProp uprop) {
			this.uprop = uprop;
		}
		@Override
		boolean isSatisfiedBy(int ch) {
			return uprop.is(ch);
		}
	}
	/**
	 * Node class that matches a POSIX type.
	 */
	static final class Ctype extends BmpCharProperty {
		final int ctype;
		Ctype(int ctype) {
			this.ctype = ctype;
		}
		@Override
		boolean isSatisfiedBy(int ch) {
			return ch < 128 && ASCII.isType(ch, ctype);
		}
	}
	/**
	 * Node class that matches a Perl vertical whitespace
	 */
	static final class VertWS extends BmpCharProperty {
		@Override
		boolean isSatisfiedBy(int cp) {
			return (cp >= 0x0A && cp <= 0x0D) || cp == 0x85 || cp == 0x2028
					|| cp == 0x2029;
		}
	}
	/**
	 * Node class that matches a Perl horizontal whitespace
	 */
	static final class HorizWS extends BmpCharProperty {
		@Override
		boolean isSatisfiedBy(int cp) {
			return cp == 0x09 || cp == 0x20 || cp == 0xa0 || cp == 0x1680
					|| cp == 0x180e || cp >= 0x2000 && cp <= 0x200a
					|| cp == 0x202f || cp == 0x205f || cp == 0x3000;
		}
	}
	/**
	 * Implements the Unicode category ALL and the dot metacharacter when
	 * in dotall mode.
	 */
	static final class All extends CharProperty {
		@Override
		boolean isSatisfiedBy(int ch) {
			return true;
		}
	}
	/**
	 * Node class for the dot metacharacter when dotall is not enabled.
	 */
	static final class Dot extends CharProperty {
		@Override
		boolean isSatisfiedBy(int ch) {
			return (ch != '\n' && ch != '\r' && (ch | 1) != '\u2029' && ch != '\u0085');
		}
	}
	/**
	 * Node class for the dot metacharacter when dotall is not enabled
	 * but UNIX_LINES is enabled.
	 */
	static final class UnixDot extends CharProperty {
		@Override
		boolean isSatisfiedBy(int ch) {
			return ch != '\n';
		}
	}
	/**
	 * Creates a bit vector for matching Latin-1 values. A normal BitClass
	 * never matches values above Latin-1, and a complemented BitClass always
	 * matches values above Latin-1.
	 */
	static final class BitClass extends BmpCharProperty {
		private final boolean[] bits;
		BitClass() {
			bits = new boolean[256];
		}
		BitClass add(int c, int flags) {
			assert c >= 0 && c <= 255;
			if ((flags & CASE_INSENSITIVE) != 0) {
				if (ASCII.isAscii(c)) {
					bits[ASCII.toUpper(c)] = true;
					bits[ASCII.toLower(c)] = true;
				} else if ((flags & UNICODE_CASE) != 0) {
					bits[Character.toLowerCase(c)] = true;
					bits[Character.toUpperCase(c)] = true;
				}
			}
			bits[c] = true;
			return this;
		}
		@Override
		boolean isSatisfiedBy(int ch) {
			return ch < 256 && bits[ch];
		}
	}
}