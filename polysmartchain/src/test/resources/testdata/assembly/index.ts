// @ts-ignore
@external("env", "_log")
declare function _log(offset: usize, len: usize): void;

// @ts-ignore
@external("env", "_gas")
declare function _gas(): u64;

// @ts-ignore
export function log(a: string): void {
    const str = String.UTF8.encode(a);
    _log(changetype<usize>(str), str.byteLength)
}

// get currently gas used
export function gas(): u64 {
    return _gas();
}

export class Hex {
    static encode(buf: Uint8Array): string {
        let out = "";
        for (let i = 0; i < buf.length; i++) {
            const a = buf[i] as u32;
            const b = a & 0xf;
            const c = a >> 4;

            let x: u32 = ((87 + b + (((b - 10) >> 8) & ~38)) << 8) | (87 + c + (((c - 10) >> 8) & ~38));
            out += String.fromCharCode(x as u8);
            x >>= 8;
            out += String.fromCharCode(x as u8);
        }
        return out;
    }
}


export * from './context';
export * from './decimal';
export * from './json';
// export * from './payload';
export * from './result'



