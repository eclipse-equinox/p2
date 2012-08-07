/*******************************************************************************
 * Copyright (c) 2012 Wind River and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Wind River - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.repository;

import java.io.*;
import java.net.URI;
import java.util.Date;
import java.util.EventObject;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.repository.DownloadProgressEvent;
import org.eclipse.equinox.internal.p2.transport.ecf.FileReader;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.ProvisioningListener;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.junit.Test;

public class FileReaderTest2 extends AbstractProvisioningTest {

	abstract class PauseJob extends Job {

		private FileReader reader;

		public PauseJob(String name, FileReader reader) {
			super(name);
			this.reader = reader;
		}

		public FileReader getReader() {
			return reader;
		}
	}

	@Test
	public void testPauseAndResume() throws IOException, CoreException {
		IProvisioningEventBus eventBus = getEventBus();
		class PauseResumeProvisioningListener implements ProvisioningListener {
			boolean downloadIsOngoing = false;
			boolean isPaused = false;
			float downloadProgressEventAfterPaused = 0;

			public void notify(EventObject event) {
				if (event instanceof DownloadProgressEvent) {
					downloadIsOngoing = true;
					if (isPaused) {
						downloadProgressEventAfterPaused++;
					}
				}
			}
		}
		final PauseResumeProvisioningListener listener = new PauseResumeProvisioningListener();
		eventBus.addListener(listener);
		try {
			final org.eclipse.equinox.internal.p2.transport.ecf.FileReader reader = new org.eclipse.equinox.internal.p2.transport.ecf.FileReader(getAgent(), null);
			final Job resumeJob = new Job("resume") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					listener.isPaused = false;
					System.out.println("Download job is resumed at " + new Date());
					reader.resume();
					return Status.OK_STATUS;
				}
			};
			PauseJob pauseJob = new PauseJob("pause", reader) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					reader.pause();
					// wait for actual downloading thread is paused.
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					listener.isPaused = true;
					System.out.println("Download job is paused at " + new Date());
					resumeJob.schedule(10000);
					return Status.OK_STATUS;
				}
			};
			doFileReaderTest(pauseJob, null);
			assertTrue("No download progress event is fired!", listener.downloadIsOngoing);
			assertEquals("Download is not paused!", 0, listener.downloadProgressEventAfterPaused, 1);
		} finally {
			eventBus.removeListener(listener);
		}
	}

	@Test
	public void testPauseAndResumeMoreThanOnce() throws IOException, CoreException {
		final org.eclipse.equinox.internal.p2.transport.ecf.FileReader reader = new org.eclipse.equinox.internal.p2.transport.ecf.FileReader(getAgent(), null);
		abstract class ResumeJob extends Job {
			PauseJob pausejob;

			public ResumeJob(String name, PauseJob pauseJob) {
				super(name);
				this.pausejob = pauseJob;
			}
		}

		final PauseJob pauseJob = new PauseJob("pause", reader) {
			int count = 0;
			final int threhold = 3;

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				reader.pause();
				// wait for actual downloading thread is paused.
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("Download job is paused at " + new Date());
				final ResumeJob resumeJob = new ResumeJob("resume", this) {

					@Override
					protected IStatus run(IProgressMonitor monitor1) {
						System.out.println("Download job is resumed at " + new Date());
						reader.resume();
						if (count++ < threhold)
							this.pausejob.schedule(5000);
						return Status.OK_STATUS;
					}
				};
				resumeJob.schedule(10000);
				return Status.OK_STATUS;
			}
		};
		doFileReaderTest(pauseJob, null);
	}

	@Test
	public void testCancelPausedDownload() throws IOException, CoreException {
		final org.eclipse.equinox.internal.p2.transport.ecf.FileReader reader = new org.eclipse.equinox.internal.p2.transport.ecf.FileReader(getAgent(), null);
		PauseJob pauseJob = new PauseJob("pause", reader) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				reader.pause();
				// wait for actual downloading thread is paused.
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("Download job is paused at " + new Date());
				return Status.OK_STATUS;
			}
		};
		try {
			class CancelDownloadListener extends JobChangeAdapter {
				boolean cancelDownload = false;

				@Override
				public void done(IJobChangeEvent event) {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					cancelDownload = true;
				}
			}
			final CancelDownloadListener pauseJobListener = new CancelDownloadListener();
			pauseJob.addJobChangeListener(pauseJobListener);
			doFileReaderTest(pauseJob, new NullProgressMonitor() {
				@Override
				public boolean isCanceled() {
					return pauseJobListener.cancelDownload;
				}
			});
			fail("Don't throw operation cancel exception.");
		} catch (OperationCanceledException e) {
			// expected
		}
	}

	private void doFileReaderTest(final PauseJob pauseJob, IProgressMonitor monitor) throws IOException, CoreException {
		final String testRemoteFileURL = "http://download.eclipse.org/releases/juno/201206270900/content.jar";
		OutputStream out = null;
		OutputStream out1 = null;
		File tmpFolder = getTempFolder();
		File tmpFile = new File(tmpFolder, "testDownloadPauseResume.zip");
		File tmpFile1 = new File(tmpFolder, "testDownloadWithoutPause.zip");
		ProvisioningListener listener = new ProvisioningListener() {
			boolean startedPauseJob = false;

			public void notify(EventObject o) {
				if (!startedPauseJob && o instanceof DownloadProgressEvent) {
					pauseJob.schedule();
					startedPauseJob = true;
				}

			}
		};
		getEventBus().addListener(listener);
		try {
			tmpFile1.createNewFile();
			out1 = new FileOutputStream(tmpFile1);
			FileReader readerWithoutPausing = new FileReader(null, null);
			readerWithoutPausing.readInto(URI.create(testRemoteFileURL), out1, null);
			assertNotNull(readerWithoutPausing.getResult());
			assertTrue(readerWithoutPausing.getResult().isOK());
			tmpFile.createNewFile();
			out = new FileOutputStream(tmpFile);
			FileReader reader = pauseJob.getReader();
			reader.readInto(URI.create(testRemoteFileURL), out, monitor);
			assertNotNull(reader.getResult());
			assertTrue(reader.getResult().isOK());
			assertEquals("File with pausing/resuming is not identical with file without pausing.", tmpFile1.length(), tmpFile.length());
		} finally {
			getEventBus().removeListener(listener);
			if (out1 != null)
				out1.close();
			tmpFile1.delete();
			if (out != null)
				out.close();
			tmpFile.delete();
			delete(tmpFolder);
		}
	}
}
