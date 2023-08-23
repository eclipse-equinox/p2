/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
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
package org.eclipse.equinox.p2.tests.artifact.repository;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Performs all automated artifact repository tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
		ArtifactLockingTest.class, ArtifactOutputStreamTest.class, ArtifactRepositoryManagerTest.class,
		ArtifactRepositoryMissingSizeData.class, ArtifactRepositoryWithReferenceDescriptors.class,
		BatchExecuteArtifactRepositoryTest.class, Bug252308.class, Bug265577.class, Bug351944.class,
		CompositeArtifactRepositoryTest.class, CorruptedJar.class, FoldersRepositoryTest.class,
		JarURLArtifactRepositoryTest.class, MD5Tests.class, MirrorSelectorTest.class,
		MirrorRequestTest.class, SimpleArtifactRepositoryTest.class, TransferTest.class, PGPVerifierTest.class
})
public class AllTests {
// test suite
}