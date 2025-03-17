/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Danail Nachev -  ProSyst - bug 218625
 *     Rob Harrop - SpringSource Inc. (bug 247522 and 255520)
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.eclipse.bundledescription;

import java.net.URL;
import java.util.*;
import org.eclipse.osgi.framework.util.KeyedElement;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.*;
import org.osgi.framework.wiring.*;
import org.osgi.resource.*;

@SuppressWarnings("restriction")
final class BundleDescriptionImpl extends BaseDescriptionImpl implements BundleDescription, KeyedElement {
	static final String[] EMPTY_STRING = new String[0];
	static final ImportPackageSpecification[] EMPTY_IMPORTS = new ImportPackageSpecification[0];
	static final BundleSpecification[] EMPTY_BUNDLESPECS = new BundleSpecification[0];
	static final ExportPackageDescription[] EMPTY_EXPORTS = new ExportPackageDescription[0];
	static final BundleDescription[] EMPTY_BUNDLEDESCS = new BundleDescription[0];
	static final GenericSpecification[] EMPTY_GENERICSPECS = new GenericSpecification[0];
	static final GenericDescription[] EMPTY_GENERICDESCS = new GenericDescription[0];

	static final int RESOLVED = 0x01;
	static final int SINGLETON = 0x02;
	static final int REMOVAL_PENDING = 0x04;
	static final int FULLY_LOADED = 0x08;
	static final int LAZY_LOADED = 0x10;
	static final int HAS_DYNAMICIMPORT = 0x20;
	static final int ATTACH_FRAGMENTS = 0x40;
	static final int DYNAMIC_FRAGMENTS = 0x80;

	// set to fully loaded and allow dynamic fragments by default
	private volatile int stateBits = FULLY_LOADED | ATTACH_FRAGMENTS | DYNAMIC_FRAGMENTS;

	private volatile long bundleId = -1;
	volatile HostSpecification host; //null if the bundle is not a fragment. volatile to allow unsynchronized checks for null

	private List<BundleDescription> dependents;
	private String[] mandatory;
	private Map<String, Object> attributes;
	private Map<String, String> arbitraryDirectives;

	private volatile LazyData lazyData;
	private volatile int equinox_ee = -1;

	private DescriptionWiring bundleWiring;

	public BundleDescriptionImpl() {
		//
	}

	@Override
	public long getBundleId() {
		return bundleId;
	}

	@Override
	public String getSymbolicName() {
		return getName();
	}

	@Override
	public BundleDescription getSupplier() {
		return this;
	}

