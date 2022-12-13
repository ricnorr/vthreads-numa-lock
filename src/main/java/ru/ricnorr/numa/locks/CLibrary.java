package ru.ricnorr.numa.locks;

import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.IntByReference;

public interface CLibrary extends Library {
    CLibrary INSTANCE = (CLibrary) Native.load("c", CLibrary.class);

    int syscall(int number, Object... args);
}