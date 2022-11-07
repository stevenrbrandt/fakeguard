public class IntVar {
    volatile int id;
    volatile int val;
    public int get() {
        id = ThreadID.get();
        return val;
    }
    public void set(int val) {
        int nid = ThreadID.get();
        assert nid == id;
        this.val = val;
    }
    public void incr() {
        int v = get();
        //try { Thread.sleep(1); } catch(InterruptedException ie) {}
        set(v+1);
    }
}
