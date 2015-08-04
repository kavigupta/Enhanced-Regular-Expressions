package eredmel.test.preprocessor.normalizer;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

import eredmel.collections.Pair;
import eredmel.preprocessor.EredmelPreprocessor;

public class NormalizerTest {
	@Test
	public void tabTest() {
		testNormalization(4, "tab.edmh");
	}
	@Test
	public void tabSpaceDefaultTest() {
		testNormalization(4, "tab_space_default.edmh");
	}
	@Test
	public void tabSpaceTest() {
		testNormalization(4, "tab_space_4.edmh");
		testNormalization(5, "tab_space_5.edmh");
	}
	@Test
	public void spacesPureTest() {
		testNormalization(2, "spaces_2.edmh");
		testNormalization(3, "spaces_3.edmh");
		testNormalization(4, "spaces_4.edmh");
		testNormalization(5, "spaces_5.edmh");
		testNormalization(8, "spaces_8.edmh");
	}
	@Test
	public void slightlyOffTestWDecl() {
		testNormalization(4, "spaces_4_off_decl.edmh");
		testNormalization(5, "spaces_5_off_decl.edmh");
	}
	/**
	 * Should be viewed as a warning to always make an explicit declaration of
	 * tabwidth
	 */
	@Test
	public void slightlyOffTestWODecl() {
		testNormalization(1, "spaces_4_off_nodecl.edmh",
				"normalized_4tab.edmh");
	}
	@Test
	public void spaces8Actually4() {
		testNormalization(4, "spaces_8_actually_4.edmh",
				"normalized_doubletab.edmh");
	}
	public static void testNormalization(int expectedTabwidth, String... paths) {
		try {
			List<String> original = readAll(paths[0]);
			List<String> normExpected = readAll(paths.length == 1 ? "normalized.edmh"
					: paths[1]);
			Pair<List<String>, Integer> normActual = EredmelPreprocessor
					.normalize(original);
			assertEquals("File Size", normExpected.size(), normActual
					.getKey().size());
			assertEquals("Tabwidth", expectedTabwidth, normActual.getValue()
					.intValue());
			for (int i = 0; i < normExpected.size(); i++) {
				assertEquals(format("Line %s:", i), normExpected.get(i),
						normActual.getKey().get(i));
			}
		} catch (IOException | URISyntaxException e) {
			throw new AssertionError(e);
		}
	}
	public static List<String> readAll(String path) throws IOException,
			URISyntaxException {
		return Files.readAllLines(Paths.get(NormalizerTest.class.getResource(
				path).toURI()));
	}
}
