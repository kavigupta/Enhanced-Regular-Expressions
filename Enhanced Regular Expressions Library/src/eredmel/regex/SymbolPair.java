package eredmel.regex;

public class SymbolPair implements java.io.Serializable {
	public final char open, close;
	public final boolean openEscaped, closeEscaped;
	public SymbolPair(char open, char close, boolean openEscaped,
			boolean closeEscaped) {
		this.open = open;
		this.close = close;
		this.openEscaped = openEscaped;
		this.closeEscaped = closeEscaped;
	}
	public boolean same() {
		return open == close;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + close;
		result = prime * result + (closeEscaped ? 1231 : 1237);
		result = prime * result + open;
		result = prime * result + (openEscaped ? 1231 : 1237);
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		SymbolPair other = (SymbolPair) obj;
		if (close != other.close) return false;
		if (closeEscaped != other.closeEscaped) return false;
		if (open != other.open) return false;
		if (openEscaped != other.openEscaped) return false;
		return true;
	}
	@Override
	public String toString() {
		return open + "" + close;
	}
	public boolean openMatches(char next, int slashcount) {
		if (next != open) return false;
		if (slashcount % 2 != 0 && openEscaped) return false;
		return true;
	}
	public boolean closeMatches(char next, int slashcount) {
		if (next != close) return false;
		if (slashcount % 2 != 0 && closeEscaped) return false;
		return true;
	}
}
