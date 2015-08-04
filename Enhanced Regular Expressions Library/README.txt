The Enhanced Regex Library is a derivative of the OpenJDK java.util.regex library that
has the following enhancements:

    1. The ability to refer to any repetition of a capturing group. For
        example:
            Matcher mat = Pattern.compile("(a.)*").matcher("a1a2a3a4a5");
            mat.find();
            String firstMatch = mat.group(1, 0); // impossible in the original
            String secondMatch = mat.group(1, 1); // impossible in the original
            // ...
            String lastMatch = mat.group(1); // preserve original functionality
    2. The ability to match being in or out of quotes. For example, "~^"//.*"
        matches any valid Java to-end-of-line comment, like
            println("hello!"); // printlns "hello!"
        but not
            println("hello!//"); syntax error!
    3. The ability to match matching parentheses. For example ~(.+~) will match
        the following:
            1 2 3 {
            (1 + 2) * (3 + 4) - (5 * 6)
            ("%s, %s, %s"), (124+34), 123, 23
            (1 2 3] [4 5 6)
            123 + '))((' + 456
        but not the following:
            (abcdef
            abc)(def
