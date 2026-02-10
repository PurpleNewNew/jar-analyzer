/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.state;

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.LinkedList;

/**
 * Bounded state list used by GUI navigation.
 */
public class StateLinkedList extends LinkedList<State> {
    private static final Logger logger = LogManager.getLogger();
    private static final int MAX_CAPACITY = 0xFF;

    @Override
    public void add(int index, State element) {
        if (this.size() >= MAX_CAPACITY) {
            logger.info("states too large (0xff) delete first element");
            this.removeFirst();
            super.add(index - 1, element);
        } else {
            super.add(index, element);
        }
    }
}

