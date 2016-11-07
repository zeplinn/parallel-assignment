//Prototype implementation of Car Control
//Mandatory assignment
//Course 02158 Concurrent Programming, DTU, Fall 2016

//Hans Henrik Lovengreen    Oct 3, 2016


import java.awt.Color;

class Gate {

    Semaphore g = new Semaphore(0);
    Semaphore e = new Semaphore(1);
    boolean isopen = false;

    public void pass() throws InterruptedException {
        g.P();
        g.V();
    }

    public void open() {
        try {
            e.P();
        } catch (InterruptedException e) {
        }
        if (!isopen) {
            g.V();
            isopen = true;
        }
        e.V();
    }

    public void close() {
        try {
            e.P();
        } catch (InterruptedException e) {
        }
        if (isopen) {
            try {
                g.P();
            } catch (InterruptedException e) {
            }
            isopen = false;
        }
        e.V();
    }

}

class Car extends Thread {
    private final CarDistanceControl carDistanceControl;
    private final Barrier barrier;
    private Alley alley;
    int basespeed = 100;             // Rather: degree of slowness
    int variation = 50;             // Percentage of base speed
    private Semaphore semBreaks;
    CarDisplayI cd;                  // GUI part

    int no;                          // Car number
    Pos startpos;                    // Startpositon (provided by GUI)
    Pos barpos;                      // Barrierpositon (provided by GUI)
    Color col;                       // Car  color
    Gate mygate;                     // Gate at startposition


    int speed;                       // Current cars speed
    Pos curpos;                      // Current position 
    Pos newpos;                      // New position to go to
    private boolean isStopped;


    public Car(int no, CarDisplayI cd, Gate g, CarControl ctrl) {
        isStopped = false;
        semBreaks = new Semaphore(0);
        this.no = no;
        this.cd = cd;
        mygate = g;
        this.alley = ctrl.alley;
        this.carDistanceControl=ctrl.distanceControl;
        this.barrier= ctrl.barrier;
        startpos = cd.getStartPos(no);
        barpos = cd.getBarrierPos(no);  // For later use

        col = chooseColor();

        // do not change the special settings for cars no. 0
        if (no == 0) {
            basespeed = 0;
            variation = 0;
            setPriority(Thread.MAX_PRIORITY);
        }
    }

    public synchronized void setSpeed(int speed) {
        if (no != 0 && speed >= 0) {
            basespeed = speed;
        } else
            cd.println("Illegal speed settings");
    }

    public synchronized void setVariation(int var) {
        if (no != 0 && 0 <= var && var <= 100) {
            variation = var;
        } else
            cd.println("Illegal variation settings");
    }

    synchronized int chooseSpeed() {
        double factor = (1.0D + (Math.random() - 0.5D) * 2 * variation / 100);
        return (int) Math.round(factor * basespeed);
    }

    private int speed() {
        // Slow downToUp if requested
        final int slowfactor = 3;
        return speed * (cd.isSlow(curpos) ? slowfactor : 1);
    }

    Color chooseColor() {
        return Color.blue; // You can get any color, as longs as it's blue 
    }

    // it is not allowed to change nextPos
    Pos nextPos(Pos pos) {
        // Get my track from display
        return cd.nextPos(no, pos);
    }

    boolean atGate(Pos pos) {
        return pos.equals(startpos);
    }
    // boolean atAlley(Pos loc){return  loc.equals(alley.southEntry) || loc.equals(alley.northEntry);}
    //boolean atAlleyExit(Pos tryEnter,Pos cur) { return tryEnter.equals(alley.northEntry)? cur.equals(alley.southEntry) : cur.equals(alley.northEntry); }

    public void pushBreaks() {
        try {
            isStopped = true;
            semBreaks.P();
        } catch (InterruptedException e) {
            isStopped = false;
            e.printStackTrace();
        }
    }

    public void releaseBreaks() {
        if (isStopped) {
            isStopped=false;
            semBreaks.V();
        }
    }

    public void run() {
        try {
            isStopped = false;
            speed = chooseSpeed();
            curpos = startpos;
            cd.mark(curpos, col, no);
            boolean inAlley = false;
            int normSpeed = 0;
            Pos carEntered = new Pos(0, 0);

                boolean isEnteredAlley=false;
            while (true) {
                sleep(speed());

                if (atGate(curpos)) {
                    mygate.pass();
                    speed = chooseSpeed();
                }


                newpos = nextPos(curpos);

                carDistanceControl.isNewPostionAvailable(no,newpos);
                //  Move to new position
                cd.clear(curpos);
                cd.mark(curpos, newpos, col, no);
                sleep(speed());
                cd.clear(curpos, newpos);
                cd.mark(newpos, col, no);

                carDistanceControl.notifyPositionChanged(no,curpos);

                curpos = newpos;
                if(!isEnteredAlley)
                    isEnteredAlley=alley.tryEnter(no);
                if(isEnteredAlley)
                    isEnteredAlley = !alley.leave(no);
                this.barrier.sync(no);
            }

        } catch (Exception e) {
            cd.println("Exception in Car no. " + no);
            System.err.println("Exception in Car no. " + no + ":" + e);
            e.printStackTrace();
        }
    }

}

public class CarControl implements CarControlI {

    final CarDisplayI cd;           // Reference to GUI
    final Car[] cars;               // Cars
    final Gate[] gate;              // Gates
    final Alley alley;
    final CarDistanceControl distanceControl;
    final Barrier barrier;


    public CarControl(CarDisplayI cd) {
        final EDirection up = EDirection.upToDown;
        final EDirection down = EDirection.downToUp;
        final EBorder enter = EBorder.enter;
        final EBorder exit = EBorder.exit;
        Border[] alleyBorders = new Border[]{
                //entrances
                new Border(10, 0, up, enter),
                new Border(1, 3, down, enter),
                new Border(2, 3, down, enter),
                //exits
                new Border(0, 2, up, exit),
                new Border(9, 1, down, exit)
        };
        this.cd = cd;
        cars = new Car[9];
        alley = new Alley(cars, alleyBorders);
        distanceControl = new CarDistanceControl(cars,11,12);
        barrier = new Barrier(cars,new Pos(5,3),9);
        gate = new Gate[9];

        for (int no = 0; no < 9; no++) {
            gate[no] = new Gate();
            cars[no] = new Car(no, cd, gate[no], this);
            cars[no].start();
        }
    }

    public void startCar(int no) {
        gate[no].open();
    }

    public void stopCar(int no) {
        gate[no].close();
    }

    public void barrierOn() {        barrier.on();    }

    public void barrierOff() {
        barrier.off();
    }

    public void barrierSet(int k) {

        cd.println("Barrier threshold setting not implemented in this version");
        // This sleep is for illustrating how blocking affects the GUI
        // Remove when feature is properly implemented.
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }
    }

    public void removeCar(int no) {
        cd.println("Remove Car not implemented in this version");
    }

    public void restoreCar(int no) {
        cd.println("Restore Car not implemented in this version");
    }

    /* Speed settings for testing purposes */

    public void setSpeed(int no, int speed) {
        cars[no].setSpeed(speed);
    }

    public void setVariation(int no, int var) {
        cars[no].setVariation(var);
    }

}







