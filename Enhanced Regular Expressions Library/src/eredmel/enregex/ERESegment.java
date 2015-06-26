package eredmel.enregex;

public class ERESegment implements CharSequence {
	private final String backing;
	private final EREMetadata[] metadata;
	private final int start, end;
	private ERESegment(String backing, EREMetadata[] metadata, int start,
			int end) {
		this.backing = backing;
		this.metadata = metadata;
		this.start = start;
		this.end = end;
	}
	public static ERESegment getInstance(String str, EnregexType type) {
		EREMetadata[] metadata = new EREMetadata[str.length() + 1];
		metadata[0] = EREMetadata.startOfString(type);
		for (int i = 1; i < metadata.length; i++) {
			metadata[i] = metadata[i - 1].next(str.charAt(i - 1));
		}
		return new ERESegment(str, metadata, 0, str.length());
	}
	public boolean parensMatch(int i, int j) {
		if (!metadata[i].equalParenState(metadata[j])) return false;
		for (int k = i + 1; k < j; k++) {
			if (!metadata[k].greaterOrEqualParenState(metadata[i]))
				return false;
		}
		return true;
	}
	public boolean quoteTypeMatches(int loc, int quoteType) {
		return metadata[loc].quoteTypeMatches(quoteType);
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
		return new ERESegment(backing, metadata, start + start, start + end);
	}
	@Override
	public String toString() {
		return backing;
	}
}
