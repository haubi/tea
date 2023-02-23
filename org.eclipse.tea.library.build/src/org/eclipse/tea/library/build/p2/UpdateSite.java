/*******************************************************************************
 *  Copyright (c) 2017 SSI Schaefer IT Solutions GmbH and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.tea.library.build.p2;

import java.io.File;

import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.jar.JarManager;
import org.eclipse.tea.library.build.jar.ZipExec;
import org.eclipse.tea.library.build.jar.ZipExecFactory;
import org.eclipse.tea.library.build.jar.ZipExecPart;
import org.eclipse.tea.library.build.util.FileUtils;

/**
 * Holds information about an update site in the build.
 */
public final class UpdateSite {

	/**
	 * global unique ID of the site
	 */
	public final String guid;

	/**
	 * build directory of the site
	 */
	public final File directory;

	/**
	 * ZIP file which will contain the site
	 */
	public final File zip;

	UpdateSite(File out, String guid, JarManager jarManager) {
		this.guid = guid;

		directory = new File(out, guid);
		FileUtils.deleteDirectory(directory);
		FileUtils.mkdirs(directory);

		String zipName = guid + "-" + jarManager.getQualifier() + ".zip";
		zip = new File(out, zipName);
	}

	public static void createUpdateSiteZip(File dir, File zip, ZipExecFactory zef, TaskingLog console) {
		FileUtils.delete(zip);
		console.info("Archiving update site to " + zip);
		final ZipExec zipExec = zef.createZipExec();
		zipExec.setZipFile(zip);

		final ZipExecPart part = new ZipExecPart();
		part.sourceDirectory = dir;
		for (File file : dir.listFiles()) {
			part.relativePaths.add(file.getName());
		}
		zipExec.addPart(part);
		zipExec.createZip();
	}

	public void createUpdateSiteZip(ZipExecFactory zef, TaskingLog console) {
		console.info("Archiving update site " + guid);
		createUpdateSiteZip(directory, zip, zef, console);
	}

}
