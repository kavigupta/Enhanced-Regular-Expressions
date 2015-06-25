package eredmel.object;

public class EredmelState {
	private EredmelVariableRoster evr;
	private EredmelMacroRoster emr;
	public void interpret(String line) {
		evr.expand(line);
	}
}
