package org.tdf.common.trie;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.spongycastle.util.encoders.Hex;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.Store;
import org.tdf.common.util.ByteArraySet;
import org.tdf.common.util.HashUtil;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.tdf.common.TrieUtil.newInstance;
import static org.tdf.common.util.HashUtil.EMPTY_TRIE_HASH;

@RunWith(JUnit4.class)
public abstract class AbstractTrieTest {
    private static String LONG_STRING = "1234567890abcdefghijklmnopqrstuvwxxzABCEFGHIJKLMNOPQRSTUVWXYZ";
    private static String ROOT_HASH_EMPTY = Hex.toHexString(EMPTY_TRIE_HASH);

    private static String c = "c";
    private static String ca = "ca";
    private static String cat = "cat";
    private static String dog = "dog";
    private static String doge = "doge";
    private static String test = "test";
    private static String dude = "dude";
    private final String randomDictionary = "spinneries, archipenko, prepotency, herniotomy, preexpress, relaxative, insolvably, debonnaire, apophysate, virtuality, cavalryman, utilizable, diagenesis, vitascopic, governessy, abranchial, cyanogenic, gratulated, signalment, predicable, subquality, crystalize, prosaicism, oenologist, repressive, impanelled, cockneyism, bordelaise, compigne, konstantin, predicated, unsublimed, hydrophane, phycomyces, capitalise, slippingly, untithable, unburnable, deoxidizer, misteacher, precorrect, disclaimer, solidified, neuraxitis, caravaning, betelgeuse, underprice, uninclosed, acrogynous, reirrigate, dazzlingly, chaffiness, corybantes, intumesced, intentness, superexert, abstrusely, astounding, pilgrimage, posttarsal, prayerless, nomologist, semibelted, frithstool, unstinging, ecalcarate, amputating, megascopic, graphalloy, platteland, adjacently, mingrelian, valentinus, appendical, unaccurate, coriaceous, waterworks, sympathize, doorkeeper, overguilty, flaggingly, admonitory, aeriferous, normocytic, parnellism, catafalque, odontiasis, apprentice, adulterous, mechanisma, wilderness, undivorced, reinterred, effleurage, pretrochal, phytogenic, swirlingly, herbarized, unresolved, classifier, diosmosing, microphage, consecrate, astarboard, predefying, predriving, lettergram, ungranular, overdozing, conferring, unfavorite, peacockish, coinciding, erythraeum, freeholder, zygophoric, imbitterer, centroidal, appendixes, grayfishes, enological, indiscreet, broadcloth, divulgated, anglophobe, stoopingly, bibliophil, laryngitis, separatist, estivating, bellarmine, greasiness, typhlology, xanthation, mortifying, endeavorer, aviatrices, unequalise, metastatic, leftwinger, apologizer, quatrefoil, nonfouling, bitartrate, outchiding, undeported, poussetted, haemolysis, asantehene, montgomery, unjoinable, cedarhurst, unfastener, nonvacuums, beauregard, animalized, polyphides, cannizzaro, gelatinoid, apologised, unscripted, tracheidal, subdiscoid, gravelling, variegated, interabang, inoperable, immortelle, laestrygon, duplicatus, proscience, deoxidised, manfulness, channelize, nondefense, ectomorphy, unimpelled, headwaiter, hexaemeric, derivation, prelexical, limitarian, nonionized, prorefugee, invariably, patronizer, paraplegia, redivision, occupative, unfaceable, hypomnesia, psalterium, doctorfish, gentlefolk, overrefine, heptastich, desirously, clarabelle, uneuphonic, autotelism, firewarden, timberjack, fumigation, drainpipes, spathulate, novelvelle, bicorporal, grisliness, unhesitant, supergiant, unpatented, womanpower, toastiness, multichord, paramnesia, undertrick, contrarily, neurogenic, gunmanship, settlement, brookville, gradualism, unossified, villanovan, ecospecies, organising, buckhannon, prefulfill, johnsonese, unforegone, unwrathful, dunderhead, erceldoune, unwadeable, refunction, understuff, swaggering, freckliest, telemachus, groundsill, outslidden, bolsheviks, recognizer, hemangioma, tarantella, muhammedan, talebearer, relocation, preemption, chachalaca, septuagint, ubiquitous, plexiglass, humoresque, biliverdin, tetraploid, capitoline, summerwood, undilating, undetested, meningitic, petrolatum, phytotoxic, adiphenine, flashlight, protectory, inwreathed, rawishness, tendrillar, hastefully, bananaquit, anarthrous, unbedimmed, herborized, decenniums, deprecated, karyotypic, squalidity, pomiferous, petroglyph, actinomere, peninsular, trigonally, androgenic, resistance, unassuming, frithstool, documental, eunuchised, interphone, thymbraeus, confirmand, expurgated, vegetation, myographic, plasmagene, spindrying, unlackeyed, foreknower, mythically, albescence, rebudgeted, implicitly, unmonastic, torricelli, mortarless, labialized, phenacaine, radiometry, sluggishly, understood, wiretapper, jacobitely, unbetrayed, stadholder, directress, emissaries, corelation, sensualize, uncurbable, permillage, tentacular, thriftless, demoralize, preimagine, iconoclast, acrobatism, firewarden, transpired, bluethroat, wanderjahr, groundable, pedestrian, unulcerous, preearthly, freelanced, sculleries, avengingly, visigothic, preharmony, bressummer, acceptable, unfoolable, predivider, overseeing, arcosolium, piriformis, needlecord, homebodies, sulphation, phantasmic, unsensible, unpackaged, isopiestic, cytophagic, butterlike, frizzliest, winklehawk, necrophile, mesothorax, cuchulainn, unrentable, untangible, unshifting, unfeasible, poetastric, extermined, gaillardia, nonpendent, harborside, pigsticker, infanthood, underrower, easterling, jockeyship, housebreak, horologium, undepicted, dysacousma, incurrable, editorship, unrelented, peritricha, interchaff, frothiness, underplant, proafrican, squareness, enigmatise, reconciled, nonnumeral, nonevident, hamantasch, victualing, watercolor, schrdinger, understand, butlerlike, hemiglobin, yankeeland";

