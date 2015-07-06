package openjdk.regex;

public class CodepointSequence {
	private final int[] buffer;
	public int length;
	public CodepointSequence(int[] temp, int length) {
		this.buffer = temp;
		this.length = length;
	}
	public int pointAt(int i) {
		return buffer[i];
	}
	public void setPointAt(int i, int c) {
		buffer[i] = c;
	}
	public CodepointSequence copyToSize(int size) {
		int[] newtemp = new int[Math.max(size, length)];
		System.arraycopy(buffer, 0, newtemp, 0, length);
		return new CodepointSequence(buffer, length);
	}
	public String toString(int start, int len) {
		return new String(buffer, start, len);
	}
	public static CodepointSequence construct(int length) {
		return new CodepointSequence(new int[length + 10], length);
	}
}
