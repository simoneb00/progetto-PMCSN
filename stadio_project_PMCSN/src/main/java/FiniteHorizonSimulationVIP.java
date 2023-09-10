import batch.Estimate;
import libraries.Rngs;
import model.TimeSlot;

import java.util.ArrayList;
import java.util.List;

import static batch.Batch.writeFile;
import static model.Constants.*;
import static model.Events.*;

public class FiniteHorizonSimulationVIP {

    static List<Double> responseTimesTC = new ArrayList<>();
    static List<Double> interarrivalsTC = new ArrayList<>();
    static List<Double> delaysTC = new ArrayList<>();
    static List<Double> avgPopulationsTC = new ArrayList<>();
    static List<Double> allAbandonsTC = new ArrayList<>();
    static List<Double> utilizationsTC = new ArrayList<>();
    static List<Double> serviceTimesTC = new ArrayList<>();
    static List<Double> indexesTC = new ArrayList<>();

    static List<Double> responseTimesP = new ArrayList<>();
    static List<Double> interarrivalsP = new ArrayList<>();
    static List<Double> delaysP = new ArrayList<>();
    static List<Double> avgPopulationsP = new ArrayList<>();
    static List<Double> allAbandonsP = new ArrayList<>();
    static List<Double> utilizationsP = new ArrayList<>();
    static List<Double> serviceTimesP = new ArrayList<>();
    static List<Double> indexesP = new ArrayList<>();


    static final double START = 0.0;            /* initial (open the door)        */
    static final double STOP = 3 * 3600;        /* terminal (close the door) time */
    static double sarrival = START;

    static List<TimeSlot> slotList = new ArrayList<>();

