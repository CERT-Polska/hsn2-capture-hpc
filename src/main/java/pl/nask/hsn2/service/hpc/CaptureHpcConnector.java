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

public interface CaptureHpcConnector {

    /**
     * Sends 'exit' command
     * @throws IOException
     */
    public void exit() throws IOException;

    /**
     * Sends 'reload' command
     * @throws IOException
     */
    public void reload() throws IOException;

    /**
     * Sends addurl command with 2 parameters: url and urlId.
     * @param url
     * @param urlId
     * @throws IOException
     */
    public void addUrl(String url, long urlId) throws IOException;

    public String getConnectorInfo();
}