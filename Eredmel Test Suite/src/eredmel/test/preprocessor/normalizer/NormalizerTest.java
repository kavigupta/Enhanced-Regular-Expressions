package eredmel.test.preprocessor.normalizer;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

import eredmel.preprocessor.EredmelNormalizer;

public class NormalizerTest {
	@Test
	public void tabTest() {
		testNormalization("tab.edmh");
	}
	@Test
	public void tabSpaceDefaultTest() {
		testNormalization("tab_space_default.edmh");
	}
	@Test
	public void tabSpaceTest() {
		testNormalization("tab_space_4.edmh");
		testNormalization("tab_space_5.edmh");
	}
	@Test
	public void spacesPureTest() {
		testNormalization("spaces_2.edmh");
		testNormalization("spaces_3.edmh");
		testNormalization("spaces_4.edmh");
		testNormalization("spaces_5.edmh");
		testNormalization("spaces_8.edmh");
	}
	@Test
	public void slightlyOffTestWDecl() {
		testNormalization("spaces_4_off_decl.edmh");
		testNormalization("spaces_5_off_decl.edmh");
	}
	/**
	 * Should be viewed as a warning to always make an explicit declaration of
	 * tabwidth
	 */
	@Test
	public void slightlyOffTestWODecl() {
		testNormalization("spaces_4_off_nodecl.edmh", "normalized_4tab.edmh");
	}
	@Test
	public void spaces8Actually4() {
		testNormalization("spaces_8_actually_4.edmh",
				"normalized_doubletab.edmh");
	}
	public static void testNormalization(String... paths) {
		try {
			List<String> original = readAll(paths[0]);
			List<String> normExpected = readAll(paths.length == 1 ? "normalized.edmh"
					: paths[1]);
			List<String> normActual = EredmelNormalizer.normalize(original);
			assertEquals("Size Mismatch", normExpected.size(),
					normActual.size());
			for (int i = 0; i < normExpected.size(); i++) {
				assertEquals(i + "th line", normExpected.get(i),
						normActual.get(i));
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
