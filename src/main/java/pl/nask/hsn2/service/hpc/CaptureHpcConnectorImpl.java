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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Simple implementation of HPC connector. Will use a new connection for each command.
 * 
 * This class is thread safe.
 * 
 *
 */
public class CaptureHpcConnectorImpl implements CaptureHpcConnector {

    private final String host;
    private final int port;

    public CaptureHpcConnectorImpl(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void exit() throws IOException {
        send("exit");
    }

    private void send(String command) throws IOException {
    	Socket socket = null;
    	PrintWriter pw = null;
    	try {
    		socket = createSocket();
    		pw = new PrintWriter(socket.getOutputStream());
    		pw.println(command);
    	} finally {
    		if (pw != null) {
    			pw.flush();
    			pw.close();
    		}
    		if (socket != null)
    			socket.close();
    	}
    }

    private Socket createSocket() throws UnknownHostException, IOException {
        Socket socket = new Socket(host, port);
        return socket;
    }

    @Override
    public void reload() throws IOException {
    	send("reload");
    }
     @Override
    public void addUrl(String url, long urlId) throws IOException {
        if (url == null || url.trim().length() == 0) {
            throw new IllegalArgumentException("URL parameter cannot be empty");
        }
        send(String.format("addurl %s %s", url, urlId));
    }

    @Override
    public String getConnectorInfo() {
        return String.format("HpcConnector (host=%s, port=%s)", host, port);
    }

}