    static long simulation(long seed, Rngs r) throws Exception {
        sarrival = START;
        int streamIndex = 1;
        long numberTicketCheck = 0;   /* number in the ticket check node    */
        long numberPerquisition = 0;  /* number in the perquisition node    */
        int e;                      /* next event index                    */
        int s;                      /* server index                        */
        long indexPerquisition = 0;            /* used to count processed jobs in perquisition        */
        long indexTicketCheck = 0;             /* used to count processed jobs in ticket check        */
        double areaTicketCheck = 0.0;           /* time integrated number in the node */
        double areaPerquisition = 0.0;           /* time integrated number in the node */
        double service;
        long abandonTicketCheck = 0;
        long abandonPerquisition = 0;
        List<Double> abandonsTicket = new ArrayList<>();
        List<Double> abandonsPerquisition = new ArrayList<>();
        double firstCompletionTicketCheck = 0;
        double firstCompletionPerquisition = 0;

        r.plantSeeds(seed);


        for (int f = 0; f < 3; f++) {
            TimeSlot slot = new TimeSlot(PERCENTAGE[f], 922, 3600 * f, 3600 * (f + 1) - 1);
            slotList.add(slot);
        }

        VIPMsqEvent[] event = new VIPMsqEvent[ALL_EVENTS_VIP_TICKET + ALL_EVENTS_VIP_PERQUISITION];
        VIPMsqSum[] sum = new VIPMsqSum[ALL_EVENTS_VIP_TICKET + ALL_EVENTS_VIP_PERQUISITION];
        for (s = 0; s < ALL_EVENTS_VIP_TICKET + ALL_EVENTS_VIP_PERQUISITION; s++) {
            event[s] = new VIPMsqEvent();
            sum[s] = new VIPMsqSum();
        }

        VIPMsqT t = new VIPMsqT();
        t.current = START;

        event[0].t = getArrival(r, t.current);
        event[0].x = 1;

        for (s = 1; s < ALL_EVENTS_VIP_TICKET + ALL_EVENTS_VIP_PERQUISITION; s++) {
            event[s].t = START;          /* this value is arbitrary because */
            event[s].x = 0;              /* all servers are initially idle  */
            sum[s].service = 0.0;
            sum[s].served = 0;
        }


        while ((event[0].x != 0) || (numberTicketCheck + numberPerquisition != 0)) {

            if (!abandonsTicket.isEmpty()) {
                event[SERVERS_VIP_TICKET + ABANDON_EVENT_VIP_TICKET].t = abandonsTicket.get(0);
                event[SERVERS_VIP_TICKET + ABANDON_EVENT_VIP_TICKET].x = 1;     // activate abandon
            } else {
                event[SERVERS_VIP_TICKET + ABANDON_EVENT_VIP_TICKET].x = 0;     // deactivate abandon
            }

            if (!abandonsPerquisition.isEmpty()) {
                event[ALL_EVENTS_VIP_TICKET + SERVERS_VIP_PERQUISITION + ABANDON_EVENT_VIP_PERQUISITION].t = abandonsPerquisition.get(0);
                event[ALL_EVENTS_VIP_TICKET + SERVERS_VIP_PERQUISITION + ABANDON_EVENT_VIP_PERQUISITION].x = 1;
            } else {
                event[ALL_EVENTS_VIP_TICKET + SERVERS_VIP_PERQUISITION + ABANDON_EVENT_VIP_PERQUISITION].x = 0;
            }

            e = nextEvent(event);                                         /* next event index */
            t.next = event[e].t;                                            /* next event time  */
            areaTicketCheck += (t.next - t.current) * numberTicketCheck;    /* update integral  */
            areaPerquisition += (t.next - t.current) * numberPerquisition;  /* update integral  */
            t.current = t.next;                                             /* advance the clock*/

            if (e == ARRIVAL_EVENT_VIP_TICKET - 1) {     /* process an arrival */

                numberTicketCheck++;

                event[0].t = getArrival(r, t.current);
                if (event[0].t > STOP)
                    event[0].x = 0;

                if (numberTicketCheck <= SERVERS_VIP_TICKET) {
                    service = getService(r, V_TC_SR);
                    s = findOneTicketCheck(event);
                    sum[s].service += service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                    event[s].x = 1;
                }
            } else if (e == SERVERS_VIP_TICKET + ABANDON_EVENT_VIP_TICKET) {    // process an abandon
                abandonTicketCheck++;
                abandonsTicket.remove(0);
            } else if (e == ALL_EVENTS_VIP_TICKET) {      /* process a departure (i.e. arrival to vip perquisition) */

                event[ALL_EVENTS_VIP_TICKET].x = 0;

                // generate, with probability P6, an abandon
                boolean abandon = generateAbandon(r, streamIndex, P4);
                if (abandon) {  // add an abandon
                    double abandonTime = t.current + 0.01;  // this will be the next abandon time (it must be small in order to execute the abandon as next event)
                    abandonsTicket.add(abandonTime);
                } else {    // arrival in the next servant (single server queue, perquisition)

                    /* here a new arrival in the perquisition servant is handled */

                    numberPerquisition++;
                    if (numberPerquisition <= SERVERS_VIP_PERQUISITION) {   // if false, there's queue
                        service = getService(r, V_P_SR);
                        s = findOnePerquisition(event);
                        sum[s].service += service;
                        sum[s].served++;
                        event[s].t = t.current + service;
                        event[s].x = 1;

                    }
                }
            } else if ((ALL_EVENTS_VIP_TICKET + ARRIVAL_EVENT_VIP_PERQUISIION <= e) && (e < ALL_EVENTS_VIP_TICKET + ARRIVAL_EVENT_VIP_PERQUISIION + SERVERS_VIP_PERQUISITION)) {    // departure from perquisition

                if (firstCompletionPerquisition == 0)
                    firstCompletionPerquisition = t.current;

                boolean abandon = generateAbandon(r, streamIndex, P5);
                if (abandon) {
                    double abandonTime = t.current + 0.01;
                    abandonsPerquisition.add(abandonTime);
                } else {
                    numberPerquisition--;
                    indexPerquisition++;
                    s = e;

                    if (numberPerquisition >= SERVERS_VIP_PERQUISITION) {
                        service = getService(r, V_P_SR);
                        sum[s].service += service;
                        sum[s].served++;
                        event[s].t = t.current + service;
                    } else {
                        event[s].x = 0;
                    }
                }
            } else if (e == ALL_EVENTS_VIP_TICKET + SERVERS_VIP_PERQUISITION + ABANDON_EVENT_VIP_PERQUISITION) {
                abandonPerquisition++;
                abandonsPerquisition.remove(0);
            } else {
                if (firstCompletionTicketCheck == 0)
                    firstCompletionTicketCheck = t.current;
                indexTicketCheck++;                                     /* departure from ticket check */
                numberTicketCheck--;
                event[ALL_EVENTS_VIP_TICKET].t = t.current;
                event[ALL_EVENTS_VIP_TICKET].x = 1;

                s = e;
                if (numberTicketCheck >= SERVERS_VIP_TICKET) {     // there are jobs in queue
                    service = getService(r, V_TC_SR);
                    sum[s].service += service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                } else
                    event[s].x = 0;
            }
        }


        /* SAVE STATISTICS */

        /* TICKET CHECK */
        responseTimesTC.add(areaTicketCheck / indexTicketCheck);
        interarrivalsTC.add((event[0].t) / indexTicketCheck);
        allAbandonsTC.add((double) abandonTicketCheck);

        double finalTimeTC = 0;
        for (s = 1; s <= SERVERS_VIP_TICKET; s++) {
            if (event[s].t > finalTimeTC)
                finalTimeTC = event[s].t;
        }

        double actualTimeTC = finalTimeTC - firstCompletionTicketCheck;

        avgPopulationsTC.add(areaTicketCheck / actualTimeTC);

        for (s = 1; s <= SERVERS_VIP_TICKET; s++)          /* adjust area to calculate */
            areaTicketCheck -= sum[s].service;                 /* averages for the queue   */

        delaysTC.add(areaTicketCheck / indexTicketCheck);

        double sumUtilizations = 0.0;
        double sumServices = 0.0;
        double sumServed = 0.0;


        for (s = 1; s <= SERVERS_VIP_TICKET; s++) {
            sumUtilizations += sum[s].service / actualTimeTC;
            sumServices += sum[s].service;
            sumServed += sum[s].served;
        }

        utilizationsTC.add(sumUtilizations / SERVERS_VIP_TICKET);
        serviceTimesTC.add(sumServices / sumServed);
        indexesTC.add((double)indexTicketCheck);


        /* PERQUISITION */
        responseTimesP.add(areaPerquisition / indexPerquisition);
        interarrivalsP.add((event[ALL_EVENTS_VIP_TICKET].t) / indexPerquisition);
        allAbandonsP.add((double) abandonPerquisition);

        double finalTimeP = 0;
        for (s = ALL_EVENTS_VIP_TICKET + 1; s <= ALL_EVENTS_VIP_TICKET + SERVERS_VIP_PERQUISITION; s++) {
            if (event[s].t > finalTimeP)
                finalTimeP = event[s].t;
        }

        double actualTimeP = finalTimeP - firstCompletionPerquisition;

        avgPopulationsP.add(areaPerquisition / actualTimeP);

        for (s = ALL_EVENTS_VIP_TICKET + 1; s <= ALL_EVENTS_VIP_TICKET + SERVERS_VIP_PERQUISITION; s++) {
            areaPerquisition -= sum[s].service;                 /* averages for the queue   */
        }

        delaysP.add(areaPerquisition / indexPerquisition);

        sumUtilizations = 0.0;
        sumServices = 0.0;
        sumServed = 0.0;


        for (s = ALL_EVENTS_VIP_TICKET + 1; s <= ALL_EVENTS_VIP_TICKET + SERVERS_VIP_PERQUISITION; s++) {
            sumUtilizations += sum[s].service / actualTimeP;
            sumServices += sum[s].service;
            sumServed += sum[s].served;
        }

        utilizationsP.add(sumUtilizations / SERVERS_VIP_PERQUISITION);
        serviceTimesP.add(sumServices / sumServed);
        indexesP.add((double)indexPerquisition);


        r.selectStream(255);
        return r.getSeed();
    }

