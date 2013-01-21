/*
 * Copyright (c) NASK, NCSC
 * 
 * This file is part of HoneySpider Network 2.0.
 * 
 * This is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.nask.hsn2.service.hpc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;


/**
 * Holds data of the asynchronous task sent to the HPC Server.
 * A client would create an instance of HpcTask by registering it in the TaskRegistry
 * and invoking waitForFinish() which would block clients execution until the task is finished.
 */
public class HpcTask {
    private final long id;

    private String lastStatus;
    private boolean processed;

    private boolean unregistered;

    private final TaskRegistry taskRegistry;

    private String failureReason;

    private String classification;

    private List<String> allStatuses = new ArrayList<String>();

    private CountDownLatch countDownLatch;

    HpcTask(long dataId, TaskRegistry taskRegistry) {
        this.taskRegistry = taskRegistry;
        this.id = dataId;
        this.countDownLatch = new CountDownLatch(1);
    }

    public boolean isClassificationMalicious() {
        return "MALICIOUS".equalsIgnoreCase(classification);
    }

    public String getClassification() {
        return classification;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void waitForFinish() throws InterruptedException {
        countDownLatch.await();
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public boolean isUnregistered() {
        return unregistered;
    }

    public long getId() {
        return id;
    }

    public boolean isProcessed() {
        return processed;
    }

    void update(String flag, String status) {
        /*
        SENDING
        VISITING
        VISITED
        BENIGN
        NETWORK_ERROR
        QUEUED - when URL is not duplicated and has been queued
        DPLCT - when URL is rejected  because it's duplicated
        */

        if ("F".equalsIgnoreCase(flag)) {
            taskCompleted(status);
        } else {
            updateStatus(status);
        }
    }

    private void taskCompleted(String status) {
        try {
            if ("BENIGN".equalsIgnoreCase(status) || "MALICIOUS".equalsIgnoreCase(status)) {
                classification = status;
                processed = true;
            } else {
                failureReason = status;
                processed = false;
            }
            updateStatus(status);

        } finally {
            unregisterTask();
            finished();
        }
    }

    private void finished() {
        countDownLatch.countDown();
    }

    private void unregisterTask() {
        unregistered = true;
        taskRegistry.unregister(this);
    }

    /**
     * Status BENIGN or MALICIOUS refers to URL classification. Other values indicates an error.
     * @param status
     */
    private void updateStatus(String status) {
        lastStatus = status;
        allStatuses .add(status);
    }
    public String toString() {
    	return "[HpcTask ID="+this.id+this.allStatuses.toString()+"]";
    }
}
