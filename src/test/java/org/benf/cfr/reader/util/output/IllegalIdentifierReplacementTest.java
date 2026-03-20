package org.benf.cfr.reader.util.output;

import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.state.TypeUsageInformationImpl;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IllegalIdentifierReplacementTest {
    @Test
    void shouldRestartRenameSequenceForEachTypeUsageInformation() {
        List<String> first = renderIllegalNames("0first", "bad\bfirst");
        List<String> second = renderIllegalNames("0second", "bad\bsecond");

        assertEquals(List.of("cfr_renamed_0", "CfrRenamed1"), first);
        assertEquals(first, second);
    }

    @Test
    void shouldKeepConcurrentBatchRenameResultsStable() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<List<String>>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 16; ++i) {
                final int taskId = i;
                futures.add(executor.submit(() -> {
                    start.await();
                    return renderIllegalNames("0task" + taskId, "bad\b" + taskId, "1task" + taskId, "worse\b" + taskId);
                }));
            }
            start.countDown();

            Set<List<String>> results = new HashSet<>();
            for (Future<List<String>> future : futures) {
                results.add(future.get());
            }

            assertEquals(Set.of(List.of("cfr_renamed_0", "CfrRenamed1", "cfr_renamed_1", "CfrRenamed2")), results);
        } finally {
            executor.shutdownNow();
        }
    }

    private static List<String> renderIllegalNames(String... values) {
        Options options = new OptionsImpl(Map.of(OptionsImpl.RENAME_ILLEGAL_IDENTS.getName(), "true"));
        JavaRefTypeInstance analysisType = JavaRefTypeInstance.createTypeConstant("sample.AnalysisType");
        TypeUsageInformationImpl typeUsageInformation = new TypeUsageInformationImpl(
                options,
                analysisType,
                Collections.singleton(analysisType),
                Collections.emptySet());
        IllegalIdentifierDump illegalIdentifierDump = typeUsageInformation.getIid();

        List<String> rendered = new ArrayList<>(values.length);
        for (int i = 0; i < values.length; ++i) {
            if ((i & 1) == 0) {
                rendered.add(illegalIdentifierDump.getLegalIdentifierFor(values[i]));
            } else {
                rendered.add(illegalIdentifierDump.getLegalShortName(values[i]));
            }
        }
        return rendered;
    }
}
