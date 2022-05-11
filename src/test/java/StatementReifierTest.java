import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StatementReifierTest {
    @Test
    public void testPostProcessComments() {
        String s = "COMMENT 'text5' 'text6'";
        s = StatementReifier.postProcessStatement(s);
        assertEquals("COMMENT 'text6'", s);
    }
}
