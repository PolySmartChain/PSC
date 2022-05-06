package org.tdf.sunflower.consensus.pow;

import com.github.salpadding.rlpstream.Rlp;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.util.HashUtil;
import org.tdf.sunflower.types.Block;


@Slf4j(topic = "pow")
public class PoW {

    public PoW() {

    }

    public static byte[] getPoWHash(Block block) {
        byte[] d = HashUtil.sha3(Rlp.encode(block.getHeader()));
        return HashUtil.sha3(d);
    }

    public static int compare(byte[] x, byte[] y) {
        if (x.length != y.length)
            throw new RuntimeException("compare failed for x and y, length not equal");
        for (int i = 0; i < x.length; i++) {
            int a = Byte.toUnsignedInt(x[i]);
            int b = Byte.toUnsignedInt(y[i]);
            int ret = Integer.compare(a, b);
            if (ret != 0) return ret;
        }
        return 0;
    }

}
