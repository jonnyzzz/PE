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

public class ImportDirectoryTable {
  @NotNull
  private ArrayList imports = new ArrayList();

  public void add(ImportEntry entry) {
    imports.add(entry);
  }

  public int size() {
    return imports.size();
  }

  @NotNull
  public ImportEntry getEntry(int index) {
    return (ImportEntry) imports.get(index);
  }
}
