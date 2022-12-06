import java.util.concurrent.ThreadLocalRandom;

public class IntVar {
    volatile int id;
    volatile int val;
    public IntVar() {}
    public IntVar(int n) { val = n; }
    synchronized int _get() {
        id = ThreadID.get();
        return val;
    }
    public int get() {
        int r = _get();
        try {
            int sl = ThreadLocalRandom.current().nextInt(3);
            Thread.sleep(sl);
        } catch(InterruptedException ie) {}
        return r;
    }
    public synchronized void set(int val) {
        int nid = ThreadID.get();
        assert nid == id;
        this.val = val;
    }
    public void incr() {
        int v = get();
        set(v+1);
    }
    public String toString() { return ""+val+"{"+id+"}"; }
}
