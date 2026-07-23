//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package lcm.lcm;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;

public final class LCMDataInputStream implements DataInput {
    byte[] buf;
    int pos = 0;
    int len;
    int startpos;
    int endpos;

    public LCMDataInputStream(byte[] var1) {
        this.buf = var1;
        this.endpos = var1.length + 1;
    }

    public LCMDataInputStream(byte[] var1, int var2, int var3) {
        this.buf = var1;
        this.pos = var2;
        this.startpos = var2;
        this.endpos = var2 + var3 + 1;
    }

    void needInput(int var1) throws EOFException {
        if (this.pos + var1 >= this.endpos) {
            throw new EOFException("LCMDataInputStream needed " + var1 + " bytes, only " + this.available() + " available.");
        }
    }

    public int available() {
        return this.endpos - this.pos - 1;
    }

    public void close() {
    }

    public void reset() {
        this.pos = this.startpos;
    }

    public boolean readBoolean() throws IOException {
        this.needInput(1);
        return this.buf[this.pos++] != 0;
    }

    public byte readByte() throws IOException {
        this.needInput(1);
        return this.buf[this.pos++];
    }

    public int readUnsignedByte() throws IOException {
        this.needInput(1);
        return this.buf[this.pos++] & 255;
    }

    public char readChar() throws IOException {
        return (char)this.readShort();
    }

    public short readShort() throws IOException {
        this.needInput(2);
        return (short)((this.buf[this.pos++] & 255) << 8 | (this.buf[this.pos++] & 255) << 0);
    }

    public int readUnsignedShort() throws IOException {
        this.needInput(2);
        return (this.buf[this.pos++] & 255) << 8 | (this.buf[this.pos++] & 255) << 0;
    }

    public int readInt() throws IOException {
        this.needInput(4);
        return (this.buf[this.pos++] & 255) << 24 | (this.buf[this.pos++] & 255) << 16 | (this.buf[this.pos++] & 255) << 8 | (this.buf[this.pos++] & 255) << 0;
    }

    public long readLong() throws IOException {
        this.needInput(8);
        return ((long)this.buf[this.pos++] & 255L) << 56 | ((long)this.buf[this.pos++] & 255L) << 48 | ((long)this.buf[this.pos++] & 255L) << 40 | ((long)this.buf[this.pos++] & 255L) << 32 | ((long)this.buf[this.pos++] & 255L) << 24 | ((long)this.buf[this.pos++] & 255L) << 16 | ((long)this.buf[this.pos++] & 255L) << 8 | ((long)this.buf[this.pos++] & 255L) << 0;
    }

    public float readFloat() throws IOException {
        return Float.intBitsToFloat(this.readInt());
    }

    public void readFully(byte[] var1) throws IOException {
        this.needInput(var1.length);
        System.arraycopy(this.buf, this.pos, var1, 0, var1.length);
        this.pos += var1.length;
    }

    public void readFully(byte[] var1, int var2, int var3) throws IOException {
        this.needInput(var3);
        System.arraycopy(this.buf, this.pos, var1, var2, var3);
        this.pos += var3;
    }

    public void readFullyBytesAsChars(char[] var1) throws IOException {
        this.needInput(var1.length);

        for(int var2 = 0; var2 < var1.length; ++var2) {
            var1[var2] = (char)(this.buf[this.pos++] & 255);
        }

    }

    public double readDouble() throws IOException {
        return Double.longBitsToDouble(this.readLong());
    }

    public String readLine() throws IOException {
        StringBuffer var1 = new StringBuffer();

        while(true) {
            this.needInput(1);
            byte var2 = this.buf[this.pos++];
            if (var2 == 0) {
                return var1.toString();
            }

            var1.append((char)var2);
        }
    }

    public String readStringZ() throws IOException {
        StringBuffer var1 = new StringBuffer();

        while(true) {
            int var2 = this.buf[this.pos++] & 255;
            if (var2 == 0) {
                return var1.toString();
            }

            var1.append((char)var2);
        }
    }

    public String readUTF() throws IOException {
        assert false;

        return null;
    }

    public int skipBytes(int var1) {
        this.pos += var1;
        return var1;
    }

    public byte[] getBuffer() {
        return this.buf;
    }

    public int getBufferOffset() {
        return this.pos;
    }
}
