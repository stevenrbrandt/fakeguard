import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.TreeSet;

public class GuardTask {
    final static AtomicInteger activeCount = new AtomicInteger(-1);
    final Guard guard;
    final static GuardTask DONE = new GuardTask(null,()->{
        assert false : "DONE should not be executed";
    },null);
    final AtomicReference<GuardTask> next = new AtomicReference<>();

    final AtomicBoolean check1 = new AtomicBoolean(false);
    final AtomicBoolean check2 = new AtomicBoolean(false);
    final AtomicBoolean check3 = new AtomicBoolean(false);

    final TreeSet<Guard> guards_held;

    void add_guard() {
        synchronized(guards_held) {
            assert !guards_held.contains(guard);
            guards_held.add(guard);
        }
    }
    void del_guard() {
        synchronized(guards_held) {
            assert guards_held.contains(guard);
            guards_held.remove(guard);
        }
    }

    private Runnable r;
    final boolean cleanup;
    public GuardTask(Guard g,TreeSet<Guard> guards_held) {
        guard = g;
        cleanup = false;
        this.guards_held = guards_held;
        activeCount.getAndIncrement();
        add_guard();
    }
    public GuardTask(Guard g,Runnable r,TreeSet<Guard> guards_held) {
        guard = g;
        this.r = r;
        cleanup = true;
        this.guards_held = guards_held;
        activeCount.getAndIncrement();
        if(g != null) add_guard();
    }
    public void setRun(Runnable r) {
        assert this.r == null;
        this.r = r;
    }
    private void run_() {
        assert this != DONE : "DONE should not be executed";
        assert check2.compareAndSet(false, true) : "Rerun of Guard Task";
        assert check1.compareAndSet(false, true);
        int sz1 = guards_held.size();
        if(cleanup) for(Guard g : guards_held) GuardCheck.checkLock(g);
        Run.run(r);
        activeCount.getAndDecrement();
        int sz2 = guards_held.size();
        if(cleanup) assert sz1 == sz2 : String.format("%d != %d",sz1,sz2);
        if(cleanup) for(Guard g : guards_held) GuardCheck.checkUnlock(g);
    }
    public void run() {
        run_();
        if(cleanup)
            endRun_();
    }
    public void endRun() {
        assert !cleanup : "Manual cleanup on GuardTask set to auto clean";
        endRun_();
    }
    private void endRun_() {
        assert check1.compareAndSet(true, false) : "End without start";
        assert check3.compareAndSet(false, true) : "Double cleanup";
        //del_guard();
        AtomicReference<GuardTask> n = next;
        //int m = 0;
        while(!n.compareAndSet(null,DONE)) {
            final GuardTask gt = n.get();
            gt.run_();
            if(!gt.cleanup) return;
            n = gt.next;
            //if(m % 1000 == 999) System.out.println("m="+m);
            //m++;
        }
    }
}
