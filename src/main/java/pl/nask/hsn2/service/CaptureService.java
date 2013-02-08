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

import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonController;
import org.apache.commons.daemon.DaemonInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.GenericService;
import pl.nask.hsn2.service.hpc.CaptureHpcConnector;
import pl.nask.hsn2.service.hpc.CaptureHpcConnectorImpl;
import pl.nask.hsn2.service.hpc.HpcLogAnalyserImpl;
import pl.nask.hsn2.service.hpc.TaskRegistry;
import pl.nask.hsn2.task.TaskFactory;

public final class CaptureService implements Daemon{
    private static final Logger LOGGER = LoggerFactory.getLogger(CaptureService.class);

    private static Thread analyserThread;

	private static  HpcCommandLineParams cmd;
	
	// Keeps track of registered hpc tasks
	private static volatile TaskRegistry taskRegistry;

	
	private Thread serviceRunner ;

    public static void main(final String[] args) throws DaemonInitException, Exception {

    	CaptureService cs = new CaptureService();
    	cs.init(new DaemonContext() {
			
			@Override
			public DaemonController getController() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String[] getArguments() {
				return args;
			}
		});
    	cs.start();
    	while(!Thread.currentThread().isInterrupted())
    		Thread.sleep(5000);
    	cs.stop();
    	cs.destroy();

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

    @Override
    public void init(DaemonContext context) throws DaemonInitException, Exception {
    	cmd = parseArguments(context.getArguments());
    	taskRegistry = new TaskRegistry(cmd.getHpcFileIdGen());


    	// checks the log file for the results of analysis and triggers taskRegistry
    	HpcLogAnalyserImpl logAnalyser = null;
    	try {
    		logAnalyser = new HpcLogAnalyserImpl(cmd.getLogFileName(), taskRegistry);
    	} catch (IllegalArgumentException e) {
    		LOGGER.error("Cannot open HPC server log file: {}\t - Shutting down",e.getMessage());
    		throw new DaemonInitException("Cannot open HPC server log file");
    	}
    	analyserThread = new Thread(logAnalyser, "hpcLogAnalyser");
    	
    	serviceRunner = new Thread(new Runnable() {
			
			@Override
			public void run() {
				Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
					
					@Override
					public void uncaughtException(Thread t, Throwable e) {
						LOGGER.error("Service crash.exiting");
						System.exit(1);
					}
				});
				try {
					// A connector for the Capture HPC server
					CaptureHpcConnector hpcConnector = new CaptureHpcConnectorImpl(cmd.getHpcHost(), cmd.getHpcPort());
			    	LOGGER.info(hpcConnector.getConnectorInfo());

			    	// a task factory for a generic service
			    	TaskFactory jobFactory = new CaptureTaskFactory(hpcConnector, taskRegistry, cmd.getChangesDirName());

					GenericService service = new GenericService(jobFactory, cmd.getMaxThreads(), cmd.getRbtCommonExchangeName());
					cmd.applyArguments(service);
					
					service.run();
				} catch (RuntimeException e) {
					analyserThread.interrupt();
					joinSafely(analyserThread);
					LOGGER.error("Caught RuntimeException, shutting down service", e);
					System.exit(1);
				} catch (InterruptedException e) {
					analyserThread.interrupt();
					joinSafely(analyserThread);
					System.exit(1);
				}
				
			}
		},"Capture-Service");

    }

	@Override
	public void start() throws Exception {
		analyserThread.start();
		serviceRunner.start();
		
	}

	@Override
	public void stop() throws Exception {
		analyserThread.interrupt();
		serviceRunner.interrupt();
		joinSafely(analyserThread);
		joinSafely(serviceRunner);
		
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
}
