package eredmel.regex;

import java.util.logging.Level;
import java.util.logging.Logger;

// TODO allow for smarter lazy evaluation
public class EnregexSegment implements CharSequence {
	private final CharSequence backing;
	public final EnregexType type;
	private EnregexMetadata[] metadata;
	private boolean compiled;
	private final int start, end;
	private EnregexSegment(CharSequence str, EnregexType type,
			EnregexMetadata[] metadata, int start, int end) {
		this.backing = str;
		this.type = type;
		this.metadata = metadata;
		this.compiled = metadata != null;
		this.start = start;
		this.end = end;
	}
	public static EnregexSegment getInstance(CharSequence str, EnregexType type) {
		return new EnregexSegment(str, type, null, 0, str.length());
	}
	private void compile() {
		if (compiled) return;
		compiled = true;
		metadata = new EnregexMetadata[backing.length() + 1];
		metadata[0] = EnregexMetadata.startOfString(type);
		for (int i = 1; i < metadata.length; i++) {
			metadata[i] = metadata[i - 1].next(backing.charAt(i - 1));
		}
	}
	public boolean parensMatch(int i, int j, int closeParen) {
		if (!metadataAt(i).equalParenState(metadataAt(j), closeParen))
			return false;
		Logger.getGlobal().log(Level.FINE, "Equal Paren State");
		for (int k = i + 1; k < j; k++) {
			if (!metadataAt(k).greaterOrEqualParenState(metadataAt(i),
					closeParen)) return false;
		}
		return true;
	}
	public boolean quoteTypeMatches(int loc, boolean positive, int openQuote) {
		return metadataAt(loc).quoteTypeMatches(positive, openQuote);
	}
	@Override
	public char charAt(int index) {
		return backing.charAt(index + start);
	}
	public EnregexMetadata metadataAt(int index) {
		compile();
		return metadata[index + start];
	}
	@Override
	public int length() {
		return end - start;
	}
	@Override
	public EnregexSegment subSequence(int start, int end) {
		return new EnregexSegment(backing, type, metadata,
				this.start + start, this.start + end);
	}
	@Override
	public String toString() {
		return backing.subSequence(start, end).toString();
	}
}
