package eredmel.test.compiler.parser;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import eredmel.statement.Replacer;

@Ignore
// TODO unignore
public class ReplacerTest {
	@Test
	public void literalNoncrecursiveTest() {
		assertBlockParse("`", "[ \t]*", "println`(?<x>.*)",
				"println[ \t]*(?<x>.*)");
		assertBlockParse("`", "[ \t]*", "(?<a>~<.+~>)`\\*`(?<b>~<.+~>)",
				"(?<a>~<.+~>)[ \t]*\\*[ \t]*(?<b>~<.+~>)");
	}
	@Test(
			expected = StackOverflowError.class)
	public void literalRecursiveTestError() {
		assertBlockParse("a", "a", "abcdef", "abcdef");
	}
	@Test
	public void literalRecursiveTestNotApplicable() {
		assertBlockParse("a", "a", "bcdef", "bcdef");
	}
	@Test
	public void nonLiteralNonRecursiveTest() {
		assertBlockParse("'(?<x>.+)'\\s*~^'==\\s*'\\k<x>'", "'true'",
				"'1'=='1' '2'=='3' 'LL!'		== 'LL!'",
				"'true' '2'=='3' 'true'");
		assertBlockParse(
				"first \\(LinkedList (?<fst>'.*'),.+\\)",
				"${fst}",
				"first (LinkedList '2', (LinkedList '3', (LinkedList '4')))",
				"'2'");
	}
	@Test
	public void nonLiteralRecursiveTest() {
		assertBlockParse(
				"last \\(LinkedList~<.+~>, (?<tail>.+)\\)",
				"last ${tail}",
				"last (LinkedList '2', (LinkedList '3', (LinkedList '4')))",
				"last '4'");
		assertBlockParse(
				"map ~<(?<f>.+)~> \\(LinkedList ~<(?<head>'.+')~>, (?<tail>.+)\\)",
				"(LinkedList ${f} ${head}, map ${f} ${tail})",
				"map f (LinkedList '2', (LinkedList '3', (LinkedList '4')))",
				"(LinkedList f '2', (LinkedList f '3', (LinkedList map f '4')))");
	}
	private void assertBlockParse(String find, String replace, String input,
			String output) {
		assertEquals(output, Replacer.getInstance(find, replace).apply(input));
	}
}
