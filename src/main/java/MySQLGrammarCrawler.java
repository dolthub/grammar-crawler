import java.util.*;

public class MySQLGrammarCrawler {
    // TODO: Move this into Crawler, so it doesn't have to reach into this class
    public static Set<String> rulesToSkip = new HashSet<>();
    private static Map<String, Rules.Rule> ruleMap;

    public static void main(String[] args) throws Exception {
        ruleMap = MySQLGrammarUtils.loadMySQLGrammarRules();

//        generateCreateTableStatements();
        generateAllDropStatements();
    }

    private static void generateAllDropStatements() {
        Crawler crawler = new Crawler(ruleMap);
        Rules.Rule rule = ruleMap.get("dropStatement");
        rulesToSkip.add("identifierKeyword");
        rulesToSkip.add("dotIdentifier");
        rulesToSkip.add("roleKeyword");

        crawler.setStatementWriter(new StdOutStatementWriter());
//        crawler.setCrawlStrategy(new CrawlStrategies.RandomCrawl());
        crawler.startCrawl(rule);
    }

    private static void generateCreateTableStatements() {
        Crawler crawler = new Crawler(ruleMap);
        Rules.Rule rule = ruleMap.get("createTable");

        // Configure crawling rules...
        // Skipping these rules to simplify the output and to make it easier to plug in identifier tokens
        rulesToSkip.add("dotIdentifier");
        rulesToSkip.add("identifierKeyword");

        // Disabling these to limit crawler's output for CreateTable
        rulesToSkip.add("procedureAnalyseClause");
        rulesToSkip.add("expr");
        rulesToSkip.add("queryExpression");
        rulesToSkip.add("queryExpressionOrParens");
        rulesToSkip.add("partitionClause");
        rulesToSkip.add("createTableOptions");
        rulesToSkip.add("tableConstraintDef");
        rulesToSkip.add("spatialIndexOption");
        rulesToSkip.add("fulltextIndexOption");

        crawler.setCrawlStrategy(new CrawlStrategies.RandomCrawl());

        crawler.setStatementPrefix("CREATE ");
        crawler.setStatementWriter(new StdOutStatementWriter());
//        crawler.setStatementWriter(new SQLLogicProtoStatementWriter("sqllogic-test.proto"););

        crawler.startCrawl(rule);
    }
}
