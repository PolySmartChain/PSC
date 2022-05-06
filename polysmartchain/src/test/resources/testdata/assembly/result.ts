// @ts-ignore
@external("env", "_result")
declare function _result(offset: usize, len: usize): void;

export class Result {
    static write(data: Uint8Array): void {
        const v = data.buffer;
        _result(changetype<usize>(v), v.byteLength);
    }
}