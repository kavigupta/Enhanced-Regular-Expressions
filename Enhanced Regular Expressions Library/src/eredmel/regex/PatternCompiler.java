/*
 * Refactored out of the original openjdk.regex.Pattern class
 */
package eredmel.regex;

import static eredmel.regex.Pattern.*;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import eredmel.regex.CharProperty.All;
import eredmel.regex.CharProperty.BitClass;
import eredmel.regex.CharProperty.Block;
import eredmel.regex.CharProperty.Category;
import eredmel.regex.CharProperty.Ctype;
import eredmel.regex.CharProperty.Dot;
import eredmel.regex.CharProperty.HorizWS;
import eredmel.regex.CharProperty.Script;
import eredmel.regex.CharProperty.UnixDot;
import eredmel.regex.CharProperty.Utype;
import eredmel.regex.CharProperty.VertWS;
import eredmel.regex.Node.BackRef;
import eredmel.regex.Node.Begin;
import eredmel.regex.Node.Behind;
import eredmel.regex.Node.BehindS;
import eredmel.regex.Node.BnM;
import eredmel.regex.Node.Bound;
import eredmel.regex.Node.Branch;
import eredmel.regex.Node.BranchConn;
import eredmel.regex.Node.CIBackRef;
import eredmel.regex.Node.Caret;
import eredmel.regex.Node.Curly;
import eredmel.regex.Node.Dollar;
import eredmel.regex.Node.End;
import eredmel.regex.Node.EnregexCloseParen;
import eredmel.regex.Node.EnregexOpenParen;
import eredmel.regex.Node.EnregexQuote;
import eredmel.regex.Node.First;
import eredmel.regex.Node.GroupCurly;
import eredmel.regex.Node.GroupHead;
import eredmel.regex.Node.GroupTail;
import eredmel.regex.Node.LastMatch;
import eredmel.regex.Node.LazyLoop;
import eredmel.regex.Node.LineEnding;
import eredmel.regex.Node.Loop;
import eredmel.regex.Node.Neg;
import eredmel.regex.Node.NotBehind;
import eredmel.regex.Node.NotBehindS;
import eredmel.regex.Node.Pos;
import eredmel.regex.Node.Prolog;
import eredmel.regex.Node.Ques;
import eredmel.regex.Node.Slice;
import eredmel.regex.Node.SliceI;
import eredmel.regex.Node.SliceIS;
import eredmel.regex.Node.SliceS;
import eredmel.regex.Node.SliceU;
import eredmel.regex.Node.SliceUS;
import eredmel.regex.Node.Start;
import eredmel.regex.Node.StartS;
import eredmel.regex.Node.UnixCaret;
import eredmel.regex.Node.UnixDollar;
import eredmel.regex.Pattern.TreeInfo;

/**
 * A parser class for regex patterns
 */
