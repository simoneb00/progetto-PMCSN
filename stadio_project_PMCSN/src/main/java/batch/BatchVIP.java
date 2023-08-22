package batch;

import java.lang.*;
import java.text.*;
import java.util.ArrayList;
import java.util.List;

import libraries.*;
import model.*;

import static batch.Batch.writeFile;
import static model.Constants.*;
import static model.Events.*;


class VIPMsqT {
    double current;                   /* current time                       */
    double next;                      /* next (most imminent) event time    */
}

class VIPMsqSum {                      /* accumulated sums of                */
    double service;                   /*   service times                    */
    long served;                    /*   number served                    */
}

class VIPMsqEvent {                     /* the next-event list    */
    double t;                         /*   next event time      */
    int x;                         /*   event status, 0 or 1 */
}


class BatchVIP {
    static double START = 0.0;            /* initial (open the door)        */
    static double STOP = Double.POSITIVE_INFINITY;        /* terminal (close the door) time todo verification step (put back 3 * 3600)*/
    static double sarrival = START;

    static List<TimeSlot> slotList = new ArrayList<>();

    public static void main(String[] args) {

        /* batch simulation parameters */
        int b = 1024;
        int k = 128;

        double currentBatchStartingTime = 0;
        double currentFirstArrivalTimeTC = 0;
        double currentFirstArrivalTimeP = 0;

        /* batch simulation values */
        List<Double> responseTimesTicketCheck = new ArrayList<>();
        List<Double> delaysTicketCheck = new ArrayList<>();
        List<Double> utilizationsTicketCheck = new ArrayList<>();
        List<Double> avgPopulationsTicketCheck = new ArrayList<>();
        List<Double> interarrivalsTicketCheck = new ArrayList<>();
        List<Double> allAbandonsTicketCheck = new ArrayList<>();
        List<Double> serviceTimesTicketCheck = new ArrayList<>();

        List<Double> responseTimesPerquisition = new ArrayList<>();
        List<Double> delaysPerquisition = new ArrayList<>();
        List<Double> utilizationsPerquisition = new ArrayList<>();
        List<Double> avgPopulationsPerquisition = new ArrayList<>();
        List<Double> interarrivalsPerquisition = new ArrayList<>();
        List<Double> allAbandonsPerquisition = new ArrayList<>();
        List<Double> serviceTimesPerquisition = new ArrayList<>();

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

        BatchVIP m = new BatchVIP();
        Rngs r = new Rngs();
        r.plantSeeds(0);


        for (int f = 0; f < 3; f++) {
            TimeSlot slot = new TimeSlot(PERCENTAGE[f], 922, 3600 * f, 3600 * (f + 1) - 1);
            slotList.add(slot);
        }

        VIPMsqEvent[] events = new VIPMsqEvent[ALL_EVENTS_VIP_TICKET + ALL_EVENTS_VIP_PERQUISITION];
        VIPMsqSum[] sum = new VIPMsqSum[ALL_EVENTS_VIP_TICKET + ALL_EVENTS_VIP_PERQUISITION];
        for (s = 0; s < ALL_EVENTS_VIP_TICKET + ALL_EVENTS_VIP_PERQUISITION; s++) {
            events[s] = new VIPMsqEvent();
            sum[s] = new VIPMsqSum();
        }

        VIPMsqT t = new VIPMsqT();
        t.current = START;

        events[0].t = m.getArrival(r, t.current);
        events[0].x = 1;

        for (s = 1; s < ALL_EVENTS_VIP_TICKET + ALL_EVENTS_VIP_PERQUISITION; s++) {
            events[s].t = START;          /* this value is arbitrary because */
            events[s].x = 0;              /* all servers are initially idle  */
            sum[s].service = 0.0;
            sum[s].served = 0;
        }


        int batchCounter = 0;

        while ((events[0].x != 0)) {

            if (indexTicketCheck != 0 && indexTicketCheck % b == 0) {
                /* new batch */
                batchCounter++;

                /* VIP TICKET CHECK */
                responseTimesTicketCheck.add(areaTicketCheck / indexTicketCheck);
                interarrivalsTicketCheck.add((events[ARRIVAL_EVENT_VIP_TICKET - 1].t - currentFirstArrivalTimeTC) / indexTicketCheck);
                allAbandonsTicketCheck.add((double) abandonTicketCheck / b);


                double ticketCheckActualTime = t.current - currentBatchStartingTime;

                avgPopulationsTicketCheck.add(areaTicketCheck / ticketCheckActualTime);

                for (s = 1; s <= DEPARTURE_EVENT_VIP_TICKET; s++)          /* adjust area to calculate */
                    areaTicketCheck -= sum[s].service;                 /* averages for the queue   */

                delaysTicketCheck.add(areaTicketCheck / indexTicketCheck);

                double sumUtilizations = 0.0;
                double sumServices = 0.0;
                double sumServed = 0.0;


                for (s = 1; s <= DEPARTURE_EVENT_VIP_TICKET; s++) {
                    sumUtilizations += sum[s].service / ticketCheckActualTime;
                    sumServices += sum[s].service;
                    sumServed += sum[s].served;
                }

                utilizationsTicketCheck.add(sumUtilizations / DEPARTURE_EVENT_VIP_TICKET);
                serviceTimesTicketCheck.add(sumServices / sumServed);


                areaTicketCheck = 0;
                indexTicketCheck = 0;
                abandonTicketCheck = 0;

                for (s = 1; s <= DEPARTURE_EVENT_VIP_TICKET; s++) {
                    sum[s].served = 0;
                    sum[s].service = 0;
                }


                /* VIP PERQUISITION */

                responseTimesPerquisition.add(areaPerquisition / indexPerquisition);
                interarrivalsPerquisition.add((events[ALL_EVENTS_VIP_TICKET].t - currentFirstArrivalTimeP) / indexPerquisition);
                allAbandonsPerquisition.add((double) abandonPerquisition / b);


                double perquisitionActualTime = t.current - currentBatchStartingTime;

                avgPopulationsPerquisition.add(areaPerquisition / perquisitionActualTime);

                for (s = ALL_EVENTS_VIP_TICKET + 1; s <= ALL_EVENTS_VIP_TICKET + DEPARTURE_EVENT_VIP_PERQUISITION; s++)          /* adjust area to calculate */
                    areaPerquisition -= sum[s].service;                                                                /* averages for the queue   */

                delaysPerquisition.add(areaPerquisition / indexPerquisition);

                sumUtilizations = 0.0;
                sumServices = 0.0;
                sumServed = 0.0;


                for (s = ALL_EVENTS_VIP_TICKET + 1; s <= ALL_EVENTS_VIP_TICKET + DEPARTURE_EVENT_VIP_PERQUISITION; s++) {
                    sumUtilizations += sum[s].service / perquisitionActualTime;
                    sumServices += sum[s].service;
                    sumServed += sum[s].served;
                }

                utilizationsPerquisition.add(sumUtilizations / DEPARTURE_EVENT_VIP_PERQUISITION);
                serviceTimesPerquisition.add(sumServices / sumServed);


                areaPerquisition = 0;
                indexPerquisition = 0;
                abandonPerquisition = 0;

                for (s = ALL_EVENTS_VIP_TICKET + 1; s <= ALL_EVENTS_VIP_TICKET + DEPARTURE_EVENT_VIP_PERQUISITION; s++) {
                    sum[s].served = 0;
                    sum[s].service = 0;
                }


                /* final updates */

                currentFirstArrivalTimeTC = events[ARRIVAL_EVENT_VIP_TICKET - 1].t;
                currentFirstArrivalTimeP = events[ALL_EVENTS_VIP_TICKET].t;
                currentBatchStartingTime = t.current;
            }

            if (batchCounter == k)
                break;

            if (!abandonsTicket.isEmpty()) {
                events[DEPARTURE_EVENT_VIP_TICKET + ABANDON_EVENT_VIP_TICKET].t = abandonsTicket.get(0);
                events[DEPARTURE_EVENT_VIP_TICKET + ABANDON_EVENT_VIP_TICKET].x = 1;     // activate abandon
            } else {
                events[DEPARTURE_EVENT_VIP_TICKET + ABANDON_EVENT_VIP_TICKET].x = 0;     // deactivate abandon
            }

            if (!abandonsPerquisition.isEmpty()){
                events[ALL_EVENTS_VIP_TICKET + DEPARTURE_EVENT_VIP_PERQUISITION + ABANDON_EVENT_VIP_PERQUISITION].t = abandonsPerquisition.get(0);  //TODO migliora
                events[ALL_EVENTS_VIP_TICKET + DEPARTURE_EVENT_VIP_PERQUISITION + ABANDON_EVENT_VIP_PERQUISITION].x = 1;
            } else {
                events[ALL_EVENTS_VIP_TICKET + DEPARTURE_EVENT_VIP_PERQUISITION + ABANDON_EVENT_VIP_PERQUISITION].x = 0;
            }

            e = m.nextEvent(events);                                         /* next event index */
            t.next = events[e].t;                                            /* next event time  */
            areaTicketCheck += (t.next - t.current) * numberTicketCheck;    /* update integral  */
            areaPerquisition += (t.next - t.current) * numberPerquisition;  /* update integral  */
            t.current = t.next;                                             /* advance the clock*/

            if (e == ARRIVAL_EVENT_VIP_TICKET - 1) {     /* process an arrival */

                numberTicketCheck++;

                events[0].t = m.getArrival(r, t.current);
                if (events[0].t > STOP)
                    events[0].x = 0;

                if (numberTicketCheck <= DEPARTURE_EVENT_VIP_TICKET) {
                    service = m.getService(r, V_TC_SR);
                    s = m.findOneTicketCheck(events);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                    events[s].x = 1;
                }
            }

            else if (e == DEPARTURE_EVENT_VIP_TICKET + ABANDON_EVENT_VIP_TICKET) {    // process an abandon
                abandonTicketCheck++;
                abandonsTicket.remove(0);
            }

            else if (e == ALL_EVENTS_VIP_TICKET){      /* process a departure (i.e. arrival to vip perquisition) */

                events[ALL_EVENTS_VIP_TICKET].x = 0;

                // generate, with probability P6, an abandon
                boolean abandon = generateAbandon(r, streamIndex, P4);
                if (abandon) {  // add an abandon
                    double abandonTime = t.current + 0.01;  // this will be the next abandon time (it must be small in order to execute the abandon as next event)
                    abandonsTicket.add(abandonTime);
                } else {    // arrival in the next servant (single server queue, perquisition)

                    /* here a new arrival in the perquisition servant is handled */

                    numberPerquisition++;
                    if (numberPerquisition <= DEPARTURE_EVENT_VIP_PERQUISITION) {   // if false, there's queue
                        service = m.getService(r, V_P_SR);
                        s = m.findOnePerquisition(events);
                        sum[s].service += service;
                        sum[s].served++;
                        events[s].t = t.current + service;
                        events[s].x = 1;

                    }
                }
            }

            else if ((ALL_EVENTS_VIP_TICKET + ARRIVAL_EVENT_VIP_PERQUISIION <= e) && (e < ALL_EVENTS_VIP_TICKET + ARRIVAL_EVENT_VIP_PERQUISIION + DEPARTURE_EVENT_VIP_PERQUISITION)) {    // departure from perquisition

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

                    if (numberPerquisition >= DEPARTURE_EVENT_VIP_PERQUISITION) {
                        service = m.getService(r, V_P_SR);
                        sum[s].service += service;
                        sum[s].served++;
                        events[s].t = t.current + service;
                    } else {
                        events[s].x = 0;
                    }
                }
            }

            else if (e == ALL_EVENTS_VIP_TICKET + DEPARTURE_EVENT_VIP_PERQUISITION + ABANDON_EVENT_VIP_PERQUISITION) {
                abandonPerquisition++;
                abandonsPerquisition.remove(0);
            }

            else {
                if (firstCompletionTicketCheck == 0)
                    firstCompletionTicketCheck = t.current;
                indexTicketCheck++;                                     /* departure from ticket check */
                numberTicketCheck--;
                events[ALL_EVENTS_VIP_TICKET].t = t.current;
                events[ALL_EVENTS_VIP_TICKET].x = 1;

                s = e;
                if (numberTicketCheck >= DEPARTURE_EVENT_VIP_TICKET) {     // there are jobs in queue
                    service = m.getService(r, V_TC_SR);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                } else
                    events[s].x = 0;
            }
        }

