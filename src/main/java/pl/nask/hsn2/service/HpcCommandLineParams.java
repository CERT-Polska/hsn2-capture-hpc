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

import pl.nask.hsn2.CommandLineParams;

public class HpcCommandLineParams extends CommandLineParams {
    public static final String HPC_IDGEN_DEFAULT_FILENAME = "hpcIdGen.seq";
	private final static OptionNameWrapper HPC_HOST = new OptionNameWrapper("hh", "hpcHost");
    private final static OptionNameWrapper HPC_PORT = new OptionNameWrapper("hp", "hpcPort");
    private final static OptionNameWrapper OUTPUT_LOG_FILE = new OptionNameWrapper("log", "urlLogfile");
    private final static OptionNameWrapper CHANGES_DIR = new OptionNameWrapper("dir", "changesDir");
    private final static OptionNameWrapper HPC_SEQ_GENERATOR = new OptionNameWrapper("idgen", "captIdGen");
    @Override
    public void initOptions() {
        super.initOptions();
        addOption(HPC_HOST, "host", "Address of the host with Capture HPC");
        addOption(HPC_PORT, "port", "Port number on which Capture HPC is running");
        addOption(OUTPUT_LOG_FILE, "file", "Path to the logfile with the results of URL processing");
        addOption(CHANGES_DIR, "dir", "Path to the directory with the changes logged when processing URL");
        addOption(HPC_SEQ_GENERATOR, "idgen", "Path to the sequence file for new capture's task");
    }

    public String getHpcHost() {
        return getOptionValue(HPC_HOST);
    }

    public int getHpcPort() {
        return getOptionIntValue(HPC_PORT);
    }

    public String getLogFileName() {
        return getOptionValue(OUTPUT_LOG_FILE);
    }

    public String getChangesDirName() {
        return getOptionValue(CHANGES_DIR);
    }
    
    public String getHpcFileIdGen() {
    	return getOptionValue(HPC_SEQ_GENERATOR);
    }
    @Override
    protected void initDefaults() {
    	super.initDefaults();
    	setDefaultValue(HPC_SEQ_GENERATOR.getName(), HPC_IDGEN_DEFAULT_FILENAME);
    	setDefaultServiceNameAndQueueName("capture");
    }

}
