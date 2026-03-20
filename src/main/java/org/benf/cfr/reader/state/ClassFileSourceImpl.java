package org.benf.cfr.reader.state;

import org.benf.cfr.reader.apiunreleased.ClassFileSource2;
import org.benf.cfr.reader.apiunreleased.JarContent;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.util.AnalysisType;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.MiscConstants;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.benf.cfr.reader.bytecode.analysis.types.ClassNameUtils.getPackageAndClassNames;

public class ClassFileSourceImpl implements ClassFileSource2 {
    private Map<String, JarSourceEntry> classPathIndex;
    private final Options options;
    private final ClassRenamer classRenamer;
    private ClassFileRelocator analysisRelocator = ClassFileRelocator.NopRelocator.Instance;
    /*
     * Initialisation info
     */
    private static final boolean JrtPresent = CheckJrt();
    private static final Map<String, String> packMap = JrtPresent ? getPackageToModuleMap() : new HashMap<String, String>();

    private static boolean CheckJrt() {
        try {
            return Object.class.getResource("Object.class").getProtocol().equals("jrt");
        } catch (Exception e) {
            return false;
        }
    }

    public ClassFileSourceImpl(Options options) {
        this.options = options;
        this.classRenamer = ClassRenamer.create(options);
    }

    private byte[] getBytesFromFile(InputStream is, long length) throws IOException {
        // Create the byte array to hold the data
        byte[] bytes = new byte[(int) length];

        // Read in the bytes
        int offset = 0;
        int numRead;
        while (offset < bytes.length
                && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file");
        }

        // Close the input stream and return bytes
        is.close();
        return bytes;
    }

    @Override
    public String getPossiblyRenamedPath(String path) {
        if (classRenamer == null) return path;
        String res = classRenamer.getRenamedClass(path + ".class");
        if (res == null) return path;
        return res.substring(0, res.length()-6);
    }

    @Override
    public Pair<byte [], String> getClassFileContent(final String inputPath) throws IOException {
        // If path is an alias due to case insensitivity, restore to the correct name here, before
        // accessing zipfile.
        String path = classRenamer == null ? inputPath : classRenamer.getOriginalClass(inputPath);
        JarSourceEntry jarEntry = getClassPathIndex().get(path);

        ZipFile zipFile = null;

        try {
            InputStream is;
            long length;

            String usePath = analysisRelocator.correctPath(path);
            boolean forceJar = jarEntry != null && jarEntry.isExplicitJar();
            File file = forceJar ? null : new File(usePath);
            byte[] content;
            if (file != null && file.exists()) {
                is = new FileInputStream(file);
                length = file.length();
                content = getBytesFromFile(is, length);
            } else if (jarEntry != null) {
                zipFile = new ZipFile(new File(jarEntry.path), ZipFile.OPEN_READ);
                if (jarEntry.analysisType == AnalysisType.WAR) {
                    path = MiscConstants.WAR_PREFIX + path;
                }
                ZipEntry zipEntry = zipFile.getEntry(path);
                length = zipEntry.getSize();
                is = zipFile.getInputStream(zipEntry);
                content = getBytesFromFile(is, length);
            } else {
                // Fallback - can we get the bytes using a java9 extractor?
                content = getInternalContent(inputPath);
            }

            return Pair.make(content, inputPath);
        } finally {
            if (zipFile != null) zipFile.close();
        }
    }

