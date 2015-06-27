package eredmel.test.enregex;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import eredmel.enregex.EREMatch;
import eredmel.enregex.EnregexPattern;
import eredmel.enregex.EnregexType;

public class ERETestUtil {
	public static void assertMatch(String enregex, String text,
			int[][] startends) {
		int i = 0;
		ArrayList<EREMatch> matches = EnregexPattern.getInstance(enregex,
				EnregexType.EREDMEL).process(text);
		for (EREMatch m : matches) {
			assertArrayEquals(i + "th element", startends[i],
					new int[] { m.start(), m.end() });
			i++;
		}
		assertEquals("Match Count", startends.length, i);
	}
}
