package eredmel.test.preprocessor.normalizer;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

import eredmel.preprocessor.EredmelPreprocessor;

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
	public void tabSpace4Test() {
		testNormalization("tab_space_4.edmh");
	}
	@Test
	public void tabSpace5Test() {
		testNormalization("tab_space_5.edmh");
	}
	@Test
	public void spaces_2() {
		testNormalization("spaces_2.edmh");
	}
	@Test
	public void spaces_3() {
		testNormalization("spaces_3.edmh");
	}
	@Test
	public void spaces_4() {
		testNormalization("spaces_4.edmh");
	}
	@Test
	public void spaces_5() {
		testNormalization("spaces_5.edmh");
	}
	@Test
	public void spaces_8() {
		testNormalization("spaces_8.edmh");
	}
	@Test
	public void spaces_8_actually4() {
		testNormalization("spaces_8_actually_4.edmh",
				"normalized_doubletab.edmh");
	}
	public static void testNormalization(String... paths) {
		try {
			List<String> or = readAll(paths[0]);
			List<String> no = readAll(paths.length == 1 ? "normalized.edmh"
					: paths[1]);
			assertEquals(no, EredmelPreprocessor.normalize(or));
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
