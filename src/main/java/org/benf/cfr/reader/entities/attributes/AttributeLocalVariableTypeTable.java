package org.benf.cfr.reader.entities.attributes;

import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.List;

public class AttributeLocalVariableTypeTable extends Attribute {
    public final static String ATTRIBUTE_NAME = "LocalVariableTypeTable";

    private static final long OFFSET_OF_ATTRIBUTE_LENGTH = 2;
    private static final long OFFSET_OF_ENTRY_COUNT = 6;
    private static final long OFFSET_OF_ENTRIES = 8;
    private static final long OFFSET_OF_REMAINDER = 6;

    private final List<LocalVariableTypeEntry> localVariableTypeEntryList = ListFactory.newList();
    private final int length;

    public AttributeLocalVariableTypeTable(ByteData raw) {
        this.length = raw.getS4At(OFFSET_OF_ATTRIBUTE_LENGTH);
        int numLocalVariables = raw.getU2At(OFFSET_OF_ENTRY_COUNT);
        long offset = OFFSET_OF_ENTRIES;
        for (int x = 0; x < numLocalVariables; ++x) {
            int startPc = raw.getU2At(offset);
            int length = raw.getU2At(offset + 2);
            int nameIndex = raw.getU2At(offset + 4);
            int signatureIndex = raw.getU2At(offset + 6);
            int index = raw.getU2At(offset + 8);
            localVariableTypeEntryList.add(new LocalVariableTypeEntry(startPc, length, nameIndex, signatureIndex, index));
            offset += 10;
        }
    }

    @Override
    public String getRawName() {
        return ATTRIBUTE_NAME;
    }

    @Override
    public Dumper dump(Dumper d) {
        return d;
    }

    public List<LocalVariableTypeEntry> getLocalVariableTypeEntryList() {
        return localVariableTypeEntryList;
    }

    @Override
    public long getRawByteLength() {
        return OFFSET_OF_REMAINDER + length;
    }
}
