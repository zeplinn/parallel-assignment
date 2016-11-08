import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class Alley {
    private final Car[] cars;
    private final HashMap<Loc, Border> alleyBorders;
    private Semaphore semAlleyMUTEX;
    private Semaphore semTrafficSwitch;
    private boolean isTrafficSwitcActive;
    private Queue<Car> carsDownToUpQueue;
    private Queue<Car> carsUpToDownQueue;
    private Thread trafficChangeThread;
    private EDirection alleyDir;
    private int carsInAlley;


    public Alley(Car[] cars, Border[] alleyBorders) {
        carsInAlley = 0;
        alleyDir = EDirection.none;
        this.cars = cars;
        this.carsDownToUpQueue = new LinkedList<>();
        this.carsUpToDownQueue = new LinkedList<>();

        this.alleyBorders = new HashMap<>();
        for (Border b : alleyBorders) {
            this.alleyBorders.put(b.loc, b);
        }

        semAlleyMUTEX = new Semaphore(1);
        semTrafficSwitch = new Semaphore(0);

        trafficChangeThread = new Thread() {
            @Override
            public void run() {
                while (true) {

                    try {
                        semAlleyMUTEX.P();
                        if (isTrafficSwitcActive && carsInAlley==0) {

                            if (alleyDir==EDirection.downToUp) {
                                alleyDir = EDirection.upToDown;
                                while (!carsUpToDownQueue.isEmpty())
                                    carsUpToDownQueue.poll().releaseBreaks();
                            } else if (alleyDir==EDirection.upToDown) {
                                alleyDir = EDirection.downToUp;
                                while (!carsDownToUpQueue.isEmpty())
                                    carsDownToUpQueue.poll().releaseBreaks();
                            } else {
                                alleyDir = EDirection.none;
                            }
                        }
                            isTrafficSwitcActive = false;
                        semAlleyMUTEX.V();

                        semTrafficSwitch.P();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        trafficChangeThread.setName("alley TrafficLight");
        trafficChangeThread.start();
    }

    public boolean tryEnter(int no) throws InterruptedException {
        Car car = cars[no];
        Loc loc = new Loc(car.curpos);
        Border border = null;
        if (alleyBorders.containsKey(loc)) border = alleyBorders.get(loc);
        if (border == null || !border.BorderType.equals(EBorder.enter)) return false;

        semAlleyMUTEX.P();

        if (alleyDir.equals(EDirection.none)) {
            alleyDir = border.trafficDirection;
        }
        if (!border.trafficDirection.equals(alleyDir)) {

            if (border.trafficDirection.equals(EDirection.upToDown)) {
                carsUpToDownQueue.add(car);
                car.cd.println("car going up --breaks--");

            } else {
                car.cd.println("car going down --breaks--");
                carsDownToUpQueue.add(car);
            }
            semAlleyMUTEX.V();
            car.pushBreaks();
            return tryEnter(no);

        } else {

            carsInAlley++;
            semAlleyMUTEX.V();
            return true;
        }


    }

    public boolean leave(int no) throws InterruptedException {
        Car car = cars[no];
        Loc loc = new Loc(car.curpos);
        Border border = null;
        if (alleyBorders.containsKey(loc)) border = alleyBorders.get(loc);
        if (border == null || !border.BorderType.equals(EBorder.exit)) return false;

        semAlleyMUTEX.P();
        carsInAlley--;
        car.cd.println("cars left in alley " + carsInAlley);
        if (carsInAlley == 0) {
            switchTraffic();
            semAlleyMUTEX.V();
        } else
            semAlleyMUTEX.V();
        return true;

    }

    private void switchTraffic() {
        if (!isTrafficSwitcActive) {
            isTrafficSwitcActive = true;
            semTrafficSwitch.V();
        }
    }

}

enum EDirection {
    none,
    downToUp,
    upToDown
}

enum EBorder {
    enter,
    exit
}

// position comparer, to avoid side effects of altering Pos.equals()/getHascode()
class Loc {
    final Pos pos;

    public Loc(Pos pos) {
        this.pos = pos;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Loc) {
            Loc l = (Loc) obj;
            return pos.row == l.pos.row && pos.col == l.pos.col;
        } else return false;
    }

    @Override
    public int hashCode() {
        return pos.row ^ pos.col;
    }
}

class Border {
    final Loc loc;
    final EDirection trafficDirection;
    final EBorder BorderType;

    public Border(int i, int j, EDirection trafficDirection, EBorder alleyBorderType) {
        loc = new Loc(new Pos(i, j));
        this.trafficDirection = trafficDirection;
        this.BorderType = alleyBorderType;
    }

}