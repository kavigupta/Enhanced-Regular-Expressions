package eredmel.test.enregex;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import eredmel.enregex.EnregexMatch;
import eredmel.enregex.EnregexPattern;
import eredmel.enregex.EnregexType;

public class EnregexTestUtil {
	public static EnregexType TO_USE = EnregexType.EREDMEL;
	public static void assertMatch(String enregex, String text,
			int[][] startends) {
		int i = 0;
		ArrayList<EnregexMatch> matches = EnregexPattern.getInstance(enregex,
				TO_USE).process(text);
		System.out.println(matches);
		for (EnregexMatch m : matches) {
			assertArrayEquals(i + "th element", startends[i],
					new int[] { m.start(), m.end() });
			i++;
		}
		assertEquals("Match Count", startends.length, i);
	}
}
