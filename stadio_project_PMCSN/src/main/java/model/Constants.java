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

    // probability of abandon after vip ticket check
    public static final double P6 = 0.1;

    // probability of abandon after vip perquisition
    public static final double P7 = 0.02;

    // ---- SKIPP PROB ----
    // probability of skipping first perquisition
    public static final double P4 = 0.3;

    // probability of skipping second perquisition
    public static final double P5 = 0.5;

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
    public static final double TC_SR = 20;

    // perquisition
    public static final double P_SR = 30;

    // turnstiles
    public static final double T_SR = 5;

    // VIP ticket check
    public static final double V_TC_SR = 20;

    // VIP perquisition
    public static final double V_P_SR = 40;


}
