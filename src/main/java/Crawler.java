import java.util.LinkedList;
import java.util.List;

public class Crawler {
    // TODO: We could probably get rid of TemplateManager now that we have a signal when a templatebuffer
    //       is complete and we could feed the generated templates to the StatementWriter dynamically as
    //       they finish (or perhaps feed them through first to a reifier to plug in valid identifiers).
    public TemplateBufferManager templateBufferManager = new TemplateBufferManager();
    public List<CrawlContext> contextsToProcess = new LinkedList<>();

    public void startCrawl(Rules.Rule rule) {
        for (Rules.Alternative alternative : rule.alternatives) {
            forkCrawl(null, alternative.elements.get(0));
        }

        start();
    }

    public CrawlContext forkCrawl(CrawlContext ctx, Rules.Element elementToProcess) {
        if (ctx == null) ctx = new CrawlContext(null, new TemplateBuffer());

        TemplateBuffer newTemplateBuffer = templateBufferManager.forkTemplate(ctx.generatedTemplate.elements);
        CrawlContext newContext = new CrawlContext(elementToProcess, newTemplateBuffer);
        newContext.futureElements.addAll(ctx.futureElements);
        contextsToProcess.add(0, newContext);

        return newContext;
    }

    public CrawlContext continueCrawl(CrawlContext ctx, Rules.Element elementToProcess) {
        CrawlContext newContext = new CrawlContext(elementToProcess, ctx.generatedTemplate);
        newContext.futureElements.addAll(ctx.futureElements);
        contextsToProcess.add(0, newContext);

        return newContext;
    }

    public void writeStatements(StatementWriter writer, String statementPrefix) {
        // Plug in valid identifiers and send reified statements out to the statement writer
        for (TemplateBuffer generatedTemplate : templateBufferManager.generatedTemplates) {
            if (generatedTemplate.aborted) continue;

            String s = statementPrefix + generatedTemplate;

            // TODO: This should be extracted into a separate, configurable interface
            s = s.replaceFirst("foo", StatementReifier.randomNewTableName());
            int i = 1;
            while (s.contains("foo")) {
                s = s.replaceFirst("foo", "c" + i);
                i++;
            }

            writer.write(s);
        }
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
