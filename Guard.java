import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;

public class Guard implements Comparable<Guard> {
    final static AtomicInteger nextId = new AtomicInteger(0);
    final AtomicBoolean locked = new AtomicBoolean(false);
    final int id = nextId.getAndIncrement();
    public String toString() { return "g["+id+","+(locked.get() ? "T":"F")+"]"; }
    public int compareTo(Guard g) {
        return this.id - g.id;
    }
    //final AtomicReference<GuardTask> next = new AtomicReference<>();
    final AtomRef<GuardTask> next = new AtomRef<>();

    public void runGuarded(Runnable r) {
        TreeSet<Guard> guards_held = new TreeSet<>();
        guards_held.add(this);
        runGuarded(r,guards_held);
    }
    private void runGuarded(Runnable r,TreeSet<Guard> guards_held) {
        GuardTask gtask = new GuardTask(this,r,guards_held);
        assert gtask.cleanup;
        var prev = next.getAndSet(gtask);
        if(prev == null) {
            System.out.printf("1>> %s %d%n",this,ThreadID.get());
            gtask.run();
        } else {
            System.out.printf("order: %s -> %s%n",prev,gtask);
            if(!prev.next.compareAndSet(null,gtask)) {
                System.out.printf("2>> %s %d%n",this,ThreadID.get());
                gtask.run();
            }
        }
    }

    public void startGuarded(Runnable r,TreeSet<Guard> guards_held) {
        GuardTask gtask = new GuardTask(this, guards_held);
        gtask.setRun(r);
        startGuarded(gtask);
    }

    private void startGuarded(GuardTask gtask) {
        assert !gtask.cleanup;
        var prev = next.getAndSet(gtask);
        if(prev == null) {
            System.out.printf("3>> %s %d%n",this,ThreadID.get());
            gtask.run();
        } else {
            if(!prev.next.compareAndSet(null,gtask)) {
                System.out.printf("4>> %s %d%n",this,ThreadID.get());
                gtask.run();
            }
        }
    }

    public static void runGuarded(Runnable r,final Guard g1,final Guard g2) {
        if(g1.compareTo(g2) > 0) {
            runGuarded(r,g2,g1);
        } else {
            assert g1.compareTo(g2) < 0 : "Not sorted";
            TreeSet<Guard> gh = new TreeSet<>();
            gh.add(g1);
            gh.add(g2);
            final GuardTask gtask1 = new GuardTask(g1,gh);
            gtask1.setRun(()->{
                g2.runGuarded(()->{
                    Run.run(r);
                    gtask1.endRun();
                },gh);
            });
            g1.startGuarded(gtask1);
        }
    }

    public static void runGuarded(Runnable r,final Guard g1,final Guard g2,final Guard g3) {
        if(g1.compareTo(g2) > 0) {
            runGuarded(r,g2,g1,g3);
        } else if(g1.compareTo(g3) > 0) {
            runGuarded(r,g3,g2,g1);
        } else if(g2.compareTo(g3) > 0) {
            runGuarded(r,g1,g3,g2);
        } else {
            assert g1.compareTo(g2) < 0;
            assert g2.compareTo(g3) < 0;
            TreeSet<Guard> gh = new TreeSet<>();
            gh.add(g1);
            gh.add(g2);
            gh.add(g3);
            final GuardTask gtask1 = new GuardTask(g1,gh);
            final GuardTask gtask2 = new GuardTask(g2,gh);
            gtask2.setRun(()->{
                g3.runGuarded(()->{
                    Run.run(r);
                    gtask2.endRun();
                    gtask1.endRun();
                },gh);
            });
            gtask1.setRun(()->{
                g2.startGuarded(gtask2);
            });
            g1.startGuarded(gtask1);
        }
    }

    public static void runTree(Runnable r,final Guard g1,final Guard g2,final Guard g3) {
        TreeSet<Guard> ts = new TreeSet<>();
        ts.add(g1);
        ts.add(g2);
        ts.add(g3);
        runGuardedAll(r,ts);
    }

    public static void runGuardedAll(Runnable r,TreeSet<Guard> ts) {
        assert ts.size() > 1;
        //ist<Guard> lig = new ArrayList<>();
        //lig.addAll(ts);
        //assert lig.size() == ts.size();
        //TreeSet<Guard> guards_held = new TreeSet<>();
        //List<GuardTask> ligt = new ArrayList<>();
        /*
        for(int i=0;i<lig.size();i++) {
            ligt.add(new GuardTask(lig.get(i),guards_held));
        }
        for(int i=0;i<lig.size()-1;i++)
            assert lig.get(i).compareTo(lig.get(i+1)) < 0;
        assert ligt.size() == ts.size();
        // last task
        ligt.get(ligt.size()-1).setRun(()->{
            lig.get(lig.size()-1).runGuarded(()->{
                Run.run(r);
                // last to run unlocks everything
                for(int i=0;i<ligt.size()-1;i++) {
                    ligt.get(i).endRun();
                }
            },guards_held);
        });
        // prior tasks, each calls the next
        for(int i=0;i<ligt.size()-1;i++) {
            final var guard = lig.get(i);
            final var guardTaskNext = ligt.get(i+1);
            ligt.get(i).setRun(()->{
                guard.startGuarded(guardTaskNext);
            });
        }
        */
    }

    final public void checkLock() {
        //System.out.println("Lock of "+this+" by "+ThreadID.get());
        //assert checker.compareAndSet(false,true) : "Failure in acquiring guard";
        //GuardCheck.checkLock(this);
    }
    final public void checkUnlock() {
        //System.out.println("Unlock of "+this+" by "+ThreadID.get());
        //assert checker.compareAndSet(true,false) : "Failure in releasing guard";
        //GuardCheck.checkUnlock(this);
    }
}
