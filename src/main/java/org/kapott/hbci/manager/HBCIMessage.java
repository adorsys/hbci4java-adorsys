/*
 * Copyright 2018-2019 adorsys GmbH & Co KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kapott.hbci.manager;

import org.kapott.hbci.GV.AbstractHBCIJob;

import java.util.ArrayList;
import java.util.List;

/**
 * Kapselt die fachlichen Jobs in einr HBCI-Nachricht an die Bank.
 */
public class HBCIMessage {
    private List<AbstractHBCIJob> tasks = new ArrayList<>();

    /**
     * Liefert die Kopie der Task-Liste.
     * Aenderungen an der Liste wirken sich nicht auf die Nachricht aus. Die Tasks darin koennen jedoch geaendert
     * werden.
     *
     * @return die Kopie der Task-Liste.
     */
    public List<AbstractHBCIJob> getTasks() {
        return new ArrayList<>(this.tasks);
    }

    /**
     * Liefert die Anzahl aller Tasks in der Naxchricht.
     *
     * @return die Anzahl aller Tasks in der Naxchricht.
     */
    public int getTaskCount() {
        return this.tasks.size();
    }

    /**
     * Sucht in der Nachricht nach einem Task mit dem angegebenen HBCI-Code.
     *
     * @param hbciCode der HBCI-Code.
     * @return der Task oder NULL, wenn er nicht gefunden wurde.
     */
    public AbstractHBCIJob findTask(String hbciCode) {
        if (hbciCode == null)
            return null;

        for (AbstractHBCIJob task : this.tasks) {
            if (hbciCode.equals(task.getHBCICode()))
                return task;
        }
        return null;
    }

    /**
     * Fuegt einen neuen Job zur Nachricht hinzu.
     *
     * @param task der neue Job.
     */
    public void append(AbstractHBCIJob task) {
        this.tasks.add(task);
    }
}


