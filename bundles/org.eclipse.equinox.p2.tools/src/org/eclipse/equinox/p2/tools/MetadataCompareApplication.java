/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tools;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.console.ProvisioningHelper;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.Query;
import org.eclipse.osgi.service.resolver.VersionRange;

public class MetadataCompareApplication implements IApplication {

	private String source;
	private String target;
	private boolean compare = false;
	private boolean list = false;
	private String type = "meta";

	public Object start(IApplicationContext context) throws Exception {
		initializeFromArguments((String[]) context.getArguments().get("application.args"));

		if (compare)
			compareMetadataRepositories(new URL(source), new URL(target));
		if (list) {
			list(source);
			list(target);
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
		IInstallableUnit[] sourceIUs = source.query(null, null, null, false, null);
		sourceIUs = sort(sourceIUs, true);
		for (int i = 0; i < sourceIUs.length; i++) {
			System.out.print(sourceIUs[i]);
			System.out.println(sourceIUs[i].isFragment() ? " (fragment)" : "");
		}
		System.out.println("Total: " + sourceIUs.length);
	}

	private void compareMetadataRepositories(URL source, URL target) throws ProvisionException {
		ProvisioningHelper.addMetadataRepository(source);
		IMetadataRepository sourceRepo = ProvisioningHelper.getMetadataRepository(source);
		if (sourceRepo == null)
			return;
		ProvisioningHelper.addMetadataRepository(target);
		IMetadataRepository targetRepo = ProvisioningHelper.getMetadataRepository(target);
		if (targetRepo == null)
			return;

		System.out.println("\n" + source.toExternalForm() + " -> " + target.toExternalForm());
		compare(sourceRepo, targetRepo);

		System.out.println("\n" + target.toExternalForm() + " -> " + source.toExternalForm());
		compare(targetRepo, sourceRepo);
	}

	private void compare(IQueryable sourceRepo, IQueryable targetRepo) {
		IQueryable[] target = new IQueryable[] {targetRepo};
		IInstallableUnit[] ius = sourceRepo.query(null, null, null, false, null);
		ius = sort(ius, true);
		for (int i = 0; i < ius.length; i++) {
			IInstallableUnit iu = ius[i];
			Iterator result = Query.getIterator(target, iu.getId(), new VersionRange(iu.getVersion(), true, iu.getVersion(), true), null, false);
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
		if (!compare(iu.getApplicabilityFilter(), next.getApplicabilityFilter()))
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
			if (((InstallableUnitFragment) iu).getHostId() == null || ((InstallableUnitFragment) iu).getHostVersionRange() == null)
				return result;
			if (!((InstallableUnitFragment) iu).getHostId().equals(((InstallableUnitFragment) next).getHostId()))
				result += " hostid";
			if (!((InstallableUnitFragment) iu).getHostVersionRange().equals(((InstallableUnitFragment) next).getHostVersionRange()))
				result += " hostversionRange";
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
			result = new InstallableUnit[ius.length];
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
				source = arg;
			if (args[i - 1].equalsIgnoreCase("-target"))
				target = arg;
		}
	}

	public void stop() {
	}

}
