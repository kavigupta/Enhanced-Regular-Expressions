package eredmel.test.enregex;

import org.junit.Test;

public class ERETest {
	@Test
	public void worksWithRegexNormallyTest() {
		EREUtilities.assertMatch("ABC", "ABC ABC ABC", new int[][] {
				{ 0, 3 }, { 4, 7 }, { 8, 11 } });
		EREUtilities.assertMatch("[A-Z][A-Z][A-Z]", "ABCAABC ABC",
				new int[][] { { 0, 3 }, { 3, 6 }, { 8, 11 } });
		EREUtilities.assertMatch("println\\((?<NAME>[^\\)]*)\\)",
				"println(1) println('abc') println()println,", new int[][] {
						{ 0, 10 }, { 11, 25 }, { 26, 35 } });
	}
	@Test
	public void singleNestTest() {
		EREUtilities.assertMatch("\\(#0#.+\\)#0#", "(a b c) (d e f)",
				new int[][] { { 0, 7 }, { 8, 15 } });
		EREUtilities.assertMatch("\\(#0#.+\\)#0#", "(a b c') ('d e f)",
				new int[][] { { 0, 17 } });
	}
}
