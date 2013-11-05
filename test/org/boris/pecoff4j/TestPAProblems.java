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

import org.boris.pecoff4j.constant.ImageDataDirectoryType;
import org.boris.pecoff4j.io.PEAssembler;
import org.boris.pecoff4j.io.PEParser;
import org.boris.pecoff4j.util.Diff;
import org.boris.pecoff4j.util.IO;
import org.boris.pecoff4j.util.Reflection;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class TestPAProblems {
  // Solved
  @NotNull
  static String P1 = "C:\\windows\\system32\\makecab.exe";
  @NotNull
  static String P2 = "C:\\windows\\system32\\ds32gt.dll";
  @NotNull
  static String P3 = "C:\\windows\\system32\\usrrtosa.dll";
  @NotNull
  static String P4 = "C:\\windows\\system32\\xvidcore.dll";
  @NotNull
  static String P5 = "C:\\windows\\system32\\narrator.exe";
  @NotNull
  static String P6 = "C:\\windows\\system32\\Setup\\msmqocm.dll";
  @NotNull
  static String P7 = "C:\\windows\\system32\\esentprf.dll";
  @NotNull
  static String P8 = "C:\\windows\\system32\\MRT.exe";
  @NotNull
  static String P9 = "C:\\windows\\system32\\dgsetup.dll";
  @NotNull
  static String PA = "C:\\windows\\system32\\fde.dll";
  @NotNull
  static String PB = "C:\\windows\\system32\\usrrtosa.dll";
  @NotNull
  static String PC = "C:\\windows\\system32\\SoftwareDistribution\\Setup\\ServiceStartup\\wups2.dll\\7.2.6001.784\\wups2.dll";
  @NotNull
  static String PD = "C:\\windows\\system32\\SoftwareDistribution\\Setup\\ServiceStartup\\wups.dll\\7.0.6000.381\\wups.dll";
  @NotNull
  static String PE = "C:\\windows\\system32\\wuapi.dll";
  @NotNull
  static String PF = "C:\\windows\\system32\\sqlunirl.dll";
  @NotNull
  static String PI = "C:\\windows\\system32\\msjetoledb40.dll";

  // Outstanding
  @NotNull
  static String PG = "C:\\windows\\system32\\Macromed\\Flash\\uninstall_plugin.exe";
  @NotNull
  static String PL = "F:\\Program Files\\FlashGet\\uninst.exe";
  @NotNull
  static String PM = "F:\\Program Files\\Jeskola Buzz\\Dev\\Mdk\\dsplib.dll";
  @NotNull
  static String PN = "F:\\Program Files\\Jeskola Buzz\\Gear\\Generators\\MarC mp3loader.dll";

  // 64-bit
  @NotNull
  static String PJ = "C:\\Program Files\\WinRAR\\RarExt64.dll";
  @NotNull
  static String PK = "F:\\Program Files\\Microsoft Platform SDK\\NoRedist\\Win64\\msvcrtd.dll";
  @NotNull
  static String PO = "F:\\Program Files\\FileZilla FTP Client\\fzshellext_64.dll";
  @NotNull
  static String PP = "F:\\Program Files\\Microsoft Platform SDK\\NoRedist\\Win64\\msvcirtd.dll";
  @NotNull
  static String PQ = "F:\\Program Files\\Last.fm\\VistaLib64.dll";

  // Image data parsing
  @NotNull
  static String I1 = "C:\\windows\\system32\\mswstr10.dll";
  @NotNull
  static String T2 = "C:\\windows\\system32\\divx.dll";
  @NotNull
  static String T3 = "C:\\windows\\system32\\msvbvm60.dll";
  @NotNull
  static String T4 = "C:\\Program Files\\Image-Line\\Downloader\\FLDownload.dll";
  @NotNull
  static String T5 = "C:\\Program Files\\Image-Line\\PoiZone\\PoiZone.dll";
  @NotNull
  static String T6 = "C:\\Program Files\\Image-Line\\Toxic Biohazard\\Toxic Biohazard.dll";
  @NotNull
  static String T7 = "F:\\Program Files\\IrfanView\\i_view32.exe";
  @NotNull
  static String T8 = "F:\\Program Files\\Jeskola Buzz\\Buzz.exe";

  public static void main(String[] args) throws Exception {
    testAll();
    // test(T8);
  }

  public static void testAll() throws Exception {
    Field[] fields = TestPAProblems.class.getDeclaredFields();
    for (int i = 0; i < fields.length; i++) {
      Field f = fields[i];
      if (!Modifier.isStatic(f.getModifiers()))
        continue;
      String filename = (String) f.get(null);
      if (test(filename))
        dumpVA(filename);
    }
  }

  public static boolean test(String s) throws Exception {
    File f = new File(s);
    System.out.println(f);
    byte[] b1 = IO.toBytes(f);
    PE pe = PEParser.parse(f);
    byte[] b2 = PEAssembler.toBytes(pe);
    return Diff.findDiff(b1, b2, false);
  }

  public static void dumpVA(String s) throws Exception {
    File f = new File(s);
    // System.out.println();
    // System.out.println(f);
    System.out.println();
    PE pe = PEParser.parse(f);
    SectionTable st = pe.getSectionTable();
    System.out.println("name\tprd \tdex \tvad  \tvex");
    System.out.println("========================================");
    for (int i = 0; i < st.getNumberOfSections(); i++) {
      SectionHeader sh = st.getHeader(i);
      int dex = sh.getPointerToRawData() + sh.getSizeOfRawData();
      int vex = sh.getVirtualAddress() + sh.getVirtualSize();
      System.out.println(sh.getName() + "\t" +
              make4(Integer.toHexString(sh.getPointerToRawData())) +
              "\t" + make4(Integer.toHexString(dex)) + "\t" +
              make4(Integer.toHexString(sh.getVirtualAddress())) + "\t" +
              make4(Integer.toHexString(vex)));
    }

    System.out.println();
    int dc = pe.getOptionalHeader().getDataDirectoryCount();
    for (int i = 0; i < dc; i++) {
      ImageDataDirectory idd = pe.getOptionalHeader().getDataDirectory(i);
      if (idd.getSize() > 0) {
        String n = Reflection.getConstantName(
                ImageDataDirectoryType.class, i);
        while (n.length() < 20) {
          n = n + " ";
        }
        System.out.println(n +
                "\t" +
                Integer.toHexString(idd.getVirtualAddress()) +
                "\t" +
                Integer.toHexString(idd.getVirtualAddress() +
                        idd.getSize()));
      }
    }
    System.out.println();
  }

  @NotNull
  private static String make4(@NotNull String s) {
    while (s.length() < 4) {
      s = " " + s;
    }
    return s;
  }
}
