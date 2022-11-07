import java.util.concurrent.ThreadLocalRandom;

public class IntVar {
    volatile int id;
    volatile int val;
    public synchronized int get() {
        id = ThreadID.get();
        return val;
    }
    public synchronized void set(int val) {
        int nid = ThreadID.get();
        assert nid == id;
        this.val = val;
    }
    public void incr() {
        int v = get();
        try {
            int sl = ThreadLocalRandom.current().nextInt(3);
            Thread.sleep(sl);
        } catch(InterruptedException ie) {}
        set(v+1);
    }
}
