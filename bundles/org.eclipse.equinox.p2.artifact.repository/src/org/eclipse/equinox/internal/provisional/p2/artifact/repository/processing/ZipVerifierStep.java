/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing;

import java.io.IOException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;

public class ZipVerifierStep extends ProcessingStep {
	static final int[] ZIP_HEADER = new int[] {0x50, 0x4b, 0x03, 0x04};

	private int valid = 0; //-1 indicates that it is not a zip, >3 indicates that we are done the verification 

	public void write(int b) throws IOException {
		getDestination().write(b);
		if (valid > 3)
			return;
		if (valid == -1) {
			return;
		}
		if (b != ZIP_HEADER[valid++]) {
			valid = -1;
			setStatus(new Status(IStatus.ERROR, Activator.ID, Messages.ZipVerifierStep_invalid_archive));
			return;
		}
	}

	public void close() throws IOException {
		if (valid > 3) {
			setStatus(Status.OK_STATUS);
		} else {
			setStatus(new Status(IStatus.ERROR, Activator.ID, Messages.ZipVerifierStep_invalid_archive));
		}
		super.close();
	}
}
