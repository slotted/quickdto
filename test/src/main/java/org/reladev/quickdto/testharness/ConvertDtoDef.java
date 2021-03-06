package org.reladev.quickdto.testharness;

import java.util.List;
import java.util.Set;

import org.reladev.quickdto.shared.QuickDto;

@QuickDto
public class ConvertDtoDef {
    ConvertChildDtoDef child;
    List<ConvertChildDtoDef> childList;
    ConvertExistingDtoDef existing;
    List<ConvertExistingDtoDef> existingList;

    // duplicates to test variables in copy
    ConvertChildDtoDef child2;
    List<ConvertChildDtoDef> childList2;
    ConvertExistingDtoDef existing2;
    List<ConvertExistingDtoDef> existingList2;


    //todo support automatic conversion for other common types
    //ConvertChildDtoDef[] childArray;
    Set<ConvertChildDtoDef> childSet;
    //Map<String, ConvertChildDtoDef> childMap;

}
