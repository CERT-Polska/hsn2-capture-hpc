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

import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.Test;

import pl.nask.hsn2.utils.IdGeneratorException;

public class TaskRegistryTest {
	@Test
	public void testStringConcatenation() throws IdGeneratorException {
		TaskRegistry tr = new TaskRegistry();
		HpcTask t = tr.registerTask(null, null);
		tr.log("" + t.getId(), "sasdf", "sfsdf", "sdfsdf");
	}
	
	@Test
	public void testSetToString() {
		Set<String> set = new HashSet<String>();
		set.add("s1");
		set.add("s2");
		System.out.println(set);
	}
}
