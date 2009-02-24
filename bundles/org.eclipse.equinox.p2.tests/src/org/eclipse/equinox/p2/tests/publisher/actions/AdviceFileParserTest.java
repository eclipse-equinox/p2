/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import java.util.*;
import junit.framework.TestCase;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.publisher.eclipse.AdviceFileParser;

public class AdviceFileParserTest extends TestCase {
	public void testNoAdvice() {
		AdviceFileParser parser = new AdviceFileParser(Collections.EMPTY_MAP);
		parser.parse();
	}

	public void testPropertyAdvice() {
		Map map = new HashMap();
		map.put("properties.testName1", "testValue1");
		map.put("properties.testName2", "testValue2");

		AdviceFileParser parser = new AdviceFileParser(map);
		parser.parse();
		assertEquals("testValue1", parser.getProperties().getProperty("testName1"));
		assertEquals("testValue2", parser.getProperties().getProperty("testName2"));
	}

	public void testProvidesAdvice() {
		Map map = new HashMap();
		map.put("provides.0.namespace", "testNamespace1");
		map.put("provides.0.name", "testName1");
		map.put("provides.0.version", "1.2.3");

		AdviceFileParser parser = new AdviceFileParser(map);
		parser.parse();
		IProvidedCapability[] capabilities = parser.getProvidedCapabilities();
		assertEquals(1, capabilities.length);
		assertEquals("testNamespace1", capabilities[0].getNamespace());
		assertEquals("testName1", capabilities[0].getName());
		assertEquals(new Version("1.2.3"), capabilities[0].getVersion());

		map.put("provides.1.namespace", "testNamespace2");
		map.put("provides.1.name", "testName2");
		map.put("provides.1.version", "1.2.4");

		parser = new AdviceFileParser(map);
		parser.parse();
		capabilities = parser.getProvidedCapabilities();
		assertEquals(2, capabilities.length);
		assertEquals("testNamespace1", capabilities[0].getNamespace());
		assertEquals("testName1", capabilities[0].getName());
		assertEquals(new Version("1.2.3"), capabilities[0].getVersion());
		assertEquals("testNamespace2", capabilities[1].getNamespace());
		assertEquals("testName2", capabilities[1].getName());
		assertEquals(new Version("1.2.4"), capabilities[1].getVersion());
	}

	public void testRequiresAdvice() {
		Map map = new HashMap();
		map.put("requires.0.namespace", "testNamespace1");
		map.put("requires.0.name", "testName1");
		map.put("requires.0.range", "1.2.3");
		map.put("requires.0.greedy", Boolean.TRUE.toString());
		map.put("requires.0.optional", Boolean.TRUE.toString());
		map.put("requires.0.multiple", Boolean.TRUE.toString());

		AdviceFileParser parser = new AdviceFileParser(map);
		parser.parse();
		IRequiredCapability[] capabilities = parser.getRequiredCapabilities();
		assertEquals(1, capabilities.length);
		assertEquals("testNamespace1", capabilities[0].getNamespace());
		assertEquals("testName1", capabilities[0].getName());
		assertEquals(new VersionRange("1.2.3"), capabilities[0].getRange());

		map.put("requires.1.namespace", "testNamespace2");
		map.put("requires.1.name", "testName2");
		map.put("requires.1.range", "1.2.4");
		map.put("requires.1.greedy", Boolean.FALSE.toString());
		map.put("requires.1.optional", Boolean.FALSE.toString());
		//default 
		//		map.put("requires.1.multiple", Boolean.FALSE.toString());

		parser = new AdviceFileParser(map);
		parser.parse();
		capabilities = parser.getRequiredCapabilities();
		assertEquals(2, capabilities.length);
		assertEquals("testNamespace1", capabilities[0].getNamespace());
		assertEquals("testName1", capabilities[0].getName());
		assertEquals(new VersionRange("1.2.3"), capabilities[0].getRange());
		assertEquals(true, capabilities[0].isGreedy());
		assertEquals(true, capabilities[0].isOptional());
		assertEquals(true, capabilities[0].isMultiple());
		assertEquals("testNamespace2", capabilities[1].getNamespace());
		assertEquals("testName2", capabilities[1].getName());
		assertEquals(new VersionRange("1.2.4"), capabilities[1].getRange());
		assertEquals(false, capabilities[1].isGreedy());
		assertEquals(false, capabilities[1].isOptional());
		assertEquals(false, capabilities[1].isMultiple());
	}

	public void testInstructionsAdvice() {
		Map map = new HashMap();
		map.put("instructions.configure", "addProgramArg(programArg:-startup); addProgramArg(programArg:@artifact);");

		map.put("instructions.unconfigure", "removeProgramArg(programArg:-startup); removeProgramArg(programArg:@artifact);)");
		map.put("instructions.unconfigure.import", "some.removeProgramArg");

		AdviceFileParser parser = new AdviceFileParser(map);
		parser.parse();
		ITouchpointInstruction configure = (ITouchpointInstruction) parser.getTouchpointInstructions().get("configure");
		assertEquals(null, configure.getImportAttribute());
		assertEquals("addProgramArg(programArg:-startup); addProgramArg(programArg:@artifact);", configure.getBody());

		ITouchpointInstruction unconfigure = (ITouchpointInstruction) parser.getTouchpointInstructions().get("unconfigure");
		assertEquals("some.removeProgramArg", unconfigure.getImportAttribute());
		assertEquals("removeProgramArg(programArg:-startup); removeProgramArg(programArg:@artifact);)", unconfigure.getBody());
	}

}
