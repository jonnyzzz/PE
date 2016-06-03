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
import org.boris.pecoff4j.util.IntMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PEParser {
  @NotNull
  public static PE parse(InputStream is) throws IOException {
    return read(new DataReader(is));
  }

  @NotNull
  public static PE parse(String filename) throws IOException {
    return parse(new File(filename));
  }

  @NotNull
  public static PE parse(File file) throws IOException {
    return read(new DataReader(new FileInputStream(file)));
  }

  @NotNull
  public static PE read(@NotNull IDataReader dr) throws IOException {
    PE pe = new PE();
    pe.setDosHeader(readDos(dr));

    // Check if we have an old file type
    if (pe.getDosHeader().getAddressOfNewExeHeader() == 0 ||
            pe.getDosHeader().getAddressOfNewExeHeader() > 8192) {
      return pe;
    }

    pe.setStub(readStub(pe.getDosHeader(), dr));
    pe.setSignature(readSignature(dr));

    // Check signature to ensure we have a pe/coff file
    if (!pe.getSignature().isValid()) {
      return pe;
    }

    pe.setCoffHeader(readCOFF(dr));
    pe.setOptionalHeader(readOptional(dr));
    pe.setSectionTable(readSectionHeaders(pe, dr));

    // Now read the rest of the file
    DataEntry entry;
    while ((entry = findNextEntry(pe, dr.getPosition())) != null) {
      if (entry.isSection) {
        readSection(pe, entry, dr);
      } else if (entry.isDebugRawData) {
        readDebugRawData(pe, entry, dr);
      } else {
        readImageData(pe, entry, dr);
      }
    }

    // Read any trailing data
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    int read;
    while ((read = dr.readByte()) != -1) {
      bos.write(read);
    }
    byte[] tb = bos.toByteArray();
    if (tb.length > 0) {
      pe.getImageData().setTrailingData(tb);
    }

    return pe;
  }

  @NotNull
  public static DOSHeader readDos(@NotNull IDataReader dr) throws IOException {
    DOSHeader dh = new DOSHeader();
    dh.setMagic(dr.readWord());
    dh.setUsedBytesInLastPage(dr.readWord());
    dh.setFileSizeInPages(dr.readWord());
    dh.setNumRelocationItems(dr.readWord());
    dh.setHeaderSizeInParagraphs(dr.readWord());
    dh.setMinExtraParagraphs(dr.readWord());
    dh.setMaxExtraParagraphs(dr.readWord());
    dh.setInitialSS(dr.readWord());
    dh.setInitialSP(dr.readWord());
    dh.setChecksum(dr.readWord());
    dh.setInitialIP(dr.readWord());
    dh.setInitialRelativeCS(dr.readWord());
    dh.setAddressOfRelocationTable(dr.readWord());
    dh.setOverlayNumber(dr.readWord());
    int[] reserved = new int[4];
    for (int i = 0; i < reserved.length; i++) {
      reserved[i] = dr.readWord();
    }
    dh.setReserved(reserved);
    dh.setOemId(dr.readWord());
    dh.setOemInfo(dr.readWord());
    int[] reserved2 = new int[10];
    for (int i = 0; i < reserved2.length; i++) {
      reserved2[i] = dr.readWord();
    }
    dh.setReserved2(reserved2);
    dh.setAddressOfNewExeHeader(dr.readDoubleWord());

    // calc stub size
    int stubSize = dh.getFileSizeInPages() * 512 -
            (512 - dh.getUsedBytesInLastPage());
    if (stubSize > dh.getAddressOfNewExeHeader())
      stubSize = dh.getAddressOfNewExeHeader();
    stubSize -= dh.getHeaderSizeInParagraphs() * 16;
    dh.setStubSize(stubSize);

    return dh;
  }

  @NotNull
  public static DOSStub readStub(@NotNull DOSHeader header, @NotNull IDataReader dr)
          throws IOException {
    DOSStub ds = new DOSStub();
    int pos = dr.getPosition();
    int add = header.getAddressOfNewExeHeader();
    byte[] stub = new byte[add - pos];
    dr.read(stub);
    ds.setStub(stub);
    return ds;
  }

  @NotNull
  public static PESignature readSignature(@NotNull IDataReader dr) throws IOException {
    PESignature ps = new PESignature();
    byte[] signature = new byte[4];
    dr.read(signature);
    ps.setSignature(signature);
    return ps;
  }

  @NotNull
  public static COFFHeader readCOFF(@NotNull IDataReader dr) throws IOException {
    COFFHeader h = new COFFHeader();
    h.setMachine(dr.readWord());
    h.setNumberOfSections(dr.readWord());
    h.setTimeDateStamp(dr.readDoubleWord());
    h.setPointerToSymbolTable(dr.readDoubleWord());
    h.setNumberOfSymbols(dr.readDoubleWord());
    h.setSizeOfOptionalHeader(dr.readWord());
    h.setCharacteristics(dr.readWord());
    return h;
  }

  @NotNull
  public static OptionalHeader readOptional(@NotNull IDataReader dr)
          throws IOException {
    OptionalHeader oh = new OptionalHeader();
    oh.setMagic(dr.readWord());
    boolean is64 = oh.isPE32plus();
    oh.setMajorLinkerVersion(dr.readByte());
    oh.setMinorLinkerVersion(dr.readByte());
    oh.setSizeOfCode(dr.readDoubleWord());
    oh.setSizeOfInitializedData(dr.readDoubleWord());
    oh.setSizeOfUninitializedData(dr.readDoubleWord());
    oh.setAddressOfEntryPoint(dr.readDoubleWord());
    oh.setBaseOfCode(dr.readDoubleWord());

    if (!is64)
      oh.setBaseOfData(dr.readDoubleWord());

    // NT additional fields.
    oh.setImageBase(is64 ? dr.readLong() : dr.readDoubleWord());
    oh.setSectionAlignment(dr.readDoubleWord());
    oh.setFileAlignment(dr.readDoubleWord());
    oh.setMajorOperatingSystemVersion(dr.readWord());
    oh.setMinorOperatingSystemVersion(dr.readWord());
    oh.setMajorImageVersion(dr.readWord());
    oh.setMinorImageVersion(dr.readWord());
    oh.setMajorSubsystemVersion(dr.readWord());
    oh.setMinorSubsystemVersion(dr.readWord());
    oh.setWin32VersionValue(dr.readDoubleWord());
    oh.setSizeOfImage(dr.readDoubleWord());
    oh.setSizeOfHeaders(dr.readDoubleWord());
    oh.setCheckSum(dr.readDoubleWord());
    oh.setSubsystem(dr.readWord());
    oh.setDllCharacteristics(dr.readWord());
    oh.setSizeOfStackReserve(is64 ? dr.readLong() : dr.readDoubleWord());
    oh.setSizeOfStackCommit(is64 ? dr.readLong() : dr.readDoubleWord());
    oh.setSizeOfHeapReserve(is64 ? dr.readLong() : dr.readDoubleWord());
    oh.setSizeOfHeapCommit(is64 ? dr.readLong() : dr.readDoubleWord());
    oh.setLoaderFlags(dr.readDoubleWord());
    oh.setNumberOfRvaAndSizes(dr.readDoubleWord());

    // Data directories
    ImageDataDirectory[] dds = new ImageDataDirectory[16];
    for (int i = 0; i < dds.length; i++) {
      dds[i] = readImageDD(dr);
    }
    oh.setDataDirectories(dds);

    return oh;
  }

  @NotNull
  public static ImageDataDirectory readImageDD(@NotNull IDataReader dr)
          throws IOException {
    ImageDataDirectory idd = new ImageDataDirectory();
    idd.setVirtualAddress(dr.readDoubleWord());
    idd.setSize(dr.readDoubleWord());
    return idd;
  }

  @NotNull
  public static SectionTable readSectionHeaders(@NotNull PE pe, @NotNull IDataReader dr)
          throws IOException {
    SectionTable st = new SectionTable();
    int ns = pe.getCoffHeader().getNumberOfSections();
    for (int i = 0; i < ns; i++) {
      st.add(readSectionHeader(dr));
    }

    SectionHeader[] sorted = st.getHeadersPointerSorted();
    int[] virtualAddress = new int[sorted.length];
    int[] pointerToRawData = new int[sorted.length];
    for (int i = 0; i < sorted.length; i++) {
      virtualAddress[i] = sorted[i].getVirtualAddress();
      pointerToRawData[i] = sorted[i].getPointerToRawData();
    }

    st.setRvaConverter(new RVAConverter(virtualAddress, pointerToRawData));
    return st;
  }

  @NotNull
  public static SectionHeader readSectionHeader(@NotNull IDataReader dr)
          throws IOException {
    SectionHeader sh = new SectionHeader();
    sh.setName(dr.readUtf(8));
    sh.setVirtualSize(dr.readDoubleWord());
    sh.setVirtualAddress(dr.readDoubleWord());
    sh.setSizeOfRawData(dr.readDoubleWord());
    sh.setPointerToRawData(dr.readDoubleWord());
    sh.setPointerToRelocations(dr.readDoubleWord());
    sh.setPointerToLineNumbers(dr.readDoubleWord());
    sh.setNumberOfRelocations(dr.readWord());
    sh.setNumberOfLineNumbers(dr.readWord());
    sh.setCharacteristics(dr.readDoubleWord());
    return sh;
  }

  @Nullable
  public static DataEntry findNextEntry(@NotNull PE pe, int pos) {
    DataEntry de = new DataEntry();

    // Check sections first
    int ns = pe.getCoffHeader().getNumberOfSections();
    for (int i = 0; i < ns; i++) {
      SectionHeader sh = pe.getSectionTable().getHeader(i);
      if (sh.getSizeOfRawData() > 0 && sh.getPointerToRawData() >= pos &&
              (de.pointer == 0 || sh.getPointerToRawData() < de.pointer)) {
        de.pointer = sh.getPointerToRawData();
        de.index = i;
        de.isSection = true;
      }
    }

    // Now check image data directories
    RVAConverter rvc = pe.getSectionTable().getRVAConverter();
    int dc = pe.getOptionalHeader().getDataDirectoryCount();
    for (int i = 0; i < dc; i++) {
      ImageDataDirectory idd = pe.getOptionalHeader().getDataDirectory(i);
      if (idd.getSize() > 0) {
        int prd = idd.getVirtualAddress();
        // Assume certificate live outside section ?
        if (i != ImageDataDirectoryType.CERTIFICATE_TABLE &&
                isInsideSection(pe, idd)) {
          prd = rvc.convertVirtualAddressToRawDataPointer(idd
                  .getVirtualAddress());
        }
        if (prd >= pos && (de.pointer == 0 || prd < de.pointer)) {
          de.pointer = prd;
          de.index = i;
          de.isSection = false;
        }
      }
    }

    // Check debug
    ImageData id = pe.getImageData();
    DebugDirectory dd = null;
    if (id != null)
      dd = id.getDebug();
    if (dd != null) {
      int prd = dd.getPointerToRawData();
      if (prd >= pos && (de.pointer == 0 || prd < de.pointer)) {
        de.pointer = prd;
        de.index = -1;
        de.isDebugRawData = true;
        de.isSection = false;
        de.baseAddress = prd;
      }
    }

    if (de.pointer == 0)
      return null;

    return de;
  }

  private static boolean isInsideSection(@NotNull PE pe, @NotNull ImageDataDirectory idd) {
    int prd = idd.getVirtualAddress();
    int pex = prd + idd.getSize();
    SectionTable st = pe.getSectionTable();
    int ns = st.getNumberOfSections();
    for (int i = 0; i < ns; i++) {
      SectionHeader sh = st.getHeader(i);
      int vad = sh.getVirtualAddress();
      int vex = vad + sh.getVirtualSize();
      if (prd >= vad && prd < vex && pex <= vex)
        return true;
    }
    return false;
  }

  private static void readImageData(@NotNull PE pe, @NotNull DataEntry entry, @NotNull IDataReader dr)
          throws IOException {

    // Read any preamble data
    ImageData id = pe.getImageData();
    byte[] pa = readPreambleData(entry.pointer, dr);
    if (pa != null)
      id.put(entry.index, pa);

    // Read the image data
    ImageDataDirectory idd = pe.getOptionalHeader().getDataDirectory(
            entry.index);
    byte[] b = new byte[idd.getSize()];
    dr.read(b);

    switch (entry.index) {
      case ImageDataDirectoryType.EXPORT_TABLE:
        id.setExportTable(readExportDirectory(b));
        break;
      case ImageDataDirectoryType.IMPORT_TABLE:
        id.setImportTable(readImportDirectory(b, entry.baseAddress));
        break;
      case ImageDataDirectoryType.RESOURCE_TABLE:
        id.setResourceTable(readResourceDirectory(b, entry.baseAddress));
        break;
      case ImageDataDirectoryType.EXCEPTION_TABLE:
        id.setExceptionTable(b);
        break;
      case ImageDataDirectoryType.CERTIFICATE_TABLE:
        id.setCertificateTable(b);
        break;
      case ImageDataDirectoryType.BASE_RELOCATION_TABLE:
        id.setBaseRelocationTable(b);
        break;
      case ImageDataDirectoryType.DEBUG:
        id.setDebug(readDebugDirectory(b));
        break;
      case ImageDataDirectoryType.ARCHITECTURE:
        id.setArchitecture(b);
        break;
      case ImageDataDirectoryType.GLOBAL_PTR:
        id.setGlobalPtr(b);
        break;
      case ImageDataDirectoryType.TLS_TABLE:
        id.setTlsTable(b);
        break;
      case ImageDataDirectoryType.LOAD_CONFIG_TABLE:
        id.setLoadConfigTable(readLoadConfigDirectory(b));
        break;
      case ImageDataDirectoryType.BOUND_IMPORT:
        id.setBoundImports(readBoundImportDirectoryTable(b));
        break;
      case ImageDataDirectoryType.IAT:
        id.setIat(b);
        break;
      case ImageDataDirectoryType.DELAY_IMPORT_DESCRIPTOR:
        id.setDelayImportDescriptor(b);
        break;
      case ImageDataDirectoryType.CLR_RUNTIME_HEADER:
        id.setClrRuntimeHeader(b);
        break;
      case ImageDataDirectoryType.RESERVED:
        id.setReserved(b);
        break;
    }
  }

  @Nullable
  private static byte[] readPreambleData(int pointer, @NotNull IDataReader dr)
          throws IOException {
    if (pointer > dr.getPosition()) {
      final byte[] pa = new byte[pointer - dr.getPosition()];
      dr.read(pa);
      boolean zeroes = true;
      for (byte aPa : pa) {
        if (aPa != 0) {
          zeroes = false;
          break;
        }
      }
      if (!zeroes)
        return pa;
    }

    return null;
  }

  private static void readDebugRawData(@NotNull PE pe,
                                       @NotNull DataEntry entry,
                                       @NotNull IDataReader dr) throws IOException {
    // Read any preamble data
    final ImageData id = pe.getImageData();
    final byte[] pa = readPreambleData(entry.pointer, dr);
    if (pa != null) id.setDebugRawDataPreamble(pa);
    final DebugDirectory dd = id.getDebug();
    final byte[] b = new byte[dd.getSizeOfData()];
    dr.read(b);
    id.setDebugRawData(b);
  }

  private static void readSection(@NotNull PE pe, @NotNull DataEntry entry, @NotNull IDataReader dr) throws IOException {
    SectionTable st = pe.getSectionTable();
    SectionHeader sh = st.getHeader(entry.index);
    SectionData sd = new SectionData();

    // Read any preamble - store if non-zero
    byte[] pa = readPreambleData(sh.getPointerToRawData(), dr);
    if (pa != null)
      sd.setPreamble(pa);

    // Read in the raw data block
    dr.jumpTo(sh.getPointerToRawData());
    byte[] b = new byte[sh.getSizeOfRawData()];
    dr.read(b);
    sd.setData(b);
    st.put(entry.index, sd);

    // Check for an image directory within this section
    int ddc = pe.getOptionalHeader().getDataDirectoryCount();
    for (int i = 0; i < ddc; i++) {
      if (i == ImageDataDirectoryType.CERTIFICATE_TABLE)
        continue;
      ImageDataDirectory idd = pe.getOptionalHeader().getDataDirectory(i);
      if (idd.getSize() > 0) {
        int vad = sh.getVirtualAddress();
        int vex = vad + sh.getVirtualSize();
        int dad = idd.getVirtualAddress();
        if (dad >= vad && dad < vex) {
          int off = dad - vad;
          off += 32;
          IDataReader idr = new ByteArrayDataReader(b, off, idd.getSize());
          DataEntry de = new DataEntry(i, 0);
          de.baseAddress = sh.getVirtualAddress();
          readImageData(pe, de, idr);
        }
      }
    }
  }

  @NotNull
  private static BoundImportDirectoryTable readBoundImportDirectoryTable(
          byte[] b) throws IOException {
    DataReader dr = new DataReader(b);
    BoundImportDirectoryTable bidt = new BoundImportDirectoryTable();
    List<BoundImport> imports = new ArrayList<BoundImport>();
    BoundImport bi;
    while ((bi = readBoundImport(dr)) != null) {
      bidt.add(bi);
      imports.add(bi);
    }
    Collections.sort(imports, new Comparator<BoundImport>() {
      public int compare(@NotNull BoundImport o1, @NotNull BoundImport o2) {
        return o1.getOffsetToModuleName() - o2.getOffsetToModuleName();
      }
    });
    IntMap names = new IntMap();
    for (BoundImport anImport : imports) {
      bi = anImport;
      int offset = bi.getOffsetToModuleName();
      String n = (String) names.get(offset);
      if (n == null) {
        dr.jumpTo(offset);
        n = dr.readUtf();
        names.put(offset, n);
      }
      bi.setModuleName(n);
    }
    return bidt;
  }

  @Nullable
  private static BoundImport readBoundImport(@NotNull IDataReader dr) throws IOException {
    BoundImport bi = new BoundImport();
    bi.setTimestamp(dr.readDoubleWord());
    bi.setOffsetToModuleName(dr.readWord());
    bi.setNumberOfModuleForwarderRefs(dr.readWord());

    if (bi.getTimestamp() == 0 && bi.getOffsetToModuleName() == 0 &&
            bi.getNumberOfModuleForwarderRefs() == 0)
      return null;

    return bi;
  }

  @NotNull
  public static ImportDirectory readImportDirectory(byte[] b,
                                                    final int baseAddress) throws IOException {
    DataReader dr = new DataReader(b);
    ImportDirectory id = new ImportDirectory();
    ImportDirectoryEntry ide = null;
    while ((ide = readImportDirectoryEntry(dr)) != null) {
      id.add(ide);
    }

        /* FIXME - name table refer to data outside image directory
        for (int i = 0; i < id.size(); i++) {
            ImportDirectoryEntry e = id.getEntry(i);
            dr.jumpTo(e.getNameRVA() - baseAddress);
            String name = dr.readUtf();
            dr.jumpTo(e.getImportLookupTableRVA() - baseAddress);
            ImportDirectoryTable nt = readImportDirectoryTable(dr, baseAddress);
            dr.jumpTo(e.getImportAddressTableRVA() - baseAddress);
            ImportDirectoryTable at = null; // readImportDirectoryTable(dr,
            // baseAddress);
            id.add(name, nt, at);
        }*/

    return id;
  }

  @Nullable
  public static ImportDirectoryEntry readImportDirectoryEntry(@NotNull IDataReader dr) throws IOException {
    ImportDirectoryEntry ide = new ImportDirectoryEntry();
    ide.setImportLookupTableRVA(dr.readDoubleWord());
    ide.setTimeDateStamp(dr.readDoubleWord());
    ide.setForwarderChain(dr.readDoubleWord());
    ide.setNameRVA(dr.readDoubleWord());
    ide.setImportAddressTableRVA(dr.readDoubleWord());

    // The last entry is null
    if (ide.getImportLookupTableRVA() == 0) {
      return null;
    }

    return ide;
  }

  @NotNull
  public static ImportDirectoryTable readImportDirectoryTable(@NotNull final IDataReader dr,
                                                              final int baseAddress) throws IOException {
    ImportDirectoryTable idt = new ImportDirectoryTable();
    ImportEntry ie;
    while ((ie = readImportEntry(dr)) != null) {
      idt.add(ie);
    }

    for (int i = 0; i < idt.size(); i++) {
      ImportEntry iee = idt.getEntry(i);
      if ((iee.getVal() & 0x80000000) != 0) {
        iee.setOrdinal(iee.getVal() & 0x7fffffff);
      } else {
        dr.jumpTo(iee.getVal() - baseAddress);
        dr.readWord(); // FIXME this is an index into the export table
        iee.setName(dr.readUtf());
      }
    }
    return idt;
  }

  @Nullable
  public static ImportEntry readImportEntry(@NotNull IDataReader dr)
          throws IOException {
    ImportEntry ie = new ImportEntry();
    ie.setVal(dr.readDoubleWord());
    if (ie.getVal() == 0) {
      return null;
    }

    return ie;
  }

  @NotNull
  public static ExportDirectory readExportDirectory(byte[] b)
          throws IOException {
    DataReader dr = new DataReader(b);
    ExportDirectory edt = new ExportDirectory();
    edt.set(b);
    edt.setExportFlags(dr.readDoubleWord());
    edt.setTimeDateStamp(dr.readDoubleWord());
    edt.setMajorVersion(dr.readWord());
    edt.setMinorVersion(dr.readWord());
    edt.setNameRVA(dr.readDoubleWord());
    edt.setOrdinalBase(dr.readDoubleWord());
    edt.setAddressTableEntries(dr.readDoubleWord());
    edt.setNumberOfNamePointers(dr.readDoubleWord());
    edt.setExportAddressTableRVA(dr.readDoubleWord());
    edt.setNamePointerRVA(dr.readDoubleWord());
    edt.setOrdinalTableRVA(dr.readDoubleWord());
    return edt;
  }

  @NotNull
  public static LoadConfigDirectory readLoadConfigDirectory(byte[] b)
          throws IOException {
    DataReader dr = new DataReader(b);
    LoadConfigDirectory lcd = new LoadConfigDirectory();
    lcd.set(b);
    lcd.setCharacteristics(dr.readDoubleWord());
    lcd.setTimeDateStamp(dr.readDoubleWord());
    lcd.setMajorVersion(dr.readWord());
    lcd.setMinorVersion(dr.readWord());
    lcd.setGlobalFlagsClear(dr.readDoubleWord());
    lcd.setGlobalFlagsSet(dr.readDoubleWord());
    lcd.setCriticalSectionDefaultTimeout(dr.readDoubleWord());
    lcd.setDeCommitFreeBlockThreshold(dr.readLong());
    lcd.setDeCommitTotalFreeThreshold(dr.readLong());
    lcd.setLockPrefixTable(dr.readLong());
    lcd.setMaximumAllocationSize(dr.readLong());
    lcd.setVirtualMemoryThreshold(dr.readLong());
    lcd.setProcessAffinityMask(dr.readLong());
    lcd.setProcessHeapFlags(dr.readDoubleWord());
    lcd.setCsdVersion(dr.readWord());
    lcd.setReserved(dr.readWord());
    lcd.setEditList(dr.readLong());
    lcd.setSecurityCookie(dr.readDoubleWord());
    lcd.setSeHandlerTable(dr.readDoubleWord());
    lcd.setSeHandlerCount(dr.readDoubleWord());

    return lcd;
  }

  @NotNull
  public static DebugDirectory readDebugDirectory(byte[] b)
          throws IOException {
    return readDebugDirectory(b, new DataReader(b));
  }

  @NotNull
  public static DebugDirectory readDebugDirectory(byte[] b, @NotNull IDataReader dr)
          throws IOException {
    DebugDirectory dd = new DebugDirectory();
    dd.set(b);
    dd.setCharacteristics(dr.readDoubleWord());
    dd.setTimeDateStamp(dr.readDoubleWord());
    dd.setMajorVersion(dr.readWord());
    dd.setMajorVersion(dr.readWord());
    dd.setType(dr.readDoubleWord());
    dd.setSizeOfData(dr.readDoubleWord());
    dd.setAddressOfRawData(dr.readDoubleWord());
    dd.setPointerToRawData(dr.readDoubleWord());
    return dd;
  }

  @NotNull
  private static ResourceDirectory readResourceDirectory(byte[] b,
                                                         int baseAddress) throws IOException {
    IDataReader dr = new ByteArrayDataReader(b);
    return readResourceDirectory(dr, baseAddress);
  }

  @NotNull
  private static ResourceDirectory readResourceDirectory(@NotNull IDataReader dr,
                                                         int baseAddress) throws IOException {
    ResourceDirectory d = new ResourceDirectory();
    d.setTable(readResourceDirectoryTable(dr));
    int ne = d.getTable().getNumNameEntries() +
            d.getTable().getNumIdEntries();
    ResourceEntry resourceEntry = null;
    int count = 0;
    while((resourceEntry = readResourceEntry(dr, baseAddress)) != null && count < ne) {
      d.add(resourceEntry);
      count++;
    }

    return d;
  }

  @Nullable
  private static ResourceEntry readResourceEntry(@NotNull IDataReader dr,
                                                 int baseAddress) throws IOException {
    ResourceEntry re = new ResourceEntry();
    int id = dr.readDoubleWord();
    int offset = dr.readDoubleWord();
    int pos = dr.getPosition();
    if ((id & 0x80000000) != 0) {
      dr.jumpTo(id & 0x7fffffff);
      if(dr.getPosition() < 0) return null;
      re.setName(dr.readUnicode());
    } else {
      re.setId(id);
    }
    if ((offset & 0x80000000) != 0) {
      dr.jumpTo(offset & 0x7fffffff);
      if(dr.getPosition() < 0) return null;
      re.setDirectory(readResourceDirectory(dr, baseAddress));
    } else {
      dr.jumpTo(offset);
      if(dr.getPosition() < 0) return null;
      int rva = dr.readDoubleWord();
      int size = dr.readDoubleWord();
      int cp = dr.readDoubleWord();
      int res = dr.readDoubleWord();
      re.setCodePage(cp);
      re.setReserved(res);
      dr.jumpTo(rva - baseAddress);
      if(dr.getPosition() < 0) return null;
      byte[] b = new byte[size];
      dr.read(b);
      re.setData(b);
    }
    dr.jumpTo(pos);
    return re;
  }

  @NotNull
  private static ResourceDirectoryTable readResourceDirectoryTable(
          @NotNull IDataReader dr) throws IOException {
    ResourceDirectoryTable t = new ResourceDirectoryTable();
    t.setCharacteristics(dr.readDoubleWord());
    t.setTimeDateStamp(dr.readDoubleWord());
    t.setMajorVersion(dr.readWord());
    t.setMinVersion(dr.readWord());
    t.setNumNameEntries(dr.readWord());
    t.setNumIdEntries(dr.readWord());

    return t;
  }
}
