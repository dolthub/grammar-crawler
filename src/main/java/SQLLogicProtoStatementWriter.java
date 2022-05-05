import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SQLLogicProtoStatementWriter implements StatementWriter {
    private FileWriter fileWriter;

    public SQLLogicProtoStatementWriter(String filename) throws IOException {
        File file = new File(filename);
        fileWriter = new FileWriter(file);
    }

    @Override
    public void write(String statement) {
        try {
            fileWriter.write("statement ok\n");
            fileWriter.write(statement);
            fileWriter.write("\n\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void finished() {
        try {
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
