/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     Peter Smith
 *******************************************************************************/
package org.boris.pecoff4j.io;

import org.boris.pecoff4j.*;
import org.boris.pecoff4j.constant.ImageDataDirectoryType;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;

public class PEAssembler {
  public static byte[] toBytes(@NotNull PE pe) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    write(pe, bos);
    return bos.toByteArray();
  }

  public static void write(@NotNull PE pe, String filename) throws IOException {
    write(pe, new FileOutputStream(filename));
  }

  public static void write(@NotNull PE pe, File file) throws IOException {
    write(pe, new FileOutputStream(file));
  }

  public static void write(@NotNull PE pe, OutputStream os) throws IOException {
    DataWriter dw = new DataWriter(os);
    write(pe, dw);
    dw.flush();
  }

  public static void write(@NotNull PE pe, @NotNull IDataWriter dw) throws IOException {
    write(pe.getDosHeader(), dw);
    write(pe.getStub(), dw);
    write(pe.getSignature(), dw);
    write(pe.getCoffHeader(), dw);
    write(pe.getOptionalHeader(), dw);
    writeSectionHeaders(pe, dw);

    // Now write out the rest
    DataEntry entry = null;
    while ((entry = PEParser.findNextEntry(pe, dw.getPosition())) != null) {
      if (entry.isSection) {
        writeSection(pe, entry, dw);
      } else if (entry.isDebugRawData) {
        writeDebugRawData(pe, entry, dw);
      } else {
        writeImageData(pe, entry, dw);
      }
    }

    // Dump out any trailing data - TODO find out what this is
    byte[] tb = pe.getImageData().getTrailingData();
    if (tb != null)
      dw.writeBytes(tb);
  }

  private static void write(@NotNull DOSHeader dh, @NotNull IDataWriter dw) throws IOException {
    dw.writeWord(dh.getMagic());
    dw.writeWord(dh.getUsedBytesInLastPage());
    dw.writeWord(dh.getFileSizeInPages());
    dw.writeWord(dh.getNumRelocationItems());
    dw.writeWord(dh.getHeaderSizeInParagraphs());
    dw.writeWord(dh.getMinExtraParagraphs());
    dw.writeWord(dh.getMaxExtraParagraphs());
    dw.writeWord(dh.getInitialSS());
    dw.writeWord(dh.getInitialSP());
    dw.writeWord(dh.getChecksum());
    dw.writeWord(dh.getInitialIP());
    dw.writeWord(dh.getInitialRelativeCS());
    dw.writeWord(dh.getAddressOfRelocationTable());
    dw.writeWord(dh.getOverlayNumber());
    int[] res = dh.getReserved();
    for (int i = 0; i < res.length; i++) {
      dw.writeWord(res[i]);
    }
    dw.writeWord(dh.getOemId());
    dw.writeWord(dh.getOemInfo());
    int[] res2 = dh.getReserved2();
    for (int i = 0; i < res2.length; i++) {
      dw.writeWord(res2[i]);
    }
    dw.writeDoubleWord(dh.getAddressOfNewExeHeader());
  }

  private static void write(@NotNull DOSStub stub, @NotNull IDataWriter dw) throws IOException {
    dw.writeBytes(stub.getStub());
  }

  private static void write(@NotNull PESignature s, @NotNull IDataWriter dw) throws IOException {
    dw.writeBytes(s.getSignature());
  }

  private static void write(@NotNull COFFHeader ch, @NotNull IDataWriter dw) throws IOException {
    dw.writeWord(ch.getMachine());
    dw.writeWord(ch.getNumberOfSections());
    dw.writeDoubleWord(ch.getTimeDateStamp());
    dw.writeDoubleWord(ch.getPointerToSymbolTable());
    dw.writeDoubleWord(ch.getNumberOfSymbols());
    dw.writeWord(ch.getSizeOfOptionalHeader());
    dw.writeWord(ch.getCharacteristics());
  }

  private static void write(@NotNull OptionalHeader oh, @NotNull IDataWriter dw)
          throws IOException {
    boolean is64 = oh.isPE32plus();

    dw.writeWord(oh.getMagic());
    dw.writeByte(oh.getMajorLinkerVersion());
    dw.writeByte(oh.getMinorLinkerVersion());
    dw.writeDoubleWord(oh.getSizeOfCode());
    dw.writeDoubleWord(oh.getSizeOfInitializedData());
    dw.writeDoubleWord(oh.getSizeOfUninitializedData());
    dw.writeDoubleWord(oh.getAddressOfEntryPoint());
    dw.writeDoubleWord(oh.getBaseOfCode());
    if (!is64)
      dw.writeDoubleWord(oh.getBaseOfData());

    // NT additional fields.
    if (is64)
      dw.writeLong(oh.getImageBase());
    else
      dw.writeDoubleWord((int) oh.getImageBase());

    dw.writeDoubleWord(oh.getSectionAlignment());
    dw.writeDoubleWord(oh.getFileAlignment());
    dw.writeWord(oh.getMajorOperatingSystemVersion());
    dw.writeWord(oh.getMinorOperatingSystemVersion());
    dw.writeWord(oh.getMajorImageVersion());
    dw.writeWord(oh.getMinorImageVersion());
    dw.writeWord(oh.getMajorSubsystemVersion());
    dw.writeWord(oh.getMinorSubsystemVersion());
    dw.writeDoubleWord(oh.getWin32VersionValue());
    dw.writeDoubleWord(oh.getSizeOfImage());
    dw.writeDoubleWord(oh.getSizeOfHeaders());
    dw.writeDoubleWord(oh.getCheckSum());
    dw.writeWord(oh.getSubsystem());
    dw.writeWord(oh.getDllCharacteristics());
    if (is64) {
      dw.writeLong(oh.getSizeOfStackReserve());
      dw.writeLong(oh.getSizeOfStackCommit());
      dw.writeLong(oh.getSizeOfHeapReserve());
      dw.writeLong(oh.getSizeOfHeapCommit());
    } else {
      dw.writeDoubleWord((int) oh.getSizeOfStackReserve());
      dw.writeDoubleWord((int) oh.getSizeOfStackCommit());
      dw.writeDoubleWord((int) oh.getSizeOfHeapReserve());
      dw.writeDoubleWord((int) oh.getSizeOfHeapCommit());
    }

    dw.writeDoubleWord(oh.getLoaderFlags());
    dw.writeDoubleWord(oh.getNumberOfRvaAndSizes());

    // Data directories
    int ddc = oh.getDataDirectoryCount();
    for (int i = 0; i < ddc; i++) {
      write(oh.getDataDirectory(i), dw);
    }
  }

  private static void write(@NotNull ImageDataDirectory idd, @NotNull IDataWriter dw)
          throws IOException {
    dw.writeDoubleWord(idd.getVirtualAddress());
    dw.writeDoubleWord(idd.getSize());
  }

  private static void writeSectionHeaders(@NotNull PE pe, @NotNull IDataWriter dw)
          throws IOException {
    SectionTable st = pe.getSectionTable();
    int ns = st.getNumberOfSections();
    for (int i = 0; i < ns; i++) {
      SectionHeader sh = st.getHeader(i);
      writeSectionHeader(sh, dw);
    }
  }

  private static void writeSectionHeader(@NotNull SectionHeader sh, @NotNull IDataWriter dw)
          throws IOException {
    dw.writeUtf(sh.getName(), 8);
    dw.writeDoubleWord(sh.getVirtualSize());
    dw.writeDoubleWord(sh.getVirtualAddress());
    dw.writeDoubleWord(sh.getSizeOfRawData());
    dw.writeDoubleWord(sh.getPointerToRawData());
    dw.writeDoubleWord(sh.getPointerToRelocations());
    dw.writeDoubleWord(sh.getPointerToLineNumbers());
    dw.writeWord(sh.getNumberOfRelocations());
    dw.writeWord(sh.getNumberOfLineNumbers());
    dw.writeDoubleWord(sh.getCharacteristics());
  }

  private static void writeImageData(@NotNull PE pe, @NotNull DataEntry entry, @NotNull IDataWriter dw)
          throws IOException {
    ImageDataDirectory idd = pe.getOptionalHeader().getDataDirectory(
            entry.index);
    RVAConverter rvc = pe.getSectionTable().getRVAConverter();
    int prd = idd.getVirtualAddress();
    if (entry.index != ImageDataDirectoryType.CERTIFICATE_TABLE)
      prd = rvc.convertVirtualAddressToRawDataPointer(idd
              .getVirtualAddress());
    if (prd > dw.getPosition()) {
      byte[] pa = pe.getImageData().getPreamble(entry.index);
      if (pa != null)
        dw.writeBytes(pa);
      else
        dw.writeByte(0, prd - dw.getPosition());
    }

    ImageData id = pe.getImageData();

    switch (entry.index) {
      case ImageDataDirectoryType.EXPORT_TABLE:
        dw.writeBytes(id.getExportTable().get());
        break;
      case ImageDataDirectoryType.IMPORT_TABLE:
        dw.writeBytes(id.getImportTable().get());
        break;
      case ImageDataDirectoryType.RESOURCE_TABLE:
        dw.writeBytes(id.getResourceTable().get());
        break;
      case ImageDataDirectoryType.EXCEPTION_TABLE:
        dw.writeBytes(id.getExceptionTable());
        break;
      case ImageDataDirectoryType.CERTIFICATE_TABLE:
        dw.writeBytes(id.getCertificateTable());
        break;
      case ImageDataDirectoryType.BASE_RELOCATION_TABLE:
        dw.writeBytes(id.getBaseRelocationTable());
        break;
      case ImageDataDirectoryType.DEBUG:
        dw.writeBytes(id.getDebug().get());
        break;
      case ImageDataDirectoryType.ARCHITECTURE:
        dw.writeBytes(id.getArchitecture());
        break;
      case ImageDataDirectoryType.GLOBAL_PTR:
        dw.writeBytes(id.getGlobalPtr());
        break;
      case ImageDataDirectoryType.TLS_TABLE:
        dw.writeBytes(id.getTlsTable());
        break;
      case ImageDataDirectoryType.LOAD_CONFIG_TABLE:
        break;
      case ImageDataDirectoryType.BOUND_IMPORT:
        write(pe, id.getBoundImports(), dw);
        break;
      case ImageDataDirectoryType.IAT:
        dw.writeBytes(id.getIat());
        break;
      case ImageDataDirectoryType.DELAY_IMPORT_DESCRIPTOR:
        dw.writeBytes(id.getDelayImportDescriptor());
        break;
      case ImageDataDirectoryType.CLR_RUNTIME_HEADER:
        dw.writeBytes(id.getClrRuntimeHeader());
        break;
      case ImageDataDirectoryType.RESERVED:
        dw.writeBytes(id.getReserved());
        break;
    }
  }

  private static void writeDebugRawData(@NotNull PE pe, @NotNull DataEntry entry, @NotNull IDataWriter dw)
          throws IOException {
    if (entry.pointer > dw.getPosition()) {
      byte[] pa = pe.getImageData().getDebugRawDataPreamble();
      if (pa != null)
        dw.writeBytes(pa);
      else
        dw.writeByte(0, entry.pointer - dw.getPosition());
    }
    dw.writeBytes(pe.getImageData().getDebugRawData());
  }

  private static void writeSection(@NotNull PE pe, @NotNull DataEntry entry, @NotNull IDataWriter dw)
          throws IOException {
    SectionTable st = pe.getSectionTable();
    SectionHeader sh = st.getHeader(entry.index);
    SectionData sd = st.getSection(entry.index);
    int prd = sh.getPointerToRawData();
    if (prd > dw.getPosition()) {
      byte[] pa = sd.getPreamble();
      if (pa != null) {
        dw.writeBytes(pa);
      } else {
        dw.writeByte(0, prd - dw.getPosition());
      }
    }

    byte[] b = sd.getData();
    dw.writeBytes(b);
  }

  private static void write(@NotNull PE pe, @NotNull BoundImportDirectoryTable bidt,
                            @NotNull IDataWriter dw) throws IOException {
    int pos = dw.getPosition();
    List<BoundImport> bil = new ArrayList();

    for (int i = 0; i < bidt.size(); i++) {
      BoundImport bi = bidt.get(i);
      bil.add(bi);
      dw.writeDoubleWord((int) bi.getTimestamp());
      dw.writeWord(bi.getOffsetToModuleName());
      dw.writeWord(bi.getNumberOfModuleForwarderRefs());
    }

    Collections.sort(bil, new Comparator<BoundImport>() {
      public int compare(@NotNull BoundImport o1, @NotNull BoundImport o2) {
        return o1.getOffsetToModuleName() - o2.getOffsetToModuleName();
      }
    });

    // Now write out empty block
    dw.writeDoubleWord(0);
    dw.writeDoubleWord(0);

    // Now write out module names
    Set names = new HashSet();
    for (int i = 0; i < bil.size(); i++) {
      String s = bil.get(i).getModuleName();
      if (!names.contains(s))
        dw.writeUtf(s);
      names.add(s);
    }

    // Check for empty block at end - padding for alignment
    int dpos = dw.getPosition() - pos;
    int bis = pe.getOptionalHeader().getDataDirectory(
            ImageDataDirectoryType.BOUND_IMPORT).getSize();
    if (bis > dpos) {
      dw.writeByte(0, bis - dpos);
    }
  }
}
