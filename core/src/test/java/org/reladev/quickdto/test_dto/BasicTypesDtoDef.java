package org.reladev.quickdto.test_dto;


import org.reladev.quickdto.feature.DirtyFeature;
import org.reladev.quickdto.shared.GwtIncompatible;
import org.reladev.quickdto.shared.QuickDto;
import org.reladev.quickdto.test_classes.BasicTypesImpl;

@QuickDto(sources = BasicTypesImpl.class, features = DirtyFeature.class)
public class BasicTypesDtoDef {
    byte myByte;
    @GwtIncompatible
    Byte objByte;
    short myShort;
    Short objShort;
    char myChar;
    Character objCharacter;
    int myInt;
    Integer objInteger;
    long myLong;
    Long objLong;
    float myFloat;
    Float objFloat;
    double myDouble;
    Double objDouble;
    boolean myBoolean;
    Boolean objBoolean;
    String myString;
}
