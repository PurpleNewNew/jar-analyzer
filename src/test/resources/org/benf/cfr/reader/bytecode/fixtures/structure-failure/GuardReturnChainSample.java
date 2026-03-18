package org.benf.cfr.reader.bytecode.fixtures.structure.failure;

import java.util.Set;

public class GuardReturnChainSample {
    public static boolean matchesAllowedHash(String dirName, Set<String> allowedHashes) {
        if (dirName == null || dirName.isEmpty() || allowedHashes == null || allowedHashes.isEmpty()) {
            return false;
        }
        int lastDash = dirName.lastIndexOf('-');
        if (lastDash < 0 || lastDash >= dirName.length() - 1) {
            return false;
        }
        String suffix = dirName.substring(lastDash + 1);
        if (suffix.isEmpty()) {
            return false;
        }
        return allowedHashes.contains(suffix);
    }
}