        DecimalFormat f = new DecimalFormat("###0.00");
        DecimalFormat g = new DecimalFormat("###0.000");

        /* BATCH SIMULATION RESULTS */

        System.out.println("Completed " + batchCounter + " batches");

        /* TICKET CHECK */

        double allRTs = 0;
        for (double rt : responseTimesTicketCheck) {
            allRTs += rt;
        }
        System.out.println("Mean response time for ticket check: " + allRTs / responseTimesTicketCheck.size());

        double allDelays = 0;
        for (double delay : delaysTicketCheck) {
            allDelays += delay;
        }
        System.out.println("Average queueing time for ticket check: " + allDelays / delaysTicketCheck.size());

        double allUtilizations = 0;
        for (double u : utilizationsTicketCheck) {
            allUtilizations += u;
        }
        System.out.println("Avg utilization for ticket check: " + allUtilizations / utilizationsTicketCheck.size());

        double allInterarrivals = 0;
        for (double i : interarrivalsTicketCheck) {
            allInterarrivals += i;
        }
        System.out.println("Avg interarrivals for ticket check: " + allInterarrivals / interarrivalsTicketCheck.size());

        double allAbandons = 0;
        for (double a : allAbandonsTicketCheck) {
            allAbandons += a;
        }
        System.out.println("Avg abandons for ticket check: " + allAbandons / allAbandonsTicketCheck.size());

