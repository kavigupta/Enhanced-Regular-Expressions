package eredmel.test.enregex;

import static eredmel.test.enregex.EnregexTestUtil.assertMatch;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import eredmel.regex.EnregexType;
import eredmel.regex.SymbolPair;

public class MultiParenMatchingTest {
	@Before
	public void init() {
		EnregexTestUtil.TO_USE = new EnregexType(Arrays.asList(
				new SymbolPair('(', ')', false, false),//
				new SymbolPair('[', ']', false, true),//
				new SymbolPair('{', '}', true, false),//
				new SymbolPair('<', '>', true, true)),//
				Arrays.asList());
	}
	@Test
	public void basicTest() {
		assertMatch("[(\\{\\[\\<]~<.+~>[)\\}\\]\\>]",
				"( () () ) { {}} [[ ]] <<> <> <>>", new int[][] { { 0, 9 },
						{ 10, 15 }, { 16, 21 }, { 22, 32 } });
	}
	@Test
	public void nestedTest() {
		assertMatch("\\(~<.+~>\\)", "({} [ [() {[]}] {([])}]) ([] () [)]",
				new int[][] { { 0, 24 } });
		assertMatch("\\(~<.+~>\\)", "({}[{}]())( {})", new int[][] {
				{ 0, 10 }, { 10, 15 } });
	}
	@Test
	public void overlapTest() {
		assertMatch("\\(~<.+~>\\)", "(([)]) ({)}", new int[][] { { 0, 6 } });
		assertMatch("printf\\(~<.+,.+~>\\)",
				"printf(2+{34[2}3], ()) printf([),(]) printf([ ] ,<{>(}))",
				new int[][] { { 0, 22 }, { 37, 56 } });
	}
	@Test
	public void escapesIgnored() {
		assertMatch("\\(~<.+~>\\)", "\\({)}", new int[][] {});
		assertMatch("\\(~<.+~>\\)", "(([]\\)\\) \\({)}",
				new int[][] { { 0, 8 } });
		assertMatch("\\(~<.+~>\\)", "(([\\)]\\) \\(\\[])", new int[][] {
				{ 0, 8 }, { 10, 15 } });
	}
	@Test
	public void escapesProcessed() {
		assertMatch("\\(~<.+~>\\)", "\\(\\{)", new int[][] { { 1, 5 } });
		assertMatch("\\(~<.+~>\\)", "(\\<) (\\>)", new int[][] { { 0, 4 },
				{ 5, 9 } });
		assertMatch("\\(~<.+~>\\)", "(\\\\<>)", new int[][] { { 0, 6 } });
		assertMatch("\\(~<.+~>\\)", "(\\<\\>)", new int[][] { { 0, 6 } });
		assertMatch("\\(~<.+~>\\)", "(\\\\\\<\\>)", new int[][] { { 0, 8 } });
		assertMatch("\\(~<.+~>\\)", "([\\])", new int[][] {});
		assertMatch("\\(~<.+~>\\)", "([\\\\\\])", new int[][] {});
		assertMatch("\\(~<.+~>\\)", "(\\[\\\\])", new int[][] { { 0, 7 } });
		assertMatch("\\(~<.+~>\\)", "(\\{\\])", new int[][] { { 0, 6 } });
		assertMatch("\\(~<.+~>\\)", "({}[{}\\]]())( {})", new int[][] {
				{ 0, 12 }, { 12, 17 } });
		assertMatch("\\(~<.+~>\\)", "(\\{[{}\\]]())( {})", new int[][] {
				{ 0, 12 }, { 12, 17 } });
	}
}
