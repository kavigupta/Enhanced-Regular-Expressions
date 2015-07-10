/*
 * Refactored out of the original openjdk.regex.Pattern class.
 */
package eredmel.regex;

import static eredmel.regex.Pattern.*;

import java.util.ArrayList;
import java.util.Arrays;

import eredmel.regex.Pattern.TreeInfo;

/**
 * Base class for all node classes. Subclasses should override the match()
 * method as appropriate. This class is an accepting node, so its match()
 * always returns true.
 */
class Node extends Object {
	Node next;
	Node() {
		next = Pattern.accept;
	}
	/**
	 * This method implements the classic accept node.
	 */
	boolean match(Matcher matcher, int i, CharSequence seq) {
		matcher.last = i;
		matcher.cacheGroup(0, Range.of(matcher.first, matcher.last));
		return true;
	}
	/**
	 * This method does nothing by default. Called whenever backtracking is
	 * needed.
	 */
	public void clean(Matcher mat) {}
	/**
	 * This method is good for all zero length assertions.
	 */
	boolean study(TreeInfo info) {
		if (next != null) {
			return next.study(info);
		} else {
			return info.deterministic;
		}
	}
	static class LastNode extends Node {
		/**
		 * This method implements the classic accept node with
		 * the addition of a check to see if the match occurred
		 * using all of the input.
		 */
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			if (matcher.acceptMode == Matcher.ENDANCHOR && i != matcher.to)
				return false;
			matcher.last = i;
			matcher.cacheGroup(0, Range.of(matcher.first, matcher.last));
			return true;
		}
	}
	/**
	 * Used for REs that can start anywhere within the input string.
	 * This basically tries to match repeatedly at each spot in the
	 * input string, moving forward after each try. An anchored search
	 * or a BnM will bypass this node completely.
	 */
	static class Start extends Node {
		int minLength;
		Start(Node node) {
			this.next = node;
			TreeInfo info = new TreeInfo();
			next.study(info);
			minLength = info.minLength;
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			if (i > matcher.to - minLength) {
				matcher.hitEnd = true;
				return false;
			}
			int guard = matcher.to - minLength;
			for (; i <= guard; i++) {
				if (next.match(matcher, i, seq)) {
					matcher.first = i;
					matcher.cacheGroup(0,
							Range.of(matcher.first, matcher.last));
					return true;
				}
			}
			matcher.hitEnd = true;
			return false;
		}
		@Override
		boolean study(TreeInfo info) {
			next.study(info);
			info.maxValid = false;
			info.deterministic = false;
			return false;
		}
	}
	/*
	 * StartS supports supplementary characters, including unpaired surrogates.
	 */
	static final class StartS extends Start {
		StartS(Node node) {
			super(node);
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			if (i > matcher.to - minLength) {
				matcher.hitEnd = true;
				return false;
			}
			int guard = matcher.to - minLength;
			while (i <= guard) {
				// if ((ret = next.match(matcher, i, seq)) || i == guard)
				if (next.match(matcher, i, seq)) {
					matcher.first = i;
					matcher.cacheGroup(0,
							Range.of(matcher.first, matcher.last));
					return true;
				}
				if (i == guard) break;
				// Optimization to move to the next character. This is
				// faster than countChars(seq, i, 1).
				if (Character.isHighSurrogate(seq.charAt(i++))) {
					if (i < seq.length()
							&& Character.isLowSurrogate(seq.charAt(i))) {
						i++;
					}
				}
			}
			matcher.hitEnd = true;
			return false;
		}
	}
	/**
	 * Node to anchor at the beginning of input. This object implements the
	 * match for a \A sequence, and the caret anchor will use this if not in
	 * multiline mode.
	 */
	static final class Begin extends Node {
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int fromIndex = (matcher.anchoringBounds) ? matcher.from : 0;
			if (i == fromIndex && next.match(matcher, i, seq)) {
				matcher.first = i;
				matcher.cacheGroup(0, Range.of(i, matcher.last));
				return true;
			} else {
				return false;
			}
		}
	}
	/**
	 * Node to anchor at the end of input. This is the absolute end, so this
	 * should not match at the last newline before the end as $ will.
	 */
	static final class End extends Node {
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int endIndex = (matcher.anchoringBounds) ? matcher.to : matcher
					.getTextLength();
			if (i == endIndex) {
				matcher.hitEnd = true;
				return next.match(matcher, i, seq);
			}
			return false;
		}
	}
	/**
	 * Node to anchor at the beginning of a line. This is essentially the
	 * object to match for the multiline ^.
	 */
	static final class Caret extends Node {
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int startIndex = matcher.from;
			int endIndex = matcher.to;
			if (!matcher.anchoringBounds) {
				startIndex = 0;
				endIndex = matcher.getTextLength();
			}
			// Perl does not match ^ at end of input even after newline
			if (i == endIndex) {
				matcher.hitEnd = true;
				return false;
			}
			if (i > startIndex) {
				char ch = seq.charAt(i - 1);
				if (ch != '\n' && ch != '\r' && (ch | 1) != '\u2029'
						&& ch != '\u0085') { return false; }
				// Should treat /r/n as one newline
				if (ch == '\r' && seq.charAt(i) == '\n') return false;
			}
			return next.match(matcher, i, seq);
		}
	}
	/**
	 * Node to anchor at the beginning of a line when in unixdot mode.
	 */
	static final class UnixCaret extends Node {
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int startIndex = matcher.from;
			int endIndex = matcher.to;
			if (!matcher.anchoringBounds) {
				startIndex = 0;
				endIndex = matcher.getTextLength();
			}
			// Perl does not match ^ at end of input even after newline
			if (i == endIndex) {
				matcher.hitEnd = true;
				return false;
			}
			if (i > startIndex) {
				char ch = seq.charAt(i - 1);
				if (ch != '\n') { return false; }
			}
			return next.match(matcher, i, seq);
		}
	}
	/**
	 * Node to match the location where the last match ended.
	 * This is used for the \G construct.
	 */
	static final class LastMatch extends Node {
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			if (i != matcher.oldLast) return false;
			return next.match(matcher, i, seq);
		}
	}
	/**
	 * Node to anchor at the end of a line or the end of input based on the
	 * multiline mode.
	 * When not in multiline mode, the $ can only match at the very end
	 * of the input, unless the input ends in a line terminator in which
	 * it matches right before the last line terminator.
	 * Note that \r\n is considered an atomic line terminator.
	 * Like ^ the $ operator matches at a position, it does not match the
	 * line terminators themselves.
	 */
	static final class Dollar extends Node {
		boolean multiline;
		Dollar(boolean mul) {
			multiline = mul;
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int endIndex = (matcher.anchoringBounds) ? matcher.to : matcher
					.getTextLength();
			if (!multiline) {
				if (i < endIndex - 2) return false;
				if (i == endIndex - 2) {
					char ch = seq.charAt(i);
					if (ch != '\r') return false;
					ch = seq.charAt(i + 1);
					if (ch != '\n') return false;
				}
			}
			// Matches before any line terminator; also matches at the
			// end of input
			// Before line terminator:
			// If multiline, we match here no matter what
			// If not multiline, fall through so that the end
			// is marked as hit; this must be a /r/n or a /n
			// at the very end so the end was hit; more input
			// could make this not match here
			if (i < endIndex) {
				char ch = seq.charAt(i);
				if (ch == '\n') {
					// No match between \r\n
					if (i > 0 && seq.charAt(i - 1) == '\r') return false;
					if (multiline) return next.match(matcher, i, seq);
				} else if (ch == '\r' || ch == '\u0085'
						|| (ch | 1) == '\u2029') {
					if (multiline) return next.match(matcher, i, seq);
				} else { // No line terminator, no match
					return false;
				}
			}
			// Matched at current end so hit end
			matcher.hitEnd = true;
			// If a $ matches because of end of input, then more input
			// could cause it to fail!
			matcher.requireEnd = true;
			return next.match(matcher, i, seq);
		}
		@Override
		boolean study(TreeInfo info) {
			next.study(info);
			return info.deterministic;
		}
	}
	/**
	 * Node to anchor at the end of a line or the end of input based on the
	 * multiline mode when in unix lines mode.
	 */
	static final class UnixDollar extends Node {
		boolean multiline;
		UnixDollar(boolean mul) {
			multiline = mul;
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int endIndex = (matcher.anchoringBounds) ? matcher.to : matcher
					.getTextLength();
			if (i < endIndex) {
				char ch = seq.charAt(i);
				if (ch == '\n') {
					// If not multiline, then only possible to
					// match at very end or one before end
					if (multiline == false && i != endIndex - 1)
						return false;
					// If multiline return next.match without setting
					// matcher.hitEnd
					if (multiline) return next.match(matcher, i, seq);
				} else {
					return false;
				}
			}
			// Matching because at the end or 1 before the end;
			// more input could change this so set hitEnd
			matcher.hitEnd = true;
			// If a $ matches because of end of input, then more input
			// could cause it to fail!
			matcher.requireEnd = true;
			return next.match(matcher, i, seq);
		}
		@Override
		boolean study(TreeInfo info) {
			next.study(info);
			return info.deterministic;
		}
	}
	/**
	 * Node class that matches a Unicode line ending '\R'
	 */
	static final class LineEnding extends Node {
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			// (u+000Du+000A|[u+000Au+000Bu+000Cu+000Du+0085u+2028u+2029])
			if (i < matcher.to) {
				int ch = seq.charAt(i);
				if (ch == 0x0A || ch == 0x0B || ch == 0x0C || ch == 0x85
						|| ch == 0x2028 || ch == 0x2029)
					return next.match(matcher, i + 1, seq);
				if (ch == 0x0D) {
					i++;
					if (i < matcher.to && seq.charAt(i) == 0x0A) i++;
					return next.match(matcher, i, seq);
				}
			} else {
				matcher.hitEnd = true;
			}
			return false;
		}
		@Override
		boolean study(TreeInfo info) {
			info.minLength++;
			info.maxLength += 2;
			return next.study(info);
		}
	}
	/**
	 * The 0 or 1 quantifier. This one class implements all three types.
	 */
	static final class Ques extends Node {
		Node atom;
		int type;
		Ques(Node node, int type) {
			this.atom = node;
			this.type = type;
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			switch (type) {
				case GREEDY:
					return (atom.match(matcher, i, seq) && next.match(
							matcher, matcher.last, seq))
							|| next.match(matcher, i, seq);
				case LAZY:
					return next.match(matcher, i, seq)
							|| (atom.match(matcher, i, seq) && next
									.match(matcher, matcher.last, seq));
				case POSSESSIVE:
					if (atom.match(matcher, i, seq)) i = matcher.last;
					return next.match(matcher, i, seq);
				default:
					return atom.match(matcher, i, seq)
							&& next.match(matcher, matcher.last, seq);
			}
		}
		@Override
		boolean study(TreeInfo info) {
			if (type != INDEPENDENT) {
				int minL = info.minLength;
				atom.study(info);
				info.minLength = minL;
				info.deterministic = false;
				return next.study(info);
			} else {
				atom.study(info);
				return next.study(info);
			}
		}
	}
	/**
	 * Handles the curly-brace style repetition with a specified minimum and
	 * maximum occurrences. The * quantifier is handled as a special case.
	 * This class handles the three types.
	 */
	static final class Curly extends Node {
		Node atom;
		int type;
		int cmin;
		int cmax;
		Curly(Node node, int cmin, int cmax, int type) {
			this.atom = node;
			this.type = type;
			this.cmin = cmin;
			this.cmax = cmax;
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int j;
			for (j = 0; j < cmin; j++) {
				if (atom.match(matcher, i, seq)) {
					i = matcher.last;
					continue;
				}
				return false;
			}
			if (type == GREEDY)
				return match0(matcher, i, j, seq);
			else if (type == LAZY)
				return match1(matcher, i, j, seq);
			else return match2(matcher, i, j, seq);
		}
		// Greedy match.
		// i is the index to start matching at
		// j is the number of atoms that have matched
		boolean match0(Matcher matcher, int i, int numberMatched,
				CharSequence seq) {
			if (numberMatched >= cmax) {
				// We have matched the maximum... continue with the rest of
				// the regular expression
				return next.match(matcher, i, seq);
			}
			int backLimit = numberMatched;
			while (atom.match(matcher, i, seq)) {
				// k is the length of this match
				int matchLen = matcher.last - i;
				if (matchLen == 0) // Zero length match
					break;
				// Move up index and number matched
				i = matcher.last;
				numberMatched++;
				// We are greedy so match as many as we can
				while (numberMatched < cmax) {
					if (!atom.match(matcher, i, seq)) break;
					if (i + matchLen != matcher.last) {
						if (match0(matcher, matcher.last,
								numberMatched + 1, seq)) return true;
						break;
					}
					i += matchLen;
					numberMatched++;
				}
				// Handle backing off if match fails
				while (numberMatched >= backLimit) {
					if (next.match(matcher, i, seq)) return true;
					i -= matchLen;
					numberMatched--;
					atom.clean(matcher);
				}
				return false;
			}
			return next.match(matcher, i, seq);
		}
		// Reluctant match. At this point, the minimum has been satisfied.
		// i is the index to start matching at
		// j is the number of atoms that have matched
		boolean match1(Matcher matcher, int i, int j, CharSequence seq) {
			for (;;) {
				// Try finishing match without consuming any more
				if (next.match(matcher, i, seq)) return true;
				// At the maximum, no match found
				if (j >= cmax) return false;
				// Okay, must try one more atom
				if (!atom.match(matcher, i, seq)) return false;
				// If we haven't moved forward then must break out
				if (i == matcher.last) return false;
				// Move up index and number matched
				i = matcher.last;
				j++;
			}
		}
		boolean match2(Matcher matcher, int i, int j, CharSequence seq) {
			for (; j < cmax; j++) {
				if (!atom.match(matcher, i, seq)) break;
				if (i == matcher.last) break;
				i = matcher.last;
			}
			return next.match(matcher, i, seq);
		}
		@Override
		boolean study(TreeInfo info) {
			// Save original info
			int minL = info.minLength;
			int maxL = info.maxLength;
			boolean maxV = info.maxValid;
			boolean detm = info.deterministic;
			info.reset();
			atom.study(info);
			int temp = info.minLength * cmin + minL;
			if (temp < minL) {
				temp = 0xFFFFFFF; // arbitrary large number
			}
			info.minLength = temp;
			if (maxV & info.maxValid) {
				temp = info.maxLength * cmax + maxL;
				info.maxLength = temp;
				if (temp < maxL) {
					info.maxValid = false;
				}
			} else {
				info.maxValid = false;
			}
			if (info.deterministic && cmin == cmax)
				info.deterministic = detm;
			else info.deterministic = false;
			return next.study(info);
		}
	}
	/**
	 * Handles the curly-brace style repetition with a specified minimum and
	 * maximum occurrences in deterministic cases. This is an iterative
	 * optimization over the Prolog and Loop system which would handle this
	 * in a recursive way. The * quantifier is handled as a special case.
	 * If capture is true then this class saves group settings and ensures
	 * that groups are unset when backing off of a group match.
	 */
	static final class GroupCurly extends Node {
		Node atom;
		int type;
		int cmin;
		int cmax;
		int localIndex;
		int groupIndex;
		private final boolean capture;
		GroupCurly(Node node, int cmin, int cmax, int type, int local,
				int group, boolean capture) {
			this.atom = node;
			this.type = type;
			this.cmin = cmin;
			this.cmax = cmax;
			this.localIndex = local;
			this.groupIndex = group;
			this.capture = capture;
		}
		@SuppressWarnings("unchecked")
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			ArrayList<Range>[] save = null;
			int[] locals = matcher.locals;
			int save0 = locals[localIndex];
			if (capture) {
				save = new ArrayList[matcher.groupsr.length];
				for (int j = 0; j < matcher.groupsr.length; j++) {
					save[j] = (ArrayList<Range>) matcher.groupsr[j]
							.clone();
				}
			}
			// Notify GroupTail there is no need to setup group info
			// because it will be set here
			locals[localIndex] = -1;
			boolean ret = true;
			for (int j = 0; j < cmin; j++) {
				if (atom.match(matcher, i, seq)) {
					if (capture) {
						matcher.cacheGroup(groupIndex / 2,
								Range.of(i, matcher.last));
					}
					i = matcher.last;
				} else {
					ret = false;
					break;
				}
			}
			if (ret) {
				if (type == GREEDY) {
					ret = match0(matcher, i, cmin, seq);
				} else if (type == LAZY) {
					ret = match1(matcher, i, cmin, seq);
				} else {
					ret = match2(matcher, i, cmin, seq);
				}
			}
			if (!ret) {
				locals[localIndex] = save0;
				if (capture) {
					matcher.groupsr = save;
				}
			}
			return ret;
		}
		// Aggressive group match
		boolean match0(Matcher matcher, int i, int j, CharSequence seq) {
			// don't back off passing the starting "j"
			int min = j;
			for (;;) {
				if (j >= cmax) break;
				if (!atom.match(matcher, i, seq)) break;
				int k = matcher.last - i;
				if (k <= 0) {
					if (capture) {
						matcher.cacheGroup(groupIndex / 2,
								Range.of(i, i + k));
					}
					i = i + k;
					break;
				}
				for (;;) {
					if (capture) {
						matcher.cacheGroup(groupIndex / 2,
								Range.of(i, i + k));
					}
					i = i + k;
					if (++j >= cmax) break;
					if (!atom.match(matcher, i, seq)) break;
					if (i + k != matcher.last) {
						if (match0(matcher, i, j, seq)) return true;
						break;
					}
				}
				while (j > min) {
					if (next.match(matcher, i, seq)) { return true; }
					// backing off
					i = i - k;
					if (capture) {
						clean(matcher);
					}
					j--;
				}
				break;
			}
			return next.match(matcher, i, seq);
		}
		// Reluctant matching
		boolean match1(Matcher matcher, int i, int j, CharSequence seq) {
			for (;;) {
				if (next.match(matcher, i, seq)) return true;
				if (j >= cmax) return false;
				if (!atom.match(matcher, i, seq)) return false;
				if (i == matcher.last) return false;
				if (capture) {
					matcher.cacheGroup(0, Range.of(i, matcher.last));
				}
				i = matcher.last;
				j++;
			}
		}
		// Possessive matching
		boolean match2(Matcher matcher, int i, int j, CharSequence seq) {
			for (; j < cmax; j++) {
				if (!atom.match(matcher, i, seq)) {
					break;
				}
				if (capture) {
					matcher.cacheGroup(0, Range.of(i, matcher.last));
				}
				if (i == matcher.last) {
					break;
				}
				i = matcher.last;
			}
			return next.match(matcher, i, seq);
		}
		@Override
		public void clean(Matcher mat) {
			System.out.println(Arrays.toString(mat.groupsr));
			mat.removeGroup(groupIndex / 2);
			System.out.println("THIS: " + this);
			System.out.println("NEXT: " + next);
			for (Node n = atom; n != null && n.next.next != null; n = n.next) {
				System.out.println("Cleaning " + n);
				System.out.println(Arrays.toString(mat.groupsr));
				n.clean(mat);
			}
			System.out.println(Arrays.toString(mat.groupsr));
		}
		@Override
		boolean study(TreeInfo info) {
			// Save original info
			int minL = info.minLength;
			int maxL = info.maxLength;
			boolean maxV = info.maxValid;
			boolean detm = info.deterministic;
			info.reset();
			atom.study(info);
			int temp = info.minLength * cmin + minL;
			if (temp < minL) {
				temp = 0xFFFFFFF; // Arbitrary large number
			}
			info.minLength = temp;
			if (maxV & info.maxValid) {
				temp = info.maxLength * cmax + maxL;
				info.maxLength = temp;
				if (temp < maxL) {
					info.maxValid = false;
				}
			} else {
				info.maxValid = false;
			}
			if (info.deterministic && cmin == cmax) {
				info.deterministic = detm;
			} else {
				info.deterministic = false;
			}
			return next.study(info);
		}
	}
	/**
	 * A Guard node at the end of each atom node in a Branch. It
	 * serves the purpose of chaining the "match" operation to
	 * "next" but not the "study", so we can collect the TreeInfo
	 * of each atom node without including the TreeInfo of the
	 * "next".
	 */
	static final class BranchConn extends Node {
		BranchConn() {};
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			return next.match(matcher, i, seq);
		}
		@Override
		boolean study(TreeInfo info) {
			return info.deterministic;
		}
	}
	/**
	 * Handles the branching of alternations. Note this is also used for
	 * the ? quantifier to branch between the case where it matches once
	 * and where it does not occur.
	 */
	static final class Branch extends Node {
		Node[] atoms = new Node[2];
		int size = 2;
		Node conn;
		Branch(Node first, Node second, Node branchConn) {
			conn = branchConn;
			atoms[0] = first;
			atoms[1] = second;
		}
		void add(Node node) {
			if (size >= atoms.length) {
				Node[] tmp = new Node[atoms.length * 2];
				System.arraycopy(atoms, 0, tmp, 0, atoms.length);
				atoms = tmp;
			}
			atoms[size++] = node;
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			for (int n = 0; n < size; n++) {
				if (atoms[n] == null) {
					if (conn.next.match(matcher, i, seq)) return true;
				} else if (atoms[n].match(matcher, i, seq)) { return true; }
			}
			return false;
		}
		@Override
		boolean study(TreeInfo info) {
			int minL = info.minLength;
			int maxL = info.maxLength;
			boolean maxV = info.maxValid;
			int minL2 = Integer.MAX_VALUE; // arbitrary large enough num
			int maxL2 = -1;
			for (int n = 0; n < size; n++) {
				info.reset();
				if (atoms[n] != null) atoms[n].study(info);
				minL2 = Math.min(minL2, info.minLength);
				maxL2 = Math.max(maxL2, info.maxLength);
				maxV = (maxV & info.maxValid);
			}
			minL += minL2;
			maxL += maxL2;
			info.reset();
			conn.next.study(info);
			info.minLength += minL;
			info.maxLength += maxL;
			info.maxValid &= maxV;
			info.deterministic = false;
			return false;
		}
	}
	/**
	 * The GroupHead saves the location where the group begins in the locals
	 * and restores them when the match is done.
	 * The matchRef is used when a reference to this group is accessed later
	 * in the expression. The locals will have a negative value in them to
	 * indicate that we do not want to unset the group if the reference
	 * doesn't match.
	 */
	static final class GroupHead extends Node {
		int localIndex;
		GroupHead(int localCount) {
			localIndex = localCount;
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int save = matcher.locals[localIndex];
			matcher.locals[localIndex] = i;
			boolean ret = next.match(matcher, i, seq);
			matcher.locals[localIndex] = save;
			return ret;
		}
		boolean matchRef(Matcher matcher, int i, CharSequence seq) {
			int save = matcher.locals[localIndex];
			matcher.locals[localIndex] = ~i; // HACK
			boolean ret = next.match(matcher, i, seq);
			matcher.locals[localIndex] = save;
			return ret;
		}
	}
	/**
	 * Recursive reference to a group in the regular expression. It calls
	 * matchRef because if the reference fails to match we would not unset
	 * the group.
	 */
	static final class GroupRef extends Node {
		GroupHead head;
		GroupRef(GroupHead head) {
			this.head = head;
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			return head.matchRef(matcher, i, seq)
					&& next.match(matcher, matcher.last, seq);
		}
		@Override
		boolean study(TreeInfo info) {
			info.maxValid = false;
			info.deterministic = false;
			return next.study(info);
		}
	}
	/**
	 * The GroupTail handles the setting of group beginning and ending
	 * locations when groups are successfully matched. It must also be able to
	 * unset groups that have to be backed off of.
	 * The GroupTail node is also used when a previous group is referenced,
	 * and in that case no group information needs to be set.
	 */
	static final class GroupTail extends Node {
		int localIndex;
		int groupIndex;
		GroupTail(int localCount, int groupCount) {
			localIndex = localCount;
			groupIndex = groupCount + groupCount;
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int tmp = matcher.locals[localIndex];
			if (tmp >= 0) { // This is the normal group case.
				// Save the group so we can unset it if it
				// backs off of a match.
				matcher.cacheGroup(groupIndex / 2, Range.of(tmp, i));
				if (next.match(matcher, i, seq)) return true;
				matcher.removeGroup(groupIndex / 2);
				return false;
			} else {
				// This is a group reference case. We don't need to save any
				// group info because it isn't really a group.
				matcher.last = i;
				return true;
			}
		}
		@Override
		public void clean(Matcher mat) {
			mat.removeGroup(groupIndex / 2);
		}
	}
	/**
	 * This sets up a loop to handle a recursive quantifier structure.
	 */
	static final class Prolog extends Node {
		Loop loop;
		Prolog(Loop loop) {
			this.loop = loop;
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			return loop.matchInit(matcher, i, seq);
		}
		@Override
		boolean study(TreeInfo info) {
			return loop.study(info);
		}
	}
	/**
	 * Handles the repetition count for a greedy Curly. The matchInit
	 * is called from the Prolog to save the index of where the group
	 * beginning is stored. A zero length group check occurs in the
	 * normal match but is skipped in the matchInit.
	 */
	static class Loop extends Node {
		Node body;
		int countIndex; // local count index in matcher locals
		int beginIndex; // group beginning index
		int cmin, cmax;
		Loop(int countIndex, int beginIndex) {
			this.countIndex = countIndex;
			this.beginIndex = beginIndex;
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			// Avoid infinite loop in zero-length case.
			if (i > matcher.locals[beginIndex]) {
				int count = matcher.locals[countIndex];
				// This block is for before we reach the minimum
				// iterations required for the loop to match
				if (count < cmin) {
					matcher.locals[countIndex] = count + 1;
					boolean b = body.match(matcher, i, seq);
					// If match failed we must backtrack, so
					// the loop count should NOT be incremented
					if (!b) matcher.locals[countIndex] = count;
					// Return success or failure since we are under
					// minimum
					return b;
				}
				// This block is for after we have the minimum
				// iterations required for the loop to match
				if (count < cmax) {
					matcher.locals[countIndex] = count + 1;
					boolean b = body.match(matcher, i, seq);
					// If match failed we must backtrack, so
					// the loop count should NOT be incremented
					if (!b)
						matcher.locals[countIndex] = count;
					else return true;
				}
			}
			return next.match(matcher, i, seq);
		}
		boolean matchInit(Matcher matcher, int i, CharSequence seq) {
			int save = matcher.locals[countIndex];
			boolean ret = false;
			if (0 < cmin) {
				matcher.locals[countIndex] = 1;
				ret = body.match(matcher, i, seq);
			} else if (0 < cmax) {
				matcher.locals[countIndex] = 1;
				ret = body.match(matcher, i, seq);
				if (ret == false) ret = next.match(matcher, i, seq);
			} else {
				ret = next.match(matcher, i, seq);
			}
			matcher.locals[countIndex] = save;
			return ret;
		}
		@Override
		boolean study(TreeInfo info) {
			info.maxValid = false;
			info.deterministic = false;
			return false;
		}
	}
	/**
	 * Handles the repetition count for a reluctant Curly. The matchInit
	 * is called from the Prolog to save the index of where the group
	 * beginning is stored. A zero length group check occurs in the
	 * normal match but is skipped in the matchInit.
	 */
	static final class LazyLoop extends Loop {
		LazyLoop(int countIndex, int beginIndex) {
			super(countIndex, beginIndex);
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			// Check for zero length group
			if (i > matcher.locals[beginIndex]) {
				int count = matcher.locals[countIndex];
				if (count < cmin) {
					matcher.locals[countIndex] = count + 1;
					boolean result = body.match(matcher, i, seq);
					// If match failed we must backtrack, so
					// the loop count should NOT be incremented
					if (!result) matcher.locals[countIndex] = count;
					return result;
				}
				if (next.match(matcher, i, seq)) return true;
				if (count < cmax) {
					matcher.locals[countIndex] = count + 1;
					boolean result = body.match(matcher, i, seq);
					// If match failed we must backtrack, so
					// the loop count should NOT be incremented
					if (!result) matcher.locals[countIndex] = count;
					return result;
				}
				return false;
			}
			return next.match(matcher, i, seq);
		}
		@Override
		boolean matchInit(Matcher matcher, int i, CharSequence seq) {
			int save = matcher.locals[countIndex];
			boolean ret = false;
			if (0 < cmin) {
				matcher.locals[countIndex] = 1;
				ret = body.match(matcher, i, seq);
			} else if (next.match(matcher, i, seq)) {
				ret = true;
			} else if (0 < cmax) {
				matcher.locals[countIndex] = 1;
				ret = body.match(matcher, i, seq);
			}
			matcher.locals[countIndex] = save;
			return ret;
		}
		@Override
		boolean study(TreeInfo info) {
			info.maxValid = false;
			info.deterministic = false;
			return false;
		}
	}
	/**
	 * Refers to a group in the regular expression. Attempts to match
	 * whatever the group referred to last matched.
	 */
	static class BackRef extends Node {
		int groupIndex;
		BackRef(int groupCount) {
			super();
			groupIndex = groupCount + groupCount;
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int j, k;
			try {
				j = matcher.start(groupIndex / 2);
				k = matcher.end(groupIndex / 2);
			} catch (IndexOutOfBoundsException e) {
				j = k = -1;
			}
			int groupSize = k - j;
			// If the referenced group didn't match, neither can this
			if (j < 0) return false;
			// If there isn't enough input left no match
			if (i + groupSize > matcher.to) {
				matcher.hitEnd = true;
				return false;
			}
			// Check each new char to make sure it matches what the group
			// referenced matched last time around
			for (int index = 0; index < groupSize; index++)
				if (seq.charAt(i + index) != seq.charAt(j + index))
					return false;
			return next.match(matcher, i + groupSize, seq);
		}
		@Override
		boolean study(TreeInfo info) {
			info.maxValid = false;
			return next.study(info);
		}
	}
	static class CIBackRef extends Node {
		int groupIndex;
		boolean doUnicodeCase;
		CIBackRef(int groupCount, boolean doUnicodeCase) {
			super();
			groupIndex = groupCount + groupCount;
			this.doUnicodeCase = doUnicodeCase;
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int j = matcher.start(groupIndex / 2);
			int k = matcher.end(groupIndex / 2);
			int groupSize = k - j;
			// If the referenced group didn't match, neither can this
			if (j < 0) return false;
			// If there isn't enough input left no match
			if (i + groupSize > matcher.to) {
				matcher.hitEnd = true;
				return false;
			}
			// Check each new char to make sure it matches what the group
			// referenced matched last time around
			int x = i;
			for (int index = 0; index < groupSize; index++) {
				int c1 = Character.codePointAt(seq, x);
				int c2 = Character.codePointAt(seq, j);
				if (c1 != c2) {
					if (doUnicodeCase) {
						int cc1 = Character.toUpperCase(c1);
						int cc2 = Character.toUpperCase(c2);
						if (cc1 != cc2
								&& Character.toLowerCase(cc1) != Character
										.toLowerCase(cc2))
							return false;
					} else {
						if (ASCII.toLower(c1) != ASCII.toLower(c2))
							return false;
					}
				}
				x += Character.charCount(c1);
				j += Character.charCount(c2);
			}
			return next.match(matcher, i + groupSize, seq);
		}
		@Override
		boolean study(TreeInfo info) {
			info.maxValid = false;
			return next.study(info);
		}
	}
	/**
	 * Searches until the next instance of its atom. This is useful for
	 * finding the atom efficiently without passing an instance of it
	 * (greedy problem) and without a lot of wasted search time (reluctant
	 * problem).
	 */
	static final class First extends Node {
		Node atom;
		First(Node node) {
			this.atom = BnM.optimize(node);
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			if (atom instanceof BnM) { return atom.match(matcher, i, seq)
					&& next.match(matcher, matcher.last, seq); }
			for (;;) {
				if (i > matcher.to) {
					matcher.hitEnd = true;
					return false;
				}
				if (atom.match(matcher, i, seq)) { return next.match(
						matcher, matcher.last, seq); }
				i += countChars(seq, i, 1);
				matcher.first++;
			}
		}
		@Override
		boolean study(TreeInfo info) {
			atom.study(info);
			info.maxValid = false;
			info.deterministic = false;
			return next.study(info);
		}
	}
	static final class Conditional extends Node {
		Node cond, yes, not;
		Conditional(Node cond, Node yes, Node not) {
			this.cond = cond;
			this.yes = yes;
			this.not = not;
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			if (cond.match(matcher, i, seq)) {
				return yes.match(matcher, i, seq);
			} else {
				return not.match(matcher, i, seq);
			}
		}
		@Override
		boolean study(TreeInfo info) {
			int minL = info.minLength;
			int maxL = info.maxLength;
			boolean maxV = info.maxValid;
			info.reset();
			yes.study(info);
			int minL2 = info.minLength;
			int maxL2 = info.maxLength;
			boolean maxV2 = info.maxValid;
			info.reset();
			not.study(info);
			info.minLength = minL + Math.min(minL2, info.minLength);
			info.maxLength = maxL + Math.max(maxL2, info.maxLength);
			info.maxValid = (maxV & maxV2 & info.maxValid);
			info.deterministic = false;
			return next.study(info);
		}
	}
	/**
	 * Zero width positive lookahead.
	 */
	static final class Pos extends Node {
		Node cond;
		Pos(Node cond) {
			this.cond = cond;
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int savedTo = matcher.to;
			boolean conditionMatched = false;
			// Relax transparent region boundaries for lookahead
			if (matcher.transparentBounds)
				matcher.to = matcher.getTextLength();
			try {
				conditionMatched = cond.match(matcher, i, seq);
			} finally {
				// Reinstate region boundaries
				matcher.to = savedTo;
			}
			return conditionMatched && next.match(matcher, i, seq);
		}
	}
	/**
	 * Zero width negative lookahead.
	 */
	static final class Neg extends Node {
		Node cond;
		Neg(Node cond) {
			this.cond = cond;
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int savedTo = matcher.to;
			boolean conditionMatched = false;
			// Relax transparent region boundaries for lookahead
			if (matcher.transparentBounds)
				matcher.to = matcher.getTextLength();
			try {
				if (i < matcher.to) {
					conditionMatched = !cond.match(matcher, i, seq);
				} else {
					// If a negative lookahead succeeds then more input
					// could cause it to fail!
					matcher.requireEnd = true;
					conditionMatched = !cond.match(matcher, i, seq);
				}
			} finally {
				// Reinstate region boundaries
				matcher.to = savedTo;
			}
			return conditionMatched && next.match(matcher, i, seq);
		}
	}
	/**
	 * For use with lookbehinds; matches the position where the lookbehind
	 * was encountered.
	 */
	static Node lookbehindEnd = new Node() {
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			return i == matcher.lookbehindTo;
		}
	};
	/**
	 * Zero width positive lookbehind.
	 */
	static class Behind extends Node {
		Node cond;
		int rmax, rmin;
		Behind(Node cond, int rmax, int rmin) {
			this.cond = cond;
			this.rmax = rmax;
			this.rmin = rmin;
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int savedFrom = matcher.from;
			boolean conditionMatched = false;
			int startIndex = (!matcher.transparentBounds) ? matcher.from : 0;
			int from = Math.max(i - rmax, startIndex);
			// Set end boundary
			int savedLBT = matcher.lookbehindTo;
			matcher.lookbehindTo = i;
			// Relax transparent region boundaries for lookbehind
			if (matcher.transparentBounds) matcher.from = 0;
			for (int j = i - rmin; !conditionMatched && j >= from; j--) {
				conditionMatched = cond.match(matcher, j, seq);
			}
			matcher.from = savedFrom;
			matcher.lookbehindTo = savedLBT;
			return conditionMatched && next.match(matcher, i, seq);
		}
	}
	/**
	 * Zero width positive lookbehind, including supplementary
	 * characters or unpaired surrogates.
	 */
	static final class BehindS extends Behind {
		BehindS(Node cond, int rmax, int rmin) {
			super(cond, rmax, rmin);
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int rmaxChars = countChars(seq, i, -rmax);
			int rminChars = countChars(seq, i, -rmin);
			int savedFrom = matcher.from;
			int startIndex = (!matcher.transparentBounds) ? matcher.from : 0;
			boolean conditionMatched = false;
			int from = Math.max(i - rmaxChars, startIndex);
			// Set end boundary
			int savedLBT = matcher.lookbehindTo;
			matcher.lookbehindTo = i;
			// Relax transparent region boundaries for lookbehind
			if (matcher.transparentBounds) matcher.from = 0;
			for (int j = i - rminChars; !conditionMatched && j >= from; j -= j > from ? countChars(
					seq, j, -1) : 1) {
				conditionMatched = cond.match(matcher, j, seq);
			}
			matcher.from = savedFrom;
			matcher.lookbehindTo = savedLBT;
			return conditionMatched && next.match(matcher, i, seq);
		}
	}
	/**
	 * Zero width negative lookbehind.
	 */
	static class NotBehind extends Node {
		Node cond;
		int rmax, rmin;
		NotBehind(Node cond, int rmax, int rmin) {
			this.cond = cond;
			this.rmax = rmax;
			this.rmin = rmin;
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int savedLBT = matcher.lookbehindTo;
			int savedFrom = matcher.from;
			boolean conditionMatched = false;
			int startIndex = (!matcher.transparentBounds) ? matcher.from : 0;
			int from = Math.max(i - rmax, startIndex);
			matcher.lookbehindTo = i;
			// Relax transparent region boundaries for lookbehind
			if (matcher.transparentBounds) matcher.from = 0;
			for (int j = i - rmin; !conditionMatched && j >= from; j--) {
				conditionMatched = cond.match(matcher, j, seq);
			}
			// Reinstate region boundaries
			matcher.from = savedFrom;
			matcher.lookbehindTo = savedLBT;
			return !conditionMatched && next.match(matcher, i, seq);
		}
	}
	/**
	 * Zero width negative lookbehind, including supplementary
	 * characters or unpaired surrogates.
	 */
	static final class NotBehindS extends NotBehind {
		NotBehindS(Node cond, int rmax, int rmin) {
			super(cond, rmax, rmin);
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int rmaxChars = countChars(seq, i, -rmax);
			int rminChars = countChars(seq, i, -rmin);
			int savedFrom = matcher.from;
			int savedLBT = matcher.lookbehindTo;
			boolean conditionMatched = false;
			int startIndex = (!matcher.transparentBounds) ? matcher.from : 0;
			int from = Math.max(i - rmaxChars, startIndex);
			matcher.lookbehindTo = i;
			// Relax transparent region boundaries for lookbehind
			if (matcher.transparentBounds) matcher.from = 0;
			for (int j = i - rminChars; !conditionMatched && j >= from; j -= j > from ? countChars(
					seq, j, -1) : 1) {
				conditionMatched = cond.match(matcher, j, seq);
			}
			// Reinstate region boundaries
			matcher.from = savedFrom;
			matcher.lookbehindTo = savedLBT;
			return !conditionMatched && next.match(matcher, i, seq);
		}
	}
	/**
	 * Base class for all Slice nodes
	 */
	static class SliceNode extends Node {
		int[] buffer;
		SliceNode(int[] buf) {
			buffer = buf;
		}
		@Override
		boolean study(TreeInfo info) {
			info.minLength += buffer.length;
			info.maxLength += buffer.length;
			return next.study(info);
		}
	}
	/**
	 * Node class for a case sensitive/BMP-only sequence of literal
	 * characters.
	 */
	static final class Slice extends SliceNode {
		Slice(int[] buf) {
			super(buf);
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int[] buf = buffer;
			int len = buf.length;
			for (int j = 0; j < len; j++) {
				if ((i + j) >= matcher.to) {
					matcher.hitEnd = true;
					return false;
				}
				if (buf[j] != seq.charAt(i + j)) return false;
			}
			return next.match(matcher, i + len, seq);
		}
	}
	/**
	 * Node class for a case_insensitive/BMP-only sequence of literal
	 * characters.
	 */
	static class SliceI extends SliceNode {
		SliceI(int[] buf) {
			super(buf);
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int[] buf = buffer;
			int len = buf.length;
			for (int j = 0; j < len; j++) {
				if ((i + j) >= matcher.to) {
					matcher.hitEnd = true;
					return false;
				}
				int c = seq.charAt(i + j);
				if (buf[j] != c && buf[j] != ASCII.toLower(c))
					return false;
			}
			return next.match(matcher, i + len, seq);
		}
	}
	/**
	 * Node class for a unicode_case_insensitive/BMP-only sequence of
	 * literal characters. Uses unicode case folding.
	 */
	static final class SliceU extends SliceNode {
		SliceU(int[] buf) {
			super(buf);
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int[] buf = buffer;
			int len = buf.length;
			for (int j = 0; j < len; j++) {
				if ((i + j) >= matcher.to) {
					matcher.hitEnd = true;
					return false;
				}
				int c = seq.charAt(i + j);
				if (buf[j] != c
						&& buf[j] != Character.toLowerCase(Character
								.toUpperCase(c))) return false;
			}
			return next.match(matcher, i + len, seq);
		}
	}
	/**
	 * Node class for a case sensitive sequence of literal characters
	 * including supplementary characters.
	 */
	static final class SliceS extends SliceNode {
		SliceS(int[] buf) {
			super(buf);
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int[] buf = buffer;
			int x = i;
			for (int j = 0; j < buf.length; j++) {
				if (x >= matcher.to) {
					matcher.hitEnd = true;
					return false;
				}
				int c = Character.codePointAt(seq, x);
				if (buf[j] != c) return false;
				x += Character.charCount(c);
				if (x > matcher.to) {
					matcher.hitEnd = true;
					return false;
				}
			}
			return next.match(matcher, x, seq);
		}
	}
	/**
	 * Node class for a case insensitive sequence of literal characters
	 * including supplementary characters.
	 */
	static class SliceIS extends SliceNode {
		SliceIS(int[] buf) {
			super(buf);
		}
		int toLower(int c) {
			return ASCII.toLower(c);
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int[] buf = buffer;
			int x = i;
			for (int j = 0; j < buf.length; j++) {
				if (x >= matcher.to) {
					matcher.hitEnd = true;
					return false;
				}
				int c = Character.codePointAt(seq, x);
				if (buf[j] != c && buf[j] != toLower(c)) return false;
				x += Character.charCount(c);
				if (x > matcher.to) {
					matcher.hitEnd = true;
					return false;
				}
			}
			return next.match(matcher, x, seq);
		}
	}
	/**
	 * Node class for a case insensitive sequence of literal characters.
	 * Uses unicode case folding.
	 */
	static final class SliceUS extends SliceIS {
		SliceUS(int[] buf) {
			super(buf);
		}
		@Override
		int toLower(int c) {
			return Character.toLowerCase(Character.toUpperCase(c));
		}
	}
	/**
	 * Handles word boundaries. Includes a field to allow this one class to
	 * deal with the different types of word boundaries we can match. The word
	 * characters include underscores, letters, and digits. Non spacing marks
	 * can are also part of a word if they have a base character, otherwise
	 * they are ignored for purposes of finding word boundaries.
	 */
	static final class Bound extends Node {
		static int LEFT = 0x1;
		static int RIGHT = 0x2;
		static int BOTH = 0x3;
		static int NONE = 0x4;
		int type;
		boolean useUWORD;
		Bound(int n, boolean useUWORD) {
			type = n;
			this.useUWORD = useUWORD;
		}
		boolean isWord(int ch) {
			return useUWORD ? UnicodeProp.WORD.is(ch)
					: (ch == '_' || Character.isLetterOrDigit(ch));
		}
		int check(Matcher matcher, int i, CharSequence seq) {
			int ch;
			boolean left = false;
			int startIndex = matcher.from;
			int endIndex = matcher.to;
			if (matcher.transparentBounds) {
				startIndex = 0;
				endIndex = matcher.getTextLength();
			}
			if (i > startIndex) {
				ch = Character.codePointBefore(seq, i);
				left = (isWord(ch) || ((Character.getType(ch) == Character.NON_SPACING_MARK) && hasBaseCharacter(
						matcher, i - 1, seq)));
			}
			boolean right = false;
			if (i < endIndex) {
				ch = Character.codePointAt(seq, i);
				right = (isWord(ch) || ((Character.getType(ch) == Character.NON_SPACING_MARK) && hasBaseCharacter(
						matcher, i, seq)));
			} else {
				// Tried to access char past the end
				matcher.hitEnd = true;
				// The addition of another char could wreck a boundary
				matcher.requireEnd = true;
			}
			return ((left ^ right) ? (right ? LEFT : RIGHT) : NONE);
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			return (check(matcher, i, seq) & type) > 0
					&& next.match(matcher, i, seq);
		}
	}
	/**
	 * Attempts to match a slice in the input using the Boyer-Moore string
	 * matching algorithm. The algorithm is based on the idea that the
	 * pattern can be shifted farther ahead in the search text if it is
	 * matched right to left.
	 * <p>
	 * The pattern is compared to the input one character at a time, from the
	 * rightmost character in the pattern to the left. If the characters all
	 * match the pattern has been found. If a character does not match, the
	 * pattern is shifted right a distance that is the maximum of two
	 * functions, the bad character shift and the good suffix shift. This shift
	 * moves the attempted match position through the input more quickly than a
	 * naive one position at a time check.
	 * <p>
	 * The bad character shift is based on the character from the text that did
	 * not match. If the character does not appear in the pattern, the pattern
	 * can be shifted completely beyond the bad character. If the character
	 * does occur in the pattern, the pattern can be shifted to line the
	 * pattern up with the next occurrence of that character.
	 * <p>
	 * The good suffix shift is based on the idea that some subset on the right
	 * side of the pattern has matched. When a bad character is found, the
	 * pattern can be shifted right by the pattern length if the subset does
	 * not occur again in pattern, or by the amount of distance to the next
	 * occurrence of the subset in the pattern. Boyer-Moore search methods
	 * adapted from code by Amy Yu.
	 */
	static class BnM extends Node {
		int[] buffer;
		int[] lastOcc;
		int[] optoSft;
		/**
		 * Pre calculates arrays needed to generate the bad character
		 * shift and the good suffix shift. Only the last seven bits
		 * are used to see if chars match; This keeps the tables small
		 * and covers the heavily used ASCII range, but occasionally
		 * results in an aliased match for the bad character shift.
		 */
		static Node optimize(Node node) {
			if (!(node instanceof Slice)) { return node; }
			int[] src = ((Slice) node).buffer;
			int patternLength = src.length;
			// The BM algorithm requires a bit of overhead;
			// If the pattern is short don't use it, since
			// a shift larger than the pattern length cannot
			// be used anyway.
			if (patternLength < 4) { return node; }
			int i, j;
			int[] lastOcc = new int[128];
			int[] optoSft = new int[patternLength];
			// Precalculate part of the bad character shift
			// It is a table for where in the pattern each
			// lower 7-bit value occurs
			for (i = 0; i < patternLength; i++) {
				lastOcc[src[i] & 0x7F] = i + 1;
			}
			// Precalculate the good suffix shift
			// i is the shift amount being considered
			NEXT: for (i = patternLength; i > 0; i--) {
				// j is the beginning index of suffix being considered
				for (j = patternLength - 1; j >= i; j--) {
					// Testing for good suffix
					if (src[j] == src[j - i]) {
						// src[j..len] is a good suffix
						optoSft[j - 1] = i;
					} else {
						// No match. The array has already been
						// filled up with correct values before.
						continue NEXT;
					}
				}
				// This fills up the remaining of optoSft
				// any suffix can not have larger shift amount
				// then its sub-suffix. Why???
				while (j > 0) {
					optoSft[--j] = i;
				}
			}
			// Set the guard value because of unicode compression
			optoSft[patternLength - 1] = 1;
			if (node instanceof SliceS)
				return new BnMS(src, lastOcc, optoSft, node.next);
			return new BnM(src, lastOcc, optoSft, node.next);
		}
		BnM(int[] src, int[] lastOcc, int[] optoSft, Node next) {
			this.buffer = src;
			this.lastOcc = lastOcc;
			this.optoSft = optoSft;
			this.next = next;
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int[] src = buffer;
			int patternLength = src.length;
			int last = matcher.to - patternLength;
			// Loop over all possible match positions in text
			NEXT: while (i <= last) {
				// Loop over pattern from right to left
				for (int j = patternLength - 1; j >= 0; j--) {
					int ch = seq.charAt(i + j);
					if (ch != src[j]) {
						// Shift search to the right by the maximum of the
						// bad character shift and the good suffix shift
						i += Math.max(j + 1 - lastOcc[ch & 0x7F],
								optoSft[j]);
						continue NEXT;
					}
				}
				// Entire pattern matched starting at i
				matcher.first = i;
				boolean ret = next.match(matcher, i + patternLength, seq);
				if (ret) {
					matcher.first = i;
					matcher.cacheGroup(0,
							Range.of(matcher.first, matcher.last));
					return true;
				}
				i++;
			}
			// BnM is only used as the leading node in the unanchored case,
			// and it replaced its Start() which always searches to the end
			// if it doesn't find what it's looking for, so hitEnd is true.
			matcher.hitEnd = true;
			return false;
		}
		@Override
		boolean study(TreeInfo info) {
			info.minLength += buffer.length;
			info.maxValid = false;
			return next.study(info);
		}
	}
	/**
	 * Supplementary support version of BnM(). Unpaired surrogates are
	 * also handled by this class.
	 */
	static final class BnMS extends BnM {
		int lengthInChars;
		BnMS(int[] src, int[] lastOcc, int[] optoSft, Node next) {
			super(src, lastOcc, optoSft, next);
			for (int x = 0; x < buffer.length; x++) {
				lengthInChars += Character.charCount(buffer[x]);
			}
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			int[] src = buffer;
			int patternLength = src.length;
			int last = matcher.to - lengthInChars;
			// Loop over all possible match positions in text
			NEXT: while (i <= last) {
				// Loop over pattern from right to left
				int ch;
				for (int j = countChars(seq, i, patternLength), x = patternLength - 1; j > 0; j -= Character
						.charCount(ch), x--) {
					ch = Character.codePointBefore(seq, i + j);
					if (ch != src[x]) {
						// Shift search to the right by the maximum of the
						// bad character shift and the good suffix shift
						int n = Math.max(x + 1 - lastOcc[ch & 0x7F],
								optoSft[x]);
						i += countChars(seq, i, n);
						continue NEXT;
					}
				}
				// Entire pattern matched starting at i
				matcher.first = i;
				boolean ret = next.match(matcher, i + lengthInChars, seq);
				if (ret) {
					matcher.first = i;
					matcher.cacheGroup(0,
							Range.of(matcher.first, matcher.last));
					return true;
				}
				i += countChars(seq, i, 1);
			}
			matcher.hitEnd = true;
			return false;
		}
	}
	/**
	 * 
	 * A node representing the opening of an enregex parenthesis match.
	 * 
	 * @author Kavi Gupta
	 * 
	 */
	static final class EnregexOpenParen extends Node {
		private final int paren;
		public EnregexOpenParen(int paren) {
			this.paren = paren;
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			matcher.system.addParen(paren, i);
			return super.match(matcher, i, seq);
		}
	}
	/**
	 * 
	 * A node representing the closing of an enregex parenthesis match.
	 * 
	 * @author Kavi Gupta
	 * 
	 */
	static final class EnregexCloseParen extends Node {
		private final int paren;
		public EnregexCloseParen(int paren) {
			this.paren = paren;
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			matcher.system.parenMatches(paren, i);
			return super.match(matcher, i, seq);
		}
	}
	/**
	 * 
	 * A node representing the assertion that the given location is in or is
	 * not in a quote.
	 * 
	 * @author Kavi Gupta
	 * 
	 */
	static final class EnregexQuote extends Node {
		private final boolean positive;
		private final int quote;
		EnregexQuote(boolean positive, int quote) {
			this.positive = positive;
			this.quote = quote;
		}
		@Override
		boolean match(Matcher matcher, int i, CharSequence seq) {
			matcher.system.quoteMatches(quote, positive, i);
			return super.match(matcher, i, seq);
		}
	}
}
