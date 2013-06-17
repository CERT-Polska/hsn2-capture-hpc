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

import org.apache.commons.daemon.DaemonInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.CommandLineParams;
import pl.nask.hsn2.ServiceMain;
import pl.nask.hsn2.service.hpc.CaptureHpcConnector;
import pl.nask.hsn2.service.hpc.CaptureHpcConnectorImpl;
import pl.nask.hsn2.service.hpc.HpcLogAnalyserImpl;
import pl.nask.hsn2.service.hpc.TaskRegistry;
import pl.nask.hsn2.task.TaskFactory;

public final class CaptureService extends ServiceMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaptureService.class);
    private static Thread analyserThread;

	// Keeps track of registered hpc tasks
	private static volatile TaskRegistry taskRegistry;

    public static void main(final String[] args) throws DaemonInitException, Exception {
    	CaptureService cs = new CaptureService();
    	cs.init(new DefaultDaemonContext(args));
    	cs.start();
    }

	@Override
	public void stop() {
		analyserThread.interrupt();
		super.stop();
	}

	@Override
	protected void prepareService() {
		HpcCommandLineParams cmd = (HpcCommandLineParams)getCommandLineParams();
    	taskRegistry = new TaskRegistry(cmd.getHpcFileIdGen());

    	// checks the log file for the results of analysis and triggers taskRegistry
    	HpcLogAnalyserImpl logAnalyser = null;
    	try {
    		logAnalyser = new HpcLogAnalyserImpl(cmd.getLogFileName(), taskRegistry);
    	} catch (IllegalArgumentException e) {
    		LOGGER.error("Cannot open HPC server log file: {}\t - Shutting down",e.getMessage());
    		throw new RuntimeException("Cannot open HPC server log file", e);
    	}
    	analyserThread = new Thread(logAnalyser, "hpcLogAnalyser");
    	analyserThread.start();
	}

	@Override
	protected TaskFactory createTaskFactory() {
		// A connector for the Capture HPC server
		HpcCommandLineParams cmd = (HpcCommandLineParams)getCommandLineParams();
		CaptureHpcConnector hpcConnector = new CaptureHpcConnectorImpl(cmd.getHpcHost(), cmd.getHpcPort());
    	LOGGER.info(hpcConnector.getConnectorInfo());
		return new CaptureTaskFactory(hpcConnector, taskRegistry, cmd.getChangesDirName());
	}
	
	@Override
	protected CommandLineParams newCommandLineParams() {
		return new HpcCommandLineParams();
	}
}
