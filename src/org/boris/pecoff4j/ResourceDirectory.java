/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     Peter Smith
 *******************************************************************************/
package org.boris.pecoff4j;

import org.boris.pecoff4j.util.DataObject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ResourceDirectory extends DataObject {
  private ResourceDirectoryTable table;
  @NotNull
  private List<ResourceEntry> entries = new ArrayList();

  public ResourceDirectoryTable getTable() {
    return table;
  }

  public void setTable(ResourceDirectoryTable table) {
    this.table = table;
  }

  public void add(ResourceEntry entry) {
    this.entries.add(entry);
  }

  public ResourceEntry get(int index) {
    return entries.get(index);
  }

  public int size() {
    return entries.size();
  }
}
