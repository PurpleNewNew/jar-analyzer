package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.EditorDocumentDto;

public interface EditorFacade {
    EditorDocumentDto current();

    void openClass(String className, Integer jarId);

    void openMethod(String className, String methodName, String methodDesc, Integer jarId);

    void searchInCurrent(String keyword, boolean forward);

    void applyEditorText(String content, int caretOffset);

    boolean goPrev();

    boolean goNext();

    boolean addCurrentToFavorites();
}
