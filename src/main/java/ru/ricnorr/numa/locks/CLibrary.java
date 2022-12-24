package ru.ricnorr.numa.locks;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface CLibrary extends Library {
    CLibrary INSTANCE = (CLibrary) Native.load("c", CLibrary.class);

    int syscall(int number, Object... args);
}