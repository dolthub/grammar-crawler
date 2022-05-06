import java.text.NumberFormat;
import java.util.Locale;

public class StdOutStatementWriter implements StatementWriter {
    private int statementCount;

    @Override
    public void write(String statement) {
        statementCount++;
        System.out.println(statement);
    }

    @Override
    public void finished() {
        System.out.println();
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        System.out.println("Total Statements: " + numberFormat.format(statementCount));
    }
}