        double allServiceTimes = 0;
        for (double st : serviceTimesTicketCheck) {
            allServiceTimes += st;
        }
        System.out.println("Avg service time for ticket check: " + allServiceTimes / serviceTimesTicketCheck.size());

        double allPopulations = 0;
        for (double pop : avgPopulationsTicketCheck) {
            allPopulations += pop;
        }
        System.out.println("Avg population for ticket check: " + allPopulations / avgPopulationsTicketCheck.size());


        System.out.println("");


        /* PERQUISITION */

        allRTs = 0;
        for (double rt : responseTimesPerquisition) {
            allRTs += rt;
        }
        System.out.println("Mean response time for perquisition: " + allRTs / responseTimesPerquisition.size());

        allDelays = 0;
        for (double delay : delaysPerquisition) {
            allDelays += delay;
        }
        System.out.println("Average queueing time for perquisition: " + allDelays / delaysPerquisition.size());

        allUtilizations = 0;
        for (double u : utilizationsPerquisition) {
            allUtilizations += u;
        }
        System.out.println("Avg utilization for perquisition: " + allUtilizations / utilizationsPerquisition.size());

        allInterarrivals = 0;
        for (double i : interarrivalsPerquisition) {
            allInterarrivals += i;
        }
        System.out.println("Avg interarrivals for perquisition: " + allInterarrivals / interarrivalsPerquisition.size());

