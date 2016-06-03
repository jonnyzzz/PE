package org.boris.pecoff4j;

import org.boris.pecoff4j.io.PEParser;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;

// author douggard

public class TestImportTable {
  public static void main(String[] args) throws Exception {
    File file = new File("/tmp/bc9ab25f23827689fded588e1cadefc923e32a33a5c759c7c6b94fc7915bb6e1");
    PE pe = PEParser.parse(file);
    RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
    ImageData imageData = pe.getImageData();
    ImportDirectory importTable = imageData.getImportTable();
    SectionTable sectionTable = pe.getSectionTable();
    RVAConverter rvaConverter = sectionTable.getRVAConverter();

    for (int i = 0; i < importTable.size(); ++i) {
      ImportDirectoryEntry entry = importTable.getEntry(i);
      int nameRVA = entry.getNameRVA();
      int nameOffset = rvaConverter.convertVirtualAddressToRawDataPointer(nameRVA);
      randomAccessFile.seek(nameOffset);
      String libName = readString(randomAccessFile);
      System.out.println(libName);
    }
  }

  public static String readString(RandomAccessFile randomAccessFile) throws Exception {
    byte[] byteString = new byte[512];
    int count = 0;

    while(count < 512 && (byteString[count] = (byte)randomAccessFile.read()) != 0) {
      count += 1;
    }

    byteString = Arrays.copyOf(byteString, count);

    return new String(byteString);
  }
}