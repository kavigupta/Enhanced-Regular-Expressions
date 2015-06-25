package test.regex.multiplecap;

import static org.junit.Assert.assertEquals;
import openjdk.regex.Matcher;
import openjdk.regex.Pattern;

import org.junit.Test;

public class MultipleCapturesTest {
	@Test
	public void literalTest() {
		assertCorrectIterations("(a)+", "aaa", 1, "a", "a", "a");
		assertCorrectIterations("(abc)+", "abcabc", 1, "abc", "abc");
		assertCorrectIterations("(abc)+(asdf35432)?", "abcabc", 1, "abc",
				"abc");
		assertCorrectIterations("(def)*(abc)*(asdf35432)?", "abcabc", 2,
				"abc", "abc");
	}
	@Test
	public void charclasssTest() {
		assertCorrectIterations("([a-f])+", "abcdefghi", 1, "a", "b", "c",
				"d", "e", "f");
		assertCorrectIterations("(([0-9]+)[^0-9]*)+", "1 2 345 12", 2, "1",
				"2", "345", "12");
	}
	@Test
	public void orTest() {
		assertCorrectIterations("((?<group>we|the|people) ?)+",
				"we the people", "group", "we", "the", "people");
		assertCorrectIterations("(((?<group>we|the|people) ?)+\\.? ?)+",
				"we the people. the we people. people we the.", "group",
				"we", "the", "people", "the", "we", "people", "people",
				"we", "the");
		assertCorrectIterations("((?<group>(we|the|people)\\.? ?)+)+",
				"we the people. the we people. people we the.", "group",
				"we ", "the ", "people. ", "the ", "we ", "people. ",
				"people ", "we ", "the.");
	}
	public static void assertCorrectIterations(String regex, String text,
			int group, String... captures) {
		Matcher mat = Pattern.compile(regex).matcher(text);
		mat.find();
		assertEquals("Iteration Count", captures.length,
				mat.iterations(group));
		for (int i = 0; i < captures.length; i++) {
			assertEquals(i + "th iteration", captures[i],
					mat.getRange(mat.range(group, i)));
		}
	}
	public static void assertCorrectIterations(String regex, String text,
			String group, String... captures) {
		Matcher mat = Pattern.compile(regex).matcher(text);
		mat.find();
		System.out.println(mat.group());
		assertEquals("Iteration Count", captures.length,
				mat.iterations(group));
		for (int i = 0; i < captures.length; i++) {
			assertEquals(i + "th iteration", captures[i],
					mat.getRange(mat.range(group, i)));
		}
	}
}
