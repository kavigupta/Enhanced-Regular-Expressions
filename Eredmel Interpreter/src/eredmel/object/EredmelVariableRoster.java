package eredmel.object;

import java.util.HashMap;
import java.util.Map.Entry;

import eredmel.enregex.EnregexPattern;
import eredmel.regex.EnregexType;

public class EredmelVariableRoster {
	private HashMap<String, EredmelObject> variables;
	public EredmelVariableRoster() {
		variables = new HashMap<>();
	}
	public String expand(String line) {
		for (Entry<String, EredmelObject> var : variables.entrySet()) {
			line = EnregexPattern.compile(
					"[^A-Za-z]" + var.getKey() + "^A-Za-z",
					EnregexType.EREDMEL_STANDARD).process(line,
					x -> var.getValue().asString());
		}
		return line;
	}
}
