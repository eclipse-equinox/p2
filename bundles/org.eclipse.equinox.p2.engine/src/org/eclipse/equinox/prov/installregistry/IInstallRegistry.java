/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.prov.installregistry;

import java.util.Collection;
import org.eclipse.equinox.prov.engine.Profile;

public interface IInstallRegistry {

	/**
	 * Save the registry as XML.  If exception is thrown, no change was made.
	 */
	//	public void saveXML() throws IOException {
	//		checkMetadata();
	//		File xmlFile = getLocation();
	//		plog.start(plog.debug("Saving {0}", xmlFile)); //$NON-NLS-1$
	//		if (isEmpty()) { // delete when empty
	//			xmlFile.delete();
	//			if (xmlFile.exists()) {
	//				throw new IOException(NLS.bind(Messages.InstallRegistry_Failed_To_Delete_Install_Registry, xmlFile));
	//			}
	//		} else {
	//			new FileUtil.SafeUpdate(xmlFile) {
	//				public void write(FileOutputStream stream) throws IOException {
	//					XMLWriter writer = new XMLWriter(stream, XML.getProcessingInstruction());
	//					XML.write(writer);
	//					writer.flush();
	//					stream.getFD().sync();
	//					writer.close();
	//				}
	//			}.write();
	//		}
	//		plog.stop();
	//	}
	//	private static class XML implements InstallRegistryXML {
	//		public static XMLWriter.ProcessingInstruction getProcessingInstruction() {
	//			return new XMLWriter.ProcessingInstruction(PI.INSTALL_REGISTRY, MetaInfo.formatVersion(MetaInfo.INSTALL_REGISTRY_VERSION));
	//		}
	//
	//		public static void write(XMLWriter writer) {
	//			InstallRegistry ir = InstallRegistry.getInstance();
	//			writer.start(Elements.INSTALL_REGISTRY);
	//			writer.write(ir.profileRegistry.getProperties());
	//			for (Iterator i = ir.getProfileInstallRegistries().iterator(); i.hasNext();) {
	//				ProfileInstallRegistry registry = (ProfileInstallRegistry) i.next();
	//				if (!registry.isEmpty()) {
	//					writer.start(Elements.PROFILE);
	//					Profile profile = registry.getProfile();
	//					writer.attribute(Attrs.ID, profile.getProfileId());
	//					writer.attribute(Attrs.KIND, profile.getProfileKind());
	//					writer.writeProperty(Profile.INSTALL_LOCATION, profile.getInstallLocation());
	//					writer.write(profile.getAllData());
	//					InstallContext rootContext = profile.getRootContext();
	//					if (rootContext != null) {
	//						write(writer, rootContext);
	//					}
	//					registry.emitXML(writer);
	//					writer.end(Elements.PROFILE);
	//				}
	//			}
	//			writer.end(Elements.INSTALL_REGISTRY);
	//		}
	//
	//		private static void write(XMLWriter writer, InstallContext installContext) {
	//			writer.start(Elements.INSTALL_CONTEXT);
	//			writer.attribute(Attrs.ID, installContext.getId());
	//			writer.attribute(Attrs.NAME, installContext.getName());
	//			writer.attribute(Attrs.DESCRIPTION, installContext.getDescription());
	//			writer.attribute(Attrs.SHAREABLE, installContext.isShareable(), true);
	//			writer.attribute(Attrs.QUALIFIABLE, installContext.isQualifiable(), false);
	//			InstallationContextScope scope = installContext.getScope();
	//			if (scope != InstallationContextScope.NONE_SCOPE) {
	//				writer.attribute(Attrs.SCOPE, scope.getName());
	//			}
	//			writer.write(installContext.getLocalProperties());
	//			String[] adapterTypes = installContext.getAdaptorTypes();
	//			for (int i = 0; i < adapterTypes.length; i += 1) {
	//				writer.start(Elements.ADAPTER);
	//				writer.attribute(Attrs.TYPE, adapterTypes[i]);
	//				writer.end();
	//			}
	//			InstallContext[] subcontexts = installContext.getSubcontexts();
	//			for (int i = 0; i < subcontexts.length; i += 1) {
	//				write(writer, subcontexts[i]);
	//			}
	//			writer.end(Elements.INSTALL_CONTEXT);
	//		}
	//	}
	//	/**
	//	 * The file of the install registry.
	//	 */
	//	public File getLocation() {
	//		return this.location;
	//	}
	//	// The location of the old install registry directory.
	//	// This is where the metadata is still stored.
	//	private File getLegacyLocation(String subdir) {
	//		String path = getLocation().getPath();
	//		if (path.endsWith(CommonDef.Extensions.Xml)) {
	//			path = path.substring(0, path.length() - CommonDef.Extensions.Xml.length());
	//		} else {
	//			path += ".dir"; //$NON-NLS-1$
	//		}
	//		return new File(path, subdir);
	//	}
	public abstract IProfileInstallRegistry getProfileInstallRegistry(Profile profile);

	/**
	 * Open the install registry.  It must be open before any operations can be performed.
	 */
	//	public void open() throws IOException {
	//		//		openFile(Agent.getInstance().getInstallRegistryLocation());
	//	}
	//
	//	// This form is for AbstractAgentTestCase because the preferences aren't set correctly
	//	// when it needs the install registry.
	//	public void open(File dir) throws IOException {
	//		openFile(new File(dir, Agent.FILENAME_INSTALL_REGISTRY));
	//	}
	//
	//	private void openFile(File file) throws IOException {
	//		if (isOpen()) {
	//			throw new InstallRegistryException(Messages2.InstallRegistry_Install_Registry_Is_Already_Open);
	//		}
	//		this.location = file;
	//		if (this.location.isDirectory()) {
	//			throw new InstallRegistryException(NLS.bind(Messages2.InstallRegistry_Install_Registry_Exists_And_Is_A_Directory, this.location));
	//		} else if (!this.location.exists()) {
	//			// verify we can write it
	//			this.location.getParentFile().mkdirs();
	//			new FileOutputStream(this.location).close();
	//			this.location.delete();
	//		}
	//
	//		// TODO: move to cache
	//		File metadataDir = getLegacyLocation(METADATA_DIR);
	//		try {
	//			this.metadataRepo = StandardRepository.create(this.installedMetadata.getRepositoryGroup(), metadataDir);
	//		} catch (RuntimeException e) {
	//			// report error below
	//		}
	//		if (this.metadataRepo == null) {
	//			throw new InstallRegistryException(NLS.bind(Messages.InstallRegistry_Failed_To_Create_Install_Registry_Repo, metadataDir));
	//		}
	//		this.metadataRepo.setOpen(true);
	//		load();
	//		checkMetadata();
	//	}
	//
	//	public void close() {
	//		this.installedMetadata.getRepositoryGroup().removeRepository(this.metadataRepo);
	//		this.metadataRepo = null;
	//		if (isEmpty()) {
	//			purge();
	//		}
	//	}
	//	public void purge() {
	//		getLocation().delete();
	//		FileUtil.rm_r(getLegacyLocation(""), /*removeRoot*/true); //$NON-NLS-1$
	//	}
	public abstract Collection getProfileInstallRegistries();

}