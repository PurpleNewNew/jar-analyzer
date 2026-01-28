package org.benf.cfr.reader.util;

/**
 * Provides information about the CFR build.
 *
 * <p>The information in this class is usually generated at build time. This
 * copy is materialized for the jar-analyzer in-repo build.
 */
public class CfrVersionInfo {
    private CfrVersionInfo() {
    }

    /** CFR version */
    public static final String VERSION = "master";

    /** Are we a snapshot? */
    public static final boolean SNAPSHOT = CfrVersionInfo.VERSION.contains("SNAPSHOT");
    /**
     * Abbreviated Git commit hash of the commit representing this state
     * of the project.
     */
    public static final String GIT_COMMIT_ABBREVIATED = "7c48b8c";
    /**
     * Whether the working tree contained not yet committed changes when
     * the project was built.
     */
    public static final boolean GIT_IS_DIRTY = false;

    /** String consisting of CFR version and Git commit hash */
    public static final String VERSION_INFO =
            VERSION + " (FabricMC " + GIT_COMMIT_ABBREVIATED + (GIT_IS_DIRTY ? "-dirty" : "") + ")";
}
