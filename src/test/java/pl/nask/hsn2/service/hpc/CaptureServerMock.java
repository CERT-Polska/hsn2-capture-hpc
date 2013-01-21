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


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaptureServerMock  implements Runnable{
	private static int MIN_DELAY = 30;
	private static int DELAY_RANGE = 2000;
	private static final int NUMBER_OF_EXECUTORS = 20;
	private ServerSocket socket;
    private Random r = new Random(1);

    private PrintWriter writer;
    private ExecutorService ex = Executors.newFixedThreadPool(NUMBER_OF_EXECUTORS);
	private final String WORKING_DIR;
	
	
    private final static Logger LOGGER = LoggerFactory.getLogger(CaptureServerMock.class);
	private static int DEFAULT_PORT = 32337;
	private boolean GEN_FILES = false;
   
    static class Flows { 
    	
    	
    	private static final String[] SIMPLE_FLOW = {"T QUEUED","T SENDING","T VISITING","T VISITED","F BENIGN"};
    	private static final String[] MALICIOUS__LONG_FLOW = {"T QUEUED","T SENDING","T VISITING","T MALICIOUS","T VM_STALLED-0","T SENDING","T VISITING","F MALICIOUS"};
    	private static final String[] MALICIOUS_FLOW = {"T QUEUED","T SENDING","T VISITING","F MALICIOUS"};
    	private static final String[] NET_ERROR_FLOW = {"T QUEUED","T SENDING","T VISITING","F NETWORK_ERROR-2148270085","F NETWORK_ERROR-2148270085"};
    	private static final String[] NET_404_FLOW = {"T QUEUED","T SENDING","T VISITING", "F NETWORK_ERROR-404","F NETWORK_ERROR-404"};
    	private static final String[] SOCKET_ERROR_FLOW = {"T QUEUED","T SENDING","T PROCESS_ERROR-0","T SENDING","T VISITING","F SOCKET_ERROR-0"};
    	private static final String[] CONNECTION_RESET_FLOW = {"T QUEUED","T SENDING","T VISITING","T VM_STALLED-0","T SENDING","T VISITING","F CAPTURE_CLIENT_CONNECTION_RESET-0"};
    	private static final String[] INVALID_URL_FLOW = {"T QUEUED","T SENDING","F INVALID_URL"};
		
    	static String[] getFlow(int i) {
    		if(i<0)
    			return SIMPLE_FLOW;
    		try {
    			Class<?> c = Class.forName(Flows.class.getName());
				Field[] fields = c.getDeclaredFields();
				return (String[]) fields[i % fields.length].get(null);
			} catch (ClassNotFoundException e) {
				System.exit(7);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
    		return SIMPLE_FLOW;
    	}
    	static String[] getFlow(String flowName) {
    		
			try {
				Class<?> c = Class.forName(Flows.class.getName());
				Field field = c.getField(flowName);
				return (String[]) field.get(null);
			} catch (ClassNotFoundException e) {
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				return SIMPLE_FLOW;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			
			return SIMPLE_FLOW;
			
			
    		
    	}
    }

    private final int port;
    
    /**
     * @param port
     * @param fileName - based on location of this file, changes directory will be created as well
     * @param logAppend
     */
    public CaptureServerMock(int port,String fileName,boolean logAppend) {
    	this.port = port;
    	File f = new File(fileName);
    	WORKING_DIR = f.getParent();
    	LOGGER.debug("WORKING DIR IS: {}",WORKING_DIR);
    	if(!logAppend)
    		f.delete();
    		deleteChangesDir();
    	try {
			writer = new PrintWriter(new FileOutputStream(f,logAppend));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    	
    }
    
    private void deleteChangesDir() {
		File d = new File(WORKING_DIR !=null ? WORKING_DIR+"/changes" : "changes");
		if ( d.exists() && d.isDirectory()) {
			for(File f :d.listFiles())
				f.delete();
		}
		
	}

	public static void main(String[] args) {
    	
    	CaptureServerMock cm;
    	if ( args.length == 0) {
    		cm = new CaptureServerMock(DEFAULT_PORT, "output.log", false);
    	} else {
    		cm = new CaptureServerMock(DEFAULT_PORT, args[0],true);
    	}
    	cm.generateHPCfiles(true);
    	cm.startServer();
    }


	public void startServer() {
		try {
			socket = new ServerSocket(port);
			socket.setSoTimeout(1000);
			LOGGER.info("Listening port: {}",port);
			while(!Thread.currentThread().isInterrupted() ) {
				try {
					Socket s = socket.accept();
					LOGGER.info("New socket connection {}",s.getRemoteSocketAddress().toString());
					CaptureSocketHandler c = new CaptureSocketHandler(s, r.nextInt(DELAY_RANGE+1)+MIN_DELAY);
					ex.execute(c);
				}catch (SocketTimeoutException e) {
					LOGGER.trace("Timeout, wait again");
				} catch (IOException e) {
					LOGGER.info("Interrupting current thread {}",e.getMessage());
					Thread.currentThread().interrupt();
				}
			}
		}  catch (IOException e) {
			LOGGER.error("ServerSocket error: {}",e.getMessage());
			throw new IllegalStateException("Cannot open socket", e);
		}
		finally {
			stopServer();
		}
	}
    private void stopServer() {
    	ex.shutdown();
    	try {
			socket.close();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	
    }
    public CaptureServerMock generateHPCfiles(boolean enabled ) {
    	GEN_FILES = enabled;
    	return this;
    }
    public CaptureServerMock setMinFlowStepGenerationTime(int t)  {
    	if(t>1)
    		MIN_DELAY = t;
    	return this;
    }
    public CaptureServerMock setMaxFlowStepGenerationTime(int t) {
    	if(t>1)
    		DELAY_RANGE = t;
    	return this;
    }


    
    public class CaptureSocketHandler implements Runnable {

		private Socket inSocket;
		private long delay;

		public CaptureSocketHandler(Socket s, long captureDelay) {		
			this.inSocket = s;
			this.delay = captureDelay;
		}


		@Override
		public void run() {
			try {
				LineNumberReader lnr = new LineNumberReader(new InputStreamReader(inSocket.getInputStream()));
				try {
					String cmd = null;
					while ((cmd = lnr.readLine()) != null && !Thread.currentThread().isInterrupted()) {
						LOGGER.info("Received command: {}",cmd);
						processCommand(cmd);
					}
					
				} catch (IOException e) {
					LOGGER.error("Exception while reading from the InputStream", e);
				} finally {
					try {
						lnr.close();
					} catch (IOException e) {
						LOGGER.error("Exception while closing Reader", e);
						e.printStackTrace();
					}
				}

			} catch (IOException e1) {
				e1.printStackTrace();
			}
			finally {
				try {
					if(!inSocket.isClosed())
						inSocket.close();
					LOGGER.info("Socket closed");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
		

		private void processCommand(String cmd) throws IOException {
			if (cmd != null) {
				String[] cmdArray = cmd.split(" ",2);
				execCommand(cmdArray);
			}
		}
		    private void logUsage() {
		    	LOGGER.info("Usage:\n'Telnet' the capture server at {}/{} and issue following commands or connect server:",inSocket.getLocalAddress(),inSocket.getLocalPort());
		    	LOGGER.info("\taddurl <url> [<flow number>]  <capture task id>");
		    	LOGGER.info("\tqueue .....");
		    	LOGGER.info("\tfinish ....");
		    	LOGGER.info("\twrite ......");
		    	LOGGER.info("\traw/raw_write <line write to capture's output log>");
		    	LOGGER.info("\texit/quit - stop the server");
		    }

		    private void execCommand(String[] cmdArray) throws IOException {
		    	String [] cmd = null;
		    	if ("addurl".equalsIgnoreCase(cmdArray[0]) || "add".equalsIgnoreCase(cmdArray[0]) ) {
		    		cmd = cmdArray[1].split(" ");
		    		//HPC handles ids of type long
		    		if ( cmd.length < 2 ) {
		    			logUsage();
		    			return;
		    		}
		    		Long id = Long.decode(cmd[cmd.length - 1]);
		    		int jobFlow = -1;
		    		if ( cmd.length > 2) {
		    			try {
		    			jobFlow = Integer.decode(cmd[cmd.length - 2]);
		    			} catch (NumberFormatException e) {
		    				//fields on beginnig are part of url.
		    			}
		    		}
		    		String url = "";
		    		int end = jobFlow >= 0 ? cmd.length : cmd.length-1;
		    		for (int i=0;i<end;i++) {
		    			url+=cmd[i];
		    			if ( i != end -1)
		    				url += " ";
		    		}
		    		if ( jobFlow < 0)
		    			jobFlow = r.nextInt();
		    		processAddUrl(url, id.toString(), Flows.getFlow(jobFlow));
		    		
		    	} else if ("queue".equalsIgnoreCase(cmdArray[0])) {
		    		cmd = cmdArray[1].split(" ");
		    		if ( cmd.length > 1) {
		    			writer.format("%1$tF %1$tT.%1$tL %2$s %3$s %4$s %5$s %6$s\n", new Date(), "127.0.0.1", "T", "QUEUED", cmdArray[0],cmdArray[1]);
		    		} else {
		    			logUsage();
		    		}
		    	} else if ("finish".equalsIgnoreCase(cmdArray[0])) {
		    		Long var = null;
		    		cmd = cmdArray[1].split(" ");
		    		String url = "http://MOCKED"+".PL";
		    		for(int i = 1;i < cmd.length;i++) {
		    			try {
		    				var = Long.decode(cmdArray[i]);
		    				if(i < cmd.length - 1)
		    					Long.decode(cmdArray[i+1]);
		    			} catch(NumberFormatException e) {
		    				url = cmdArray[++i];
		    			}
		    			writer.format("%1$tF %1$tT.%1$tL %2$s %3$s %4$s %5$s %6$s\n",new Date(),"127.0.0.1","T","SENDING",var.toString(),url);
		    			writer.format("%1$tF %1$tT.%1$tL %2$s %3$s %4$s %5$s %6$s\n",new Date(),"127.0.0.1","T","VISITING",var.toString(),url);		    		
		    			writer.format("%1$tF %1$tT.%1$tL %2$s %3$s %4$s %5$s %6$s\n",new Date(),"127.0.0.1","T","VISITED",var.toString(),url);		    		
		    			writer.format("%1$tF %1$tT.%1$tL %2$s %3$s %4$s %5$s %6$s\n",new Date(),"127.0.0.1","F","BENIGN",var.toString(),url);

		    		}	

		    	} else if ("quit".equalsIgnoreCase(cmdArray[0]) || "exit".equalsIgnoreCase(cmdArray[0])) {
		    		stopServer();
		    	} else if ("raw".equalsIgnoreCase(cmdArray[0]) || "raw_write".equalsIgnoreCase(cmdArray[0])) {
		    		writer.write(cmdArray[1]);
		    	} else if ("write".equalsIgnoreCase(cmdArray[0])) {
		    		cmd = cmdArray[1].split(" ");
		    		if ( cmd.length == 4) {
		    			writer.format("%1$tF %1$tT.%1$tL %2$s %3$s %4$s %5$s %6$s\n",new Date(),"127.0.0.1",cmd[0],cmd[1],cmd[2],cmd[3] );
		    		} else {
		    			logUsage();
		    		}
		    	} else {
		    		logUsage();
		    	}
		    	writer.flush();

		    }
		    
		    private void processAddUrl(String url, String id, String[] jobFlow) throws IOException {
		    	LOGGER.debug("Writing to output.log: URL: {}, id: {}" ,new Object[] {url,id,});
		    	try {
		    		long test = Long.parseLong(id);
		    	} catch (NumberFormatException e) {
		    		LOGGER.warn("ID value must be a number [{}] ",id);
		    	}
		    	writer.format("%1$tF %1$tT.%1$tL %2$s %3$s %4$s %5$s\n", new Date(), "127.0.0.1", jobFlow[0], id, url);
		    	writer.flush();
		    	try {
		    		Thread.sleep(delay*2);
		    		for(int i = 1; i<jobFlow.length;i++) {
		    			if (GEN_FILES && "F MALICIOUS".equalsIgnoreCase(jobFlow[i])) {
		    				createFiles(id,"log",1);
		    				createFiles(id,"zip",1);
		    				
		    			}
		    			if ( jobFlow[i].equals("F INVALID_URL")) {
		    			writer.format("%1$tF %1$tT.%1$tL %2$s %3$s %4$s %5$s\n", new Date(), "IP_NA", jobFlow[i], id, url);
		    			} else {
		    				writer.format("%1$tF %1$tT.%1$tL %2$s %3$s %4$s %5$s\n", new Date(), "127.0.0.1", jobFlow[i], id, url);
		    			}
		    			writer.flush();
		    			
		    			Thread.sleep(delay);
		    		}
		    	} catch (InterruptedException e) {
		    		e.printStackTrace();
		    	}
		    }

		    private void createFiles(String id, String suffix, int count) throws IOException {
		    	LOGGER.debug("Writing files: {}changes/*.{}",WORKING_DIR == null ?"":WORKING_DIR+"/",suffix);
		        File dir = new File(WORKING_DIR !=null ? WORKING_DIR+"/changes" : "changes");
		        dir.mkdir();
		        for (int i=0; i<count; i++) {
		            File newFile = new File(dir, id + "_0123456_" + i + "." + suffix);
		            newFile.createNewFile();
		            if ( newFile.exists()) {
		            	if ( suffix.equalsIgnoreCase("zip")) {
		            		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(newFile));
		            		out.putNextEntry(new ZipEntry("hpc_id"+id+".txt"));
		            		out.write((id+" "+(new Date()).toString()).getBytes());
		            		out.closeEntry();
		            		out.close();
		            	} else {
		            		PrintWriter pw = new PrintWriter(newFile);
		            		pw.append("HPC task id: "+id);
		            		pw.flush();
		            	}
		            }
		        }

		    }
		   
    }

	@Override
	public void run() {
		startServer();
		
	}



	
}
