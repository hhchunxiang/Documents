package java.io;

public class BufferedReader extends Reader {

    private Reader in;

    // 字符缓冲区
    private char cb[];
    // nChars 是cb缓冲区中字符的总的个数
    // nextChar 是下一个要读取的字符在cb缓冲区中的位置
    private int nChars, nextChar;

    // 表示“标记无效”。它与UNMARKED的区别是：
    // (01) UNMARKED 是压根就没有设置过标记。
    // (02) 而INVALIDATED是设置了标记，但是被标记位置太长，导致标记无效！
    private static final int INVALIDATED = -2;
    // 表示没有设置“标记”
    private static final int UNMARKED = -1;
    // “标记”
    private int markedChar = UNMARKED;
    // “标记”能标记位置的最大长度
    private int readAheadLimit = 0; /* Valid only when markedChar > 0 */

    // skipLF(即skip Line Feed)是“是否忽略换行符”标记
    private boolean skipLF = false;

    // 设置“标记”时，保存的skipLF的值
    private boolean markedSkipLF = false;

    // 默认字符缓冲区大小
    private static int defaultCharBufferSize = 8192;
    // 默认每一行的字符个数
    private static int defaultExpectedLineLength = 80;

    // 创建“Reader”对应的BufferedReader对象，sz是BufferedReader的缓冲区大小
    public BufferedReader(Reader in, int sz) {
        super(in);
        if (sz <= 0)
            throw new IllegalArgumentException("Buffer size <= 0");
        this.in = in;
        cb = new char[sz];
        nextChar = nChars = 0;
    }

    // 创建“Reader”对应的BufferedReader对象，默认的BufferedReader缓冲区大小是8k
    public BufferedReader(Reader in) {
        this(in, defaultCharBufferSize);
    }

    // 确保“BufferedReader”是打开状态
    private void ensureOpen() throws IOException {
        if (in == null)
            throw new IOException("Stream closed");
    }

    // 填充缓冲区函数。有以下两种情况被调用：
    // (01) 缓冲区没有数据时，通过fill()可以向缓冲区填充数据。
    // (02) 缓冲区数据被读完，需更新时，通过fill()可以更新缓冲区的数据。
    private void fill() throws IOException {
        // dst表示“cb中填充数据的起始位置”。
        int dst;
        if (markedChar <= UNMARKED) {
            // 没有标记的情况，则设dst=0。
            dst = 0;
        } else {
            // delta表示“当前标记的长度”，它等于“下一个被读取字符的位置”减去“标记的位置”的差值；
            int delta = nextChar - markedChar;
            if (delta >= readAheadLimit) {
                // 若“当前标记的长度”超过了“标记上限(readAheadLimit)”，
                // 则丢弃标记！
                markedChar = INVALIDATED;
                readAheadLimit = 0;
                dst = 0;
            } else {
                if (readAheadLimit <= cb.length) {
                    // 若“当前标记的长度”没有超过了“标记上限(readAheadLimit)”，
                    // 并且“标记上限(readAheadLimit)”小于/等于“缓冲的长度”；
                    // 则先将“下一个要被读取的位置，距离我们标记的置符的距离”间的字符保存到cb中。
                    System.arraycopy(cb, markedChar, cb, 0, delta);
                    markedChar = 0;
                    dst = delta;
                } else {
                    // 若“当前标记的长度”没有超过了“标记上限(readAheadLimit)”，
                    // 并且“标记上限(readAheadLimit)”大于“缓冲的长度”；
                    // 则重新设置缓冲区大小，并将“下一个要被读取的位置，距离我们标记的置符的距离”间的字符保存到cb中。
                    char ncb[] = new char[readAheadLimit];
                    System.arraycopy(cb, markedChar, ncb, 0, delta);
                    cb = ncb;
                    markedChar = 0;
                    dst = delta;
                }
                // 更新nextChar和nChars
                nextChar = nChars = delta;
            }
        }

        int n;
        do {
            // 从“in”中读取数据，并存储到字符数组cb中；
            // 从cb的dst位置开始存储，读取的字符个数是cb.length - dst
            // n是实际读取的字符个数；若n==0(即一个也没读到)，则继续读取！
            n = in.read(cb, dst, cb.length - dst);
        } while (n == 0);

        // 如果从“in”中读到了数据，则设置nChars(cb中字符的数目)=dst+n，
        // 并且nextChar(下一个被读取的字符的位置)=dst。
        if (n > 0) {
            nChars = dst + n;
            nextChar = dst;
        }
    }

    // 从BufferedReader中读取一个字符，该字符以int的方式返回
    public int read() throws IOException {
        synchronized (lock) {
            ensureOpen();
            for (;;) {
                // 若“缓冲区的数据已经被读完”，
                // 则先通过fill()更新缓冲区数据
                if (nextChar >= nChars) {
                    fill();
                    if (nextChar >= nChars)
                        return -1;
                }
                // 若要“忽略换行符”，
                // 则对下一个字符是否是换行符进行处理。
                if (skipLF) {
                    skipLF = false;
                    if (cb[nextChar] == '\n') {
                        nextChar++;
                        continue;
                    }
                }
                // 返回下一个字符
                return cb[nextChar++];
            }
        }
    }

