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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.GenericService;
import pl.nask.hsn2.service.hpc.CaptureHpcConnector;
import pl.nask.hsn2.service.hpc.CaptureHpcConnectorImpl;
import pl.nask.hsn2.service.hpc.HpcLogAnalyserImpl;
import pl.nask.hsn2.service.hpc.TaskRegistry;
import pl.nask.hsn2.task.TaskFactory;

public class CaptureService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaptureService.class);

    private static Thread analyserThread;

    private CaptureService() {}

    public static void main(String[] args) {
        HpcCommandLineParams cmd = parseArguments(args);
        
        // Keeps track of registered hpc tasks
        TaskRegistry taskRegistry = new TaskRegistry(cmd.getHpcFileIdGen());
        

        // checks the log file for the results of analysis and triggers taskRegistry
        
        HpcLogAnalyserImpl logAnalyser = null;
        try {
        	logAnalyser = new HpcLogAnalyserImpl(cmd.getLogFileName(), taskRegistry);
        } catch (IllegalArgumentException e) {
        	LOGGER.error("Cannot open HPC server log file: {}\t - Shutting down",e.getMessage());
        	System.exit(1);
        }

        analyserThread = new Thread(logAnalyser, "hpcLogAnalyser");
        analyserThread.start();
        
        // A connector for the Capture HPC server
		CaptureHpcConnector hpcConnector = new CaptureHpcConnectorImpl(cmd.getHpcHost(), cmd.getHpcPort());

		// a task factory for a generic service
		TaskFactory jobFactory = new CaptureTaskFactory(hpcConnector, taskRegistry, cmd.getChangesDirName());

		try {
			// and a generic service
			GenericService service = new GenericService(jobFactory, cmd.getMaxThreads(), cmd.getRbtCommonExchangeName(), cmd.getRbtNotifyExchangeName());
			cmd.applyArguments(service);
			LOGGER.info(hpcConnector.getConnectorInfo());

			service.run();
		} catch (RuntimeException e) {
			analyserThread.interrupt();
			joinSafely(analyserThread);
			LOGGER.error("Caught RuntimeException, shutting down service", e);
			System.exit(1);
		} catch (InterruptedException e) {
			analyserThread.interrupt();
			joinSafely(analyserThread);
		}
    }

    private static void joinSafely(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted while joining thread {}", thread);
        }
    }

    private static HpcCommandLineParams parseArguments(String[] args) {
        HpcCommandLineParams params = new HpcCommandLineParams();
        params.setDefaultServiceNameAndQueueName("capture");
        params.parseParams(args);

        return params;
    }
}
