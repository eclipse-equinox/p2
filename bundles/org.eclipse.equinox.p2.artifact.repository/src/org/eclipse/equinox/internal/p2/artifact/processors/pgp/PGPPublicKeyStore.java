/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.processors.pgp;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;

public class PGPPublicKeyStore {
	private Map<Long, PGPPublicKey> keys = new HashMap<>();

	public PGPPublicKey addKey(PGPPublicKey key) {
		if (key == null) {
			return null;
		}
		PGPPublicKey alreadyStoredKey = keys.putIfAbsent(key.getKeyID(), key);
		return alreadyStoredKey == null ? key : alreadyStoredKey;
	}

	public PGPPublicKey getKey(long id) {
		return keys.get(id);
	}

	public void addKeys(String... armoredPublicKeys) {
		for (String armoredKey : armoredPublicKeys) {
			if (armoredKey != null) {
				PGPPublicKeyStore.readPublicKeys(armoredKey).forEach(this::addKey);
			}
		}
	}

	/**
	 * Test only
	 */
	public void clear() {
		keys.clear();
	}

	public Collection<PGPPublicKey> all() {
		return Collections.unmodifiableCollection(keys.values());
	}

	public boolean isEmpty() {
		return keys.isEmpty();
	}

	public String toArmoredString() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ArmoredOutputStream armoredOut = new ArmoredOutputStream(out);
		// PGPPublicKeyRing ring = new PGPPublicKeyRing(new ArrayList<>(keys.values()));
		// ring.encode(armoredOut);
		for (PGPPublicKey key : keys.values()) {
			key.encode(armoredOut);
		}
		armoredOut.close();
		out.close();
		return new String(out.toByteArray(), StandardCharsets.US_ASCII);
	}

	public void remove(PGPPublicKey selectedKey) {
		keys.remove(selectedKey.getKeyID());
	}

	public void add(File file) {
		try (InputStream stream = new FileInputStream(file)) {
			readPublicKeys(stream).forEach(this::addKey);
		} catch (IOException e) {
			// TODO: how to log?
		}
	}

	public static Set<PGPPublicKey> readPublicKeys(InputStream input) throws IOException {
		return readPublicKeys(new String(input.readAllBytes(), StandardCharsets.US_ASCII));
	}

	@SuppressWarnings("unchecked")
	public static Set<PGPPublicKey> readPublicKeys(String armoredPublicKeyring) {
		if (armoredPublicKeyring == null) {
			return Set.of();
		}
		Set<PGPPublicKey> res = new HashSet<>();
		try (InputStream stream = PGPUtil.getDecoderStream(new ByteArrayInputStream(
				PGPSignatureVerifier.unnormalizedPGPProperty(armoredPublicKeyring).getBytes()))) {
			new JcaPGPObjectFactory(stream).forEach(o -> {
				if (o instanceof PGPPublicKeyRingCollection) {
					collectKeys((PGPPublicKeyRingCollection) o, res::add);
				}
				if (o instanceof PGPPublicKeyRing) {
					collectKeys((PGPPublicKeyRing) o, res::add);
				}
				if (o instanceof PGPPublicKey) {
					res.add((PGPPublicKey) o);
				}
			});
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, e.getMessage(), e));
		}
		return res;

	}

	private static void collectKeys(PGPPublicKeyRingCollection pgpPublicKeyRingCollection,
			Consumer<PGPPublicKey> collector) {
		pgpPublicKeyRingCollection.forEach(keyring -> collectKeys(keyring, collector));
	}

	private static void collectKeys(PGPPublicKeyRing pgpPublicKeyRing, Consumer<PGPPublicKey> collector) {
		pgpPublicKeyRing.getPublicKeys().forEachRemaining(collector::accept);
	}
}