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

import org.testng.Assert;
import org.testng.annotations.Test;

import pl.nask.hsn2.ParameterException;
import pl.nask.hsn2.ResourceException;
import pl.nask.hsn2.StorageException;
import pl.nask.hsn2.TaskContext;
import pl.nask.hsn2.service.CaptureTask;
import pl.nask.hsn2.wrappers.ObjectDataWrapper;

@Test
public class HpcConnectorTest extends HpcAbstractTest {


    public void testAddUrl() throws IOException, InterruptedException, ParameterException, ResourceException, StorageException {
    	final long objectId = 1;
    	HpcTaskTest.setTestId(objectId);
    	
    	ObjectDataWrapper data= createObjectDataFor(objectId, "nask.pl");
        CaptureTask task = (CaptureTask) factory.newTask(new TaskContext(1, 1, objectId, serviceConnector), params, data);
        // process the task
        task.process();
        // check if the task is processed
        HpcTask hpcTask = task.getHpcTask();
        Assert.assertTrue(hpcTask.isUnregistered());
        Assert.assertTrue(hpcTask.isProcessed());
    }
}
