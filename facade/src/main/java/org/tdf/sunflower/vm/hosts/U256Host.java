package org.tdf.sunflower.vm.hosts;

import org.tdf.evm.MutableBigInteger;
import org.tdf.evm.SlotUtils;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;


import java.util.Arrays;
import java.util.Collections;


import static org.tdf.evm.SlotUtils.*;
import static org.tdf.sunflower.vm.WBI.*;

public class U256Host extends HostFunction {
    private static final int[] ZERO_SLOT = new int[SLOT_SIZE * 2];

    private final int[] slot0 = new int[SLOT_SIZE];
    private final int[] slot1 = new int[SLOT_SIZE];
    private final int[] varSlot0 = new int[SLOT_SIZE * 2];
    private final int[] varSlot1 = new int[SLOT_SIZE * 2];

    private final int[] divisor = new int[SLOT_SIZE * 2];


    private final byte[] tempBytes = new byte[SLOT_BYTE_ARRAY_SIZE];

    private final MutableBigInteger mut0 = new MutableBigInteger(slot0);
    private final MutableBigInteger mut1 = new MutableBigInteger(slot1);

    private final MutableBigInteger mut2 = new MutableBigInteger(varSlot0);
    private final MutableBigInteger mut3 = new MutableBigInteger(varSlot1);


    enum U256OP {
        ADD,
        SUB,
        MUL,
        DIV,
        MOD
    }

    public static final FunctionType FUNCTION_TYPE = new FunctionType(
        // offset, length, offset
        Arrays.asList(
            ValueType.I64, ValueType.I64, ValueType.I64,
            ValueType.I64, ValueType.I64, ValueType.I64,
            ValueType.I64, ValueType.I64, ValueType.I64
        ),
        Collections.singletonList(ValueType.I64));

    public U256Host() {
        super("_u256", FUNCTION_TYPE);
    }

    // override slot by uint256 pointer
    private void copyToSlot(int[] slot, long[] args, int offset) {
        // call peek function, convert uint256 pointer to byte array fat pointer
        for(int i = 0; i < 4; i++) {
            slot[i * 2] = (int) (args[offset + i] >>> 32);
            slot[i * 2 + 1] = (int) args[offset + i];
        }
    }

    // write slot into memory
    private long writeSlot(int[] slot) {
        long[] data = new long[slot.length / 2];
        for(int i = 0; i < data.length; i++) {
            data[i] = ((slot[i * 2] & LONG_MASK) << 32) | ((slot[i * 2 + 1] & LONG_MASK));
        }
        return INSTANCE.mallocWords(getInstance(), data);
    }

    @Override
    public long execute(long[] longs) {
        System.out.println("u256..");
        int i = (int) longs[0];

        U256OP op = U256OP.values()[i];



        copyToSlot(slot0, longs, 1);
        copyToSlot(slot1, longs, 5);

        if (op == U256OP.MUL || op == U256OP.DIV || op == U256OP.MOD) {
            mut0.setValue(slot0, SLOT_SIZE);
            mut0.normalize();
            mut1.setValue(slot1, SLOT_SIZE);
            mut1.normalize();
            mut2.clear();
        }


        switch (op) {
            case ADD: {
                Arrays.fill(varSlot0, 0);
                long carry = SlotUtils.add(slot0, 0, slot1, 0, varSlot0, 8);
                varSlot0[7] = (int) carry;
                return writeSlot(varSlot0);
            }
            case SUB: {
                SlotUtils.sub(slot0, 0, slot1, 0, slot0, 0);
                return writeSlot(slot0);
            }
            case MUL: {
                if (mut0.isZero() || mut1.isZero()) {
                    return writeSlot(ZERO_SLOT);
                }
                mut0.multiply(mut1, mut2);
                Arrays.fill(varSlot1, 0);
                System.arraycopy(varSlot0, mut2.getOffset(), varSlot1, SLOT_SIZE * 2 - mut2.getIntLen(), mut2.getIntLen());
                return writeSlot(varSlot1);
            }
            case DIV: {
                // reset temporary divisor and rem
                Arrays.fill(divisor, 0);
                mut3.clear();

                mut0.divideKnuth(mut1, mut2, mut3, divisor, false);

                Arrays.fill(slot0, 0);
                SlotUtils.trim256(mut2.getValue(), mut2.getOffset(), mut2.getIntLen(), slot0, 0);
                return writeSlot(slot0);
            }
            case MOD: {
                // reset temporary divisor and rem
                Arrays.fill(divisor, 0);
                mut3.clear();

                mut0.divideKnuth(mut1, mut2, mut3, divisor, true);

                Arrays.fill(slot0, 0);
                SlotUtils.trim256(mut3.getValue(), mut3.getOffset(), mut3.getIntLen(), slot0, 0);
                return writeSlot(slot0);
            }
            default:
                throw new UnsupportedOperationException();
        }
    }
}
