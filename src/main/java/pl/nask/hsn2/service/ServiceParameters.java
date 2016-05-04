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

import pl.nask.hsn2.RequiredParameterMissingException;
import pl.nask.hsn2.wrappers.ParametersWrapper;

public class ServiceParameters {

    private String hpcProfile;
    private boolean savePcap;
    private boolean saveZip;
    private boolean saveLog;
    private boolean saveScreenshot;

    public ServiceParameters(ParametersWrapper parameters) throws RequiredParameterMissingException {
        hpcProfile = parameters.get("hpc_profile");
        savePcap = parameters.getBoolean("save_pcap", false);
        saveZip = parameters.getBoolean("save_zip", true);
        saveLog = parameters.getBoolean("save_log", true);
        saveScreenshot = parameters.getBoolean("save_screenshot", true);
    }

    public final String getHpcProfile() {
        return hpcProfile;
    }

    public final boolean isSavePcap() {
        return savePcap;
    }

    public final boolean isSaveZip() {
        return saveZip;
    }

    public final boolean isSaveLog() {
        return saveLog;
    }

    public final boolean isSaveScreenshot() {
        return saveScreenshot;
    }


}
