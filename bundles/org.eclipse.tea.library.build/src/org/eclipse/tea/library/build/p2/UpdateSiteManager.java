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
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.jar.JarManager;
import org.eclipse.tea.library.build.jar.ZipExecFactory;
import org.eclipse.tea.library.build.util.FileUtils;
import org.eclipse.tea.library.build.util.StringHelper;

/**
 * Holds information about update sites in the build.
 */
public final class UpdateSiteManager {

	private static final String ALL_SITES = "all";
	private final Map<String, UpdateSite> sites = new TreeMap<>();
	private final JarManager jarManager;
	private final File out;

	public UpdateSiteManager(File out, JarManager jarManager) {
		this.out = out;
		this.jarManager = jarManager;
	}

	/**
	 * Gets an update site. Clears the directory of the site during the first
	 * call.
	 *
	 * @param guid
	 *            global unique ID of the site
	 * @return update site information
	 */
	public UpdateSite getSite(String guid) {
		guid = safeGuid(guid);
		UpdateSite site = sites.get(guid);
		if (site == null) {
			site = new UpdateSite(out, guid, jarManager);
			sites.put(guid, site);
		}
		return site;
	}

	private String safeGuid(String guid) {
		if (StringHelper.isNullOrEmpty(guid)) {
			return ALL_SITES;
		}
		return guid;
	}

	public void createUpdateSiteZips(TaskingLog console) {
		ZipExecFactory zef = jarManager.getZipExecFactory();
		for (UpdateSite site : sites.values()) {
			site.createUpdateSiteZip(zef, console);
		}
	}

	/**
	 * Clears all sites and removes all site directories (but not the ZIP
	 * files).
	 */
	public void clearSites() {
		try {
			for (UpdateSite site : sites.values()) {
				FileUtils.deleteDirectory(site.directory);
			}
		} finally {
			sites.clear();
		}
	}

}