    public static byte[] intToBytes(int val) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(val).array();
    }

    abstract Trie<String, String> newStringTrie();

    abstract Trie<byte[], byte[]> newBytesTrie();

    @Test
    public void test() {
    }

    @Test
    public void test1() {
        Trie<byte[], byte[]> trie = newBytesTrie();
        Arrays.asList("test", "toaster", "toasting", "slow", "slowly")
            .forEach(x -> trie.set(x.getBytes(), x.getBytes()));

        Set<byte[]> keys = trie.entries()
            .stream()
            .map(Map.Entry::getKey)
            .collect(Collectors.toCollection(ByteArraySet::new));
        Set<byte[]> values = trie.entries()
            .stream()
            .map(Map.Entry::getValue)
            .collect(Collectors.toCollection(ByteArraySet::new));
        for (String s : Arrays.asList("test", "toaster", "toasting", "slow", "slowly")
        ) {
            assert keys.contains(s.getBytes());
            assert Arrays.equals(trie.get(s.getBytes()), s.getBytes());
            assert values.contains(s.getBytes());
        }

        assert trie.getSize() == 5;
    }

    @Test
    public void testDeleteShortString1() {
        String ROOT_HASH_BEFORE = "a9539c810cc2e8fa20785bdd78ec36cc1dab4b41f0d531e80a5e5fd25c3037ee";
        String ROOT_HASH_AFTER = "fc5120b4a711bca1f5bb54769525b11b3fb9a8d6ac0b8bf08cbb248770521758";


        Trie<String, String> trie = newStringTrie();

        trie.set(cat, dog);
        assertEquals(dog, trie.get(cat));

        trie.set(ca, dude);
        assertEquals(dude, trie.get(ca));
        assertEquals(ROOT_HASH_BEFORE, trie.commit().getHex());

        trie.remove(ca);
        assertNull(trie.get(ca));
        assertEquals(ROOT_HASH_AFTER, trie.commit().getHex());
    }

    @Test
    public void testDeleteShortString2() {
        String ROOT_HASH_BEFORE = "a9539c810cc2e8fa20785bdd78ec36cc1dab4b41f0d531e80a5e5fd25c3037ee";
        String ROOT_HASH_AFTER = "b25e1b5be78dbadf6c4e817c6d170bbb47e9916f8f6cc4607c5f3819ce98497b";
        Trie<String, String> trie = newStringTrie();

        trie.set(ca, dude);
        assertEquals(dude, trie.get(ca));

        trie.set(cat, dog);
        assertEquals(dog, trie.get(cat));
        assertEquals(ROOT_HASH_BEFORE, trie.commit().getHex());

        trie.remove(cat);
        assertNull(trie.get(cat));
        assertEquals(ROOT_HASH_AFTER, trie.commit().getHex());
    }

    @Test
    public void testDeleteShortString3() {
        String ROOT_HASH_BEFORE = "778ab82a7e8236ea2ff7bb9cfa46688e7241c1fd445bf2941416881a6ee192eb";
        String ROOT_HASH_AFTER = "05875807b8f3e735188d2479add82f96dee4db5aff00dc63f07a7e27d0deab65";
        Trie<String, String> trie = newStringTrie();

        trie.set(cat, dude);
        assertEquals(dude, trie.get(cat));

        trie.set(dog, test);
        assertEquals(test, trie.get(dog));
        assertEquals(ROOT_HASH_BEFORE, trie.commit().getHex());

        trie.remove(dog);
        assertNull("", trie.get(dog));
        assertEquals(ROOT_HASH_AFTER, trie.commit().getHex());
    }

    @Test
    public void testDeleteLongString1() {
        String ROOT_HASH_BEFORE = "318961a1c8f3724286e8e80d312352f01450bc4892c165cc7614e1c2e5a0012a";
        String ROOT_HASH_AFTER = "63356ecf33b083e244122fca7a9b128cc7620d438d5d62e4f8b5168f1fb0527b";
        Trie<String, String> trie = newStringTrie();

        trie.set(cat, LONG_STRING);
        assertEquals(LONG_STRING, trie.get(cat));

        trie.set(dog, LONG_STRING);
        assertEquals(LONG_STRING, trie.get(dog));
        assertEquals(ROOT_HASH_BEFORE, trie.commit().getHex());

        trie.remove(dog);
        assertNull(trie.get(dog));
        assertEquals(ROOT_HASH_AFTER, trie.commit().getHex());
    }

    @Test
    public void testDeleteLongString2() {
        String ROOT_HASH_BEFORE = "e020de34ca26f8d373ff2c0a8ac3a4cb9032bfa7a194c68330b7ac3584a1d388";
        String ROOT_HASH_AFTER = "334511f0c4897677b782d13a6fa1e58e18de6b24879d57ced430bad5ac831cb2";
        Trie<String, String> trie = newStringTrie();

        trie.set(ca, LONG_STRING);
        assertEquals(LONG_STRING, trie.get(ca));

        trie.set(cat, LONG_STRING);
        assertEquals(LONG_STRING, trie.get(cat));
        assertEquals(ROOT_HASH_BEFORE, trie.commit().getHex());

        trie.remove(cat);
        assertNull(trie.get(cat));
        assertEquals(ROOT_HASH_AFTER, trie.commit().getHex());
    }

    @Test
    public void testDeleteLongString3() {
        String ROOT_HASH_BEFORE = "e020de34ca26f8d373ff2c0a8ac3a4cb9032bfa7a194c68330b7ac3584a1d388";
        String ROOT_HASH_AFTER = "63356ecf33b083e244122fca7a9b128cc7620d438d5d62e4f8b5168f1fb0527b";
        Trie<String, String> trie = newStringTrie();

        trie.set(cat, LONG_STRING);
        assertEquals(LONG_STRING, trie.get(cat));

        trie.set(ca, LONG_STRING);
        assertEquals(LONG_STRING, trie.get(ca));
        assertEquals(ROOT_HASH_BEFORE, trie.commit().getHex());

        trie.remove(ca);
        assertNull(trie.get(ca));
        assertEquals(ROOT_HASH_AFTER, trie.commit().getHex());
    }

    @Test
    public void testDeleteCompletellyDiferentItems() {
        TrieImpl<byte[], byte[]> trie = newInstance(new NoDoubleDeleteStore(), Codec.identity(), Codec.identity());

        String val_1 = "1000000000000000000000000000000000000000000000000000000000000000";
        String val_2 = "2000000000000000000000000000000000000000000000000000000000000000";
        String val_3 = "3000000000000000000000000000000000000000000000000000000000000000";

        trie.set(Hex.decode(val_1), Hex.decode(val_1));
        trie.set(Hex.decode(val_2), Hex.decode(val_2));

        String root1 = trie.commit().getHex();

        trie.set(Hex.decode(val_3), Hex.decode(val_3));
        trie.remove(Hex.decode(val_3));
        String root1_ = trie.commit().getHex();

        Assert.assertEquals(root1, root1_);
    }

    @Test
    public void testDeleteMultipleItems1() {
        String ROOT_HASH_BEFORE = "3a784eddf1936515f0313b073f99e3bd65c38689021d24855f62a9601ea41717";
        String ROOT_HASH_AFTER1 = "60a2e75cfa153c4af2783bd6cb48fd6bed84c6381bc2c8f02792c046b46c0653";
        String ROOT_HASH_AFTER2 = "a84739b4762ddf15e3acc4e6957e5ab2bbfaaef00fe9d436a7369c6f058ec90d";
        Trie<String, String> trie = newStringTrie();

        trie.set(cat, dog);
        assertEquals(dog, trie.get(cat));

        trie.set(ca, dude);
        assertEquals(dude, trie.get(ca));

        trie.set(doge, LONG_STRING);
        assertEquals(LONG_STRING, trie.get(doge));

        trie.set(dog, test);
        assertEquals(test, trie.get(dog));

        trie.set(test, LONG_STRING);
        assertEquals(LONG_STRING, trie.get(test));
        assertEquals(ROOT_HASH_BEFORE, trie.commit().getHex());

        trie.remove(dog);
        assertNull(trie.get(dog));
        assertEquals(ROOT_HASH_AFTER1, trie.commit().getHex());

        trie.remove(test);
        assertNull(trie.get(test));
        assertEquals(ROOT_HASH_AFTER2, trie.commit().getHex());
    }

    @Test
    public void testDeleteMultipleItems2() {
        String ROOT_HASH_BEFORE = "cf1ed2b6c4b6558f70ef0ecf76bfbee96af785cb5d5e7bfc37f9804ad8d0fb56";
        String ROOT_HASH_AFTER1 = "f586af4a476ba853fca8cea1fbde27cd17d537d18f64269fe09b02aa7fe55a9e";
        String ROOT_HASH_AFTER2 = "c59fdc16a80b11cc2f7a8b107bb0c954c0d8059e49c760ec3660eea64053ac91";
        Trie<String, String> trie = newStringTrie();

        trie.set(c, LONG_STRING);
        assertEquals(LONG_STRING, trie.get(c));

        trie.set(ca, LONG_STRING);
        assertEquals(LONG_STRING, trie.get(ca));

        trie.set(cat, LONG_STRING);
        assertEquals(LONG_STRING, trie.get(cat));
        assertEquals(ROOT_HASH_BEFORE, trie.commit().getHex());

        trie.remove(ca);
        assertNull(trie.get(ca));
        assertEquals(ROOT_HASH_AFTER1, trie.commit().getHex());

        trie.remove(cat);
        assertNull(trie.get(cat));
        assertEquals(ROOT_HASH_AFTER2, trie.commit().getHex());
    }

    @Test
    public void testMassiveDelete() {
        TrieImpl<byte[], byte[]> trie = newInstance(new NoDoubleDeleteStore(), Codec.identity(), Codec.identity());
        byte[] rootHash1 = null;
        for (int i = 0; i < 11000; i++) {
            trie.set(HashUtil.sha3(intToBytes(i)), HashUtil.sha3(intToBytes(i + 1000000)));
            if (i == 10000) {
                rootHash1 = trie.commit().getBytes();
            }
        }
        for (int i = 10001; i < 11000; i++) {
            trie.remove(HashUtil.sha3(intToBytes(i)));
        }

        byte[] rootHash2 = trie.commit().getBytes();
        assertArrayEquals(rootHash1, rootHash2);
    }

    @Test
    public void testDeleteAll() {
        String ROOT_HASH_BEFORE = "a84739b4762ddf15e3acc4e6957e5ab2bbfaaef00fe9d436a7369c6f058ec90d";
        Trie<String, String> trie = newStringTrie();
        assertEquals(ROOT_HASH_EMPTY, trie.commit().getHex());

        trie.set(ca, dude);
        trie.set(cat, dog);
        trie.set(doge, LONG_STRING);
        assertEquals(ROOT_HASH_BEFORE, trie.commit().getHex());

        trie.remove(ca);
        trie.remove(cat);
        trie.remove(doge);
        assertEquals(ROOT_HASH_EMPTY, trie.commit().getHex());
    }

    @Test
    public void testTrieEquals() {
        Trie<String, String> trie1 = newStringTrie();
        Trie<String, String> trie2 = newStringTrie();

        trie1.set(doge, LONG_STRING);
        trie2.set(doge, LONG_STRING);
        assertEquals(trie1.commit().getHex(), trie2.commit().getHex());

        trie1.set(dog, LONG_STRING);
        trie2.set(cat, LONG_STRING);
        assertNotEquals(trie1.commit().getHex(), trie2.commit().getHex());
    }

    @Test
    public void testSingleItem() {
        Trie<String, String> trie = newStringTrie();
        trie.set("A", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        assertEquals("d23786fb4a010da3ce639d66d5e904a11dbc02746d1ce25029e53290cabf28ab", trie.commit().getHex());
    }

    @Test
    public void testDogs() {
        Trie<String, String> trie = newStringTrie();
        Trie<String, String> impl = trie;
        trie.set("doe", "reindeer");
        assertEquals("11a0327cfcc5b7689b6b6d727e1f5f8846c1137caaa9fc871ba31b7cce1b703e", impl.commit().getHex());

        trie.set("dog", "puppy");
        assertEquals("05ae693aac2107336a79309e0c60b24a7aac6aa3edecaef593921500d33c63c4", impl.commit().getHex());

        Trie<String, String> trie2 = impl;
        assert trie2.get("dog").equals("puppy");
        trie.set("dogglesworth", "cat");
        impl.commit();
    }

    @Test
    public void testPuppy() {
        Trie<String, String> trie = newStringTrie();
        trie.set("do", "verb");
        trie.set("doge", "coin");
        trie.set("horse", "stallion");
        trie.set("dog", "puppy");

        assertEquals("5991bb8c6514148a29db676a14ac506cd2cd5775ace63c30a4fe457715e9ac84", trie.commit().getHex());
    }


    @Test
    public void testEmptyValues() {
        Trie<String, String> trie = newStringTrie();
        trie.set("do", "verb");
        trie.set("ether", "wookiedoo");
        trie.set("horse", "stallion");
        trie.set("shaman", "horse");
        trie.set("doge", "coin");
        trie.remove("ether");
        trie.set("dog", "puppy");
        trie.remove("shaman");

        assertEquals("5991bb8c6514148a29db676a14ac506cd2cd5775ace63c30a4fe457715e9ac84", trie.commit().getHex());
    }


    @Test
    public void testFoo() {
        Trie<String, String> trie = newStringTrie();
        trie.set("foo", "bar");
        trie.set("food", "bat");
        trie.set("food", "bass");

        assertEquals("17beaa1648bafa633cda809c90c04af50fc8aed3cb40d16efbddee6fdf63c4c3", trie.commit().getHex());
    }

    @Test
    public void testSmallValues() {
        Trie<String, String> trie = newStringTrie();

        trie.set("be", "e");
        trie.set("dog", "puppy");
        trie.set("bed", "d");
        assertEquals("3f67c7a47520f79faa29255d2d3c084a7a6df0453116ed7232ff10277a8be68b", trie.commit().getHex());
    }

    @Test
    public void testTesty() {
        Trie<String, String> trie = newStringTrie();

        trie.set("test", "test");
        assertEquals("85d106d4edff3b7a4889e91251d0a87d7c17a1dda648ebdba8c6060825be23b8", trie.commit().getHex());

        trie.set("te", "testy");
        assertEquals("8452568af70d8d140f58d941338542f645fcca50094b20f3c3d8c3df49337928", trie.commit().getHex());
    }

    @Test
    public void testMasiveUpdate() {
        boolean massiveUpdateTestEnabled = false;

        if (massiveUpdateTestEnabled) {
            List<String> randomWords = Arrays.asList(randomDictionary.split(","));
            HashMap<String, String> testerMap = new HashMap<>();

            Trie<String, String> trie = newStringTrie();
            Random generator = new Random();

            // Random insertion
            for (int i = 0; i < 100000; ++i) {

                int randomIndex1 = generator.nextInt(randomWords.size());
                int randomIndex2 = generator.nextInt(randomWords.size());

                String word1 = randomWords.get(randomIndex1).trim();
                String word2 = randomWords.get(randomIndex2).trim();

                trie.set(word1, word2);
                testerMap.put(word1, word2);
            }

            int half = testerMap.size() / 2;
            for (int r = 0; r < half; ++r) {

                int randomIndex = generator.nextInt(randomWords.size());
                String word1 = randomWords.get(randomIndex).trim();

                testerMap.remove(word1);
                trie.remove(word1);
            }

            // Assert the result now
            Iterator<String> keys = testerMap.keySet().iterator();
            while (keys.hasNext()) {

                String mapWord1 = keys.next();
                String mapWord2 = testerMap.get(mapWord1);
                String treeWord2 = trie.get(mapWord1);

                Assert.assertEquals(mapWord2, treeWord2);
            }
        }
    }

    @Test
    public void testMassiveDeterministicUpdate() throws IOException, URISyntaxException {

        // should be root: cfd77c0fcb037adefce1f4e2eb94381456a4746379d2896bb8f309c620436d30

        Store<byte[], byte[]> db = new NoDoubleDeleteStore();
        Trie<String, String> trieSingle = newStringTrie();

        URL massiveUpload_1 = ClassLoader
            .getSystemResource("trie/massive-upload.dmp");

        File file = new File(massiveUpload_1.toURI());
        List<String> strData = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);

        // *** Part - 1 ***
        // 1. load the data from massive-upload.dmp
        //    which includes deletes/upadtes (5000 operations)
        for (String aStrData : strData) {

            String[] keyVal = aStrData.split("=");

            if (keyVal[0].equals("*"))
                trieSingle.remove(keyVal[1].trim());
            else
                trieSingle.set(keyVal[0].trim(), keyVal[1].trim());
        }


        assert "cfd77c0fcb037adefce1f4e2eb94381456a4746379d2896bb8f309c620436d30".equals(trieSingle.commit().getHex());

    }

    @Test
    public void testGetFromRootNode() {
        Trie<String, String> trie1 = newStringTrie();
        trie1.set(cat, LONG_STRING);
        Trie<String, String> trie2 = trie1;
        assertEquals(LONG_STRING, trie2.get(cat));
    }

    @Test
    public void storageHashCalc_1() {

        byte[] key1 = Hex.decode("0000000000000000000000000000000000000000000000000000000000000010");
        byte[] key2 = Hex.decode("0000000000000000000000000000000000000000000000000000000000000014");
        byte[] key3 = Hex.decode("0000000000000000000000000000000000000000000000000000000000000016");
        byte[] key4 = Hex.decode("0000000000000000000000000000000000000000000000000000000000000017");

        byte[] val1 = Hex.decode("947e70f9460402290a3e487dae01f610a1a8218fda");
        byte[] val2 = Hex.decode("40");
        byte[] val3 = Hex.decode("94412e0c4f0102f3f0ac63f0a125bce36ca75d4e0d");
        byte[] val4 = Hex.decode("01");

        Trie<byte[], byte[]> storage = newBytesTrie();
        storage.set(key1, val1);
        storage.set(key2, val2);
        storage.set(key3, val3);
        storage.set(key4, val4);

        String hash = storage.commit().getHex();

        System.out.println(hash);
        Assert.assertEquals("517eaccda568f3fa24915fed8add49d3b743b3764c0bc495b19a47c54dbc3d62", hash);
    }

    @Test
