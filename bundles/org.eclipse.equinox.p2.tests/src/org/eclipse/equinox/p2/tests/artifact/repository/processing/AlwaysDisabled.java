/*******************************************************************************
* Copyright (c) 2018 Mykola Nikishov.
*
* This program and the accompanying materials
* are made available under the terms of the Eclipse Public License 2.0
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
* 	Mykola Nikishov - initial API and implementation
*******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository.processing;

import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;

public class AlwaysDisabled extends ProcessingStep {

	public AlwaysDisabled() {
		// needed
	}

	@Override
	public boolean isEnabled() {
		return false;
	}
}
