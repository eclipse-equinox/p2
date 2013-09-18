/*******************************************************************************
 * Copyright (c) 2009, 2013 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     Sonatype, Inc. - added caching support
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.discovery.compatibility;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import org.eclipse.core.internal.registry.ExtensionRegistry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.discovery.*;
import org.eclipse.equinox.internal.p2.discovery.compatibility.Directory.Entry;
import org.eclipse.equinox.internal.p2.discovery.compatibility.util.TransportUtil;
import org.eclipse.equinox.internal.p2.discovery.compatibility.util.TransportUtil.TextContentProcessor;
import org.eclipse.osgi.util.NLS;

/**
 * A discovery strategy that downloads a simple directory of remote jars. The directory is first downloaded, then each
 * remote jar is downloaded.
 * 
 * @author David Green
 */
@SuppressWarnings("restriction")
public class RemoteBundleDiscoveryStrategy extends BundleDiscoveryStrategy {

	private String directoryUrl;

	private DiscoveryRegistryStrategy registryStrategy;

	private File temporaryStorage;

	private int maxDiscoveryJarDownloadAttempts = 1;

	@Override
	public void performDiscovery(IProgressMonitor monitor) throws CoreException {
		if (items == null || categories == null || directoryUrl == null || tags == null) {
			throw new IllegalStateException();
		}
		if (registryStrategy != null) {
			throw new IllegalStateException();
		}

		final int totalTicks = 100000;
		final int ticksTenPercent = totalTicks / 10;
		monitor.beginTask(Messages.RemoteBundleDiscoveryStrategy_task_remote_discovery, totalTicks);
		try {
			File registryCacheFolder;
			try {
				if (temporaryStorage != null && temporaryStorage.exists()) {
					delete(temporaryStorage);
				}
				temporaryStorage = File.createTempFile(RemoteBundleDiscoveryStrategy.class.getSimpleName(), ".tmp"); //$NON-NLS-1$
				temporaryStorage.delete();
				if (!temporaryStorage.mkdirs()) {
					throw new IOException();
				}
				registryCacheFolder = new File(temporaryStorage, ".rcache"); //$NON-NLS-1$
				if (!registryCacheFolder.mkdirs()) {
					throw new IOException();
				}
			} catch (IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN, Messages.RemoteBundleDiscoveryStrategy_io_failure_temp_storage, e));
			}
			if (monitor.isCanceled()) {
				return;
			}

