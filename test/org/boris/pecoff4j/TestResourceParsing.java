package org.boris.pecoff4j;

import org.boris.pecoff4j.io.PEParser;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;

// author douggard

public class TestResourceParsing {
  public static void main(String[] args) throws Exception {
    File file = new File("/tmp/test_pe_01.exe"); //sha256 bc9ab25f23827689fded588e1cadefc923e32a33a5c759c7c6b94fc7915bb6e1
    PE pe = PEParser.parse(file);
    ResourceDirectory resourceTable = pe.getImageData().getResourceTable();
    System.out.println("DIRECTORY");
    for(int i = 0; i < resourceTable.size(); ++i) {
      ResourceEntry resourceEntry = resourceTable.get(i);
      System.out.println("\tENTRY");
      traverseResources(resourceEntry.getDirectory(), 2);
    }
  }

  // doesn't even try to handle directory structure loops
  public static void traverseResources(ResourceDirectory directory, int depth) {
    if(directory == null) {
      for(int i=0; i<depth; ++i) {
        System.out.print("\t");
      }
      System.out.println("DATA");
      return;
    }

    for(int i=0; i<depth; ++i) {
      System.out.print("\t");
    }
    System.out.println("DIRECTORY");

//    System.out.println("||");
    depth++;
    for(int i=0; i<directory.size(); ++i) {
      ResourceEntry entry = directory.get(i);
      for(int j=0; j<depth; ++j) {
        System.out.print("\t");
      }
      System.out.println("ENTRY_" + Integer.toHexString(entry.getId()));

      traverseResources(entry.getDirectory(), depth+1);
    }
  }
}