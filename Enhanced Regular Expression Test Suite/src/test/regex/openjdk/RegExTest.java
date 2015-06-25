package test.regex.openjdk;

/*
 * Copyright (c) 1999, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 * 
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
/**
 * @test
 * @summary tests RegExp framework
 * @author Mike McCloskey
 * @bug 4481568 4482696 4495089 4504687 4527731 4599621 4631553 4619345
 *      4630911 4672616 4711773 4727935 4750573 4792284 4803197 4757029 4808962
 *      4872664 4803179 4892980 4900747 4945394 4938995 4979006 4994840 4997476
 *      5013885 5003322 4988891 5098443 5110268 6173522 4829857 5027748 6376940
 *      6358731 6178785 6284152 6231989 6497148 6486934 6233084 6504326 6635133
 *      6350801 6676425 6878475 6919132 6931676 6948903 7014645 7039066
 */
import static org.junit.Assert.assertTrue;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import jregex.*;

import org.junit.Test;

/**
 * This is a test class created to check the operation of
 * the Pattern and Matcher classes.
 */
public class RegExTest {
	private static Random generator = new Random();
	private static int failCount = 0;
	/**
	 * Main to interpret arguments and run several tests.
	 * 
	 * @throws Exception
	 */
	@Test
	public void generalCases() throws Exception {
		// Most of the tests are in a file
		processFile("TestCases.txt");
		// processFile("PerlCases.txt");
		processFile("BMPTestCases.txt");
		processFile("SupplementaryTestCases.txt");
		// These test many randomly generated char patterns
		bm();
		// These are hard to put into the file
		escapes();
		blankInput();
		// Substitition tests on randomly generated sequences
		substitutionBasher();
	}
	// Utility functions
	private static String getRandomAlphaString(int length) {
		StringBuffer buf = new StringBuffer(length);
		for (int i = 0; i < length; i++) {
			char randChar = (char) (97 + generator.nextInt(26));
			buf.append(randChar);
		}
		return buf.toString();
	}
	private static void check(Matcher m, String expected) {
		m.find();
		if (!m.group().equals(expected)) failCount++;
	}
	private static void check(Matcher m, String result, boolean expected) {
		m.find();
		if (m.group().equals(result) != expected) failCount++;
	}
	private static void check(Pattern p, String s, boolean expected) {
		if (p.matcher(s).find() != expected) failCount++;
	}
	private static void check(String p, String s, boolean expected) {
		Matcher matcher = new Pattern(p).matcher(s);
		if (matcher.find() != expected) failCount++;
	}
	private static void check(String p, char c, boolean expected) {
		String propertyPattern = expected ? "\\p" + p : "\\P" + p;
		Pattern pattern = new Pattern(propertyPattern);
		char[] ca = new char[1];
		ca[0] = c;
		Matcher matcher = pattern.matcher(new String(ca));
		if (!matcher.find()) failCount++;
	}
	private static void check(String p, int codePoint, boolean expected) {
		String propertyPattern = expected ? "\\p" + p : "\\P" + p;
		Pattern pattern = new Pattern(propertyPattern);
		char[] ca = Character.toChars(codePoint);
		Matcher matcher = pattern.matcher(new String(ca));
		if (!matcher.find()) failCount++;
	}
	private static void check(String p, int flag, String input, String s,
			boolean expected) {
		Pattern pattern = new Pattern(p, flag);
		Matcher matcher = pattern.matcher(input);
		if (expected)
			check(matcher, s, expected);
		else check(pattern, input, false);
	}
	private static void report(String testName) {
		int spacesToAdd = 30 - testName.length();
		StringBuffer paddedNameBuffer = new StringBuffer(testName);
		for (int i = 0; i < spacesToAdd; i++)
			paddedNameBuffer.append(" ");
		String paddedName = paddedNameBuffer.toString();
		System.err.println(paddedName + ": "
				+ (failCount == 0 ? "Passed" : "Failed(" + failCount + ")"));
		if (failCount > 0) throw new AssertionError("Failure");
		failCount = 0;
	}
	/**
	 * Converts ASCII alphabet characters [A-Za-z] in the given 's' to
	 * supplementary characters. This method does NOT fully take care
	 * of the regex syntax.
	 */
	private static String toSupplementaries(String s) {
		int length = s.length();
		StringBuffer sb = new StringBuffer(length * 2);
		for (int i = 0; i < length;) {
			char c = s.charAt(i++);
			if (c == '\\') {
				sb.append(c);
				if (i < length) {
					c = s.charAt(i++);
					sb.append(c);
					if (c == 'u') {
						// assume no syntax error
						sb.append(s.charAt(i++));
						sb.append(s.charAt(i++));
						sb.append(s.charAt(i++));
						sb.append(s.charAt(i++));
					}
				}
			} else if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
				sb.append('\ud800').append((char) ('\udc00' + c));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
	// Regular expression tests
	// This is for bug 6178785
	// Test if an expected NPE gets thrown when passing in a null argument
	private static boolean check(Runnable test) {
		try {
			test.run();
			failCount++;
			return false;
		} catch (NullPointerException npe) {
			return true;
		}
	}
	@Test
	public void nullArgumentTest() {
		check(new Runnable() {
			@Override
			public void run() {
				new Pattern(null);
			}
		});
		check(new Runnable() {
			@Override
			public void run() {
				Pattern.matches(null, null);
			}
		});
		check(new Runnable() {
			@Override
			public void run() {
				Pattern.matches("xyz", null);
			}
		});
		check(new Runnable() {
			@Override
			public void run() {
				new Pattern("xyz").matcher(null);
			}
		});
		final Matcher m = new Pattern("xyz").matcher("xyz");
		m.matches();
		// check(new Runnable() { public void run() { m.usePattern(null);}});
		report("Null Argument");
	}
	// This is for bug6635133
	// Test if surrogate pair in Unicode escapes can be handled correctly.
	@Test
	public void surrogatesInClassTest() throws Exception {
		Pattern pattern = new Pattern("[\\ud834\\udd21-\\ud834\\udd24]");
		Matcher matcher = pattern.matcher("\ud834\udd22");
		if (!matcher.find()) failCount++;
	}
	// This is for bug 4988891
	// Test toMatchResult to see that it is a copy of the Matcher
	// that is not affected by subsequent operations on the original
	@Test
	public void toMatchResultTest() throws Exception {
		Pattern pattern = new Pattern("squid");
		Matcher matcher = pattern
				.matcher("agiantsquidofdestinyasmallsquidoffate");
		matcher.find();
		int matcherStart1 = matcher.start();
		MatchResult mr = matcher;
		if (mr == matcher) failCount++;
		int resultStart1 = mr.start();
		if (matcherStart1 != resultStart1) failCount++;
		matcher.find();
		int matcherStart2 = matcher.start();
		int resultStart2 = mr.start();
		if (matcherStart2 == resultStart2) failCount++;
		if (resultStart1 != resultStart2) failCount++;
		MatchResult mr2 = matcher;
		if (mr == mr2) failCount++;
		if (mr2.start() != matcherStart2) failCount++;
		report("toMatchResult is a copy");
	}
	// This is for bug 4997476
	// It is weird code submitted by customer demonstrating a regression
	@Test
	public void wordSearchTest() throws Exception {
		String testString = new String("word1 word2 word3");
		Pattern p = new Pattern("\\b");
		Matcher m = p.matcher(testString);
		int position = 0;
		int start = 0;
		while (m.find(position)) {
			start = m.start();
			if (start == testString.length()) break;
			if (m.find(start + 1)) {
				position = m.start();
			} else {
				position = testString.length();
			}
			if (testString.substring(start, position).equals(" ")) continue;
			if (!testString.substring(start, position - 1)
					.startsWith("word")) failCount++;
		}
		report("Customer word search");
	}
	// This is for bug 4994840
	@Test
	public void caretAtEndTest() throws Exception {
		// Problem only occurs with multiline patterns
		// containing a beginning-of-line caret "^" followed
		// by an expression that also matches the empty string.
		Pattern pattern = new Pattern("^x?", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher("\r");
		matcher.find();
		matcher.find();
		report("Caret at end");
	}
	// This test is for 4979006
	// Check to see if word boundary construct properly handles unicode
	// non spacing marks
	@Test
	public void unicodeWordBoundsTest() throws Exception {
		String spaces = "  ";
		String wordChar = "a";
		String nsm = "\u030a";
		assert (Character.getType('\u030a') == Character.NON_SPACING_MARK);
		Pattern pattern = new Pattern("\\b");
		Matcher matcher = pattern.matcher("");
		// S=other B=word character N=non spacing mark .=word boundary
		// SS.BB.SS
		String input = spaces + wordChar + wordChar + spaces;
		twoFindIndexes(input, matcher, 2, 4);
		// SS.BBN.SS
		input = spaces + wordChar + wordChar + nsm + spaces;
		twoFindIndexes(input, matcher, 2, 5);
		// SS.BN.SS
		input = spaces + wordChar + nsm + spaces;
		twoFindIndexes(input, matcher, 2, 4);
		// SS.BNN.SS
		input = spaces + wordChar + nsm + nsm + spaces;
		twoFindIndexes(input, matcher, 2, 5);
		// SSN.BB.SS
		input = spaces + nsm + wordChar + wordChar + spaces;
		twoFindIndexes(input, matcher, 3, 5);
		// SS.BNB.SS
		input = spaces + wordChar + nsm + wordChar + spaces;
		twoFindIndexes(input, matcher, 2, 5);
		// SSNNSS
		input = spaces + nsm + nsm + spaces;
		matcher.setTarget(input);
		if (matcher.find()) failCount++;
		// SSN.BBN.SS
		input = spaces + nsm + wordChar + wordChar + nsm + spaces;
		twoFindIndexes(input, matcher, 3, 6);
		report("Unicode word boundary");
	}
	private static void twoFindIndexes(String input, Matcher matcher, int a,
			int b) throws Exception {
		matcher.setTarget(input);
		matcher.find();
		if (matcher.start() != a) failCount++;
		matcher.find();
		if (matcher.start() != b) failCount++;
	}
	// This test is for 6284152
	static void check(String regex, String input, String[] expected) {
		List<String> result = new ArrayList<String>();
		Pattern p = new Pattern(regex);
		Matcher m = p.matcher(input);
		while (m.find()) {
			result.add(m.group());
		}
		if (!Arrays.asList(expected).equals(result)) failCount++;
	}
	@Test
	public void lookbehindTest() throws Exception {
		// Positive
		check("(?<=%.{0,5})foo\\d",
				"%foo1\n%bar foo2\n%bar  foo3\n%blahblah foo4\nfoo5",
				new String[] { "foo1", "foo2", "foo3" });
		// boundary at end of the lookbehind sub-regex should work
		// consistently
		// with the boundary just after the lookbehind sub-regex
		check("(?<=.*\\b)foo", "abcd foo", new String[] { "foo" });
		check("(?<=.*)\\bfoo", "abcd foo", new String[] { "foo" });
		check("(?<!abc )\\bfoo", "abc foo", new String[0]);
		check("(?<!abc \\b)foo", "abc foo", new String[0]);
		// Negative
		check("(?<!%.{0,5})foo\\d",
				"%foo1\n%bar foo2\n%bar  foo3\n%blahblah foo4\nfoo5",
				new String[] { "foo4", "foo5" });
		// Positive greedy
		check("(?<=%b{1,4})foo", "%bbbbfoo", new String[] { "foo" });
		// Positive reluctant
		check("(?<=%b{1,4}?)foo", "%bbbbfoo", new String[] { "foo" });
		// supplementary
		check("(?<=%b{1,4})fo\ud800\udc00o", "%bbbbfo\ud800\udc00o",
				new String[] { "fo\ud800\udc00o" });
		check("(?<=%b{1,4}?)fo\ud800\udc00o", "%bbbbfo\ud800\udc00o",
				new String[] { "fo\ud800\udc00o" });
		check("(?<!%b{1,4})fo\ud800\udc00o", "%afo\ud800\udc00o",
				new String[] { "fo\ud800\udc00o" });
		check("(?<!%b{1,4}?)fo\ud800\udc00o", "%afo\ud800\udc00o",
				new String[] { "fo\ud800\udc00o" });
		report("Lookbehind");
	}
	// This test is for 4945394
	@Test
	public void findFromTest() throws Exception {
		String message = "This is 40 $0 message.";
		Pattern pat = new Pattern("\\$0");
		Matcher match = pat.matcher(message);
		if (!match.find()) failCount++;
		if (match.find()) failCount++;
		if (match.find()) failCount++;
		report("Check for alternating find");
	}
	// This test is for 4872664 and 4892980
	@Test
	public void negatedCharClassTest() throws Exception {
		Pattern pattern = new Pattern("[^>]");
		Matcher matcher = pattern.matcher("\u203A");
		if (!matcher.matches()) failCount++;
		pattern = new Pattern("[^fr]");
		matcher = pattern.matcher("a");
		if (!matcher.find()) failCount++;
		matcher.setTarget("\u203A");
		if (!matcher.find()) failCount++;
		String s = "for";
		String result[] = s.split("[^fr]");
		if (!result[0].equals("f")) failCount++;
		if (!result[1].equals("r")) failCount++;
		s = "f\u203Ar";
		result = s.split("[^fr]");
		if (!result[0].equals("f")) failCount++;
		if (!result[1].equals("r")) failCount++;
		// Test adding to bits, subtracting a node, then adding to bits again
		pattern = new Pattern("[^f\u203Ar]");
		matcher = pattern.matcher("a");
		if (!matcher.find()) failCount++;
		matcher.setTarget("f");
		if (matcher.find()) failCount++;
		matcher.setTarget("\u203A");
		if (matcher.find()) failCount++;
		matcher.setTarget("r");
		if (matcher.find()) failCount++;
		matcher.setTarget("\u203B");
		if (!matcher.find()) failCount++;
		// Test subtracting a node, adding to bits, subtracting again
		pattern = new Pattern("[^\u203Ar\u203B]");
		matcher = pattern.matcher("a");
		if (!matcher.find()) failCount++;
		matcher.setTarget("\u203A");
		if (matcher.find()) failCount++;
		matcher.setTarget("r");
		if (matcher.find()) failCount++;
		matcher.setTarget("\u203B");
		if (matcher.find()) failCount++;
		matcher.setTarget("\u203C");
		if (!matcher.find()) failCount++;
		report("Negated Character Class");
	}
	// This test is for 4628291
	@Test
	public void toStringTest() throws Exception {
		Pattern pattern = new Pattern("b+");
		if (pattern.toString() != "b+") failCount++;
		Matcher matcher = pattern.matcher("aaabbbccc");
		matcher.toString();
		matcher.find();
		matcher.toString();
		matcher.setTarget("");
		matcher.toString();
		report("toString");
	}
	// This test is for 4803197
	@Test
	public void escapedSegmentTest() throws Exception {
		Pattern pattern = new Pattern("\\Qdir1\\dir2\\E");
		check(pattern, "dir1\\dir2", true);
		pattern = new Pattern("\\Qdir1\\dir2\\\\E");
		check(pattern, "dir1\\dir2\\", true);
		pattern = new Pattern("(\\Qdir1\\dir2\\\\E)");
		check(pattern, "dir1\\dir2\\", true);
		// Supplementary character test
		pattern = new Pattern(toSupplementaries("\\Qdir1\\dir2\\E"));
		check(pattern, toSupplementaries("dir1\\dir2"), true);
		pattern = new Pattern(toSupplementaries("\\Qdir1\\dir2") + "\\\\E");
		check(pattern, toSupplementaries("dir1\\dir2\\"), true);
		pattern = new Pattern(toSupplementaries("(\\Qdir1\\dir2") + "\\\\E)");
		check(pattern, toSupplementaries("dir1\\dir2\\"), true);
		report("Escaped segment");
	}
	// This test is for 4792284
	@Test
	public void nonCaptureRepetitionTest() throws Exception {
		String input = "abcdefgh;";
		String[] patterns = new String[] { "(?:\\w{4})+;", "(?:\\w{8})*;",
				"(?:\\w{2}){2,4};", "(?:\\w{4}){2,};", // only matches the
				".*?(?:\\w{5})+;", // specified minimum
				".*?(?:\\w{9})*;", // number of reps - OK
				"(?:\\w{4})+?;", // lazy repetition - OK
				"(?:\\w{4})++;", // possessive repetition - OK
				"(?:\\w{2,}?)+;", // non-deterministic - OK
				"(\\w{4})+;", // capturing group - OK
		};
		for (int i = 0; i < patterns.length; i++) {
			// Check find()
			check(patterns[i], 0, input, input, true);
			// Check matches()
			Pattern p = new Pattern(patterns[i]);
			Matcher m = p.matcher(input);
			if (m.matches()) {
				if (!m.group(0).equals(input)) failCount++;
			} else {
				failCount++;
			}
		}
		report("Non capturing repetition");
	}
	// This test is for 6358731
	@Test
	public void notCapturedGroupCurlyMatchTest() throws Exception {
		Pattern pattern = new Pattern("(abc)+|(abcd)+");
		Matcher matcher = pattern.matcher("abcd");
		if (!matcher.matches() || matcher.group(1) != null
				|| !matcher.group(2).equals("abcd")) {
			failCount++;
		}
		report("Not captured GroupCurly");
	}
	// This test is for 4706545
	@Test
	public void javaCharClassTest() throws Exception {
		for (int i = 0; i < 1000; i++) {
			char c = (char) generator.nextInt();
			check("{javaLowerCase}", c, Character.isLowerCase(c));
			check("{javaUpperCase}", c, Character.isUpperCase(c));
			check("{javaUpperCase}+", c, Character.isUpperCase(c));
			check("{javaTitleCase}", c, Character.isTitleCase(c));
			check("{javaDigit}", c, Character.isDigit(c));
			check("{javaDefined}", c, Character.isDefined(c));
			check("{javaLetter}", c, Character.isLetter(c));
			check("{javaLetterOrDigit}", c, Character.isLetterOrDigit(c));
			check("{javaJavaIdentifierStart}", c,
					Character.isJavaIdentifierStart(c));
			check("{javaJavaIdentifierPart}", c,
					Character.isJavaIdentifierPart(c));
			check("{javaUnicodeIdentifierStart}", c,
					Character.isUnicodeIdentifierStart(c));
			check("{javaUnicodeIdentifierPart}", c,
					Character.isUnicodeIdentifierPart(c));
			check("{javaIdentifierIgnorable}", c,
					Character.isIdentifierIgnorable(c));
			check("{javaSpaceChar}", c, Character.isSpaceChar(c));
			check("{javaWhitespace}", c, Character.isWhitespace(c));
			check("{javaISOControl}", c, Character.isISOControl(c));
			check("{javaMirrored}", c, Character.isMirrored(c));
		}
		// Supplementary character test
		for (int i = 0; i < 1000; i++) {
			int c = generator.nextInt(Character.MAX_CODE_POINT
					- Character.MIN_SUPPLEMENTARY_CODE_POINT)
					+ Character.MIN_SUPPLEMENTARY_CODE_POINT;
			check("{javaLowerCase}", c, Character.isLowerCase(c));
			check("{javaUpperCase}", c, Character.isUpperCase(c));
			check("{javaUpperCase}+", c, Character.isUpperCase(c));
			check("{javaTitleCase}", c, Character.isTitleCase(c));
			check("{javaDigit}", c, Character.isDigit(c));
			check("{javaDefined}", c, Character.isDefined(c));
			check("{javaLetter}", c, Character.isLetter(c));
			check("{javaLetterOrDigit}", c, Character.isLetterOrDigit(c));
			check("{javaJavaIdentifierStart}", c,
					Character.isJavaIdentifierStart(c));
			check("{javaJavaIdentifierPart}", c,
					Character.isJavaIdentifierPart(c));
			check("{javaUnicodeIdentifierStart}", c,
					Character.isUnicodeIdentifierStart(c));
			check("{javaUnicodeIdentifierPart}", c,
					Character.isUnicodeIdentifierPart(c));
			check("{javaIdentifierIgnorable}", c,
					Character.isIdentifierIgnorable(c));
			check("{javaSpaceChar}", c, Character.isSpaceChar(c));
			check("{javaWhitespace}", c, Character.isWhitespace(c));
			check("{javaISOControl}", c, Character.isISOControl(c));
			check("{javaMirrored}", c, Character.isMirrored(c));
		}
		report("Java character classes");
	}
	// This test is for 4523620
	/*
	 * @Test public void numOccurrencesTest() throws Exception {
	 * Pattern pattern = new Pattern("aaa");
	 * 
	 * if (pattern.numOccurrences("aaaaaa", false) != 2)
	 * failCount++;
	 * if (pattern.numOccurrences("aaaaaa", true) != 4)
	 * failCount++;
	 * 
	 * pattern = new Pattern("^");
	 * if (pattern.numOccurrences("aaaaaa", false) != 1)
	 * failCount++;
	 * if (pattern.numOccurrences("aaaaaa", true) != 1)
	 * failCount++;
	 * 
	 * report("Number of Occurrences");
	 * }
	 */
	@Test
	public void multilineDollarTest() throws Exception {
		Pattern findCR = new Pattern("$", Pattern.MULTILINE);
		Matcher matcher = findCR.matcher("first bit\nsecond bit");
		matcher.find();
		if (matcher.start(0) != 9) failCount++;
		matcher.find();
		if (matcher.start(0) != 20) failCount++;
		// Supplementary character test
		matcher = findCR
				.matcher(toSupplementaries("first  bit\n second  bit")); // double
																// BMP
																// chars
		matcher.find();
		if (matcher.start(0) != 9 * 2) failCount++;
		matcher.find();
		if (matcher.start(0) != 20 * 2) failCount++;
		report("Multiline Dollar");
	}
	@Test
	public void reluctantRepetitionTest() throws Exception {
		Pattern p = new Pattern("1(\\s\\S+?){1,3}?[\\s,]2");
		check(p, "1 word word word 2", true);
		check(p, "1 wor wo w 2", true);
		check(p, "1 word word 2", true);
		check(p, "1 word 2", true);
		check(p, "1 wo w w 2", true);
		check(p, "1 wo w 2", true);
		check(p, "1 wor w 2", true);
		p = new Pattern("([a-z])+?c");
		Matcher m = p.matcher("ababcdefdec");
		check(m, "ababc");
		// Supplementary character test
		p = new Pattern(toSupplementaries("([a-z])+?c"));
		m = p.matcher(toSupplementaries("ababcdefdec"));
		check(m, toSupplementaries("ababc"));
		report("Reluctant Repetition");
	}
	@Test
	public void serializeTest() throws Exception {
		String patternStr = "(b)";
		String matchStr = "b";
		Pattern pattern = new Pattern(patternStr);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(pattern);
		oos.close();
		ObjectInputStream ois = new ObjectInputStream(
				new ByteArrayInputStream(baos.toByteArray()));
		Pattern serializedPattern = (Pattern) ois.readObject();
		ois.close();
		Matcher matcher = serializedPattern.matcher(matchStr);
		if (!matcher.matches()) failCount++;
		if (matcher.groupCount() != 1) failCount++;
		report("Serialization");
	}
	@Test
	public void gTest() {
		Pattern pattern = new Pattern("\\G\\w");
		Matcher matcher = pattern.matcher("abc#x#x");
		matcher.find();
		matcher.find();
		matcher.find();
		if (matcher.find()) failCount++;
		pattern = new Pattern("\\GA*");
		matcher = pattern.matcher("1A2AA3");
		matcher.find();
		if (matcher.find()) failCount++;
		pattern = new Pattern("\\GA*");
		matcher = pattern.matcher("1A2AA3");
		if (!matcher.find(1)) failCount++;
		matcher.find();
		if (matcher.find()) failCount++;
		report("\\G");
	}
	@Test
	public void zTest() {
		Pattern pattern = new Pattern("foo\\Z");
		// Positives
		check(pattern, "foo\u0085", true);
		check(pattern, "foo\u2028", true);
		check(pattern, "foo\u2029", true);
		check(pattern, "foo\n", true);
		check(pattern, "foo\r", true);
		check(pattern, "foo\r\n", true);
		// Negatives
		check(pattern, "fooo", false);
		check(pattern, "foo\n\r", false);
	}
	@Test
	public void replaceFirstTest() {
		Pattern pattern = new Pattern("(ab)(c*)");
		Replacer matcher = pattern.replacer("abccczzzabcczzzabccc");
		if (!matcher.replace("test").equals("testzzzabcczzzabccc"))
			failCount++;
		matcher = pattern.replacer("zzzabccczzzabcczzzabccczzz");
		if (!matcher.replace("test").equals("zzztestzzzabcczzzabccczzz"))
			failCount++;
		matcher = pattern.replacer("zzzabccczzzabcczzzabccczzz");
		String result = matcher.replace("$1");
		if (!result.equals("zzzabzzzabcczzzabccczzz")) failCount++;
		matcher = pattern.replacer("zzzabccczzzabcczzzabccczzz");
		result = matcher.replace("$2");
		if (!result.equals("zzzccczzzabcczzzabccczzz")) failCount++;
		pattern = new Pattern("a*");
		matcher = pattern.replacer("aaaaaaaaaa");
		if (!matcher.replace("test").equals("test")) failCount++;
		pattern = new Pattern("a+");
		matcher = pattern.replacer("zzzaaaaaaaaaa");
		if (!matcher.replace("test").equals("zzztest")) failCount++;
		// Supplementary character test
		pattern = new Pattern(toSupplementaries("(ab)(c*)"));
		matcher = pattern.replacer(toSupplementaries("abccczzzabcczzzabccc"));
		if (!matcher.replace(toSupplementaries("test")).equals(
				toSupplementaries("testzzzabcczzzabccc"))) failCount++;
		matcher = pattern
				.replacer(toSupplementaries("zzzabccczzzabcczzzabccczzz"));
		if (!matcher.replace(toSupplementaries("test")).equals(
				toSupplementaries("zzztestzzzabcczzzabccczzz")))
			failCount++;
		matcher = pattern
				.replacer(toSupplementaries("zzzabccczzzabcczzzabccczzz"));
		result = matcher.replace("$1");
		if (!result.equals(toSupplementaries("zzzabzzzabcczzzabccczzz")))
			failCount++;
		matcher = pattern
				.replacer(toSupplementaries("zzzabccczzzabcczzzabccczzz"));
		result = matcher.replace("$2");
		if (!result.equals(toSupplementaries("zzzccczzzabcczzzabccczzz")))
			failCount++;
		pattern = new Pattern(toSupplementaries("a*"));
		matcher = pattern.replacer(toSupplementaries("aaaaaaaaaa"));
		if (!matcher.replace(toSupplementaries("test")).equals(
				toSupplementaries("test"))) failCount++;
		pattern = new Pattern(toSupplementaries("a+"));
		matcher = pattern.replacer(toSupplementaries("zzzaaaaaaaaaa"));
		if (!matcher.replace(toSupplementaries("test")).equals(
				toSupplementaries("zzztest"))) failCount++;
		report("Replace First");
	}
	@Test
	public void negationTest() {
		Pattern pattern = new Pattern("[\\[@^]+");
		Matcher matcher = pattern.matcher("@@@@[[[[^^^^");
		if (!matcher.find()) failCount++;
		if (!matcher.group(0).equals("@@@@[[[[^^^^")) failCount++;
		pattern = new Pattern("[@\\[^]+");
		matcher = pattern.matcher("@@@@[[[[^^^^");
		if (!matcher.find()) failCount++;
		if (!matcher.group(0).equals("@@@@[[[[^^^^")) failCount++;
		pattern = new Pattern("[@\\[^@]+");
		matcher = pattern.matcher("@@@@[[[[^^^^");
		if (!matcher.find()) failCount++;
		if (!matcher.group(0).equals("@@@@[[[[^^^^")) failCount++;
		pattern = new Pattern("\\)");
		matcher = pattern.matcher("xxx)xxx");
		if (!matcher.find()) failCount++;
		report("Negation");
	}
	@Test
	public void ampersandTest() {
		Pattern pattern = new Pattern("[&@]+");
		check(pattern, "@@@@&&&&", true);
		pattern = new Pattern("[@&]+");
		check(pattern, "@@@@&&&&", true);
		pattern = new Pattern("[@\\&]+");
		check(pattern, "@@@@&&&&", true);
		report("Ampersand");
	}
	@Test
	public void octalTest() throws Exception {
		Pattern pattern = new Pattern("\\u0007");
		Matcher matcher = pattern.matcher("\u0007");
		if (!matcher.matches()) failCount++;
		pattern = new Pattern("\\07");
		matcher = pattern.matcher("\u0007");
		if (!matcher.matches()) failCount++;
		pattern = new Pattern("\\007");
		matcher = pattern.matcher("\u0007");
		if (!matcher.matches()) failCount++;
		pattern = new Pattern("\\0007");
		matcher = pattern.matcher("\u0007");
		if (!matcher.matches()) failCount++;
		pattern = new Pattern("\\040");
		matcher = pattern.matcher("\u0020");
		if (!matcher.matches()) failCount++;
		pattern = new Pattern("\\0403");
		matcher = pattern.matcher("\u00203");
		if (!matcher.matches()) failCount++;
		pattern = new Pattern("\\0103");
		matcher = pattern.matcher("\u0043");
		if (!matcher.matches()) failCount++;
		report("Octal");
	}
	@Test
	public void longPatternTest() throws Exception {
		try {
			new Pattern("a 32-character-long pattern xxxx");
			new Pattern("a 33-character-long pattern xxxxx");
			new Pattern("a thirty four character long regex");
			StringBuffer patternToBe = new StringBuffer(101);
			for (int i = 0; i < 100; i++)
				patternToBe.append((char) (97 + i % 26));
			new Pattern(patternToBe.toString());
		} catch (PatternSyntaxException e) {
			failCount++;
		}
		// Supplementary character test
		try {
			new Pattern(
					toSupplementaries("a 32-character-long pattern xxxx"));
			new Pattern(
					toSupplementaries("a 33-character-long pattern xxxxx"));
			new Pattern(
					toSupplementaries("a thirty four character long regex"));
			StringBuffer patternToBe = new StringBuffer(101 * 2);
			for (int i = 0; i < 100; i++)
				patternToBe.append(Character
						.toChars(Character.MIN_SUPPLEMENTARY_CODE_POINT
								+ 97 + i % 26));
			new Pattern(patternToBe.toString());
		} catch (PatternSyntaxException e) {
			failCount++;
		}
		report("LongPattern");
	}
	@Test
	public void group0Test() throws Exception {
		Pattern pattern = new Pattern("(tes)ting");
		Matcher matcher = pattern.matcher("testing");
		check(matcher, "testing");
		matcher.setTarget("testing");
		if (matcher.find()) {
			if (!matcher.group(0).equals("testing")) failCount++;
		} else {
			failCount++;
		}
		matcher.setTarget("testing");
		if (matcher.matches()) {
			if (!matcher.group(0).equals("testing")) failCount++;
		} else {
			failCount++;
		}
		pattern = new Pattern("(tes)ting");
		matcher = pattern.matcher("testing");
		if (matcher.find()) {
			if (!matcher.group(0).equals("testing")) failCount++;
		} else {
			failCount++;
		}
		pattern = new Pattern("^(tes)ting");
		matcher = pattern.matcher("testing");
		if (matcher.matches()) {
			if (!matcher.group(0).equals("testing")) failCount++;
		} else {
			failCount++;
		}
		// Supplementary character test
		pattern = new Pattern(toSupplementaries("(tes)ting"));
		matcher = pattern.matcher(toSupplementaries("testing"));
		check(matcher, toSupplementaries("testing"));
		matcher.setTarget(toSupplementaries("testing"));
		if (matcher.find()) {
			if (!matcher.group(0).equals(toSupplementaries("testing")))
				failCount++;
		} else {
			failCount++;
		}
		matcher.setTarget(toSupplementaries("testing"));
		if (matcher.matches()) {
			if (!matcher.group(0).equals(toSupplementaries("testing")))
				failCount++;
		} else {
			failCount++;
		}
		pattern = new Pattern(toSupplementaries("(tes)ting"));
		matcher = pattern.matcher(toSupplementaries("testing"));
		if (matcher.find()) {
			if (!matcher.group(0).equals(toSupplementaries("testing")))
				failCount++;
		} else {
			failCount++;
		}
		pattern = new Pattern(toSupplementaries("^(tes)ting"));
		matcher = pattern.matcher(toSupplementaries("testing"));
		if (matcher.matches()) {
			if (!matcher.group(0).equals(toSupplementaries("testing")))
				failCount++;
		} else {
			failCount++;
		}
		report("Group0");
	}
	@Test
	public void findIntTest() throws Exception {
		Pattern p = new Pattern("blah");
		Matcher m = p.matcher("zzzzblahzzzzzblah");
		boolean result = m.find(2);
		if (!result) failCount++;
		p = new Pattern("$");
		m = p.matcher("1234567890");
		result = m.find(10);
		if (!result) failCount++;
		try {
			result = m.find(11);
			failCount++;
		} catch (IndexOutOfBoundsException e) {
			// correct result
		}
		// Supplementary character test
		p = new Pattern(toSupplementaries("blah"));
		m = p.matcher(toSupplementaries("zzzzblahzzzzzblah"));
		result = m.find(2);
		if (!result) failCount++;
		report("FindInt");
	}
	@Test
	public void emptyPatternTest() throws Exception {
		Pattern p = new Pattern("");
		Matcher m = p.matcher("foo");
		// Should find empty pattern at beginning of input
		boolean result = m.find();
		if (result != true) failCount++;
		if (m.start() != 0) failCount++;
		// Should not match entire input if input is not empty
		m.setTarget("");
		result = m.matches();
		if (result == true) failCount++;
		try {
			m.start(0);
			failCount++;
		} catch (IllegalStateException e) {
			// Correct result
		}
		// Should match entire input if input is empty
		m.setTarget("");
		result = m.matches();
		if (result != true) failCount++;
		result = Pattern.matches("", "");
		if (result != true) failCount++;
		result = Pattern.matches("", "foo");
		if (result == true) failCount++;
		report("EmptyPattern");
	}
	@Test
	public void charClassTest() throws Exception {
		Pattern pattern = new Pattern("blah[ab]]blech");
		check(pattern, "blahb]blech", true);
		pattern = new Pattern("[abc[def]]");
		check(pattern, "b", true);
		// Supplementary character tests
		pattern = new Pattern(toSupplementaries("blah[ab]]blech"));
		check(pattern, toSupplementaries("blahb]blech"), true);
		pattern = new Pattern(toSupplementaries("[abc[def]]"));
		check(pattern, toSupplementaries("b"), true);
	}
	@Test
	public void caretTest() throws Exception {
		Pattern pattern = new Pattern("\\w*");
		Matcher matcher = pattern.matcher("a#bc#def##g");
		check(matcher, "a");
		check(matcher, "");
		check(matcher, "bc");
		check(matcher, "");
		check(matcher, "def");
		check(matcher, "");
		check(matcher, "");
		check(matcher, "g");
		check(matcher, "");
		if (matcher.find()) failCount++;
		pattern = new Pattern("^\\w*");
		matcher = pattern.matcher("a#bc#def##g");
		check(matcher, "a");
		if (matcher.find()) failCount++;
		pattern = new Pattern("\\w");
		matcher = pattern.matcher("abc##x");
		check(matcher, "a");
		check(matcher, "b");
		check(matcher, "c");
		check(matcher, "x");
		if (matcher.find()) failCount++;
		pattern = new Pattern("^\\w");
		matcher = pattern.matcher("abc##x");
		check(matcher, "a");
		if (matcher.find()) failCount++;
		pattern = new Pattern("\\A\\p{Alpha}{3}");
		matcher = pattern.matcher("abcdef-ghi\njklmno");
		check(matcher, "abc");
		if (matcher.find()) failCount++;
		pattern = new Pattern("^\\p{Alpha}{3}", Pattern.MULTILINE);
		matcher = pattern.matcher("abcdef-ghi\njklmno");
		check(matcher, "abc");
		check(matcher, "jkl");
		if (matcher.find()) failCount++;
		pattern = new Pattern("^", Pattern.MULTILINE);
		Replacer replacer = pattern.replacer("this is some text");
		String result = replacer.replace("X");
		if (!result.equals("Xthis is some text")) failCount++;
		pattern = new Pattern("^");
		replacer = pattern.replacer("this is some text");
		result = replacer.replace("X");
		if (!result.equals("Xthis is some text")) failCount++;
		result = replacer.replace("X");
		if (!result.equals("Xthis is some text\n")) failCount++;
		report("Caret");
	}
	@Test
	public void groupCaptureTest() throws Exception {
		// Independent group
		Pattern pattern = new Pattern("x+(?>y+)z+");
		Matcher matcher = pattern.matcher("xxxyyyzzz");
		matcher.find();
		try {
			matcher.group(1);
			failCount++;
		} catch (IndexOutOfBoundsException ioobe) {
			// Good result
		}
		// Pure group
		pattern = new Pattern("x+(?:y+)z+");
		matcher = pattern.matcher("xxxyyyzzz");
		matcher.find();
		try {
			matcher.group(1);
			failCount++;
		} catch (IndexOutOfBoundsException ioobe) {
			// Good result
		}
		// Supplementary character tests
		// Independent group
		pattern = new Pattern(toSupplementaries("x+(?>y+)z+"));
		matcher = pattern.matcher(toSupplementaries("xxxyyyzzz"));
		matcher.find();
		try {
			matcher.group(1);
			failCount++;
		} catch (IndexOutOfBoundsException ioobe) {
			// Good result
		}
		// Pure group
		pattern = new Pattern(toSupplementaries("x+(?:y+)z+"));
		matcher = pattern.matcher(toSupplementaries("xxxyyyzzz"));
		matcher.find();
		try {
			matcher.group(1);
			failCount++;
		} catch (IndexOutOfBoundsException ioobe) {
			// Good result
		}
		report("GroupCapture");
	}
	@Test
	public void backRefTest() throws Exception {
		Pattern pattern = new Pattern("(a*)bc\\1");
		check(pattern, "zzzaabcazzz", true);
		pattern = new Pattern("(a*)bc\\1");
		check(pattern, "zzzaabcaazzz", true);
		pattern = new Pattern("(abc)(def)\\1");
		check(pattern, "abcdefabc", true);
		pattern = new Pattern("(abc)(def)\\3");
		check(pattern, "abcdefabc", false);
		try {
			for (int i = 1; i < 10; i++) {
				// Make sure backref 1-9 are always accepted
				pattern = new Pattern("abcdef\\" + i);
				// and fail to match if the target group does not exit
				check(pattern, "abcdef", false);
			}
		} catch (PatternSyntaxException e) {
			failCount++;
		}
		pattern = new Pattern("(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)\\11");
		check(pattern, "abcdefghija", false);
		check(pattern, "abcdefghija1", true);
		pattern = new Pattern("(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)(k)\\11");
		check(pattern, "abcdefghijkk", true);
		pattern = new Pattern("(a)bcdefghij\\11");
		check(pattern, "abcdefghija1", true);
		// Supplementary character tests
		pattern = new Pattern(toSupplementaries("(a*)bc\\1"));
		check(pattern, toSupplementaries("zzzaabcazzz"), true);
		pattern = new Pattern(toSupplementaries("(a*)bc\\1"));
		check(pattern, toSupplementaries("zzzaabcaazzz"), true);
		pattern = new Pattern(toSupplementaries("(abc)(def)\\1"));
		check(pattern, toSupplementaries("abcdefabc"), true);
		pattern = new Pattern(toSupplementaries("(abc)(def)\\3"));
		check(pattern, toSupplementaries("abcdefabc"), false);
		pattern = new Pattern(
				toSupplementaries("(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)\\11"));
		check(pattern, toSupplementaries("abcdefghija"), false);
		check(pattern, toSupplementaries("abcdefghija1"), true);
		pattern = new Pattern(
				toSupplementaries("(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)(k)\\11"));
		check(pattern, toSupplementaries("abcdefghijkk"), true);
		report("BackRef");
	}
	/**
	 * Unicode Technical Report #18, section 2.6 End of Line
	 * There is no empty line to be matched in the sequence
	 * but there is an empty line in the sequence .
	 */
	@Test
	public void anchorTest() throws Exception {
		Pattern p = new Pattern("^.*$", Pattern.MULTILINE);
		Matcher m = p.matcher("blah1\r\nblah2");
		m.find();
		m.find();
		if (!m.group().equals("blah2")) failCount++;
		m.setTarget("blah1\n\rblah2");
		m.find();
		m.find();
		m.find();
		if (!m.group().equals("blah2")) failCount++;
		// Test behavior of $ with \r\n at end of input
		p = new Pattern(".+$");
		m = p.matcher("blah1\r\n");
		if (!m.find()) failCount++;
		if (!m.group().equals("blah1")) failCount++;
		if (m.find()) failCount++;
		// Test behavior of $ with \r\n at end of input in multiline
		p = new Pattern(".+$", Pattern.MULTILINE);
		m = p.matcher("blah1\r\n");
		if (!m.find()) failCount++;
		if (m.find()) failCount++;
		// Test for $ recognition of \u0085 for bug 4527731
		p = new Pattern(".+$", Pattern.MULTILINE);
		m = p.matcher("blah1\u0085");
		if (!m.find()) failCount++;
		// Supplementary character test
		p = new Pattern("^.*$", Pattern.MULTILINE);
		m = p.matcher(toSupplementaries("blah1\r\nblah2"));
		m.find();
		m.find();
		if (!m.group().equals(toSupplementaries("blah2"))) failCount++;
		m.setTarget(toSupplementaries("blah1\n\rblah2"));
		m.find();
		m.find();
		m.find();
		if (!m.group().equals(toSupplementaries("blah2"))) failCount++;
		// Test behavior of $ with \r\n at end of input
		p = new Pattern(".+$");
		m = p.matcher(toSupplementaries("blah1\r\n"));
		if (!m.find()) failCount++;
		if (!m.group().equals(toSupplementaries("blah1"))) failCount++;
		if (m.find()) failCount++;
		// Test behavior of $ with \r\n at end of input in multiline
		p = new Pattern(".+$", Pattern.MULTILINE);
		m = p.matcher(toSupplementaries("blah1\r\n"));
		if (!m.find()) failCount++;
		if (m.find()) failCount++;
		// Test for $ recognition of \u0085 for bug 4527731
		p = new Pattern(".+$", Pattern.MULTILINE);
		m = p.matcher(toSupplementaries("blah1\u0085"));
		if (!m.find()) failCount++;
		report("Anchors");
	}
	/**
	 * A basic sanity test of Matcher.find().
	 */
	@Test
	public void findTest() throws Exception {
		Pattern p = new Pattern("(ab)(c*)");
		Matcher m = p.matcher("abccczzzabcczzzabccc");
		if (!m.find()) failCount++;
		if (!m.group().equals(m.group(0))) failCount++;
		m = p.matcher("zzzabccczzzabcczzzabccczzz");
		if (m.find()) failCount++;
		// Supplementary character test
		p = new Pattern(toSupplementaries("(ab)(c*)"));
		m = p.matcher(toSupplementaries("abccczzzabcczzzabccc"));
		if (!m.find()) failCount++;
		if (!m.group().equals(m.group(0))) failCount++;
		m = p.matcher(toSupplementaries("zzzabccczzzabcczzzabccczzz"));
		if (m.find()) failCount++;
		report("Looking At");
	}
	/**
	 * A basic sanity test of Matcher.matches().
	 */
	@Test
	public void matchesTest() throws Exception {
		// matches()
		Pattern p = new Pattern("ulb(c*)");
		Matcher m = p.matcher("ulbcccccc");
		assertTrue(m.toString(), m.matches());
		// find() but not matches()
		m.setTarget("zzzulbcccccc");
		assertTrue(m.toString(), m.matches());
		// find() but not matches()
		m.setTarget("ulbccccccdef");
		assertTrue(m.toString(), !m.matches());
		// matches()
		p = new Pattern("a|ad");
		m = p.matcher("ad");
		assertTrue(m.toString(), m.matches());
		// Supplementary character test
		// matches()
		p = new Pattern(toSupplementaries("ulb(c*)"));
		m = p.matcher(toSupplementaries("ulbcccccc"));
		assertTrue(m.toString(), m.matches());
		// find() but not matches()
		m.setTarget(toSupplementaries("zzzulbcccccc"));
		assertTrue(m.toString(), !m.matches());
		// find() but not matches()
		m.setTarget(toSupplementaries("ulbccccccdef"));
		assertTrue(m.toString(), !m.matches());
		// matches()
		p = new Pattern(toSupplementaries("a|ad"));
		m = p.matcher(toSupplementaries("ad"));
		assertTrue(m.toString(), !m.matches());
		report("Matches");
	}
	/**
	 * A basic sanity test of Pattern.matches().
	 */
	@Test
	public void patternMatchesTest() throws Exception {
		// matches()
		if (!Pattern.matches(toSupplementaries("ulb(c*)"),
				toSupplementaries("ulbcccccc"))) failCount++;
		// find() but not matches()
		if (Pattern.matches(toSupplementaries("ulb(c*)"),
				toSupplementaries("zzzulbcccccc"))) failCount++;
		// find() but not matches()
		if (Pattern.matches(toSupplementaries("ulb(c*)"),
				toSupplementaries("ulbccccccdef"))) failCount++;
		// Supplementary character test
		// matches()
		if (!Pattern.matches(toSupplementaries("ulb(c*)"),
				toSupplementaries("ulbcccccc"))) failCount++;
		// find() but not matches()
		if (Pattern.matches(toSupplementaries("ulb(c*)"),
				toSupplementaries("zzzulbcccccc"))) failCount++;
		// find() but not matches()
		if (Pattern.matches(toSupplementaries("ulb(c*)"),
				toSupplementaries("ulbccccccdef"))) failCount++;
		report("Pattern Matches");
	}
	private static void substitutionBasher() {
		for (int runs = 0; runs < 1000; runs++) {
			// Create a base string to work in
			int leadingChars = generator.nextInt(10);
			StringBuffer baseBuffer = new StringBuffer(100);
			String leadingString = getRandomAlphaString(leadingChars);
			baseBuffer.append(leadingString);
			// Create 5 groups of random number of random chars
			// Create the string to substitute
			// Create the pattern string to search for
			StringBuffer bufferToSub = new StringBuffer(25);
			StringBuffer bufferToPat = new StringBuffer(50);
			String[] groups = new String[5];
			for (int i = 0; i < 5; i++) {
				int aGroupSize = generator.nextInt(5) + 1;
				groups[i] = getRandomAlphaString(aGroupSize);
				bufferToSub.append(groups[i]);
				bufferToPat.append('(');
				bufferToPat.append(groups[i]);
				bufferToPat.append(')');
			}
			String stringToSub = bufferToSub.toString();
			String pattern = bufferToPat.toString();
			// Place sub string into working string at random index
			baseBuffer.append(stringToSub);
			// Append random chars to end
			int trailingChars = generator.nextInt(10);
			String trailingString = getRandomAlphaString(trailingChars);
			baseBuffer.append(trailingString);
			String baseString = baseBuffer.toString();
			// Create test pattern and matcher
			Pattern p = new Pattern(pattern);
			Matcher m = p.matcher(baseString);
			// Reject candidate if pattern happens to start early
			m.find();
			if (m.start() < leadingChars) continue;
			// Reject candidate if more than one match
			if (m.find()) continue;
			// Construct a replacement string with :
			// random group + random string + random group
			StringBuffer bufferToRep = new StringBuffer();
			int groupIndex1 = generator.nextInt(5);
			bufferToRep.append("$" + (groupIndex1 + 1));
			String randomMidString = getRandomAlphaString(5);
			bufferToRep.append(randomMidString);
			int groupIndex2 = generator.nextInt(5);
			bufferToRep.append("$" + (groupIndex2 + 1));
			String replacement = bufferToRep.toString();
			// Do the replacement
		}
		report("Substitution Basher");
	}
	/**
	 * Checks the handling of some escape sequences that the Pattern
	 * class should process instead of the java compiler. These are
	 * not in the file because the escapes should be be processed
	 * by the Pattern class when the regex is compiled.
	 */
	private static void escapes() throws Exception {
		Pattern p = new Pattern("\\043");
		Matcher m = p.matcher("#");
		if (!m.find()) failCount++;
		p = new Pattern("\\x23");
		m = p.matcher("#");
		if (!m.find()) failCount++;
		p = new Pattern("\\u0023");
		m = p.matcher("#");
		if (!m.find()) failCount++;
		report("Escape sequences");
	}
	/**
	 * Checks the handling of blank input situations. These
	 * tests are incompatible with my test file format.
	 */
	private static void blankInput() throws Exception {
		Pattern p = new Pattern("abc", Pattern.IGNORE_CASE);
		Matcher m = p.matcher("");
		if (m.find()) failCount++;
		p = new Pattern("a*", Pattern.IGNORE_CASE);
		m = p.matcher("");
		if (!m.find()) failCount++;
		p = new Pattern("abc");
		m = p.matcher("");
		if (m.find()) failCount++;
		p = new Pattern("a*");
		m = p.matcher("");
		if (!m.find()) failCount++;
		report("Blank input");
	}
	/**
	 * Tests the Boyer-Moore pattern matching of a character sequence
	 * on randomly generated patterns.
	 */
	private static void bm() throws Exception {
		doBnM('a');
		report("Boyer Moore (ASCII)");
		doBnM(Character.MIN_SUPPLEMENTARY_CODE_POINT - 10);
		report("Boyer Moore (Supplementary)");
	}
	private static void doBnM(int baseCharacter) throws Exception {
		for (int i = 0; i < 100; i++) {
			// Create a short pattern to search for
			int patternLength = generator.nextInt(7) + 4;
			StringBuffer patternBuffer = new StringBuffer(patternLength);
			for (int x = 0; x < patternLength; x++) {
				int ch = baseCharacter + generator.nextInt(26);
				if (Character.isSupplementaryCodePoint(ch)) {
					patternBuffer.append(Character.toChars(ch));
				} else {
					patternBuffer.append((char) ch);
				}
			}
			String pattern = patternBuffer.toString();
			Pattern p = new Pattern(pattern);
			// Create a buffer with random ASCII chars that does
			// not match the sample
			String toSearch = null;
			StringBuffer s = null;
			Matcher m = p.matcher("");
			do {
				s = new StringBuffer(100);
				for (int x = 0; x < 100; x++) {
					int ch = baseCharacter + generator.nextInt(26);
					if (Character.isSupplementaryCodePoint(ch)) {
						s.append(Character.toChars(ch));
					} else {
						s.append((char) ch);
					}
				}
				toSearch = s.toString();
				m.setTarget(toSearch);
			} while (m.find());
			// Insert the pattern at a random spot
			int insertIndex = generator.nextInt(99);
			if (Character.isLowSurrogate(s.charAt(insertIndex)))
				insertIndex++;
			s = s.insert(insertIndex, pattern);
			toSearch = s.toString();
			// Make sure that the pattern is found
			m.setTarget(toSearch);
			if (!m.find()) failCount++;
			// Make sure that the match text is the pattern
			if (!m.group().equals(pattern)) failCount++;
			// Make sure match occured at insertion point
			if (m.start() != insertIndex) failCount++;
		}
	}
	private static void explainFailure(String pattern, String data,
			String expected, String actual) {
		System.err.println("----------------------------------------");
		System.err.println("Pattern = " + pattern);
		System.err.println("Data = " + data);
		System.err.println("Expected = " + expected);
		System.err.println("Actual   = " + actual);
	}
	private static void explainFailure(String pattern, String data, Throwable t) {
		System.err.println("----------------------------------------");
		System.err.println("Pattern = " + pattern);
		System.err.println("Data = " + data);
		t.printStackTrace(System.err);
	}
	// Testing examples from a file
	/**
	 * Goes through the file "TestCases.txt" and creates many patterns
	 * described in the file, matching the patterns against input lines in
	 * the file, and comparing the results against the correct results
	 * also found in the file. The file format is described in comments
	 * at the head of the file.
	 */
	private static void processFile(String fileName) throws Exception {
		File testCases = new File(System.getProperty("test.src", "."),
				fileName);
		FileInputStream in = new FileInputStream(testCases);
		BufferedReader r = new BufferedReader(new InputStreamReader(in));
		while ((r.readLine()) != null) {
			// Read a line for pattern
			String patternString = grabLine(r);
			Pattern p = null;
			try {
				p = compileTestPattern(patternString);
			} catch (PatternSyntaxException e) {
				String dataString = grabLine(r);
				String expectedResult = grabLine(r);
				if (expectedResult.startsWith("error")) continue;
				explainFailure(patternString, dataString, e);
				failCount++;
				continue;
			}
			// Read a line for input string
			String dataString = grabLine(r);
			Matcher m = p.matcher(dataString);
			StringBuffer result = new StringBuffer();
			// Check for IllegalStateExceptions before a match
			failCount += preMatchInvariants(m);
			boolean found = m.find();
			if (found)
				failCount += postTrueMatchInvariants(m);
			else failCount += postFalseMatchInvariants(m);
			if (found) {
				result.append("true ");
				result.append(m.group(0) + " ");
			} else {
				result.append("false ");
			}
			result.append(m.groupCount());
			if (found) {
				for (int i = 1; i < m.groupCount() + 1; i++)
					if (m.group(i) != null)
						result.append(" " + m.group(i));
			}
			// Read a line for the expected result
			String expectedResult = grabLine(r);
			if (!result.toString().equals(expectedResult)) {
				explainFailure(patternString, dataString, expectedResult,
						result.toString());
				failCount++;
			}
		}
		report(fileName);
	}
	private static int preMatchInvariants(Matcher m) {
		int failCount = 0;
		try {
			m.start();
			failCount++;
		} catch (IllegalStateException ise) {}
		try {
			m.end();
			failCount++;
		} catch (IllegalStateException ise) {}
		try {
			m.group();
			failCount++;
		} catch (IllegalStateException ise) {}
		return failCount;
	}
	private static int postFalseMatchInvariants(Matcher m) {
		int failCount = 0;
		try {
			m.group();
			failCount++;
		} catch (IllegalStateException ise) {}
		try {
			m.start();
			failCount++;
		} catch (IllegalStateException ise) {}
		try {
			m.end();
			failCount++;
		} catch (IllegalStateException ise) {}
		return failCount;
	}
	private static int postTrueMatchInvariants(Matcher m) {
		int failCount = 0;
		// assert(m.start() = m.start(0);
		if (m.start() != m.start(0)) failCount++;
		// assert(m.end() = m.end(0);
		if (m.start() != m.start(0)) failCount++;
		// assert(m.group() = m.group(0);
		if (!m.group().equals(m.group(0))) failCount++;
		try {
			m.group(50);
			failCount++;
		} catch (IndexOutOfBoundsException ise) {}
		return failCount;
	}
	private static Pattern compileTestPattern(String patternString) {
		if (!patternString.startsWith("'")) { return new Pattern(
				patternString); }
		int break1 = patternString.lastIndexOf("'");
		String flagString = patternString.substring(break1 + 1,
				patternString.length());
		patternString = patternString.substring(1, break1);
		if (flagString.equals("i"))
			return new Pattern(patternString, Pattern.IGNORE_CASE);
		if (flagString.equals("m"))
			return new Pattern(patternString, Pattern.MULTILINE);
		return new Pattern(patternString);
	}
	/**
	 * Reads a line from the input file. Keeps reading lines until a non
	 * empty non comment line is read. If the line contains a \n then
	 * these two characters are replaced by a newline char. If a \\uxxxx
	 * sequence is read then the sequence is replaced by the unicode char.
	 */
	private static String grabLine(BufferedReader r) throws Exception {
		int index = 0;
		String line = r.readLine();
		while (line.startsWith("//") || line.length() < 1)
			line = r.readLine();
		while ((index = line.indexOf("\\n")) != -1) {
			StringBuffer temp = new StringBuffer(line);
			temp.replace(index, index + 2, "\n");
			line = temp.toString();
		}
		while ((index = line.indexOf("\\u")) != -1) {
			StringBuffer temp = new StringBuffer(line);
			String value = temp.substring(index + 2, index + 6);
			char aChar = (char) Integer.parseInt(value, 16);
			String unicodeChar = "" + aChar;
			temp.replace(index, index + 6, unicodeChar);
			line = temp.toString();
		}
		return line;
	}
	private static void check(Pattern p, String s, String g, String expected) {
		Matcher m = p.matcher(s);
		m.find();
		if (!m.group(g).equals(expected)) failCount++;
	}
	private static void checkReplaceFirst(String p, String s, String r,
			String expected) {
		if (!expected.equals(new Pattern(p).replacer(s).replace(r)))
			failCount++;
	}
	private static void checkReplaceAll(String p, String s, String r,
			String expected) {
		if (!expected.equals(new Pattern(p).replacer(s).replace(r)))
			failCount++;
	}
	private static void checkExpectedFail(String p) {
		try {
			new Pattern(p);
		} catch (PatternSyntaxException pse) {
			// pse.printStackTrace();
			return;
		}
		failCount++;
	}
	private static void checkExpectedFail(Matcher m, String g) {
		m.find();
		try {
			m.group(g);
		} catch (IllegalArgumentException iae) {
			// iae.printStackTrace();
			return;
		} catch (NullPointerException npe) {
			return;
		}
		failCount++;
	}
	@Test
	public void namedGroupCaptureTest() throws Exception {
		check(new Pattern("x+(?<gname>y+)z+"), "xxxyyyzzz", "gname", "yyy");
		check(new Pattern("x+(?<gname8>y+)z+"), "xxxyyyzzz", "gname8", "yyy");
		// backref
		Pattern pattern = new Pattern("(a*)bc\\1");
		check(pattern, "zzzaabcazzz", true); // found "abca"
		check(new Pattern("(?<gname>a*)bc\\k<gname>"), "zzzaabcaazzz", true);
		check(new Pattern("(?<gname>abc)(def)\\k<gname>"), "abcdefabc", true);
		check(new Pattern(
				"(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)(?<gname>k)\\k<gname>"),
				"abcdefghijkk", true);
		// Supplementary character tests
		check(new Pattern("(?<gname>" + toSupplementaries("a*)bc")
				+ "\\k<gname>"), toSupplementaries("zzzaabcazzz"), true);
		check(new Pattern("(?<gname>" + toSupplementaries("a*)bc")
				+ "\\k<gname>"), toSupplementaries("zzzaabcaazzz"), true);
		check(new Pattern("(?<gname>" + toSupplementaries("abc)(def)")
				+ "\\k<gname>"), toSupplementaries("abcdefabc"), true);
		check(new Pattern(toSupplementaries("(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)")
				+ "(?<gname>" + toSupplementaries("k)") + "\\k<gname>"),
				toSupplementaries("abcdefghijkk"), true);
		check(new Pattern("x+(?<gname>y+)z+\\k<gname>"), "xxxyyyzzzyyy",
				"gname", "yyy");
		// replaceFirst/All
		checkReplaceFirst("(?<gn>ab)(c*)", "abccczzzabcczzzabccc", "${gn}",
				"abzzzabcczzzabccc");
		checkReplaceAll("(?<gn>ab)(c*)", "abccczzzabcczzzabccc", "${gn}",
				"abzzzabzzzab");
		checkReplaceFirst("(?<gn>ab)(c*)", "zzzabccczzzabcczzzabccczzz",
				"${gn}", "zzzabzzzabcczzzabccczzz");
		checkReplaceAll("(?<gn>ab)(c*)", "zzzabccczzzabcczzzabccczzz",
				"${gn}", "zzzabzzzabzzzabzzz");
		checkReplaceFirst("(?<gn1>ab)(?<gn2>c*)",
				"zzzabccczzzabcczzzabccczzz", "${gn2}",
				"zzzccczzzabcczzzabccczzz");
		checkReplaceAll("(?<gn1>ab)(?<gn2>c*)", "zzzabccczzzabcczzzabccczzz",
				"${gn2}", "zzzccczzzcczzzccczzz");
		// toSupplementaries("(ab)(c*)"));
		checkReplaceFirst("(?<gn1>" + toSupplementaries("ab") + ")(?<gn2>"
				+ toSupplementaries("c") + "*)",
				toSupplementaries("abccczzzabcczzzabccc"), "${gn1}",
				toSupplementaries("abzzzabcczzzabccc"));
		checkReplaceAll("(?<gn1>" + toSupplementaries("ab") + ")(?<gn2>"
				+ toSupplementaries("c") + "*)",
				toSupplementaries("abccczzzabcczzzabccc"), "${gn1}",
				toSupplementaries("abzzzabzzzab"));
		checkReplaceFirst("(?<gn1>" + toSupplementaries("ab") + ")(?<gn2>"
				+ toSupplementaries("c") + "*)",
				toSupplementaries("abccczzzabcczzzabccc"), "${gn2}",
				toSupplementaries("ccczzzabcczzzabccc"));
		checkReplaceAll("(?<gn1>" + toSupplementaries("ab") + ")(?<gn2>"
				+ toSupplementaries("c") + "*)",
				toSupplementaries("abccczzzabcczzzabccc"), "${gn2}",
				toSupplementaries("ccczzzcczzzccc"));
		checkReplaceFirst("(?<dog>Dog)AndCat", "zzzDogAndCatzzzDogAndCatzzz",
				"${dog}", "zzzDogzzzDogAndCatzzz");
		checkReplaceAll("(?<dog>Dog)AndCat", "zzzDogAndCatzzzDogAndCatzzz",
				"${dog}", "zzzDogzzzDogzzz");
		// backref in Matcher & String
		if (!"abcdefghij".replaceFirst("cd(?<gn>ef)gh", "${gn}").equals(
				"abefij")
				|| !"abbbcbdbefgh".replaceAll("(?<gn>[a-e])b", "${gn}")
						.equals("abcdefgh")) failCount++;
		// negative
		checkExpectedFail("(?<groupnamehasnoascii.in>abc)(def)");
		checkExpectedFail("(?<groupnamehasnoascii_in>abc)(def)");
		checkExpectedFail("(?<6groupnamestartswithdigit>abc)(def)");
		checkExpectedFail("(?<gname>abc)(def)\\k<gnameX>");
		checkExpectedFail("(?<gname>abc)(?<gname>def)\\k<gnameX>");
		checkExpectedFail(
				new Pattern("(?<gname>abc)(def)").matcher("abcdef"),
				"gnameX");
		checkExpectedFail(
				new Pattern("(?<gname>abc)(def)").matcher("abcdef"), null);
		report("NamedGroupCapture");
	}
	// This is for bug 6969132
	@Test
	public void nonBmpClassComplementTest() throws Exception {
		Pattern p = new Pattern("\\P{Lu}");
		Matcher m = p.matcher(new String(new int[] { 0x1d400 }, 0, 1));
		if (m.find() && m.start() == 1) failCount++;
		// from a unicode category
		p = new Pattern("\\P{Lu}");
		m = p.matcher(new String(new int[] { 0x1d400 }, 0, 1));
		if (m.find()) failCount++;
		// block
		p = new Pattern("\\P{InMathematicalAlphanumericSymbols}");
		m = p.matcher(new String(new int[] { 0x1d400 }, 0, 1));
		if (m.find() && m.start() == 1) failCount++;
		report("NonBmpClassComplement");
	}
	@Test
	public void unicodePropertiesTest() throws Exception {
		// different forms
		if (!new Pattern("\\p{IsLu}").matcher("A").matches()
				|| !new Pattern("\\p{Lu}").matcher("A").matches()
				|| !new Pattern("\\p{gc=Lu}").matcher("A").matches()
				|| !new Pattern("\\p{general_category=Lu}").matcher("A")
						.matches()
				|| !new Pattern("\\p{IsLatin}").matcher("B").matches()
				|| !new Pattern("\\p{sc=Latin}").matcher("B").matches()
				|| !new Pattern("\\p{script=Latin}").matcher("B").matches()
				|| !new Pattern("\\p{InBasicLatin}").matcher("c").matches()
				|| !new Pattern("\\p{blk=BasicLatin}").matcher("c")
						.matches()
				|| !new Pattern("\\p{block=BasicLatin}").matcher("c")
						.matches()) failCount++;
		Matcher common = new Pattern("\\p{script=Common}").matcher("");
		Matcher unknown = new Pattern("\\p{IsUnknown}").matcher("");
		Matcher lastSM = common;
		Character.UnicodeScript lastScript = Character.UnicodeScript.of(0);
		Matcher latin = new Pattern("\\p{block=basic_latin}").matcher("");
		Matcher greek = new Pattern("\\p{InGreek}").matcher("");
		Matcher lastBM = latin;
		Character.UnicodeBlock lastBlock = Character.UnicodeBlock.of(0);
		for (int cp = 1; cp < Character.MAX_CODE_POINT; cp++) {
			if (cp >= 0x30000 && (cp & 0x70) == 0) {
				continue; // only pick couple code points, they are the same
			}
			// Unicode Script
			Character.UnicodeScript script = Character.UnicodeScript.of(cp);
			Matcher m;
			String str = new String(Character.toChars(cp));
			if (script == lastScript) {
				m = lastSM;
				m.setTarget(str);
			} else {
				m = new Pattern("\\p{Is" + script.name() + "}")
						.matcher(str);
			}
			if (!m.matches()) {
				failCount++;
			}
			Matcher other = (script == Character.UnicodeScript.COMMON) ? unknown
					: common;
			other.setTarget(str);
			if (other.matches()) {
				failCount++;
			}
			lastSM = m;
			lastScript = script;
			// Unicode Block
			Character.UnicodeBlock block = Character.UnicodeBlock.of(cp);
			if (block == null) {
				// System.out.printf("Not a Block: cp=%x%n", cp);
				continue;
			}
			if (block == lastBlock) {
				m = lastBM;
				m.setTarget(str);
			} else {
				m = new Pattern("\\p{block=" + block.toString() + "}")
						.matcher(str);
			}
			if (!m.matches()) {
				failCount++;
			}
			other = (block == Character.UnicodeBlock.BASIC_LATIN) ? greek
					: latin;
			other.setTarget(str);
			if (other.matches()) {
				failCount++;
			}
			lastBM = m;
			lastBlock = block;
		}
		report("unicodeProperties");
	}
	@Test
	public void unicodeHexNotationTest() throws Exception {
		// negative
		checkExpectedFail("\\x{-23}");
		checkExpectedFail("\\x{110000}");
		checkExpectedFail("\\x{}");
		checkExpectedFail("\\x{AB[ef]");
		// codepoint
		check("^\\x{1033c}$", "\uD800\uDF3C", true);
		check("^\\xF0\\x90\\x8C\\xBC$", "\uD800\uDF3C", false);
		check("^\\x{D800}\\x{DF3c}+$", "\uD800\uDF3C", false);
		check("^\\xF0\\x90\\x8C\\xBC$", "\uD800\uDF3C", false);
		// in class
		check("^[\\x{D800}\\x{DF3c}]+$", "\uD800\uDF3C", false);
		check("^[\\xF0\\x90\\x8C\\xBC]+$", "\uD800\uDF3C", false);
		check("^[\\x{D800}\\x{DF3C}]+$", "\uD800\uDF3C", false);
		check("^[\\x{DF3C}\\x{D800}]+$", "\uD800\uDF3C", false);
		check("^[\\x{D800}\\x{DF3C}]+$", "\uDF3C\uD800", true);
		check("^[\\x{DF3C}\\x{D800}]+$", "\uDF3C\uD800", true);
		for (int cp = 0; cp <= 0x10FFFF; cp++) {
			String s = "A" + new String(Character.toChars(cp)) + "B";
			String hexUTF16 = (cp <= 0xFFFF) ? String.format("\\u%04x", cp)
					: String.format("\\u%04x\\u%04x",
							(int) Character.toChars(cp)[0],
							(int) Character.toChars(cp)[1]);
			String hexCodePoint = "\\x{" + Integer.toHexString(cp) + "}";
			if (!Pattern.matches("A" + hexUTF16 + "B", s)) failCount++;
			if (!Pattern.matches("A[" + hexUTF16 + "]B", s)) failCount++;
			if (!Pattern.matches("A" + hexCodePoint + "B", s)) failCount++;
			if (!Pattern.matches("A[" + hexCodePoint + "]B", s))
				failCount++;
		}
		report("unicodeHexNotation");
	}
}