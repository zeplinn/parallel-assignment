/**
 * Created by JesperRytter on 10/27/2016.
 */
public class CarBumper {

    private Semaphore[] semBumper;

    public CarBumper(int gridSize){
        semBumper = new Semaphore[gridSize];
        for(int i =0; i<=semBumper.length;i++){
            semBumper[i]=new Semaphore(1);
        }
    }
    public void ensureBumpDistance(Pos pos){

    }
}
