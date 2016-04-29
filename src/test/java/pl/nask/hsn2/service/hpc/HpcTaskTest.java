/*
 * Copyright (c) NASK, NCSC
 * 
 * This file is part of HoneySpider Network 2.1.
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

import mockit.NonStrictExpectations;

import org.testng.Assert;
import org.testng.annotations.Test;

import pl.nask.hsn2.ParameterException;
import pl.nask.hsn2.ResourceException;
import pl.nask.hsn2.StorageException;
import pl.nask.hsn2.TaskContext;
import pl.nask.hsn2.bus.operations.AttributeType;
import pl.nask.hsn2.bus.operations.ObjectData;
import pl.nask.hsn2.connector.REST.DataResponse;
import pl.nask.hsn2.service.CaptureTask;
import pl.nask.hsn2.wrappers.ObjectDataWrapper;

@SuppressWarnings("unused")
public class HpcTaskTest extends HpcAbstractTest {
    private static int testId;
	DataResponse dataResponse = new DataResponse(1L);

    @Test(enabled=true)
    public void testNormalUrl() throws InterruptedException, IOException, ParameterException, ResourceException, StorageException {
    	final long objectId = 1;
    	HpcTaskTest.setTestId(objectId);
        new NonStrictExpectations() {{
           serviceConnector.updateObject(anyLong, null);times=1;
           forEachInvocation = new Object() {
			void validate(long jobId, ObjectData object) {
                   Assert.assertEquals(jobId, objectId);
                   Assert.assertEquals(object.getId(), objectId);
                   assertActive(object, "benign");
                   Assert.assertNotNull(object.findAttribute("hpc_time_start",  AttributeType.TIME));
                   Assert.assertNotNull(object.findAttribute("hpc_time_stop",  AttributeType.TIME));
               }
           };
        }};

        runHpcTask(1);
    }

    @Test(enabled=true)
    public void testNormalUrlWithZipFile() throws InterruptedException, IOException, ResourceException, ParameterException, StorageException {
        final long objectId = 2;
        HpcTaskTest.setTestId(objectId);
        
		new NonStrictExpectations() {
			{
				serviceConnector.sendDataStoreData(anyLong, withInstanceOf(InputStream.class));
				result = dataResponse;
				times = 1;

				serviceConnector.updateObject(anyLong, withInstanceOf(ObjectData.class));
				times = 1;
				forEachInvocation = new Object() {
					void validate(long jobId, ObjectData object) {
						Assert.assertEquals(jobId, objectId);
						Assert.assertEquals(object.getId(), objectId);
						assertActive(object, "benign");
						Assert.assertNotNull(object.findAttribute("hpc_zip_file", AttributeType.BYTES));
						Assert.assertNull(object.findAttribute("hpc_zip_file2", AttributeType.BYTES));
					}
				};
			}};

        runHpcTask(objectId);
    }

    @Test(enabled=true)
    public void testWithNetworkError() throws InterruptedException, IOException, ResourceException, ParameterException, StorageException {
        final long objectId = 3;
        HpcTaskTest.setTestId(objectId);
        
		new NonStrictExpectations() {
			{
				serviceConnector.updateObject(anyLong, withInstanceOf(ObjectData.class));
				times = 1;
				forEachInvocation = new Object() {
					void validate(long jobId, ObjectData object) {
						Assert.assertEquals(jobId, objectId);
						Assert.assertEquals(object.getId(), objectId);
						assertNotActive(object, "NETWORK_ERROR-123123");
						Assert.assertNull(object.findAttribute("hpc_log_file", AttributeType.BYTES));
						Assert.assertNull(object.findAttribute("hpc_zip_file2", AttributeType.BYTES));
						Assert.assertNull(object.findAttribute("hpc_zip_file", AttributeType.BYTES));
						Assert.assertNull(object.findAttribute("hpc_zip_file2", AttributeType.BYTES));
					}
				};
			}};

			runHpcTask(objectId);
    }

    @Test(enabled=true)
    public void testWithVmStalled() throws InterruptedException, IOException, ResourceException, ParameterException, StorageException {
        final long objectId = 4;
        HpcTaskTest.setTestId(objectId);
        
		new NonStrictExpectations() {
			{
				serviceConnector.updateObject(anyLong, withInstanceOf(ObjectData.class));
				times = 1;
				forEachInvocation = new Object() {
					void validate(long jobId, ObjectData object) {
						Assert.assertEquals(jobId, objectId);
						Assert.assertEquals(object.getId(), objectId);
						assertNotActive(object, "VM_STALLED-0");
						Assert.assertNull(object.findAttribute("hpc_log_file", AttributeType.BYTES));
						Assert.assertNull(object.findAttribute("hpc_zip_file2", AttributeType.BYTES));
						Assert.assertNull(object.findAttribute("hpc_zip_file", AttributeType.BYTES));
						Assert.assertNull(object.findAttribute("hpc_zip_file2", AttributeType.BYTES));
					}
				};
			}};

        runHpcTask(objectId);
    }

    @Test(enabled=true)
    public void testMalicious() throws InterruptedException, IOException, ResourceException, ParameterException, StorageException {
        final long objectId = 5;
        HpcTaskTest.setTestId(objectId);

        new NonStrictExpectations() {
            {
                serviceConnector.sendDataStoreData(anyLong, withInstanceOf(InputStream.class));result=dataResponse;times=2;

           serviceConnector.updateObject(anyLong, withInstanceOf(ObjectData.class));times=1;
           forEachInvocation = new Object() {
               void validate(long jobId, ObjectData object) {
                   Assert.assertEquals(jobId, objectId);
                   Assert.assertEquals(object.getId(), objectId);
                   assertActive(object, "malicious");
                   Assert.assertNotNull(object.findAttribute("hpc_log_file", AttributeType.BYTES));
                   Assert.assertNull(object.findAttribute("hpc_zip_file2", AttributeType.BYTES));
                   Assert.assertNotNull(object.findAttribute("hpc_zip_file", AttributeType.BYTES));
                   Assert.assertNull(object.findAttribute("hpc_zip_file2", AttributeType.BYTES));
               }
           };
        }};

        runHpcTask(objectId);
    }

   

	@Test(enabled=true)
    public void testMaliciousWith2logAndZipFiles() throws InterruptedException, IOException, ResourceException, ParameterException, StorageException {
        final long objectId = 6;
        HpcTaskTest.setTestId(objectId);
        
		new NonStrictExpectations() {
			{
				serviceConnector.sendDataStoreData(anyLong, withInstanceOf(InputStream.class));
				result = dataResponse;
				times = 4;

				serviceConnector.updateObject(anyLong, withInstanceOf(ObjectData.class));
				times = 1;
				forEachInvocation = new Object() {
					void validate(long jobId, ObjectData object) {
						Assert.assertEquals(jobId, objectId);
						Assert.assertEquals(object.getId(), objectId);
						assertActive(object, "malicious");
						Assert.assertNotNull(object.findAttribute("hpc_log_file", AttributeType.BYTES));
						Assert.assertNotNull(object.findAttribute("hpc_zip_file2", AttributeType.BYTES));
						Assert.assertNotNull(object.findAttribute("hpc_zip_file", AttributeType.BYTES));
						Assert.assertNotNull(object.findAttribute("hpc_zip_file2", AttributeType.BYTES));
					}
				};
			}};

        runHpcTask(objectId);
    }

    @Test(enabled=true)
    public void testMaliciousWithLogFileOnly() throws InterruptedException, IOException, ResourceException, ParameterException, StorageException {
        final long objectId = 7;
        HpcTaskTest.setTestId(objectId);
        
        new NonStrictExpectations() {
            {
           serviceConnector.sendDataStoreData(anyLong, withInstanceOf(InputStream.class));result=dataResponse;times=1;

           serviceConnector.updateObject(anyLong, withInstanceOf(ObjectData.class));times=1;
           forEachInvocation = new Object() {
               void validate(long jobId, ObjectData object) {
                   Assert.assertEquals(jobId, objectId);
                   Assert.assertEquals(object.getId(), objectId);
                   assertActive(object, "malicious");
                   Assert.assertNotNull(object.findAttribute("hpc_log_file", AttributeType.BYTES));
                   Assert.assertNull(object.findAttribute("hpc_zip_file2", AttributeType.BYTES));
                   Assert.assertNull(object.findAttribute("hpc_zip_file", AttributeType.BYTES));
                   Assert.assertNull(object.findAttribute("hpc_zip_file2", AttributeType.BYTES));
               }
           };
        }};

        runHpcTask(objectId);
    }

    @Test(enabled=true)
    public void testWithClientConnectionReset() throws InterruptedException, IOException, ResourceException, ParameterException, StorageException {
        final long objectId = 8;
        HpcTaskTest.setTestId(objectId);
        
        new NonStrictExpectations() {
            {

           serviceConnector.updateObject(anyLong, withInstanceOf(ObjectData.class));times=1;
           forEachInvocation = new Object() {
               void validate(long jobId, ObjectData object) {
                   Assert.assertEquals(object.getId(), objectId);
                   assertNotActive(object, "CAPTURE_CLIENT_CONNECTION_RESET-0");
                   Assert.assertNull(object.findAttribute("hpc_log_file", AttributeType.BYTES));
                   Assert.assertNull(object.findAttribute("hpc_zip_file2", AttributeType.BYTES));
                   Assert.assertNull(object.findAttribute("hpc_zip_file", AttributeType.BYTES));
                   Assert.assertNull(object.findAttribute("hpc_zip_file2", AttributeType.BYTES));
               }
           };
        }};

        runHpcTask(objectId);
    }

    @Test(enabled=true)
    public void testWithSocketError() throws InterruptedException, IOException, ResourceException, ParameterException, StorageException {
        final long objectId = 9;
        HpcTaskTest.setTestId(objectId);
        
        new NonStrictExpectations() {
            {

           serviceConnector.updateObject(anyLong, withInstanceOf(ObjectData.class));times=1;
           forEachInvocation = new Object() {
               void validate(long jobId, ObjectData object) {
                   Assert.assertEquals(object.getId(), objectId);
                   assertNotActive(object, "SOCKET_ERROR-0");
                   Assert.assertNull(object.findAttribute("hpc_log_file", AttributeType.BYTES));
                   Assert.assertNull(object.findAttribute("hpc_zip_file2", AttributeType.BYTES));
                   Assert.assertNull(object.findAttribute("hpc_zip_file", AttributeType.BYTES));
                   Assert.assertNull(object.findAttribute("hpc_zip_file2", AttributeType.BYTES));
               }
           };
        }};

        runHpcTask(objectId);
    }

    @Test(enabled=true)
    public void testDuplicated() throws InterruptedException, IOException, ResourceException, ParameterException, StorageException {
        final long objectId = 10;
        HpcTaskTest.setTestId(objectId);
        
        new NonStrictExpectations() {
            {

           serviceConnector.updateObject(anyLong, withInstanceOf(ObjectData.class));times=1;
           forEachInvocation = new Object() {
               void validate(long jobId, ObjectData object) {
                   Assert.assertEquals(object.getId(), objectId);
                   assertNotActive(object, "DPLCT");
                   Assert.assertNull(object.findAttribute("hpc_log_file", AttributeType.BYTES));
                   Assert.assertNull(object.findAttribute("hpc_zip_file2", AttributeType.BYTES));
                   Assert.assertNull(object.findAttribute("hpc_zip_file", AttributeType.BYTES));
                   Assert.assertNull(object.findAttribute("hpc_zip_file2", AttributeType.BYTES));
               }
           };
        }};

        runHpcTask(objectId);
    }

    private void assertActive(ObjectData wrapper, String classification) {
        Assert.assertTrue(wrapper.findAttribute("hpc_active", AttributeType.BOOL).getBool());
        Assert.assertNull(wrapper.findAttribute("hpc_reason_failed", AttributeType.STRING));
        Assert.assertEquals(wrapper.findAttribute("hpc_classification", AttributeType.STRING).getString(), classification);
        Assert.assertNotNull(wrapper.findAttribute("hpc_time_start", AttributeType.TIME));
        Assert.assertNotNull(wrapper.findAttribute("hpc_time_stop", AttributeType.TIME));
    }

    private void assertNotActive(ObjectData wrapper, String failureReason) {
        Assert.assertFalse(wrapper.findAttribute("hpc_active", AttributeType.BOOL).getBool());
        Assert.assertEquals(wrapper.findAttribute("hpc_reason_failed", AttributeType.STRING).getString(), failureReason);
        Assert.assertNull(wrapper.findAttribute("hpc_classification", AttributeType.STRING));
        Assert.assertNotNull(wrapper.findAttribute("hpc_time_start", AttributeType.TIME));
        Assert.assertNotNull(wrapper.findAttribute("hpc_time_stop", AttributeType.TIME));
    }


    private HpcTask runHpcTask(long id) throws InterruptedException, ParameterException, ResourceException, StorageException {
        ObjectDataWrapper data= createObjectDataFor(id, "nask.pl");
        TaskContext ctx = new TaskContext(id, 1, id, serviceConnector);
        CaptureTask task = (CaptureTask) factory.newTask(ctx, params, data);
        // process the task     
        task.process();
        // check if the task is processed
        HpcTask hpcTask = task.getHpcTask();

        Assert.assertTrue(hpcTask.isUnregistered(), "Task processed");
        ctx.flush();
        return hpcTask;
    }

	public static int getTestId() {
		return testId;
	}
	public synchronized static void setTestId(long l) {
		synchronized (HpcTaskTest.class) {
			HpcTaskTest.testId =(int)l;
		}
	}
}
