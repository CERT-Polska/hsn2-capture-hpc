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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HpcLogAnalyserImpl implements HpcLogAnalyser, Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(HpcLogAnalyserImpl.class);

    private static final int SLEEP_TIMEOUT = 500;

    private LineNumberReader reader;

    private final TaskRegistry taskRegistry;

    public HpcLogAnalyserImpl(String file, TaskRegistry taskRegistry) {
        this.taskRegistry = taskRegistry;
        try {
            reader = new LineNumberReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("FileNotFoundException "+e.getMessage(), e);
        }
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                String line = reader.readLine();
                if (line != null) {
                    processLine(line,reader.getLineNumber());
                } else {
                    getSomeSleep();
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error processing log file", e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                LOGGER.error("Error closing reader", e);
            }
        }
    }

    private void getSomeSleep() {
        try {
            Thread.sleep(SLEEP_TIMEOUT);
        } catch (InterruptedException e) {
            LOGGER.info("Interrupted while taking a nap", e);
            Thread.currentThread().interrupt();
        }
    }

    private void processLine(String line, int lineNo) {
    	final int field_count = 7 ;
        String[] lnArr = line.split(" ",field_count);
        if (lnArr.length < field_count) {
            LOGGER.warn("Unparseable line[{}]: {},",lineNo, line);
        } else {
            LOGGER.info("Parsing [{}]: {}",lineNo, line);
            String flag = lnArr[3];
            String status = lnArr[4];
            String id = lnArr[5];
            String url = lnArr[6];
            taskRegistry.log(id, url, flag, status);
        }
    }
}
