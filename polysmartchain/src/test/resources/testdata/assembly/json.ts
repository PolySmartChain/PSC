// @ts-ignore
@external("env", "_json_builder_put_json")
declare function _json_builder_put_json(key_offset: usize, key_len: usize, json: usize, json_len: usize): void;

// @ts-ignore
@external("env", "_json_builder_put_str")
declare function _json_builder_put_str(key_offset: usize, key_len: usize, str_offset: usize, str_len: usize): void;

// @ts-ignore
@external("env", "_json_builder_put_i64")
declare function _json_builder_put_i64(key_offset: usize, key_len: usize, val: i64): void;

// @ts-ignore
@external("env", "_json_builder_put_u64")
declare function _json_builder_put_u64(key_offset: usize, key_len: usize, val: u64): void;

// @ts-ignore
@external("env", "_json_builder_put_bool")
// if (b) val = 1 else val = 0
declare function _json_builder_put_bool(key_offset: usize, key_len: usize, val: u64): void;

// @ts-ignore
@external("env", "_json_builder_put_f64")
declare function _json_builder_put_f64(key_offset: usize, key_len: usize, val: f64): void;

// @ts-ignore
@external("env", "_json_builder_set_json")
declare function _json_builder_set_json(idx: i32, json: usize, json_len: usize): void;

// @ts-ignore
@external("env", "_json_builder_set_str")
declare function _json_builder_set_str(idx: i32, str_offset: usize, str_len: usize): void;

// @ts-ignore
@external("env", "_json_builder_set_i64")
declare function _json_builder_set_i64(idx: i32, val: i64): void;

// @ts-ignore
@external("env", "_json_builder_set_u64")
declare function _json_builder_set_u64(idx: i32, val: u64): void;

// @ts-ignore
@external("env", "_json_builder_set_bool")
// if (b) val = 1 else val = 0
declare function _json_builder_set_bool(idx: i32, val: u64): void;

// @ts-ignore
@external("env", "_json_builder_set_f64")
declare function _json_builder_set_f64(idx: i32, val: f64): void;

// @ts-ignore
@external("env", "_json_builder_array_size")
declare function _json_builder_array_size(): i32;

// @ts-ignore
@external("env", "_json_builder_build")
declare function _json_builder_build(ptr: usize): void;

// @ts-ignore
@external("env", "_json_builder_build_len")
declare function _json_builder_build_len(): usize;

// @ts-ignore
@external("env", "_json_reader_get_json_by_key")
declare function _json_reader_get_json_by_key(ptr: usize, ptr_len: usize, key_offset: usize, key_len: usize, json: usize): void;

// @ts-ignore
@external("env", "_json_reader_get_json_len_by_key")
declare function _json_reader_get_json_len_by_key(ptr: usize, ptr_len: usize, key_offset: usize, key_len: usize): usize;

// @ts-ignore
@external("env", "_json_reader_get_str_by_key")
declare function _json_reader_get_str(ptr: usize, ptr_len: usize, key_offset: usize, key_len: usize, str: usize): void;

// @ts-ignore
@external("env", "_json_reader_get_str_len_by_key")
declare function _json_reader_get_str_len(ptr: usize, ptr_len: usize, key_offset: usize, key_len: usize): usize;

// @ts-ignore
@external("env", "_json_reader_get_i64_by_key")
declare function _json_reader_get_i64_by_key(ptr: usize, ptr_len: usize, key_offset: usize, key_len: usize): i64;

// @ts-ignore
@external("env", "_json_reader_get_u64_by_key")
declare function _json_reader_get_u64_by_key(ptr: usize, ptr_len: usize, key_offset: usize, key_len: usize): u64;

// @ts-ignore
@external("env", "_json_reader_get_bool_by_key")
declare function _json_reader_get_bool_by_key(ptr: usize, ptr_len: usize, key_offset: usize, key_len: usize): i64;

// @ts-ignore
@external("env", "_json_reader_get_f64_by_key")
declare function _json_reader_get_f64_by_key(ptr: usize, ptr_len: usize, key_offset: usize, key_len: usize): f64;

// @ts-ignore
@external("env", "_json_reader_get_json_by_index")
declare function _json_array_get_json_by_index(ptr: usize, ptr_len: usize, idx: i32, json: usize): void;

