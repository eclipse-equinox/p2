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
package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import java.io.File;
import java.util.List;
import org.apache.tools.ant.types.DataType;

public class ComparatorDescription extends DataType {

	private DestinationRepository baseline;
	private String comparatorId;
	private File comparatorLog;
	private List<ArtifactDescription> excludedArtifacts = null;

	/*
	 * Set the baseline repository to compare to
	 */
	public void addRepository(DestinationRepository value) {
		this.baseline = value;
	}

	public void addConfiguredExclude(ElementList<ArtifactDescription> excludeList) {
		excludedArtifacts = excludeList.getElements();
	}

	/*
	 * Set the comparator to use
	 */
	public void setComparator(String value) {
		comparatorId = value;
	}

	/*
	 * Set the log location for the comparator
	 */
	public void setComparatorLog(File value) {
		comparatorLog = value;
	}

	public DestinationRepository getBaseline() {
		return baseline;
	}

	public String getComparator() {
		return comparatorId;
	}

	public File getComparatorLog() {
		return comparatorLog;
	}

	public List<ArtifactDescription> getExcluded() {
		return excludedArtifacts;
	}
}
