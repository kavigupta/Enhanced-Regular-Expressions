package eredmel.test.enregex;

import static org.junit.Assert.assertArrayEquals;
import enregex.matcher.EREMatch;
import enregex.matcher.EREMatcher;

public class EREUtilities {
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
	}
}
