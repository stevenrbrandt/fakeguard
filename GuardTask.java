import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.TreeSet;
import java.util.Set;

public class GuardTask {
    final static AtomicInteger nextId = new AtomicInteger(-1);
    final int id = nextId.getAndIncrement();
    public String toString() { return "gt["+id+","+guard+","+next+"]"; }
    final static AtomicInteger activeCount = new AtomicInteger(-1);
    final Guard guard;
    final static GuardTask DONE = new GuardTask(null,()->{
        assert false : "DONE should not be executed";
    },null);
    //final AtomicReference<GuardTask> next = new AtomicReference<>();
    final AtomRef<GuardTask> next = new AtomRef<>();

    final AtomicBoolean check1 = new AtomicBoolean(false);
    final AtomicBoolean check2 = new AtomicBoolean(false);
    final AtomicBoolean check3 = new AtomicBoolean(false);

    final TreeSet<Guard> guards_held;

    final static ThreadLocal<Set<Guard>> GUARDS_HELD = new ThreadLocal();

    private Runnable r;
    final boolean cleanup;
    public GuardTask(Guard g,TreeSet<Guard> guards_held) {
        guard = g;
        cleanup = false;
        this.guards_held = guards_held;
        activeCount.getAndIncrement();
    }
    public GuardTask(Guard g,Runnable r,TreeSet<Guard> guards_held) {
        guard = g;
        this.r = r;
        cleanup = true;
        this.guards_held = guards_held;
        activeCount.getAndIncrement();
    }
    public void setRun(Runnable r) {
        assert this.r == null;
        this.r = r;
    }
    private void run_() {
        assert this != DONE : "DONE should not be executed";
        assert check2.compareAndSet(false, true) : "Rerun of Guard Task";
        assert check1.compareAndSet(false, true);
        int id = ThreadID.get();
        assert guard.locked.compareAndSet(false,true) : String.format("%s %d %d",this,ThreadID.get(),guards_held.size());
        if(cleanup) for(Guard g : guards_held) assert g.id <= guard.id : "Not last";
        if(cleanup) GUARDS_HELD.set(guards_held);
        //if(cleanup) for(Guard g : guards_held) GuardCheck.checkLock(g,id);
        Run.run(r);
        activeCount.getAndDecrement();
        //if(cleanup) for(Guard g : guards_held) GuardCheck.checkUnlock(g,id);
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
    private AtomRef<GuardTask> finish() {
        assert check1.compareAndSet(true, false) : "End without start";
        assert check3.compareAndSet(false, true) : "Double cleanup";
        assert guard.locked.compareAndSet(true,false);
        return next;
    }
    private void endRun_() {
        var n = finish();
        while(!n.compareAndSet(null,DONE)) {
            final GuardTask gt = n.get();
            gt.run_();
            if(!gt.cleanup) return;
            n = gt.finish();
            //n = gt.next;
            //if(m % 1000 == 999) System.out.println("m="+m);
            //m++;
        }
    }
}
