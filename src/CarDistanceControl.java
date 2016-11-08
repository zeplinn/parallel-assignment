import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by JesperRytter on 10/27/2016.
 */
public class CarDistanceControl {
    private Queue<Car>[] carSpotQueue;
    private Car[] cars;
    //private Semaphore semBumperControl;
    private Semaphore semBumperMUTEX;
    private Queue<Queue<Car>> emptyCarQueues;
    //private HashMap<Loc, PosQueing> postionControl;
    //private Queue<Car> releaseOrder;
    //private Thread carReleaseThread;
    private final int mapWidth;
    // private boolean isReleaseThreadPaused;

    public CarDistanceControl(Car[] cars, int mapWidth, int mapHeight) {
        carSpotQueue = new LinkedList[mapWidth * mapHeight];
        this.mapWidth = mapWidth;
        this.cars = cars;
        emptyCarQueues = new LinkedList<>();

        for (int i = 0; i < cars.length * 2; i++) {
            emptyCarQueues.add(new LinkedList<>());
        }
//        for (Car car : cars){
//            carSpotQueue[indexOf(car.startpos)]=emptyCarQueues.poll();
//        }

        // semBumperControl = new Semaphore(0);
        semBumperMUTEX = new Semaphore(1);

       /* carReleaseThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    if (releaseOrder.isEmpty()) {
                        try {
                            isReleaseThreadPaused=true;
                            semBumperControl.P();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // mutex
                        try {
                            semBumperMUTEX.P();
                            releaseOrder.poll().releaseBreaks();
                            semBumperMUTEX.V();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };*/
    }

    public boolean isNewPostionAvailable(int no, Pos newPos) throws InterruptedException {
        Car car = cars[no];
        int index = indexOf(newPos);
        semBumperMUTEX.P();

        Queue<Car> queue = carSpotQueue[index];
        if (queue == null) {
            carSpotQueue[index] = emptyCarQueues.poll();
            semBumperMUTEX.V();
            return true;
        } else {
            queue.add(car);
            semBumperMUTEX.V();
            car.pushBreaks();
        }
        return false;
    }

    public void notifyPositionChanged(int no, Pos oldPos) throws InterruptedException {
        Car car = cars[no];
        int index = indexOf(oldPos);
        semBumperMUTEX.P();
        Queue<Car> carQueue = carSpotQueue[index];
        if (carQueue != null) {

            if (carQueue.isEmpty()) {
                emptyCarQueues.add(carQueue);
                carSpotQueue[index] = null;
            } else {
                carQueue.poll().releaseBreaks();

            }
        }
        semBumperMUTEX.V();
    }

    private int indexOf(Pos pos) {
        return pos.col * mapWidth + pos.row;
    }


}

