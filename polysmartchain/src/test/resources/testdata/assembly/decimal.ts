// @ts-ignore
@external("env", "_decimal_result")
//result z
declare function _decimal_result(z_offset: usize): void;

// @ts-ignore
@external("env", "_decimal_add")
// x + y = z
declare function _decimal_add(x_offset: usize, x_len: usize, y_offset: usize, y_len: usize): usize;

// @ts-ignore
@external("env", "_decimal_sub")
// x - y = z
declare function _decimal_sub(x_offset: usize, x_len: usize, y_offset: usize, y_len: usize): usize;

// @ts-ignore
@external("env", "_decimal_mul")
// x * y = z
declare function _decimal_mul(x_offset: usize, x_len: usize, y_offset: usize, y_len: usize): usize;

// @ts-ignore
@external("env", "_decimal_strict_div")
// x / y = z
declare function _decimal_strict_div(x_offset: usize, x_len: usize, y_offset: usize, y_len: usize): usize;

// @ts-ignore
@external("env", "_decimal_div")
// x / y = z
declare function _decimal_div(x_offset: usize, x_len: usize, y_offset: usize, y_len: usize, precision: i32): usize;

// @ts-ignore
@external("env", "_decimal_compare_to")
// x / y = z >1 =0 <-1
declare function _decimal_compare_to(x_offset: usize, x_len: usize, y_offset: usize, y_len: usize): i32;


// decimal will not be stored in memory
// please store decimal string and parse it next time
// e.g. use Map<string, string> other than Map<string, Decimal>
export class Decimal {
    static add(x: string, y: string): string {
        const str_x = String.UTF8.encode(x);
        const str_y = String.UTF8.encode(y);
        const result: usize = _decimal_add(changetype<usize>(str_x), str_x.byteLength, changetype<usize>(str_y), str_y.byteLength);
        const result_buf = new ArrayBuffer(result);
        _decimal_result(changetype<usize>(result_buf));
        return String.UTF8.decode(result_buf);
    }

    static sub(x: string, y: string): string {
        const str_x = String.UTF8.encode(x);
        const str_y = String.UTF8.encode(y);
        const result: usize = _decimal_sub(changetype<usize>(str_x), str_x.byteLength, changetype<usize>(str_y), str_y.byteLength);
        const result_buf = new ArrayBuffer(result);
        _decimal_result(changetype<usize>(result_buf));
        return String.UTF8.decode(result_buf);
    }

    static mul(x: string, y: string): string {
        const str_x = String.UTF8.encode(x);
        const str_y = String.UTF8.encode(y);
        const result: usize = _decimal_mul(changetype<usize>(str_x), str_x.byteLength, changetype<usize>(str_y), str_y.byteLength);
        const result_buf = new ArrayBuffer(result);
        _decimal_result(changetype<usize>(result_buf));
        return String.UTF8.decode(result_buf);
    }

    static strictDiv(x: string, y: string): string {
        const str_x = String.UTF8.encode(x);
        const str_y = String.UTF8.encode(y);
        const result: usize = _decimal_strict_div(changetype<usize>(str_x), str_x.byteLength, changetype<usize>(str_y), str_y.byteLength);
        const result_buf = new ArrayBuffer(result);
        _decimal_result(changetype<usize>(result_buf));
        return String.UTF8.decode(result_buf);
    }

    static div(x: string, y: string, precision: i32): string {
        const str_x = String.UTF8.encode(x);
        const str_y = String.UTF8.encode(y);
        const result: usize = _decimal_div(changetype<usize>(str_x), str_x.byteLength, changetype<usize>(str_y), str_y.byteLength, precision);
        const result_buf = new ArrayBuffer(result);
        _decimal_result(changetype<usize>(result_buf));
        return String.UTF8.decode(result_buf);
    }

    static compare(x: string, y: string): i32 {
        const str_x = String.UTF8.encode(x);
        const str_y = String.UTF8.encode(y);
        const result: usize = _decimal_compare_to(changetype<usize>(str_x), str_x.byteLength, changetype<usize>(str_y), str_y.byteLength);
        return result;
    }
}