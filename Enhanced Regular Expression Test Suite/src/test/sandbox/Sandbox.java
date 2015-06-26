package test.sandbox;

import openjdk.regex.Matcher;
import openjdk.regex.Pattern;

public class Sandbox {
	public static void main(String[] args) {
		Matcher mat = Pattern.compile("regex(?<name>)").matcher("regex");
		mat.find();
		System.out.println(mat.start(1));
	}
}
