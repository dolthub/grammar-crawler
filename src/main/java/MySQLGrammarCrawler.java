import java.io.IOException;
import java.util.*;

public class MySQLGrammarCrawler {
    // TODO: Move this into Crawler, so it doesn't have to reach into this class
    public static Set<String> rulesToSkip = new HashSet<>();
    private static Map<String, Rules.Rule> ruleMap;

    public static void main(String[] args) throws Exception {
        ruleMap = MySQLGrammarUtils.loadMySQLGrammarRules();

        generateCreateTableStatements();
//        generateAllDropStatements();
    }

    private static void generateAllDropStatements() {
        Crawler crawler = new Crawler(ruleMap);
        Rules.Rule rule = ruleMap.get("dropStatement");
        rulesToSkip.add("identifierKeyword");
        rulesToSkip.add("dotIdentifier");
        rulesToSkip.add("roleKeyword");

        crawler.setStatementWriter(new StdOutStatementWriter());
        crawler.startCrawl(rule);
        crawler.printCoverageStats();
    }

    private static void generateCreateTableStatements() throws IOException {
        Crawler crawler = new Crawler(ruleMap);
        crawler.setStatementLimit(1000);

        // Skipping these rules to simplify the output and to make it easier to plug in identifier tokens
        rulesToSkip.add("dotIdentifier");
        rulesToSkip.add("identifierKeyword");
        // This generates double-quoted strings for column names, which MySQL doesn't actually allow.
        rulesToSkip.add("DOUBLE_QUOTED_TEXT");

        // Disabling these to limit crawler's output for CreateTable
        rulesToSkip.add("partitionClause");          // TODO: Can we turn this back on?
        rulesToSkip.add("createTableOptions");       // TODO: We should turn this on soon

        // We should add support for constraints and indexes, but that will require
        // knowledge of type information and table schema when generating statements and
        // when plugging in identifiers.
        rulesToSkip.add("tableConstraintDef");
        rulesToSkip.add("spatialIndexOption");
        rulesToSkip.add("fulltextIndexOption");
        rulesToSkip.add("constraintEnforcement");
        rulesToSkip.add("references");

        // Supporting expressions requires smarter logic for filling in
        // identifiers and type information in order to generate valid expressions
        rulesToSkip.add("expr");
        rulesToSkip.add("queryExpression");
        rulesToSkip.add("queryExpressionOrParens");


        // COLUMN_FORMAT is a feature of MySQL NDB Cluster that allows you to specify a data storage format
        // for individual columns. MySQL silently ignores COLUMN_FORMAT for all other engine types.
        rulesToSkip.add("columnFormat");

        // Dolt doesn't support SECONDARY engine configuration (e.g. MySQL HeatWave configuration options)
        rulesToSkip.add("SECONDARY_SYMBOL");

        // Dolt doesn't support STORAGE options
        rulesToSkip.add("STORAGE_SYMBOL");

        // Dolt doesn't support non-default collations and charsets or underscore charset notation
        rulesToSkip.add("collate");
        rulesToSkip.add("charset");
        rulesToSkip.add("CHARSET_SYMBOL");
        rulesToSkip.add("UNDERSCORE_CHARSET");

        // Generated default values (including auto-increment) aren't smart enough to use the
        // correct type for the default value yet, so skip those rules for now. We could fix this
        // by having the reifier be smart enough to recognize default value tokens and look back
        // at the type of column and then generate a valid value.
        rulesToSkip.add("DEFAULT_SYMBOL");
        rulesToSkip.add("AUTO_INCREMENT_SYMBOL");
        rulesToSkip.add("NOW_SYMBOL");

        // TODO: Temporarily disable the SIGNED keyword, until that change lands in a release
        //       https://github.com/dolthub/vitess/pull/162
        rulesToSkip.add("SIGNED_SYMBOL");

        // Disabling spatial reference IDs
        rulesToSkip.add("SRID_SYMBOL");

        crawler.setCrawlStrategy(new CrawlStrategies.RandomCrawl());

        crawler.setStatementPrefix("CREATE ");
        crawler.setStatementWriters(
                new StdOutStatementWriter(),
                new SQLLogicProtoStatementWriter("sqllogic-test-create-table.proto"));

        crawler.startCrawl(ruleMap.get("createTable"));
        crawler.printCoverageStats();
    }
}
