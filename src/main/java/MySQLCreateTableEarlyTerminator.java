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

/**
 * EarlyTerminator implementation for MySQL CREATE TABLE statements. Detects invalid column definitions (e.g. trying
 * to add a unique constraint for geometry data types) and instructs crawler to terminate any further processing on
 * that path.
 * <p>
 * Also provides a configurable limit for the number of times MySQL data types can be used. This allows callers to
 * better control the number of generated statements, while still getting coverage over all data types.
 */
public class MySQLCreateTableEarlyTerminator implements EarlyTerminator {
    private final Crawler crawler;
    private final int dataTypeUsageLimit;

    private static final String[] TERMINAL_PATTERNS = {
            // MySQL does not allow geometry types to be used as keys
            "GEOMETRY UNIQUE",
            "POINT UNIQUE",
            "LINESTRING UNIQUE",
            "POLYGON UNIQUE",
            "MULTIPOINT UNIQUE",
            "MULTILINESTRING UNIQUE",
            "MULTIPOLYGON UNIQUE",
            "GEOMETRYCOLLECTION UNIQUE",

            "GEOMETRY KEY",
            "POINT KEY",
            "LINESTRING KEY",
            "POLYGON KEY",
            "MULTIPOINT KEY",
            "MULTILINESTRING KEY",
            "MULTIPOLYGON KEY",
            "GEOMETRYCOLLECTION KEY",

            "GEOMETRY PRIMARY",
            "POINT PRIMARY",
            "LINESTRING PRIMARY",
            "POLYGON PRIMARY",
            "MULTIPOINT PRIMARY",
            "MULTILINESTRING PRIMARY",
            "MULTIPOLYGON PRIMARY",
            "GEOMETRYCOLLECTION PRIMARY",

            // MySQL doesn't support unique indexes directly on JSON fields
            "JSON UNIQUE", "JSON KEY", "JSON PRIMARY",

            // MySQL doesn't allow directly keys on BLOB/TEXT types
            "BLOB KEY", "BLOB UNIQUE", "BLOB PRIMARY KEY",
            "TEXT KEY", "TEXT UNIQUE", "TEXT PRIMARY KEY",
            "LONG KEY", "LONG UNIQUE", "LONG PRIMARY KEY",
    };

    private static final String[] DATATYPE_ELEMENTS = new String[]{
            "GEOMETRY_SYMBOL", "POINT_SYMBOL", "LINESTRING_SYMBOL", "POLYGON_SYMBOL",
            "MULTIPOINT_SYMBOL", "MULTILINESTRING_SYMBOL", "MULTIPOLYGON_SYMBOL", "GEOMETRYCOLLECTION_SYMBOL",
            "INT_SYMBOL", "TINYINT_SYMBOL", "SMALLINT_SYMBOL", "MEDIUMINT_SYMBOL", "BIGINT_SYMBOL",
            "REAL_SYMBOL", "DOUBLE_SYMBOL", "PRECISION_SYMBOL", "FLOAT_SYMBOL", "DECIMAL_SYMBOL", "NUMERIC_SYMBOL", "FIXED_SYMBOL",
            "BIT_SYMBOL", "BOOL_SYMBOL", "BOOLEAN_SYMBOL",
            "BINARY_SYMBOL", "VARBINARY_SYMBOL",
            "CHAR_SYMBOL", "VARYING_SYMBOL", "VARCHAR_SYMBOL",
            "NATIONAL_SYMBOL", "NVARCHAR_SYMBOL", "NCHAR_SYMBOL",
            "YEAR_SYMBOL", "DATE_SYMBOL", "TIME_SYMBOL", "TIMESTAMP_SYMBOL", "DATETIME_SYMBOL",
            "TINYBLOB_SYMBOL", "BLOB_SYMBOL", "MEDIUMBLOB_SYMBOL", "LONGBLOB_SYMBOL", "LONG_SYMBOL",
            "TINYTEXT_SYMBOL", "TEXT_SYMBOL", "MEDIUMTEXT_SYMBOL", "LONGTEXT_SYMBOL",
            "ENUM_SYMBOL", "SET_SYMBOL", "JSON_SYMBOL", "SERIAL_SYMBOL"};

    public MySQLCreateTableEarlyTerminator(Crawler crawler, int dataTypeUsageLimit) {
        this.crawler = crawler;
        this.dataTypeUsageLimit = dataTypeUsageLimit;
    }

    @Override
    public boolean shouldTerminate(TemplateBuffer buffer) {
        // Check for known invalid patterns that MySQL doesn't support
        String statement = buffer.toString();
        for (String terminalPattern : TERMINAL_PATTERNS) {
            if (statement.contains(terminalPattern)) {
                return true;
            }
        }

        // Check for over use of certain elements
        for (String limitedElement : DATATYPE_ELEMENTS) {
            if (buffer.elements.contains(new Rules.LiteralElement(limitedElement))) {
                Integer usage = crawler.getElementUsage().get(limitedElement);
                if (usage != null && usage > this.dataTypeUsageLimit) {
                    return true;
                }
            }
        }

        return false;
    }
}
