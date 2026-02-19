package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.EditorDocumentDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.EditorDeclarationResultDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.EditorDeclarationTargetDto;

public interface EditorFacade {
    EditorDocumentDto current();

    void openClass(String className, Integer jarId);

    void openMethod(String className, String methodName, String methodDesc, Integer jarId);

    EditorDeclarationResultDto resolveDeclaration(int caretOffset);

    EditorDeclarationResultDto resolveUsages(int caretOffset);

    EditorDeclarationResultDto resolveImplementations(int caretOffset);

    EditorDeclarationResultDto resolveTypeHierarchy(int caretOffset);

    boolean openDeclarationTarget(EditorDeclarationTargetDto target);

    void searchInCurrent(String keyword, boolean forward);

    void applyEditorText(String content, int caretOffset);

    boolean goPrev();

    boolean goNext();

    boolean addCurrentToFavorites();
}
