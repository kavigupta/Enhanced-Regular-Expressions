package eredmel.interpreter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EredmelInterpreter {
	public static final Pattern EREDMEL_PREPROC = Pattern
			.compile("%def[\\s ]*(?<regex>[^%]+)[\\s ]*%->[\\s ]*(?<repl>.*(\r?\n(\t| {4,}).*)*)");
	private EredmelInterpreter() {}
	public static String preprocess(String eredmel) {
		Matcher mat = EREDMEL_PREPROC.matcher(eredmel);
		if (!mat.find()) return eredmel;
		return eredmel.substring(mat.end()).replaceAll(mat.group("regex"),
				mat.group("repl"));
	}
}
