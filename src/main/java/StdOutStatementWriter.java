public class StdOutStatementWriter implements StatementWriter {
    @Override
    public void write(String statement) {
        System.out.println(statement);
    }

    @Override
    public void finished() {
        // TODO: End with template statistics
    }
}
