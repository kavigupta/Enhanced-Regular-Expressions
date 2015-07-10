package test.sandbox;

import eredmel.regex.Matcher;
import eredmel.regex.Pattern;

public class Sandbox {
	public static void main(String[] args) {
		Matcher mat = Pattern.compile("regex(?<name>)").matcher("regex");
		mat.find();
		System.out.println(mat.start(1));
	}
}
