import java.util.concurrent.atomic.AtomicInteger;
public class AtomRef<T> {
    final static AtomicInteger nextId = new AtomicInteger(0);
    final int id = nextId.getAndIncrement();
    private T data;
    private int count = 0;
    public synchronized T getAndSet(T t) {
        T tmp = data;
        data = t;
        return tmp;
    }
    public synchronized boolean compareAndSet(T oldval,T newval) {
        if(count == 0) assert data == null;
        if(count == 1) assert data != null;
        count++;
        assert count <= 2 : "Only 2 violated";
        if(data == oldval) {
            data = newval;
            return true;
        } else {
            return false;
        }
    }
    public String toString() {
        return "AtomRef("+id+","+(null==data)+")";
    }
    public synchronized T get() {
        return data;
    }
}
