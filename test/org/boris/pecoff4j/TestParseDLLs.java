package org.boris.pecoff4j;

import org.boris.pecoff4j.io.PEParser;
import org.boris.pecoff4j.util.IO;
import org.boris.pecoff4j.util.Reflection;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FilenameFilter;

public class TestParseDLLs {

  public static void main(String[] args) throws Exception {
    File[] files = findPEs();
    for (int i = 0; i < files.length; i++) {
      System.out.println(files[i]);
      PE pe = PEParser.parse(files[i]);
      System.out.println(Reflection.toString(pe));
    }
  }

  public static File[] findPEs() {
    FilenameFilter ff = new FilenameFilter() {
      public boolean accept(File dir, @NotNull String name) {
        return (name.endsWith(".dll") || name.endsWith(".exe")) &&
                name.indexOf("dllcache") == -1;
      }
    };
    File[] files = IO.findFiles(new File("F:/Program Files/"), ff);
    // File[] files = IO.findFiles(new File("C:/Program Files/"), ff);
    // File[] files = IO.findFiles(new File("C:/windows/system32"), ff);

    return files;
  }

}
