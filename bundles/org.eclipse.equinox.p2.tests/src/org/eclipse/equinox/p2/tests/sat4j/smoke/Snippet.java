package org.eclipse.equinox.p2.tests.sat4j.smoke;

import java.io.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.SolverFactory;
import org.sat4j.pb.reader.OPBEclipseReader2007;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.*;

public class Snippet extends AbstractProvisioningTest {

	private void invokeSolver(File problemFile) throws FileNotFoundException, ParseFormatException, ContradictionException, TimeoutException {

		IPBSolver solver = SolverFactory.newEclipseP2();
		solver.setTimeoutOnConflicts(1000);
		OPBEclipseReader2007 reader = new OPBEclipseReader2007(solver);
		// CNF filename is given on the command line 
		FileReader fr = null;
		fr = new FileReader(problemFile);
		IProblem problem = reader.parseInstance(fr);
		if (problem.isSatisfiable()) {
			System.out.println(reader.decode(problem.model()));
		} else {
			System.out.println("Unsatisfiable !"); //$NON-NLS-1$
		}
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
}
