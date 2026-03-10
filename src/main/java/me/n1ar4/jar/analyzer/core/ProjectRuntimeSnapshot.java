/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record ProjectRuntimeSnapshot(
        int schemaVersion,
        long buildSeq,
        ProjectModelData projectModel,
        List<JarData> jars,
        List<ClassFileData> classFiles,
        List<ClassReferenceData> classReferences,
        List<MethodReferenceData> methodReferences,
        Map<String, List<String>> methodStrings,
        Map<String, List<String>> methodAnnoStrings,
        List<ResourceData> resources,
        List<CallSiteData> callSites,
        List<LocalVarData> localVars,
        List<SpringControllerData> springControllers,
        Set<String> springInterceptors,
        Set<String> servlets,
        Set<String> filters,
        Set<String> listeners
) {
    public static final int CURRENT_SCHEMA_VERSION = 2;

    public ProjectRuntimeSnapshot {
        schemaVersion = schemaVersion <= 0 ? CURRENT_SCHEMA_VERSION : schemaVersion;
        buildSeq = Math.max(0L, buildSeq);
        jars = jars == null ? List.of() : List.copyOf(jars);
        classFiles = classFiles == null ? List.of() : List.copyOf(classFiles);
        classReferences = classReferences == null ? List.of() : List.copyOf(classReferences);
        methodReferences = methodReferences == null ? List.of() : List.copyOf(methodReferences);
        methodStrings = methodStrings == null ? Map.of() : Map.copyOf(methodStrings);
        methodAnnoStrings = methodAnnoStrings == null ? Map.of() : Map.copyOf(methodAnnoStrings);
        resources = resources == null ? List.of() : List.copyOf(resources);
        callSites = callSites == null ? List.of() : List.copyOf(callSites);
        localVars = localVars == null ? List.of() : List.copyOf(localVars);
        springControllers = springControllers == null ? List.of() : List.copyOf(springControllers);
        springInterceptors = springInterceptors == null ? Set.of() : Set.copyOf(springInterceptors);
        servlets = servlets == null ? Set.of() : Set.copyOf(servlets);
        filters = filters == null ? Set.of() : Set.copyOf(filters);
        listeners = listeners == null ? Set.of() : Set.copyOf(listeners);
    }

    public record ProjectModelData(
            String buildMode,
            String primaryInputPath,
            String runtimePath,
            List<ProjectRootData> roots,
            List<String> analyzedArchives,
            boolean resolveInnerJars
    ) {
        public ProjectModelData {
            roots = roots == null ? List.of() : List.copyOf(roots);
            analyzedArchives = analyzedArchives == null ? List.of() : List.copyOf(analyzedArchives);
        }
    }

    public record ProjectRootData(
            String kind,
            String origin,
            String path,
            String presentableName,
            boolean archive,
            boolean test,
            int priority
    ) {
    }

    public record JarData(
            int jid,
            String jarName,
            String jarAbsPath
    ) {
    }

    public record ClassFileData(
            int cfId,
            String className,
            String pathStr,
            String jarName,
            Integer jarId
    ) {
    }

    public record AnnoData(
            String annoName,
            Boolean visible,
            Integer parameter
    ) {
    }

    public record ClassHandleData(
            String name,
            Integer jarId
    ) {
    }

    public record ClassMemberData(
            String name,
            int modifiers,
            String value,
            String desc,
            String signature,
            ClassHandleData type
    ) {
    }

    public record ClassReferenceData(
            Integer version,
            Integer access,
            String name,
            String superClass,
            List<String> interfaces,
            boolean isInterface,
            List<ClassMemberData> members,
            List<AnnoData> annotations,
            String jarName,
            Integer jarId
    ) {
        public ClassReferenceData {
            interfaces = interfaces == null ? List.of() : List.copyOf(interfaces);
            members = members == null ? List.of() : List.copyOf(members);
            annotations = annotations == null ? List.of() : List.copyOf(annotations);
        }
    }

    public record MethodReferenceData(
            ClassHandleData classReference,
            List<AnnoData> annotations,
            String name,
            String desc,
            int access,
            boolean isStatic,
            int lineNumber,
            String jarName,
            Integer jarId,
            int semanticFlags
    ) {
        public MethodReferenceData {
            annotations = annotations == null ? List.of() : List.copyOf(annotations);
        }

        public MethodReferenceData(ClassHandleData classReference,
                                   List<AnnoData> annotations,
                                   String name,
                                   String desc,
                                   int access,
                                   boolean isStatic,
                                   int lineNumber,
                                   String jarName,
                                   Integer jarId) {
            this(classReference, annotations, name, desc, access, isStatic, lineNumber, jarName, jarId, 0);
        }
    }

    public record ResourceData(
            int rid,
            String resourcePath,
            String pathStr,
            String jarName,
            Integer jarId,
            long fileSize,
            int isText
    ) {
    }

    public record CallSiteData(
            String callerClassName,
            String callerMethodName,
            String callerMethodDesc,
            String calleeOwner,
            String calleeMethodName,
            String calleeMethodDesc,
            Integer opCode,
            Integer lineNumber,
            Integer callIndex,
            String receiverType,
            Integer jarId,
            String callSiteKey
    ) {
    }

    public record LocalVarData(
            String className,
            String methodName,
            String methodDesc,
            Integer varIndex,
            String varName,
            String varDesc,
            String varSignature,
            Integer startLine,
            Integer endLine,
            Integer jarId
    ) {
    }

    public record SpringControllerData(
            boolean rest,
            String basePath,
            ClassHandleData classHandle,
            List<SpringMappingData> mappings
    ) {
        public SpringControllerData {
            mappings = mappings == null ? List.of() : List.copyOf(mappings);
        }
    }

    public record SpringMappingData(
            boolean rest,
            ClassHandleData methodOwner,
            String methodName,
            String methodDesc,
            String path,
            String restfulType,
            String pathRestful,
            List<SpringParamData> params
    ) {
        public SpringMappingData {
            params = params == null ? List.of() : List.copyOf(params);
        }
    }

    public record SpringParamData(
            int paramIndex,
            String paramName,
            String paramType,
            String reqName
    ) {
    }
}
