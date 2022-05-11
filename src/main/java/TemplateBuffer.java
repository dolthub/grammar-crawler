import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TemplateBuffer {
    boolean aborted = false;
    List<Rules.Element> elements = new ArrayList<>();

    public void abort() {
        aborted = true;
        elements.clear();
    }

    public void addElement(Rules.Element e) {
        elements.add(e);
    }

    public void addElements(Collection<Rules.Element> newElements) {
        if (newElements == null) return;
        elements.addAll(newElements);
    }

    public String toString() {
        if (aborted) return "ABORTED";

        boolean inSetOrEnum = false;

        List<String> strings = new ArrayList<>();
        for (Rules.Element e : elements) {
            // TODO: Should we bother translating here in toString, or model a separate step after template
            //       creation that reifies the template into a real statement?
            String translated = StatementReifier.translateSymbol(e.getName());
            if (inSetOrEnum) {
                switch (translated.trim()) {
                    case ")":
                        inSetOrEnum = false;
                        break;
                    case "(":
                    case ",":
                        break;
                    default:
                        translated = quoteString(translated);
                }
            }
            strings.add(translated);

            if (translated.trim().equalsIgnoreCase("ENUM") ||
                    translated.trim().equalsIgnoreCase("SET")) {
                inSetOrEnum = true;
            }
        }
        String s = String.join(" ", strings) + ";";
        s = StatementReifier.postProcessStatement(s);
        return s;
    }

    private String quoteString(String translated) {
        translated = translated.trim();
        for (String s : new String[]{"'", "\"", "`"}) {
            if (translated.startsWith(s) && translated.endsWith(s)) return translated;
        }
        return "'" + translated + "'";
    }
}
