package openjdk.regex;

import static openjdk.regex.Pattern.*;

import java.util.Locale;
import java.util.function.Supplier;

import openjdk.regex.CharProperty.BitClass;
import openjdk.regex.CharProperty.Block;
import openjdk.regex.CharProperty.Category;
import openjdk.regex.CharProperty.Ctype;
import openjdk.regex.CharProperty.HorizWS;
import openjdk.regex.CharProperty.Script;
import openjdk.regex.CharProperty.Utype;
import openjdk.regex.CharProperty.VertWS;
import openjdk.regex.Node.BackRef;
import openjdk.regex.Node.Begin;
import openjdk.regex.Node.Bound;
import openjdk.regex.Node.CIBackRef;
import openjdk.regex.Node.Curly;
import openjdk.regex.Node.Dollar;
import openjdk.regex.Node.End;
import openjdk.regex.Node.LastMatch;
import openjdk.regex.Node.LineEnding;
import openjdk.regex.Node.Ques;
import openjdk.regex.Node.UnixDollar;

public class UnprocessedPattern {
	/**
	 * Temporary storage used by parsing pattern slice.
	 */
	int[] buffer;
	/**
	 * Index into the pattern string that keeps track of how much has been
	 * parsed.
	 */
	CodePointSequence codepoints;
	/**
	 * If the Start node might possibly match supplementary characters.
	 * It is set to true during compiling if
	 * (1) There is supplementary char in pattern, or
	 * (2) There is complement node of Category or Block
	 */
	boolean hasSupplementary;
	/**
	 * The starting point of state machine for the find operation. This allows
	 * a match to start anywhere in the input.
	 */
	transient Node root;
	private final Supplier<Integer> flags;
	private final GroupRegistry registry;
	public UnprocessedPattern(Supplier<Integer> flags, GroupRegistry registry) {
		this.flags = flags;
		codepoints = new CodePointSequence(flags, 0);
		this.registry = registry;
	}
	void append(int ch, int len) {
		if (len >= buffer.length) {
			int[] tmp = new int[len + len];
			System.arraycopy(buffer, 0, tmp, 0, len);
			buffer = tmp;
		}
		buffer[len] = ch;
	}
	/**
	 * Parses a backref greedily, taking as many numbers as it
	 * can. The first digit is always treated as a backref, but
	 * multi digit numbers are only treated as a backref if at
	 * least that many backrefs exist at this point in the regex.
	 */
	private Node ref(int refNum) {
		boolean done = false;
		while (!done) {
			int ch = codepoints.peek();
			switch (ch) {
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
					int newRefNum = (refNum * 10) + (ch - '0');
					// Add another number if it doesn't make a group
					// that doesn't exist
					if (registry.capturingGroupCount - 1 < newRefNum) {
						done = true;
						break;
					}
					refNum = newRefNum;
					codepoints.read();
					break;
				default:
					done = true;
					break;
			}
		}
		if (has(CASE_INSENSITIVE))
			return new CIBackRef(refNum, has(UNICODE_CASE));
		else return new BackRef(refNum);
	}
	/**
	 * Parses an escape sequence to determine the actual value that needs
	 * to be matched.
	 * If -1 is returned and create was true a new object was added to the tree
	 * to handle the escape sequence.
	 * If the returned value is greater than zero, it is the value that
	 * matches the escape sequence.
	 */
	int escape(boolean inclass, boolean create, boolean isrange) {
		int ch = codepoints.skip();
		switch (ch) {
			case '0':
				return codepoints.o();
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				if (inclass) break;
				if (create) {
					root = ref((ch - '0'));
				}
				return -1;
			case 'A':
				if (inclass) break;
				if (create) root = new Begin();
				return -1;
			case 'B':
				if (inclass) break;
				if (create)
					root = new Bound(Bound.NONE,
							has(UNICODE_CHARACTER_CLASS));
				return -1;
			case 'C':
				break;
			case 'D':
				if (create)
					root = has(UNICODE_CHARACTER_CLASS) ? new Utype(
							UnicodeProp.DIGIT).complement() : new Ctype(
							ASCII.DIGIT).complement();
				return -1;
			case 'E':
			case 'F':
				break;
			case 'G':
				if (inclass) break;
				if (create) root = new LastMatch();
				return -1;
			case 'H':
				if (create) root = new HorizWS().complement();
				return -1;
			case 'I':
			case 'J':
			case 'K':
			case 'L':
			case 'M':
			case 'N':
			case 'O':
			case 'P':
			case 'Q':
				break;
			case 'R':
				if (inclass) break;
				if (create) root = new LineEnding();
				return -1;
			case 'S':
				if (create)
					root = has(UNICODE_CHARACTER_CLASS) ? new Utype(
							UnicodeProp.WHITE_SPACE).complement()
							: new Ctype(ASCII.SPACE).complement();
				return -1;
			case 'T':
			case 'U':
				break;
			case 'V':
				if (create) root = new VertWS().complement();
				return -1;
			case 'W':
				if (create)
					root = has(UNICODE_CHARACTER_CLASS) ? new Utype(
							UnicodeProp.WORD).complement() : new Ctype(
							ASCII.WORD).complement();
				return -1;
			case 'X':
			case 'Y':
				break;
			case 'Z':
				if (inclass) break;
				if (create) {
					if (has(UNIX_LINES))
						root = new UnixDollar(false);
					else root = new Dollar(false);
				}
				return -1;
			case 'a':
				return '\007';
			case 'b':
				if (inclass) break;
				if (create)
					root = new Bound(Bound.BOTH,
							has(UNICODE_CHARACTER_CLASS));
				return -1;
			case 'c':
				return codepoints.c();
			case 'd':
				if (create)
					root = has(UNICODE_CHARACTER_CLASS) ? new Utype(
							UnicodeProp.DIGIT) : new Ctype(ASCII.DIGIT);
				return -1;
			case 'e':
				return '\033';
			case 'f':
				return '\f';
			case 'g':
				break;
			case 'h':
				if (create) root = new HorizWS();
				return -1;
			case 'i':
			case 'j':
				break;
			case 'k':
				if (inclass) break;
				if (codepoints.read() != '<')
					throw codepoints
							.error("\\k is not followed by '<' for named capturing group");
				String name = groupname(codepoints.read());
				if (!registry.groupDefined(name))
					throw codepoints.error("(named capturing group <"
							+ name + "> does not exit");
				if (create) {
					if (has(CASE_INSENSITIVE))
						root = new CIBackRef(registry.groupNumber(name),
								has(UNICODE_CASE));
					else root = new BackRef(registry.groupNumber(name));
				}
				return -1;
			case 'l':
			case 'm':
				break;
			case 'n':
				return '\n';
			case 'o':
			case 'p':
			case 'q':
				break;
			case 'r':
				return '\r';
			case 's':
				if (create)
					root = has(UNICODE_CHARACTER_CLASS) ? new Utype(
							UnicodeProp.WHITE_SPACE) : new Ctype(
							ASCII.SPACE);
				return -1;
			case 't':
				return '\t';
			case 'u':
				return codepoints.u();
			case 'v':
				// '\v' was implemented as VT/0x0B in releases < 1.8 (though
				// undocumented). In JDK8 '\v' is specified as a predefined
				// character class for all vertical whitespace characters.
				// So [-1, root=VertWS node] pair is returned (instead of a
				// single 0x0B). This breaks the range if '\v' is used as
				// the start or end value, such as [\v-...] or [...-\v], in
				// which a single definite value (0x0B) is expected. For
				// compatibility concern '\013'/0x0B is returned if isrange.
				if (isrange) return '\013';
				if (create) root = new VertWS();
				return -1;
			case 'w':
				if (create)
					root = has(UNICODE_CHARACTER_CLASS) ? new Utype(
							UnicodeProp.WORD) : new Ctype(ASCII.WORD);
				return -1;
			case 'x':
				return codepoints.x();
			case 'y':
				break;
			case 'z':
				if (inclass) break;
				if (create) root = new End();
				return -1;
			default:
				return ch;
		}
		throw codepoints.error("Illegal/unsupported escape sequence");
	}
	/**
	 * Processes repetition. If the next character peeked is a quantifier
	 * then new nodes must be appended to handle the repetition.
	 * Prev could be a single or a group, so it could be a chain of nodes.
	 */
	Node closure(Node prev) {
		int ch = codepoints.peek();
		switch (ch) {
			case '?':
				ch = codepoints.next();
				if (ch == '?') {
					codepoints.next();
					return new Ques(prev, LAZY);
				} else if (ch == '+') {
					codepoints.next();
					return new Ques(prev, POSSESSIVE);
				}
				return new Ques(prev, GREEDY);
			case '*':
				ch = codepoints.next();
				if (ch == '?') {
					codepoints.next();
					return new Curly(prev, 0, MAX_REPS, LAZY);
				} else if (ch == '+') {
					codepoints.next();
					return new Curly(prev, 0, MAX_REPS, POSSESSIVE);
				}
				return new Curly(prev, 0, MAX_REPS, GREEDY);
			case '+':
				ch = codepoints.next();
				if (ch == '?') {
					codepoints.next();
					return new Curly(prev, 1, MAX_REPS, LAZY);
				} else if (ch == '+') {
					codepoints.next();
					return new Curly(prev, 1, MAX_REPS, POSSESSIVE);
				}
				return new Curly(prev, 1, MAX_REPS, GREEDY);
			case '{':
				ch = codepoints.temp[codepoints.cursor + 1];
				if (ASCII.isDigit(ch)) {
					codepoints.skip();
					int cmin = 0;
					do {
						cmin = cmin * 10 + (ch - '0');
					} while (ASCII.isDigit(ch = codepoints.read()));
					int cmax = cmin;
					if (ch == ',') {
						ch = codepoints.read();
						cmax = MAX_REPS;
						if (ch != '}') {
							cmax = 0;
							while (ASCII.isDigit(ch)) {
								cmax = cmax * 10 + (ch - '0');
								ch = codepoints.read();
							}
						}
					}
					if (ch != '}')
						throw this.codepoints
								.error("Unclosed counted closure");
					if (((cmin) | (cmax) | (cmax - cmin)) < 0)
						throw codepoints
								.error("Illegal repetition range");
					Curly curly;
					ch = codepoints.peek();
					if (ch == '?') {
						codepoints.next();
						curly = new Curly(prev, cmin, cmax, LAZY);
					} else if (ch == '+') {
						codepoints.next();
						curly = new Curly(prev, cmin, cmax, POSSESSIVE);
					} else {
						curly = new Curly(prev, cmin, cmax, GREEDY);
					}
					return curly;
				} else {
					throw codepoints.error("Illegal repetition");
				}
			default:
				return prev;
		}
	}
	/**
	 * Parse a character class, and return the node that matches it.
	 * Consumes a ] on the way out if consume is true. Usually consume
	 * is true except for the case of [abc&&def] where def is a separate
	 * right hand node with "understood" brackets.
	 */
	CharProperty clazz(boolean consume) {
		CharProperty prev = null;
		CharProperty node = null;
		BitClass bits = new BitClass();
		boolean include = true;
		boolean firstInClass = true;
		int ch = codepoints.next();
		for (;;) {
			switch (ch) {
				case '^':
					// Negates if first char in a class, otherwise literal
					if (firstInClass) {
						if (codepoints.temp[codepoints.cursor - 1] != '[')
							break;
						ch = codepoints.next();
						include = !include;
						continue;
					} else {
						// ^ not first in class, treat as literal
						break;
					}
				case '[':
					firstInClass = false;
					node = clazz(true);
					if (prev == null)
						prev = node;
					else prev = union(prev, node);
					ch = codepoints.peek();
					continue;
				case '&':
					firstInClass = false;
					ch = codepoints.next();
					if (ch == '&') {
						ch = codepoints.next();
						CharProperty rightNode = null;
						while (ch != ']' && ch != '&') {
							if (ch == '[') {
								if (rightNode == null)
									rightNode = clazz(true);
								else rightNode = union(rightNode,
										clazz(true));
							} else { // abc&&def
								codepoints.unread();
								rightNode = clazz(false);
							}
							ch = codepoints.peek();
						}
						if (rightNode != null) node = rightNode;
						if (prev == null) {
							if (rightNode == null)
								throw codepoints
										.error("Bad class syntax");
							else prev = rightNode;
						} else {
							prev = intersection(prev, node);
						}
					} else {
						// treat as a literal &
						codepoints.unread();
						break;
					}
					continue;
				case 0:
					firstInClass = false;
					if (codepoints.cursor >= codepoints.patternLength)
						throw codepoints
								.error("Unclosed character class");
					break;
				case ']':
					firstInClass = false;
					if (prev != null) {
						if (consume) codepoints.next();
						return prev;
					}
					break;
				default:
					firstInClass = false;
					break;
			}
			node = range(bits);
			if (include) {
				if (prev == null) {
					prev = node;
				} else {
					if (prev != node) prev = union(prev, node);
				}
			} else {
				if (prev == null) {
					prev = node.complement();
				} else {
					if (prev != node) prev = setDifference(prev, node);
				}
			}
			ch = codepoints.peek();
		}
	}
	private CharProperty bitsOrSingle(BitClass bits, int ch) {
		if (ch < 256
				&& !(has(CASE_INSENSITIVE) && has(UNICODE_CASE) && (ch == 0xff
						|| ch == 0xb5 || ch == 0x49 || ch == 0x69 || // I
															// and
															// i
						ch == 0x53 || ch == 0x73 || // S and s
						ch == 0x4b || ch == 0x6b || // K and k
						ch == 0xc5 || ch == 0xe5))) // A+ring
			return bits.add(ch, flags.get());
		return newSingle(ch, flags.get());
	}
	/**
	 * Parse a single character or a character range in a character class
	 * and return its representative node.
	 */
	private CharProperty range(BitClass bits) {
		int ch = codepoints.peek();
		if (ch == '\\') {
			ch = codepoints.nextEscaped();
			if (ch == 'p' || ch == 'P') { // A property
				boolean comp = (ch == 'P');
				boolean oneLetter = true;
				// Consume { if present
				ch = codepoints.next();
				if (ch != '{')
					codepoints.unread();
				else oneLetter = false;
				return family(oneLetter, comp);
			} else { // ordinary escape
				boolean isrange = codepoints.temp[codepoints.cursor + 1] == '-';
				codepoints.unread();
				ch = escape(true, true, isrange);
				if (ch == -1) return (CharProperty) root;
			}
		} else {
			codepoints.next();
		}
		if (ch >= 0) {
			if (codepoints.peek() == '-') {
				int endRange = codepoints.temp[codepoints.cursor + 1];
				if (endRange == '[') { return bitsOrSingle(bits, ch); }
				if (endRange != ']') {
					codepoints.next();
					int m = codepoints.peek();
					if (m == '\\') {
						m = escape(true, false, true);
					} else {
						codepoints.next();
					}
					if (m < ch) { throw codepoints
							.error("Illegal character range"); }
					if (has(CASE_INSENSITIVE))
						return caseInsensitiveRangeFor(ch, m);
					else return rangeFor(ch, m);
				}
			}
			return bitsOrSingle(bits, ch);
		}
		throw codepoints.error("Unexpected character '" + ((char) ch) + "'");
	}
	/**
	 * Parses a Unicode character family and returns its representative node.
	 */
	CharProperty family(boolean singleLetter, boolean maybeComplement) {
		codepoints.next();
		String name;
		CharProperty node = null;
		if (singleLetter) {
			int c = codepoints.temp[codepoints.cursor];
			if (!Character.isSupplementaryCodePoint(c)) {
				name = String.valueOf((char) c);
			} else {
				name = new String(codepoints.temp, codepoints.cursor, 1);
			}
			codepoints.read();
		} else {
			int i = codepoints.cursor;
			codepoints.mark('}');
			while (codepoints.read() != '}') {}
			codepoints.mark('\000');
			int j = codepoints.cursor;
			if (j > codepoints.patternLength)
				throw codepoints.error("Unclosed character family");
			if (i + 1 >= j)
				throw codepoints.error("Empty character family");
			name = new String(codepoints.temp, i, j - i - 1);
		}
		int i = name.indexOf('=');
		if (i != -1) {
			// property construct \p{name=value}
			String value = name.substring(i + 1);
			name = name.substring(0, i).toLowerCase(Locale.ENGLISH);
			if ("sc".equals(name) || "script".equals(name)) {
				node = unicodeScriptPropertyFor(value);
			} else if ("blk".equals(name) || "block".equals(name)) {
				node = unicodeBlockPropertyFor(value);
			} else if ("gc".equals(name) || "general_category".equals(name)) {
				node = charPropertyNodeFor(value);
			} else {
				throw codepoints.error("Unknown Unicode property {name=<"
						+ name + ">, " + "value=<" + value + ">}");
			}
		} else {
			if (name.startsWith("In")) {
				// \p{inBlockName}
				node = unicodeBlockPropertyFor(name.substring(2));
			} else if (name.startsWith("Is")) {
				// \p{isGeneralCategory} and \p{isScriptName}
				name = name.substring(2);
				UnicodeProp uprop = UnicodeProp.forName(name);
				if (uprop != null) node = new Utype(uprop);
				if (node == null)
					node = CharPropertyNames.charPropertyFor(name);
				if (node == null) node = unicodeScriptPropertyFor(name);
			} else {
				if (has(UNICODE_CHARACTER_CLASS)) {
					UnicodeProp uprop = UnicodeProp.forPOSIXName(name);
					if (uprop != null) node = new Utype(uprop);
				}
				if (node == null) node = charPropertyNodeFor(name);
			}
		}
		if (maybeComplement) {
			if (node instanceof Category || node instanceof Block)
				hasSupplementary = true;
			node = node.complement();
		}
		return node;
	}
	/**
	 * Returns a CharProperty matching all characters belong to
	 * a UnicodeScript.
	 */
	private CharProperty unicodeScriptPropertyFor(String name) {
		final Character.UnicodeScript script;
		try {
			script = Character.UnicodeScript.forName(name);
		} catch (IllegalArgumentException iae) {
			throw codepoints.error("Unknown character script name {" + name
					+ "}");
		}
		return new Script(script);
	}
	/**
	 * Returns a CharProperty matching all characters in a UnicodeBlock.
	 */
	private CharProperty unicodeBlockPropertyFor(String name) {
		final Character.UnicodeBlock block;
		try {
			block = Character.UnicodeBlock.forName(name);
		} catch (IllegalArgumentException iae) {
			throw codepoints.error("Unknown character block name {" + name
					+ "}");
		}
		return new Block(block);
	}
	/**
	 * Returns a CharProperty matching all characters in a named property.
	 */
	private CharProperty charPropertyNodeFor(String name) {
		CharProperty p = CharPropertyNames.charPropertyFor(name);
		if (p == null)
			throw codepoints.error("Unknown character property name {"
					+ name + "}");
		return p;
	}
	/**
	 * Parses and returns the name of a "named capturing group", the trailing
	 * ">" is consumed after parsing.
	 */
	String groupname(int ch) {
		StringBuilder sb = new StringBuilder();
		sb.append(Character.toChars(ch));
		while (ASCII.isLower(ch = codepoints.read()) || ASCII.isUpper(ch)
				|| ASCII.isDigit(ch)) {
			sb.append(Character.toChars(ch));
		}
		if (sb.length() == 0)
			throw codepoints
					.error("named capturing group has 0 length name");
		if (ch != '>')
			throw codepoints
					.error("named capturing group is missing trailing '>'");
		return sb.toString();
	}
	/**
	 * Returns the set union of two CharProperty nodes.
	 */
	private static CharProperty union(final CharProperty lhs,
			final CharProperty rhs) {
		return new CharProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				return lhs.isSatisfiedBy(ch) || rhs.isSatisfiedBy(ch);
			}
		};
	}
	/**
	 * Returns the set intersection of two CharProperty nodes.
	 */
	private static CharProperty intersection(final CharProperty lhs,
			final CharProperty rhs) {
		return new CharProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				return lhs.isSatisfiedBy(ch) && rhs.isSatisfiedBy(ch);
			}
		};
	}
	/**
	 * Returns the set difference of two CharProperty nodes.
	 */
	private static CharProperty setDifference(final CharProperty lhs,
			final CharProperty rhs) {
		return new CharProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				return !rhs.isSatisfiedBy(ch) && lhs.isSatisfiedBy(ch);
			}
		};
	}
	/**
	 * Returns node for matching characters within an explicit value
	 * range in a case insensitive manner.
	 */
	private CharProperty caseInsensitiveRangeFor(final int lower,
			final int upper) {
		if (has(UNICODE_CASE)) return new CharProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				if (inRange(lower, ch, upper)) return true;
				int up = Character.toUpperCase(ch);
				return inRange(lower, up, upper)
						|| inRange(lower, Character.toLowerCase(up),
								upper);
			}
		};
		return new CharProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				return inRange(lower, ch, upper)
						|| ASCII.isAscii(ch)
						&& (inRange(lower, ASCII.toUpper(ch), upper) || inRange(
								lower, ASCII.toLower(ch), upper));
			}
		};
	}
	private boolean has(int flag) {
		return Pattern.has(flags.get(), flag);
	}
}
