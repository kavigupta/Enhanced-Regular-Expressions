package eredmel.test.enregex;

import static eredmel.test.enregex.EnregexTestUtil.assertMatch;

import org.junit.Before;
import org.junit.Test;

import eredmel.regex.EnregexType;

public class ParenQuoteInteractionTest {
	@Before
	public void init() {
		EnregexTestUtil.TO_USE = EnregexType.EREDMEL_STANDARD;
	}
	@Test
	public void nullifyInternal() {
		assertMatch("\\(~<.*~~,.*~>\\)", "('abc', 'def')", new int[][] { { 0,
				14 } });
		assertMatch("\\(~<.*~~,.*~>\\)",
				"('commas in \\'strings, should not count')",
				new int[][] {});
		assertMatch("\\(~<.+~>\\)", "('closeparen!!!)[to be ignored]')",
				new int[][] { { 0, 33 } });
	}
}
