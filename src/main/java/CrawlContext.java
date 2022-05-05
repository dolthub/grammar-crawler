import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

class CrawlContext {
    public Stack<FutureElementContext> futureElements = new Stack();
    public TemplateBuffer generatedTemplate;
    public MySQLGrammarCrawler.Element elementToProcess;

    // TODO: This could change to something like numberOfTimesToProcess = 0 | 1 | 2;
    public boolean includeOptional = false;

    public List<String> parentPath = new ArrayList<>();


    public CrawlContext(MySQLGrammarCrawler.Element element, TemplateBuffer generatedTemplate) {
        this.elementToProcess = element;
        this.generatedTemplate = generatedTemplate;
    }

    public static class FutureElementContext {
        public MySQLGrammarCrawler.Element element;
        public List<String> parentPath = new ArrayList<>();

        public FutureElementContext(MySQLGrammarCrawler.Element element) {
            this.element = element;
        }
    }
}
