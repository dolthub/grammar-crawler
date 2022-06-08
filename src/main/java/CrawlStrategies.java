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

import java.util.HashSet;
import java.util.Set;

/**
 * CrawlStrategies contains CrawlStrategy, the main interface for how the Crawler makes decisions about which paths
 * to traverse in the grammar, as well as common implementations of CrawlStrategy, such as FullCrawl, RandomCrawl, and
 * CoverageAwareCrawl.
 */
public class CrawlStrategies {
    public static final CrawlStrategy FULL_CRAWL = new FullCrawl();
    public static final CrawlStrategy RANDOM_CRAWL = new RandomCrawl();

    public static interface CrawlStrategy {
        public boolean shouldCrawl(Rules.Element element);
    }

    public static class FullCrawl implements CrawlStrategy {
        @Override
        public boolean shouldCrawl(Rules.Element element) {
            return true;
        }
    }

    public static class RandomCrawl implements CrawlStrategy {
        @Override
        public boolean shouldCrawl(Rules.Element element) {
            return Math.random() > 0.5;
        }
    }

    @SuppressWarnings("ClassCanBeRecord")
    public static class CoverageAwareCrawl implements CrawlStrategy {

        private final Crawler crawler;

        public CoverageAwareCrawl(Crawler crawler) {
            this.crawler = crawler;
        }

        @Override
        public boolean shouldCrawl(Rules.Element element) {
            // If the current element is parent of literal elements that we haven't
            // used in completed expressions yet, then crawl to increase coverage.
            Set<Rules.LiteralElement> literalElements = findLiteralElements(element);
            for (Rules.LiteralElement literalElement : literalElements) {
                if (crawler.getElementUsage().get(literalElement) == 0) return true;
            }

            // Otherwise, if a rule contains only literal elements that we've already
            // used, then use a low probability random choice for selection.
            return Math.random() < 0.33;
        }

        private Set<Rules.LiteralElement> findLiteralElements(Rules.Element element) {
            // TODO: Consider moving this crawling logic to Crawler
            Set<Rules.LiteralElement> results = new HashSet<>();
            Set<Rules.Element> visitedElements = new HashSet<>();
            findLiteralElements(element, results, visitedElements);
            return results;
        }

        private void findLiteralElements(Rules.Element element, Set<Rules.LiteralElement> results, Set<Rules.Element> visitedElements) {
            if (visitedElements.contains(element)) return;
            visitedElements.add(element);

            if (crawler.getRulesToSkip().contains(element.getName())) return;

            if (element instanceof Rules.LiteralElement) {
                results.add((Rules.LiteralElement) element);
            } else if (element instanceof Rules.Choice) {
                Rules.Choice choice = (Rules.Choice) element;
                for (Rules.Element e : choice.choices) {
                    findLiteralElements(e, results, visitedElements);
                }
            } else if (element instanceof Rules.ElementGroup) {
                Rules.ElementGroup group = (Rules.ElementGroup) element;
                for (Rules.Element e : group.elements) {
                    findLiteralElements(e, results, visitedElements);
                }
            } else if (element instanceof Rules.RuleRefElement) {
                Rules.RuleRefElement ruleRef = (Rules.RuleRefElement) element;
                Rules.Rule rule = crawler.getRuleMap().get(ruleRef.getName());
                for (Rules.Alternative alternative : rule.alternatives) {
                    for (Rules.Element e : alternative.elements) {
                        findLiteralElements(e, results, visitedElements);
                    }
                }
            } else {
                throw new RuntimeException("Unexpected type of element: " + element.getClass());
            }
        }
    }
}
