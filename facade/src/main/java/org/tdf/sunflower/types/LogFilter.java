package org.tdf.sunflower.types;

import org.tdf.common.util.HashUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.facade.RepositoryReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LogFilter {

    private List<byte[][]> topics = new ArrayList<>();  //  [[addr1, addr2], null, [A, B], [C]]
    private byte[][] contractAddresses = new byte[0][];
    private Bloom[][] filterBlooms;

    public LogFilter withContractAddress(byte[]... orAddress) {
        contractAddresses = orAddress;
        return this;
    }

    public LogFilter withTopic(byte[]... orTopic) {
        topics.add(orTopic);
        return this;
    }

    private void initBlooms() {
        if (filterBlooms != null) return;

        List<byte[][]> addrAndTopics = new ArrayList<>(topics);
        addrAndTopics.add(contractAddresses);

        filterBlooms = new Bloom[addrAndTopics.size()][];
        for (int i = 0; i < addrAndTopics.size(); i++) {
            byte[][] orTopics = addrAndTopics.get(i);
            if (orTopics == null || orTopics.length == 0) {
                filterBlooms[i] = new Bloom[]{new Bloom()}; // always matches
            } else {
                filterBlooms[i] = new Bloom[orTopics.length];
                for (int j = 0; j < orTopics.length; j++) {
                    filterBlooms[i][j] = Bloom.create(HashUtil.sha3(orTopics[j]));
                }
            }
        }
    }

    public boolean matchBloom(Bloom blockBloom) {
        initBlooms();
        for (Bloom[] andBloom : filterBlooms) {
            boolean orMatches = false;
            for (Bloom orBloom : andBloom) {
                if (blockBloom.matches(orBloom)) {
                    orMatches = true;
                    break;
                }
            }
            if (!orMatches) return false;
        }
        return true;
    }

    public boolean matchesContractAddress(byte[] toAddr) {
        initBlooms();
        for (byte[] address : contractAddresses) {
            if (Arrays.equals(address, toAddr)) return true;
        }
        return contractAddresses.length == 0;
    }

    public boolean matchesExactly(LogInfo logInfo) {
        initBlooms();
        if (!matchesContractAddress(logInfo.getAddress().getBytes())) return false;
        List<HexBytes> logTopics = logInfo.getTopics();
        for (int i = 0; i < this.topics.size(); i++) {
            if (i >= logTopics.size()) return false;
            byte[][] orTopics = topics.get(i);
            if (orTopics != null && orTopics.length > 0) {
                boolean orMatches = false;
                HexBytes logTopic = logTopics.get(i);
                for (byte[] orTopic : orTopics) {
                    if (HexBytes.fromBytes(orTopic).equals(logTopic)) {
                        orMatches = true;
                        break;
                    }
                }
                if (!orMatches) return false;
            }
        }
        return true;
    }

    public interface OnLogMatch {
        void onLogMatch(LogInfo info, Block b, int txIdx, Transaction tx, int logIdx);
    }

    public void onReceipt(Transaction tx, TransactionReceipt r, Block b, int txIdx, OnLogMatch cb) {
//        if (!matchBloom(r.getBloom())) return;

        for (int i = 0; i < r.getLogInfoList().size(); i++) {
            LogInfo info = r.getLogInfoList().get(i);
            System.out.println(info);
            if (!matchBloom(info.getBloom())) continue;
            cb.onLogMatch(info, b, txIdx, tx, i);
        }
    }

    public void onTx(RepositoryReader rd, Block b, Transaction tx, int i, OnLogMatch cb) {
        TransactionReceipt r = rd.getTransactionInfo(tx.getHash()).getFirst().getReceipt();
        onReceipt(tx, r, b, i, cb);
    }

    public void onBlock(RepositoryReader rd, Block b, OnLogMatch cb) {
//        if (!matchBloom(new Bloom(b.getLogsBloom().getBytes())))
//            return;
        System.out.println(b);
        for (int i = 0; i < b.getBody().size(); i++) {
            System.out.println(i);
            onTx(rd, b, b.getBody().get(i), i, cb);
        }
    }
}