    /*
     * There are costs associated in the Class.forName method of finding the URL for a class -
     * notably the running of the static initialiser.
     *
     * We can avoid that by knowing what packages are in what modules, and skipping for those.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String> getPackageToModuleMap() {
        Map<String, String> mapRes = MapFactory.newMap();
        try {
            Class moduleLayerClass = Class.forName("java.lang.ModuleLayer");
            Method bootMethod = moduleLayerClass.getMethod("boot");
            Object boot = bootMethod.invoke(null);
            Method modulesMeth = boot.getClass().getMethod("modules");
            Object modules = modulesMeth.invoke(boot);
            Class moduleClass = Class.forName("java.lang.Module");
            Method getPackagesMethod = moduleClass.getMethod("getPackages");
            Method getNameMethod = moduleClass.getMethod("getName");
            for (Object module : (Set)modules) {
                Set<String> packageNames = (Set<String>)getPackagesMethod.invoke(module);
                String moduleName = (String)getNameMethod.invoke(module);
                for (String packageName : packageNames) {
                    if (mapRes.containsKey(packageName)) {
                        mapRes.put(packageName, null);
                    } else {
                        mapRes.put(packageName, moduleName);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore and return anything we have.
        }
        return mapRes;
    }

    private byte[] getContentByFromReflectedClass(final String inputPath) {
        try {
            String classPath = inputPath.replace("/", ".").substring(0, inputPath.length() - 6);

            Pair<String, String> packageAndClassNames = getPackageAndClassNames(classPath);

            String packageName = packageAndClassNames.getFirst();
            String moduleName = packMap.get(packageName);

            if (moduleName != null) {
                byte[] res = getUrlContent(new URL("jrt:/" + moduleName + "/" + inputPath));
                if (res != null) return res;
            }

            int idx = inputPath.lastIndexOf("/");
            String name = idx < 0 ? inputPath : inputPath.substring(idx + 1);

            // Going down this branch will trigger a class load, which will cause
            // static initialisers to be run.
            // This is expensive, but normally tolerable, except that there is a javafx
            // static initialiser which crashes the VM if called unexpectedly!
            URL resource = Class.forName(classPath).getResource(name);

            if (resource == null) {
                return null;
            }
            return getUrlContent(resource);
        } catch (Throwable t) {
            // Class.forName can throw a linkage error in some circumstances, so it's necessary to trap throwable.
            return null;
        }
    }

    private byte[] getUrlContent(URL url) {
        String protocol = url.getProtocol();
        // Strictly speaking, we could use this mechanism for pre-9 classes, but it's.... so wrong!
        if (!protocol.equals("jrt")) return null;

        InputStream is;
        int len;
        try {
            URLConnection uc;
            uc = url.openConnection();
            uc.connect();
            is = uc.getInputStream();
            len = uc.getContentLength();
        } catch (IOException ioe) {
            return null;
        }

        try {
            if (len >= 0) {
                byte[] b = new byte[len];
                int i = len;
                while (i > 0) {
                    if (i < (i -= is.read(b, len - i, i))) i = -1;
                }
                if (i == 0) return b;
            }
        } catch (IOException e) {
            //
        }
        return null;
    }

    private byte[] getInternalContent(final String inputPath) throws IOException {
        if (JrtPresent) {
            byte[] res = getContentByFromReflectedClass(inputPath);
            if (res != null) return res;
        }
        throw new IOException("No such file " + inputPath);
    }

    @Deprecated
    public Collection<String> addJar(String jarPath) {
        return addJarContent(jarPath, AnalysisType.JAR).getClassFiles();
    }

    public JarContent addJarContent(String jarPath, AnalysisType analysisType) {
        // Make sure classpath is scraped first, so we'll overwrite it.
        getClassPathIndex();

        File file = new File(jarPath);
        if (!file.exists()) {
            throw new ConfusedCFRException("No such jar file " + jarPath);
        }
        jarPath = file.getAbsolutePath();
        JarContent jarContent = readJarContent(file, analysisType, false);
        if (jarContent == null){
            throw new ConfusedCFRException("Failed to load jar " + jarPath);
        }

        String jarClassPath = jarContent.getManifestEntries().get(MiscConstants.MANIFEST_CLASS_PATH);
        if (jarClassPath != null) {
            addToRelativeClassPath(file, jarClassPath);
        }

        registerJarContent(jarContent, new JarSourceEntry(analysisType, jarPath, true), null);
        return jarContent;
    }

    private static class JarSourceEntry {
        private final AnalysisType analysisType;
        private final String path;
        private final boolean explicitJar;

        JarSourceEntry(AnalysisType analysisType, String path, boolean explicitJar) {
            this.analysisType = analysisType;
            this.path = path;
            this.explicitJar = explicitJar;
        }

        private boolean isExplicitJar() {
            return explicitJar;
        }
    }

    private Map<String, JarSourceEntry> getClassPathIndex() {
        if (classPathIndex == null) {
            boolean dump = options.getOption(OptionsImpl.DUMP_CLASS_PATH);

            classPathIndex = MapFactory.newMap();
            String classPath = System.getProperty("java.class.path");
            String sunBootClassPath = System.getProperty("sun.boot.class.path");
            if (sunBootClassPath != null) {
                classPath += File.pathSeparatorChar + sunBootClassPath;
            }

            if (dump) {
                System.out.println("/* ClassPath Diagnostic - searching :" + classPath);
            }
            String extraClassPath = options.getOption(OptionsImpl.EXTRA_CLASS_PATH);
            if (null != extraClassPath) {
                classPath = classPath + File.pathSeparatorChar + extraClassPath;
            }
            List<String> pendingClassFiles = ListFactory.newList();
            String[] classPaths = classPath.split("" + File.pathSeparatorChar);
            scanPaths(dump, pendingClassFiles, Arrays.asList(classPaths));
            notifyClassFiles(pendingClassFiles);
            if (dump) {
                System.out.println(" */");
            }
        }
        return classPathIndex;
    }

    private void notifyClassFiles(Collection<String> classFiles) {
        if (classRenamer != null && classFiles != null && !classFiles.isEmpty()) {
            classRenamer.notifyClassFiles(classFiles);
        }
    }

    private void registerClassFiles(Collection<String> classFiles,
                                    JarSourceEntry sourceEntry,
                                    Collection<String> pendingClassFiles) {
        if (classFiles == null || classFiles.isEmpty()) {
            return;
        }
        if (pendingClassFiles == null) {
            notifyClassFiles(classFiles);
        } else {
            pendingClassFiles.addAll(classFiles);
        }
        for (String classPath : classFiles) {
            if (classPath.toLowerCase().endsWith(".class")) {
                classPathIndex.put(classPath, sourceEntry);
            }
        }
    }

    private void scanPaths(boolean dump, Collection<String> pendingClassFiles, Collection<String> paths) {
        for (String path : paths) {
            processToClassPath(dump, path, pendingClassFiles);
        }
    }

    private void processToClassPath(boolean dump, String path, Collection<String> pendingClassFiles) {
        if (dump) {
            System.out.println(" " + path);
        }
        File pathEntry = new File(path);
        if (!pathEntry.exists()) {
            if (dump) {
                System.out.println(" (Can't access)");
            }
            return;
        }
        List<File> scanTargets;
        if (pathEntry.isDirectory()) {
            if (dump) {
                System.out.println(" (Directory)");
            }
            File[] files = pathEntry.listFiles();
            scanTargets = files == null ? Collections.<File>emptyList() : Arrays.asList(files);
        } else {
            scanTargets = Collections.singletonList(pathEntry);
        }
        for (File scanTarget : scanTargets) {
            registerJarContent(readJarContent(scanTarget, AnalysisType.JAR, dump),
                    new JarSourceEntry(AnalysisType.JAR, scanTarget.getAbsolutePath(), false),
                    pendingClassFiles);
        }
    }

    private void addToRelativeClassPath(File file, String jarClassPath) {
        String[] classPaths = jarClassPath.split(" ");
        boolean dump = options.getOption(OptionsImpl.DUMP_CLASS_PATH);
        String relative = null;
        try {
            File parent = file.getAbsoluteFile().getParentFile();
            if (parent == null) {
                return;
            }
            relative = parent.getCanonicalPath();
        } catch (IOException e) {
            return;
        }
        List<String> resolvedClassPaths = ListFactory.newList();
        for (String path : classPaths) {
            resolvedClassPaths.add(relative + File.separatorChar + path);
        }
        List<String> pendingClassFiles = ListFactory.newList();
        scanPaths(dump, pendingClassFiles, resolvedClassPaths);
        notifyClassFiles(pendingClassFiles);
    }

    private void registerJarContent(JarContent content,
                                    JarSourceEntry sourceEntry,
                                    Collection<String> pendingClassFiles) {
        if (content == null) {
            return;
        }
        Collection<String> classFiles = content.getClassFiles();
        registerClassFiles(classFiles, sourceEntry, pendingClassFiles);
    }

    private JarContent readJarContent(File file, AnalysisType analysisType, boolean dump) {
        List<String> classFiles = ListFactory.newList();
        Map<String, String> manifestEntries = MapFactory.newMap();
        try (ZipFile zipFile = new ZipFile(file, ZipFile.OPEN_READ)) {
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry entry = enumeration.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (MiscConstants.MANIFEST_PATH.equals(name)) {
                    try (InputStream is = zipFile.getInputStream(entry);
                         BufferedReader bis = new BufferedReader(new InputStreamReader(is))) {
                        String line;
                        while (null != (line = bis.readLine())) {
                            int idx = line.indexOf(':');
                            if (idx <= 0) continue;
                            manifestEntries.put(line.substring(0, idx), line.substring(idx + 1).trim());
                        }
                    } catch (IOException ignore) {
                    }
                    continue;
                }
                if (!name.endsWith(".class")) {
                    if (dump) {
                        System.out.println("  [ignoring] " + name);
                    }
                    continue;
                }
                if (analysisType == AnalysisType.WAR) {
                    if (!name.startsWith(MiscConstants.WAR_PREFIX)) {
                        continue;
                    }
                    name = name.substring(MiscConstants.WAR_PREFIX.length());
                }
                if (dump) {
                    System.out.println("  " + name);
                }
                classFiles.add(name);
            }
        } catch (IOException e) {
            return null;
        }
        return new JarContentImpl(classFiles, manifestEntries, analysisType);
    }

    @Override
    public void informAnalysisRelativePathDetail(String usePath, String specPath) {
        analysisRelocator = new ClassFileRelocator.Configurator().configureWith(usePath, specPath);
    }
}
