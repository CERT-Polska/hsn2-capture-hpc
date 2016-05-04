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

package pl.nask.hsn2.service;

import pl.nask.hsn2.CommandLineParams;

public class HpcCommandLineParams extends CommandLineParams {
    public static final String HPC_IDGEN_DEFAULT_FILENAME = "hpcIdGen.seq";
	private static final OptionNameWrapper HPC_HOST = new OptionNameWrapper("hh", "hpcHost");
    private static final OptionNameWrapper HPC_PORT = new OptionNameWrapper("hp", "hpcPort");
    private static final OptionNameWrapper OUTPUT_LOG_FILE = new OptionNameWrapper("log", "urlLogfile");
    private static final OptionNameWrapper CHANGES_DIR = new OptionNameWrapper("dir", "changesDir");
    private static final OptionNameWrapper HPC_SEQ_GENERATOR = new OptionNameWrapper("idgen", "captIdGen");

    @Override
    public final void initOptions() {
        super.initOptions();
        addOption(HPC_HOST, "host", "Address of the host with Capture HPC");
        addOption(HPC_PORT, "port", "Port number on which Capture HPC is running");
        addOption(OUTPUT_LOG_FILE, "file", "Path to the logfile with the results of URL processing");
        addOption(CHANGES_DIR, "dir", "Path to the directory with the changes logged when processing URL");
        addOption(HPC_SEQ_GENERATOR, "idgen", "Path to the sequence file for new capture's task");
    }

    public final String getHpcHost() {
        return getOptionValue(HPC_HOST);
    }

    public final int getHpcPort() {
        return getOptionIntValue(HPC_PORT);
    }

    public final String getLogFileName() {
        return getOptionValue(OUTPUT_LOG_FILE);
    }

    public final String getChangesDirName() {
        return getOptionValue(CHANGES_DIR);
    }

    public final String getHpcFileIdGen() {
    	return getOptionValue(HPC_SEQ_GENERATOR);
    }
    @Override
    protected final void initDefaults() {
    	super.initDefaults();
    	setDefaultValue(HPC_SEQ_GENERATOR.getName(), HPC_IDGEN_DEFAULT_FILENAME);
    	setDefaultServiceNameAndQueueName("capture");
    }

}
