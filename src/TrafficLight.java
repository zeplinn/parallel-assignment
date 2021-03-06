/*
import java.util.Queue;

*/
/**
 * Created by JesperRytter on 10/28/2016.
 *//*

public class TrafficLight extends Thread {
    private Semaphore semDownDir;
    private Semaphore semUpDir;
    private  Semaphore semLightControl;
    private Semaphore semExclusion;
    private Queue<Semaphore> downCarQueue;
    private Queue<Semaphore> upCarQueue;
    private EDirection trafficDir;
    private Car[] cars;

    public TrafficLight(Car[] cars) {
        this.cars = cars;
        trafficDir = EDirection.none;
        semDownDir = new Semaphore(0);
        semUpDir = new Semaphore(0);
        semLightControl= new Semaphore(0);
        semExclusion = new Semaphore(1);

        try {
            lightControl();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void Queue(EDirection carDir) throws InterruptedException {
        semExclusion.P();
        Semaphore light = new Semaphore(0);
        switch (trafficDir) {
            case none:
                trafficDir=carDir;

                break;
            case downToUp:
                if(carDir.equals(EDirection.upToDown)){
                    upCarQueue.add(no);
                }
                break;
            case upToDown:
                if(carDir.equals(EDirection.downToUp)){
                    downCarQueue.add(no);
                }
                break;
        }
        semExclusion.V();
        semLightControl.V();
    }
    public void queCar(Queue<Semaphore> f , Queue<Semaphore> s, EDirection carDir){
        Semaphore light = new Semaphore(0);
        if(carDir.equals(EDirection.upToDown)){
            upCarQueue.add(light);
            semLightControl.V();
        }
    }
    private void lightControl() throws InterruptedException {
        while(true){
            semLightControl.P();
            semExclusion.P();
            switch (trafficDir) {
                case none:
                    break;
                case downToUp:
                    greenLight(downCarQueue,upCarQueue,EDirection.upToDown);

                    break;
                case upToDown:
                    greenLight(upCarQueue,downCarQueue,EDirection.downToUp);
                    break;
            }
        }
    }
    private void greenLight(Queue<Semaphore> f, Queue<Semaphore> s,EDirection sdir)  {
        try {
            semExclusion.P();
            if( !f.isEmpty())
                f.poll().V();
            else if(!s.isEmpty()) {
                trafficDir = sdir;
                s.poll().V();
            }
            semExclusion.V();
        } catch (InterruptedException e) {

            e.printStackTrace();
        }

    }
}

*/
