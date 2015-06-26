package eredmel.test.enregex;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import enregex.matcher.EREMatcher;
import eredmel.enregex.EREMatch;

public class ERETestUtil {
	public static void assertMatch(String enregex, String text,
			int[][] startends) {
		assertMatch(new EREMatcher(enregex, text), startends);
	}
	public static void assertMatch(EREMatcher mat, int[][] startends) {
		for (int i = 0; i < startends.length; i++) {
			EREMatch match = mat.find();
			assertArrayEquals(startends[i],
					new int[] { match.start(), match.end() });
		}
		assertEquals(null, mat.find());
	}
}
