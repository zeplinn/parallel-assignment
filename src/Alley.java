class Alley5 {
    public final Pos southEntry;
    public final Pos northEntry;
    int enteredCars = 0;
    private Semaphore semAlley;
    //private Semaphore semAlleyLeave;
    private Semaphore semAlleyUp;
    private Semaphore semAlleyDown;
    private EDirection trafficDir;
    Car[] cars;
    EDirection[] carDir;


    public Alley5(Car[] cars, Pos sEntry, Pos nEntry) {
        northEntry = nEntry;
        southEntry = sEntry;
        trafficDir = EDirection.none;
        if (cars == null) throw new NullPointerException("cars can not be null");
        semAlley = new Semaphore(1);
        //semAlleyLeave = new Semaphore(1);
        semAlleyUp = new Semaphore(0);
        semAlleyDown = new Semaphore(0);
        this.cars = cars;
        carDir = new EDirection[cars.length];
    }

    public void enter(int no) throws InterruptedException {
        //critical section
        semAlley.P();
        Car car = cars[no];
        EDirection carDir = carAlleyDirection(car.newpos);
        switch (trafficDir) {
            case none:
                if (carDir.equals(EDirection.down)) {
                    semAlleyDown.V();
                } else {
                    semAlleyUp.V();
                }
                enterDone();
                //cars will always have a direction
                //trafficLight(carDir,EDirection.none);
                break;
            case up:
                if (car.equals(EDirection.down)) {
                    semAlley.V();
                    semAlleyDown.P();
                    semAlley.P();
                    enteredCars++;
                    semAlley.V();
                    semAlleyDown.V();
                } else {
                    enterDone();
                }
                break;
            case down:
                if (car.equals(EDirection.up)) {
                    semAlley.V();
                    semAlleyUp.P();
                    semAlley.P();
                    enteredCars++;
                    semAlley.V();
                    semAlleyUp.V();
                } else {
                    enterDone();
                }
                break;
        }

    }

    private void enterDone() {
        enteredCars++;
        semAlley.V();
    }

    public void leave(int no) throws InterruptedException {
        //critical section
        semAlley.P();
        Car car = cars[no];
        EDirection carDir = carAlleyDirection(car.curpos);
        enteredCars--;
        if (enteredCars == 0) {
            trafficDir = EDirection.none;
            if (carDir.equals(EDirection.down)) {
                semAlleyDown.P(); // reset to 0
                semAlleyUp.V();// change traffic dir
            } else if (carDir.equals(EDirection.up)) {
                semAlleyUp.P(); // reset to 0
                semAlleyDown.V();// change trafic dir
            } else throw new UnsupportedOperationException("no such car direction exist: " + carDir);
        }
        semAlley.V();
    }

    private void trafficLight(EDirection car, EDirection move, Semaphore semMove, Semaphore semStop) throws InterruptedException {
        if (car.equals(move)) {
            enteredCars++;
            trafficDir = move;
            semAlley.V();
        } else {
            semAlley.V();
            //waiting for traffic to change
            semStop.P();
            trafficDir = move;
            enteredCars++;
            semAlleyUp.V();

        }
    }

    private EDirection carAlleyDirection(Pos pos) {
        if (northEntry.equals(pos)) return EDirection.up;
        else if (southEntry.equals(pos)) return EDirection.down;
        else throw new UnsupportedOperationException("car is not at alley entrance: " + pos);
    }
}

public class Alley {
    public final Pos southEntry;
    public final Pos northEntry;
    int downCarsCount = 0;
    int upCarsCount = 0;
    private Semaphore semAlley;
    private Semaphore semAlleyLeave;
    private Semaphore semPause;

    //private Semaphore semAlleyLeave;
    private Semaphore semAlleyUpDir;
    private Semaphore semAlleyDownDir;
    private EDirection trafficDir;
    Car[] cars;
    private Semaphore semEnteredCars;
    private Thread t;


    public Alley(Car[] cars, Pos sEntry, Pos nEntry) {
        northEntry = nEntry;
        southEntry = sEntry;
        trafficDir = EDirection.none;
        if (cars == null) throw new NullPointerException("cars can not be null");
        semAlley = new Semaphore(1);
        semPause = new Semaphore(0);
        semAlleyLeave = new Semaphore(1);
        semEnteredCars = new Semaphore(1);
        semAlleyDownDir = new Semaphore(0);
        semAlleyUpDir = new Semaphore(0);


        this.cars = cars;

        // independent thread for trafficlight control
        this.t = new Thread() {
            public void run() {
                // System.out.println("green light start");
                try {
                    while (true) {
                        semPause.P();
//System.out.println("green light");
                        //sleep(1000);
                        semAlley.P();
                        //System.out.println("entered alley");
                        if (trafficDir.equals(EDirection.up)) {
                            semAlleyUpDir.V();
                        } else {
                            semAlleyDownDir.V();
                        }
                        semAlley.V();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.out.println(" traffic light crashed");
                }
            }
        };
        this.t.setName("trafic light control");
        t.setPriority(Thread.MAX_PRIORITY);
        this.t.start();
    }

    public void enter(int no) throws InterruptedException {
        semAlley.P();
        Car car = cars[no];
        EDirection carDir = carAlleyEnterDirection(car.newpos);
        AlleyCarDirectionCount(true, carDir);


        if (trafficDir.equals(EDirection.none)) {
            trafficDir = carDir;
        }


        if (carDir.equals(EDirection.up)) {
            semAlley.V();
            semPause.V();
            semAlleyUpDir.P();
        } else {
            semAlley.V();
            semPause.V();
            semAlleyDownDir.P();
        }

    }

    public void leave(int no) throws InterruptedException, Exception {
        semAlley.P();
        Car car = cars[no];
        EDirection carDir = carAlleyLeaveDirection(car.curpos);
        AlleyCarDirectionCount(false, carDir);
        if (upCarsCount == 0 && downCarsCount == 0) {
            trafficDir = EDirection.none;
        } else if (trafficDir.equals(EDirection.down) && downCarsCount == 0 && upCarsCount > 0) {
            trafficDir = EDirection.up;
        } else if (trafficDir.equals(EDirection.up) && upCarsCount == 0 && downCarsCount > 0) {
            trafficDir = EDirection.down;

        }
        //else throw new Exception(String.format("leave did not terminate as expected up-count=%d, down-count=%d",upCarsCount,downCarsCount));
        if (upCarsCount == 0 || downCarsCount == 0)
            System.out.print(String.format("%s(%d,%d)\t", trafficDir, upCarsCount, downCarsCount));
        semAlley.V();
    }

    private EDirection carAlleyLeaveDirection(Pos pos) {
        if (northEntry.equals(pos)) return EDirection.down;
        else if (southEntry.equals(pos)) return EDirection.up;
        else throw new UnsupportedOperationException("car is not at alley entrance: " + pos);
    }

    // incr = false -> incr--, incr = true ->incr++
    private void AlleyCarDirectionCount(boolean incr, EDirection carDir) throws InterruptedException {
//        semEnteredCars.P();
        switch (carDir) {
            case none:
                break;
            case down:
                if (incr) downCarsCount++;
                else downCarsCount--;
                break;
            case up:
                if (incr) upCarsCount++;
                else upCarsCount--;
                break;
        }
        // semEnteredCars.V();
    }

    private EDirection carAlleyEnterDirection(Pos pos) {
        if (northEntry.equals(pos)) return EDirection.up;
        else if (southEntry.equals(pos)) return EDirection.down;
        else throw new UnsupportedOperationException("car is not at alley entrance: " + pos);
    }


}


enum EDirection {
    none,
    down,
    up
}