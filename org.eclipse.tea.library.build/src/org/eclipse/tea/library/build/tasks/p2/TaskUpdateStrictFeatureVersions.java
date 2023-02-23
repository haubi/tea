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
package org.eclipse.tea.library.build.tasks.p2;

import java.util.Collections;
import java.util.List;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.library.build.jar.JarManager;
import org.eclipse.tea.library.build.model.FeatureBuild;
import org.eclipse.tea.library.build.model.WorkspaceBuild;

public class TaskUpdateStrictFeatureVersions {

	private final String feature;
	private FeatureBuild fb;

	private TaskUpdateStrictFeatureVersions(String feature) {
		this.feature = feature;
	}

	public static TaskUpdateStrictFeatureVersions create(String feature,
			List<TaskUpdateStrictFeatureVersions> updates) {
		TaskUpdateStrictFeatureVersions t = new TaskUpdateStrictFeatureVersions(feature);
		updates.add(t);
		return t;
	}

	@Override
	public String toString() {
		return "Update plugin versions for " + feature;
	}

	@Execute
	public void update(WorkspaceBuild wb, JarManager jm) throws Exception {
		fb = wb.getFeature(feature);
		if (fb == null) {
			return;
		}

		fb.backupFeatureXml();
		fb.generateFeatureXml(jm, Collections.emptyList(), fb.getIncludedPlugins());
	}

	public Object restore() {
		return new Object() {

			@Execute
			public void restore() {
				if (fb != null) {
					try {
						fb.restoreFeatureXml();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}

			@Override
			public String toString() {
				return "Restore feature.xml for " + feature;
			}
		};
	}

}
