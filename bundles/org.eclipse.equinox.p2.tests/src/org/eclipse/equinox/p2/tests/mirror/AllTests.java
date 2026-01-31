/*******************************************************************************
 *  Copyright (c) 2008, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.mirror;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Performs all automated mirror repository tests.
 */
@Suite
@SelectClasses({
		ArtifactMirrorApplicationTest.class, MetadataMirrorApplicationTest.class, ArtifactRepositoryCleanupTest.class,
		MetadataRepositoryCleanupTest.class, NewMirrorApplicationArtifactTest.class,
		NewMirrorApplicationMetadataTest.class, MirrorApplicationTest.class
})
public class AllTests {
// test suite
}