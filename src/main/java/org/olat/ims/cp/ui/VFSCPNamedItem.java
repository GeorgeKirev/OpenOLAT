/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* frentix GmbH, Switzerland, http://www.frentix.com
* <p>
*/
package org.olat.ims.cp.ui;

import org.olat.core.util.vfs.NamedLeaf;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;

/**
 * 
 * Description:<br>
 * A specialization of the NamedLeaf with a custom resolve method
 * 
 * <P>
 * Initial Date:  5 mai 2011 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
//fxdiff FXOLAT-125: virtual file system for CP
public class VFSCPNamedItem extends NamedLeaf {
	
	public VFSCPNamedItem(String name, VFSLeaf delegate) {
		super(name, delegate);
	}

	@Override
	public VFSItem resolve(String path) {
		if(path == null || path.length() == 0 || path.equals("/")) {
			return this;
		}
		return delegate.resolve(path);
	}
}