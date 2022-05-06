package org.tdf.sunflower.types;

import com.github.salpadding.rlpstream.Rlp;
import com.github.salpadding.rlpstream.RlpBuffer;
import com.github.salpadding.rlpstream.RlpList;
import com.github.salpadding.rlpstream.RlpWritable;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HashUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.tdf.common.util.ByteUtil.toHexString;

public class LogInfo1 implements RlpWritable {


    byte[] address = new byte[]{};
    List<Uint256> topics = new ArrayList<>();
    byte[] data = new byte[]{};

    public LogInfo1(byte[] rlp) {
        RlpList params = Rlp.decodeList(rlp);

        RlpList topics = params.listAt(1);

        this.address = params.bytesAt(0);
        this.data = params.bytesAt(2);

        for(int i = 0; i < topics.size(); i++){
            byte[] topic = topics.bytesAt(i);
            this.topics.add(Uint256.of(topic));
        }
    }

    public LogInfo1(byte[] address, List<Uint256> topics, byte[] data) {
        this.address = (address != null) ? address : new byte[]{};
        this.topics = (topics != null) ? topics : new ArrayList<>();
        this.data = (data != null) ? data : new byte[]{};
    }

    public byte[] getAddress() {
        return address;
    }

    public List<Uint256> getTopics() {
        return topics;
    }

    public byte[] getData() {
        return data;
    }

    /*  [address, [topic, topic ...] data] */
    public byte[] getEncoded() {

        byte[] addressEncoded = Rlp.encodeBytes(this.address);

        byte[][] topicsEncoded = null;
        if (topics != null) {
            topicsEncoded = new byte[topics.size()][];
            int i = 0;
            for (Uint256 topic : topics) {
                byte[] topicData = topic.getByte32();
                topicsEncoded[i] = Rlp.encodeBytes(topicData);
                ++i;
            }
        }

        byte[] dataEncoded = Rlp.encodeBytes(data);
        return Rlp.encodeElements(
                addressEncoded,
                Rlp.encodeElements(
                    topicsEncoded == null ?
                        Collections.emptyList() :
                        Arrays.asList(topicsEncoded)
                ),
                dataEncoded
        );
    }

    public Bloom getBloom() {
        Bloom ret = Bloom.create(HashUtil.sha3(address));
        for (Uint256 topic : topics) {
            byte[] topicData = topic.getByte32();
            ret.or(Bloom.create(HashUtil.sha3(topicData)));
        }
        return ret;
    }

    @Override
    public String toString() {

        StringBuilder topicsStr = new StringBuilder();
        topicsStr.append("[");

        for (Uint256 topic : topics) {
            String topicStr = toHexString(topic.getByte32());
            topicsStr.append(topicStr).append(" ");
        }
        topicsStr.append("]");


        return "LogInfo{" +
            "address=" + toHexString(address) +
            ", topics=" + topicsStr +
            ", data=" + toHexString(data) +
            '}';
    }

    @Override
    public int writeToBuf(RlpBuffer rlpBuffer) {
        return rlpBuffer.writeRaw(getEncoded());
    }
}

