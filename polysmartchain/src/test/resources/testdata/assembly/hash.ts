// @ts-ignore
@external("env", "_hash_keccak256")
declare function _hash_keccak256(ptr: usize, ptr_len: usize, dst: usize): void;

export class Hash {
    static keccak256(data: Uint8Array): Uint8Array {
        let res = new ArrayBuffer(32);
        _hash_keccak256(changetype<usize>(data.buffer), data.buffer.byteLength, changetype<usize>(res));
        return Uint8Array.wrap(res);
    }
}