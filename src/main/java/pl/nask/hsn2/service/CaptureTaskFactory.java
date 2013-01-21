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

import pl.nask.hsn2.ParameterException;
import pl.nask.hsn2.TaskContext;
import pl.nask.hsn2.service.hpc.CaptureHpcConnector;
import pl.nask.hsn2.service.hpc.TaskRegistry;
import pl.nask.hsn2.task.Task;
import pl.nask.hsn2.task.TaskFactory;
import pl.nask.hsn2.wrappers.ObjectDataWrapper;
import pl.nask.hsn2.wrappers.ParametersWrapper;

public class CaptureTaskFactory implements TaskFactory {

    private final CaptureHpcConnector captureHpcConnector;
    private final TaskRegistry taskRegistry;
    private final String changesDir;

    public CaptureTaskFactory(CaptureHpcConnector hpcConnector, TaskRegistry taskRegistry, String changesDir) {
        captureHpcConnector = hpcConnector;
        this.taskRegistry = taskRegistry;
        this.changesDir = changesDir;
    }

    @Override
    public Task newTask(TaskContext jobContext, ParametersWrapper parameters, ObjectDataWrapper data) throws ParameterException {

        FilesHandler fh = new FilesHandler(changesDir, "" +data.getId(), 4);
        ServiceParameters serviceParams = new ServiceParameters(parameters);
        ServiceData serviceData = new ServiceData(data);

        return new CaptureTask(jobContext, serviceParams , serviceData , captureHpcConnector, taskRegistry, fh);
    }

}