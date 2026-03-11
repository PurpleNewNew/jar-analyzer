package me.n1ar4.jar.analyzer.gui.swing.jcef;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JcefAssetLoaderTest {
    @Test
    void shouldRecognizeWorkbenchOriginOnly() {
        Assertions.assertTrue(JcefAssetLoader.isWorkbenchUrl("http://cypher.workbench.local/index.html"));
        Assertions.assertFalse(JcefAssetLoader.isWorkbenchUrl("https://cypher.workbench.local/index.html"));
        Assertions.assertFalse(JcefAssetLoader.isWorkbenchUrl("http://example.com/index.html"));
    }

    @Test
    void shouldResolveAndCacheWorkbenchAssets() {
        String entryUrl = JcefAssetLoader.resolveEntryUrl();
        JcefAssetLoader.FrontendAsset entry = JcefAssetLoader.resolveAsset(entryUrl);
        Assertions.assertNotNull(entry);
        Assertions.assertEquals("index.html", entry.path());

        JcefAssetLoader.FrontendAsset root = JcefAssetLoader.resolveAsset("http://cypher.workbench.local/");
        Assertions.assertSame(entry, root);

        JcefAssetLoader.FrontendAsset cached = JcefAssetLoader.resolveAsset(entryUrl);
        Assertions.assertSame(entry, cached);
    }

    @Test
    void shouldRejectPathTraversal() {
        Assertions.assertNull(JcefAssetLoader.resolveAsset("http://cypher.workbench.local/../secret.txt"));
    }

    @Test
    void shouldReturnExplicitNotFoundAssetForMissingStaticResource() {
        JcefAssetLoader.FrontendAsset missing = JcefAssetLoader.resolveAsset(
                "http://cypher.workbench.local/assets/not-found.js"
        );
        Assertions.assertNotNull(missing);
        Assertions.assertEquals(404, missing.statusCode());
        Assertions.assertEquals("Not Found", missing.statusText());
        Assertions.assertEquals("assets/not-found.js", missing.path());
    }
}
