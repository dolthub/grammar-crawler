import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TemplateBufferTest {
    @Test
    public void testToString() {
        TemplateBuffer tb = new TemplateBuffer();
        tb.addElement(new Rules.LiteralElement("ENUM_SYMBOL"));
        tb.addElement(new Rules.LiteralElement("OPEN_PAR_SYMBOL"));
        tb.addElement(new Rules.LiteralElement("0x111"));
        tb.addElement(new Rules.LiteralElement("COMMA_SYMBOL"));
        tb.addElement(new Rules.LiteralElement("'0x112'"));
        tb.addElement(new Rules.LiteralElement("CLOSE_PAR_SYMBOL"));
        assertEquals("ENUM ('0x111', '0x112');", tb.toString());
    }
}