        allAbandons = 0;
        for (double a : allAbandonsPerquisition) {
            allAbandons += a;
        }
        System.out.println("Avg abandons for perquisition: " + allAbandons / allAbandonsPerquisition.size());


        allServiceTimes = 0;
        for (double st : serviceTimesPerquisition) {
            allServiceTimes += st;
        }
        System.out.println("Avg service time for first perquisition: " + allServiceTimes / serviceTimesPerquisition.size());

        allPopulations = 0;
        for (double pop : avgPopulationsPerquisition) {
            allPopulations += pop;
        }
        System.out.println("Avg population for first perquisition: " + allPopulations / avgPopulationsPerquisition.size());


        System.out.println("");


        /* files creation for interval estimation */

        /* TICKET CHECK */
        writeFile(delaysTicketCheck, "delays_vip_ticket_check");
        writeFile(responseTimesTicketCheck, "response_times_vip_ticket_check");
        writeFile(utilizationsTicketCheck, "utilizations_vip_ticket_check");
        writeFile(avgPopulationsTicketCheck, "populations_vip_ticket_check");
        writeFile(interarrivalsTicketCheck, "interarrivals_vip_ticket_check");
        writeFile(allAbandonsTicketCheck, "abandons_vip_ticket_check");
        writeFile(serviceTimesTicketCheck, "service_times_vip_ticket_check");

