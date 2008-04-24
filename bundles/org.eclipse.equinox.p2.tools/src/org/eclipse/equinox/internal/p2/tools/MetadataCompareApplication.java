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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.console.ProvisioningHelper;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IUPropertyQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.query.*;

public class MetadataCompareApplication implements IApplication {

	private String type = "meta";
	private boolean compare;
	private boolean list;
	private String sourceLocation;
	private String targetLocation;

	public Object start(IApplicationContext context) throws Exception {
		initializeFromArguments((String[]) context.getArguments().get("application.args"));

		if (compare)
			compareMetadataRepositories(new URL(sourceLocation), new URL(targetLocation));
		if (list) {
			list(sourceLocation);
			list(targetLocation);
		}
		return IApplication.EXIT_OK;
	}

	private void list(String sourceLocation) throws ProvisionException {
		if (sourceLocation == null)
			return;
		IQueryable source = null;
		if (type.equals("meta")) {
			URL location;
			try {
				location = new URL(sourceLocation);
			} catch (MalformedURLException e) {
				return;
			}
			ProvisioningHelper.addMetadataRepository(location);
			source = ProvisioningHelper.getMetadataRepository(location);
			if (source == null)
				return;
		} else if (type.equals("profile")) {
			source = ProvisioningHelper.getProfile(sourceLocation);
			if (source == null)
				return;
		}
		Collector sourceIUs = source.query(new InstallableUnitQuery(null), new Collector(), null);
		sourceIUs = sort(sourceIUs, true);
		for (int i = 0; i < sourceIUs.length; i++) {
			System.out.print(sourceIUs[i]);
			System.out.println(sourceIUs[i].isFragment() ? " (fragment)" : "");
		}
		System.out.println("Total: " + sourceIUs.length);
	}

	private void compareMetadataRepositories(URL sourceLocation, URL targetLocation) throws ProvisionException {
		ProvisioningHelper.addMetadataRepository(sourceLocation);
		IMetadataRepository sourceRepo = ProvisioningHelper.getMetadataRepository(sourceLocation);
		if (sourceRepo == null)
			return;
		ProvisioningHelper.addMetadataRepository(targetLocation);
		IMetadataRepository targetRepo = ProvisioningHelper.getMetadataRepository(targetLocation);
		if (targetRepo == null)
			return;

		System.out.println("\n" + sourceLocation.toExternalForm() + " -> " + targetLocation.toExternalForm());
		compare(sourceRepo, targetRepo);

		System.out.println("\n" + targetLocation.toExternalForm() + " -> " + sourceLocation.toExternalForm());
		compare(targetRepo, sourceRepo);
	}

	private void compare(IQueryable source, IQueryable target) {
		Query query = new IUPropertyQuery(IInstallableUnit.PROP_TYPE_GROUP, Boolean.TRUE.toString());
		Collector collector = source.query(query, new Collector(), null);
		IInstallableUnit[] ius = (IInstallableUnit[]) collector.toArray(IInstallableUnit.class);
		ius = sort(ius, true);
		for (int i = 0; i < ius.length; i++) {
			IInstallableUnit iu = ius[i];
			Iterator result = target.query(new InstallableUnitQuery(iu.getId()), new Collector(), null).iterator();
			if (!result.hasNext())
				System.out.println(iu);
			else {
				String comparison = compare(iu, (IInstallableUnit) result.next());
				if (comparison.length() > 0)
					System.out.println(iu + comparison);
			}
		}
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
		String result = "";
		if (!iu.equals(next))
			result += " iu";
		if (!compare(iu.getFilter(), next.getFilter()))
			result += " applicabilityFilter";
		if (!compare(iu.getArtifacts(), next.getArtifacts()))
			result += " artifactLocators";
		if (!compare(iu.getProvidedCapabilities(), next.getProvidedCapabilities()))
			result += " providedCapabilities";
		if (!compareRequires(iu.getRequiredCapabilities(), next.getRequiredCapabilities()))
			result += " requiredCapabilities";
		if (!compare(iu.getTouchpointType(), next.getTouchpointType()))
			result += " touchpointType";

		if (iu.isFragment()) {
			if (!next.isFragment())
				result += "host/fragment";
			RequiredCapability[] thisHost = ((IInstallableUnitFragment) iu).getHost();
			RequiredCapability[] nextHost = ((IInstallableUnitFragment) next).getHost();
			if (!thisHost.equals(nextHost))
				result += "hostRequirement";
		}
		return result;
	}

	private boolean compareRequires(RequiredCapability[] a, RequiredCapability[] b) {
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

	private RequiredCapability findCapability(RequiredCapability target, RequiredCapability[] b) {
		for (int i = 0; i < b.length; i++) {
			RequiredCapability capability = b[i];
			if (target.equals(capability))
				return capability;
		}
		return null;
	}

	private IInstallableUnit[] sort(IInstallableUnit[] ius, boolean clone) {
		Comparator comparator = new Comparator() {
			public int compare(Object source, Object target) {
				IInstallableUnit sourceIU = (IInstallableUnit) source;
				IInstallableUnit targetIU = (IInstallableUnit) target;
				int id = sourceIU.getId().compareTo(targetIU.getId());
				if (id != 0)
					return id;
				int version = sourceIU.getVersion().compareTo(targetIU.getVersion());
				if (version != 0)
					return version;
				return 0;
			}
		};
		IInstallableUnit[] result = ius;
		if (clone) {
			result = new IInstallableUnit[ius.length];
			System.arraycopy(ius, 0, result, 0, ius.length);
		}
		Arrays.sort(result, comparator);
		return result;
	}

	public void initializeFromArguments(String[] args) throws Exception {
		if (args == null)
			return;
		for (int i = 0; i < args.length; i++) {
			// check for args without parameters (i.e., a flag arg)

			//			if (args[i].equalsIgnoreCase("-sort"))
			//				sort = true;

			if (args[i].equalsIgnoreCase("-meta"))
				type = "meta";

			if (args[i].equalsIgnoreCase("-compare"))
				compare = true;

			if (args[i].equalsIgnoreCase("-profile"))
				type = "profile";

			if (args[i].equalsIgnoreCase("-list"))
				list = true;

			// check for args with parameters. If we are at the last argument or
			// if the next one
			// has a '-' as the first character, then we can't have an arg with
			// a parm so continue.
			if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
				continue;

			String arg = args[++i];

			if (args[i - 1].equalsIgnoreCase("-source"))
				sourceLocation = arg;
			if (args[i - 1].equalsIgnoreCase("-target"))
				targetLocation = arg;
		}
	}

	public void stop() {
	}

}
