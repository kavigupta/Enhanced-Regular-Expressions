package eredmel.enregex;

public class SymbolPair {
	public final char open, close;
	public SymbolPair(char open, char close) {
		this.open = open;
		this.close = close;
	}
	public boolean same() {
		return open == close;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + close;
		result = prime * result + open;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		SymbolPair other = (SymbolPair) obj;
		if (close != other.close) return false;
		if (open != other.open) return false;
		return true;
	}
	@Override
	public String toString() {
		return open + "" + close;
	}
}
