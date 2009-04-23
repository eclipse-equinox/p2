/*******************************************************************************
 *  Copyright (c) 2009 IBM Corporation and others.
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
import org.apache.tools.ant.types.DataType;

public class ComparatorDescription extends DataType {

	DestinationRepository baseline;
	String comparatorId;
	File comparatorLog;

	/*
	 * Set the baseline repository to compare to
	 */
	public void addRepository(DestinationRepository value) {
		this.baseline = value;
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
}
