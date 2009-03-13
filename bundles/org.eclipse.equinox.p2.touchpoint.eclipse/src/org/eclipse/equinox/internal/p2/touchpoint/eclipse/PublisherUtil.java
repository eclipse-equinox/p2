package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.File;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.*;
import org.eclipse.osgi.service.resolver.BundleDescription;

public class PublisherUtil {

	public static IInstallableUnit createBundleIU(IArtifactKey artifactKey, File bundleFile) {
		BundleDescription bundleDescription = BundlesAction.createBundleDescription(bundleFile);
		PublisherInfo info = new PublisherInfo();
		Version version = new Version(bundleDescription.getVersion().toString());
		AdviceFileAdvice advice = new AdviceFileAdvice(bundleDescription.getSymbolicName(), version, new Path(bundleFile.getAbsolutePath()), AdviceFileAdvice.BUNDLE_ADVICE_FILE);
		if (advice.containsAdvice())
			info.addAdvice(advice);
		String shape = bundleFile.isDirectory() ? IBundleShapeAdvice.DIR : IBundleShapeAdvice.JAR;
		info.addAdvice(new BundleShapeAdvice(bundleDescription.getSymbolicName(), version, shape));
		return BundlesAction.createBundleIU(bundleDescription, artifactKey, info);
	}

}
