package model;

public class Constants {

    // ---- VIP USER PROB ----
    public static final  double q = 0.076438;

    // ---- ABANDON PROB ----
    // probability of abandon after ticket check
    public static final double P1 = 0.08;

    // probability of abandon after first perquisition
    public static final double P2 = 0.02;

    // probability of abandon after second perquisition
    public static final double P3 = 0.02;

    // probability of being subscribed ( abbonato )
    public static final double P7 = 0.1;

    // probability of abandon after subscribed perquisition
    public static final double P8 = 0.02;

    // probability of abandon after vip ticket check
    public static final double P4 = 0.1;

    // probability of abandon after vip perquisition
    public static final double P5 = 0.02;
    // probability of abandon after vip single queue
    public static final double P6 = 0.002; //First probability combined with second probability


    // ---- TIME SLOT PERCENTAGE ----
    public static final double [] PERCENTAGE = { 0.20, 0.70, 0.10};

    // ---- ARRIVAL RATES [req/sec]----
    // first time window rate
    public static final double LAMBDA1 = 0.24044;

    // second time window rate
    public static final double LAMBDA2 = 0.84154;

    // third time window rate
    public static final double LAMBDA3 = 0.12022;

    // ---- SERVICE RATES  [sec] ----
    // ticket check
    public static final double TC_SR = 7;

    // perquisition
    public static final double P_SR = 20;

    // turnstiles
    public static final double T_SR = 5;

    // VIP ticket check
    public static final double V_TC_SR = 7;

    // VIP perquisition
    public static final double V_P_SR = 20;

    // VIP single queue service time
    public static final double VIP_MEAN_SERVICE_TIME = 27;

}