	@Override
	public String getLocation() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			return currentData.location;
		}
	}

	@Override
	public String getPlatformFilter() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			return currentData.platformFilter;
		}
	}

	@Override
	public String[] getExecutionEnvironments() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			if (currentData.executionEnvironments == null) {
				return EMPTY_STRING;
			}
			return currentData.executionEnvironments;
		}
	}

	@Override
	public ImportPackageSpecification[] getImportPackages() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			if (currentData.importPackages == null) {
				return EMPTY_IMPORTS;
			}
			return currentData.importPackages;
		}
	}

	@Override
	public ImportPackageSpecification[] getAddedDynamicImportPackages() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			if (currentData.addedDynamicImports == null) {
				return EMPTY_IMPORTS;
			}
			return currentData.addedDynamicImports
					.toArray(new ImportPackageSpecification[currentData.addedDynamicImports.size()]);
		}
	}

	@Override
	public BundleSpecification[] getRequiredBundles() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			if (currentData.requiredBundles == null) {
				return EMPTY_BUNDLESPECS;
			}
			return currentData.requiredBundles;
		}
	}

	@Override
	public GenericSpecification[] getGenericRequires() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			if (currentData.genericRequires == null) {
				return EMPTY_GENERICSPECS;
			}
			return currentData.genericRequires;
		}
	}

	@Override
	public GenericDescription[] getGenericCapabilities() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			if (currentData.genericCapabilities == null) {
				return EMPTY_GENERICDESCS;
			}
			return currentData.genericCapabilities;
		}
	}

	@Override
	public NativeCodeSpecification getNativeCodeSpecification() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ExportPackageDescription[] getExportPackages() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			return currentData.exportPackages == null ? EMPTY_EXPORTS : currentData.exportPackages;
		}
	}

	@Override
	public boolean isResolved() {
		return (stateBits & RESOLVED) != 0;
	}

	@Override
	public State getContainingState() {
		return null;
	}

	@Override
	public BundleDescription[] getFragments() {
		if (host != null) {
			return EMPTY_BUNDLEDESCS;
		}
		throw new IllegalStateException("BundleDescription does not belong to a state."); //$NON-NLS-1$
	}

	@Override
	public HostSpecification getHost() {
		return host;
	}

	@Override
	public boolean isSingleton() {
		return (stateBits & SINGLETON) != 0;
	}

	@Override
	public boolean isRemovalPending() {
		return (stateBits & REMOVAL_PENDING) != 0;
	}

	@Override
	public boolean hasDynamicImports() {
		return (stateBits & HAS_DYNAMICIMPORT) != 0;
	}

	@Override
	public boolean attachFragments() {
		return (stateBits & ATTACH_FRAGMENTS) != 0;
	}

	@Override
	public boolean dynamicFragments() {
		return (stateBits & DYNAMIC_FRAGMENTS) != 0;
	}

	@Override
	public ExportPackageDescription[] getSelectedExports() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			if (currentData.selectedExports == null) {
				return EMPTY_EXPORTS;
			}
			return currentData.selectedExports;
		}
	}

	@Override
	public GenericDescription[] getSelectedGenericCapabilities() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			if (currentData.selectedCapabilities == null) {
				return EMPTY_GENERICDESCS;
			}
			return currentData.selectedCapabilities;
		}
	}

	@Override
	public ExportPackageDescription[] getSubstitutedExports() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			if (currentData.substitutedExports == null) {
				return EMPTY_EXPORTS;
			}
			return currentData.substitutedExports;
		}
	}

	@Override
	public BundleDescription[] getResolvedRequires() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			if (currentData.resolvedRequires == null) {
				return EMPTY_BUNDLEDESCS;
			}
			return currentData.resolvedRequires;
		}
	}

	@Override
	public ExportPackageDescription[] getResolvedImports() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			if (currentData.resolvedImports == null) {
				return EMPTY_EXPORTS;
			}
			return currentData.resolvedImports;
		}
	}

	@Override
	public GenericDescription[] getResolvedGenericRequires() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			if (currentData.resolvedCapabilities == null) {
				return EMPTY_GENERICDESCS;
			}
			return currentData.resolvedCapabilities;
		}
	}

	public Map<String, List<StateWire>> getWires() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			if (currentData.stateWires == null) {
				currentData.stateWires = new HashMap<>(0);
			}
			return currentData.stateWires;
		}
	}

	protected void setBundleId(long bundleId) {
		this.bundleId = bundleId;
	}

	protected void setSymbolicName(String symbolicName) {
		setName(symbolicName);
	}

	protected void setLocation(String location) {
		synchronized (this.monitor) {
			checkLazyData();
			lazyData.location = location;
		}
	}

	protected void setPlatformFilter(String platformFilter) {
		synchronized (this.monitor) {
			checkLazyData();
			lazyData.platformFilter = platformFilter;
		}
	}

	protected void setExecutionEnvironments(String[] executionEnvironments) {
		synchronized (this.monitor) {
			checkLazyData();
			lazyData.executionEnvironments = executionEnvironments == null || executionEnvironments.length > 0 ? executionEnvironments : EMPTY_STRING;
		}
	}

	protected void setExportPackages(ExportPackageDescription[] exportPackages) {
		synchronized (this.monitor) {
			checkLazyData();
			lazyData.exportPackages = exportPackages == null || exportPackages.length > 0 ? exportPackages : EMPTY_EXPORTS;
			if (exportPackages != null) {
				for (ExportPackageDescription exportPackage : exportPackages) {
					((ExportPackageDescriptionImpl) exportPackage).setExporter(this);
				}
			}
		}
	}

	protected void setImportPackages(ImportPackageSpecification[] importPackages) {
		synchronized (this.monitor) {
			checkLazyData();
			lazyData.importPackages = importPackages == null || importPackages.length > 0 ? importPackages : EMPTY_IMPORTS;
			if (importPackages != null) {
				for (ImportPackageSpecification importPackage : importPackages) {
					((ImportPackageSpecificationImpl) importPackage).setBundle(this);
					if (ImportPackageSpecification.RESOLUTION_DYNAMIC.equals(importPackage.getDirective(Constants.RESOLUTION_DIRECTIVE))) {
						stateBits |= HAS_DYNAMICIMPORT;
					}
				}
			}
		}
	}

	protected void setRequiredBundles(BundleSpecification[] requiredBundles) {
		synchronized (this.monitor) {
			checkLazyData();
			lazyData.requiredBundles = requiredBundles == null || requiredBundles.length > 0 ? requiredBundles : EMPTY_BUNDLESPECS;
			if (requiredBundles != null) {
				for (BundleSpecification requiredBundle : requiredBundles) {
					((VersionConstraintImpl) requiredBundle).setBundle(this);
				}
			}
		}
	}

	protected void setGenericCapabilities(GenericDescription[] genericCapabilities) {
		synchronized (this.monitor) {
			checkLazyData();
			lazyData.genericCapabilities = genericCapabilities == null || genericCapabilities.length > 0 ? genericCapabilities : EMPTY_GENERICDESCS;
			if (genericCapabilities != null) {
				for (GenericDescription genericCapability : genericCapabilities) {
					((GenericDescriptionImpl) genericCapability).setSupplier(this);
				}
			}
		}
	}

	protected void setGenericRequires(GenericSpecification[] genericRequires) {
		synchronized (this.monitor) {
			checkLazyData();
			lazyData.genericRequires = genericRequires == null || genericRequires.length > 0 ? genericRequires : EMPTY_GENERICSPECS;
			if (genericRequires != null) {
				for (GenericSpecification genericRequire : genericRequires) {
					((VersionConstraintImpl) genericRequire).setBundle(this);
				}
			}
		}
	}

	protected void setStateBit(int stateBit, boolean on) {
		synchronized (this.monitor) {
			if (on) {
				stateBits |= stateBit;
			} else {
				stateBits &= ~stateBit;
				if (stateBit == RESOLVED) {
					if (bundleWiring != null) {
						bundleWiring.invalidate();
					}
					bundleWiring = null;
				}
			}
		}
	}

	protected void setHost(HostSpecification host) {
		synchronized (this.monitor) {
			this.host = host;
			if (host != null) {
				((VersionConstraintImpl) host).setBundle(this);
			}
		}
	}

	@Override
	public String toString() {
		if (getSymbolicName() == null) {
			return "[" + getBundleId() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return getSymbolicName() + "_" + getVersion(); //$NON-NLS-1$
	}

	@Override
	public Object getKey() {
		return Long.valueOf(bundleId);
	}

	@Override
	public boolean compare(KeyedElement other) {
		if (!(other instanceof BundleDescriptionImpl otherBundleDescription)) {
			return false;
		}
		return bundleId == otherBundleDescription.bundleId;
	}

	@Override
	public int getKeyHashCode() {
		return (int) (bundleId ^ (bundleId >>> 32));
	}

	@Override
	public BundleDescription[] getDependents() {
		synchronized (this.monitor) {
			if (dependents == null) {
				return EMPTY_BUNDLEDESCS;
			}
			return dependents.toArray(new BundleDescription[dependents.size()]);
		}
	}

	boolean hasDependents() {
		synchronized (this.monitor) {
			return dependents == null ? false : dependents.size() > 0;
		}
	}

	// DO NOT call while holding this.monitor
	private LazyData loadLazyData() {
		if ((stateBits & LAZY_LOADED) == 0) {
			return this.lazyData;
		}

		throw new IllegalStateException("No valid reader for the bundle description"); //$NON-NLS-1$

	}

	public void setEquinoxEE(int equinox_ee) {
		this.equinox_ee = equinox_ee;
	}

	public int getEquinoxEE() {
		return equinox_ee;
	}

	private void checkLazyData() {
		if (lazyData == null) {
			lazyData = new LazyData();
		}
	}

	static final class LazyData {
		String location;
		String platformFilter;

		BundleSpecification[] requiredBundles;
		ExportPackageDescription[] exportPackages;
		ImportPackageSpecification[] importPackages;
		GenericDescription[] genericCapabilities;
		GenericSpecification[] genericRequires;
		ExportPackageDescription[] selectedExports;
		GenericDescription[] selectedCapabilities;
		BundleDescription[] resolvedRequires;
		ExportPackageDescription[] resolvedImports;
		GenericDescription[] resolvedCapabilities;
		ExportPackageDescription[] substitutedExports;
		String[] executionEnvironments;

		Map<String, List<StateWire>> stateWires;
		// Note that this is not persisted in the state cache
		List<ImportPackageSpecification> addedDynamicImports;
	}

	@Override
	public Map<String, Object> getAttributes() {
		synchronized (this.monitor) {
			return attributes;
		}
	}

	@SuppressWarnings("unchecked")
	void setAttributes(Map<String, ?> attributes) {
		synchronized (this.monitor) {
			this.attributes = (Map<String, Object>) attributes;
		}
	}

	Object getDirective(String key) {
		synchronized (this.monitor) {
			if (Constants.MANDATORY_DIRECTIVE.equals(key)) {
				return mandatory;
			}
			if (Constants.SINGLETON_DIRECTIVE.equals(key)) {
				return isSingleton() ? Boolean.TRUE : Boolean.FALSE;
			}
			if (Constants.FRAGMENT_ATTACHMENT_DIRECTIVE.equals(key)) {
				if (!attachFragments()) {
					return Constants.FRAGMENT_ATTACHMENT_NEVER;
				}
				if (dynamicFragments()) {
					return Constants.FRAGMENT_ATTACHMENT_ALWAYS;
				}
				return Constants.FRAGMENT_ATTACHMENT_RESOLVETIME;
			}
		}
		return null;
	}

	void setDirective(String key, Object value) {
		// only pay attention to mandatory directive for now; others are set with setState method
		if (Constants.MANDATORY_DIRECTIVE.equals(key)) {
			mandatory = (String[]) value;
		}
	}

	@SuppressWarnings("unchecked")
	void setArbitraryDirectives(Map<String, ?> directives) {
		synchronized (this.monitor) {
			this.arbitraryDirectives = (Map<String, String>) directives;
		}
	}

	Map<String, String> getArbitraryDirectives() {
		synchronized (this.monitor) {
			return arbitraryDirectives;
		}
	}

	@Override
	public Map<String, String> getDeclaredDirectives() {
		Map<String, String> result = new HashMap<>(2);
		Map<String, String> arbitrary = getArbitraryDirectives();
		if (arbitrary != null) {
			result.putAll(arbitrary);
		}
		if (!attachFragments()) {
			result.put(Constants.FRAGMENT_ATTACHMENT_DIRECTIVE, Constants.FRAGMENT_ATTACHMENT_NEVER);
		} else {
			if (dynamicFragments()) {
				result.put(Constants.FRAGMENT_ATTACHMENT_DIRECTIVE, Constants.FRAGMENT_ATTACHMENT_ALWAYS);
			} else {
				result.put(Constants.FRAGMENT_ATTACHMENT_DIRECTIVE, Constants.FRAGMENT_ATTACHMENT_RESOLVETIME);
			}
		}
		if (isSingleton()) {
			result.put(Constants.SINGLETON_DIRECTIVE, Boolean.TRUE.toString());
		}
		String[] mandatoryDirective = (String[]) getDirective(Constants.MANDATORY_DIRECTIVE);
		if (mandatoryDirective != null) {
			result.put(Constants.MANDATORY_DIRECTIVE, ExportPackageDescriptionImpl.toString(mandatoryDirective));
		}
		return Collections.unmodifiableMap(result);
	}

	@Override
	public Map<String, Object> getDeclaredAttributes() {
		Map<String, Object> result = new HashMap<>(1);
		synchronized (this.monitor) {
			if (attributes != null) {
				result.putAll(attributes);
			}
		}
		result.put(BundleRevision.BUNDLE_NAMESPACE, getName());
		result.put(Constants.BUNDLE_VERSION_ATTRIBUTE, getVersion());
		return Collections.unmodifiableMap(result);
	}

	@Override
	public List<BundleRequirement> getDeclaredRequirements(String namespace) {
		List<BundleRequirement> result = new ArrayList<>();
		if (namespace == null || BundleRevision.BUNDLE_NAMESPACE.equals(namespace)) {
			BundleSpecification[] requires = getRequiredBundles();
			for (BundleSpecification require : requires) {
				result.add(require.getRequirement());
			}
		}
		if (host != null && (namespace == null || BundleRevision.HOST_NAMESPACE.equals(namespace))) {
			result.add(host.getRequirement());
		}
		if (namespace == null || BundleRevision.PACKAGE_NAMESPACE.equals(namespace)) {
			ImportPackageSpecification[] imports = getImportPackages();
			for (ImportPackageSpecification importPkg : imports) {
				result.add(importPkg.getRequirement());
			}
		}
		GenericSpecification[] genericSpecifications = getGenericRequires();
		for (GenericSpecification requirement : genericSpecifications) {
			if (namespace == null || namespace.equals(requirement.getType())) {
				result.add(requirement.getRequirement());
			}
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public List<BundleCapability> getDeclaredCapabilities(String namespace) {
		List<BundleCapability> result = new ArrayList<>();
		if (host == null) {
			if (getSymbolicName() != null) {
				if (namespace == null || BundleRevision.BUNDLE_NAMESPACE.equals(namespace)) {
					result.add(BundleDescriptionImpl.this.getCapability());
				}
				if (attachFragments() && (namespace == null || BundleRevision.HOST_NAMESPACE.equals(namespace))) {
					result.add(BundleDescriptionImpl.this.getCapability(BundleRevision.HOST_NAMESPACE));
				}
			}

		} else {
			// may need to have a osgi.wiring.fragment capability
		}
		if (namespace == null || BundleRevision.PACKAGE_NAMESPACE.equals(namespace)) {
			ExportPackageDescription[] exports = getExportPackages();
			for (ExportPackageDescription exportPkg : exports) {
				result.add(exportPkg.getCapability());
			}
		}
		GenericDescription[] genericCapabilities = getGenericCapabilities();
		for (GenericDescription capabilitiy : genericCapabilities) {
			if (namespace == null || namespace.equals(capabilitiy.getType())) {
				result.add(capabilitiy.getCapability());
			}
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public int getTypes() {
		return getHost() != null ? BundleRevision.TYPE_FRAGMENT : 0;
	}

	@Override
	public Bundle getBundle() {
		Object ref = getUserObject();
		if (ref instanceof BundleReference) {
			return ((BundleReference) ref).getBundle();
		}
		return null;
	}

	@Override
	String getInternalNameSpace() {
		return BundleRevision.BUNDLE_NAMESPACE;
	}

	@Override
	public BundleWiring getWiring() {
		synchronized (this.monitor) {
			if (bundleWiring != null || !isResolved()) {
				return bundleWiring;
			}
			return bundleWiring = new DescriptionWiring();
		}
	}

	static class BundleWireImpl implements BundleWire {
		private final BundleCapability capability;
		private final BundleWiring provider;
		private final BundleRequirement requirement;
		private final BundleWiring requirer;

		public BundleWireImpl(StateWire wire) {
			VersionConstraint declaredRequirement = wire.getDeclaredRequirement();
			if (declaredRequirement instanceof HostSpecification) {
				this.capability = ((BaseDescriptionImpl) wire.getDeclaredCapability()).getCapability(BundleRevision.HOST_NAMESPACE);
			} else {
				this.capability = wire.getDeclaredCapability().getCapability();
			}
			this.provider = wire.getCapabilityHost().getWiring();
			this.requirement = declaredRequirement.getRequirement();
			this.requirer = wire.getRequirementHost().getWiring();
		}

		@Override
		public BundleCapability getCapability() {
			return capability;
		}

		@Override
		public BundleRequirement getRequirement() {
			return requirement;
		}

		@Override
		public BundleWiring getProviderWiring() {
			return provider;
		}

		@Override
		public BundleWiring getRequirerWiring() {
			return requirer;
		}

		@Override
		public int hashCode() {
			int hashcode = 31 + capability.hashCode();
			hashcode = hashcode * 31 + requirement.hashCode();
			hashcode = hashcode * 31 + provider.hashCode();
			hashcode = hashcode * 31 + requirer.hashCode();
			return hashcode;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof BundleWireImpl other)) {
				return false;
			}
			return capability.equals(other.getCapability()) && requirement.equals(other.getRequirement()) && provider.equals(other.getProviderWiring()) && requirer.equals(other.getRequirerWiring());
		}

		@Override
		public String toString() {
			return getRequirement() + " -> " + getCapability(); //$NON-NLS-1$
		}

		@Override
		public BundleRevision getProvider() {
			return provider.getRevision();
		}

		@Override
		public BundleRevision getRequirer() {
			return requirer.getRevision();
		}
	}

	/**
	 * Coerce the generic type of a list from List<BundleWire>
	 * to List<Wire>
	 * @param l List to be coerced.
	 * @return l coerced to List<Wire>
	 */
	@SuppressWarnings("unchecked")
	static List<Wire> asListWire(List<? extends Wire> l) {
		return (List<Wire>) l;
	}

	/**
	 * Coerce the generic type of a list from List<BundleCapability>
	 * to List<Capability>
	 * @param l List to be coerced.
	 * @return l coerced to List<Capability>
	 */
	@SuppressWarnings("unchecked")
	static List<Capability> asListCapability(List<? extends Capability> l) {
		return (List<Capability>) l;
	}

	/**
	 * Coerce the generic type of a list from List<BundleRequirement>
	 * to List<Requirement>
	 * @param l List to be coerced.
	 * @return l coerced to List<Requirement>
	 */
	@SuppressWarnings("unchecked")
	static List<Requirement> asListRequirement(List<? extends Requirement> l) {
		return (List<Requirement>) l;
	}

	// Note that description wiring are identity equality based
	class DescriptionWiring implements BundleWiring {
		private volatile boolean valid = true;

		@Override
		public Bundle getBundle() {
			return BundleDescriptionImpl.this.getBundle();
		}

		@Override
		public boolean isInUse() {
			return valid && (isCurrent() || BundleDescriptionImpl.this.hasDependents());
		}

		void invalidate() {
			valid = false;
		}

		@Override
		public boolean isCurrent() {
			return valid && !BundleDescriptionImpl.this.isRemovalPending();
		}

		@Override
		public List<BundleCapability> getCapabilities(String namespace) {
			if (!isInUse()) {
				return null;
			}
			List<BundleCapability> result = new ArrayList<>();
			GenericDescription[] genericCapabilities = getSelectedGenericCapabilities();
			for (GenericDescription capabilitiy : genericCapabilities) {
				if (namespace == null || namespace.equals(capabilitiy.getType())) {
					result.add(capabilitiy.getCapability());
				}
			}
			if (host != null) {
				return result;
			}
			if (getSymbolicName() != null) {
				if (namespace == null || BundleRevision.BUNDLE_NAMESPACE.equals(namespace)) {
					result.add(BundleDescriptionImpl.this.getCapability());
				}
				if (attachFragments() && (namespace == null || BundleRevision.HOST_NAMESPACE.equals(namespace))) {
					result.add(BundleDescriptionImpl.this.getCapability(BundleRevision.HOST_NAMESPACE));
				}
			}
			if (namespace == null || BundleRevision.PACKAGE_NAMESPACE.equals(namespace)) {
				ExportPackageDescription[] exports = getSelectedExports();
				for (ExportPackageDescription exportPkg : exports) {
					result.add(exportPkg.getCapability());
				}
			}
			return result;
		}

		@Override
		public List<Capability> getResourceCapabilities(String namespace) {
			return asListCapability(getCapabilities(namespace));
		}

		@Override
		public List<BundleRequirement> getRequirements(String namespace) {
			List<BundleWire> requiredWires = getRequiredWires(namespace);
			if (requiredWires == null) {
				// happens if not in use
				return null;
			}
			List<BundleRequirement> requirements = new ArrayList<>(requiredWires.size());
			for (BundleWire wire : requiredWires) {
				if (!requirements.contains(wire.getRequirement())) {
					requirements.add(wire.getRequirement());
				}
			}
			// get dynamic imports
			if (getHost() == null && (namespace == null || BundleRevision.PACKAGE_NAMESPACE.equals(namespace))) {
				// TODO need to handle fragments that add dynamic imports
				if (hasDynamicImports()) {
					ImportPackageSpecification[] imports = getImportPackages();
					for (ImportPackageSpecification impPackage : imports) {
						if (ImportPackageSpecification.RESOLUTION_DYNAMIC.equals(impPackage.getDirective(Constants.RESOLUTION_DIRECTIVE))) {
							BundleRequirement req = impPackage.getRequirement();
							if (!requirements.contains(req)) {
								requirements.add(req);
							}
						}
					}
				}
				ImportPackageSpecification[] addedDynamic = getAddedDynamicImportPackages();
				for (ImportPackageSpecification dynamicImport : addedDynamic) {
					BundleRequirement req = dynamicImport.getRequirement();
					if (!requirements.contains(req)) {
						requirements.add(req);
					}
				}
			}
			return requirements;
		}

		@Override
		public List<Requirement> getResourceRequirements(String namespace) {
			return asListRequirement(getRequirements(namespace));
		}

		@Override
		public List<BundleWire> getProvidedWires(String namespace) {
			if (!isInUse()) {
				return null;
			}
			BundleDescription[] dependentBundles = getDependents();
			List<BundleWire> unorderedResult = new ArrayList<>();
			for (BundleDescription dependent : dependentBundles) {
				List<BundleWire> dependentWires = dependent.getWiring().getRequiredWires(namespace);
				if (dependentWires != null) {
					for (BundleWire bundleWire : dependentWires) {
						if (bundleWire.getProviderWiring() == this) {
							unorderedResult.add(bundleWire);
						}
					}
				}
			}
			List<BundleWire> orderedResult = new ArrayList<>(unorderedResult.size());
			List<BundleCapability> capabilities = getCapabilities(namespace);
			for (BundleCapability capability : capabilities) {
				for (Iterator<BundleWire> wires = unorderedResult.iterator(); wires.hasNext();) {
					BundleWire wire = wires.next();
					if (wire.getCapability().equals(capability)) {
						wires.remove();
						orderedResult.add(wire);
					}
				}
			}
			return orderedResult;
		}

		@Override
		public List<Wire> getProvidedResourceWires(String namespace) {
			return asListWire(getProvidedWires(namespace));
		}

		@Override
		public List<BundleWire> getRequiredWires(String namespace) {
			if (!isInUse()) {
				return null;
			}
			List<BundleWire> result = Collections.emptyList();
			Map<String, List<StateWire>> wireMap = getWires();
			if (namespace == null) {
				result = new ArrayList<>();
				for (List<StateWire> wires : wireMap.values()) {
					for (StateWire wire : wires) {
						result.add(new BundleWireImpl(wire));
					}
				}
				return result;
			}
			List<StateWire> wires = wireMap.get(namespace);
			if (wires == null) {
				return result;
			}
			result = new ArrayList<>(wires.size());
			for (StateWire wire : wires) {
				result.add(new BundleWireImpl(wire));
			}
			return result;
		}

		@Override
		public List<Wire> getRequiredResourceWires(String namespace) {
			return asListWire(getRequiredWires(namespace));
		}

		@Override
		public BundleRevision getRevision() {
			return BundleDescriptionImpl.this;
		}

		@Override
		public BundleRevision getResource() {
			return getRevision();
		}

		@Override
		public ClassLoader getClassLoader() {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<URL> findEntries(String path, String filePattern, int options) {
			return null;
		}

		@Override
		public Collection<String> listResources(String path, String filePattern, int options) {
			return null;
		}

		@Override
		public String toString() {
			return BundleDescriptionImpl.this.toString();
		}
	}

	@Override
	public List<Capability> getCapabilities(String namespace) {
		return asListCapability(getDeclaredCapabilities(namespace));
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		return asListRequirement(getDeclaredRequirements(namespace));
	}
}
