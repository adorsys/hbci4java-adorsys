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
 * Kapselt die Liste der Nachrichten, die innerhalb eines Dialogs an die Bank gesendet werden sollen.
 */
public class HBCIMessageQueue {
    private List<HBCIMessage> messages = new ArrayList<>();

    /**
     * ct.
     * Erzeugt die Queue und befuellt sie gleich mit der ersten Nachricht.
     */
    public HBCIMessageQueue() {
        this.append(new HBCIMessage());
    }

    /**
     * Liefert die Kopie der Nachrichten-Liste.
     * Aenderungen an der Liste wirken sich nicht auf die Queue aus. Die Nachrichten darin koennen jedoch geaendert
     * werden.
     *
     * @return die Kopie der Nachrichten-Liste.
     */
    public List<HBCIMessage> getMessages() {
        return new ArrayList<>(this.messages);
    }

    /**
     * Liefert die naechste auszufuehrende Nachricht mit Tasks aus der Queue.
     *
     * @return die naechste auszufuehrende Nachricht mit Tasks aus der Queue oder NULL, wenn keine weitere mehr mit
     * Tasks existiert.
     */
    public HBCIMessage poll() {
        while (!this.messages.isEmpty()) {
            HBCIMessage m = this.messages.remove(0);
            if (m.getTaskCount() > 0)
                return m;
        }
        return null;
    }

    /**
     * Liefert die Anzahl aller Tasks in allen Naxchrichten.
     *
     * @return die Anzahl aller Tasks in allen Naxchrichten.
     */
    public int getTaskCount() {
        int count = 0;
        for (HBCIMessage msg : this.messages) {
            count += msg.getTaskCount();
        }

        return count;
    }

    /**
     * Sucht in der ganzen Queue nach einem Task mit dem angegebenen HBCI-Code.
     *
     * @param hbciCode der HBCI-Code.
     * @return der Task oder NULL, wenn er nicht gefunden wurde.
     */
    public AbstractHBCIJob findTask(String hbciCode) {
        if (hbciCode == null)
            return null;

        for (HBCIMessage msg : this.messages) {
            AbstractHBCIJob task = msg.findTask(hbciCode);
            if (task != null)
                return task;
        }

        return null;
    }

    /**
     * Liefert die letzte Nachricht.
     *
     * @return die letzte Nachricht.
     */
    public HBCIMessage getLast() {
        return this.messages.get(this.messages.size() - 1);
    }

    /**
     * Fuegt eine neue Nachricht am Ende der Queue hinzu.
     *
     * @param message die neue Nachricht.
     */
    public void append(HBCIMessage message) {
        this.messages.add(message);
    }

    /**
     * Fuegt vor der angegebenen Nachricht noch eine neue hinzu und liefert sie zurueck.
     *
     * @param message die Nachricht, vor der noch eine neue eingfuegt werden soll.
     * @return die neue Nachricht.
     */
    public HBCIMessage insertBefore(HBCIMessage message) {
        if (message == null)
            throw new IllegalArgumentException("no message given");

        final int pos = this.messages.indexOf(message);

        if (pos == -1)
            throw new IllegalArgumentException("message unknown to queue");

        HBCIMessage m = new HBCIMessage();
        this.messages.add(pos, m);
        return m;
    }

}