			Directory directory;
			try {
				final Directory[] temp = new Directory[1];
				TransportUtil.readResource(new URI(directoryUrl), new TextContentProcessor() {
					public void process(Reader reader) throws IOException {
						DirectoryParser parser = new DirectoryParser();
						temp[0] = parser.parse(reader);
					}
				}, new SubProgressMonitor(monitor, ticksTenPercent));
				directory = temp[0];
				if (directory == null) {
					throw new IllegalStateException();
				}
			} catch (UnknownHostException e) {
				throw new CoreException(new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN, NLS.bind(Messages.RemoteBundleDiscoveryStrategy_unknown_host_discovery_directory, e.getMessage()), e));
			} catch (URISyntaxException e) {
				throw new CoreException(new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN, NLS.bind(Messages.RemoteBundleDiscoveryStrategy_Invalid_source_specified_Error, directoryUrl), e));
			} catch (IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN, Messages.RemoteBundleDiscoveryStrategy_io_failure_discovery_directory, e));
			}
			if (monitor.isCanceled()) {
				return;
			}
			if (directory.getEntries().isEmpty()) {
				throw new CoreException(new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN, Messages.RemoteBundleDiscoveryStrategy_empty_directory));
			}

			Map<File, Directory.Entry> bundleFileToDirectoryEntry = new HashMap<File, Directory.Entry>();

			ExecutorService executorService = createExecutorService(directory.getEntries().size());
			try {
				List<Future<DownloadBundleJob>> futures = new ArrayList<Future<DownloadBundleJob>>();
				// submit jobs
				for (Directory.Entry entry : directory.getEntries()) {
					futures.add(executorService.submit(new DownloadBundleJob(entry, monitor)));
				}
				int futureSize = ticksTenPercent * 4 / directory.getEntries().size();
				// collect job results
				for (Future<DownloadBundleJob> job : futures) {
					try {
						DownloadBundleJob bundleJob;
						for (;;) {
							try {
								bundleJob = job.get(1L, TimeUnit.SECONDS);
								break;
							} catch (TimeoutException e) {
								if (monitor.isCanceled()) {
									return;
								}
							}
						}
						if (bundleJob.file != null) {
							bundleFileToDirectoryEntry.put(bundleJob.file, bundleJob.entry);
						}
						monitor.worked(futureSize);
					} catch (ExecutionException e) {
						Throwable cause = e.getCause();
						if (cause instanceof OperationCanceledException) {
							monitor.setCanceled(true);
							return;
						}
						IStatus status;
						if (cause instanceof CoreException) {
							status = ((CoreException) cause).getStatus();
						} else {
							status = new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN, Messages.RemoteBundleDiscoveryStrategy_unexpectedError, cause);
						}
						// log errors but continue on
						LogHelper.log(status);
					} catch (InterruptedException e) {
						monitor.setCanceled(true);
						return;
					}
				}
			} finally {
				executorService.shutdownNow();
			}

			try {
				registryStrategy = new DiscoveryRegistryStrategy(new File[] {registryCacheFolder}, new boolean[] {false}, this);
				registryStrategy.setBundles(bundleFileToDirectoryEntry);
				IExtensionRegistry extensionRegistry = new ExtensionRegistry(registryStrategy, this, this);
				try {
					IExtensionPoint extensionPoint = extensionRegistry.getExtensionPoint(ConnectorDiscoveryExtensionReader.EXTENSION_POINT_ID);
					if (extensionPoint != null) {
						IExtension[] extensions = extensionPoint.getExtensions();
						if (extensions.length > 0) {
							processExtensions(new SubProgressMonitor(monitor, ticksTenPercent * 3), extensions);
						}
					}
				} finally {
					extensionRegistry.stop(this);
				}
			} finally {
				registryStrategy = null;
			}
		} finally {
			monitor.done();
		}
	}

	private class DownloadBundleJob implements Callable<DownloadBundleJob> {
		private final IProgressMonitor monitor;

		private final Entry entry;

		private File file;

		public DownloadBundleJob(Entry entry, IProgressMonitor monitor) {
			this.entry = entry;
			this.monitor = monitor;
		}

		public DownloadBundleJob call() {

			String bundleUrl = entry.getLocation();
			for (int attemptCount = 0; attemptCount < maxDiscoveryJarDownloadAttempts; ++attemptCount) {
				try {
					File target = File.createTempFile(TransportUtil.getFileNameFor(bundleUrl) + "_", ".jar", temporaryStorage); //$NON-NLS-1$ //$NON-NLS-2$

					if (monitor.isCanceled()) {
						break;
					}

					TransportUtil.downloadResource(new URI(bundleUrl), target, new NullProgressMonitor() {
						@Override
						public boolean isCanceled() {
							return super.isCanceled() || monitor.isCanceled();
						}
					}/*don't use sub progress monitor here*/);
					file = target;
				} catch (URISyntaxException e) {
					LogHelper.log(new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN, NLS.bind(Messages.RemoteBundleDiscoveryStrategy_Invalid_source_specified_Error, bundleUrl), e));
				} catch (IOException e) {
					LogHelper.log(new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN, NLS.bind(Messages.RemoteBundleDiscoveryStrategy_cannot_download_bundle, bundleUrl, e.getMessage()), e));
					if (isUnknownHostException(e)) {
						break;
					}
				} catch (CoreException e) {
					LogHelper.log(new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN, NLS.bind(Messages.RemoteBundleDiscoveryStrategy_cannot_download_bundle, bundleUrl, e.getMessage()), e));
				}
			}
			return this;
		}
	}

	private ExecutorService createExecutorService(int size) {
		final int maxThreads = 4;
		return Executors.newFixedThreadPool(Math.min(size, maxThreads));
	}

	/**
	 * walk the exception chain to determine if the given exception or any of its underlying causes are an
	 * {@link UnknownHostException}.
	 * 
	 * @return true if the exception or one of its causes are {@link UnknownHostException}.
	 */
	private boolean isUnknownHostException(Throwable t) {
		while (t != null) {
			if (t instanceof UnknownHostException) {
				return true;
			}
			Throwable t2 = t.getCause();
			if (t2 == t) {
				break;
			}
			t = t2;
		}
		return false;
	}

	private void delete(File file) {
		if (file.exists()) {
			if (file.isDirectory()) {
				File[] children = file.listFiles();
				if (children != null) {
					for (File child : children) {
						delete(child);
					}
				}
			}
			if (!file.delete()) {
				// fail quietly
			}
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		if (temporaryStorage != null) {
			delete(temporaryStorage);
		}
	}

	public String getDirectoryUrl() {
		return directoryUrl;
	}

	public void setDirectoryUrl(String directoryUrl) {
		this.directoryUrl = directoryUrl;
	}

	@Override
	protected AbstractCatalogSource computeDiscoverySource(IContributor contributor) {
		Entry directoryEntry = registryStrategy.getDirectoryEntry(contributor);
		Policy policy = new Policy(directoryEntry.isPermitCategories());
		JarDiscoverySource discoverySource = new JarDiscoverySource(contributor.getName(), registryStrategy.getJarFile(contributor));
		discoverySource.setPolicy(policy);
		return discoverySource;
	}

	/**
	 * indicate how many times discovyer jar downloads should be attempted
	 */
	public int getMaxDiscoveryJarDownloadAttempts() {
		return maxDiscoveryJarDownloadAttempts;
	}

	/**
	 * indicate how many times discovyer jar downloads should be attempted
	 * 
	 * @param maxDiscoveryJarDownloadAttempts
	 *            a number >= 1
	 */
	public void setMaxDiscoveryJarDownloadAttempts(int maxDiscoveryJarDownloadAttempts) {
		if (maxDiscoveryJarDownloadAttempts < 1 || maxDiscoveryJarDownloadAttempts > 2) {
			throw new IllegalArgumentException();
		}
		this.maxDiscoveryJarDownloadAttempts = maxDiscoveryJarDownloadAttempts;
	}
}