    static boolean generateAbandon(Rngs rngs, int streamIndex, double percentage) {
        rngs.selectStream(1 + streamIndex);
        return rngs.random() <= percentage;
    }


    static double exponential(double m, Rngs r) {
        /* ---------------------------------------------------
         * generate an Exponential random variate, use m > 0.0
         * ---------------------------------------------------
         */
        return (-m * Math.log(1.0 - r.random()));
    }

    static double getArrival(Rngs r, double currentTime) {
        /* --------------------------------------------------------------
         * generate the next arrival time, exponential with rate given by the current time slot
         * --------------------------------------------------------------
         */
        r.selectStream(0);

        int index = TimeSlotController.timeSlotSwitch(slotList, currentTime);

        sarrival += exponential(1 / (slotList.get(index).getAveragePoisson() / 3600), r);
        return (sarrival);
    }


    static double getService(Rngs r, double serviceTime) {
        r.selectStream(3);
        return (exponential(serviceTime, r));
    }

    static int nextEvent(VIPMsqEvent[] event) {
        /* ---------------------------------------
         * return the index of the next event type
         * ---------------------------------------
         */
        int e;
        int i = 0;

        while (event[i].x == 0)       /* find the index of the first 'active' */
            i++;                        /* element in the event list            */
        e = i;
        while (i < ALL_EVENTS_VIP_TICKET + ALL_EVENTS_VIP_PERQUISITION - 1) {         /* now, check the others to find which  */
            i++;                        /* event type is most imminent          */
            if ((event[i].x == 1) && (event[i].t < event[e].t))
                e = i;
        }
        return (e);
    }

