/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.sat4j.smoke;

import java.io.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.SolverFactory;
import org.sat4j.pb.reader.OPBEclipseReader2007;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.*;

public class SmokeTestSAT4J extends AbstractProvisioningTest {

	private IProblem invokeSolver(File problemFile) throws FileNotFoundException, ParseFormatException, ContradictionException, TimeoutException {
		IPBSolver solver = SolverFactory.newEclipseP2();
		solver.setTimeoutOnConflicts(1000);
		OPBEclipseReader2007 reader = new OPBEclipseReader2007(solver);
		// CNF filename is given on the command line 
		FileReader fr = null;
		fr = new FileReader(problemFile);
		IProblem problem = reader.parseInstance(fr);
		if (problem.isSatisfiable())
			return problem;
		return null;
	}

	public void testBogusFile() {
		File data = getTestData("Opb file 247638", "testData/sat4j/Bug247638.opb");
		Exception raised = null;
		try {
			invokeSolver(data);
		} catch (FileNotFoundException e) {
			//Ignore
		} catch (ParseFormatException e) {
			raised = e;
		} catch (ContradictionException e) {
			fail("Contradiction exception", e);
		} catch (TimeoutException e) {
			fail("Timeout exception", e);
		}
		assertNotNull(raised);
	}

	public void testBug247567() {
		File data = getTestData("Opb file 247567", "testData/sat4j/Bug247567.opb");
		Exception raised = null;
		try {
			IProblem pb = invokeSolver(data);
			assertNotNull(pb);
			for (int i = 1; i <= 6; i++) {
				pb.model(i);
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			raised = e;
		} catch (FileNotFoundException e) {
			//Ignore
		} catch (ParseFormatException e) {
			raised = e;
		} catch (ContradictionException e) {
			fail("Contradiction exception", e);
		} catch (TimeoutException e) {
			fail("Timeout exception", e);
		}
		assertNull(raised);

	}
}
