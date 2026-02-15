/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.runtime;

import me.n1ar4.jar.analyzer.cli.StartCmd;

/**
 * Runtime GUI launcher extension point.
 * Implementations are discovered by {@link java.util.ServiceLoader}.
 */
public interface GuiLauncher {
    /**
     * Launch GUI with already parsed start command.
     *
     * @param startCmd cli start command
     */
    void launch(StartCmd startCmd);
}
