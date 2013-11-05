package org.boris.pecoff4j;

import org.boris.pecoff4j.util.FindFilesCallback;
import org.boris.pecoff4j.util.IO;
import org.boris.pecoff4j.util.IconExtractor;
import org.boris.pecoff4j.util.PEFilenameFilter;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class IconExtractorTest implements FindFilesCallback {
  @NotNull
  private static File outdir = new File("F:/Development/icons/extracted");

  public static void main(String[] args) throws Exception {
    IO.findFiles(new File("F:/Program Files/"), new PEFilenameFilter(),
            new IconExtractorTest());
  }

  public void fileFound(@NotNull File fs) {
    try {
      System.out.println(fs);
      IconExtractor.extract(fs, outdir);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
