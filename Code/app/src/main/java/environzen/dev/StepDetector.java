package environzen.dev;


import java.util.Observer;

public interface StepDetector {
    void addObserver(Observer o);
    void deleteObserver(Observer o);
    void notifyObservers();
    void start();
    void stop();
}