// @ts-ignore
@external("env", "_json_reader_get_json_len_by_index")
declare function _json_reader_get_json_len_by_index(ptr: usize, ptr_len: usize, idx: i32): usize;

// @ts-ignore
@external("env", "_json_reader_get_str_by_index")
declare function _json_reader_get_str_by_index(ptr: usize, ptr_len: usize, idx: i32, str_offset: usize): void;

// @ts-ignore
@external("env", "_json_reader_get_str_len_by_index")
declare function _json_reader_get_str_len_by_index(ptr: usize, ptr_len: usize, idx: i32): usize;

// @ts-ignore
@external("env", "_json_reader_get_i64_by_index")
declare function _json_reader_get_i64_by_index(ptr: usize, ptr_len: usize, idx: i32): i64;

// @ts-ignore
@external("env", "_json_reader_get_u64_by_index")
declare function _json_reader_get_u64_by_index(ptr: usize, ptr_len: usize, idx: i32): u64;

// @ts-ignore
@external("env", "_json_reader_get_bool_by_index")
// if (b) val = 1 else val = 0
declare function _json_reader_get_bool_by_index(ptr: usize, ptr_len: usize, idx: i32): i64;

// @ts-ignore
@external("env", "_json_reader_get_f64_by_index")
declare function _json_reader_get_f64_by_index(ptr: usize, ptr_len: usize, idx: i32): f64;

// link to java JsonElement
export class JSONBuilder {
    static putJSON(name: string, json: string): void {
        let buf_name: ArrayBuffer = String.UTF8.encode(name);
        let buf_json: ArrayBuffer = String.UTF8.encode(json);
        _json_builder_put_json(changetype<usize>(buf_name), buf_name.byteLength, changetype<usize>(buf_json), buf_json.byteLength)
    }

    static setJSON(idx: i32, json: string): void {
        let buf_json: ArrayBuffer = String.UTF8.encode(json);
        _json_builder_set_json(idx, changetype<usize>(buf_json), buf_json.byteLength)
    }

    static putString(name: string, value: string): void {
        let buf_name: ArrayBuffer = String.UTF8.encode(name);
        let buf_value: ArrayBuffer = String.UTF8.encode(value);
        _json_builder_put_str(changetype<usize>(buf_name), buf_name.byteLength, changetype<usize>(buf_value), buf_value.byteLength)
    }

    static setString(idx: i32, value: string): void {
        let buf_json: ArrayBuffer = String.UTF8.encode(value);
        _json_builder_set_str(idx, changetype<usize>(buf_json), buf_json.byteLength)
    }

    static putBool(name: string, value: bool): void {
        let buf_name: ArrayBuffer = String.UTF8.encode(name);
        _json_builder_put_bool(changetype<usize>(buf_name), buf_name.byteLength, value ? 1 : 0)
    }

    static setBool(idx: i32, value: bool): void {
        _json_builder_set_bool(idx, value ? 1 : 0)
    }

    static putI64(name: string, value: i64): void {
        let buf_name: ArrayBuffer = String.UTF8.encode(name);
        _json_builder_put_i64(changetype<usize>(buf_name), buf_name.byteLength, value)
    }

    static setI64(idx: i32, value: i64): void {
        _json_builder_set_i64(idx, value);
    }

    static putU64(name: string, value: u64): void {
        let buf_name: ArrayBuffer = String.UTF8.encode(name);
        _json_builder_put_u64(changetype<usize>(buf_name), buf_name.byteLength, value)
    }

    static setU64(idx: i32, value: u64): void {
        _json_builder_set_u64(idx, value)
    }

    // warning: float-number will lose precision in json
    static putF64(name: string, value: f64): void {
        let buf_name: ArrayBuffer = String.UTF8.encode(name);
        _json_builder_put_f64(changetype<usize>(buf_name), buf_name.byteLength, value)
    }

    static setF64(idx: i32, value: f64): void {
        _json_builder_set_f64(idx, changetype<usize>(value))
    }

    static build(): string {
        const len = _json_builder_build_len();
        const ptr = new ArrayBuffer(len);
        _json_builder_build(changetype<usize>(ptr));
        return String.UTF8.decode(ptr);
    }
}

