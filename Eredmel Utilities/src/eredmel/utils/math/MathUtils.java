package eredmel.utils.math;

public class MathUtils {
	public static int gcf(int a, int b) {
		a = Math.abs(a);
		b = Math.abs(b);
		int t;
		while (b != 0) {
			t = b;
			b = a % b;
			a = t;
		}
		return a;
	}
}
