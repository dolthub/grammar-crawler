import java.util.Map;

public class DropStatementsExample {
    private static Map<String, Rules.Rule> ruleMap;

    public static void main(String[] args) throws Exception {
        ruleMap = MySQLGrammarUtils.loadMySQLGrammarRules();

        generateAllDropStatements();
    }

    private static void generateAllDropStatements() {
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
