import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.TreeSet;
import java.util.Set;

public class GuardTask {
    final static AtomicInteger nextId = new AtomicInteger(-1);
    final int id = nextId.getAndIncrement();
    public String toString() { return "gt["+id+","+guard+","+next+"]"; }
    final Guard guard;
    final static GuardTask DONE = new GuardTask(null,()->{
        assert false : "DONE should not be executed";
    },null);
    final AtomicReference<GuardTask> next = new AtomicReference<>();

    final TreeSet<Guard> guards_held;

    final static ThreadLocal<Set<Guard>> GUARDS_HELD = new ThreadLocal<>();

    private Runnable r;
    final boolean cleanup;
    public GuardTask(Guard g,TreeSet<Guard> guards_held) {
        guard = g;
        cleanup = false;
        this.guards_held = guards_held;
    }
    public GuardTask(Guard g,Runnable r,TreeSet<Guard> guards_held) {
        guard = g;
        this.r = r;
        cleanup = true;
        this.guards_held = guards_held;
    }
    public void setRun(Runnable r) {
        assert this.r == null;
        this.r = r;
    }
    private void run_() {
        int id = ThreadID.get();
        assert guard.locked.compareAndSet(false,true) : String.format("%s %d %d",this,ThreadID.get(),guards_held.size());
        if(cleanup) {
            GUARDS_HELD.set(guards_held);
            for(Guard g : guards_held)
                assert g.locked.get();
        }
        Run.run(r);
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
        assert guard.locked.compareAndSet(true,false);
        var n = next;
        while(!n.compareAndSet(null,DONE)) {
            final GuardTask gt = n.get();
            gt.run_();
            if(!gt.cleanup) return;
            assert gt.guard.locked.compareAndSet(true,false);
            n = gt.next;
        }
    }
}
