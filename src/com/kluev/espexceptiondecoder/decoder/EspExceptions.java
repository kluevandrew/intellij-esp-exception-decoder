package com.kluev.espexceptiondecoder.decoder;

abstract public class EspExceptions {
    public static String[] messages = {
            "Illegal instruction",
            "SYSCALL instruction",
            "InstructionFetchError: Processor internal physical address or data error during instruction fetch",
            "LoadStoreError: Processor internal physical address or data error during load or store",
            "Level1Interrupt: Level-1 interrupt as indicated by set level-1 bits in the INTERRUPT register",
            "Alloca: MOVSP instruction, if caller's registers are not in the register file",
            "IntegerDivideByZero: QUOS, QUOU, REMS, or REMU divisor operand is zero",
            "reserved",
            "Privileged: Attempt to execute a privileged operation when CRING ? 0",
            "LoadStoreAlignmentCause: Load or store to an unaligned address",
            "reserved",
            "reserved",
            "InstrPIFDataError: PIF data error during instruction fetch",
            "LoadStorePIFDataError: Synchronous PIF data error during LoadStore access",
            "InstrPIFAddrError: PIF address error during instruction fetch",
            "LoadStorePIFAddrError: Synchronous PIF address error during LoadStore access",
            "InstTLBMiss: Error during Instruction TLB refill",
            "InstTLBMultiHit: Multiple instruction TLB entries matched",
            "InstFetchPrivilege: An instruction fetch referenced a virtual address at a ring level less than CRING",
            "reserved",
            "InstFetchProhibited: An instruction fetch referenced a page mapped with an attribute that does not permit instruction fetch",
            "reserved",
            "reserved",
            "reserved",
            "LoadStoreTLBMiss: Error during TLB refill for a load or store",
            "LoadStoreTLBMultiHit: Multiple TLB entries matched for a load or store",
            "LoadStorePrivilege: A load or store referenced a virtual address at a ring level less than CRING",
            "reserved",
            "LoadProhibited: A load referenced a page mapped with an attribute that does not permit loads",
            "StoreProhibited: A store referenced a page mapped with an attribute that does not permit stores"
    };
}
