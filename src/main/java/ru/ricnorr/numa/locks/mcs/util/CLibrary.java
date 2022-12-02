package ru.ricnorr.numa.locks.mcs.util;

import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.IntByReference;

public interface CLibrary extends Library {
        CLibrary INSTANCE =
                Native.load("c", CLibrary.class);

        int getcpu(final IntByReference cpu,
                   final IntByReference node,
                   final PointerType tcache) throws LastErrorException;

    }