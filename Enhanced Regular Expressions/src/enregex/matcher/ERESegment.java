package enregex.matcher;


public class ERESegment implements CharSequence {
	private final String backing;
	private final int[] parens;
	private final boolean[] inQuote;
	private final int start, end;
	private ERESegment(String backing, int[] parens, boolean[] inQuote,
			int start, int end) {
		this.backing = backing;
		this.parens = parens;
		this.inQuote = inQuote;
		this.start = start;
		this.end = end;
	}
	public static ERESegment getInstance(String str) {
		int[] parens = new int[str.length()];
		boolean[] inQuote = new boolean[str.length()];
		boolean quote = false;
		int slashct = 0, parenct = 0;
		for (int i = 0; i < str.length(); i++) {
			if (slashct % 2 == 0 && str.charAt(i) == '\'') quote = !quote;
			if (!quote) {
				if (str.charAt(i) == '(') parenct++;
				if (str.charAt(i) == ')') parenct--;
			}
			parens[i] = parenct;
			inQuote[i] = quote;
		}
		return new ERESegment(str, parens, inQuote, 0, str.length());
	}
	@Override
	public char charAt(int index) {
		return backing.charAt(index + start);
	}
	@Override
	public int length() {
		return end - start;
	}
	@Override
	public ERESegment subSequence(int start, int end) {
		return new ERESegment(backing, parens, inQuote, start + start, start
				+ end);
	}
}
