package eredmel.interpreter;

import java.util.regex.Pattern;

public final class EredmelInterpreter {
	public static final Pattern EREDMEL_PREPROC = Pattern
			.compile("%def[\\s ]*(?<regex>[^%]+)[\\s ]*%->[\\s ]*(?<repl>.*(\r?\n(\t| {4,}).*)*)");
	private EredmelInterpreter() {}
}
