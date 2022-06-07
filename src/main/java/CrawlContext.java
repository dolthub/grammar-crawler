// Copyright 2022 Dolthub, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * CrawlContext contains the complete context for an in-progress crawl through a grammar, including: the next element
 * to process, the in-progress template buffer, the stack of future elements to process, and instructions on whether
 * optional elements should be included in this path.
 * <p>
 * As the Crawler crawls the grammar, it pushes new CrawlContext objects into a datastructure to process. When conditional
 * choices in the grammar are encountered, depending on the CrawlStrategy, the Crawler will push multiple CrawlContext
 * objects into this data structure so that multiple paths can be traversed.
 */
class CrawlContext {
    public Stack<FutureElementContext> futureElements = new Stack<>();
    public TemplateBuffer generatedTemplate;
    public Rules.Element elementToProcess;

    // TODO: This could change to something like numberOfTimesToProcess = 0 | 1 | 2;
    //       That would enable processing *,+, and ? with the same codepath.
    public boolean includeOptional = false;

    public List<String> parentPath = new ArrayList<>();


    public CrawlContext(Rules.Element element, TemplateBuffer generatedTemplate) {
        this.elementToProcess = element;
        this.generatedTemplate = generatedTemplate;
    }

    /**
     * Aborts processing for the current crawl path. When a crawl path is aborted, any partial template being built
     * from the current path will be removed and not included in the final crawl results.
     */
    public void abort() {
        generatedTemplate.abort();
    }

    /**
     * Returns true if the current crawl path has been aborted and should not be processed any further.
     *
     * @return true if the current crawl path has been aborted and should not be processed any further.
     */
    public boolean isAborted() {
        return generatedTemplate.aborted;
    }

    public static class FutureElementContext {
        public Rules.Element element;
        public List<String> parentPath = new ArrayList<>();

        public FutureElementContext(Rules.Element element) {
            this.element = element;
        }
    }
}
