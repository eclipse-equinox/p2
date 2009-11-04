/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * 
 */
package org.eclipse.equinox.p2.operations;

import org.eclipse.equinox.internal.p2.operations.Messages;
import org.eclipse.equinox.internal.provisional.p2.engine.Phase;
import org.eclipse.equinox.internal.provisional.p2.engine.PhaseSet;
import org.eclipse.equinox.internal.provisional.p2.engine.phases.Sizing;

public class SizingPhaseSet extends PhaseSet {

	/**
	 * Indicates that the size is currently unknown
	 */
	public static final long SIZE_UNKNOWN = -1L;

	/**
	 * Indicates that the size is unavailable (an
	 * attempt was made to compute size but it failed)
	 */
	public static final long SIZE_UNAVAILABLE = -2L;

	/**
	 * Indicates that there was nothing to size (there
	 * was no valid plan that could be used to compute
	 * size).
	 */
	public static final long SIZE_NOTAPPLICABLE = -3L;
	private static Sizing sizing;

	public SizingPhaseSet() {
		super(new Phase[] {sizing = new Sizing(100, Messages.SizingPhaseSet_PhaseSetName)});
	}

	public Sizing getSizing() {
		return sizing;
	}
}
