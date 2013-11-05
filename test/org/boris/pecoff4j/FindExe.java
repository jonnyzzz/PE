package org.boris.pecoff4j;

import org.boris.pecoff4j.util.IO;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FilenameFilter;

public class FindExe {
  public static void main(String[] args) throws Exception {
    File[] files = IO.findFiles(new File("C:/windows/system32"),
            new FilenameFilter() {
              public boolean accept(File dir, @NotNull String name) {
                return name.endsWith(".dll") &&
                        name.indexOf("dllcache") == -1;
              }
            });
    System.out.println("public static String[] DLL_FILES = {");
    for (File f : files) {
      String str = f.toString();
      str = str.replaceAll("\\\\", "/");
      System.out.println("\"" + str + "\",");
    }
    System.out.println("};");
  }
}
