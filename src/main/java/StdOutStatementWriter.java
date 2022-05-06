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
        System.out.println("Total Statements: " + statementCount);
    }
}
