/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.prov.tests;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import junit.framework.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.prov.core.helpers.OrderedProperties;
import org.eclipse.equinox.prov.core.helpers.UnmodifiableProperties;
import org.eclipse.equinox.prov.core.repository.IRepositoryInfo;
import org.eclipse.equinox.prov.metadata.IInstallableUnit;
import org.eclipse.equinox.prov.metadata.RequiredCapability;
import org.eclipse.equinox.prov.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.prov.query.CompoundIterator;
import org.eclipse.osgi.service.resolver.VersionRange;

/**
 * A simple metadata repository used for testing purposes.  All metadata
 * is kept in memory.
 */
public class TestMetadataRepository extends Assert implements IMetadataRepository {

	private static final String SCHEME = "testmetadatarepo"; //$NON-NLS-1$
	private final List units = new ArrayList();
	private URL url;

	public TestMetadataRepository(IInstallableUnit[] ius) {
		units.addAll(Arrays.asList(ius));
		try {
			url = File.createTempFile("TestMetadataRepository", Long.toString(System.currentTimeMillis())).toURL();
		} catch (MalformedURLException e) {
			fail(e.getMessage());
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

	public IInstallableUnit[] getInstallableUnits(IProgressMonitor monitor) {
		IInstallableUnit[] result = query(null, null, null, false, monitor);
		return result;
	}

	public Iterator getIterator(String id, VersionRange range, RequiredCapability[] requirements, boolean and) {
		return new CompoundIterator(new Iterator[] {units.iterator()}, id, range, requirements, and);
	}

	public URL getLocation() {
		return url;
	}

	public IInstallableUnit[] query(String id, VersionRange range, RequiredCapability[] requirements, boolean and, IProgressMonitor monitor) {
		return CompoundIterator.asArray(new CompoundIterator(new Iterator[] {units.iterator()}, id, range, requirements, and), null);
	}

	public String getDescription() {
		return "A Test Metadata Repository"; //$NON-NLS-1$;
	}

	public String getName() {
		return "ATestMetadataRepository"; //$NON-NLS-1$;
	}

	public String getProvider() {
		return "org.eclipse"; //$NON-NLS-1$
	}

	public String getType() {
		return SCHEME;
	}

	public String getVersion() {
		return "1"; //$NON-NLS-1$
	}

	public UnmodifiableProperties getProperties() {
		return new UnmodifiableProperties(new OrderedProperties());
	}

	public Object getAdapter(Class adapter) {
		if (adapter == TestMetadataRepository.class || adapter == IMetadataRepository.class || adapter == IRepositoryInfo.class) {
			return this;
		}
		return null;
	}
}
