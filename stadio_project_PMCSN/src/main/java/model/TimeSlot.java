package model;

public class TimeSlot {

    private double averagePoisson;
    private double percentage;
    private int dailyArrival;
    private int lowerBound; //tempo in secondi
    private int upperBound; //tempo in secondi

    public TimeSlot( double percentage, int dailyArrival, int lowerBound, int upperBound) {
        this.percentage = percentage;
        this.dailyArrival = dailyArrival;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public double getAveragePoisson() {
        averagePoisson = percentage * dailyArrival;
        return averagePoisson/3600;
    }

    public double getPercentage() {
        return percentage;
    }

    public int getDailyArrival() {
        return dailyArrival;
    }

    public int getLowerBound() {
        return lowerBound;
    }

    public int getUpperBound() {
        return upperBound;
    }
}
