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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaptureMock implements Runnable {
    private final static Logger LOGGER = LoggerFactory.getLogger(CaptureMock.class);

    ServerSocket socket;

    private PrintWriter writer;

    public CaptureMock(int port) throws IOException {
        try {
            File f = new File("output.log");
            f.delete();
            writer = new PrintWriter("output.log");
            socket = new ServerSocket(port);
            socket.setSoTimeout(1000);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) throws IOException {
        new CaptureMock(32337).run();
    }


    @Override
    public void run() {
    	process();
    }

    private void process() {
        while (!Thread.interrupted()) {
            try {
                Socket s = socket.accept();
                LOGGER.info("after socket.accept()");
                processWith(s.getInputStream());
                LOGGER.info("waiting on socket.accept()");
            } catch (SocketTimeoutException e) {
                // ignore and wait again
                LOGGER.debug("Timeout, wait again");
            } catch (IOException e) {
                LOGGER.error("Error on socket.accept()", e);
                Thread.currentThread().interrupt();
            }
        }
        try {
            socket.close();
        } catch (IOException e) {
            LOGGER.error("Error closing socket", e);
        }
        LOGGER.info("Thread interrupted, exiting");
    }

    private void processWith(InputStream inputStream) {
        LineNumberReader lnr = new LineNumberReader(new InputStreamReader(inputStream));

        try {
        String cmd = null;
            while ((cmd = lnr.readLine()) != null) {
                LOGGER.info("Received command: {}", cmd);
                processCommand(cmd);
            }
            LOGGER.info("InputStream closed");
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


    }

    private void processCommand(String cmd) throws IOException {
        if (cmd != null) {
            String[] cmdArray = cmd.split(" ");
            if (cmdArray.length >=2 && "addurl".equalsIgnoreCase(cmdArray[0]) ) {
                if (cmdArray.length == 2) {
                    processAddUrl(cmdArray[1], "" + randomId());
                } else if (cmdArray.length == 3) {
                    processAddUrl(cmdArray[1], cmdArray[2]);
                }
            }
        }
    }

    private void processAddUrl(String url, String id) throws IOException {
    	LOGGER.info("writing to 'output.log': URL: {}, id: {}" ,new Object[] {url,id});
        switch (HpcTaskTest.getTestId()) {
        case 1: write(url, id, "BENIGN", 0, 0); break;
        case 2: write(url, id, "BENIGN", 0, 1); break;
        case 3: write(url, id, "NETWORK_ERROR-123123", 0, 0); break;
        case 4: write(url, id, "VM_STALLED-0", 0, 0); break;
        case 5: write(url, id, "MALICIOUS", 1, 1);break;
        case 6: write(url, id, "MALICIOUS", 2, 2);break;
        case 7: write(url, id, "MALICIOUS", 1, 0);break;
        case 8: write(url, id, "CAPTURE_CLIENT_CONNECTION_RESET-0", 0, 0); break;
        case 9: write(url, id, "SOCKET_ERROR-0", 0, 0); break;
        case 10: write(url, id, "DPLCT", 0, 0); break;
        }
        writer.flush();
    }

    private void write(String url, String id, String endStatus, int logFiles, int zipFiles) throws IOException {
        write(url, id, "T QUEUED");
        write(url, id, "T SENDING");
        write(url, id, "T VISITING");
        write(url, id, "T MALICIOUS");
        write(url, id, "T VM_STALLED_0");
        write(url, id, "T SENDING");
        write(url, id, "T VISITING");
        write(url, id, "F " + endStatus);
        createFiles(id, "log", logFiles);
        createFiles(id, "zip", zipFiles);
    }

    private void createFiles(String id, String suffix, int count) throws IOException {
        File dir = new File("changes");
        dir.mkdir();
        for (int i=0; i<count; i++) {
            File newFile = new File(dir, id + "_0123456_" + i + "." + suffix);

            newFile.createNewFile();
        }

    }

    private int randomId() {
        return 0;
    }

    private void write(String url, String id, String flagStatus) {
        // DATE TIME IP 'FLAG STATUS' ID URL
        writer.format("%1$tF %1$tT.%1$tL %2$s %3$s %4$s %5$s\n", new Date(), "127.0.0.1", flagStatus, id, url);

    }
	
}
