/**
 * Created by Jesper on 30-10-2016.
 */
public class CarBreaks {
    private Semaphore semBreaks;
    private boolean isStopped;
    public CarBreaks(){
        isStopped= false;
        semBreaks = new Semaphore(0);
    }

    public void stop() throws InterruptedException {
        isStopped = true;
        semBreaks.P();
    }

    public void Run(){
        semBreaks.V();

    }

}


