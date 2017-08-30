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
package org.eclipse.tea.library.build.lcdsl.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.tea.library.build.chain.TeaBuildElement;
import org.eclipse.tea.library.build.util.TeaBuildUtil;
import org.eclipse.xtext.ui.XtextProjectHelper;

public class TeaLcDslElement extends TeaBuildElement {

	static final String NAME_PRE = "LcDsl: Prepare launch configurations";
	static final String NAME_POST = "LcDsl: Update launch configurations";

	private final List<IProject> projects = new ArrayList<>();
	private final String name;

	public TeaLcDslElement(String name) {
		this.name = name;
	}

	public void add(IProject toBuild) {
		projects.add(toBuild);
	}

	public void execute() {
		TeaBuildUtil.tryCompile(projects);
	}

	@Override
	public String getName() {
		return name;
	}

	static boolean hasLc(IProject prj) {
		if (XtextProjectHelper.hasNature(prj)) {
			// look for .lc files
			AtomicBoolean hasLc = new AtomicBoolean();
			try {
				prj.accept((res) -> {
					if (hasLc.get()) {
						return false; // cancel ASAP
					}
					if (res.getType() == IResource.FOLDER || res.getType() == IResource.PROJECT) {
						return true;
					}
					if (res.getType() == IResource.FILE && res.getName().endsWith(".lc")) {
						hasLc.set(true);
					}
					return false;
				});
			} catch (CoreException e) {
				throw new RuntimeException("cannot visit " + prj + "'s resources", e);
			}
			return hasLc.get();
		}
		return false;
	}

}
