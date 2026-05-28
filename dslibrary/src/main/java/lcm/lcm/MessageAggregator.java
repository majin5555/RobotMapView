//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package lcm.lcm;

import java.io.IOException;
import java.util.LinkedList;

public class MessageAggregator implements LCMSubscriber {
    LinkedList<Message> messages = new LinkedList<>();
    long queue_data_size = 0L;
    long max_queue_data_size = 104857600L;
    int max_queue_length = Integer.MAX_VALUE;

    public synchronized void messageReceived(LCM var1, String var2, LCMDataInputStream var3) {
        try {
            byte[] var4 = new byte[var3.available()];
            var3.readFully(var4);
            this.messages.addLast(new Message(var2, var4));

//            Message var5;
//            for(this.queue_data_size += (long)var4.length; this.queue_data_size > this.max_queue_data_size || this.messages.size() > this.max_queue_length; this.queue_data_size -= (long)var5.data.length) {
//                var5 = (Message)this.messages.removeFirst();
//            }

            this.queue_data_size += var4.length;
            while (this.queue_data_size > this.max_queue_data_size
                    || this.messages.size() > this.max_queue_length) {
                Message var5 = this.messages.removeFirst();
                this.queue_data_size -= var5.data.length;
            }

            this.notify();
        } catch (IOException var6) {
        }

    }

    public synchronized void setMaxBufferSize(long var1) {
        this.max_queue_data_size = var1;
    }

    public synchronized long getMaxBufferSize() {
        return this.max_queue_data_size;
    }

    public synchronized void setMaxMessages(int var1) {
        this.max_queue_length = var1;
    }

    public synchronized int getMaxMessages() {
        return this.max_queue_length;
    }

    public synchronized Message getNextMessage(long var1) {
        if (!this.messages.isEmpty()) {
            Message var5 = (Message) this.messages.removeFirst();
            this.queue_data_size -= (long) var5.data.length;
            return var5;
        } else if (var1 == 0L) {
            return null;
        } else {
            try {
                if (var1 > 0L) {
                    this.wait(var1);
                } else {
                    this.wait();
                }

                if (!this.messages.isEmpty()) {
                    Message var3 = (Message) this.messages.removeFirst();
                    this.queue_data_size -= (long) var3.data.length;
                    return var3;
                }
            } catch (InterruptedException var4) {
            }

            return null;
        }
    }

    public synchronized Message getNextMessage() {
        return this.getNextMessage(-1L);
    }

    public synchronized int numMessagesAvailable() {
        return this.messages.size();
    }

    public class Message {
        public final byte[] data;
        public final String channel;

        public Message(String var2, byte[] var3) {
            this.data = var3;
            this.channel = var2;
        }
    }
}
