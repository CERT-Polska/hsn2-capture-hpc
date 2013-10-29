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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import pl.nask.hsn2.FinishedJobsListener;
import pl.nask.hsn2.ParameterException;
import pl.nask.hsn2.RequiredParameterMissingException;
import pl.nask.hsn2.ResourceException;
import pl.nask.hsn2.ServiceConnector;
import pl.nask.hsn2.StorageException;
import pl.nask.hsn2.TaskProcessor;
import pl.nask.hsn2.connector.BusException;
import pl.nask.hsn2.connector.REST.DataResponse;
import pl.nask.hsn2.protobuff.Object.Attribute;
import pl.nask.hsn2.protobuff.Object.Attribute.Builder;
import pl.nask.hsn2.protobuff.Object.Attribute.Type;
import pl.nask.hsn2.protobuff.Object.ObjectData;
import pl.nask.hsn2.protobuff.ObjectStore.ObjectResponse;
import pl.nask.hsn2.protobuff.ObjectStore.ObjectResponse.ResponseType;
import pl.nask.hsn2.protobuff.Process.TaskRequest;
import pl.nask.hsn2.service.CaptureTaskFactory;
import pl.nask.hsn2.wrappers.ParametersWrapper;

public class CaptureTaskProcessingTest {
	
	private static final String OUTPUT_LOG = "output.log";
	private static final int SERVICE_THREAD_COUNT=10;
	private static Thread captureServerThread , logAnalizerThread;
	private TaskRegistry taskRegistry = null;
	private CaptureHpcConnector captureConnector = null;
	private static ExecutorService genericServiceRunner = null;
	private CaptureTaskFactory taskFactory = null;
	
	private static final int URL_LIMIT = 300;
	
	private static final AtomicInteger taskAcceptedCalls = new AtomicInteger(0);
	private static final AtomicInteger taskRequestCalls = new AtomicInteger(0);
	private static final AtomicInteger taskCompletedCalls = new AtomicInteger(0);

	@Mocked
	ParametersWrapper paramWrapper;
	@Mocked
	private FinishedJobsListener finishedJobsListener;

	public static class ServiceConnectorMock extends MockUp<ServiceConnector> {
		@Mock
		public TaskRequest getTaskRequest() throws BusException, InterruptedException{
			int taskId = -1;
			synchronized (taskRequestCalls) {
			if(taskRequestCalls.get() >= URL_LIMIT) {
				throw new InterruptedException();
			}

			taskId = taskRequestCalls.incrementAndGet();
			}
			pl.nask.hsn2.protobuff.Process.TaskRequest.Builder trb = TaskRequest.newBuilder().setJob(1).setTaskId(taskId).setObject(taskId);
			return trb.build() ;
		}
		
		@Mock(maxInvocations=URL_LIMIT)
		public ObjectResponse getObjectStoreData(long jobId, List<Long> objectsId) throws StorageException {
			ObjectResponse.Builder b = ObjectResponse.newBuilder();
			ObjectData.Builder objDataBuilder = ObjectData.newBuilder();
			
			Builder attrBuilder = Attribute.newBuilder();
			attrBuilder.setName("url_original");
			attrBuilder.setType(Type.STRING);
			attrBuilder.setDataString("http://test.com/test_"+objectsId.get(0));
			objDataBuilder.addAttrs(attrBuilder);
			
			b.addData(objDataBuilder.build() );
			b.setType(ResponseType.SUCCESS_GET);
			return b.build();
		}
		@Mock
		public void sendTaskAccepted(long jobId, int requestId)
				throws BusException {
			taskAcceptedCalls.incrementAndGet();
			
		}
		@Mock
		public void sendTaskComplete(long jobId, int requestId,
				List<Long> newObjects) throws BusException {
			taskCompletedCalls.incrementAndGet();
		}
		@Mock(maxInvocations=0)
		public void sendTaskCompletedWithWarnings(long jobId, int requestId,
				List<Long> newObjects, List<String> warnings) {
			
		}
		@Mock(invocations=0)
		public ObjectResponse sendObjectStoreData(long jobId, int requestId,
				Iterable<? extends ObjectData> dataList)
				throws StorageException {
			return null;
		}
		@Mock
		public pl.nask.hsn2.bus.operations.ObjectResponse saveObjects(
				long jobId,
				List<pl.nask.hsn2.bus.operations.ObjectData> dataList)
				throws StorageException {
			return null;
		}
		@Mock
		public void sendTaskError(long jobId, int reqId, ParameterException e) {
			
		}
		@Mock
		public void sendTaskError(long jobId, int reqId, Exception e) {
			
		}
		@Mock
		public void sendTaskError(long jobId, int reqId, ResourceException e) {
			
		}
		@Mock
		public ObjectResponse updateObjectStoreData(long jobId,
				Iterable<? extends ObjectData> dataList)
				throws StorageException {
			return null;
		}
		@Mock
		public pl.nask.hsn2.bus.operations.ObjectResponse updateObject(
				long jobId, pl.nask.hsn2.bus.operations.ObjectData objectData)
				throws StorageException {
			pl.nask.hsn2.bus.operations.ObjectResponse or = new pl.nask.hsn2.bus.operations.ObjectResponse(
					pl.nask.hsn2.bus.operations.ObjectResponse.ResponseType.SUCCESS_UPDATE);
			return or;
		}
		@Mock
		public DataResponse sendDataStoreData(long jobId, byte[] data)
				throws IOException {
			return null;
		}
		@Mock
		public DataResponse sendDataStoreData(long jobId, InputStream is)
				throws ResourceException, IOException {
			return null;
		}
		@Mock
		public InputStream getDataStoreDataAsStream(long jobId, long referenceId)
				throws ResourceException, StorageException {
			return null;
		}
	}
	
