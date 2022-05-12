public class StatementReifier {
    private static int tableNameCounter = 1;
    private static long textId = 0;

    /**
     * Turns the specified template into a valid statement.
     *
     * @param prefix            The prefix to add to the generated statement. Useful when crawling a rule
     *                          that starts at a subset of a complete statement.
     * @param generatedTemplate The template to use to create a valid statement.
     * @return A valid statement created from specified statement template.
     */
    public static String reifyStatement(String prefix, TemplateBuffer generatedTemplate) {
        // Plug in valid identifiers and send reified statements out to the statement writer
        String s = prefix + generatedTemplate;

        s = s.replaceFirst("foo", StatementReifier.randomNewTableName());
        int i = 1;
        while (s.contains("foo")) {
            s = s.replaceFirst("foo", "c" + i);
            i++;
        }

        return s;
    }

    static String postProcessStatement(String statement) {
        // Clean up spacing around parens and commas
        statement = statement.replaceAll("\\( ", "(");
        statement = statement.replaceAll(" \\)", ")");
        statement = statement.replaceAll(" , ", ", ");

        // The MySQL grammar allows two text strings after COMMENT, but those statements won't actually parse
        statement = statement.replaceAll("COMMENT ['\"`]?text(\\d+)['\"`]? '", "COMMENT '");
        statement = statement.replaceAll("COMMENT ['\"`]?text(\\d+)['\"`]? \"", "COMMENT \"");
        statement = statement.replaceAll("COMMENT ['\"`]?text(\\d+)['\"`]? `", "COMMENT `");

        return statement;
    }

    static String translateSymbol(String symbolName) {
        // TODO: Load these from the lexer instead of manually defining them...
        switch (symbolName) {
            case "DOT_SYMBOL":
                return ".";
            case "COMMA_SYMBOL":
                return ",";
            case "AT_SIGN_SYMBOL":
                return "@";
            case "EQUAL_OPERATOR":
                return "=";
            case "SEMICOLON_SYMBOL":
                return ";";
            case "OPEN_PAR_SYMBOL":
                return "(";
            case "CLOSE_PAR_SYMBOL":
                return ")";
            case "MULT_OPERATOR":
                return "*";
            case "OPEN_CURLY_SYMBOL":
                return "{";
            case "CLOSE_CURLY_SYMBOL":
                return "}";
            case "NULL2_SYMBOL":
                return "NULL";
            case "MINUS_OPERATOR":
                return "-";
            case "PLUS_OPERATOR":
                return "+";
            case "NOT2_SYMBOL":
                return "NOT";
            case "AUTO_INCREMENT_SYMBOL":
                return "AUTO INCREMENT";
        }

        // TODO: All of these identifiers need more granular control for customizing in statements
        switch (symbolName) {
            case "IDENTIFIER":
                return "foo";
            case "BACK_TICK_QUOTED_ID":
                return "`foo`";
            case "SINGLE_QUOTED_TEXT":
                return "'text" + textId++ + "'";
            case "DOUBLE_QUOTED_TEXT":
                return "\"text" + textId++ + "\"";
            case "NCHAR_TEXT":
                return "text" + textId++;
            case "ULONGLONG_NUMBER":
                return "42";
            case "FLOAT_NUMBER":
                return "4.2";
            case "INT_NUMBER":
            case "LONG_NUMBER":
                return Integer.toString(randomInt(10, 20));
            case "DECIMAL_NUMBER":
                return "4";
            case "HEX_NUMBER":
                return "0x" + Integer.toHexString(randomInt(1, 20));
            case "BIN_NUMBER":
                return "0b" + Integer.toBinaryString(randomInt(1, 20));
        }

        if (symbolName.endsWith("_SYMBOL")) {
            return symbolName.substring(0, symbolName.indexOf("_SYMBOL"));
        }

        return symbolName;
    }

    private static String randomNewTableName() {
        return "t" + Integer.toHexString(tableNameCounter++);
    }
    
    private static int randomInt(int min, int max) {
        return (int) Math.floor(Math.random() * (max - min + 1) + min);
    }
}
