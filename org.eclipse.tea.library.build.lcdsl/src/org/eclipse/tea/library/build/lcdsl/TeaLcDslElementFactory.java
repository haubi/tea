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
package org.eclipse.tea.library.build.lcdsl;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.tea.library.build.chain.TeaBuildChain;
import org.eclipse.tea.library.build.chain.TeaBuildElement;
import org.eclipse.tea.library.build.services.TeaBuildElementFactory;
import org.osgi.service.component.annotations.Component;

@Component
public class TeaLcDslElementFactory implements TeaBuildElementFactory {

	@Override
	public Collection<TeaBuildElement> createElements(TeaBuildChain chain, IProject prj) {
		Collection<TeaBuildElement> result = null;
		if (TeaLcDslElement.hasLc(prj)) {
			TeaLcDslElement pre = (TeaLcDslElement) chain.getElementFor(TeaLcDslElement.NAME_PRE);
			TeaLcDslElement post = (TeaLcDslElement) chain.getElementFor(TeaLcDslElement.NAME_POST);
			if (pre == null) {
				// need a pre and a post element
				result = new ArrayList<>();
				pre = new TeaLcDslElement(TeaLcDslElement.NAME_PRE);
				post = new TeaLcDslElement(TeaLcDslElement.NAME_POST);
				result.add(pre);
				result.add(post);
			}

			pre.add(prj);
			post.add(prj);
		}
		return result;
	}

}
