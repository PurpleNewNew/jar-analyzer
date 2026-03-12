package me.n1ar4.jar.analyzer.storage.neo4j;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveProjectContextMutationTest {
    private static final String PROJECT_KEY = "mutation-refcount-project";

    @AfterEach
    void cleanup() {
        while (ActiveProjectContext.isProjectMutationInProgress(PROJECT_KEY)) {
            ActiveProjectContext.endProjectMutation(PROJECT_KEY);
        }
    }

    @Test
    void sameProjectMutationShouldStayVisibleUntilLastMutationEnds() {
        ActiveProjectContext.beginProjectMutation(PROJECT_KEY);
        ActiveProjectContext.beginProjectMutation(PROJECT_KEY);

        assertTrue(ActiveProjectContext.isProjectMutationInProgress());
        assertTrue(ActiveProjectContext.isProjectMutationInProgress(PROJECT_KEY));

        ActiveProjectContext.endProjectMutation(PROJECT_KEY);

        assertTrue(ActiveProjectContext.isProjectMutationInProgress());
        assertTrue(ActiveProjectContext.isProjectMutationInProgress(PROJECT_KEY));

        ActiveProjectContext.endProjectMutation(PROJECT_KEY);

        assertFalse(ActiveProjectContext.isProjectMutationInProgress(PROJECT_KEY));
        assertFalse(ActiveProjectContext.isProjectMutationInProgress());
    }
}
