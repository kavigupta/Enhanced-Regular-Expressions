package eredmel.test.enregex;

import org.junit.Before;
import org.junit.Test;

import eredmel.regex.EnregexType;

public class SingleParenMatchingTest {
	@Before
	public void start() {
		EnregexTestUtil.TO_USE = EnregexType.EREDMEL_STANDARD;
	}
	@Test
	public void worksWithRegexNormallyTest() {
		EnregexTestUtil.assertMatch("ABC", "ABC ABC ABC", new int[][] {
				{ 0, 3 }, { 4, 7 }, { 8, 11 } });
		EnregexTestUtil.assertMatch("[A-Z][A-Z][A-Z]", "ABCAABC ABC",
				new int[][] { { 0, 3 }, { 3, 6 }, { 8, 11 } });
		EnregexTestUtil.assertMatch("println\\((?<NAME>[^\\)]*)\\)",
				"println(1) println('abc') println()println,", new int[][] {
						{ 0, 10 }, { 11, 25 }, { 26, 35 } });
	}
	@Test
	public void singleNestTest() {
		EnregexTestUtil.assertMatch("\\(~(.+~)\\)", "(a b c) (d e f)",
				new int[][] { { 0, 7 }, { 8, 15 } });
		EnregexTestUtil.assertMatch("\\(~(.+~)\\)", "(a b c') ('d e f)",
				new int[][] { { 0, 17 } });
	}
	@Test
	public void multiNestTest() {
		EnregexTestUtil.assertMatch("\\(~(.+~)\\)", "(a b c() ()d e f)",
				new int[][] { { 0, 17 } });
		EnregexTestUtil.assertMatch("\\(~(.+~)\\)", "(()(())) (()(()))",
				new int[][] { { 0, 8 }, { 9, 17 } });
		EnregexTestUtil.assertMatch("\\(~(.+~)\\)", "()()", new int[][] {});
	}
	@Test
	public void argTest() {
		EnregexTestUtil.assertMatch("\\(~(.*~),~(.*~)\\)", "(,)",
				new int[][] { { 0, 3 } });
		EnregexTestUtil.assertMatch("\\(~(.*~),~(.*~)\\)", "((),()) (,)",
				new int[][] { { 0, 7 }, { 8, 11 } });
		EnregexTestUtil.assertMatch("\\(~(.*~),~(.*~)\\)", "((),()) (,)",
				new int[][] { { 0, 7 }, { 8, 11 } });
	}
}
