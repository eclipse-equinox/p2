/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Mykola Nikishov - extract MD5 checksum calculation
 *******************************************************************************/

package org.eclipse.equinox.p2.internal.repository.tools;

import java.io.File;
import org.eclipse.equinox.internal.p2.repository.helpers.ChecksumProducer;

public class RepositoryUtilities {

	public static String computeMD5(File file) {
		return ChecksumProducer.computeMD5(file);
	}
}
