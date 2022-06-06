import java.io.IOException;
import java.util.*;

public class CreateTableStatementsExample {
    private static Map<String, Rules.Rule> ruleMap;

    public static void main(String[] args) throws IOException {
        ruleMap = MySQLGrammarUtils.loadMySQLGrammarRules();

        Crawler crawler = new Crawler(ruleMap);
        crawler.setStatementLimit(50_000);

        // Skipping these rules to simplify the output and to make it easier to plug in identifier tokens
        // DOUBLE_QUOTED_TEXT generates double-quoted strings for column names, which MySQL doesn't actually allow.
        crawler.addRulesToSkip(
                "dotIdentifier",
                "identifierKeyword",
                "DOUBLE_QUOTED_TEXT");

        // Disabling these to limit crawler's output for CreateTable
        crawler.addRulesToSkip("partitionClause");    // TODO: Can we turn this back on?
        crawler.addRulesToSkip("createTableOptions"); // TODO: We should turn this on soon

        // We should add support for constraints and indexes, but that will require
        // knowledge of type information and table schema when generating statements and
        // when plugging in identifiers.
        crawler.addRulesToSkip(
                "tableConstraintDef",
                "spatialIndexOption",
                "fulltextIndexOption",
                "constraintEnforcement",
                "references");

        // Supporting expressions requires smarter logic for filling in
        // identifiers and type information in order to generate valid expressions
        crawler.addRulesToSkip("expr",
                "queryExpression",
                "queryExpressionOrParens");


        // COLUMN_FORMAT is a feature of MySQL NDB Cluster that allows you to specify a data storage format
        // for individual columns. MySQL silently ignores COLUMN_FORMAT for all other engine types.
        crawler.addRulesToSkip("columnFormat");

        // Dolt doesn't support SECONDARY engine configuration (e.g. MySQL HeatWave configuration options)
        crawler.addRulesToSkip("SECONDARY_SYMBOL");

        // Dolt doesn't support STORAGE options
        crawler.addRulesToSkip("STORAGE_SYMBOL");

        // Dolt doesn't support non-default collations and charsets or underscore charset notation
        crawler.addRulesToSkip(
                "collate",
                "charset",
                "CHARSET_SYMBOL",
                "UNDERSCORE_CHARSET");

        // Generated default values (including auto-increment) aren't smart enough to use the
        // correct type for the default value yet, so skip those rules for now. We could fix this
        // by having the reifier be smart enough to recognize default value tokens and look back
        // at the type of column and then generate a valid value.
        crawler.addRulesToSkip(
                "DEFAULT_SYMBOL",
                "AUTO_INCREMENT_SYMBOL",
                "NOW_SYMBOL");

        // Disabling spatial reference IDs
        crawler.addRulesToSkip("SRID_SYMBOL");

        crawler.setCrawlStrategy(
                new CrawlStrategies.CoverageAwareCrawl(crawler));


        crawler.setStatementPrefix("CREATE ");
        crawler.setStatementWriters(
                new StdOutStatementWriter(),
                new SQLLogicProtoStatementWriter("sqllogic-test-create-table.proto"));

        crawler.startCrawl(ruleMap.get("createTable"));
        crawler.printCoverageStats();
    }
}