        /* FIRST PERQUISITION */
        writeFile(delaysPerquisition, "delays_vip_perquisition");
        writeFile(responseTimesPerquisition, "response_times_vip_perquisition");
        writeFile(utilizationsPerquisition, "utilizations_vip_perquisition");
        writeFile(avgPopulationsPerquisition, "populations_vip_perquisition");
        writeFile(interarrivalsPerquisition, "interarrivals_vip_perquisition");
        writeFile(allAbandonsPerquisition, "abandons_vip_perquisition");
        writeFile(serviceTimesPerquisition, "service_times_vip_perquisition");

        /* INTERVAL ESTIMATION */

        Estimate estimate = new Estimate();

        List<String> filenames = List.of("delays_vip_ticket_check", "response_times_vip_ticket_check", "utilizations_vip_ticket_check", "populations_vip_ticket_check", "interarrivals_vip_ticket_check", "abandons_vip_ticket_check", "service_times_vip_ticket_check",
                "delays_vip_perquisition", "response_times_vip_perquisition", "utilizations_vip_perquisition", "populations_vip_perquisition", "interarrivals_vip_perquisition", "abandons_vip_perquisition", "service_times_vip_perquisition"
                );

        for (String filename : filenames) {
            estimate.createInterval(filename);
        }
    }


    // this function generate a true value with (percentage * 100) % probability, oth. false
    static boolean generateAbandon(Rngs rngs, int streamIndex, double percentage) {
        rngs.selectStream(1 + streamIndex);
        return rngs.random() <= percentage;
    }


    double exponential(double m, Rngs r) {
        /* ---------------------------------------------------
         * generate an Exponential random variate, use m > 0.0
         * ---------------------------------------------------
         */
        return (-m * Math.log(1.0 - r.random()));
    }

    double uniform(double a, double b, Rngs r) {
        /* --------------------------------------------
         * generate a Uniform random variate, use a < b
         * --------------------------------------------
         */
        return (a + (b - a) * r.random());
    }

    double getArrival(Rngs r, double currentTime) {
        /* --------------------------------------------------------------
         * generate the next arrival time, exponential with rate given by the current time slot
         * --------------------------------------------------------------
         */
        r.selectStream(0);

        // int index = TimeSlotController.timeSlotSwitch(slotList, currentTime);
        int index = 0;  // todo verification step (forcing the first timeslot)

        sarrival += exponential(1 / (slotList.get(index).getAveragePoisson() / 3600), r);
        return (sarrival);
    }


    double getService(Rngs r, double serviceTime) {
        r.selectStream(3);
        return (exponential(serviceTime, r));
    }

    int nextEvent(VIPMsqEvent[] event) {
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

    int findOneTicketCheck(VIPMsqEvent[] event) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;

        int i = 1;

        while (event[i].x == 1)       /* find the index of the first available */
            i++;                        /* (idle) server                         */
        s = i;
        while (i < DEPARTURE_EVENT_VIP_TICKET) {         /* now, check the others to find which   */
            i++;                                             /* has been idle longest                 */
            if ((event[i].x == 0) && (event[i].t < event[s].t))
                s = i;
        }
        return (s);
    }

    int findOnePerquisition(VIPMsqEvent[] event) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;

        int i = ALL_EVENTS_VIP_TICKET + 1;

        while (event[i].x == 1)       /* find the index of the first available */
            i++;                        /* (idle) server                         */
        s = i;
        while (i < ALL_EVENTS_VIP_TICKET + DEPARTURE_EVENT_VIP_PERQUISITION) {         /* now, check the others to find which   */
            i++;                                             /* has been idle longest                 */
            if ((event[i].x == 0) && (event[i].t < event[s].t))
                s = i;
        }
        return (s);
    }


}

