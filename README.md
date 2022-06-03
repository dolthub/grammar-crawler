# mysql-grammar-crawler

MySQL Grammar Crawler is a configurable SQL fuzzer that works by crawling the
[MySQL 8 ANTLR grammar maintained as part of Oracle's MySQL Workbench project](https://github.com/mysql/mysql-workbench/blob/8.0/library/parsers/grammars/MySQLParser.g4)
to generate a wide variety of statements with valid MySQL syntax. These

# Get Crawling

TODO: An example of how to get started quickly!

# Crawler Components

## Crawler Configuration

The crawler can be configured to skip parts of the grammar, to ... ??? ...

## Crawl Strategies

The crawler supports three crawl strategies:

* Full Crawl – every path through the grammar graph will be explored, with some caveats (e.g. cycles are detected and
  skipped). This mode works well for small grammars or small subsets of a grammar, but can quickly produce a LOT of
  generated statement templates.

## Reification

After the crawler finishes a complete path through the grammar graph and has generated the template for a valid
statement, it reifies the template into a valid SQL statement. This involves plugging in literal values for placeholders
in the template and doing any minor cleanup to the statement to help increase the chances of it executing cleanly.

## Statement Output

After a statement has been reified, it is sent to the `StatementWriter` configured for the Crawler. There are currently
two implementations of `StatementWriter` available:

* `StdOutStatementWriter` – This implementation simply outputs the statement directly to StdOut.
* `SQLLogicProtoStatementWriter` – This implementation writes out a proto format suitable
  for [SqlLogicTest](https://www.sqlite.org/sqllogictest/doc/trunk/about.wiki) to process.

# Contributions

We're happy to accept contributions if you want to use the grammar crawler in your work and need additional behavior.
Feel free to cut issues or Pull Requests or [come join the Dolt DB Discord server](https://discord.com/invite/RFwfYpu)
and chat with us about ideas.

