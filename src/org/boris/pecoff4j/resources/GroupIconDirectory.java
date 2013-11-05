/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     Peter Smith
 *******************************************************************************/
package org.boris.pecoff4j.resources;

import org.boris.pecoff4j.io.DataReader;
import org.boris.pecoff4j.io.IDataReader;
import org.boris.pecoff4j.util.Reflection;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class GroupIconDirectory {
  private int reserved;
  private int type;
  private int count;
  private GroupIconDirectoryEntry[] entries;

  public int getReserved() {
    return reserved;
  }

  public int getType() {
    return type;
  }

  public int getCount() {
    return count;
  }

  public GroupIconDirectoryEntry getEntry(int index) {
    return entries[index];
  }

  public String toString() {
    return Reflection.toString(this);
  }

  @NotNull
  public static GroupIconDirectory read(byte[] data) throws IOException {
    return read(new DataReader(data));
  }

  @NotNull
  public static GroupIconDirectory read(@NotNull IDataReader dr) throws IOException {
    GroupIconDirectory gi = new GroupIconDirectory();
    gi.reserved = dr.readWord();
    gi.type = dr.readWord();
    gi.count = dr.readWord();
    gi.entries = new GroupIconDirectoryEntry[gi.count];
    for (int i = 0; i < gi.count; i++) {
      gi.entries[i] = GroupIconDirectoryEntry.read(dr);
    }

    return gi;
  }
}
