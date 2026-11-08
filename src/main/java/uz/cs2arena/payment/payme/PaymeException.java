package uz.cs2arena.payment.payme;

/** Payme Merchant API xato kodlari (hujjatdagi rasmiy kodlar). */
public class PaymeException extends RuntimeException {

    public static final int INVALID_AMOUNT = -31001;
    public static final int TRANSACTION_NOT_FOUND = -31003;
    public static final int CANNOT_CANCEL = -31007;
    public static final int CANNOT_PERFORM = -31008;
    public static final int ORDER_NOT_FOUND = -31050;
    public static final int ORDER_BUSY = -31051;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int AUTH_FAILED = -32504;
    public static final int PARSE_ERROR = -32700;

    private final int code;
    private final String data;

    public PaymeException(int code, String message, String data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public PaymeException(int code, String message) {
        this(code, message, null);
    }

    public int getCode() { return code; }
    public String getData() { return data; }
}
