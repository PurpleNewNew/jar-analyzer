package org.benf.cfr.reader.state;

import org.benf.cfr.reader.apiunreleased.ClassFileSource2;
import org.benf.cfr.reader.apiunreleased.JarContent;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.util.AnalysisType;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class DCCommonStateForkTest {
    @Test
    void shouldIsolateWorkerCachesButKeepSharedConfiguration() {
        DCCommonState original = new DCCommonState(new OptionsImpl(Map.of()), new NoopClassFileSource());
        DCCommonState worker = original.forkForBatchWorker();

        assertNotSame(original, worker);
        assertNotSame(original.getClassCache(), worker.getClassCache());
        assertNotSame(original.getOverloadMethodSetCache(), worker.getOverloadMethodSetCache());
        assertSame(original.getOptions(), worker.getOptions());
        assertSame(original.getObfuscationMapping(), worker.getObfuscationMapping());
        assertSame(original.getVersionCollisions(), worker.getVersionCollisions());
    }

    private static final class NoopClassFileSource implements ClassFileSource2 {
        @Override
        public void informAnalysisRelativePathDetail(String usePath, String classFilePath) {
        }

        @Override
        public Collection<String> addJar(String jarPath) {
            return Collections.emptyList();
        }

        @Override
        public String getPossiblyRenamedPath(String path) {
            return path;
        }

        @Override
        public Pair<byte[], String> getClassFileContent(String path) throws IOException {
            throw new IOException("not used in fork test");
        }

        @Override
        public JarContent addJarContent(String jarPath, AnalysisType analysisType) {
            return new JarContent() {
                @Override
                public Collection<String> getClassFiles() {
                    return Collections.emptyList();
                }

                @Override
                public Map<String, String> getManifestEntries() {
                    return Collections.emptyMap();
                }

                @Override
                public AnalysisType getAnalysisType() {
                    return analysisType;
                }
            };
        }
    }
}
