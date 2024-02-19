/*******************************************************************************
 *  Copyright (c) 2017 SSI Schaefer IT Solutions GmbH and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.tea.library.build.p2;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.internal.Activator;

@SuppressWarnings("restriction")
public class P2InstallationHelper {

	public static File getInstallLocation(TaskingLog console, String installableUnitId) {
		IProfile profile = loadSelfProfile(console);
		console.debug("looking up " + installableUnitId + " in profile: " + profile);

		IQuery<IInstallableUnit> tpQ = QueryUtil.createIUPropertyQuery("org.eclipse.equinox.p2.name",
				installableUnitId);
		IQueryResult<IInstallableUnit> result = profile.query(tpQ, new NullProgressMonitor());
		if (result.isEmpty()) {
			throw new IllegalStateException("cannot find '" + installableUnitId + "' in profile:  " + profile);
		}
		TreeSet<IInstallableUnit> ius = new TreeSet<>(Collections.reverseOrder());
		ius.addAll(result.toSet());
		IInstallableUnit rootIu = ius.first();

		console.debug("looking up '" + installableUnitId + "' from IU: " + rootIu);

		return P2InstallationHelper.getInstallLocation(profile, rootIu);

	}

	public static IProfile loadSelfProfile(TaskingLog log) {
		int retryCnt = 100;
		IProfile profile = null;
		while (profile == null && retryCnt-- > 0) {
			IProfileRegistry r = getProfileRegistry();
			if (r == null) {
				throw new IllegalStateException("no profile registry present");
			}
			profile = r.getProfile(IProfileRegistry.SELF);
			if (profile == null) {
				// not using profiles!
				return null;
			}
			log.debug("p2 gave us this profile: " + profile + "(timestamp: " + profile.getTimestamp() + ")");
			if (profile.getTimestamp() == 0) {
				// not valid - not loaded.
				profile = null;
				if (r instanceof SimpleProfileRegistry) {
					((SimpleProfileRegistry) r).resetProfiles();
				}
				try {
					TimeUnit.MILLISECONDS.sleep(100);
				} catch (InterruptedException e) {
					// ignore.
				}
			}
		}
		if (profile == null) {
			// no luck. there IS a profile, but we cannot load it...
			throw new RuntimeException("cannot load profile");
		}

		return profile;
	}

	private static File getInstallLocation(IProfile profile, IInstallableUnit rootIu) {
		IProvisioningAgent agent = getProvisioningAgent();
		Collection<IRequirement> requirements = rootIu.getRequirements();
		if (requirements.isEmpty()) {
			throw new IllegalStateException("cannot find requirements for " + rootIu);
		}
		for (IRequirement req : requirements) {
			IQueryResult<IInstallableUnit> iuqr = profile
					.query(QueryUtil.createMatchQuery(req.getMatches(), new Object[0]), new NullProgressMonitor());
			for (IInstallableUnit iu : iuqr) {
				Collection<IArtifactKey> artifacts = iu.getArtifacts();
				if (artifacts == null || artifacts.isEmpty()) {
					continue;
				}
				IArtifactKey key = artifacts.iterator().next();
				File dir = Util.getArtifactFile(agent, key, profile);
				if (dir == null || !dir.isDirectory()) {
					continue;
				}
				return dir;
			}
		}
		throw new IllegalStateException("cannot find install location for " + rootIu);
	}

	private static IProvisioningAgent getProvisioningAgent() {
		return ((IProvisioningAgent) ServiceHelper.getService(Activator.getContext(), IProvisioningAgent.SERVICE_NAME));
	}

	private static IProfileRegistry getProfileRegistry() {
		return ((IProfileRegistry) getProvisioningAgent().getService(IProfileRegistry.SERVICE_NAME));
	}

}
