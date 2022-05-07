public class StatementReifier {
    private static int tableNameCounter = 1;

    public static String randomNewTableName() {
        return "t" + Integer.toHexString(tableNameCounter++);
    }


    public static String translateSymbol(String symbolName) {
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
        }

        // TODO: All of these identifiers need more granular control for customizing in statements
        switch (symbolName) {
            case "IDENTIFIER":
                return "foo";
            case "BACK_TICK_QUOTED_ID":
                return "`foo`";
            case "SINGLE_QUOTED_TEXT":
                return "'text'";
            case "DOUBLE_QUOTED_TEXT":
                return "\"text\"";
            case "INT_NUMBER":
                return "42";
            case "DECIMAL_NUMBER":
                return "4.2";
            case "HEX_NUMBER":
                return "0x2A";
            case "BIN_NUMBER":
                return "0b1101";
        }

        if (symbolName.endsWith("_SYMBOL")) {
            return symbolName.substring(0, symbolName.indexOf("_SYMBOL"));
        }

        return symbolName;
    }
}
