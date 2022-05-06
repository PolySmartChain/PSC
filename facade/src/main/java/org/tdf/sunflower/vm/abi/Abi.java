package org.tdf.sunflower.vm.abi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.tdf.common.util.HashUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;
import static java.lang.String.format;
import static org.tdf.common.util.ByteUtil.merge;
import static org.tdf.common.util.ByteUtil.subarray;


public class Abi extends ArrayList<Abi.Entry> {

    private final static ObjectMapper DEFAULT_MAPPER = new ObjectMapper();

    public static Abi fromJson(String json) {
        try {
            return DEFAULT_MAPPER.readValue(json, Abi.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String toJson() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Abi.Entry> T find(
        Class<T> clazz,
        final Abi.Entry.Type type,
        final Predicate<T> searchPredicate
    ) {
        return (T) this.stream()
            .filter(entry -> entry.type == type && searchPredicate.test((T) entry))
            .findFirst().orElse(null);
    }

    public Function findFunction(Predicate<Function> searchPredicate) {
        return find(Function.class, Abi.Entry.Type.function, searchPredicate);
    }

    public Event findEvent(Predicate<Event> searchPredicate) {
        return find(Event.class, Abi.Entry.Type.event, searchPredicate);
    }

    public Abi.Constructor findConstructor() {
        return find(Constructor.class, Entry.Type.constructor, object -> true);
    }

    @Override
    public String toString() {
        return toJson();
    }


    @JsonInclude(Include.NON_NULL)
    @RequiredArgsConstructor
    public static abstract class Entry {

        public enum Type {
            constructor,
            function,
            event,
            fallback
        }

        public enum StateMutability {
            view,  // specified to not read blockchain state
            pure, // specified to not modify the blockchain state
            nonpayable, // function does not accept Ether
            payable, // function accepts Ether
        }

        @JsonInclude(Include.NON_NULL)
        public static class Param {
            public Boolean indexed;
            public String name;
            public SolidityType type;
            public String internalType;

            public static byte[] encodeList(List<Param> inputs, Object... args) {
                if (args.length != inputs.size())
                    throw new RuntimeException("invalid arguments: " + args.length + " != " + inputs.size());

                int staticSize = 0;
                int dynamicCnt = 0;
                // calculating static size and number of dynamic params
                for (int i = 0; i < args.length; i++) {
                    SolidityType type = inputs.get(i).type;
                    if (type.isDynamicType()) {
                        dynamicCnt++;
                    }
                    staticSize += type.getFixedSize();
                }

                byte[][] bb = new byte[args.length + dynamicCnt][];
                for (int curDynamicPtr = staticSize, curDynamicCnt = 0, i = 0; i < args.length; i++) {
                    SolidityType type = inputs.get(i).type;
                    if (type.isDynamicType()) {
                        byte[] dynBB = type.encode(args[i]);
                        bb[i] = SolidityType.IntType.encodeInt(curDynamicPtr);
                        bb[args.length + curDynamicCnt] = dynBB;
                        curDynamicCnt++;
                        curDynamicPtr += dynBB.length;
                    } else {
                        bb[i] = type.encode(args[i]);
                    }
                }

                return merge(bb);
            }

            // string -> String
            // bytes -> byte[]
            // address -> byte[]
            // uint/int -> BigInteger
            public static List<?> decodeList(List<Param> params, byte[] encoded) {
                List<Object> result = new ArrayList<>(params.size());

                int offset = 0;
                for (Param param : params) {
                    Object decoded = param.type.isDynamicType()
                        ? param.type.decode(encoded, SolidityType.IntType.decodeInt(encoded, offset).intValue())
                        : param.type.decode(encoded, offset);
                    result.add(decoded);

                    offset += param.type.getFixedSize();
                }

                return result;
            }

            @Override
            public String toString() {
                return format("%s%s%s", type.getCanonicalName(), (indexed != null && indexed) ? " indexed " : " ", name);
            }
        }

        public final Boolean anonymous;
        public final Boolean constant;
        public final String name;
        public final List<Param> inputs;
        public final List<Param> outputs;
        public final Type type;
        public final Boolean payable;
        public final StateMutability stateMutability;

        @JsonIgnore
        public boolean isPayable() {
            return stateMutability != null && stateMutability == StateMutability.payable;
        }

        public String formatSignature() {
            StringBuilder paramsTypes = new StringBuilder();
            for (Entry.Param param : inputs) {
                paramsTypes.append(param.type.getCanonicalName()).append(",");
            }

            return String.format("%s(%s)", name, StringUtils.stripEnd(paramsTypes.toString(), ","));
        }

        public byte[] fingerprintSignature() {
            return HashUtil.sha3(formatSignature().getBytes());
        }

        public byte[] encodeSignature() {
            return fingerprintSignature();
        }

        @JsonCreator
        public static Entry create(@JsonProperty("anonymous") boolean anonymous,
                                   @JsonProperty("constant") boolean constant,
                                   @JsonProperty("name") String name,
                                   @JsonProperty("inputs") List<Param> inputs,
                                   @JsonProperty("outputs") List<Param> outputs,
                                   @JsonProperty("type") Type type,
                                   @JsonProperty(value = "payable", required = false, defaultValue = "false") Boolean payable,
                                   @JsonProperty(value = "stateMutability") StateMutability stateMutability

        ) {
            Entry result = null;
            switch (type) {
                case constructor:
                    result = new Constructor(inputs, outputs, stateMutability);
                    break;
                case function:
                case fallback:
                    result = new Function(constant, name, inputs, outputs, payable, stateMutability);
                    break;
                case event:
                    result = new Event(anonymous, name, inputs, outputs, stateMutability);
                    break;
            }

            return result;
        }
    }

    public static class Constructor extends Entry {

        public Constructor(List<Param> inputs, List<Param> outputs, StateMutability stateMutability) {
            super(null, null, "", inputs, outputs, Type.constructor, false, stateMutability);
        }

        public List<?> decode(byte[] encoded) {
            return Param.decodeList(inputs, encoded);
        }

        public String formatSignature(String contractName) {
            return String.format("function %s(%s)", contractName, StringUtils.join(inputs, ", "));
        }
    }

    public static class Function extends Entry {

        private static final int ENCODED_SIGN_LENGTH = 4;

        public Function(boolean constant, String name, List<Param> inputs, List<Param> outputs, Boolean payable, StateMutability stateMutability) {
            super(null, constant, name, inputs, outputs, Type.function, payable, stateMutability);
        }

        public byte[] encode(Object... args) {
            return merge(encodeSignature(), encodeArguments(args));
        }

        private byte[] encodeArguments(Object... args) {
            return Param.encodeList(inputs, args);
        }

        public List<?> decode(byte[] encoded) {
            return Param.decodeList(inputs, subarray(encoded, ENCODED_SIGN_LENGTH, encoded.length));
        }

        public List<?> decodeResult(byte[] encoded) {
            return Param.decodeList(outputs, encoded);
        }

        @Override
        public byte[] encodeSignature() {
            return extractSignature(super.encodeSignature());
        }

        public static byte[] extractSignature(byte[] data) {
            return subarray(data, 0, ENCODED_SIGN_LENGTH);
        }

        @Override
        public String toString() {
            String returnTail = "";
            if (constant) {
                returnTail += " constant";
            }
            if (!outputs.isEmpty()) {
                List<String> types = new ArrayList<>();
                for (Param output : outputs) {
                    types.add(output.type.getCanonicalName());
                }
                returnTail += String.format(" returns(%s)", StringUtils.join(types, ", "));
            }

            return String.format("function %s(%s)%s;", name, StringUtils.join(inputs, ", "), returnTail);
        }
    }

    public static class Event extends Entry {

        public Event(boolean anonymous, String name, List<Param> inputs, List<Param> outputs, StateMutability stateMutability) {
            super(anonymous, null, name, inputs, outputs, Type.event, false, stateMutability);
        }

        public List<?> decode(byte[] data, byte[][] topics) {
            List<Object> result = new ArrayList<>(inputs.size());

            byte[][] argTopics = anonymous ? topics : subarray(topics, 1, topics.length);
            List<Param> indexedParams = filteredInputs(true);
            List<Object> indexed = new ArrayList<>();
            for (int i = 0; i < indexedParams.size(); i++) {
                Object decodedTopic;
                if (indexedParams.get(i).type.isDynamicType()) {
                    // If arrays (including string and bytes) are used as indexed arguments,
                    // the Keccak-256 hash of it is stored as topic instead.
                    decodedTopic = SolidityType.Bytes32Type.decodeBytes32(argTopics[i], 0);
                } else {
                    decodedTopic = indexedParams.get(i).type.decode(argTopics[i]);
                }
                indexed.add(decodedTopic);
            }
            List<?> notIndexed = Param.decodeList(filteredInputs(false), data);

            for (Param input : inputs) {
                result.add(input.indexed ? indexed.remove(0) : notIndexed.remove(0));
            }

            return result;
        }

        private List<Param> filteredInputs(final boolean indexed) {
            return inputs.stream()
                .filter(x -> x.indexed == indexed)
                .collect(Collectors.toCollection(ArrayList::new));
        }

        @Override
        public String toString() {
            return String.format("event %s(%s);", name, StringUtils.join(inputs, ", "));
        }
    }
}