//    count n = 1000000 size trie 2349 ms
    public void test7() throws Exception {
        boolean performance = false;
        int n = 1000000;
        if (!performance) return;
        TrieImpl<byte[], byte[]> trie = newInstance(
//            HashUtil::sha256,
            new NoDoubleDeleteStore(),
            Codec.identity(),
            Codec.identity()
        );
        byte[] dummy = new byte[]{1};
        SecureRandom sr = new SecureRandom();
        long start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            byte[] bytes = new byte[16];
            sr.nextBytes(bytes);
            trie.set(bytes, dummy);
//            if(i % 100000 == 0) {
//                byte[] root = trie.commit();
//                trie.flush();
//                trie = trie.revert(root);
//            }
        }
        trie.commit();
        trie.flush();
        long end = System.currentTimeMillis();
        System.out.println("insert " + n + " " + (end - start) + " ms");
    }

    @Test // update the trie with blog key/val
    // each time dump the entire trie
    public void testSample_1() {

        Trie<String, String> trie = newStringTrie();


        trie.set("dog", "puppy");
        System.out.println();
        Assert.assertEquals("ed6e08740e4a267eca9d4740f71f573e9aabbcc739b16a2fa6c1baed5ec21278", trie.commit().getHex());

        trie.set("do", "verb");
        Assert.assertEquals("779db3986dd4f38416bfde49750ef7b13c6ecb3e2221620bcad9267e94604d36", trie.commit().getHex());

        trie.set("doggiestan", "aeswome_place");
        Assert.assertEquals("8bd5544747b4c44d1274aa99a6293065fe319b3230e800203317e4c75a770099", trie.commit().getHex());
    }

    // this case relates to a bug which led us to conflict on Morden network (block #486248)
    // first part of the new Value was converted to String by #asString() during key deletion
    // and some lines after String.getBytes() returned byte array which differed to array before converting
    @Test
    public void testBugFix() throws Exception {

        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("6e929251b981389774af84a07585724c432e2db487381810719c3dd913192ae2", "00000000000000000000000000000000000000000000000000000000000000be");
        dataMap.put("6e92718d00dae27b2a96f6853a0bf11ded08bc658b2e75904ca0344df5aff9ae", "00000000000000000000000000000000000000000000002f0000000000000000");

        Trie<byte[], byte[]> trie = newBytesTrie();

        for (Map.Entry<String, String> e : dataMap.entrySet()) {
            trie.set(Hex.decode(e.getKey()), Hex.decode(e.getValue()));
        }

        assertArrayEquals(Objects.requireNonNull(trie.get(Hex.decode("6e929251b981389774af84a07585724c432e2db487381810719c3dd913192ae2"))),
            Hex.decode("00000000000000000000000000000000000000000000000000000000000000be"));

        assertArrayEquals(Objects.requireNonNull(trie.get(Hex.decode("6e92718d00dae27b2a96f6853a0bf11ded08bc658b2e75904ca0344df5aff9ae"))),
            Hex.decode("00000000000000000000000000000000000000000000002f0000000000000000"));

        trie.remove(Hex.decode("6e9286c946c6dd1f5d97f35683732dc8a70dc511133a43d416892f527dfcd243"));

        assertArrayEquals(Objects.requireNonNull(trie.get(Hex.decode("6e929251b981389774af84a07585724c432e2db487381810719c3dd913192ae2"))),
            Hex.decode("00000000000000000000000000000000000000000000000000000000000000be"));

        assertArrayEquals(Objects.requireNonNull(trie.get(Hex.decode("6e92718d00dae27b2a96f6853a0bf11ded08bc658b2e75904ca0344df5aff9ae"))),
            Hex.decode("00000000000000000000000000000000000000000000002f0000000000000000"));
    }

    @Test
    public void testBugFix2() throws Exception {

        // Create trie: root -> BranchNode (..., NodeValue (less than 32 bytes), ...)
        Trie<byte[], byte[]> trie = newBytesTrie();
        trie.set(Hex.decode("0000000000000000000000000000000000000000000000000000000000000011"), Hex.decode("11"));
        trie.set(Hex.decode("0000000000000000000000000000000000000000000000000000000000000022"), Hex.decode("22"));

        // Reset trie to refresh the nodes

        // Update trie: root -> dirty BranchNode (..., NodeValue (less than 32 bytes), ..., dirty NodeValue, ...)
        trie.set(Hex.decode("0000000000000000000000000000000000000000000000000000000000000033"), Hex.decode("33"));

        // BUG:
        // In that case NodeValue (encoded as plain RLP list) isn't dirty
        // while both rlp and hash fields are null, Node has been initialized with parsedRLP only
        // Therefore any subsequent call to BranchNode.encode() fails with NPE

        // FIX:
        // Supply Node initialization with raw rlp value

        assertEquals("36e350d9a1d9c02d5bc4539a05e51890784ea5d2b675a0b26725dbbdadb4d6e2", trie.commit().getHex());
    }

    @Test
    public void testBugFix3() throws Exception {

        Store<byte[], byte[]> src = new NoDoubleDeleteStore();
        // Scenario:
        // create trie with subtrie: ... -> kvNodeNode -> BranchNode() -> kvNodeValue1, kvNodeValue2
        // remove kvNodeValue2, in that way kvNodeNode and kvNodeValue1 are going to be merged in a new kvNodeValue3

        // BUG: kvNodeNode is not deleted from storage after the merge

        Trie<byte[], byte[]> trie = newBytesTrie();
        trie.set(Hex.decode("0000000000000000000000000000000000000000000000000000000000011133"),
            Hex.decode("0000000000000000000000000000000000000000000000000000000000000033"));
        trie.set(Hex.decode("0000000000000000000000000000000000000000000000000000000000021244"),
            Hex.decode("0000000000000000000000000000000000000000000000000000000000000044"));
        trie.set(Hex.decode("0000000000000000000000000000000000000000000000000000000000011255"),
            Hex.decode("0000000000000000000000000000000000000000000000000000000000000055"));

        trie.remove(Hex.decode("0000000000000000000000000000000000000000000000000000000000011255"));

        byte[] v = src.get(Hex.decode("5152f9274abb8e61f3956ccd08d31e38bfa2913afd23bc13b5e7bb709ce7f603"));
        assertTrue(v == null || v.length == 0);
    }

}
