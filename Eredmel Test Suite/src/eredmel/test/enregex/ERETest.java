package eredmel.test.enregex;

import org.junit.Test;

public class ERETest {
	@Test
	public void worksWithRegexNormallyTest() {
		ERETestUtil.assertMatch("ABC", "ABC ABC ABC", new int[][] { { 0, 3 },
				{ 4, 7 }, { 8, 11 } });
		ERETestUtil.assertMatch("[A-Z][A-Z][A-Z]", "ABCAABC ABC",
				new int[][] { { 0, 3 }, { 3, 6 }, { 8, 11 } });
		ERETestUtil.assertMatch("println\\((?<NAME>[^\\)]*)\\)",
				"println(1) println('abc') println()println,", new int[][] {
						{ 0, 10 }, { 11, 25 }, { 26, 35 } });
	}
	@Test
	public void singleNestTest() {
		ERETestUtil.assertMatch("\\(@.+#\\)", "(a b c) (d e f)", new int[][] {
				{ 0, 7 }, { 8, 15 } });
		ERETestUtil.assertMatch("\\(@.+#\\)", "(a b c') ('d e f)",
				new int[][] { { 0, 17 } });
	}
	@Test
	public void multiNestTest() {
		ERETestUtil.assertMatch("\\(@.+#\\)", "(a b c() ()d e f)",
				new int[][] { { 0, 17 } });
		ERETestUtil.assertMatch("\\(@.+#\\)", "(()(())) (()(()))",
				new int[][] { { 0, 8 }, { 9, 17 } });
		ERETestUtil.assertMatch("\\(@.+#\\)", "()()", new int[][] {});
	}
	@Test
	public void argTest() {
		ERETestUtil.assertMatch("\\(@.+#,@.+#\\)", "((),()) (,)",
				new int[][] { { 0, 7 }, { 8, 11 } });
	}
}
