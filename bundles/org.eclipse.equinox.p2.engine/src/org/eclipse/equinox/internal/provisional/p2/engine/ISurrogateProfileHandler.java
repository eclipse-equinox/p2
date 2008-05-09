package org.eclipse.equinox.internal.provisional.p2.engine;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.engine.Profile;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;

public interface ISurrogateProfileHandler {

	public abstract Profile createProfile(String id);

	public abstract boolean isSurrogate(IProfile profile);

	public abstract Collector queryProfile(IProfile profile, Query query, Collector collector, IProgressMonitor monitor);

	public abstract boolean updateProfile(Profile selfProfile);

}