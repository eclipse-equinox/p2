/*******************************************************************************
 *  Copyright (c) 2009, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.*;
import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.persistence.XMLWriter;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.ArtifactKeyQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;

public class AbstractAntProvisioningTest extends AbstractProvisioningTest {
	protected static final String TYPE_ARTIFACT = "A";
	protected static final String TYPE_METADATA = "M";
	protected static final String TYPE_BOTH = null;

	private static final String TARGET = "target";
	private static final String ROOT = "project";
	private static final String NAME = "name";
	private static final String DEFAULT_TARGET = "default";
	private static final String DEFAULT_NAME = "default";

	AntTaskElement root, target;
	File buildScript, logLocation;

	public void setUp() throws Exception {
		super.setUp();
		buildScript = new File(getTestFolder(getName()), "build_" + getName() + ".xml");
		logLocation = new File(getTestFolder(getName()), "log_" + getName() + ".log");
		createBuildScript();
	}

	public void tearDown() throws Exception {
		// Delete the build script
		delete(buildScript.getParentFile());
		delete(logLocation.getParentFile());
		super.tearDown();
	}

	/*
	 * Run the specified buildscript
	 */
	protected void runAntTask(File buildFile) {
		try {
			runAntTaskWithExceptions(buildFile);
		} catch (CoreException e) {
			fail(rootCause(e));
		}
	}

	private void runAntTaskWithExceptions(File buildFile) throws CoreException {
		AntRunner ant = new AntRunner();
		ant.setArguments("-logfile \"" + logLocation + "\"");
		ant.setBuildFileLocation(buildFile.getAbsolutePath());
		ant.addBuildLogger("org.apache.tools.ant.XmlLogger");
		ant.run();
	}

	/*
	 * Run the build script described programmatically
	 */
	protected void runAntTask() {
		try {
			runAntTaskWithExceptions();
		} catch (CoreException e) {
			fail(rootCause(e));
		}
	}

	protected void runAntTaskWithExceptions() throws CoreException {
		try {
			writeBuildScript();
		} catch (Exception e) {
			fail("Error writing build script", e);
		}
		runAntTaskWithExceptions(buildScript);
	}

	/*
	 * Adds an Ant Task to the build script
	 */
	protected void addTask(AntTaskElement task) {
		target.addElement(task);
	}

	/*
	 * Create and return an repository element for this address and type
	 */
	protected AntTaskElement getRepositoryElement(URI address, String kind) {
		return getRepositoryElement(address, kind, null, null, null, null);
	}

	protected AntTaskElement getIUElement(String name, String version) {
		AntTaskElement iuElement = new AntTaskElement("iu");
		iuElement.addAttribute("id", name);
		if (version != null)
			iuElement.addAttribute("version", version);
		return iuElement;
	}

	protected AntTaskElement getRepositoryElement(URI address, String kind, String name, String format, Boolean compressed, Boolean append) {
		AntTaskElement repo = new AntTaskElement("repository");
		repo.addAttributes(new String[] {"location", URIUtil.toUnencodedString(address)});
		if (kind != null)
			repo.addAttributes(new String[] {"kind", kind});
		if (name != null)
			repo.addAttributes(new String[] {"name", name});
		if (format != null)
			repo.addAttributes(new String[] {"format", format});
		if (compressed != null)
			repo.addAttributes(new String[] {"compressed", compressed.toString()});
		if (append != null)
			repo.addAttributes(new String[] {"append", append.toString()});
		return repo;
	}

	/*
	 * Create an element from the specified information
	 */
	protected AntTaskElement createIUElement(IInstallableUnit iu) {
		return createIUElement(iu.getId(), iu.getVersion().toString());
	}

	/*
	 * Create an element from the specified information
	 */
	protected AntTaskElement createIUElement(String id, String version) {
		AntTaskElement iuElement = new AntTaskElement("iu");
		iuElement.addAttribute("id", id);
		iuElement.addAttribute("version", version);
		return iuElement;
	}

	/*
	 * Create the base elements of the build script
	 */
	private void createBuildScript() {
		root = new AntTaskElement(ROOT);
		root.addAttributes(new String[] {NAME, ROOT, DEFAULT_TARGET, DEFAULT_NAME});
		target = new AntTaskElement(TARGET);
		target.addAttributes(new String[] {NAME, DEFAULT_NAME});
		root.addElement(target);
	}

	/*
	 * Write the build script to disk
	 */
	private void writeBuildScript() throws Exception {
		FileOutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream(buildScript);
			XMLWriter writer = new XMLWriter(outputStream, null);
			writeElement(writer, root);
			writer.flush();
		} finally {
			if (outputStream != null)
				outputStream.close();
		}
	}

	/*
	 * Write an element to the buildscript
	 */
	private void writeElement(XMLWriter writer, AntTaskElement task) {
		// Properties ought to occur in key-value pairs 
		assertTrue("Task " + task + " should have an even number of properties", (task.attributes.size() % 2) == 0);

		// Start tag
		writer.start(task.name);

		// write properties
		for (Iterator iter = task.attributes.iterator(); iter.hasNext();)
			writer.attribute((String) iter.next(), (String) iter.next());

		// write sub elements if applicable
		for (Iterator iter = task.elements.iterator(); iter.hasNext();)
			writeElement(writer, (AntTaskElement) iter.next());

		// close tag
		writer.end();
	}

	// Class which can be used to represent elements in a task
	protected class AntTaskElement {
		public String name;
		public List attributes = new ArrayList();
		public List elements = new ArrayList();

		public AntTaskElement(String name) {
			this.name = name;
		}

		public AntTaskElement(String name, String[] attributes) {
			this.name = name;
			if (attributes != null && attributes.length > 0) {
				if (attributes.length % 2 != 0)
					throw new IllegalStateException();
				this.attributes.addAll(Arrays.asList(attributes));
			}
		}

		public void addAttribute(String attribute, String value) {
			attributes.add(attribute);
			attributes.add(value);
		}

		public void addAttributes(String[] propertyArray) {
			attributes.addAll(Arrays.asList(propertyArray));
		}

		public void addElement(AntTaskElement element) {
			elements.add(element);
		}

		public String toString() {
			return name;
		}
	}

	protected static Throwable rootCause(Throwable e) {
		if (e.getCause() != null)
			return rootCause(e.getCause());
		return e;
	}

	protected static void fail(Throwable e) {
		fail("An exception occurred while running the task", e);
	}

	protected void assertLogContains(String content) {
		try {
			assertLogContainsLine(logLocation, content);
		} catch (Exception e) {
			fail("Error asserting log contents.", e);
		}
	}

	protected static void assertIUContentEquals(String message, IQueryable source, IQueryable destination) {
		assertContains(message, source, destination);
		assertContains(message, destination, source);
	}

	protected void assertArtifactKeyContentEquals(String message, IQueryResult ius, URI artifactRepositoryLocation) {
		try {
			IArtifactRepository repo = getArtifactRepositoryManager().loadRepository(artifactRepositoryLocation, null);
			List fromIUs = getArtifactKeys(ius);
			Iterator fromRepo = repo.query(ArtifactKeyQuery.ALL_KEYS, null).iterator();
			assertContains(message, fromIUs, fromRepo);
			assertContains(message, fromRepo, fromIUs);
		} catch (ProvisionException e) {
			fail("Failed to load repository", e);
		}

	}

	protected static List getArtifactKeys(IQueryResult<IInstallableUnit> ius) {
		List<IArtifactKey> keys = new ArrayList<IArtifactKey>();

		for (Iterator<IInstallableUnit> iter = ius.iterator(); iter.hasNext();)
			keys.addAll(iter.next().getArtifacts());
		return keys;
	}
}