    // 将缓冲区中的数据写入到数组cbuf中。off是数组cbuf中的写入起始位置，len是写入长度
    private int read1(char[] cbuf, int off, int len) throws IOException {
        // 若“缓冲区的数据已经被读完”，则更新缓冲区数据。
        if (nextChar >= nChars) {
            if (len >= cb.length && markedChar <= UNMARKED && !skipLF) {
                return in.read(cbuf, off, len);
            }
            fill();
        }
        // 若更新数据之后，没有任何变化；则退出。
        if (nextChar >= nChars) return -1;
        // 若要“忽略换行符”，则进行相应处理
        if (skipLF) {
            skipLF = false;
            if (cb[nextChar] == '\n') {
                nextChar++;
                if (nextChar >= nChars)
                    fill();
                if (nextChar >= nChars)
                    return -1;
            }
        }
        // 拷贝字符操作
        int n = Math.min(len, nChars - nextChar);
        System.arraycopy(cb, nextChar, cbuf, off, n);
        nextChar += n;
        return n;
    }

    // 对read1()的封装，添加了“同步处理”和“阻塞式读取”等功能
    public int read(char cbuf[], int off, int len) throws IOException {
        synchronized (lock) {
            ensureOpen();
            if ((off < 0) || (off > cbuf.length) || (len < 0) ||
                ((off + len) > cbuf.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }

            int n = read1(cbuf, off, len);
            if (n <= 0) return n;
            while ((n < len) && in.ready()) {
                int n1 = read1(cbuf, off + n, len - n);
                if (n1 <= 0) break;
                n += n1;
            }
            return n;
        }
    }

    // 读取一行数据。ignoreLF是“是否忽略换行符”
    String readLine(boolean ignoreLF) throws IOException {
        StringBuffer s = null;
        int startChar;

        synchronized (lock) {
            ensureOpen();
            boolean omitLF = ignoreLF || skipLF;

            bufferLoop:
            for (;;) {

                if (nextChar >= nChars)
                    fill();
                if (nextChar >= nChars) { /* EOF */
                    if (s != null && s.length() > 0)
                        return s.toString();
                    else
                        return null;
                }
                boolean eol = false;
                char c = 0;
                int i;

                /* Skip a leftover '\n', if necessary */
                if (omitLF && (cb[nextChar] == '\n'))
                    nextChar++;
                skipLF = false;
                omitLF = false;

            charLoop:
                for (i = nextChar; i < nChars; i++) {
                    c = cb[i];
                    if ((c == '\n') || (c == '\r')) {
                        eol = true;
                        break charLoop;
                    }
                }

                startChar = nextChar;
                nextChar = i;

                if (eol) {
                    String str;
                    if (s == null) {
                        str = new String(cb, startChar, i - startChar);
                    } else {
                        s.append(cb, startChar, i - startChar);
                        str = s.toString();
                    }
                    nextChar++;
                    if (c == '\r') {
                        skipLF = true;
                    }
                    return str;
                }

                if (s == null)
                    s = new StringBuffer(defaultExpectedLineLength);
                s.append(cb, startChar, i - startChar);
            }
        }
    }

    // 读取一行数据。不忽略换行符
    public String readLine() throws IOException {
        return readLine(false);
    }

    // 跳过n个字符
    public long skip(long n) throws IOException {
        if (n < 0L) {
            throw new IllegalArgumentException("skip value is negative");
        }
        synchronized (lock) {
            ensureOpen();
            long r = n;
            while (r > 0) {
                if (nextChar >= nChars)
                    fill();
                if (nextChar >= nChars) /* EOF */
                    break;
                if (skipLF) {
                    skipLF = false;
                    if (cb[nextChar] == '\n') {
                        nextChar++;
                    }
                }
                long d = nChars - nextChar;
                if (r <= d) {
                    nextChar += r;
                    r = 0;
                    break;
                }
                else {
                    r -= d;
                    nextChar = nChars;
                }
            }
            return n - r;
        }
    }

    // “下一个字符”是否可读
    public boolean ready() throws IOException {
        synchronized (lock) {
            ensureOpen();

            // 若忽略换行符为true；
            // 则判断下一个符号是否是换行符，若是的话，则忽略
            if (skipLF) {
                if (nextChar >= nChars && in.ready()) {
                    fill();
                }
                if (nextChar < nChars) {
                    if (cb[nextChar] == '\n')
                        nextChar++;
                    skipLF = false;
                }
            }
            return (nextChar < nChars) || in.ready();
        }
    }

    // 始终返回true。因为BufferedReader支持mark(), reset()
    public boolean markSupported() {
        return true;
    }

    // 标记当前BufferedReader的下一个要读取位置。关于readAheadLimit的作用，参考后面的说明。
    public void mark(int readAheadLimit) throws IOException {
        if (readAheadLimit < 0) {
            throw new IllegalArgumentException("Read-ahead limit < 0");
        }
        synchronized (lock) {
            ensureOpen();
            // 设置readAheadLimit
            this.readAheadLimit = readAheadLimit;
            // 保存下一个要读取的位置
            markedChar = nextChar;
            // 保存“是否忽略换行符”标记
            markedSkipLF = skipLF;
        }
    }

    // 重置BufferedReader的下一个要读取位置，
    // 将其还原到mark()中所保存的位置。
    public void reset() throws IOException {
        synchronized (lock) {
            ensureOpen();
            if (markedChar < 0)
                throw new IOException((markedChar == INVALIDATED)
                                      ? "Mark invalid"
                                      : "Stream not marked");
            nextChar = markedChar;
            skipLF = markedSkipLF;
        }
    }

    public void close() throws IOException {
        synchronized (lock) {
            if (in == null)
                return;
            in.close();
            in = null;
            cb = null;
        }
    }
}