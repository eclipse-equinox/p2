/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import junit.framework.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.query.CompoundIterator;
import org.eclipse.equinox.spi.p2.metadata.repository.AbstractMetadataRepository;
import org.eclipse.osgi.service.resolver.VersionRange;

/**
 * A simple metadata repository used for testing purposes.  All metadata
 * is kept in memory.
 */
public class TestMetadataRepository extends AbstractMetadataRepository {

	private static final String NAME = "ATestMetadataRepository"; //$NON-NLS-1$
	private static final String TYPE = "testmetadatarepo"; //$NON-NLS-1$
	private static final String VERSION = "1"; //$NON-NLS-1$
	private static final String PROVIDER = "org.eclipse"; //$NON-NLS-1$
	private static final String DESCRIPTION = "A Test Metadata Repository"; //$NON-NLS-1$
	private final List units = new ArrayList();

	public TestMetadataRepository(IInstallableUnit[] ius) {
		super(NAME, TYPE, VERSION, createLocation(), DESCRIPTION, PROVIDER);
		units.addAll(Arrays.asList(ius));
	}

	private static URL createLocation() {
		try {
			return File.createTempFile("TestMetadataRepository", Long.toString(System.currentTimeMillis())).toURL();
		} catch (MalformedURLException e) {
			Assert.fail(e.getMessage());
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		}
		return null;
	}

	public IInstallableUnit[] getInstallableUnits(IProgressMonitor monitor) {
		IInstallableUnit[] result = query(null, null, null, false, monitor);
		return result;
	}

	public Iterator getIterator(String id, VersionRange range, RequiredCapability[] requirements, boolean and) {
		return new CompoundIterator(new Iterator[] {units.iterator()}, id, range, requirements, and);
	}

	public IInstallableUnit[] query(String id, VersionRange range, RequiredCapability[] requirements, boolean and, IProgressMonitor monitor) {
		return CompoundIterator.asArray(new CompoundIterator(new Iterator[] {units.iterator()}, id, range, requirements, and), null);
	}

	public Object getAdapter(Class adapter) {
		if (adapter == TestMetadataRepository.class || adapter == IMetadataRepository.class || adapter == IRepository.class) {
			return this;
		}
		return null;
	}
}
