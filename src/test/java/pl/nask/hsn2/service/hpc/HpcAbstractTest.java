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
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

import mockit.Mocked;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;

import pl.nask.hsn2.RequiredParameterMissingException;
import pl.nask.hsn2.ServiceConnector;
import pl.nask.hsn2.protobuff.Object.Attribute;
import pl.nask.hsn2.protobuff.Object.Attribute.Type;
import pl.nask.hsn2.protobuff.Object.ObjectData;
import pl.nask.hsn2.service.CaptureTaskFactory;
import pl.nask.hsn2.wrappers.ObjectDataWrapper;
import pl.nask.hsn2.wrappers.ParametersWrapper;

public abstract class HpcAbstractTest {

    protected CaptureHpcConnector hpcConnector;

    protected CaptureTaskFactory factory;

    @Mocked
    protected ServiceConnector serviceConnector;

    protected CaptureMock capture;
    protected HpcLogAnalyserImpl analyser;
    protected TaskRegistry taskRegistry;

    private Thread captureThread;
    private Thread analyserThread;

    protected File changesDir;

    protected ParametersWrapper params;


    @BeforeClass
    public void setupMockedCaptureHpcInstance() throws IOException, RequiredParameterMissingException {
        prepareParams();
        prepareFiles();
        createTaskRegistry();
        int capturePort = createCapture();
        createConnector(capturePort);
        createAnalyser();
        createFactory();

        captureThread = new Thread(capture, "capture");
        captureThread.start();

        analyserThread = new Thread(analyser, "analyser");
        analyserThread.start();
    }


    private void createConnector(int capturePort) {
    	hpcConnector = new CaptureHpcConnectorImpl("localhost" , capturePort);		
	}


	private void createFactory() {
		CaptureTaskFactory.prepereForAllThreads(hpcConnector, taskRegistry, "changes");
        factory = new CaptureTaskFactory();
    }


    @AfterClass
    public void teardownCaptureHpcInstance() throws InterruptedException {
        captureThread.interrupt();
        analyserThread.interrupt();

        captureThread.join();
        analyserThread.join();
    }

    @AfterMethod
    public void assertThreadsRunning() {
        Assert.assertTrue(captureThread.isAlive(), "Capture thread is alive");
        Assert.assertTrue(analyserThread.isAlive(), "Analyser thread is alive");
    }

    protected void createAnalyser() {
        analyser = new HpcLogAnalyserImpl("output.log", taskRegistry );
    }

    protected int createCapture() throws IOException {
    	int port = 9696;
    	while (capture == null && port < 20000) {
    		ServerSocket ss = null;
    		try {
    			ss = new ServerSocket(port);    			
    		} catch (IOException e) {
    			port++;
    		} finally {
    			if (ss != null) {
    				ss.close();
    				capture = new CaptureMock(port);
    			}
    		}
    	}
    	
    	if (capture == null) {
    		throw new IllegalStateException("Failed to create an instance of CaptureMock, giving up");
    	} 
    	
    	return port;
    }

    protected void createTaskRegistry() {
        taskRegistry = new TaskRegistry();
    }

    protected void prepareFiles() throws IOException {
        changesDir = new File("changes");
        changesDir.mkdir();
        File outputFile = new File("output.log");
        outputFile.createNewFile();
    }

    protected ObjectDataWrapper createObjectDataFor(long id, String url) {
        ObjectData objectData = ObjectData.newBuilder().setId(id).addAttrs(Attribute.newBuilder().setType(Type.STRING).setName("url_original").setDataString(url).build()).build();
        return new ObjectDataWrapper(objectData);
    }

    private void prepareParams() throws RequiredParameterMissingException {
        Map<String, String> paramsMap = new HashMap<String, String>();
        paramsMap.put("hpc_profile", "testProfile");
        params = new ParametersWrapper(paramsMap  );
    }
}
