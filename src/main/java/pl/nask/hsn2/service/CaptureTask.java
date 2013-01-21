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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.ParameterException;
import pl.nask.hsn2.ResourceException;
import pl.nask.hsn2.StorageException;
import pl.nask.hsn2.TaskContext;
import pl.nask.hsn2.service.hpc.CaptureHpcConnector;
import pl.nask.hsn2.service.hpc.HpcTask;
import pl.nask.hsn2.service.hpc.TaskRegistry;
import pl.nask.hsn2.task.Task;
import pl.nask.hsn2.utils.IdGeneratorException;

public class CaptureTask implements Task {
    private final static Logger LOGGER = LoggerFactory.getLogger(CaptureTask.class);
    private final TaskContext ctx;
    private final ServiceParameters params;
    private final ServiceData inputData;
    private final CaptureHpcConnector connector;

    private final TaskRegistry taskRegistry;

    private HpcTask hpcTask;

    private final FilesHandler filesHandler;

    public CaptureTask(TaskContext ctx, ServiceParameters params, ServiceData inputData, CaptureHpcConnector connector, TaskRegistry taskRegistry, FilesHandler filesHandler) {
    	LOGGER.debug("CaptureTask created: [{}, {}]",params,inputData.getId());
        this.ctx = ctx;
        this.filesHandler = filesHandler;
        this.params = params;
        this.inputData = inputData;
        this.connector = connector;
        this.taskRegistry = taskRegistry;
    }

    @Override
    public boolean takesMuchTime() {
        return true;
    }

    @Override
    public void process() throws ParameterException, ResourceException, StorageException {
        try {
            ctx.addTimeAttribute("hpc_time_start", System.currentTimeMillis()/1000l);
            try {
				hpcTask = taskRegistry.registerTask(ctx, inputData);
			} catch (IdGeneratorException e) {
				LOGGER.error("Cannot register task");
				throw new ResourceException("Cannot register task",e);
			}
            LOGGER.debug("registered new hpcTask: {}",hpcTask.toString());
            LOGGER.info("Sending '{}' with ID:{} to Capture Server",inputData.getUrlForProcessing(),hpcTask.getId());
            try {
            	connector.addUrl(inputData.getUrlForProcessing(), hpcTask.getId());
            } catch (SocketException e) {
            	taskRegistry.unregister(hpcTask);
            	LOGGER.error("Cannot connect to CaptureServer, {}",e);
            	throw new ResourceException("Cannot connect to CaptureServer",e);
            }
            hpcTask.waitForFinish();
            ctx.addAttribute("hpc_active", hpcTask.isProcessed());
            ctx.addAttribute("hpc_profile_description", params.getHpcProfile());

            if (hpcTask.isProcessed()) {
            	ctx.addAttribute("hpc_classification", hpcTask.getClassification().toLowerCase());
            	filesHandler.setPrefix(""+hpcTask.getId());
            	if (params.isSaveLog() && hpcTask.isClassificationMalicious()) {
            		File [] files = filesHandler.getLogFiles();
            		if (files.length >= 1)
            			saveFile("hpc_log_file", files[0]);
            		if (files.length >= 2)
            			saveFile("hpc_log_file2", files[1]);
            	}

            	if (params.isSaveZip()) {
            		File [] files = filesHandler.getZipFiles();
            		if (files.length >= 1)
            			saveFile("hpc_zip_file", files[0]);
            		if (files.length >= 2)
            			saveFile("hpc_zip_file2", files[1]);
            	}

            	if (params.isSaveScreenshot()) {
            		saveFile("hpc_screenshot", filesHandler.getScreenshotFile());
            	}
            } else {
            	ctx.addAttribute("hpc_reason_failed", hpcTask.getFailureReason());
            }

            if (params.isSavePcap()) {
            	saveFile("hpc_pcap", filesHandler.getPcapFile());
            }
        } catch (IOException e) {
        	ctx.addAttribute("hpc_active", false);
        	ctx.addAttribute("hpc_crash_report", e.toString());
        	LOGGER.error(e.getMessage(), e);
        } catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
        	ctx.addTimeAttribute("hpc_time_stop", System.currentTimeMillis()/1000l);
        }
    }



    private void saveFile(String attributeName, File file) throws IOException, ResourceException, StorageException {
        if (file == null) {
            LOGGER.info("File not found for {}", attributeName);
        } else {
            InputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(file);
                long fileId = ctx.saveInDataStore(fileInputStream);
                ctx.addReference(attributeName, fileId);
            } catch (FileNotFoundException e) {
                throw new ResourceException("File not found", e);
            } finally {
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e) {
                        LOGGER.warn("Cannot close file", e);
                    }
                }
            }
        }
    }

    public HpcTask getHpcTask() {
        return hpcTask;
    }
}
