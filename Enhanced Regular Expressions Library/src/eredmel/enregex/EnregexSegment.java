package eredmel.enregex;

public class EnregexSegment implements CharSequence {
	private final String backing;
	final EnregexMatcher[] metadata;
	private final int start, end;
	private EnregexSegment(String backing, EnregexMatcher[] metadata, int start,
			int end) {
		this.backing = backing;
		this.metadata = metadata;
		this.start = start;
		this.end = end;
	}
	public static EnregexSegment getInstance(String str, EnregexType type) {
		EnregexMatcher[] metadata = new EnregexMatcher[str.length() + 1];
		metadata[0] = EnregexMatcher.startOfString(type);
		for (int i = 1; i < metadata.length; i++) {
			metadata[i] = metadata[i - 1].next(str.charAt(i - 1));
		}
		return new EnregexSegment(str, metadata, 0, str.length());
	}
	public boolean parensMatch(int i, int j) {
		if (!metadataAt(i).equalParenState(metadataAt(j))) return false;
		System.out.println("Equal Paren State");
		for (int k = i + 1; k < j; k++) {
			if (!metadataAt(k).greaterOrEqualParenState(metadataAt(i)))
				return false;
		}
		return true;
	}
	public boolean quoteTypeMatches(int loc, int quoteType) {
		return metadataAt(loc).quoteTypeMatches(quoteType);
	}
	@Override
	public char charAt(int index) {
		return backing.charAt(index + start);
	}
	public EnregexMatcher metadataAt(int index) {
		return metadata[index + start];
	}
	@Override
	public int length() {
		return end - start;
	}
	@Override
	public EnregexSegment subSequence(int start, int end) {
		return new EnregexSegment(backing, metadata, this.start + start,
				this.start + end);
	}
	@Override
	public String toString() {
		return backing.substring(start, end);
	}
}
