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
package org.eclipse.equinox.internal.p2.tools;

import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;

public class MetadataCompareApplication implements IApplication {

	private MetadataRepositoryManager repoManager = new MetadataRepositoryManager();
	private IMetadataRepository sourceRepo = null;
	private IMetadataRepository targetRepo = null;

	private static Comparator iuIdComparator = new Comparator() {
		public int compare(Object source, Object target) {
			IInstallableUnit sourceIU = (IInstallableUnit) source;
			IInstallableUnit targetIU = (IInstallableUnit) target;
			int id = sourceIU.getId().compareTo(targetIU.getId());
			if (id != 0)
				return id;
			return sourceIU.getVersion().compareTo(targetIU.getVersion());
		}
	};

	private URI sourceLocation;
	private URI targetLocation;
	private boolean compare = false;
	private boolean list = false;

	public Object start(IApplicationContext context) throws Exception {
		initializeFromArguments((String[]) context.getArguments().get("application.args")); //$NON-NLS-1$
		initRepositories();

		if (compare) {
			compareMetadataRepositories();
		} else if (list) {
			list(sourceLocation);
			list(targetLocation);
		}
		return IApplication.EXIT_OK;
	}

	private void list(URI location) throws ProvisionException {
		if (location == null)
			return;
		IMetadataRepository locationRepo = repoManager.getRepository(location);
		if (locationRepo == null)
			return;
		Collector sourceRoots = locationRepo.query(InstallableUnitQuery.ANY, new Collector(), new NullProgressMonitor());
		IInstallableUnit[] sourceIUs = (IInstallableUnit[]) sourceRoots.toArray(IInstallableUnit.class);

		sourceIUs = sort(sourceIUs, true);
		for (int i = 0; i < sourceIUs.length; i++) {
			System.out.print(sourceIUs[i]);
			System.out.println(sourceIUs[i].isFragment() ? " (fragment)" : ""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		System.out.println("Total: " + sourceIUs.length); //$NON-NLS-1$
	}

	private void compareMetadataRepositories() throws ProvisionException {
		System.out.println("\n" + sourceLocation + " -> " + targetLocation); //$NON-NLS-1$ //$NON-NLS-2$
		compare(sourceRepo, targetRepo);
	}

	private void initRepositories() throws ProvisionException {
		if (targetLocation == null || sourceLocation == null)
			throw new IllegalStateException("Must specify a source and target"); //$NON-NLS-1$
		sourceRepo = repoManager.loadRepository(sourceLocation, null);
		targetRepo = initializeTarget();
	}

	private IMetadataRepository initializeTarget() throws ProvisionException {
		try {
			IMetadataRepository repository = repoManager.loadRepository(targetLocation, null);
			if (!repository.isModifiable())
				throw new IllegalArgumentException("Metadata repository not modifiable: " + targetLocation); //$NON-NLS-1$
			return repository;
		} catch (ProvisionException e) {
			//fall through and create repo
		}
		String repositoryName = targetLocation + " - metadata"; //$NON-NLS-1$
		return repoManager.createRepository(targetLocation, repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
	}

	private void compare(IMetadataRepository sourceRepo, IMetadataRepository targetRepo) {
		Collector sourceRoots = sourceRepo.query(InstallableUnitQuery.ANY, new Collector(), new NullProgressMonitor());
		Collector targetRoots = targetRepo.query(InstallableUnitQuery.ANY, new Collector(), new NullProgressMonitor());
		IInstallableUnit[] sourceIUs = (IInstallableUnit[]) sourceRoots.toArray(IInstallableUnit.class);
		sourceIUs = sort(sourceIUs, true);
		IInstallableUnit[] targetIUs = (IInstallableUnit[]) targetRoots.toArray(IInstallableUnit.class);
		targetIUs = sort(targetIUs, true);

		int targetIndex = 0;
		for (int i = 0; i < sourceIUs.length; i++)
			targetIndex = compareUsingTargets(sourceIUs[i], targetIUs, targetIndex);
	}

	private int compareUsingTargets(IInstallableUnit sourceIU, IInstallableUnit[] targetIUs, int targetIndex) {
		while (targetIndex < targetIUs.length) {
			int difference = iuIdComparator.compare(sourceIU, targetIUs[targetIndex]);
			if (difference < 0) {
				System.out.println(sourceIU + " is not found in target repository"); //$NON-NLS-1$
				return targetIndex;
			} else if (difference == 0) {
				String comparison = compare(sourceIU, targetIUs[targetIndex]);
				if (comparison.length() > 0)
					System.out.println(sourceIU + comparison);
				return targetIndex + 1;
			} else {
				System.out.println(targetIUs[targetIndex++] + " is not found in source repository"); //$NON-NLS-1$
			}
		}
		System.out.println(sourceIU + " is not found in target repository"); //$NON-NLS-1$
		return targetIndex;
	}

	private boolean compare(Object a, Object b) {
		if (a == null)
			return b == null;
		return a.equals(b);
	}

	private boolean compare(Object[] a, Object b[]) {
		if (a == null)
			return b == null;
		return Arrays.equals(a, b);
	}

	private String compare(IInstallableUnit iu, IInstallableUnit next) {
		if (next == null)
			return " iu artifactLocators providedCapabilities requiredCapabilities touchpointType"; //$NON-NLS-1$
		String result = ""; //$NON-NLS-1$
		if (!iu.equals(next))
			result += " iu"; //$NON-NLS-1$
		//		if (!compare(iu.getApplicabilityFilter(), next.getApplicabilityFilter()))
		//			result += " applicabilityFilter";
		if (!compare(iu.getArtifacts(), next.getArtifacts()))
			result += " artifactLocators"; //$NON-NLS-1$
		if (!compare(iu.getProvidedCapabilities(), next.getProvidedCapabilities()))
			result += " providedCapabilities"; //$NON-NLS-1$
		if (!compareRequires(iu.getRequiredCapabilities(), next.getRequiredCapabilities()))
			result += " requiredCapabilities"; //$NON-NLS-1$
		if (!compare(iu.getTouchpointType(), next.getTouchpointType()))
			result += " touchpointType"; //$NON-NLS-1$

		if (iu.isFragment()) {
			//			if (((InstallableUnitFragment) iu).getHost() == null || ((InstallableUnitFragment) iu).getVersion() == null)
			//				return result;
			//			if (!((InstallableUnitFragment) iu).getHost().equals(((InstallableUnitFragment) next).getHost()))
			//				result += " hostid";
			//			if (!((InstallableUnitFragment) iu).getVersion().equals(((InstallableUnitFragment) next).getVersion()))
			//				result += " hostversionRange";
		}
		return result;
	}

	private boolean compareRequires(IRequiredCapability[] a, IRequiredCapability[] b) {
		if (a == null)
			return b == null;
		if (a.length != b.length)
			return false;
		if (a == b)
			return true;
		for (int i = 0; i < a.length; i++)
			if (findCapability(a[i], b) == null)
				return false;
		return true;
	}

	private IRequiredCapability findCapability(IRequiredCapability target, IRequiredCapability[] b) {
		for (int i = 0; i < b.length; i++) {
			IRequiredCapability capability = b[i];
			if (target.equals(capability))
				return capability;
		}
		return null;
	}

	private IInstallableUnit[] sort(IInstallableUnit[] ius, boolean clone) {
		IInstallableUnit[] result = ius;
		if (clone) {
			result = new InstallableUnit[ius.length];
			System.arraycopy(ius, 0, result, 0, ius.length);
		}
		Arrays.sort(result, iuIdComparator);
		return result;
	}

	public void initializeFromArguments(String[] args) throws Exception {
		if (args == null)
			return;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-compare")) //$NON-NLS-1$
				compare = true;
			if (args[i].equalsIgnoreCase("-list")) //$NON-NLS-1$
				list = true;

			// check for args with parameters. If we are at the last argument or
			// if the next one
			// has a '-' as the first character, then we can't have an arg with
			// a parm so continue.
			if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
				continue;

			String arg = args[++i];

			if (args[i - 1].equalsIgnoreCase("-source")) //$NON-NLS-1$
				sourceLocation = new URI(arg);
			if (args[i - 1].equalsIgnoreCase("-target")) //$NON-NLS-1$
				targetLocation = new URI(arg);
		}
	}

	public void stop() {
		//do nothing
	}
}
