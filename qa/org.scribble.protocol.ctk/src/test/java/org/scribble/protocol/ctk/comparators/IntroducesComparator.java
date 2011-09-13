/*
 * Copyright 2009 www.scribble.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.scribble.protocol.ctk.comparators;

import java.util.Comparator;

import org.scribble.protocol.ctk.ComparatorUtil;
import org.scribble.protocol.model.*;

public class IntroducesComparator implements Comparator<ModelObject> {

	public int compare(ModelObject arg0, ModelObject arg1) {
		Introduces m=(Introduces)arg0;
		Introduces e=(Introduces)arg1;
		
		if (m.getIntroducedRoles().size() == e.getIntroducedRoles().size()) {
			RoleComparator pcomp=(RoleComparator)
						ComparatorUtil.getComparator(Role.class);
			
			for (int i=0; i < m.getIntroducedRoles().size(); i++) {
				if (pcomp.compare(m.getIntroducedRoles().get(i), e.getIntroducedRoles().get(i)) != 0) {
					return(1);
				}
			}
		} else {
			return(1);
		}
		
		Comparator<java.util.List<org.scribble.common.model.Annotation>> ancomp=new AnnotationsComparator();	
		
		if (ancomp.compare(m.getAnnotations(), e.getAnnotations()) != 0) {
			return(1);
		}
		
		return(0);
	}
}
