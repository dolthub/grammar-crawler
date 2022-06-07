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

/**
 * Interface for outputting completed and ready to execute statements generated by Grammar Crawler.
 */
public interface StatementWriter {
    /**
     * Writes the specified completed, ready to execute statement out to this statement writer.
     *
     * @param statement The statement to write out.
     */
    void write(String statement);

    /**
     * Hook for any cleanup or final work needed as part of a StatementWriter (e.g. closing streams, writing summary
     * stats, etc).
     */
    void finished();
}
