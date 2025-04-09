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

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.eclipse.IBrandingAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.IMacOsBundleUrlType;
import org.eclipse.equinox.p2.publisher.eclipse.ProductFileAdvice;

/**
 * Ensures that the branding advice can find the icon defined by the product.
 */
@SuppressWarnings("restriction")
public class TeaBrandingAdvice implements IBrandingAdvice {

	private final IBrandingAdvice advice;

	public TeaBrandingAdvice(IBrandingAdvice advice) {
		this.advice = advice;
	}

	@Override
	public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
		return advice.isApplicable(configSpec, includeDefault, id, version);
	}

	@Override
	public String getOS() {
		return advice.getOS();
	}

	@Override
	public String getExecutableName() {
		return advice.getExecutableName();
	}

	@Override
	public List<IMacOsBundleUrlType> getMacOsBundleUrlTypes() {
		return advice.getMacOsBundleUrlTypes();
	}

	@Override
	public String[] getIcons() {
		return fixIconPath(advice);
	}

	private static String[] fixIconPath(IBrandingAdvice advice) {
		String[] iconsArr = advice.getIcons();
		if (iconsArr == null || iconsArr.length == 0) {
			return null;
		}

		IProductDescriptor productFile = ((ProductFileAdvice) advice).getProductFile();
		File workspaceLocation = productFile.getLocation().getParentFile().getParentFile();

		IPath rootPath = Path.fromOSString(workspaceLocation.getAbsolutePath());
		int idx = iconsArr.length;
		while (--idx >= 0) {
			File iconFile = new File(iconsArr[idx]);
			if (!iconFile.isFile()) {
				IPath iconPath = Path.fromOSString(iconFile.getPath());
				if (rootPath.isPrefixOf(iconPath)) {
					// The iconPath is bogus since it is relative to the project
					// files parent rather than to the project parent (the
					// workspace). This either happens on Windows platforms or
					// if the project containing the icon is not directly in the
					// workspace root (e.g. inside an intermediate "plugins"-
					// folder when it has been materialized using Buckminster.
					iconPath = iconPath.removeFirstSegments(rootPath.segmentCount() + 1).setDevice(null).makeRelative();

					// First check, if the path references a project that is
					// not directly in the workspace's root
					IProject prj = ResourcesPlugin.getWorkspace().getRoot().getProject(iconPath.segment(0));
					IResource iconRes = prj.findMember(iconPath.removeFirstSegments(1));
					if (iconRes != null && iconRes.exists()) {
						iconPath = iconRes.getLocation();
					} else {
						// Windows: we strip one more segment then the root has
						// and then we prepend the root again.
						iconPath = rootPath.append(iconPath);
					}
				} else {
					// The iconPath was considered absolute and hence not
					// altered. The problem is that it should be relative to the
					// workspace root.
					// This happens on Linux systems.
					iconPath = rootPath.append(iconPath.makeRelative());
				}
				iconFile = iconPath.toFile();
				if (iconFile.isFile()) {
					iconsArr[idx] = iconFile.getAbsolutePath();
				}
			}
		}
		return iconsArr;
	}

}
