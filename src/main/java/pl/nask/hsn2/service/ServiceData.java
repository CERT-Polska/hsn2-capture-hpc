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

import pl.nask.hsn2.wrappers.ObjectDataWrapper;

public class ServiceData {
    private long id;
    private String inputUrlNormalized;
    private String inputUrlOriginal;

    public ServiceData(ObjectDataWrapper data) {
        id = data.getId();
        inputUrlNormalized = data.getString("url_normalized");
        inputUrlOriginal = data.getString("url_original");
    }

    public final String getUrlForProcessing() {
        return inputUrlOriginal != null ? inputUrlOriginal : inputUrlNormalized;
    }

    public final long getId() {
        return id;
    }
}
