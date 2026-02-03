// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.main.extern;

public interface IClassLookupProvider {
    LookupResult lookupClass(String className);

    final class LookupResult {
        private final byte[] bytes;
        private final String externalPath;
        private final String internalPath;

        public LookupResult(byte[] bytes, String externalPath, String internalPath) {
            this.bytes = bytes;
            this.externalPath = externalPath;
            this.internalPath = internalPath;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public String getExternalPath() {
            return externalPath;
        }

        public String getInternalPath() {
            return internalPath;
        }
    }
}
