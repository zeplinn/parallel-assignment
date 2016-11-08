import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Jesper on 07-11-2016.
 */
public class Barrier {
    private Car[] cars;
    private Queue<Car> waitingCars;
    private boolean isActivated;
    private final int i;
    private final int minJ;
    private final int maxJ;
    private final Semaphore semBarrierMutex;

    public Barrier(Car[] cars, Pos start, int horLengh) {
        this.cars = cars;
        waitingCars = new LinkedList<>();
        i = start.row;
        minJ = start.col;
        maxJ = start.col + horLengh;
        semBarrierMutex = new Semaphore(1);
    }

    public void sync(int no) throws InterruptedException {
        Car car = cars[no];
        //if(isActivated && car.curpos.row==i &&car.curpos.col>=minJ && car.curpos.col<=maxJ){
        if (isActivated && car.curpos.equals(car.barpos)) {
            semBarrierMutex.P();
            waitingCars.add(car);
            if (!(waitingCars.size() == cars.length)) {
                car.cd.println(waitingCars.size()+"/"+cars.length);
                semBarrierMutex.V();
                car.pushBreaks();
            } else {
                car.cd.println("free cars");
                semBarrierMutex.V();
                freeCars();
            }
        }
    }

    public void on() {
        this.isActivated = true;
    }

    public void off() {
        this.isActivated = false;
        freeCars();
    }

    private void freeCars() {
        while (!waitingCars.isEmpty()) {
            waitingCars.poll().releaseBreaks();
        }
    }
}
