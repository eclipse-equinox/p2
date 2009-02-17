package org.eclipse.equinox.p2.internal.repository.tools;

import java.net.URI;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;

public class RepositoryDescriptor {

	private boolean compressed = true;
	private boolean append = true;
	private String name = null;
	private URI location = null;
	private String format = null;
	private int kind;

	public void setCompressed(boolean compress) {
		compressed = compress;
	}

	public void setName(String repoName) {
		name = repoName;
	}

	public void setLocation(URI repoLocation) {
		location = repoLocation;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public void setAppend(boolean appendMode) {
		append = appendMode;
	}

	public boolean isCompressed() {
		return compressed;
	}

	public boolean isAppend() {
		return append;
	}

	public String getName() {
		return name;
	}

	public URI getRepoLocation() {
		return location;
	}

	public String getFormat() {
		return format;
	}

	public int getKind() {
		return kind;
	}

	public void setKind(String repoKind) {
		if (repoKind.startsWith("m") || repoKind.startsWith("M")) //$NON-NLS-1$//$NON-NLS-2$
			kind = IRepository.TYPE_METADATA;

		if (repoKind.startsWith("a") || repoKind.startsWith("A")) //$NON-NLS-1$//$NON-NLS-2$
			kind = IRepository.TYPE_ARTIFACT;
	}
}
