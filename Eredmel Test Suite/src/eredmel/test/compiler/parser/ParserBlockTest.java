// package eredmel.test.compiler.parser;
//
// import static org.junit.Assert.assertEquals;
//
// import java.util.Arrays;
//
// import org.junit.Test;
//
// import eredmel.interpreter.EredmelParser;
//
// public class ParserBlockTest {
// @Test
// public void singleBlockTest() {
// assertBlockParse("native exp 1", "native exp 1");
// assertBlockParse("define exp (?<x>.+)" + "\n\tx+2",
// "define exp (?<x>.+)" + "\n\tx+2");
// assertBlockParse("define exp (?<x>.+)" + "\n\tx+2" + "\n\tx+3",
// "define exp (?<x>.+)" + "\n\tx+2" + "\n\tx+3");
// assertBlockParse("define exp \\n(?<x>.+)" + "\n\tx+2" + "\n\tx+3",
// "define exp \\n(?<x>.+)" + "\n\tx+2" + "\n\tx+3");
// assertBlockParse("define println (?<fst>.).+\n\t"
// + "if ${fst} == '2'\n\t\t" + "pchar ${fst}\n\t"
// + "else pchar '3'", "define println (?<fst>.).+\n\t"
// + "if ${fst} == '2'\n\t\t" + "pchar ${fst}\n\t"
// + "else pchar '3'");
// }
// @Test
// public void multiLineTest() {
// assertBlockParse(
// "native exp 1\nnative exp 2\nnative exp 3\nnative exp 4",
// "native exp 1", "native exp 2", "native exp 3",
// "native exp 4");
// }
// private void assertBlockParse(String block, String... subblocks) {
// assertEquals(Arrays.asList(subblocks),
// EredmelParser.parseBlock(block));
// }
// }
