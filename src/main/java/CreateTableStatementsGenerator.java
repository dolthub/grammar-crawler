// Copyright 2022 Dolthub, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import java.util.*;

public class CreateTableStatementsGenerator {
    public static void main(String[] args) throws Exception {
        Map<String, Rules.Rule> ruleMap = MySQLGrammarUtils.loadMySQLGrammarRules();

        Crawler crawler = new Crawler(ruleMap);
        crawler.setStatementLimit(10_000);

        // Skipping these rules to simplify the output and to make it easier to plug in identifier tokens
        // DOUBLE_QUOTED_TEXT generates double-quoted strings for column names, which MySQL doesn't actually allow.
        crawler.addRulesToSkip(
                "dotIdentifier",
                "identifierKeyword",
                "DOUBLE_QUOTED_TEXT");

        // Disabling these to limit crawler's output for CreateTable
        crawler.addRulesToSkip("partitionClause");
        crawler.addRulesToSkip("partitionOption");

        // We should add support for constraints and indexes, but that will require
        // knowledge of type information and table schema when generating statements
        // and plugging in identifiers.
        crawler.addRulesToSkip(
                "tableConstraintDef",
                "spatialIndexOption",
                "fulltextIndexOption",
                "constraintEnforcement",
                "references");

        // Supporting expressions requires smarter logic for filling in
        // identifiers and type information in order to generate valid expressions
        crawler.addRulesToSkip("expr", "queryExpression", "queryExpressionOrParens");

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
        // TODO: SRIDs are now supported in Dolt; plugging this in requires updates to reification
        //       to select a valid SRID value and to make sure it is only used for geometry types.
        crawler.addRulesToSkip("SRID_SYMBOL");

        crawler.setCrawlStrategy(new CrawlStrategies.CoverageAwareCrawl(crawler));
        crawler.setEarlyTerminators(new MySQLCreateTableEarlyTerminator(crawler, 200));
        crawler.setStatementPrefix("CREATE ");

        StatementValidators.MySQLStatementValidator mySQLStatementValidator = StatementValidators.MySQLStatementValidator.NewMySQLStatementValidatorForSQLLogicTest();
        if (mySQLStatementValidator.canConnect()) {
            crawler.setStatementValidators(mySQLStatementValidator);
            crawler.setInvalidStatementWriter(new StatementWriters.FileStatementWriter("invalid-statements.txt"));
        } else {
            System.err.println("Unable to connect to MySQL server to validate generated statements.");
        }

        crawler.setStatementWriters(
                new StatementWriters.StdOutStatementWriter(),
                new StatementWriters.SQLLogicProtoStatementWriter("sqllogic-test-create-table.proto"));

        crawler.startCrawl(ruleMap.get("createTable"));
        crawler.printCoverageStats();
    }
}
