/*******************************************************************************
 * Copyright (c) 2022 Eclipse contributors and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.repository;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.function.*;
import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.gpg.keybox.*;
import org.bouncycastle.gpg.keybox.jcajce.JcaKeyBoxBuilder;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.internal.p2.repository.helpers.DebugHelper;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.spi.PGPPublicKeyService;

/**
 * @since 2.6
 */
public class DefaultPGPPublicKeyService extends PGPPublicKeyService {

	/**
	 * Enable debug tracing either via debug options or via a system property.
	 */
	private static final boolean DEBUG_KEY_SERVICE = DebugHelper.DEBUG_KEY_SERVICE
			|| Boolean.TRUE.toString().equalsIgnoreCase(System.getProperty("p2.keyserver.debug")); //$NON-NLS-1$

	/**
	 * The system property used to initialized the {@link #keyServer}.
	 */
	private static final String KEY_SERVERS_PROPERTY = "p2.keyservers"; //$NON-NLS-1$

	/**
	 * The system property used to determine where to look for the GPG pubring.
	 *
	 * @see #getGPPDirectory()
	 */
	private static final String GPG_HOME_PROPERTY = "p2.gpg.home"; //$NON-NLS-1$

	/**
	 * The system property used to determine whether to enable GPG pubring lookup.
	 *
	 * @see #gpg
	 * @see #setGPG(boolean)
	 */
	private static final String GPG_PROPERTY = "p2.gpg"; //$NON-NLS-1$

	/**
	 * The number of elapsed milliseconds after which keys cached from a key server
	 * are considered stale such that they will be re-fetched if possible.
	 */
	private static final long STALE_AFTER_MILLIS = Long.getLong("p2.keyserver.cache.stale", 24) * 1000 * 60 * 60; //$NON-NLS-1$

	/**
	 * Reuse p2's transport layer for fetching keys from the key server.
	 */
	private final Transport transport;

	/**
	 * Keys {@link #addKey(PGPPublicKey) added} to this key service are cached via
	 * this map.
	 */
	private final Map<Long, LocalKeyCache> localKeys = new LinkedHashMap<>();

	/**
	 * A folder with locally cached keys, indexed on {@link PGPPublicKey#getKeyID()
	 * key ID}.
	 */
	private final Path keyCache;

	/**
	 * The current key servers.
	 */
	private final Map<String, PGPKeyServer> keyServers = new LinkedHashMap<>();

	/**
	 * Whether to load from GPG's pubring.
	 */
	private boolean gpg;

