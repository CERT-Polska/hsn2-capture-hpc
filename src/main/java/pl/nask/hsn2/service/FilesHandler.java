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

package pl.nask.hsn2.service;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.io.comparator.DefaultFileComparator;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.ResourceException;

public class FilesHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilesHandler.class);
    private static final Logger RACE_LOGGER = LoggerFactory.getLogger(FilesHandler.class.getName() + ".Race");

    private static final Comparator<File> FILE_COMPARATOR = new DefaultFileComparator();

    private static final int INITIAL_TIMEOUT = 100;

    private File changesDir;

    private int zipTries;

    private  String prefix;

    public FilesHandler(String changesDir, String prefix, int zipTries) {
        this.prefix = prefix;
        this.zipTries = zipTries;
        this.changesDir = new File(changesDir);
    }

    final File[] getLogFiles() throws ResourceException {
        return getTaskFilesWithSuffix("log");
    }

    final File[] getZipFiles() throws ResourceException {
        try {
            return getTaskFilesWithSuffix("zip", zipTries);
        } catch (InterruptedException e) {
           throw new ResourceException("Interrupted while waiting for the resource", e);
        }
    }

    private File[] getTaskFilesWithSuffix(String suffix, int numberOfTries) throws InterruptedException {
        long currentTimeoutMilis = INITIAL_TIMEOUT;
        for (int i = 1; i <= numberOfTries; i++) {
            File[] result = getTaskFilesWithSuffix(suffix);
            if (result.length != 0) {
                if (i > 1) {
                    RACE_LOGGER.warn("{} files retrieved after {} attempts", suffix, i);
                }
                return result;
            } else {
                // wait a while
                Thread.sleep(currentTimeoutMilis);
                currentTimeoutMilis *= 2;
            }
        }

        LOGGER.debug("No {} files retrieved after {} attempts", suffix, numberOfTries);
        return new File[0];
    }

    private File[] getTaskFilesWithSuffix(String suffix) {
        LOGGER.debug("All files in changes dir: {}", (Object) changesDir.listFiles());
        FilenameFilter filter = new WildcardFileFilter(prefix + "_*." + suffix);
        File [] files = changesDir.listFiles(filter);
        LOGGER.debug("Files matching wildcard for prefix={}, suffix={}: {}", new Object[] {prefix, suffix, files});
        // expect one or 2 files to be found
        Arrays.sort(files, FILE_COMPARATOR);
        return files;
    }

    final File getPcapFile() throws ResourceException {
        LOGGER.warn("Saving PCAP files is not implemented");
        return null;
    }

    final File getScreenshotFile() throws ResourceException {
        LOGGER.warn("Saving screenshot files in not implemented");
        return null;
    }

    @Override
	public final String toString() {
    	try {
			return "FilesHandler: "+changesDir.getCanonicalPath()+" "+prefix+"*";
		} catch (IOException e) {
			return "";
		}

    }
    public final void setPrefix(String prefix) {
    	this.prefix = prefix;
    }
}