public class PatternCompiler implements java.io.Serializable {
	/**
	 * Temporary storage used by parsing pattern slice.
	 */
	private transient int[] buffer;
	/**
	 * Index into the pattern string that keeps track of how much has been
	 * parsed.
	 */
	private transient CodePointSequence codepoints;
	/**
	 * The local variable count used by parsing tree. Used by matchers to
	 * allocate storage needed to perform a match.
	 */
	private transient int localCount;
	/**
	 * The starting point of state machine for the find operation. This allows
	 * a match to start anywhere in the input.
	 */
	private transient Node root;
	/**
	 * The original pattern flags.
	 *
	 * @serial
	 */
	private int flags;
	private final GroupRegistry registry;
	private final EnregexType type;
	private PatternCompiler(String pattern, int f, EnregexType type) {
		this.flags = f;
		this.localCount = 0;
		// to use UNICODE_CASE if UNICODE_CHARACTER_CLASS present
		if ((flags & UNICODE_CHARACTER_CLASS) != 0) flags |= UNICODE_CASE;
		codepoints = new CodePointSequence(pattern, this::flags);
		this.registry = new GroupRegistry();
		this.type = type;
	}
	/**
	 * Copies regular expression to an int array and invokes the parsing
	 * of the expression which will create the object tree.
	 */
	public static CompiledPattern compile(String pattern, int flags,
			EnregexType type) {
		PatternCompiler pc = new PatternCompiler(pattern, flags, type);
		Node matchRoot = pc.parse();
		return new CompiledPattern(pc.root, matchRoot, pc.registry,
				pc.localCount);
	}
	private Node parse() {
		// Allocate all temporary objects here.
		buffer = new int[32];
		registry.clear();
		final Node matchRoot;
		if (has(LITERAL)) {
			// Literal pattern handling
			matchRoot = newSlice(codepoints.temp, codepoints.patternLength,
					codepoints.hasSupplementary);
			matchRoot.next = lastAccept;
		} else {
			// Start recursive descent parsing
			matchRoot = expr(lastAccept);
			codepoints.confirmEnding();
		}
		// Peephole optimization
		if (matchRoot instanceof Slice) {
			root = BnM.optimize(matchRoot);
			if (root == matchRoot) {
				root = codepoints.hasSupplementary ? new StartS(matchRoot)
						: new Start(matchRoot);
			}
		} else if (matchRoot instanceof Begin || matchRoot instanceof First) {
			root = matchRoot;
		} else {
			root = codepoints.hasSupplementary ? new StartS(matchRoot)
					: new Start(matchRoot);
		}
		return matchRoot;
	}
	public int flags() {
		return flags;
	}
	private void append(int ch, int len) {
		if (len >= buffer.length) {
			int[] tmp = new int[len + len];
			System.arraycopy(buffer, 0, tmp, 0, len);
			buffer = tmp;
		}
		buffer[len] = ch;
	}
	/**
	 * The expression is parsed with branch nodes added for alternations.
	 * This may be called recursively to parse sub expressions that may
	 * contain alternations.
	 */
	private Node expr(Node end) {
		Node prev = null;
		Node firstTail = null;
		Branch branch = null;
		Node branchConn = null;
		for (;;) {
			Node node = sequence(end);
			Node nodeTail = root; // double return
			if (prev == null) {
				prev = node;
				firstTail = nodeTail;
			} else {
				// Branch
				if (branchConn == null) {
					branchConn = new BranchConn();
					branchConn.next = end;
				}
				if (node == end) {
					// if the node returned from sequence() is "end"
					// we have an empty expr, set a null atom into
					// the branch to indicate to go "next" directly.
					node = null;
				} else {
					// the "tail.next" of each atom goes to branchConn
					nodeTail.next = branchConn;
				}
				if (prev == branch) {
					branch.add(node);
				} else {
					if (prev == end) {
						prev = null;
					} else {
						// replace the "end" with "branchConn" at its
						// tail.next
						// when put the "prev" into the branch as the
						// first atom.
						firstTail.next = branchConn;
					}
					prev = branch = new Branch(prev, node, branchConn);
				}
			}
			if (codepoints.peek() != '|') { return prev; }
			codepoints.next();
		}
	}
	@SuppressWarnings("fallthrough")
	/**
	 * Parsing of sequences between alternations.
	 */
	private Node sequence(Node end) {
		Node head = null;
		Node tail = null;
		Node node = null;
		LOOP: for (;;) {
			int ch = codepoints.peek();
			switch (ch) {
				case '(':
					// Because group handles its own closure,
					// we need to treat it differently
					node = group0();
					// Check for comment or flag group
					if (node == null) continue;
					if (head == null)
						head = node;
					else tail.next = node;
					// Double return: Tail was returned in root
					tail = root;
					continue;
				case '[':
					node = clazz(true);
					break;
				case '\\':
					ch = codepoints.nextEscaped();
					if (ch == 'p' || ch == 'P') {
						boolean oneLetter = true;
						boolean comp = (ch == 'P');
						ch = codepoints.next(); // Consume { if
											// present
						if (ch != '{') {
							codepoints.unread();
						} else {
							oneLetter = false;
						}
						node = family(oneLetter, comp);
					} else {
						codepoints.unread();
						node = atom();
					}
					break;
				case '^':
					codepoints.next();
					if (has(MULTILINE)) {
						if (has(UNIX_LINES))
							node = new UnixCaret();
						else node = new Caret();
					} else {
						node = new Begin();
					}
					break;
				case '$':
					codepoints.next();
					if (has(UNIX_LINES))
						node = new UnixDollar(has(MULTILINE));
					else node = new Dollar(has(MULTILINE));
					break;
				case '.':
					codepoints.next();
					if (has(DOTALL)) {
						node = new All();
					} else {
						if (has(UNIX_LINES))
							node = new UnixDot();
						else {
							node = new Dot();
						}
					}
					break;
				case '|':
				case ')':
					break LOOP;
				case ']': // Now interpreting dangling ] and } as literals
				case '}':
					node = atom();
					break;
				case '?':
				case '*':
				case '+':
					codepoints.next();
					throw codepoints.error("Dangling meta character '"
							+ ((char) ch) + "'");
				case '~':
					if (has(ENHANCED_REGEX)) {
						node = enhancedRegex(false);
						codepoints.next();
						Logger.getGlobal().log(Level.INFO,
								node.toString());
						break;
					}
					node = atom();
					break;
				case 0:
					if (codepoints.cursor >= codepoints.patternLength) {
						break LOOP;
					}
					// Fall through
				default:
					node = atom();
					break;
			}
			node = closure(node);
			Logger.getGlobal().log(Level.FINE,
					node + "\t" + head + "\t" + tail);
			if (head == null) {
				head = tail = node;
			} else {
				tail.next = node;
				tail = node;
			}
		}
		if (head == null) { return end; }
		Logger.getGlobal().log(Level.FINE, "Tree");
		tail.next = end;
		root = tail; // double return
		return head;
	}
	private Node enhancedRegex(boolean caretted) {
		Logger.getGlobal().log(Level.FINE, "eRE");
		int ch = codepoints.next();
		switch (type.classify(ch)) {
			case OPEN_PAREN:
				if (caretted)
					throw codepoints
							.error("Carets cannot preceed a parenthesis in an enregex assertion");
				return new EnregexOpenParen(ch);
			case CLOSE_PAREN:
				if (caretted)
					throw codepoints
							.error("Carets cannot preceed a parenthesis in an enregex assertion");
				return new EnregexCloseParen(ch);
			case CLOSE_QUOTE:
				int matching = type.matching(ch);
				return new EnregexQuote(caretted ? ch != matching
						: ch == matching, matching);
			case OPEN_QUOTE:
				return new EnregexQuote(!caretted, ch);
			case CARET:
				if (caretted)
					throw codepoints
							.error("Multiple carets have no meaning in an enregex assertion");
				Node n = enhancedRegex(true);
				return n;
			case ERROR:
				throw codepoints
						.error("\""
								+ (char) ch
								+ "\" is not a valid character in an enregex assertion");
		}
		throw new RuntimeException(
				"Internal Error, theoretically unreachable point in the code reached");
	}
	@SuppressWarnings("fallthrough")
	/**
	 * Parse and add a new Single or Slice.
	 */
	private Node atom() {
		int first = 0;
		int prev = -1;
		boolean hasSupplementary = false;
		int ch = codepoints.peek();
		for (;;) {
			switch (ch) {
				case '*':
				case '+':
				case '?':
				case '{':
					if (first > 1) {
						codepoints.cursor = prev; // Unwind one
												// character
						first--;
					}
					break;
				case '$':
				case '.':
				case '^':
				case '~':
				case '(':
				case '[':
				case '|':
				case ')':
					break;
				case '\\':
					ch = codepoints.nextEscaped();
					if (ch == 'p' || ch == 'P') { // Property
						if (first > 0) { // Slice is waiting; handle it
										// first
							codepoints.unread();
							break;
						} else { // No slice; just return the family node
							boolean comp = (ch == 'P');
							boolean oneLetter = true;
							ch = codepoints.next(); // Consume {
												// if
							// present
							if (ch != '{')
								codepoints.unread();
							else oneLetter = false;
							return family(oneLetter, comp);
						}
					}
					codepoints.unread();
					prev = codepoints.cursor;
					ch = escape(false, first == 0, false);
					if (ch >= 0) {
						append(ch, first);
						first++;
						if (isSupplementary(ch)) {
							hasSupplementary = true;
						}
						ch = codepoints.peek();
						continue;
					} else if (first == 0) { return root; }
					// Unwind meta escape sequence
					codepoints.cursor = prev;
					break;
				case 0:
					if (codepoints.cursor >= codepoints.patternLength) {
						break;
					}
					// Fall through
				default:
					prev = codepoints.cursor;
					append(ch, first);
					first++;
					if (isSupplementary(ch)) {
						hasSupplementary = true;
					}
					ch = codepoints.next();
					continue;
			}
			break;
		}
		if (first == 1) {
			return newSingle(buffer[0], flags);
		} else {
			return newSlice(buffer, first, hasSupplementary);
		}
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
	private int escape(boolean inclass, boolean create, boolean isrange) {
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
	private Node closure(Node prev) {
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
	private CharProperty clazz(boolean consume) {
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
			return bits.add(ch, flags);
		return newSingle(ch, flags);
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
	private CharProperty family(boolean singleLetter, boolean maybeComplement) {
		codepoints.next();
		String name;
		CharProperty node = null;
		if (singleLetter) {
			int c = codepoints.peek();
			if (!Character.isSupplementaryCodePoint(c)) {
				name = String.valueOf((char) c);
			} else {
				name = codepoints.toString(codepoints.cursor, 1);
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
			name = codepoints.toString(i, j - i - 1);
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
				codepoints.hasSupplementary = true;
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
	private String groupname(int ch) {
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
	 * Parses a group and returns the head node of a set of nodes that process
	 * the group. Sometimes a double return system is used where the tail is
	 * returned in root.
	 */
	private Node group0() {
		boolean capturingGroup = false;
		Node head = null;
		Node tail = null;
		int save = flags;
		root = null;
		int ch = codepoints.next();
		if (ch == '?') {
			ch = codepoints.skip();
			switch (ch) {
				case ':': // (?:xxx) pure group
					head = createGroup(true);
					tail = root;
					head.next = expr(tail);
					break;
				case '=': // (?=xxx) and (?!xxx) lookahead
				case '!':
					head = createGroup(true);
					tail = root;
					head.next = expr(tail);
					if (ch == '=') {
						head = tail = new Pos(head);
					} else {
						head = tail = new Neg(head);
					}
					break;
				case '>': // (?>xxx) independent group
					head = createGroup(true);
					tail = root;
					head.next = expr(tail);
					head = tail = new Ques(head, INDEPENDENT);
					break;
				case '<': // (?<xxx) look behind
					ch = codepoints.read();
					if (ASCII.isLower(ch) || ASCII.isUpper(ch)) {
						// named captured group
						String name = groupname(ch);
						if (registry.groupDefined(name))
							throw codepoints
									.error("Named capturing group <"
											+ name
											+ "> is already defined");
						capturingGroup = true;
						head = createGroup(false);
						tail = root;
						registry.addToEnd(name);
						head.next = expr(tail);
						break;
					}
					int start = codepoints.cursor;
					head = createGroup(true);
					tail = root;
					head.next = expr(tail);
					tail.next = Node.lookbehindEnd;
					TreeInfo info = new TreeInfo();
					head.study(info);
					if (info.maxValid == false) { throw codepoints
							.error("Look-behind group does not have "
									+ "an obvious maximum length"); }
					boolean hasSupplementary = findSupplementary(start,
							codepoints.patternLength);
					if (ch == '=') {
						head = tail = (hasSupplementary ? new BehindS(
								head, info.maxLength, info.minLength)
								: new Behind(head, info.maxLength,
										info.minLength));
					} else if (ch == '!') {
						head = tail = (hasSupplementary ? new NotBehindS(
								head, info.maxLength, info.minLength)
								: new NotBehind(head, info.maxLength,
										info.minLength));
					} else {
						throw codepoints
								.error("Unknown look-behind group");
					}
					break;
				case '$':
				case '@':
					throw codepoints.error("Unknown group type");
				default: // (?xxx:) inlined match flags
					codepoints.unread();
					addFlag();
					ch = codepoints.read();
					if (ch == ')') { return null; // Inline modifier only
					}
					if (ch != ':') { throw codepoints
							.error("Unknown inline modifier"); }
					head = createGroup(true);
					tail = root;
					head.next = expr(tail);
					break;
			}
		} else { // (xxx) a regular group
			capturingGroup = true;
			head = createGroup(false);
			tail = root;
			head.next = expr(tail);
		}
		codepoints.accept(')', "Unclosed group");
		flags = save;
		// Check for quantifiers
		Node node = closure(head);
		if (node == head) { // No closure
			root = tail;
			return node; // Dual return
		}
		if (head == tail) { // Zero length assertion
			root = node;
			return node; // Dual return
		}
		if (node instanceof Ques) {
			Ques ques = (Ques) node;
			if (ques.type == POSSESSIVE) {
				root = node;
				return node;
			}
			tail.next = new BranchConn();
			tail = tail.next;
			if (ques.type == GREEDY) {
				head = new Branch(head, null, tail);
			} else { // Reluctant quantifier
				head = new Branch(null, head, tail);
			}
			root = tail;
			return head;
		} else if (node instanceof Curly) {
			Curly curly = (Curly) node;
			if (curly.type == POSSESSIVE) {
				root = node;
				return node;
			}
			// Discover if the group is deterministic
			TreeInfo info = new TreeInfo();
			if (head.study(info)) { // Deterministic
				head = root = new GroupCurly(head.next, curly.cmin,
						curly.cmax, curly.type,
						((GroupTail) tail).localIndex,
						((GroupTail) tail).groupIndex, capturingGroup);
				return head;
			} else { // Non-deterministic
				int temp = ((GroupHead) head).localIndex;
				Loop loop;
				if (curly.type == GREEDY)
					loop = new Loop(this.localCount, temp);
				else // Reluctant Curly
				loop = new LazyLoop(this.localCount, temp);
				Prolog prolog = new Prolog(loop);
				this.localCount += 1;
				loop.cmin = curly.cmin;
				loop.cmax = curly.cmax;
				loop.body = head;
				tail.next = loop;
				root = loop;
				return prolog; // Dual return
			}
		}
		throw codepoints.error("Internal logic error");
	}
	/**
	 * Create group head and tail nodes using double return. If the group is
	 * created with anonymous true then it is a pure group and should not
	 * affect group counting.
	 */
	private Node createGroup(boolean anonymous) {
		int localIndex = localCount++;
		int groupIndex = 0;
		if (!anonymous) {
			groupIndex = registry.iterateCount();
		}
		GroupHead head = new GroupHead(localIndex);
		root = new GroupTail(localIndex, groupIndex);
		return head;
	}
	@SuppressWarnings("fallthrough")
	/**
	 * Parses inlined match flags and set them appropriately.
	 */
	private void addFlag() {
		int ch = codepoints.peek();
		for (;;) {
			switch (ch) {
				case 'i':
					flags |= CASE_INSENSITIVE;
					break;
				case 'm':
					flags |= MULTILINE;
					break;
				case 's':
					flags |= DOTALL;
					break;
				case 'd':
					flags |= UNIX_LINES;
					break;
				case 'u':
					flags |= UNICODE_CASE;
					break;
				case 'c':
					flags |= CANON_EQ;
					break;
				case 'x':
					flags |= COMMENTS;
					break;
				case 'U':
					flags |= (UNICODE_CHARACTER_CLASS | UNICODE_CASE);
					break;
				case '-': // subFlag then fall through
					ch = codepoints.next();
					subFlag();
				default:
					return;
			}
			ch = codepoints.next();
		}
	}
	@SuppressWarnings("fallthrough")
	/**
	 * Parses the second part of inlined match flags and turns off
	 * flags appropriately.
	 */
	private void subFlag() {
		int ch = codepoints.peek();
		for (;;) {
			switch (ch) {
				case 'i':
					flags &= ~CASE_INSENSITIVE;
					break;
				case 'm':
					flags &= ~MULTILINE;
					break;
				case 's':
					flags &= ~DOTALL;
					break;
				case 'd':
					flags &= ~UNIX_LINES;
					break;
				case 'u':
					flags &= ~UNICODE_CASE;
					break;
				case 'c':
					flags &= ~CANON_EQ;
					break;
				case 'x':
					flags &= ~COMMENTS;
					break;
				case 'U':
					flags &= ~(UNICODE_CHARACTER_CLASS | UNICODE_CASE);
				default:
					return;
			}
			ch = codepoints.next();
		}
	}
	/**
	 * Utility method for creating a string slice matcher.
	 */
	private Node newSlice(int[] buf, int count, boolean hasSupplementary) {
		int[] tmp = new int[count];
		if (has(CASE_INSENSITIVE)) {
			if (has(UNICODE_CASE)) {
				for (int i = 0; i < count; i++) {
					tmp[i] = Character.toLowerCase(Character
							.toUpperCase(buf[i]));
				}
				return hasSupplementary ? new SliceUS(tmp)
						: new SliceU(tmp);
			}
			for (int i = 0; i < count; i++) {
				tmp[i] = ASCII.toLower(buf[i]);
			}
			return hasSupplementary ? new SliceIS(tmp) : new SliceI(tmp);
		}
		for (int i = 0; i < count; i++) {
			tmp[i] = buf[i];
		}
		return hasSupplementary ? new SliceS(tmp) : new Slice(tmp);
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
		return Pattern.has(flags, flag);
	}
	/**
	 * Determines if there is any supplementary character or unpaired
	 * surrogate in the specified range.
	 */
	private boolean findSupplementary(int start, int end) {
		for (int i = start; i < end; i++) {
			if (isSupplementary(codepoints.temp[i])) return true;
		}
		return false;
	}
}
