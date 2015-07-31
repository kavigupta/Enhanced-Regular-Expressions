package eredmel.state;

public interface EredmelMacroState {
	public boolean isDefined(String macroName);
	public int idOf(String macroName);
	public String valueOf(int macroID);
}
