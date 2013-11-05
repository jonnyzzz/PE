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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BoundImportDirectoryTable {
  @NotNull
  private List<BoundImport> imports = new ArrayList();

  public void add(BoundImport bi) {
    imports.add(bi);
  }

  public int size() {
    return imports.size();
  }

  public BoundImport get(int index) {
    return imports.get(index);
  }
}
