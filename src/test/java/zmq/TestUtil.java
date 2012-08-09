package zmq;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class TestUtil {

    public static AtomicInteger counter = new AtomicInteger(2);
    
    public static class DummyCtx extends Ctx {
        
    }
    
    public static DummyCtx CTX = new DummyCtx();
    
    public static class DummyIOThread extends IOThread {

        public DummyIOThread() {
            super(CTX, 2);
        }
    }
    
    public static class DummySocket extends SocketBase {

        public DummySocket() {
            super(CTX, counter.get(), counter.get());
            counter.incrementAndGet();
        }
        
    }
    
    public static class DummySession extends SessionBase {

        public List<Msg> out = new ArrayList<Msg>();
        
        public DummySession () {
            this(new DummyIOThread(),  false, new DummySocket(), new Options(), new Address("tcp", "localhost:9090"));
        }
        
        public DummySession(IOThread io_thread_, boolean connect_,
                SocketBase socket_, Options options_, Address addr_) {
            super(io_thread_, connect_, socket_, options_, addr_);
        }
        
        @Override
        public boolean write(Msg msg) {
            System.out.println("session.write " + msg);
            out.add(msg);
            return true;
        }
        
    }
    
    public static void bounce (SocketBase sb, SocketBase sc)
    {
        byte[] content = "12345678ABCDEFGH12345678abcdefgh".getBytes();

        //  Send the message.
        int rc = ZMQ.zmq_send (sc, content, 32, ZMQ.ZMQ_SNDMORE);
        assert (rc == 32);
        rc = ZMQ.zmq_send (sc, content, 32, 0);
        assertThat (rc ,is( 32));

        //  Bounce the message back.
        Msg msg;
        msg = ZMQ.zmq_recv (sb, 0);
        assert (msg.size() == 32);
        int rcvmore = ZMQ.zmq_getsockopt (sb, ZMQ.ZMQ_RCVMORE);
        assert (rcvmore == 1);
        msg = ZMQ.zmq_recv (sb, 0);
        assert (rc == 32);
        rcvmore = ZMQ.zmq_getsockopt (sb, ZMQ.ZMQ_RCVMORE);
        assert (rcvmore == 0);
        rc = ZMQ.zmq_send (sb, msg, ZMQ.ZMQ_SNDMORE);
        assert (rc == 32);
        rc = ZMQ.zmq_send (sb, msg, 0);
        assert (rc == 32);

        //  Receive the bounced message.
        msg = ZMQ.zmq_recv (sc, 0);
        assert (rc == 32);
        rcvmore = ZMQ.zmq_getsockopt (sc, ZMQ.ZMQ_RCVMORE);
        assert (rcvmore == 1);
        msg = ZMQ.zmq_recv (sc,  0);
        assert (rc == 32);
        rcvmore = ZMQ.zmq_getsockopt (sc, ZMQ.ZMQ_RCVMORE);
        assert (rcvmore == 0);
        //  Check whether the message is still the same.
        //assert (memcmp (buf2, content, 32) == 0);
    }
}