package eredmel.object;

import java.util.HashMap;
import java.util.Map.Entry;

import enregex.matcher.EREMatcher;

public class EredmelVariableRoster {
	private HashMap<String, EredmelObject> variables;
	public EredmelVariableRoster() {
		variables = new HashMap<>();
	}
	public String expand(String line) {
		for (Entry<String, EredmelObject> var : variables.entrySet()) {
			line = EREMatcher.processERE("[^A-Za-z]" + var.getKey()
					+ "^A-Za-z", line, x -> var.getValue().asString());
		}
		return line;
	}
}
