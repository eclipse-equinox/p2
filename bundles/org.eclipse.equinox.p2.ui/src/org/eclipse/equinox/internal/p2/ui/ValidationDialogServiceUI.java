/*******************************************************************************
 *  Copyright (c) 2008, 2018 IBM Corporation and others.
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
 *     Rapicorp, Inc. - add support for information dialog
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.engine.phases.AuthorityChecker;
import org.eclipse.equinox.internal.p2.ui.dialogs.*;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.UIServices;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.spi.IArtifactUIServices;
import org.eclipse.equinox.p2.repository.metadata.spi.IInstallableUnitUIServices;
import org.eclipse.equinox.p2.repository.spi.PGPPublicKeyService;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.TreeNode;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

/**
 * The default GUI-based implementation of {@link UIServices}. The service
 * declaration is made in the serviceui_component.xml file.
 *
 */
public class ValidationDialogServiceUI extends UIServices implements IArtifactUIServices, IInstallableUnitUIServices {

	private final IProvisioningAgent agent;

	private Display display;

	private Consumer<String> linkHandler = link -> {
		if (PlatformUI.isWorkbenchRunning()) {
			try {
				URL url = new URL(link);
				PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(url);
			} catch (Exception x) {
				ProvUIActivator.getDefault().getLog()
						.log(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, x.getMessage(), x));
			}
		}
	};

	private IShellProvider shellProvider = () -> ProvUI.getDefaultParentShell();

	public ValidationDialogServiceUI() {
		this(null);
	}

	public ValidationDialogServiceUI(IProvisioningAgent agent) {
		this.agent = agent;
	}

	static final class MessageDialogWithLink extends MessageDialog {
		private final String linkText;
		private final Consumer<String> linkOpener;

		MessageDialogWithLink(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage,
				int dialogImageType, String[] dialogButtonLabels, int defaultIndex, String linkText,
				Consumer<String> linkOpener) {
			super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels,
					defaultIndex);
			this.linkText = linkText;
			this.linkOpener = linkOpener;
		}

		@Override
		protected Control createCustomArea(Composite parent) {
			if (linkText == null)
				return super.createCustomArea(parent);

			Link link = new Link(parent, SWT.NONE);
			link.setText(linkText);
			link.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
				linkOpener.accept(e.text);
			}));
			return link;
		}
	}

	/**
	 * Subclassed to add a cancel button to the error dialog.
	 */
	static class OkCancelErrorDialog extends ErrorDialog {

		public OkCancelErrorDialog(Shell parentShell, String dialogTitle, String message, IStatus status,
				int displayMask) {
			super(parentShell, dialogTitle, message, status, displayMask);
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			// create OK and Details buttons
			createButton(parent, IDialogConstants.OK_ID, ProvUIMessages.ServiceUI_InstallAnywayAction_Label, true);
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
			createDetailsButton(parent);
		}
	}

	@Override
	public AuthenticationInfo getUsernamePassword(final String location) {

		final AuthenticationInfo[] result = new AuthenticationInfo[1];
		if (!suppressAuthentication() && !isHeadless()) {
			getDisplay().syncExec(() -> {
				Shell shell = shellProvider.getShell();
				String message = NLS.bind(ProvUIMessages.ServiceUI_LoginDetails, location);
				UserValidationDialog dialog = new UserValidationDialog(shell, ProvUIMessages.ServiceUI_LoginRequired,
						null, message);
				int dialogCode = dialog.open();
				if (dialogCode == Window.OK) {
					result[0] = dialog.getResult();
				} else if (dialogCode == Window.CANCEL) {
					result[0] = AUTHENTICATION_PROMPT_CANCELED;
				}
			});
		}
		return result[0];
	}

	private boolean suppressAuthentication() {
		Job job = Job.getJobManager().currentJob();
		if (job != null) {
			return job.getProperty(LoadMetadataRepositoryJob.SUPPRESS_AUTHENTICATION_JOB_MARKER) != null;
		}
		return false;
	}

	@Override
	public TrustInfo getTrustInfo(Certificate[][] untrustedChains, final String[] unsignedDetail) {
		return getTrustInfo(untrustedChains, Collections.emptyList(), unsignedDetail);
	}

	@Override
	public TrustInfo getTrustInfo(Certificate[][] untrustedChains, Collection<PGPPublicKey> untrustedPublicKeys,
			final String[] unsignedDetail) {
		if (untrustedChains == null) {
			untrustedChains = new Certificate[][] {};
		}
		boolean trustUnsigned = true;
		boolean persistTrust = false;
		List<Certificate> trustedCertificates = new ArrayList<>();
		List<PGPPublicKey> trustedKeys = new ArrayList<>();
		// Some day we may summarize all of this in one UI, or perhaps we'll have a
		// preference to honor regarding
		// unsigned content. For now we prompt separately first as to whether unsigned
		// detail should be trusted
		if (!isHeadless() && unsignedDetail != null && unsignedDetail.length > 0) {
			final boolean[] result = new boolean[] { false };
			getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					Shell shell = shellProvider.getShell();
					OkCancelErrorDialog dialog = new OkCancelErrorDialog(shell, ProvUIMessages.ServiceUI_warning_title,
							null, createStatus(), IStatus.WARNING);
					result[0] = dialog.open() == IDialogConstants.OK_ID;
				}

				private IStatus createStatus() {
					MultiStatus parent = new MultiStatus(ProvUIActivator.PLUGIN_ID, 0,
							ProvUIMessages.ServiceUI_unsigned_message, null);
					for (String element : unsignedDetail) {
						parent.add(new Status(IStatus.WARNING, ProvUIActivator.PLUGIN_ID, element));
					}
					return parent;
				}
			});
			trustUnsigned = result[0];
		}
		// For now, there is no need to show certificates if there was unsigned content
		// and we don't trust it.
		if (!trustUnsigned)
			return new TrustInfo(trustedCertificates, trustedKeys, persistTrust, trustUnsigned);

		// We've established trust for unsigned content, now examine the untrusted
		// chains
		if (!isHeadless() && (untrustedChains.length > 0 || !untrustedPublicKeys.isEmpty())) {
			return getTrustInfo(
					Arrays.stream(untrustedChains).collect(
							Collectors.toMap(Arrays::asList, it -> Set.of(), (e1, e2) -> e1, LinkedHashMap::new)),
					untrustedPublicKeys.stream().collect(
							Collectors.toMap(Function.identity(), it -> Set.of(), (e1, e2) -> e1, LinkedHashMap::new)),
					null, null);

		}

		return new TrustInfo(trustedCertificates, trustedKeys, persistTrust, trustUnsigned);
	}

	@Override
	public TrustInfo getTrustInfo(Map<List<Certificate>, Set<IArtifactKey>> untrustedCertificates,
			Map<PGPPublicKey, Set<IArtifactKey>> untrustedPGPKeys, //
			Set<IArtifactKey> unsignedArtifacts, //
			Map<IArtifactKey, File> artifacts) {
		boolean trustUnsigned = true;
		AtomicBoolean persistTrust = new AtomicBoolean();
		AtomicBoolean trustAlways = new AtomicBoolean();
		List<Certificate> trustedCertificates = new ArrayList<>();
		List<PGPPublicKey> trustedKeys = new ArrayList<>();
		if (!isHeadless()) {
			TreeNode[] input = createTreeNodes(untrustedCertificates, untrustedPGPKeys, unsignedArtifacts, artifacts);
			if (input.length != 0) {
				trustUnsigned = unsignedArtifacts == null || unsignedArtifacts.isEmpty();
				List<Object> result = new ArrayList<>();
				getDisplay().syncExec(() -> {
					Shell shell = shellProvider.getShell();
					TrustCertificateDialog trustCertificateDialog = new TrustCertificateDialog(shell, input);
					if (trustCertificateDialog.open() == Window.OK) {
						Object[] dialogResult = trustCertificateDialog.getResult();
						if (dialogResult != null) {
							result.addAll(Arrays.asList(dialogResult));
						}
						persistTrust.set(trustCertificateDialog.isRememberSelectedSigners());
						trustAlways.set(trustCertificateDialog.isTrustAlways());
					}
				});
				for (Object o : result) {
					if (o instanceof TreeNode) {
						o = ((TreeNode) o).getValue();
					}
					if (o instanceof Certificate) {
						trustedCertificates.add((Certificate) o);
					} else if (o instanceof PGPPublicKey) {
						trustedKeys.add((PGPPublicKey) o);
					} else if (o == null) {
						trustUnsigned = true;
					}
				}
			}
		}

		return new TrustInfo(trustedCertificates, trustedKeys, persistTrust.get(), trustUnsigned, trustAlways.get());
	}

	private TreeNode[] createTreeNodes(Map<List<Certificate>, Set<IArtifactKey>> untrustedCertificates,
			Map<PGPPublicKey, Set<IArtifactKey>> untrustedPGPKeys, //
			Set<IArtifactKey> unsignedArtifacts, //
			Map<IArtifactKey, File> artifacts) {

		List<ExtendedTreeNode> children = new ArrayList<>();
		if (untrustedCertificates != null && !untrustedCertificates.isEmpty()) {
			for (Map.Entry<List<Certificate>, Set<IArtifactKey>> entry : untrustedCertificates.entrySet()) {
				ExtendedTreeNode parent = null;
				List<Certificate> key = entry.getKey();
				Set<IArtifactKey> associatedArtifacts = entry.getValue();
				for (Certificate certificate : key) {
					ExtendedTreeNode node = new ExtendedTreeNode(certificate, associatedArtifacts);
					if (parent == null) {
						children.add(node);
					} else {
						node.setParent(parent);
						parent.setChildren(new TreeNode[] { node });
					}
					parent = node;
				}
			}
		}

		if (untrustedPGPKeys != null && !untrustedPGPKeys.isEmpty()) {
			PGPPublicKeyService keyService = agent == null ? null : agent.getService(PGPPublicKeyService.class);
			for (Map.Entry<PGPPublicKey, Set<IArtifactKey>> entry : untrustedPGPKeys.entrySet()) {
				PGPPublicKey key = entry.getKey();
				Set<IArtifactKey> associatedArtifacts = entry.getValue();
				Date verifiedRevocationDate = keyService == null ? null : keyService.getVerifiedRevocationDate(key);
				ExtendedTreeNode node = new ExtendedTreeNode(key, associatedArtifacts, verifiedRevocationDate);
				children.add(node);
				expandChildren(node, key, keyService, new HashSet<>(), Integer.getInteger("p2.pgp.trust.depth", 3)); //$NON-NLS-1$
			}
		}

		if (unsignedArtifacts != null && !unsignedArtifacts.isEmpty()) {
			ExtendedTreeNode node = new ExtendedTreeNode(null, unsignedArtifacts);
			children.add(node);

		}

		return children.toArray(TreeNode[]::new);
	}

	private void expandChildren(TreeNode result, PGPPublicKey key, PGPPublicKeyService keyService,
			Set<PGPPublicKey> visited, int remainingDepth) {
		if (keyService != null && remainingDepth > 0 && visited.add(key)) {
			Set<PGPPublicKey> certifications = keyService.getVerifiedCertifications(key);
			if (certifications != null && !certifications.isEmpty()) {
				List<TreeNode> children = new ArrayList<>();
				for (PGPPublicKey certifyingKey : certifications) {
					if (visited.add(certifyingKey)) {
						Date verifiedRevocationDate = keyService.getVerifiedRevocationDate(key);
						TreeNode treeNode = new ExtendedTreeNode(certifyingKey, null, verifiedRevocationDate);
						children.add(treeNode);
					}
				}

				if (!children.isEmpty()) {
					result.setChildren(children.toArray(TreeNode[]::new));
					children.forEach(child -> {
						child.setParent(result);
						PGPPublicKey certifyingKey = (PGPPublicKey) child.getValue();
						visited.remove(certifyingKey);
						expandChildren(child, certifyingKey, keyService, visited, remainingDepth - 1);
						visited.add(certifyingKey);
					});
				}
			}
		}
	}

	@Override
	public AuthenticationInfo getUsernamePassword(final String location, final AuthenticationInfo previousInfo) {
		final AuthenticationInfo[] result = new AuthenticationInfo[1];
		if (!suppressAuthentication() && !isHeadless()) {
			getDisplay().syncExec(() -> {
				Shell shell = shellProvider.getShell();
				String message = null;
				if (previousInfo.saveResult())
					message = NLS.bind(ProvUIMessages.ProvUIMessages_SavedNotAccepted_EnterFor_0, location);
				else
					message = NLS.bind(ProvUIMessages.ProvUIMessages_NotAccepted_EnterFor_0, location);

				UserValidationDialog dialog = new UserValidationDialog(previousInfo, shell,
						ProvUIMessages.ServiceUI_LoginRequired, null, message);
				int dialogCode = dialog.open();
				if (dialogCode == Window.OK) {
					result[0] = dialog.getResult();
				} else if (dialogCode == Window.CANCEL) {
					result[0] = AUTHENTICATION_PROMPT_CANCELED;
				}
			});
		}
		return result[0];
	}

	@Override
	public void showInformationMessage(final String title, final String text, final String linkText) {
		if (isHeadless()) {
			super.showInformationMessage(title, text, linkText);
			return;
		}
		getDisplay().syncExec(() -> {
			MessageDialog dialog = new MessageDialogWithLink(shellProvider.getShell(), title, null, text,
					MessageDialog.INFORMATION, new String[] { IDialogConstants.OK_LABEL }, 0, linkText, linkHandler);
			dialog.open();
		});
	}

	private static class OriginTreeNode extends TreeNode implements IAdaptable, Comparable<OriginTreeNode> {

		private final Set<IInstallableUnit> ius = new TreeSet<>();

		private final List<Certificate> certificates = new ArrayList<>();

		private final List<OriginTreeNode> children = new ArrayList<>();

		public OriginTreeNode(URI value, List<Certificate> certificates) {
			super(value);
			if (certificates != null) {
				this.certificates.addAll(certificates);
			}
		}

		@Override
		public int compareTo(OriginTreeNode o) {
			return ((URI) getValue()).compareTo(((URI) o.getValue()));
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			if (adapter == X509Certificate.class && !certificates.isEmpty()) {
				if (certificates.get(0) instanceof X509Certificate certificate) {
					return adapter.cast(certificate);
				}
			}
			if (adapter.equals(Certificate[].class)) {
				return adapter.cast(certificates.toArray(Certificate[]::new));
			}
			if (adapter.equals(IInstallableUnit[].class)) {
				return adapter.cast(ius.toArray(IInstallableUnit[]::new));
			}
			return null;
		}

		public Set<IInstallableUnit> getIUs() {
			return ius;
		}

		public void setParent(OriginTreeNode parent) {
			if (parent != getParent()) {
				super.setParent(parent);
				parent.children.add(this);
				children.sort(null);
			}
		}

		@Override
		public TreeNode[] getChildren() {
			return children.toArray(TreeNode[]::new);
		}

		@Override
		public boolean hasChildren() {
			return !children.isEmpty();
		}
	}

	@Override
	public TrustAuthorityInfo getTrustAuthorityInfo(Map<URI, Set<IInstallableUnit>> siteIUs,
			Map<URI, List<Certificate>> authorityCertificates) {
		var trustedAuthorities = new TreeSet<URI>();
		var persistTrust = new AtomicBoolean();
		var trustAlways = new AtomicBoolean();
		if (!isHeadless()) {
			var nodes = new TreeMap<URI, OriginTreeNode>();
			for (var entry : siteIUs.entrySet()) {
				URI location = entry.getKey();
				var authorityChain = AuthorityChecker.getAuthorityChain(location);
				if (authorityChain.size() > 3) {
					authorityChain.subList(2, authorityChain.size() - 1).clear();
				}
				var ius = entry.getValue();
				var certificates = authorityCertificates.get(location);
				OriginTreeNode parent = null;
				for (var uri : authorityChain) {
					var treeNode = nodes.computeIfAbsent(uri, key -> new OriginTreeNode(key, certificates));
					treeNode.getIUs().addAll(ius);
					treeNode.setParent(parent);
					parent = treeNode;
				}
			}

			if (!nodes.isEmpty()) {
				nodes.values().removeIf(node -> node.getParent() != null);
				getDisplay().syncExec(() -> {
					var shell = shellProvider.getShell();
					var trustCertificateDialog = new TrustAuthorityDialog(shell,
							nodes.values().toArray(TreeNode[]::new));
					if (trustCertificateDialog.open() == Window.OK) {
						URI[] dialogResult = trustCertificateDialog.getResult();
						if (dialogResult != null) {
							trustedAuthorities.addAll(Arrays.asList(dialogResult));
						}
						persistTrust.set(trustCertificateDialog.isRememberSelectedAuthorities());
						trustAlways.set(trustCertificateDialog.isTrustAlways());
					}
				});
			}
		}

		return new TrustAuthorityInfo(trustedAuthorities, persistTrust.get(), trustAlways.get());
	}

	/**
	 * Returns a handler (used by
	 * {@link #showInformationMessage(String, String, String)}) to open a link in an
	 * external browser.
	 *
	 * @return a handler to open a link in an external browser.
	 *
	 * @since 2.7.400
	 */
	public Consumer<String> getLinkHandler() {
		return linkHandler;
	}

	/**
	 * Sets the handler used by
	 * {@link #showInformationMessage(String, String, String)} to open a link in an
	 * external browser.
	 *
	 * @param linkHandler the handler for opening links in an external browser.
	 *
	 * @since 2.7.400
	 */
	public void setLinkHandler(Consumer<String> linkHandler) {
		this.linkHandler = linkHandler;
	}

	/**
	 * Returns a shell that is appropriate to use as the parent for a modal dialog
	 * about to be opened.
	 *
	 * @return a shell that is appropriate to use as the parent for a modal dialog
	 *         about to be opened.
	 *
	 * @see ProvUI#getDefaultParentShell()
	 *
	 * @since 2.7.400
	 */
	public IShellProvider getShellProvider() {
		return shellProvider;
	}

	/**
	 * Set the provider that yields a shell that is appropriate to use as the parent
	 * for a modal dialog about to be opened.
	 *
	 * @param shellProvider the provider of a shell that is appropriate to use as
	 *                      the parent for a modal dialog about to be opened.
	 */
	public void setShellProvider(IShellProvider shellProvider) {
		this.shellProvider = shellProvider;
	}

	/**
	 * Returns the display of the workbench or the display set by
	 * {@link #setDisplay(Display)} in a non-workbench application.
	 *
	 * @since 2.7.400
	 */
	public Display getDisplay() {
		if (display == null && PlatformUI.isWorkbenchRunning()) {
			display = PlatformUI.getWorkbench().getDisplay();
		}
		return display;
	}

	/**
	 * Can be called once to set the display in a non-workbench application.
	 *
	 * @throws IllegalStateException if there is a workbench running or the display
	 *                               has already been set differently.
	 *
	 * @since 2.7.400
	 */
	public void setDisplay(Display display) {
		if (this.display != null && this.display != display) {
			throw new IllegalStateException("Cannot change the display"); //$NON-NLS-1$
		}
		this.display = display;
	}

	private boolean isHeadless() {
		// If there is no UI available and we are still the IServiceUI,
		// assume that the operation should proceed. See
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=291049
		//
		// But we shouldn't just assume that no workbench mean no display and no head.
		// It should be possible to reuse this class in an application without a
		// workbench, e.g., the Eclipse Installer.
		return getDisplay() == null;
	}

	private static class ExtendedTreeNode extends TreeNode implements IAdaptable {
		private final Set<IArtifactKey> artifacts;
		private Date revocationDate;

		public ExtendedTreeNode(Object value) {
			super(value);
			artifacts = null;
		}

		public ExtendedTreeNode(Object value, Set<IArtifactKey> artifacts) {
			this(value, artifacts, null);
		}

		public ExtendedTreeNode(Object value, Set<IArtifactKey> artifacts, Date revocationDate) {
			super(value);
			this.artifacts = artifacts;
			this.revocationDate = revocationDate;
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			if (adapter.isInstance(this)) {
				return adapter.cast(this);
			}
			if (adapter.isInstance(getValue())) {
				return adapter.cast(value);
			}
			if (adapter == IArtifactKey[].class && artifacts != null) {
				return adapter.cast(artifacts.toArray(IArtifactKey[]::new));
			}
			if (adapter == Date.class) {
				return adapter.cast(revocationDate);
			}

			return null;
		}
	}

}
