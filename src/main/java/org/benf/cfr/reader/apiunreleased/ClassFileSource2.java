package org.benf.cfr.reader.apiunreleased;

import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.util.AnalysisType;

// TODO : Experimental API - before moving, snip ClassFileSource link.
public interface ClassFileSource2 extends ClassFileSource {
    /**
     * CFR would like to know about all classes contained within the jar at {@code jarPath}
     *
     * @param jarPath path to a jar.
     * @return @{link JarContent} for this jar. Returning {@code null} is only appropriate for delegate/fallback
     * chaining; final callers should treat it as a failed jar lookup.
     */
    JarContent addJarContent(String jarPath, AnalysisType analysisType);
}
