/*
 * Unmodified from original openjdk.regex package.
 */
package eredmel.regex;

import java.util.HashMap;

import eredmel.regex.CharProperty.All;
import eredmel.regex.CharProperty.Category;
import eredmel.regex.CharProperty.Ctype;

/**
 * Utility class for dealing with character property names.
 */
class CharPropertyNames {
	static CharProperty charPropertyFor(String name) {
		CharPropertyFactory m = map.get(name);
		return m == null ? null : m.make();
	}
	private static abstract class CharPropertyFactory {
		abstract CharProperty make();
	}
	private static void defCategory(String name, final int typeMask) {
		map.put(name, new CharPropertyFactory() {
			@Override
			CharProperty make() {
				return new Category(typeMask);
			}
		});
	}
	private static void defRange(String name, final int lower, final int upper) {
		map.put(name, new CharPropertyFactory() {
			@Override
			CharProperty make() {
				return Pattern.rangeFor(lower, upper);
			}
		});
	}
	private static void defCtype(String name, final int ctype) {
		map.put(name, new CharPropertyFactory() {
			@Override
			CharProperty make() {
				return new Ctype(ctype);
			}
		});
	}
	private static abstract class CloneableProperty extends CharProperty
			implements Cloneable {
		@Override
		public CloneableProperty clone() {
			try {
				return (CloneableProperty) super.clone();
			} catch (CloneNotSupportedException e) {
				throw new AssertionError(e);
			}
		}
	}
	private static void defClone(String name, final CloneableProperty p) {
		map.put(name, new CharPropertyFactory() {
			@Override
			CharProperty make() {
				return p.clone();
			}
		});
	}
	private static final HashMap<String, CharPropertyFactory> map = new HashMap<>();
	static {
		// Unicode character property aliases, defined in
		// http://www.unicode.org/Public/UNIDATA/PropertyValueAliases.txt
		defCategory("Cn", 1 << Character.UNASSIGNED);
		defCategory("Lu", 1 << Character.UPPERCASE_LETTER);
		defCategory("Ll", 1 << Character.LOWERCASE_LETTER);
		defCategory("Lt", 1 << Character.TITLECASE_LETTER);
		defCategory("Lm", 1 << Character.MODIFIER_LETTER);
		defCategory("Lo", 1 << Character.OTHER_LETTER);
		defCategory("Mn", 1 << Character.NON_SPACING_MARK);
		defCategory("Me", 1 << Character.ENCLOSING_MARK);
		defCategory("Mc", 1 << Character.COMBINING_SPACING_MARK);
		defCategory("Nd", 1 << Character.DECIMAL_DIGIT_NUMBER);
		defCategory("Nl", 1 << Character.LETTER_NUMBER);
		defCategory("No", 1 << Character.OTHER_NUMBER);
		defCategory("Zs", 1 << Character.SPACE_SEPARATOR);
		defCategory("Zl", 1 << Character.LINE_SEPARATOR);
		defCategory("Zp", 1 << Character.PARAGRAPH_SEPARATOR);
		defCategory("Cc", 1 << Character.CONTROL);
		defCategory("Cf", 1 << Character.FORMAT);
		defCategory("Co", 1 << Character.PRIVATE_USE);
		defCategory("Cs", 1 << Character.SURROGATE);
		defCategory("Pd", 1 << Character.DASH_PUNCTUATION);
		defCategory("Ps", 1 << Character.START_PUNCTUATION);
		defCategory("Pe", 1 << Character.END_PUNCTUATION);
		defCategory("Pc", 1 << Character.CONNECTOR_PUNCTUATION);
		defCategory("Po", 1 << Character.OTHER_PUNCTUATION);
		defCategory("Sm", 1 << Character.MATH_SYMBOL);
		defCategory("Sc", 1 << Character.CURRENCY_SYMBOL);
		defCategory("Sk", 1 << Character.MODIFIER_SYMBOL);
		defCategory("So", 1 << Character.OTHER_SYMBOL);
		defCategory("Pi", 1 << Character.INITIAL_QUOTE_PUNCTUATION);
		defCategory("Pf", 1 << Character.FINAL_QUOTE_PUNCTUATION);
		defCategory(
				"L",
				((1 << Character.UPPERCASE_LETTER)
						| (1 << Character.LOWERCASE_LETTER)
						| (1 << Character.TITLECASE_LETTER)
						| (1 << Character.MODIFIER_LETTER) | (1 << Character.OTHER_LETTER)));
		defCategory(
				"M",
				((1 << Character.NON_SPACING_MARK)
						| (1 << Character.ENCLOSING_MARK) | (1 << Character.COMBINING_SPACING_MARK)));
		defCategory(
				"N",
				((1 << Character.DECIMAL_DIGIT_NUMBER)
						| (1 << Character.LETTER_NUMBER) | (1 << Character.OTHER_NUMBER)));
		defCategory(
				"Z",
				((1 << Character.SPACE_SEPARATOR)
						| (1 << Character.LINE_SEPARATOR) | (1 << Character.PARAGRAPH_SEPARATOR)));
		defCategory(
				"C",
				((1 << Character.CONTROL) | (1 << Character.FORMAT)
						| (1 << Character.PRIVATE_USE) | (1 << Character.SURROGATE))); // Other
		defCategory(
				"P",
				((1 << Character.DASH_PUNCTUATION)
						| (1 << Character.START_PUNCTUATION)
						| (1 << Character.END_PUNCTUATION)
						| (1 << Character.CONNECTOR_PUNCTUATION)
						| (1 << Character.OTHER_PUNCTUATION)
						| (1 << Character.INITIAL_QUOTE_PUNCTUATION) | (1 << Character.FINAL_QUOTE_PUNCTUATION)));
		defCategory(
				"S",
				((1 << Character.MATH_SYMBOL)
						| (1 << Character.CURRENCY_SYMBOL)
						| (1 << Character.MODIFIER_SYMBOL) | (1 << Character.OTHER_SYMBOL)));
		defCategory(
				"LC",
				((1 << Character.UPPERCASE_LETTER)
						| (1 << Character.LOWERCASE_LETTER) | (1 << Character.TITLECASE_LETTER)));
		defCategory(
				"LD",
				((1 << Character.UPPERCASE_LETTER)
						| (1 << Character.LOWERCASE_LETTER)
						| (1 << Character.TITLECASE_LETTER)
						| (1 << Character.MODIFIER_LETTER)
						| (1 << Character.OTHER_LETTER) | (1 << Character.DECIMAL_DIGIT_NUMBER)));
		defRange("L1", 0x00, 0xFF); // Latin-1
		map.put("all", new CharPropertyFactory() {
			@Override
			CharProperty make() {
				return new All();
			}
		});
		// Posix regular expression character classes, defined in
		// http://www.unix.org/onlinepubs/009695399/basedefs/xbd_chap09.html
		defRange("ASCII", 0x00, 0x7F); // ASCII
		defCtype("Alnum", ASCII.ALNUM); // Alphanumeric characters
		defCtype("Alpha", ASCII.ALPHA); // Alphabetic characters
		defCtype("Blank", ASCII.BLANK); // Space and tab characters
		defCtype("Cntrl", ASCII.CNTRL); // Control characters
		defRange("Digit", '0', '9'); // Numeric characters
		defCtype("Graph", ASCII.GRAPH); // printable and visible
		defRange("Lower", 'a', 'z'); // Lower-case alphabetic
		defRange("Print", 0x20, 0x7E); // Printable characters
		defCtype("Punct", ASCII.PUNCT); // Punctuation characters
		defCtype("Space", ASCII.SPACE); // Space characters
		defRange("Upper", 'A', 'Z'); // Upper-case alphabetic
		defCtype("XDigit", ASCII.XDIGIT); // hexadecimal digits
		// Java character properties, defined by methods in
		// Character.java
		defClone("javaLowerCase", new CloneableProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				return Character.isLowerCase(ch);
			}
		});
		defClone("javaUpperCase", new CloneableProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				return Character.isUpperCase(ch);
			}
		});
		defClone("javaAlphabetic", new CloneableProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				return Character.isAlphabetic(ch);
			}
		});
		defClone("javaIdeographic", new CloneableProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				return Character.isIdeographic(ch);
			}
		});
		defClone("javaTitleCase", new CloneableProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				return Character.isTitleCase(ch);
			}
		});
		defClone("javaDigit", new CloneableProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				return Character.isDigit(ch);
			}
		});
		defClone("javaDefined", new CloneableProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				return Character.isDefined(ch);
			}
		});
		defClone("javaLetter", new CloneableProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				return Character.isLetter(ch);
			}
		});
		defClone("javaLetterOrDigit", new CloneableProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				return Character.isLetterOrDigit(ch);
			}
		});
		defClone("javaJavaIdentifierStart", new CloneableProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				return Character.isJavaIdentifierStart(ch);
			}
		});
		defClone("javaJavaIdentifierPart", new CloneableProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				return Character.isJavaIdentifierPart(ch);
			}
		});
		defClone("javaUnicodeIdentifierStart", new CloneableProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				return Character.isUnicodeIdentifierStart(ch);
			}
		});
		defClone("javaUnicodeIdentifierPart", new CloneableProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				return Character.isUnicodeIdentifierPart(ch);
			}
		});
		defClone("javaIdentifierIgnorable", new CloneableProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				return Character.isIdentifierIgnorable(ch);
			}
		});
		defClone("javaSpaceChar", new CloneableProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				return Character.isSpaceChar(ch);
			}
		});
		defClone("javaWhitespace", new CloneableProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				return Character.isWhitespace(ch);
			}
		});
		defClone("javaISOControl", new CloneableProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				return Character.isISOControl(ch);
			}
		});
		defClone("javaMirrored", new CloneableProperty() {
			@Override
			boolean isSatisfiedBy(int ch) {
				return Character.isMirrored(ch);
			}
		});
	}
}