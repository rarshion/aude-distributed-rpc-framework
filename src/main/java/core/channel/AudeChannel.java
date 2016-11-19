package core.channel;

import core.message.IMessage;
import core.serializer.ISerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by lrkin on 2016/11/19.
 * 通信通道
 * <p>
 * 消息报文的规格:前4个字节表示本次报文的长度,根据此长度读取剩余的数据报文
 */
public class AudeChannel implements IChannel {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String id;
    private final long timeout;
    private final ISerializer serializer;
    private AsynchronousSocketChannel channel;

    public AudeChannel(final AsynchronousSocketChannel channel, String id, long timeout, ISerializer serializer) {
        this.id = id;
        this.timeout = timeout;
        this.serializer = serializer;
        this.channel = channel;
    }

    public String id() {
        return this.id;
    }

    public boolean isOpen() {
        return this.channel.isOpen();
    }

    public <M extends IMessage> M read(Class<M> messageClazz) {
        if (this.isOpen()) {
            //1.先去channel取前四个字节,转为int,获取到消息的长度
            final ByteBuffer messageLength = ByteBuffer.allocate(4);
            try {
                final Integer integer = this.channel.read(messageLength).get(timeout, TimeUnit.MILLISECONDS);
                if (-1 == integer) {
                    log.debug("关闭连接 {} <-> {}", this.channel.getLocalAddress(), this.channel.getRemoteAddress());
                    close();
                    return null;
                }
                messageLength.flip();
                final int length = messageLength.getInt();
                //2.再去取完整的数据
                final ByteBuffer message = ByteBuffer.allocate(length);
                this.channel.read(message).get();
                message.flip();
                return this.serializer.encoder(message.array(), messageClazz);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void write(IMessage message) {
        if (this.isOpen()) {
            this.serializer.decoder(message);
        }

    }

    public void close() throws IOException {

    }
}
