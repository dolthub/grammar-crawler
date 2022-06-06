import java.io.IOException;
import java.util.Map;

public class DropStatementsExample {
    private static Map<String, Rules.Rule> ruleMap;

    public static void main(String[] args) throws IOException {
        ruleMap = MySQLGrammarUtils.loadMySQLGrammarRules();

        Crawler crawler = new Crawler(ruleMap);
        Rules.Rule rule = ruleMap.get("dropStatement");

        crawler.addRulesToSkip(
                "identifierKeyword",
                "dotIdentifier",
                "roleKeyword");

        crawler.setStatementWriter(new StdOutStatementWriter());
        crawler.startCrawl(rule);
        crawler.printCoverageStats();
    }
}
