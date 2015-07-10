package test.regex.multiplecap;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import eredmel.regex.Matcher;
import eredmel.regex.Pattern;

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
	public void backtrackTest() {
		assertCorrectIterations("(.())+b", "abcdefghi", 2, "");
		assertCorrectIterations("(.())*b", "abcdefghi", 2, "");
		assertCorrectIterations("a(.()+)+e", "abcdefghi", 2, "", "", "");
		assertCorrectIterations("a(.()+)?b", "abcdefghi", 2);
		assertCorrectIterations("a(.()+)?c", "abcdefghi", 2, "");
		assertCorrectIterations("(([a-z](?<cap>))+y)+z", "aeyzeyz", "cap",
				"", "", "", "", "");
	}
	@Test
	public void doubleBacktrackTest() {
		assertCorrectIterations("((?<cap>[a-z]())+y)+z", "aeyzeey", "cap",
				"a", "e");
		assertCorrectIterations("(([a-z](?<cap>))+y)+z", "aeyzey", "cap", "",
				"");
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
		ArrayList<String> actualCaps = new ArrayList<>();
		for (int i = 0; i < mat.iterations(group); i++) {
			actualCaps.add(mat.getRange(mat.range(group, i)).toString());
		}
		assertEquals(mat.toString(), Arrays.asList(captures), actualCaps);
	}
	public static void assertCorrectIterations(String regex, String text,
			String group, String... captures) {
		Matcher mat = Pattern.compile(regex).matcher(text);
		mat.find();
		ArrayList<String> actualCaps = new ArrayList<>();
		for (int i = 0; i < mat.iterations(group); i++) {
			actualCaps.add(mat.getRange(mat.range(group, i)).toString());
		}
		System.out.println("Actual Caps " + actualCaps);
		assertEquals(mat.toString(), Arrays.asList(captures), actualCaps);
	}
}