	/**
	 * Creates an instance associated with the given agent.
	 *
	 * @param agent the agent for which a key service is provided.
	 */
	public DefaultPGPPublicKeyService(IProvisioningAgent agent) {
		IAgentLocation agentLocation = agent.getService(IAgentLocation.class);
		URI dataArea = agentLocation.getDataArea(org.eclipse.equinox.internal.p2.repository.Activator.ID);
		keyCache = Paths.get(dataArea).resolve("pgp"); //$NON-NLS-1$
		try {
			Files.createDirectories(keyCache);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		if (DEBUG_KEY_SERVICE) {
			DebugHelper.debug("KeyServer", "Cache", "location", keyCache); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		String keyServersProperty = System.getProperty(KEY_SERVERS_PROPERTY, ""); //$NON-NLS-1$
		if (!keyServersProperty.isBlank()) {
			Set<String> keyServersSet = new LinkedHashSet<>();
			for (String keyServer : keyServersProperty.split("[,; \t]+")) { //$NON-NLS-1$
				if (!keyServer.isEmpty()) {
					keyServersSet.add(keyServer);
				}
			}

			setKeyServers(keyServersSet);
		}

		setGPG(Boolean.TRUE.toString().equalsIgnoreCase(System.getProperty(GPG_PROPERTY, Boolean.TRUE.toString()))
				|| !System.getProperty(GPG_HOME_PROPERTY, "").isBlank()); //$NON-NLS-1$

		transport = agent.getService(Transport.class);
	}

	public Set<String> getKeyServers() {
		return Collections.unmodifiableSet(keyServers.keySet());
	}

	public void setKeyServers(Set<String> keyServers) {
		Map<String, PGPKeyServer> newKeyServers = new LinkedHashMap<>();
		for (String keyServer : keyServers) {
			PGPKeyServer pgpKeyServer = this.keyServers.get(keyServer);
			if (pgpKeyServer == null) {
				pgpKeyServer = new PGPKeyServer(keyServer, this.keyCache) {
					@Override
					protected boolean isStale(Path path) {
						return DefaultPGPPublicKeyService.this.isStale(path);
					}

					@Override
					protected IStatus download(URI uri, OutputStream receiver, IProgressMonitor monitor) {
						return DefaultPGPPublicKeyService.this.download(uri, receiver, monitor);
					}

					@Override
					protected void log(Throwable throwable) {
						DefaultPGPPublicKeyService.this.log(throwable);
					}
				};
			}
			newKeyServers.put(keyServer, pgpKeyServer);
		}

		this.keyServers.clear();
		this.keyServers.putAll(newKeyServers);
	}

	@Override
	public PGPPublicKey getKey(String fingerprint) {
		int length = fingerprint.length();
		if (length >= 16) {
			long keyID = Long.parseUnsignedLong(fingerprint.substring(length - 16, length), 16);
			Collection<PGPPublicKey> keys = getKeys(keyID);
			for (PGPPublicKey key : keys) {
				if (toHexFingerprint(key).equalsIgnoreCase(fingerprint)) {
					return key;
				}
			}
		}
		return null;
	}

	@Override
	public Collection<PGPPublicKey> getKeys(long keyID) {
		List<PGPPublicKey> keys = new ArrayList<>();
		for (PGPKeyServer keyServer : keyServers.values()) {
			keys.addAll(keyServer.getKeys(keyID));
		}

		keys.addAll(getLocalKeyCache(keyID).get());

		keys.addAll(getDefaultKeys(keyID));

		return reconcileKeys(keys);
	}

	public boolean isGGP() {
		return gpg;
	}

	public void setGPG(boolean gpg) {
		this.gpg = gpg;
	}

	protected List<PGPPublicKey> getDefaultKeys(long keyID) {
		return gpg ? getGPGPubringKeys(keyID) : Collections.emptyList();
	}

	protected List<PGPPublicKey> reconcileKeys(List<PGPPublicKey> keys) {
		if (keys.size() <= 1) {
			return new ArrayList<>(keys);
		}

		Map<ByteBuffer, PGPPublicKey> encodings = new LinkedHashMap<>();
		Map<ByteBuffer, PGPPublicKey> fingerprints = new LinkedHashMap<>();
		for (PGPPublicKey key : keys) {
			try {
				ByteBuffer encoding = ByteBuffer.wrap(key.getEncoded());
				PGPPublicKey existingKey = encodings.put(encoding, key);
				if (existingKey == null) {
					ByteBuffer fingerprint = ByteBuffer.wrap(key.getFingerprint());
					PGPPublicKey otherKey = fingerprints.put(fingerprint, key);
					if (otherKey != null) {
						fingerprints.put(fingerprint, choose(otherKey, key));
					}
				}
			} catch (IOException e) {
				log(e);
			}
		}

		return new ArrayList<>(fingerprints.values());
	}

	/**
	 * While {@link #reconcileKeys(List) reconciling}, when two keys have the same
	 * fingerprint, this method must be chosen in favor of the other to be retained
	 * in the result.
	 *
	 * @param key1 the first key from which to choose.
	 * @param key2 the second key from which to choose.
	 * @return the key with the newest or most complete details.
	 */
	protected PGPPublicKey choose(PGPPublicKey key1, PGPPublicKey key2) {
		// Favor the one with the newest information.
		long signatureTime1 = getNewestSignature(key1);
		long signatureTime2 = getNewestSignature(key2);
		if (signatureTime1 > signatureTime2) {
			return key1;
		} else if (signatureTime1 < signatureTime2) {
			return key2;
		}

		// Favor the one with the most information.
		int signatureCount1 = getSignatureCount(key1);
		int signatureCount2 = getSignatureCount(key2);
		if (signatureCount1 > signatureCount2) {
			return key1;
		} else if (signatureCount1 < signatureCount2) {
			return key2;
		}

		return key1;
	}

	protected static int getSignatureCount(PGPPublicKey key) {
		int result = 0;
		for (Iterator<PGPSignature> signatures = key.getSignatures(); signatures.hasNext(); signatures.next()) {
			++result;
		}
		for (Iterator<PGPSignature> signatures = key.getKeySignatures(); signatures.hasNext(); signatures.next()) {
			++result;
		}
		return result;
	}

	protected static long getNewestSignature(PGPPublicKey key) {
		long result = 0;
		for (Iterator<PGPSignature> signatures = key.getSignatures(); signatures.hasNext();) {
			PGPSignature signature = signatures.next();
			long time = signature.getCreationTime().getTime();
			result = Math.max(result, time);
		}
		for (Iterator<PGPSignature> signatures = key.getKeySignatures(); signatures.hasNext();) {
			PGPSignature signature = signatures.next();
			long time = signature.getCreationTime().getTime();
			result = Math.max(result, time);
		}

		return result;
	}

	@Override
	public PGPPublicKey addKey(PGPPublicKey key) {
		long keyID = key.getKeyID();
		LocalKeyCache localKeyCache = getLocalKeyCache(keyID);
		localKeyCache.add(key);

		Collection<PGPPublicKey> keys = getKeys(keyID);
		byte[] fingerprint = key.getFingerprint();
		for (PGPPublicKey otherKey : keys) {
			if (Arrays.equals(otherKey.getFingerprint(), fingerprint)) {
				return otherKey;
			}
		}

		// We should never get this far.
		return key;
	}

	protected boolean isStale(Path path) {
		try {
			FileTime lastModifiedTime = Files.getLastModifiedTime(path);
			long lastModified = lastModifiedTime.toMillis();
			long currentTime = System.currentTimeMillis();
			return currentTime - lastModified > STALE_AFTER_MILLIS;
		} catch (IOException e) {
			return true;
		}
	}

	@Override
	public Set<PGPPublicKey> getVerifiedCertifications(PGPPublicKey key) {
		Set<PGPPublicKey> certifications = new LinkedHashSet<>();
		LOOP: for (Iterator<PGPSignature> signatures = key.getSignatures(); signatures.hasNext();) {
			PGPSignature signature = signatures.next();
			long signingKeyID = signature.getKeyID();
			for (PGPPublicKey signingKey : getKeys(signingKeyID)) {
				switch (signature.getSignatureType()) {
				case PGPSignature.SUBKEY_BINDING:
				case PGPSignature.PRIMARYKEY_BINDING: {
					try {
						signature.init(new BcPGPContentVerifierBuilderProvider(), signingKey);
						if (signature.verifyCertification(signingKey, key)
								&& isCreatedBeforeRevocation(signature, signingKey)) {
							certifications.add(signingKey);
							continue LOOP;
						}
					} catch (PGPException e) {
						//$FALL-THROUGH$
					}
					break;
				}
				case PGPSignature.DEFAULT_CERTIFICATION:
				case PGPSignature.NO_CERTIFICATION:
				case PGPSignature.CASUAL_CERTIFICATION:
				case PGPSignature.POSITIVE_CERTIFICATION: {
					for (Iterator<String> userIDs = key.getUserIDs(); userIDs.hasNext();) {
						String userID = userIDs.next();
						try {
							signature.init(new BcPGPContentVerifierBuilderProvider(), signingKey);
							if (signature.verifyCertification(userID, key)
									&& isCreatedBeforeRevocation(signature, signingKey)) {
								certifications.add(signingKey);
								continue LOOP;
							}
						} catch (PGPException e) {
							//$FALL-THROUGH$
						}
					}
					break;
				}
				}
			}
		}
		return certifications;
	}

	@Override
	public Date getVerifiedRevocationDate(PGPPublicKey key) {
		for (Iterator<PGPSignature> signatures = key.getSignatures(); signatures.hasNext();) {
			PGPSignature signature = signatures.next();
			long signingKeyID = signature.getKeyID();
			for (PGPPublicKey signingKey : getKeys(signingKeyID)) {
				switch (signature.getSignatureType()) {
				case PGPSignature.KEY_REVOCATION:
				case PGPSignature.CERTIFICATION_REVOCATION: {
					try {
						signature.init(new BcPGPContentVerifierBuilderProvider(), signingKey);
						if (signature.verifyCertification(key)) {
							return signature.getCreationTime();
						}
					} catch (PGPException e) {
						// https://bugs.eclipse.org/bugs/show_bug.cgi?id=581453
						// When something goes wrong, assume that it's revoked.
						return new Date(0);
					}
					break;
				}
				}
			}
		}

		return null;
	}

	private LocalKeyCache getLocalKeyCache(long keyID) {
		LocalKeyCache localKeyCache = localKeys.get(keyID);
		if (localKeyCache == null) {
			String hexKeyID = toHex(keyID);
			Path cache = keyCache.resolve(hexKeyID + ".asc"); //$NON-NLS-1$
			localKeyCache = new LocalKeyCache(cache) {
				@Override
				protected List<PGPPublicKey> reconcileKeys(List<PGPPublicKey> keys) {
					return DefaultPGPPublicKeyService.this.reconcileKeys(keys);
				}

				@Override
				protected void log(Throwable throwable) {
					DefaultPGPPublicKeyService.this.log(throwable);
				}
			};
			localKeys.put(keyID, localKeyCache);
		}
		return localKeyCache;
	}

	protected Collection<PGPPublicKey> fetchKeys(URI uri, Path cache) throws IOException {
		try {
			ByteArrayOutputStream reciever = new ByteArrayOutputStream();
			IStatus download = download(uri, reciever, new NullProgressMonitor());
			if (!download.isOK()) {
				Throwable exception = download.getException();
				if (exception != null) {
					throw new IOException(download.getMessage(), exception);
				}
				throw new IOException(download.getMessage());
			}
			List<PGPPublicKey> result = new ArrayList<>();
			byte[] bytes = reciever.toByteArray();
			try (InputStream input = new ArmoredInputStream(new ByteArrayInputStream(bytes))) {
				result.addAll(loadKeys(input));
			}

			try (OutputStream out = newAtomicOutputStream(cache)) {
				out.write(bytes);
			}

			return result;
		} catch (IOException ex) {
			if (Files.isRegularFile(cache)) {
				try (InputStream input = new ArmoredInputStream(new BufferedInputStream(Files.newInputStream(cache)))) {
					return loadKeys(input);
				} catch (IOException ex1) {
					try {
						// Assume the cache is corrupt so delete it.
						Files.delete(cache);
					} catch (IOException ex2) {
						log(ex2);
					}
					// Rethrow original network failure exception
					throw new IOException("Error while processing " + uri + " as well while processing the cache " //$NON-NLS-1$ //$NON-NLS-2$
							+ cache + ": " + ex1.getMessage(), ex); //$NON-NLS-1$
				}
			}
			throw new IOException("Error while processing " + uri, ex); //$NON-NLS-1$
		}
	}

	protected IStatus download(URI uri, OutputStream receiver, IProgressMonitor monitor) {
		return transport.download(uri, receiver, monitor);
	}

	protected void log(Throwable throwable) {
		if (DEBUG_KEY_SERVICE) {
			LogHelper.log(new Status(IStatus.ERROR, org.eclipse.equinox.internal.p2.repository.Activator.ID,
					throwable.getMessage(), throwable));
		}
	}

	protected static OutputStream newAtomicOutputStream(Path cache) throws IOException {
		Path temp = Files.createTempFile(cache.getParent(), "out", ".tmp"); //$NON-NLS-1$ //$NON-NLS-2$
		return new BufferedOutputStream(Files.newOutputStream(temp)) {
			@Override
			public void close() throws IOException {
				super.close();
				Files.move(temp, cache, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			}
		};
	}

	protected static List<PGPPublicKey> loadKeys(InputStream input) throws IOException {
		try {
			List<PGPPublicKey> result = new ArrayList<>();
			for (Object o : new JcaPGPObjectFactory(input)) {
				if (o instanceof PGPPublicKeyRingCollection) {
					collectKeys((PGPPublicKeyRingCollection) o, result::add);
				} else if (o instanceof PGPPublicKeyRing) {
					collectKeys((PGPPublicKeyRing) o, result::add);
				} else if (o instanceof PGPPublicKey) {
					result.add((PGPPublicKey) o);
				}
			}
			return result;
		} catch (RuntimeException ex) {
			throw new IOException(ex);
		}
	}

	private static void collectKeys(PGPPublicKeyRingCollection pgpPublicKeyRingCollection,
			Consumer<PGPPublicKey> collector) {
		pgpPublicKeyRingCollection.forEach(keyring -> collectKeys(keyring, collector));
	}

	private static void collectKeys(PGPPublicKeyRing pgpPublicKeyRing, Consumer<PGPPublicKey> collector) {
		pgpPublicKeyRing.getPublicKeys().forEachRemaining(collector::accept);
	}

	private static abstract class LocalKeyCache {
		private Path cache;
		private FileTime lastModifiedTime;
		private List<PGPPublicKey> keys;

		public LocalKeyCache(Path cache) {
			this.cache = cache;
		}

		protected abstract void log(Throwable throwable);

		protected abstract List<PGPPublicKey> reconcileKeys(List<PGPPublicKey> keysToReconcile);

		public List<PGPPublicKey> get() {
			if (keys != null) {
				try {
					FileTime newLastModifiedTime = Files.getLastModifiedTime(cache);
					if (lastModifiedTime == null || lastModifiedTime.compareTo(newLastModifiedTime) < 0) {
						lastModifiedTime = newLastModifiedTime;
					} else {
						return keys;
					}
				} catch (Exception e) {
					//$FALL-THROUGH$
				}
			}

			if (!Files.isRegularFile(cache)) {
				return List.of();
			}

			try (InputStream input = new ArmoredInputStream(new BufferedInputStream(Files.newInputStream(cache)))) {
				keys = loadKeys(input);
				return keys;
			} catch (IOException ex) {
				log(ex);
				try {
					// Assume the cache is corrupt so delete it.
					Files.delete(cache);
				} catch (IOException ex2) {
					log(ex2);
				}
				return List.of();
			}
		}

		public void add(PGPPublicKey key) {
			List<PGPPublicKey> oldKeys = get();
			List<PGPPublicKey> newKeys = new ArrayList<>(oldKeys);
			newKeys.add(key);
			newKeys = reconcileKeys(newKeys);
			if (!oldKeys.equals(newKeys)) {
				try (OutputStream underlyingStream = newAtomicOutputStream(cache);
						OutputStream output = new ArmoredOutputStream(underlyingStream)) {
					for (PGPPublicKey newKey : newKeys) {
						newKey.encode(output);
					}
				} catch (IOException e) {
					log(e);
					return;
				}
				keys = newKeys;
			}
		}
	}

	private static abstract class PGPKeyServer {
		private final Map<Long, List<PGPPublicKey>> keyIDMap = new LinkedHashMap<>();

		private final String keyServer;

		private final Path keyCache;

		public PGPKeyServer(String keyServer, Path baseCache) {
			this.keyServer = keyServer;
			keyCache = baseCache.resolve(keyServer.replace(':', '_'));
			if (!Files.isDirectory(this.keyCache)) {
				try {
					Files.createDirectories(keyCache);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}

		protected abstract boolean isStale(Path path);

		protected abstract IStatus download(URI uri, OutputStream receiver, IProgressMonitor monitor);

		protected abstract void log(Throwable throwable);

		public List<PGPPublicKey> getKeys(long keyID) {
			List<PGPPublicKey> keys = keyIDMap.get(keyID);
			String hexKeyID = toHex(keyID);
			Path cache = keyCache.resolve(hexKeyID + ".asc"); //$NON-NLS-1$
			boolean needsRemoteFetch = !Files.isRegularFile(cache) || isStale(cache);
			if (keys == null || needsRemoteFetch) {
				try {
					Iterable<PGPPublicKey> fetchedKeys;
					if (needsRemoteFetch) {
						String link = "https://" + keyServer + "/pks/lookup?op=get&search=0x" + hexKeyID; //$NON-NLS-1$ //$NON-NLS-2$
						if (DEBUG_KEY_SERVICE) {
							DebugHelper.debug("KeyServer", "Searching", "uri", link); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
						URI uri = new URI(link);
						fetchedKeys = fetchKeys(uri, cache);
					} else {
						try (InputStream input = new ArmoredInputStream(
								new BufferedInputStream(Files.newInputStream(cache)))) {
							fetchedKeys = loadKeys(input);
						}
					}

					List<PGPPublicKey> newKeys = new ArrayList<>();
					for (PGPPublicKey fetchedKey : fetchedKeys) {
						long fetchedKeyID = fetchedKey.getKeyID();
						if (fetchedKeyID == keyID) {
							newKeys.add(fetchedKey);
						}
					}

					keyIDMap.put(keyID, newKeys);
					keys = newKeys;
				} catch (URISyntaxException | IOException e) {
					log(e);
					if (keys == null || keys.isEmpty()) {
						List<PGPPublicKey> newKeys = List.of();
						keyIDMap.put(keyID, newKeys);
						keys = newKeys;
					}
				}
			}

			return Collections.unmodifiableList(keys);
		}

		protected Collection<PGPPublicKey> fetchKeys(URI uri, Path cache) throws IOException {
			try {
				ByteArrayOutputStream reciever = new ByteArrayOutputStream();
				IStatus download = download(uri, reciever, new NullProgressMonitor());
				if (!download.isOK()) {
					// If the file is not found, save an empty file to prevent repeated attempts to
					// download from this URI.
					Throwable exception = download.getException();
					if (exception instanceof FileNotFoundException) {
						log(exception);
					} else {
						if (exception != null) {
							throw new IOException(download.getMessage(), exception);
						}
						throw new IOException(download.getMessage());
					}
				}
				List<PGPPublicKey> result;
				byte[] bytes = reciever.toByteArray();
				try {
					try (InputStream input = new ArmoredInputStream(new ByteArrayInputStream(bytes))) {
						result = loadKeys(input);
					}
				} catch (IOException ex) {
					log(ex);
					// If the bytes can't be processed cache an empty file to prevent repeated
					// attempts.
					bytes = new byte[0];
					result = List.of();
				}

				try (OutputStream out = newAtomicOutputStream(cache)) {
					out.write(bytes);
				}

				return result;
			} catch (IOException ex) {
				// If the key server fails, load the cache if it exists.
				if (Files.isRegularFile(cache)) {
					try (InputStream input = new ArmoredInputStream(
							new BufferedInputStream(Files.newInputStream(cache)))) {
						return loadKeys(input);
					} catch (IOException ex1) {
						try {
							// Assume the cache is corrupt so delete it.
							Files.delete(cache);
						} catch (IOException ex2) {
							log(ex2);
						}
						// Rethrow original network failure exception with additional details
						throw new IOException("Error while processing " + uri + " as well while processing the cache " //$NON-NLS-1$ //$NON-NLS-2$
								+ cache + ": " + ex1.getMessage(), ex); //$NON-NLS-1$
					}
				}
				throw new IOException("Error while processing " + uri, ex); //$NON-NLS-1$
			}
		}

	}

	private static List<PGPPublicKey> getGPGPubringKeys(long keyID) {
		return GPGPubringCache.getKeys(keyID);
	}

	private static class GPGPubringCache {
		private static final Supplier<PGPPublicKeyRingCollection> GPG_PUBRING = getGPGPubring();
		private static volatile PGPPublicKeyRingCollection cachePubring;
		private static volatile Map<Long, List<PGPPublicKey>> cache;

		public static List<PGPPublicKey> getKeys(long keyID) {
			PGPPublicKeyRingCollection pubring = GPG_PUBRING.get();
			if (pubring != cachePubring) {
				Map<Long, List<PGPPublicKey>> newCache = new LinkedHashMap<>();
				for (Iterator<PGPPublicKeyRing> keyRings = pubring.getKeyRings(); keyRings.hasNext();) {
					for (PGPPublicKey key : keyRings.next()) {
						long keyID2 = key.getKeyID();
						List<PGPPublicKey> keys = newCache.computeIfAbsent(keyID2, it -> new ArrayList<>());
						keys.add(key);
					}
				}
				cache = newCache;
				cachePubring = pubring;
			}

			List<PGPPublicKey> result = cache.get(keyID);
			return result == null ? List.of() : result;
		}
	}

	private static abstract class GPGPubringSupplier implements Supplier<PGPPublicKeyRingCollection> {

		private final Path pubring;

		private PGPPublicKeyRingCollection keyRingCollection;

		private FileTime lastModifiedTime;

		public GPGPubringSupplier(Path pubring) {
			this.pubring = pubring;
			keyRingCollection = new PGPPublicKeyRingCollection(Collections.emptyList());
		}

		@Override
		public PGPPublicKeyRingCollection get() {
			try {
				FileTime newLastModifiedTime = Files.getLastModifiedTime(pubring);
				if (lastModifiedTime == null || lastModifiedTime.compareTo(newLastModifiedTime) < 0) {
					lastModifiedTime = newLastModifiedTime;
					keyRingCollection = buildPubring();
				}
			} catch (Exception e) {
				//$FALL-THROUGH$
			}
			return keyRingCollection;
		}

		protected abstract PGPPublicKeyRingCollection buildPubring() throws Exception;
	}

	private static Supplier<PGPPublicKeyRingCollection> getGPGPubring() {
		Path gpgDirectory = getGPPDirectory();
		Path pubringGpg = gpgDirectory.resolve("pubring.gpg"); //$NON-NLS-1$
		Path pubringKbx = gpgDirectory.resolve("pubring.kbx"); //$NON-NLS-1$

		if (Files.isRegularFile(pubringGpg)) {
			return new GPGPubringSupplier(pubringGpg) {
				@Override
				protected PGPPublicKeyRingCollection buildPubring() throws Exception {
					try (InputStream input = new BufferedInputStream(Files.newInputStream(pubringGpg))) {
						PGPPublicKeyRingCollection keyRingCollection = new PGPPublicKeyRingCollection(input,
								new JcaKeyFingerprintCalculator());
						return keyRingCollection;
					}
				}
			};
		} else if (Files.isRegularFile(pubringKbx)) {
			return new GPGPubringSupplier(pubringKbx) {
				@Override
				protected PGPPublicKeyRingCollection buildPubring() throws Exception {
					try (InputStream input = new BufferedInputStream(Files.newInputStream(pubringKbx))) {
						KeyBox keyBox = new JcaKeyBoxBuilder().build(input);
						List<PGPPublicKeyRing> pgpPublicKeyRings = new ArrayList<>();
						for (KeyBlob keyBlob : keyBox.getKeyBlobs()) {
							switch (keyBlob.getType()) {
							case OPEN_PGP_BLOB: {
								PGPPublicKeyRing pgpPublicKeyRing = ((PublicKeyRingBlob) keyBlob).getPGPPublicKeyRing();
								pgpPublicKeyRings.add(pgpPublicKeyRing);
							}
							default: {
								//$FALL-THROUGH$
							}
							}
						}
						PGPPublicKeyRingCollection keyRingCollection = new PGPPublicKeyRingCollection(
								pgpPublicKeyRings);
						return keyRingCollection;
					}
				}
			};
		} else {
			PGPPublicKeyRingCollection empty;
			try {
				empty = new PGPPublicKeyRingCollection(Collections.emptyList());
			} catch (Exception e) {
				// Cannot happen for an empty collection.
				throw new RuntimeException(e);
			}
			return () -> empty;
		}
	}

	@SuppressWarnings("nls")
	private static Path getGPPDirectory() {
		// Handle ~ as might be used on macos and linux.
		Function<String, Path> resolveTilde = s -> {
			if (s.startsWith("~/") || s.startsWith("~" + File.separatorChar)) {
				return new File(System.getProperty("user.home"), s.substring(2)).getAbsoluteFile().toPath();
			}
			return Paths.get(s);
		};

		// Allow the user to specify the GPG home used by p2 specifically.
		Path path = checkDirectory(System.getProperty(GPG_HOME_PROPERTY), resolveTilde);
		if (path != null) {
			return path;
		}

		path = checkDirectory(System.getenv("GNUPGHOME"), resolveTilde);
		if (path != null) {
			return path;
		}

		if ("win32".equals(System.getProperty("osgi.os"))) {
			// On Windows prefer %APPDATA%\gnupg if it exists, even if Cygwin is used.
			path = checkDirectory(System.getenv("APPDATA"), //$NON-NLS-1$
					s -> Paths.get(s).resolve("gnupg")); //$NON-NLS-1$
			if (path != null) {
				return path;
			}
		}
		// All systems, including Cygwin and even Windows if %APPDATA%\gnupg doesn't
		// exist.
		return resolveTilde.apply("~/.gnupg"); //$NON-NLS-1$
	}

	private static Path checkDirectory(String dir, Function<String, Path> toPath) {
		if (dir != null && !dir.isBlank()) {
			try {
				Path directory = toPath.apply(dir);
				if (Files.isDirectory(directory)) {
					return directory;
				}
			} catch (RuntimeException e) {
				//$FALL-THROUGH$
			}
		}
		return null;
	}
}
