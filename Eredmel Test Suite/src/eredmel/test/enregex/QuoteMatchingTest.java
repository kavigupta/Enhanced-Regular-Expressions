package eredmel.test.enregex;

import static eredmel.test.enregex.EnregexTestUtil.assertMatch;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import eredmel.enregex.EnregexType;
import eredmel.enregex.SymbolPair;

public class QuoteMatchingTest {
	@Before
	public void init() {
		EnregexTestUtil.TO_USE = new EnregexType(Arrays.asList(),
				Arrays.asList(//
						new SymbolPair('\'', '\'', false, true),//
						new SymbolPair('[', ']', false, false),//
						new SymbolPair('"', '"', true, true),//
						new SymbolPair('/', '/', true, false)));
	}
	@Test
	@Ignore
	// TODO unignore
	public void quoteMatchingTest() {
		assertMatch("'(.~')*'", "'a' 'b'", new int[][] { { 0, 3 }, { 5, 8 } });
	}
	@Test
	public void basicInNotQuoteTest() {
		assertMatch("~^'->", "'a'->'e'", new int[][] { { 3, 5 } });
		assertMatch("~^'->", "[a][->][e]", new int[][] { { 4, 6 } });
		assertMatch("~^']->", "[a][->][e]", new int[][] {});
		assertMatch("~^'~^\"->", "[a]\"->\"[e]", new int[][] {});
		assertMatch("~^[->", "[a][->][e]", new int[][] {});
		assertMatch("~^'->", "printf('a ->')->'e -> 345'", new int[][] { {
				14, 16 } });
		assertMatch("~^'\\)\\(", "(abc')('def)(ghi')('jkl)(mno)(pqr')'",
				new int[][] { { 11, 13 }, { 23, 25 }, { 28, 30 } });
		assertMatch("~'bcd.+~^'->.+efg~'", "'abcd'->'efgh'", new int[][] { {
				2, 12 } });
		assertMatch("~]=", "x'[=]'", new int[][] { { 3, 4 } });
		assertMatch("~^'=", "x'[=]'", new int[][] {});
	}
	@Test
	public void escapeTest() {
		assertMatch("~^'=", "'a\\'b'=", new int[][] { { 6, 7 } });
		assertMatch("~^'=", "'ab\\\\'=", new int[][] { { 6, 7 } });
		assertMatch("~^'=", "\\'ab\\\\'=", new int[][] { { 7, 8 } });
		assertMatch("~^'=", "\\[ab]=", new int[][] { { 5, 6 } });
		assertMatch("~^'=", "[ab\\]=", new int[][] { { 5, 6 } });
		assertMatch("~^'=", "\"\\\"ab\"=", new int[][] { { 6, 7 } });
		assertMatch("~^'=", "\\\"=", new int[][] { { 2, 3 } });
		assertMatch("~^'=", "\\/=", new int[][] { { 2, 3 } });
		assertMatch("~^'=", "/ab/=", new int[][] { { 4, 5 } });
		assertMatch("~^'=", "/ab\\/ /=", new int[][] { { 7, 8 } });
	}
}
