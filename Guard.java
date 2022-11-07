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
        runGuarded_(r,guards_held);
    }

    private void runGuarded_(Runnable r,TreeSet<Guard> guards_held) {
        GuardTask gtask = new GuardTask(this,r,guards_held);
        assert gtask.cleanup;
        var prev = next.getAndSet(gtask);
        if(prev == null) {
            gtask.run();
        } else {
            if(!prev.next.compareAndSet(null,gtask)) {
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
        //System.out.println("startGuarded: "+gtask);
        assert !gtask.cleanup;
        var prev = next.getAndSet(gtask);
        if(prev == null) {
            gtask.run();
        } else {
            if(!prev.next.compareAndSet(null,gtask)) {
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
                g2.runGuarded_(()->{
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
                g3.runGuarded_(()->{
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

    public static void runGuarded(Runnable r,final Guard... garray) {
        if(garray.length==0) {
            Run.run(r);
        } else if(garray.length == 1) {
            garray[0].runGuarded(r);
        } else {
            TreeSet<Guard> ts = new TreeSet<>();
            for(Guard g : garray)
                ts.add(g);
            runGuarded(r,ts);
        }
    }

    public static void runGuarded(Runnable r,TreeSet<Guard> ts) {
        assert ts.size() > 1;
        List<Guard> lig = new ArrayList<>();
        lig.addAll(ts);
        assert lig.size() == ts.size();
        TreeSet<Guard> guards_held = ts;//new TreeSet<>();
        List<GuardTask> ligt = new ArrayList<>();
        for(int i=0;i<lig.size();i++) {
            ligt.add(new GuardTask(lig.get(i),guards_held));
        }
        // Guards are correctly ordered
        int last = lig.size()-1;
        for(int i=0;i<last;i++)
            assert lig.get(i).compareTo(lig.get(i+1)) < 0;
        assert ligt.size() == ts.size();
        // last task
        ligt.get(last-1).setRun(()->{
            lig.get(last).runGuarded_(()->{
                Run.run(r);
                // last to run unlocks everything
                for(int i=0;i<last;i++) {
                    ligt.get(i).endRun();
                }
            },guards_held);
        });
        // prior tasks, each calls the next
        for(int i=0;i<last-1;i++) {
            final int step = i;
            final int next = i+1;
            final var guard = lig.get(i);
            final var guardTask = ligt.get(i);
            final var guardNext = lig.get(next);
            final var guardTaskNext = ligt.get(next);
            guardTask.setRun(()->{
                guardNext.startGuarded(guardTaskNext);
            });
        }
        lig.get(0).startGuarded(ligt.get(0));
    }
}
