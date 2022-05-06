import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

class TemplateBufferManager {
    List<TemplateBuffer> generatedTemplates = new ArrayList<>();

    public TemplateBuffer forkTemplate() {
        return forkTemplate(null);
    }

    public TemplateBuffer forkTemplate(Collection<Rules.Element> existingContent) {
        TemplateBuffer generatedTemplate = new TemplateBuffer();
        generatedTemplate.addElements(existingContent);
        generatedTemplates.add(generatedTemplate);
        return generatedTemplate;
    }

    public void printTemplateStats() {
        int totalTemplates = generatedTemplates.size();
        int numberOfAbortedTemplates = getNumberOfAbortedTemplates();
        int numberOfCompleteTemplates = totalTemplates - numberOfAbortedTemplates;

        System.out.println("Total Templates Attempted:    " + NumberFormat.getNumberInstance(Locale.US).format(totalTemplates));
        System.out.println("Number of Aborted Templates:  " + NumberFormat.getNumberInstance(Locale.US).format(numberOfAbortedTemplates));
        System.out.println("Number of Complete Templates: " + NumberFormat.getNumberInstance(Locale.US).format(numberOfCompleteTemplates));
    }

    public void printAllTemplates(String prefix) {
        for (TemplateBuffer generatedTemplate : generatedTemplates) {
            if (generatedTemplate.aborted) continue;
            System.out.println(prefix + generatedTemplate);
        }
    }

    public int getNumberOfAbortedTemplates() {
        int i = 0;
        for (TemplateBuffer generatedTemplate : generatedTemplates) {
            if (generatedTemplate.aborted) i++;
        }
        return i;
    }

    public void printAllAbortedTemplates() {
        for (TemplateBuffer generatedTemplate : generatedTemplates) {
            if (generatedTemplate.aborted) {
                System.out.println(" - " + generatedTemplate);
            }
        }
    }
}
