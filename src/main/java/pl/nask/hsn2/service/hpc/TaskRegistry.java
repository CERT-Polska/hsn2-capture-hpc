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

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.TaskContext;
import pl.nask.hsn2.service.HpcCommandLineParams;
import pl.nask.hsn2.service.ServiceData;
import pl.nask.hsn2.utils.FileIdGenerator;
import pl.nask.hsn2.utils.IdGenerator;
import pl.nask.hsn2.utils.IdGeneratorException;

public class TaskRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskRegistry.class);

    private Map<Long, HpcTask> taskMap = new HashMap<Long,HpcTask>();
    private IdGenerator idGen;
    public TaskRegistry(String idGenFilePath) {
    	idGen = new FileIdGenerator();
    	((FileIdGenerator) idGen).setSequenceFile(idGenFilePath);
    }

    TaskRegistry() {
    	idGen = new FileIdGenerator();
    	((FileIdGenerator)idGen).setSequenceFile(HpcCommandLineParams.HPC_IDGEN_DEFAULT_FILENAME);
    }

    public final synchronized void log(String id, String url, String flag, String status) {
    	long tId = -1l;
    	try {
    		tId = Long.valueOf(id);
    	} catch (NumberFormatException e) {
    		LOGGER.warn("Cannot extract TaskID from HPC output: {}. Ignoring.",id);
    		return ;
    	}
        HpcTask t = getTask(tId);
        if (t == null) {
            LOGGER.warn("No such task with ID: {}.Task count: {}", id, getTasksIdsAsString());
        } else {
        	LOGGER.debug("Updating task ID {}", id);
            t.update(flag, status);
        }
    }

    public final synchronized HpcTask registerTask(TaskContext ctx, ServiceData data) throws IdGeneratorException {
    	LOGGER.debug("Registering async TaskContext (reqID={},{})", ctx!=null ? ctx.getReqId() : "",data!=null ? data.getUrlForProcessing(): "");
    	long tmpTaskId = idGen.nextId();

    	HpcTask hpcTask = new HpcTask(tmpTaskId, this);
    	taskMap.put(tmpTaskId, hpcTask);
    	if(LOGGER.isDebugEnabled())
    		LOGGER.debug("Have registered tasks: {}", getTasksIdsAsString());
    	return hpcTask;
    }

    private HpcTask getTask(Long long1) {
        return taskMap.get(long1);
    }

    public final synchronized void unregister(HpcTask hpcTask) {
    LOGGER.debug("Unregistering HpcTask: {}", hpcTask.getId());
        taskMap.remove(hpcTask.getId());
    }

    private String getTasksIdsAsString () {
    	TreeSet<Long> ts = new TreeSet<Long>(taskMap.keySet());
    	return "["+ts.size()+"]"+ts.toString();
    }
}
