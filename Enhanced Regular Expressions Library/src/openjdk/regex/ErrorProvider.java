package openjdk.regex;

interface ErrorProvider {
	PatternSyntaxException error(String string);
}
