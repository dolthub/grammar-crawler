import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TemplateBuffer {
    boolean aborted = false;
    List<MySQLGrammarCrawler.Element> elements = new ArrayList<>();

    public void abort() {
        aborted = true;
        elements.clear();
    }

    public void addElement(MySQLGrammarCrawler.Element e) {
        elements.add(e);
    }

    public void addElements(Collection<MySQLGrammarCrawler.Element> newElements) {
        if (newElements == null) return;
        elements.addAll(newElements);
    }

    public String toString() {
        if (aborted) return "ABORTED";

        List<String> strings = new ArrayList<>();
        for (MySQLGrammarCrawler.Element e : elements) {
            // TODO: Should we bother translating here in toString, or model a separate step after template
            //       creation that reifies the template into a real statement?
            String translated = MySQLGrammarCrawler.StatementReifier.translateSymbol(e.getName());
            strings.add(translated);
        }
        return String.join(" ", strings) + ";";
    }
}
