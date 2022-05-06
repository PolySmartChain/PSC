import {Context, Hex, JSONBuilder, Decimal, log, Result, JSONReader} from "../assembly";

class Counter {
    count: i32
}

let counter: Counter;

// every contract should had a function named by init
// which will be called at most once when contract deployed
export function init(): void {
    counter = new Counter();
    log("contract deployed successfully")
}

export function invoke(): void {
    log("hello world");
}


export function incrementAndGet(): i32 {
    counter.count = counter.count + 1;
    log("call contract successful counter = " + counter.count.toString());
    return counter.count;
}

export function getString(): void {
    Result.write(Uint8Array.wrap(String.UTF8.encode("abcdfasfasdf")));
}

export function testJSON(): void {
    JSONBuilder.putJSON("key", "{\"name\":\"kitty\"}");
    log(JSONBuilder.build());
    JSONBuilder.putString("key", "value");
    log(JSONBuilder.build());
    JSONBuilder.putU64("abc", u64.MAX_VALUE);
    log(JSONBuilder.build());
    JSONBuilder.putI64("abc", i64.MAX_VALUE);
    log(JSONBuilder.build());
    JSONBuilder.putI64("abc", i64.MIN_VALUE);
    log(JSONBuilder.build());
    JSONBuilder.putBool("abc", true);
    log(JSONBuilder.build());
    JSONBuilder.putF64("abc", f64.MAX_SAFE_INTEGER);
    log(JSONBuilder.build());
    JSONBuilder.putF64("abc", 0.3);
    log(JSONBuilder.build());
    JSONBuilder.setBool(0, true);
    JSONBuilder.setString(1, "abc");
    JSONBuilder.setI64(2, i64.MAX_VALUE);
    JSONBuilder.setI64(3, i64.MIN_VALUE);
    JSONBuilder.setU64(4, 0);
    JSONBuilder.setU64(5, u64.MAX_VALUE);
    JSONBuilder.putJSON("key--ddd", JSONBuilder.build());
    log(JSONBuilder.build());

    const json = `{"key--ddd":[true,"abc",9223372036854775807,-9223372036854775808,18446744073709551615,0,-1, 0.02]}`;
    log(JSONReader.getJSONByKey(json, "key--ddd"));
    log(JSONReader.getBoolByIndex(
        JSONReader.getJSONByKey(json, "key--ddd"),
        0
    ).toString());
    log(JSONReader.getStringByIndex(
        JSONReader.getJSONByKey(json, "key--ddd"),
        1
    ).toString());
    log(JSONReader.getI64ByIndex(
        JSONReader.getJSONByKey(json, "key--ddd"),
        2
    ).toString());
    log(JSONReader.getI64ByIndex(
        JSONReader.getJSONByKey(json, "key--ddd"),
        3
    ).toString());
    log(JSONReader.getU64ByIndex(
        JSONReader.getJSONByKey(json, "key--ddd"),
        4
    ).toString());
    log(JSONReader.getF64ByIndex(
        JSONReader.getJSONByKey(json, "key--ddd"),
        7
    ).toString());
}

export function printContext(): void {
    let ctx: Context = Context.load();
    log(Hex.encode(ctx.transactionHash));
    log(Hex.encode(ctx.sender));
    log(Hex.encode(ctx.recipient));
    log(Hex.encode(ctx.parentBlockHash));
    log(ctx.amount.toString());
    log(ctx.gasPrice.toString());
    log(ctx.gasLimit.toString());
    log(ctx.blockTimestamp.toString());
    log(ctx.transactionTimestamp.toString());
    log(ctx.blockHeight.toString());
    log(ctx.method);
}

export function testJsonBuilderPutJson(): void {
    JSONBuilder.putJSON("key", "{\"name\":\"kitty\"}");
    log(JSONBuilder.build())
}

export function testJsonBuilderPutString(): void {
    JSONBuilder.putString("key", "value");
    log(JSONBuilder.build())
}

export function addtest(): void {
    log(Decimal.add('1.1', '2.2'));
    log(Decimal.sub('10', '4'));
    log(Decimal.mul('10.1', '21'));
    log(Decimal.strictDiv('10', '2'));
    log(Decimal.div('10', '3', 3));
    log(Decimal.compare('100', '101').toString());
}

export function testException(): void {
    Decimal.strictDiv('1', '3');
}