    static int findOneTicketCheck(VIPMsqEvent[] event) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;

        int i = 1;

        while (event[i].x == 1)       /* find the index of the first available */
            i++;                        /* (idle) server                         */
        s = i;
        while (i < SERVERS_VIP_TICKET) {         /* now, check the others to find which   */
            i++;                                             /* has been idle longest                 */
            if ((event[i].x == 0) && (event[i].t < event[s].t))
                s = i;
        }
        return (s);
    }

    static int findOnePerquisition(VIPMsqEvent[] event) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;

        int i = ALL_EVENTS_VIP_TICKET + 1;

        while (event[i].x == 1)       /* find the index of the first available */
            i++;                        /* (idle) server                         */
        s = i;
        while (i < ALL_EVENTS_VIP_TICKET + SERVERS_VIP_PERQUISITION) {         /* now, check the others to find which   */
            i++;                                             /* has been idle longest                 */
            if ((event[i].x == 0) && (event[i].t < event[s].t))
                s = i;
        }
        return (s);
    }

    public static void main(String[] args) throws Exception {
        long[] seeds = new long[1024];
        seeds[0] = 123456789;
        Rngs r = new Rngs();
        for (int i = 0; i < 150; i++) {
            seeds[i + 1] = simulation(seeds[i], r);
            System.out.println(i+1);
        }

        /* files creation for interval estimation */
        String directory = "replication_reports";

        /* TICKET CHECK */
        writeFile(delaysTC, directory, "delays_vip_tc");
        writeFile(responseTimesTC, directory, "response_times_vip_tc");
        writeFile(utilizationsTC, directory, "utilizations_vip_tc");
        writeFile(avgPopulationsTC, directory, "populations_vip_tc");
        writeFile(interarrivalsTC, directory, "interarrivals_vip_tc");
        writeFile(allAbandonsTC, directory, "abandons_vip_tc");
        writeFile(serviceTimesTC, directory, "service_times_vip_tc");
        writeFile(indexesTC, directory, "indexes_vip_tc");

        /* PERQUISITION */
        writeFile(delaysP, directory, "delays_vip_p");
        writeFile(responseTimesP, directory, "response_times_vip_p");
        writeFile(utilizationsP, directory, "utilizations_vip_p");
        writeFile(avgPopulationsP, directory, "populations_vip_p");
        writeFile(interarrivalsP, directory, "interarrivals_vip_p");
        writeFile(allAbandonsP, directory, "abandons_vip_p");
        writeFile(serviceTimesP, directory, "service_times_vip_p");
        writeFile(indexesP, directory, "indexes_vip_p");

        /* INTERVAL ESTIMATION */

        Estimate estimate = new Estimate();

        List<String> filenames = List.of("response_times_vip_tc", "delays_vip_tc", "utilizations_vip_tc", "interarrivals_vip_tc", "abandons_vip_tc", "service_times_vip_tc", "populations_vip_tc", "indexes_vip_tc",
                "response_times_vip_p", "delays_vip_p", "utilizations_vip_p", "interarrivals_vip_p", "abandons_vip_p", "service_times_vip_p", "populations_vip_p", "indexes_vip_p"
                );

        for (String filename : filenames) {
            estimate.createInterval(directory, filename);
        }
    }
}