	@Test(enabled=false,timeOut=1500000)
	public void multiThreadedTasksProcessing() throws InterruptedException, IOException {
		prepareHpcProfileMock("winxp");

		List<Callable<Void>> l = new ArrayList<Callable<Void>>(SERVICE_THREAD_COUNT);
		for(int i=0; i < SERVICE_THREAD_COUNT;i++) {
			TaskProcessor t = new TaskProcessor(taskFactory,new ServiceConnectorMock().getMockInstance(), new FinishedJobsListener());
			l.add(t);
		}
		
		try {
			genericServiceRunner.invokeAll(l);
		} catch (InterruptedException e) {
			genericServiceRunner.shutdownNow();
		}
		
		Assert.assertTrue(taskCompletedCalls.get() == taskAcceptedCalls.get());
		Assert.assertTrue(URL_LIMIT == taskCompletedCalls.get());
		Assert.assertTrue(URL_LIMIT == taskRequestCalls.get());
	}
	
	private void prepareHpcProfileMock(final String hpc_profile) {
		try {
			new NonStrictExpectations() {
				{
					paramWrapper.get("hpc_profile");returns(hpc_profile);
				}
			};
		} catch (RequiredParameterMissingException e1) {
			e1.printStackTrace();
		}
	}
	
	@Test(enabled = true)
	public void multiSpaceUrlTest() {
		prepareHpcProfileMock("winxp");
		
		final int noTasks = 1;
	
		List<Callable<Void>> l = new ArrayList<Callable<Void>>(SERVICE_THREAD_COUNT);
		for(int i=0; i < SERVICE_THREAD_COUNT;i++) {
			ServiceConnectorMock scm = getModifiedServiceConnectorMock(noTasks);
			TaskProcessor t = new TaskProcessor(taskFactory,scm.getMockInstance(), new FinishedJobsListener());
			l.add(t);
		}
		
		try {
			genericServiceRunner.invokeAll(l);
		} catch (InterruptedException e) {
			genericServiceRunner.shutdown();
		}
		
		Assert.assertTrue(taskCompletedCalls.get() == taskAcceptedCalls.get());
		Assert.assertTrue(noTasks == taskCompletedCalls.get());
		Assert.assertTrue(noTasks == taskRequestCalls.get());
	
	}

	private ServiceConnectorMock getModifiedServiceConnectorMock(final int noCalls) {

		return new ServiceConnectorMock() {
			@Override
			public TaskRequest getTaskRequest() throws BusException, InterruptedException{
				int taskId;
				synchronized(taskRequestCalls) {
				if(taskRequestCalls.get() >= noCalls) {
					throw new InterruptedException();
				}
				taskId = taskRequestCalls.incrementAndGet();
				}
				pl.nask.hsn2.protobuff.Process.TaskRequest.Builder trb = TaskRequest.newBuilder().setJob(2).setTaskId(taskId).setObject(taskId);
				return trb.build() ;
			}
			
			@Override
			public ObjectResponse getObjectStoreData(long jobId, List<Long> objectsId) throws StorageException {
				ObjectResponse.Builder b = ObjectResponse.newBuilder();
				ObjectData.Builder objDataBuilder = ObjectData.newBuilder();
				
				Builder attrBuilder = Attribute.newBuilder();
				attrBuilder.setName("url_original");
				attrBuilder.setType(Type.STRING);
				attrBuilder.setDataString("http://space.com/test "+objectsId.get(0)+" x.html");
				objDataBuilder.addAttrs(attrBuilder);
				
				b.addData(objDataBuilder.build() );
				b.setType(ResponseType.SUCCESS_GET);
				return b.build();
			}
			
		};

	}
  
	
  @BeforeClass
  public void beforeClass() {
	 
	  taskRegistry = new TaskRegistry();
	  
	  captureServerThread = new Thread(new CaptureServerMock(32337,OUTPUT_LOG,false)
	  		.generateHPCfiles(true)
	  		.setMaxFlowStepGenerationTime(1000)
	  		.setMinFlowStepGenerationTime(30),"Capture Server");
	  captureServerThread.start();
	  
	  logAnalizerThread = new Thread(new HpcLogAnalyserImpl(OUTPUT_LOG, taskRegistry),"Log Analyzer" );
	  logAnalizerThread.start();

	  captureConnector = new CaptureHpcConnectorImpl("localhost", 32337);
	  
	  CaptureTaskFactory.prepereForAllThreads(captureConnector, taskRegistry, "changes");
      taskFactory = new CaptureTaskFactory();
  }
  
  
  
  
  @BeforeMethod
	public void beforeTest() {
		if (genericServiceRunner == null) {
			genericServiceRunner = Executors.newFixedThreadPool(SERVICE_THREAD_COUNT);
		} else {
			genericServiceRunner.shutdownNow();
			genericServiceRunner = Executors.newFixedThreadPool(SERVICE_THREAD_COUNT);
		}
		taskAcceptedCalls.set(0);
		taskRequestCalls.set(0);
		taskCompletedCalls.set(0);

		new NonStrictExpectations() {
			{
				finishedJobsListener.isJobFinished(anyLong);
				result = false;
			}
		};
	}
  
  @AfterClass
  public void afterClass() throws InterruptedException {
	  captureServerThread.interrupt();
	  logAnalizerThread.interrupt();
	  captureServerThread.join();
	  logAnalizerThread.join();
	  
  }

}
