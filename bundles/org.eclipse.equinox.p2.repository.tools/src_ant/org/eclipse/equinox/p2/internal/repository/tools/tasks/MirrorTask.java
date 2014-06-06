/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.Messages;
import org.eclipse.equinox.p2.internal.repository.tools.MirrorApplication;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

public class MirrorTask extends AbstractRepositoryTask {

	private File mirrorLog; // file to log mirror output to (optional)
	private ComparatorDescription comparator;
	private boolean ignoreErrors = false;

	public MirrorTask() {
		application = new MirrorApplication();
	}

	public void execute() throws BuildException {
		try {
			if (mirrorLog != null)
				((MirrorApplication) application).setLog(mirrorLog);
			else
				((MirrorApplication) application).setLog(new AntMirrorLog(this));

			if (comparator != null) {
				// Enable comparison
				((MirrorApplication) application).setCompare(true);
				// Set baseline location
				if (comparator.getBaseline() != null)
					((MirrorApplication) application).setBaseline(comparator.getBaseline().getDescriptor().getRepoLocation());
				// Set comparator to use
				if (comparator.getComparator() != null)
					((MirrorApplication) application).setComparatorID(comparator.getComparator());
				// Set comparator log
				if (comparator.getComparatorLog() != null)
					((MirrorApplication) application).setComparatorLog(comparator.getComparatorLog());
				((MirrorApplication) application).setComparatorExclusions(createCompareExclusions());
			}

			prepareSourceRepos();
			application.initializeRepos(null);
			List<IInstallableUnit> ius = prepareIUs();
			application.setSourceIUs(ius);
			IStatus result = application.run(null);
			if (!ignoreErrors && result.matches(IStatus.ERROR))
				throw new BuildException(TaskHelper.statusToString(result, IStatus.ERROR, null).toString());
		} catch (ProvisionException e) {
			throw new BuildException(e);
		} catch (NoSuchMethodException e) {
			// Should not occur
			throw new BuildException(e);
		}
	}

	private IQuery<IArtifactDescriptor> createCompareExclusions() {
		if (comparator == null || comparator.getExcluded() == null)
			return null;

		List<ArtifactDescription> artifacts = comparator.getExcluded();
		List<IQuery<IArtifactDescriptor>> queries = new ArrayList<IQuery<IArtifactDescriptor>>();
		for (ArtifactDescription artifactDescription : artifacts)
			queries.add(artifactDescription.createDescriptorQuery());

		if (queries.size() == 1)
			return queries.get(0);

		return QueryUtil.<IArtifactDescriptor> createCompoundQuery(queries, false);
	}

	public SlicingOption createSlicingOptions() {
		SlicingOption options = new SlicingOption();
		((MirrorApplication) application).setSlicingOptions(options.getOptions());
		return options;
	}

	/*
	 * Set the comparison information
	 */
	public ComparatorDescription createComparator() {
		if (comparator != null)
			throw new BuildException(Messages.exception_onlyOneComparator);
		comparator = new ComparatorDescription();
		return comparator;
	}

	/*
	 * Set the location of the mirror log
	 */
	public void setLog(String value) {
		mirrorLog = new File(value);
	}

	/*
	 * Set whether or not we should ignore errors when running the mirror application.
	 */
	public void setIgnoreErrors(boolean value) {
		ignoreErrors = value;
		((MirrorApplication) application).setIgnoreErrors(value);
	}

	/*
	 * Set whether or not the the artifacts are raw.
	 */
	public void setRaw(boolean value) {
		((MirrorApplication) application).setRaw(value);
	}

	/*
	 * Set whether or not the mirror application should be run in verbose mode.
	 */
	public void setVerbose(boolean value) {
		((MirrorApplication) application).setVerbose(value);
	}

	public void setValidate(boolean value) {
		((MirrorApplication) application).setValidate(value);
	}

	public void setIncludePacked(boolean value) {
		((MirrorApplication) application).setIncludePacked(value);
	}

	public void setReferences(boolean value) {
		((MirrorApplication) application).setReferences(value);
	}

	public void setMirrorProperties(boolean value) {
		((MirrorApplication) application).setMirrorProperties(value);
	}
}
