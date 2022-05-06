import java.util.LinkedList;
import java.util.List;

public class Crawler {
    public TemplateBufferManager templateBufferManager = new TemplateBufferManager();
    public List<CrawlContext> contextsToProcess = new LinkedList<>();

    public void startCrawl(MySQLGrammarCrawler.Rule rule) {
        for (MySQLGrammarCrawler.Alternative alternative : rule.alternatives) {
            forkCrawl(null, alternative.elements.get(0));
        }

        start();
    }

    public CrawlContext forkCrawl(CrawlContext ctx, MySQLGrammarCrawler.Element elementToProcess) {
        if (ctx == null) ctx = new CrawlContext(null, new TemplateBuffer());

        TemplateBuffer newTemplateBuffer = templateBufferManager.forkTemplate(ctx.generatedTemplate.elements);
        CrawlContext newContext = new CrawlContext(elementToProcess, newTemplateBuffer);
        newContext.futureElements.addAll(ctx.futureElements);
        contextsToProcess.add(0, newContext);

        return newContext;
    }

    public CrawlContext continueCrawl(CrawlContext ctx, MySQLGrammarCrawler.Element elementToProcess) {
        CrawlContext newContext = new CrawlContext(elementToProcess, ctx.generatedTemplate);
        newContext.futureElements.addAll(ctx.futureElements);
        contextsToProcess.add(0, newContext);

        return newContext;
    }

    public void printAllTemplates(String statementPrefix) {
        templateBufferManager.printAllTemplates(statementPrefix);
    }

    public void printTemplateStats() {
        templateBufferManager.printTemplateStats();
    }

    private void start() {
        while (!contextsToProcess.isEmpty()) {
            CrawlContext ctx = contextsToProcess.remove(0);

            // If a crawl context was aborted while in process, don't process any other
            // elements contributing to that generated template.
            if (ctx.isAborted()) continue;

            MySQLGrammarCrawler.processElement(ctx);
        }
    }
}
