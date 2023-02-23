/*******************************************************************************
 *  Copyright (c) 2018 SSI Schaefer IT Solutions GmbH and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.tea.library.build.lcdsl.tasks.p2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.tea.library.build.model.FeatureBuild;
import org.eclipse.tea.library.build.model.PlatformTriple;
import org.eclipse.tea.library.build.util.StringHelper;

import com.google.common.base.Splitter;

/**
 * A {@link AbstractProductBuild} which can be created from properties stored in
 * a file.
 */
public class SimpleProductBuild extends AbstractProductBuild {

	private final SimpleProductBuildDescription desc;

	public SimpleProductBuild(SimpleProductBuildDescription desc) {
		super(desc.featureBundle, desc.productBundle, desc.productFileName, desc.needProperties);
		this.desc = desc;
	}

	@Override
	public String getOfficialName() {
		return desc.name;
	}

	@Override
	public String getDescription() {
		return desc.description;
	}

	@Override
	public PlatformTriple[] getPlatformsToBuild() {
		return desc.platforms.toArray(new PlatformTriple[desc.platforms.size()]);
	}

	/**
	 * A single product definition in the workspace
	 */
	public static final class SimpleProductBuildDescription {

		public String name;
		public String description;
		public String featureBundle;
		public String productBundle;
		public String productFileName;
		public boolean needProperties = false;
		public List<PlatformTriple> platforms = new ArrayList<>();

		public SimpleProductBuildDescription(FeatureBuild prodFeature, Properties props) {
			this.name = props.getProperty("alias");
			this.description = props.getProperty("description", this.name);
			this.featureBundle = props.getProperty("featureBundle");
			this.productBundle = prodFeature.getFeatureName();
			this.productFileName = findProductFile(prodFeature).getName();
			this.needProperties = Boolean.valueOf(props.getProperty("needProperties", "false"));
			applyPlatforms(props.getProperty("platforms"));

			if (name.isEmpty() || featureBundle.isEmpty()) {
				throw new IllegalStateException("'name' and 'featureBundle' must be set in " + prodFeature);
			}
		}

		private void applyPlatforms(String raw) {
			if (StringHelper.isNullOrEmpty(raw)) {
				platforms.add(PlatformTriple.LINUX64);
				platforms.add(PlatformTriple.WIN64);
				return;
			}

			Splitter.on(',').trimResults().omitEmptyStrings().splitToList(raw).stream().map(this::toTriple)
					.forEach(platforms::add);
		}

		private PlatformTriple toTriple(String platform) {
			switch (platform.toLowerCase()) {
			case "win32":
				return PlatformTriple.WIN32;
			case "win64":
				return PlatformTriple.WIN64;
			case "linux32":
				return PlatformTriple.LINUX32;
			case "linux64":
				return PlatformTriple.LINUX64;
			}
			throw new IllegalStateException("unsupported platform: " + platform);
		}

		private File findProductFile(FeatureBuild prodFeature) {
			File root = prodFeature.getData().getBundleDir();
			File[] candidates = root.listFiles((dir, name) -> name.endsWith(".product"));

			if (candidates == null || candidates.length != 1) {
				throw new IllegalStateException("Cannot find .product file in " + prodFeature);
			}

			return candidates[0];
		}

	}

}
