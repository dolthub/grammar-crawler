import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;

import static org.junit.Assert.*;

import parser.ANTLRv4Parser;

import java.util.Map;

public class MySQLGrammarUtilsTests {

    @Test
    public void testParseMySQLGrammar() throws Exception {
        ANTLRv4Parser parser = MySQLGrammarUtils.parseMySQLGrammar();
        assertNotNull(parser);

        ParseTree tree = parser.grammarSpec();
        assertNotNull(tree);
        assertEquals("MySQLParser", tree.getChild(0).getChild(1).getText());
    }

    @Test
    public void testLoadMySQLGrammarRules() throws Exception {
        Map<String, MySQLGrammarCrawler.Rule> ruleMap = MySQLGrammarUtils.loadMySQLGrammarRules();
        assertFalse(ruleMap.isEmpty());
        assertTrue(ruleMap.size() > 100);
        assertTrue(ruleMap.containsKey("query"));
    }
}