export class JSONReader {
    static getJSONByKey(json: string, key: string): string {
        let buf_json: ArrayBuffer = String.UTF8.encode(json);
        let buf_key: ArrayBuffer = String.UTF8.encode(key);
        let len = _json_reader_get_json_len_by_key(changetype<usize>(buf_json), buf_json.byteLength, changetype<usize>(buf_key), buf_key.byteLength);
        const ptr = new ArrayBuffer(len);
        _json_reader_get_json_by_key(changetype<usize>(buf_json), buf_json.byteLength, changetype<usize>(buf_key), buf_key.byteLength, changetype<usize>(ptr))
        return String.UTF8.decode(ptr);
    }

    static getJSONByIndex(json: string, idx: i32): string {
        let buf_json: ArrayBuffer = String.UTF8.encode(json);
        _json_reader_get_json_len_by_index(changetype<usize>(buf_json), buf_json.byteLength, idx);
        return String.UTF8.decode(buf_json);
    }

    static getStringByKey(json: string, name: string): string {
        let buf_json: ArrayBuffer = String.UTF8.encode(json);
        let buf_name: ArrayBuffer = String.UTF8.encode(name);
        let len = _json_reader_get_str_len(changetype<usize>(buf_json), buf_json.byteLength, changetype<usize>(buf_name), buf_name.byteLength);
        const ptr = new ArrayBuffer(len);
        _json_reader_get_str(changetype<usize>(buf_json), buf_json.byteLength, changetype<usize>(buf_name), buf_name.byteLength, changetype<usize>(ptr))
        return String.UTF8.decode(ptr);
    }

    static getStringByIndex(json: string, idx: i32): string {
        let buf_json: ArrayBuffer = String.UTF8.encode(json);
        let buf = new ArrayBuffer(_json_reader_get_str_len_by_index(changetype<usize>(buf_json), buf_json.byteLength, idx));
        _json_reader_get_str_by_index(changetype<usize>(buf_json), buf_json.byteLength, idx, changetype<usize>(buf));
        return String.UTF8.decode(buf);
    }

    static getBoolByKey(json: string, key: string): bool {
        let buf_json: ArrayBuffer = String.UTF8.encode(json);
        let buf_key: ArrayBuffer = String.UTF8.encode(key);
        return _json_reader_get_bool_by_key(changetype<usize>(buf_json), buf_json.byteLength, changetype<usize>(buf_key), buf_key.byteLength)
    }

    static getBoolByIndex(json: string, idx: i32): bool {
        let buf_json: ArrayBuffer = String.UTF8.encode(json);
        return _json_reader_get_bool_by_index(changetype<usize>(buf_json), buf_json.byteLength, idx) != 0;
    }

    static getI64ByKey(json: string, key: string): i64 {
        let buf_json: ArrayBuffer = String.UTF8.encode(json);
        let buf_key: ArrayBuffer = String.UTF8.encode(key);
        return _json_reader_get_i64_by_key(changetype<usize>(buf_json), buf_json.byteLength, changetype<usize>(buf_key), buf_key.byteLength)
    }

    static getI64ByIndex(json: string, idx: i32): i64 {
        let buf_json: ArrayBuffer = String.UTF8.encode(json);
        return _json_reader_get_i64_by_index(changetype<usize>(buf_json), buf_json.byteLength, idx)
    }

    static getU64ByKey(json: string, key: string): u64 {
        let buf_json: ArrayBuffer = String.UTF8.encode(json);
        let buf_key: ArrayBuffer = String.UTF8.encode(key);
        return _json_reader_get_u64_by_key(changetype<usize>(buf_json), buf_json.byteLength, changetype<usize>(buf_key), buf_key.byteLength)
    }

    static getU64ByIndex(json: string, idx: i32): u64 {
        let buf_json: ArrayBuffer = String.UTF8.encode(json);
        return _json_reader_get_u64_by_index(changetype<usize>(buf_json), buf_json.byteLength, idx)
    }

    static getF64ByKey(json: string, key: string): f64 {
        let buf_json: ArrayBuffer = String.UTF8.encode(json);
        let buf_key: ArrayBuffer = String.UTF8.encode(key);
        return _json_reader_get_f64_by_key(changetype<usize>(buf_json), buf_json.byteLength, changetype<usize>(buf_key), buf_key.byteLength)
    }

    static getF64ByIndex(json: string, idx: i32): f64 {
        let buf_json: ArrayBuffer = String.UTF8.encode(json);
        return _json_reader_get_f64_by_index(changetype<usize>(buf_json), buf_json.byteLength, idx)
    }
}


