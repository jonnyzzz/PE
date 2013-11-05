/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     Peter Smith
 *******************************************************************************/
package org.boris.pecoff4j.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class IO {
  @NotNull
  public static byte[] toBytes(@NotNull File f) throws IOException {
    byte[] b = new byte[(int) f.length()];
    FileInputStream fis = new FileInputStream(f);
    fis.read(b);
    return b;
  }

  public static byte[] toBytes(@NotNull InputStream is) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    copy(is, bos, true);
    return bos.toByteArray();
  }

  public static void findFiles(@NotNull File dir, @Nullable FilenameFilter filter,
                               @NotNull FindFilesCallback callback) {
    File[] f = dir.listFiles();
    for (File fs : f) {
      if (fs.isDirectory()) {
        findFiles(fs, filter, callback);
      } else if (filter == null || filter.accept(dir, fs.getName())) {
        callback.fileFound(fs);
      }
    }
  }

  public static File[] findFiles(@NotNull File dir, @NotNull FilenameFilter filter) {
    Set<File> files = new HashSet();
    findFiles(dir, filter, files);
    return files.toArray(new File[0]);
  }

  private static void findFiles(@NotNull File dir, @NotNull FilenameFilter filter,
                                @NotNull Set<File> files) {
    File[] f = dir.listFiles();
    for (File ff : f) {
      if (ff.isDirectory()) {
        findFiles(ff, filter, files);
      } else {
        if (filter.accept(ff.getParentFile(), ff.getName()))
          files.add(ff);
      }
    }
  }

  public static void copy(@NotNull Reader r, @NotNull Writer w, boolean close)
          throws IOException {
    char[] buf = new char[4096];
    int len = 0;
    while ((len = r.read(buf)) > 0) {
      w.write(buf, 0, len);
    }
    if (close) {
      r.close();
      w.close();
    }
  }

  public static void copy(@NotNull InputStream r, @NotNull OutputStream w, boolean close)
          throws IOException {
    byte[] buf = new byte[4096];
    int len = 0;
    while ((len = r.read(buf)) > 0) {
      w.write(buf, 0, len);
    }
    if (close) {
      r.close();
      w.close();
    }
  }
}
