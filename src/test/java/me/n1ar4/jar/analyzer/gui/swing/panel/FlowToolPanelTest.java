/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.panel;

import me.n1ar4.jar.analyzer.gui.swing.SwingI18n;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

class FlowToolPanelTest {
    @AfterEach
    void resetLanguage() {
        SwingI18n.setLanguage("");
    }

    @Test
    void shouldApplyRuleValidationSummaryToFlowStatus() throws Exception {
        SwingI18n.setLanguage("en");
        Map<String, Object> summary = Map.of(
                "ok", false,
                "compiledRules", 11,
                "errorCount", 1,
                "warningCount", 0
        );
        AtomicReference<String> statusRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            FlowToolPanel panel = new FlowToolPanel();
            panel.applyRuleValidationState(summary, List.of());
            statusRef.set(panel.currentRuleValidationStatusText());
        });
        Assertions.assertEquals("Rule Validation: error | compiled 11 | errors 1 | warnings 0", statusRef.get());
    }
}
