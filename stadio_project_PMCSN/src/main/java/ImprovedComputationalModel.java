import libraries.Rngs;
import model.TimeSlot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static model.Constants.TC_SR;
import static model.ImprovedEvents.*;

import static model.Constants.*;

/*
IMRPOVED MODEL EVENTS
-TICKET CHECK-
0 = ARRIVO
1-10 = SERVIZIO
11 = ABBANDONO

-TORNELLI-
12 = ARRIVO
13-20 = SERVIZIO

-PERQUISITION-
21 = ARRIVO
22-41 = SERVIZIO
42 =  SKIP
43 = ABBANDONO

-TORNELLO ABBONATO-
44 = ARRIVO
45 = SERVIZIO

-PERQUISIZIONE ABBONATO-
46 = ARRIVO
47-48 = SERVIZIO
49 = ABBANDONO
 */


class ImprovedComputationalModel {

    static double START = 0.0;            /* initial (open the door)        */
    static double STOP = 3 * 3600;        /* terminal (close the door) time */
    static double sarrival = START;

    static List<TimeSlot> slotList = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        /* stream index for the rng */
        int streamIndex = 1;

        /* population counter for every node */
        long numberTicketCheck = 0;
        long numberTurnstiles = 0;
        long numberSubscribedTurnstiles = 0;
        long numberSubscribedPerquisition = 0;
        long classOneTotalPopulation = 0;
        long classTwoTotalPopulation = 0;
        long firstClassJobInQueue = 0;
        long secondClassJobInQueue = 0;

        int e;      /* next event index */
        int s;      /* server index */

        /* processed jobs counter for every node */
        long indexTicketCheck = 0;
        long indexTurnstiles = 0;
        long indexSubscribedTurnstiles = 0;
        long indexSubscribedPerquisition = 0;
        long indexPerquisition = 0;

        /* time integrated number for every node */
        double areaTicketCheck = 0.0;
        double areaTurnstiles = 0.0;
        double areaSubscribedTurnstiles = 0.0;
        double areaPerquisition = 0.0;
        double areaSubscribedPerquisition = 0.0;

        double service;     /* it will contain the service times */

        /* abandons counter for ticket check and perquisitions */
        long abandonsCounterTicketCheck = 0;
        long abandonsCounterPerquisition = 0;
        long abandonsCounterSubscribedPerquisition = 0;

        /* abandons list for ticket check and perquisitions */
        List<Double> abandonsTicketCheck = new ArrayList<>();
        List<Double> abandonsPerquisition = new ArrayList<>();
        List<Double> abandonsSubscribedPerquisition = new ArrayList<>();

        /* skip list for perquisitions */
        List<Double> skipsPerquisition = new ArrayList<>();

        List<Integer> perquisitionPriorityClassService = new ArrayList<>();

        for (int i = 0; i < SERVERS_PERQUISITION; i++) {
            perquisitionPriorityClassService.add(0);
        }

        /* skip counter for perquisitions */
        long skipCounterFirstPerquisition = 0;
        long skipCounterPerquisition = 0;

        /* first completion for every node */
        double ticketCheckFirstCompletion = 0;

        double turnstilesFirstCompletion = 0;
        double subscribedTurnstilesFirstCompletion = 0;
        double subscribedPerquisitionFirstCompletion = 0;
        double perquisitionFirstCompletion = 0;
        ImprovedComputationalModel m = new ImprovedComputationalModel();
        Rngs r = new Rngs();
        r.plantSeeds(0);

        /* time slots initialization */

        for (int f = 0; f < 3; f++) {
            TimeSlot slot = new TimeSlot(PERCENTAGE[f], 12062, 3600 * f, 3600 * (f + 1) - 1);
            slotList.add(slot);
        }

        /* events array initialization */
        MsqEvent[] events = new MsqEvent[
                        ALL_EVENTS_TICKET +
                        ALL_EVENTS_TURNSTILES +
                        ALL_EVENTS_PERQUISITION +
                        ALL_EVENTS_SUBSCRIBED_TURNSTILES +
                        ALL_EVENTS_SUBSCRIBED_PERQUISITION];


        /* sum array initialization (to keep track of services) */
        MsqSum[] sum = new MsqSum[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION +ALL_EVENTS_SUBSCRIBED_TURNSTILES +  ALL_EVENTS_SUBSCRIBED_PERQUISITION];
        for (s = 0; s < ALL_EVENTS_TICKET  + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION +ALL_EVENTS_SUBSCRIBED_TURNSTILES + ALL_EVENTS_SUBSCRIBED_PERQUISITION; s++) {
            events[s] = new MsqEvent();
            sum[s] = new MsqSum();
        }

        /* clock initialization */
        MsqT t = new MsqT();
        t.current = START;

        /* generating the first arrival */
        events[0].t = m.getArrival(r, 11 ,t.current);
        events[0].x = 1;

        /* all other servers are initially idle */
        for (s = 1; s < ALL_EVENTS_TICKET  + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION +ALL_EVENTS_SUBSCRIBED_TURNSTILES + ALL_EVENTS_SUBSCRIBED_PERQUISITION ; s++) {
            events[s].t = START;
            events[s].x = 0;
            sum[s].service = 0.0;
            sum[s].served = 0;
        }



        /* START ITERATION */

        while ((events[0].x != 0) || (numberTicketCheck  + numberTurnstiles + (classOneTotalPopulation + classTwoTotalPopulation) + numberSubscribedTurnstiles +numberSubscribedPerquisition != 0)) {

            /* skip */

            if (!skipsPerquisition.isEmpty()) {
                events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION - 2].t = skipsPerquisition.get(0);
                events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION - 2].x = 1;
            } else {
                events[ALL_EVENTS_TICKET  + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION - 2].x = 0;
            }

            /* abandons */
            if (!abandonsTicketCheck.isEmpty()) {
                events[ALL_EVENTS_TICKET - 1].t = abandonsTicketCheck.get(0);
                events[ALL_EVENTS_TICKET - 1].x = 1;
            } else {
                events[ALL_EVENTS_TICKET - 1].x = 0;
            }



            if (!abandonsPerquisition.isEmpty()) {
                events[ALL_EVENTS_TICKET  + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION - 1].t = t.current;
                events[ALL_EVENTS_TICKET  + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION - 1].x = 1;
            } else {
                events[ALL_EVENTS_TICKET  + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION - 1].x = 0;
            }
            if (!abandonsSubscribedPerquisition.isEmpty()) {
                events[ALL_EVENTS_TICKET  + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION+ ALL_EVENTS_SUBSCRIBED_TURNSTILES+ ALL_EVENTS_SUBSCRIBED_PERQUISITION - 1].t = t.current;
                events[ALL_EVENTS_TICKET  + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION+ ALL_EVENTS_SUBSCRIBED_TURNSTILES+ ALL_EVENTS_SUBSCRIBED_PERQUISITION - 1].x = 1;
            } else {
                events[ALL_EVENTS_TICKET  + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION+ ALL_EVENTS_SUBSCRIBED_TURNSTILES+ ALL_EVENTS_SUBSCRIBED_PERQUISITION - 1].x = 0;
            }


            e = m.nextEvent(events);    /* next event index */
            t.next = events[e].t;       /* next event time */


            /* update integrals */
            areaTicketCheck += (t.next - t.current) * numberTicketCheck;
            areaTurnstiles += (t.next - t.current) * numberTurnstiles;
            areaPerquisition += (t.next - t.current) * (classOneTotalPopulation + classTwoTotalPopulation);
            areaSubscribedTurnstiles += (t.next -t.current) * numberSubscribedTurnstiles;
            areaSubscribedPerquisition += (t.next -t.current) * numberSubscribedPerquisition;

            t.current = t.next;     /* advance the clock */

            if (e == ARRIVAL_EVENT_TICKET - 1) {
                /* ticket check arrival */
                numberTicketCheck++;

                /* generate the next arrival */
                events[ARRIVAL_EVENT_TICKET - 1].t = m.getArrival(r, 22, t.current);
                if (events[ARRIVAL_EVENT_TICKET - 1].t > STOP)
                    events[ARRIVAL_EVENT_TICKET - 1].x = 0;

                /* if there's no queue, put a job on service */
                if (numberTicketCheck <= SERVERS_TICKET) {
                    service = m.getService(r, 33, TC_SR);
                    s = m.findOneTicketCheck(events);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                    events[s].x = 1;
                }

            } else if (e == ALL_EVENTS_TICKET - 1) {
                /* ticket check abandon*/

                abandonsCounterTicketCheck++;
                abandonsTicketCheck.remove(0);

            } else if (e == ALL_EVENTS_TICKET) {
                /* turnstiles arrival  */

                events[ALL_EVENTS_TICKET].x = 0;

                /* generate an abandon with probability P1 */
                boolean abandon = generateAbandon(r, 44, P1);

                if (abandon) {
                    /* an abandon must be generated -> we must add it to the abandons list and schedule it */
                    double abandonTime = t.current + 0.01;
                    abandonsTicketCheck.add(abandonTime);

                } else {
                    /* no abandon -> generate arrival at turnstiles */
                    numberTurnstiles++;
                    if (numberTurnstiles <= SERVERS_TURNSTILES) {
                        service = m.getService(r, 55, T_SR);
                        s = m.findOneTurnstiles(events);
                        sum[s].service += service;
                        sum[s].served++;
                        events[s].t = t.current + service;
                        events[s].x = 1;

                    }
                }
            } else if ((e >= ALL_EVENTS_TICKET+ ARRIVAL_EVENT_TURNSTILES ) && (e < ALL_EVENTS_TICKET + ARRIVAL_EVENT_TURNSTILES + SERVERS_TURNSTILES)) {

                /* turnstile service */
                if (turnstilesFirstCompletion == 0)
                    turnstilesFirstCompletion = t.current;

                indexTurnstiles++;
                numberTurnstiles--;

                /* generate an arrival at perquisition center */
                events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES].t = t.current;
                events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES].x = 1;

                s = e;
                if (numberTurnstiles >= SERVERS_TURNSTILES) {
                    /* there's a job in queue to be processed */
                    service = m.getService(r, 66, T_SR);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                } else {
                    events[s].x = 0;
                }

            } else if (e == ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES) {
                /*  perquisition arrival */

                events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES].x = 0;

                service = m.getService(r, 77, P_SR);
                if (service < 10) {
                    /* first queue */
                    classOneTotalPopulation++;
                } else {
                    /* second queue */
                    classTwoTotalPopulation++;
                }

                if (classOneTotalPopulation + classTwoTotalPopulation <= SERVERS_PERQUISITION) {
                    /* the total node population is below the total number of servers */
                    s = m.findPerquisition(events);

                    /* mark s as service for the first/second queue */
                    // bind server index with job type into the array
                    if (service < 10) {
                        try {
                            // small job ( without bags)
                            perquisitionPriorityClassService.set(s - (ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + 1), 1);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    else {
                        try {
                            // big job
                            perquisitionPriorityClassService.set(s - (ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + 1), 2);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                    events[s].x = 1;
                }else{
                    //UPDATE THE NUMBER OF JOBS IN QUEUE
                    if (service<10)
                        firstClassJobInQueue ++;
                    else secondClassJobInQueue++;
                }


            } else if ((e >= ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ARRIVAL_EVENT_PERQUISIION)
                    && (e < ALL_EVENTS_TICKET  + ALL_EVENTS_TURNSTILES + ARRIVAL_EVENT_PERQUISIION + SERVERS_PERQUISITION)
            ) {
                // Perquisition service

                boolean isFromFirstQueue = (perquisitionPriorityClassService.get(e - (ALL_EVENTS_TICKET  + ALL_EVENTS_TURNSTILES + 1)) == 1);
                boolean isFromSecondQueue = (perquisitionPriorityClassService.get(e - (ALL_EVENTS_TICKET  + ALL_EVENTS_TURNSTILES + 1)) == 2);

                boolean skip = false;

                if (isFromFirstQueue) {
                    skip = generateSkip(r, 88, classOneTotalPopulation - SERVERS_PERQUISITION);
                }
                else if (isFromSecondQueue){
                    skip = generateSkip(r, 99, classTwoTotalPopulation - SERVERS_PERQUISITION);
                }
                else{
                    throw new Exception("Unexpected behaviour");
                }

                //
                if (isFromFirstQueue)
                    classOneTotalPopulation--;
                else
                    classTwoTotalPopulation--;

                // SKIP caused by congestion
                if (skip) {
                    double skipTime = t.current + 0.01;
                    skipsPerquisition.add(skipTime);
                    perquisitionPriorityClassService.set(e - (ALL_EVENTS_TICKET  + ALL_EVENTS_TURNSTILES + 1), 0);

                } else {
                    // Update data of second perquisition queue and generate possible abandon
                    if (perquisitionFirstCompletion == 0)
                        perquisitionFirstCompletion = t.current;

                    // update number o done perquisition
                    indexPerquisition++;

                    /* abandons are generated only if the perquisition has not been skipped */
                    boolean abandon = generateAbandon(r, 110, P3);
                    if (abandon) {
                        double abandonTime = t.current + 0.02;      // 0.02 not to overlap an eventual skip
                        perquisitionPriorityClassService.set(e - (ALL_EVENTS_TICKET  + ALL_EVENTS_TURNSTILES + 1), 0);
                        abandonsPerquisition.add(abandonTime);
                    }
                }

                s = e;

                // Now, there is idle server, so re-generate service time for queued job
                if (firstClassJobInQueue >= 1) {
                    firstClassJobInQueue --;
                    // GENERATE A SERVICE LESS THAN 10 SECONDS
                    do {
                        service = m.getService(r, 121, P_SR);
                    } while (!(service < 10));
                    perquisitionPriorityClassService.set(s - (ALL_EVENTS_TICKET  + ALL_EVENTS_TURNSTILES + 1), 1);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                    events[s].x = 1;
                } else if (secondClassJobInQueue >= 1) {
                    secondClassJobInQueue --;
                    // GENERATE A SERVICE GREATER THEN 10 SECONDS
                    do {
                        service = m.getService(r, 132, P_SR);
                    } while ((service < 10));
                    perquisitionPriorityClassService.set(s - (ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + 1), 2);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                    events[s].x = 1;
                } else
                    events[s].x = 0;


            } else if (e == ALL_EVENTS_TICKET + + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION - 2) {
                // perquisition SKIP
                indexPerquisition++;
                skipCounterPerquisition++;
                skipsPerquisition.remove(0);

            } else if (e == ALL_EVENTS_TICKET  + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION - 1) {
                /*  perquisition  abandon  */
                abandonsCounterPerquisition++;
                abandonsPerquisition.remove(0);

            } else if ( e == ALL_EVENTS_TICKET+ALL_EVENTS_TURNSTILES+ALL_EVENTS_PERQUISITION ){
                // subscriber turnstiles arrival
                events[ALL_EVENTS_TICKET+ALL_EVENTS_TURNSTILES+ALL_EVENTS_PERQUISITION].x = 0;

                /* generate an abandon with probability P1 */
                boolean abandon = generateAbandon(r, 143, P1);

                if (abandon) {
                    /* an abandon must be generated -> we must add it to the abandons list and schedule it */
                    double abandonTime = t.current + 0.01;
                    abandonsTicketCheck.add(abandonTime);

                } else {
                    /* no abandon -> generate arrival at turnstiles */
                    numberSubscribedTurnstiles++;
                    if (numberSubscribedTurnstiles <= SERVERS_SUBSCRIBED_TURNSTILES) {
                        service = m.getService(r, 154, T_SR);
                        s = m.findOneSubscribedTurnstiles(events);
                        sum[s].service += service;
                        sum[s].served++;
                        events[s].t = t.current + service;
                        events[s].x = 1;

                    }
                }

            } else if ( e >= ALL_EVENTS_TICKET+ALL_EVENTS_TURNSTILES+ALL_EVENTS_PERQUISITION+ 1 && e <= ALL_EVENTS_TICKET+ALL_EVENTS_TURNSTILES+ALL_EVENTS_PERQUISITION+ SERVERS_SUBSCRIBED_TURNSTILES){
                /* subscribed turnstile service */
                if (subscribedTurnstilesFirstCompletion == 0)
                    subscribedTurnstilesFirstCompletion = t.current;

                indexSubscribedTurnstiles++;
                numberSubscribedTurnstiles--;

                /* generate an arrival at subscribed perquisition center */
                events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION +ALL_EVENTS_SUBSCRIBED_TURNSTILES].t = t.current;
                events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION +ALL_EVENTS_SUBSCRIBED_TURNSTILES].x = 1;


                s = e;
                if (numberSubscribedTurnstiles >= SERVERS_SUBSCRIBED_TURNSTILES) {
                    /* there's a job in queue to be processed */
                    service = m.getService(r, 165, T_SR);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                } else {
                    events[s].x = 0;
                }

            } else if (e == ALL_EVENTS_TICKET+ALL_EVENTS_TURNSTILES+ALL_EVENTS_PERQUISITION+ALL_EVENTS_SUBSCRIBED_TURNSTILES){
                /* subscribed perquisition arrival */
                events[ALL_EVENTS_TICKET+ALL_EVENTS_TURNSTILES+ALL_EVENTS_PERQUISITION+ALL_EVENTS_SUBSCRIBED_TURNSTILES].x = 0;
                numberSubscribedPerquisition++;
                if (numberSubscribedPerquisition <= SERVERS_SUBSCRIBED_PERQUISITION) {
                    service = m.getService(r, 176, P_SR);
                    s = m.findOneSubscribedPerquisition(events);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                    events[s].x = 1;

                }
            } else if (e >= ALL_EVENTS_TICKET+ALL_EVENTS_TURNSTILES+ALL_EVENTS_PERQUISITION+ALL_EVENTS_SUBSCRIBED_TURNSTILES+1 && e <= ALL_EVENTS_TICKET+ALL_EVENTS_TURNSTILES+ALL_EVENTS_PERQUISITION+ALL_EVENTS_SUBSCRIBED_TURNSTILES+SERVERS_SUBSCRIBED_PERQUISITION) {
                /* subscribed perquisition service */
                if (subscribedPerquisitionFirstCompletion == 0)
                    subscribedPerquisitionFirstCompletion = t.current;

                indexSubscribedPerquisition++;
                numberSubscribedPerquisition--;
                /* abandons are generated only if the perquisition has not been skipped */
                boolean abandon = generateAbandon(r, 187, P3);
                if (abandon) {
                    double abandonTime = t.current + 0.02;      // 0.02 not to overlap an eventual skip
                    abandonsSubscribedPerquisition.add(abandonTime);
                }
                /* if there's queue, put a job in queue on service on this server */
                s = e;
                if (numberSubscribedPerquisition >= SERVERS_SUBSCRIBED_PERQUISITION) {
                    service = m.getService(r, 198, P_SR);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                } else {
                    /* if there's no queue, deactivate this server */
                    events[s].x = 0;
                }
            } else if (e == ALL_EVENTS_TICKET+ALL_EVENTS_TURNSTILES+ALL_EVENTS_PERQUISITION+ALL_EVENTS_SUBSCRIBED_TURNSTILES+ALL_EVENTS_SUBSCRIBED_PERQUISITION-1) {
                /* SUBSCRIBED PERQUISITION ABANDON */
                abandonsCounterSubscribedPerquisition++;
                abandonsSubscribedPerquisition.remove(0);
            } else {
                /* ticket check service */
                if (ticketCheckFirstCompletion == 0)
                    ticketCheckFirstCompletion = t.current;

                indexTicketCheck++;
                numberTicketCheck--;

                /*Check if is subscribed*/
                if(isSubscribed(r, 209, P7)) {
                    // generate arrival to dedicated queue
                    events[ALL_EVENTS_TICKET+ALL_EVENTS_TURNSTILES+ALL_EVENTS_PERQUISITION].t= t.current;
                    events[ALL_EVENTS_TICKET+ALL_EVENTS_TURNSTILES+ALL_EVENTS_PERQUISITION].x= 1;
                }else{
                    /* generate an arrival at the turnstiles */
                    events[ALL_EVENTS_TICKET].t = t.current;
                    events[ALL_EVENTS_TICKET].x = 1;
                    }

                /* if there's queue, put a job in queue on service on this server */
                s = e;
                if (numberTicketCheck >= SERVERS_TICKET) {
                    service = m.getService(r, 220, TC_SR);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                } else {
                    /* if there's no queue, deactivate this server */
                    events[s].x = 0;
                }

            }


        }

        System.out.println("END OF ITERATION");

        /* the code below prints the simulation's statistics, for every node */

        DecimalFormat f = new DecimalFormat("###0.0000");
        DecimalFormat g = new DecimalFormat("###0.000000");


        /* TICKET CHECK */

        System.out.println("\nfor " + indexTicketCheck + " jobs the ticket check statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(events[ARRIVAL_EVENT_TICKET - 1].t / indexTicketCheck));
        System.out.println("  avg response time .. =   " + f.format(areaTicketCheck / indexTicketCheck));

        double ticketCheckFinalTime = 0;
        double ticketCheckMean = 0;
        for (s = 1; s <= SERVERS_TICKET; s++) {
            ticketCheckMean += events[s].t;
            if (events[s].t > ticketCheckFinalTime)
                ticketCheckFinalTime = events[s].t;
        }

        double ticketCheckActualTime = ticketCheckFinalTime - ticketCheckFirstCompletion;

        System.out.println("  avg # in node ...... =   " + f.format(areaTicketCheck / ticketCheckActualTime));

        System.out.println("# abandons: " + abandonsCounterTicketCheck);

        for (s = 1; s <= SERVERS_TICKET; s++)          /* adjust area to calculate */
            areaTicketCheck -= sum[s].service;                 /* averages for the queue   */

        System.out.println("  avg delay .......... =   " + f.format(areaTicketCheck / indexTicketCheck));
        System.out.println("  avg # in queue ..... =   " + f.format(areaTicketCheck / ticketCheckActualTime));

        double sumUtilizations = 0.0;
        double sumServices = 0.0;
        double sumServed = 0.0;

        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");
        for (s = 1; s <= SERVERS_TICKET; s++) {
            sumUtilizations += sum[s].service / ticketCheckActualTime;
            sumServices += sum[s].service;
            sumServed += sum[s].served;
            System.out.print("       " + (s) + "          " + g.format(sum[s].service / ticketCheckActualTime) + "            ");
            System.out.println(f.format(sum[s].service / sum[s].served) + "         " + g.format(sum[s].served / (double) indexTicketCheck));
        }

        System.out.println("Mean utilization: " + g.format(sumUtilizations / SERVERS_TICKET));
        System.out.println("Avg service time = " + g.format(sumServices / sumServed));

        System.out.println("");

        /* TURNSTILES */
        System.out.println("\nfor " + indexTurnstiles + " jobs the turnstiles statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(events[ALL_EVENTS_TICKET ].t / indexTurnstiles));
        System.out.println("  avg response time ........... =   " + f.format(areaTurnstiles / indexTurnstiles));

        double turnstilesFinalTime = 0;
        double turnstilesMean = 0;
        for (s = ALL_EVENTS_TICKET + ARRIVAL_EVENT_TURNSTILES; s <= ALL_EVENTS_TICKET + SERVERS_TURNSTILES; s++) {
            turnstilesMean += events[s].t;
            if (events[s].t > turnstilesFinalTime)
                turnstilesFinalTime = events[s].t;
        }

        double turnstilesActualTime = turnstilesFinalTime - turnstilesFirstCompletion;

        System.out.println("  avg # in node ...... =   " + f.format(areaTurnstiles / turnstilesActualTime));


        for (s = ALL_EVENTS_TICKET + ARRIVAL_EVENT_TURNSTILES; s <= ALL_EVENTS_TICKET  + SERVERS_TURNSTILES; s++)
            /* adjust area to calculate */
            areaTurnstiles -= sum[s].service;                                                                     /* averages for the queue   */

        System.out.println("  avg delay .......... =   " + f.format(areaTurnstiles / indexTurnstiles));
        System.out.println("  avg # in queue ..... =   " + f.format(areaTurnstiles / indexTurnstiles));

        sumUtilizations = 0.0;
        sumServices = 0.0;
        sumServed = 0.0;

        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");
        for (s = ALL_EVENTS_TICKET  + ARRIVAL_EVENT_TURNSTILES; s <= ALL_EVENTS_TICKET  + SERVERS_TURNSTILES; s++) {
            System.out.print("       " + (s - ALL_EVENTS_TICKET ) + "          " + g.format(sum[s].service / turnstilesActualTime) + "            ");
            System.out.println(f.format(sum[s].service / sum[s].served) + "         " + g.format(sum[s].served / (double) indexTurnstiles));
            sumUtilizations += sum[s].service / turnstilesActualTime;
            sumServices += sum[s].service;
            sumServed += sum[s].served;
        }
        System.out.println("avg utilization = " + g.format(sumUtilizations / SERVERS_TURNSTILES));
        System.out.println("avg service = " + g.format(sumServices / sumServed));

        System.out.println("");

        /* PERQUISITION */

        System.out.println("\nfor " + indexPerquisition + " jobs the second perquisition statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES].t / indexPerquisition));
        System.out.println("  avg response time .. =   " + f.format(areaPerquisition / indexPerquisition));

        double perquisitionFinalTime = 0;
        for (s = ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ARRIVAL_EVENT_PERQUISIION; s <= ALL_EVENTS_TICKET  + ALL_EVENTS_TURNSTILES + SERVERS_PERQUISITION; s++) {
            if (events[s].t > perquisitionFinalTime)
                perquisitionFinalTime = events[s].t;
        }

        double perquisitionActualTime = perquisitionFinalTime - perquisitionFirstCompletion;

        System.out.println("  avg # in node ...... =   " + f.format(areaPerquisition / perquisitionActualTime));

        System.out.println("# abandons: " + abandonsCounterPerquisition);
        System.out.println("# skips: " + skipCounterPerquisition);

        for (s = ALL_EVENTS_TICKET  + ALL_EVENTS_TURNSTILES + ARRIVAL_EVENT_PERQUISIION; s <= ALL_EVENTS_TICKET  + ALL_EVENTS_TURNSTILES + SERVERS_PERQUISITION; s++)
            /* adjust area to calculate */
            areaPerquisition -= sum[s].service;                                                                    /* averages for the queue   */

        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");
        for (s = ALL_EVENTS_TICKET  + ALL_EVENTS_TURNSTILES + ARRIVAL_EVENT_PERQUISIION; s <= ALL_EVENTS_TICKET  + ALL_EVENTS_TURNSTILES + SERVERS_PERQUISITION; s++) {
            System.out.print("       " + (s - ALL_EVENTS_TICKET  - ALL_EVENTS_TURNSTILES) + "          " + g.format(sum[s].service / perquisitionActualTime) + "            ");
            System.out.println(f.format(sum[s].service / sum[s].served) + "         " + g.format(sum[s].served / (double) indexPerquisition));
        }

        System.out.println("");

        /* SUBSCRIBED TURNSTILES */
        System.out.println("\nfor " + indexSubscribedTurnstiles + " jobs the subscribed turnstiles statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(events[ALL_EVENTS_TICKET+ALL_EVENTS_TURNSTILES+ALL_EVENTS_PERQUISITION ].t / indexSubscribedTurnstiles));
        System.out.println("  avg response time ........... =   " + f.format(areaSubscribedTurnstiles / indexSubscribedTurnstiles));

        double subscribedTurnstilesFinalTime = 0;
        double subscribedTurnstilesMean = 0;
        for (s = ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION +ARRIVAL_EVENT_SUBSCRIBED_TURNSTILES; s <= ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + SERVERS_SUBSCRIBED_TURNSTILES; s++) {
            subscribedTurnstilesMean += events[s].t;
            if (events[s].t > subscribedTurnstilesFinalTime)
                subscribedTurnstilesFinalTime = events[s].t;
        }

        double subscribedTurnstilesActualTime = subscribedTurnstilesFinalTime - subscribedTurnstilesFirstCompletion;

        System.out.println("  avg # in node ...... =   " + f.format(areaSubscribedTurnstiles / subscribedTurnstilesActualTime));


        for (s = ALL_EVENTS_TICKET+ ALL_EVENTS_TURNSTILES+ ALL_EVENTS_PERQUISITION + ARRIVAL_EVENT_SUBSCRIBED_TURNSTILES; s <= ALL_EVENTS_TICKET+ ALL_EVENTS_TURNSTILES+ ALL_EVENTS_PERQUISITION + SERVERS_SUBSCRIBED_TURNSTILES; s++)
            /* adjust area to calculate */
            areaSubscribedTurnstiles -= sum[s].service;                                                                     /* averages for the queue   */

        System.out.println("  avg delay .......... =   " + f.format(areaSubscribedTurnstiles / indexSubscribedTurnstiles));
        System.out.println("  avg # in queue ..... =   " + f.format(areaSubscribedTurnstiles / indexSubscribedTurnstiles));

        sumUtilizations = 0.0;
        sumServices = 0.0;
        sumServed = 0.0;

        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");
        for (s = ALL_EVENTS_TICKET+ ALL_EVENTS_TURNSTILES+ ALL_EVENTS_PERQUISITION + ARRIVAL_EVENT_SUBSCRIBED_TURNSTILES; s <= ALL_EVENTS_TICKET+ ALL_EVENTS_TURNSTILES+ ALL_EVENTS_PERQUISITION + SERVERS_SUBSCRIBED_TURNSTILES; s++) {
            System.out.print("       " + (s - ALL_EVENTS_TICKET - ALL_EVENTS_TURNSTILES - ALL_EVENTS_PERQUISITION ) + "          " + g.format(sum[s].service / subscribedTurnstilesActualTime) + "            ");
            System.out.println(f.format(sum[s].service / sum[s].served) + "         " + g.format(sum[s].served / (double) indexSubscribedTurnstiles));
            sumUtilizations += sum[s].service / subscribedTurnstilesActualTime;
            sumServices += sum[s].service;
            sumServed += sum[s].served;
        }
        System.out.println("avg utilization = " + g.format(sumUtilizations / SERVERS_SUBSCRIBED_TURNSTILES));
        System.out.println("avg service = " + g.format(sumServices / sumServed));

        System.out.println("");

        /* SUBSCRIBED PERQUISITION */

        System.out.println("\nfor " + indexSubscribedPerquisition + " jobs the subscribed perquisition statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES+ ALL_EVENTS_PERQUISITION+ ALL_EVENTS_SUBSCRIBED_TURNSTILES].t / indexSubscribedPerquisition));
        System.out.println("  avg response time .. =   " + f.format(areaSubscribedPerquisition / indexSubscribedPerquisition));

        double subscribedPerquisitionFinalTime = 0;
        for (s = ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES+ ALL_EVENTS_PERQUISITION+ ALL_EVENTS_SUBSCRIBED_TURNSTILES+ ARRIVAL_EVENT_SUBSCRIBED_PERQUISIION; s <= ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES+ ALL_EVENTS_PERQUISITION+ ALL_EVENTS_SUBSCRIBED_TURNSTILES+ SERVERS_SUBSCRIBED_PERQUISITION; s++) {
            if (events[s].t > subscribedPerquisitionFinalTime)
                subscribedPerquisitionFinalTime = events[s].t;
        }

        double subscribedPerquisitionActualTime = subscribedPerquisitionFinalTime - subscribedPerquisitionFirstCompletion;

        System.out.println("  avg # in node ...... =   " + f.format(areaSubscribedPerquisition / subscribedPerquisitionActualTime));

        System.out.println("# abandons: " + abandonsCounterSubscribedPerquisition);

        for (s = ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES+ ALL_EVENTS_PERQUISITION+ ALL_EVENTS_SUBSCRIBED_TURNSTILES+ ARRIVAL_EVENT_SUBSCRIBED_PERQUISIION; s <= ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES+ ALL_EVENTS_PERQUISITION+ ALL_EVENTS_SUBSCRIBED_TURNSTILES+ SERVERS_SUBSCRIBED_PERQUISITION; s++)
            /* adjust area to calculate */
            areaSubscribedPerquisition -= sum[s].service;                                                                    /* averages for the queue   */

        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");
        for (s = ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES+ ALL_EVENTS_PERQUISITION+ ALL_EVENTS_SUBSCRIBED_TURNSTILES+ ARRIVAL_EVENT_SUBSCRIBED_PERQUISIION; s <= ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES+ ALL_EVENTS_PERQUISITION+ ALL_EVENTS_SUBSCRIBED_TURNSTILES+ SERVERS_SUBSCRIBED_PERQUISITION; s++) {
            System.out.print("       " + (s - ALL_EVENTS_TICKET  - ALL_EVENTS_TURNSTILES - ALL_EVENTS_PERQUISITION - ALL_EVENTS_SUBSCRIBED_TURNSTILES) + "          " + g.format(sum[s].service / subscribedPerquisitionActualTime) + "            ");
            System.out.println(f.format(sum[s].service / sum[s].served) + "         " + g.format(sum[s].served / (double) indexSubscribedPerquisition));
        }

        System.out.println("");

    }


    static boolean generateSkip(Rngs rngs, int streamIndex, long queueSize) {
        rngs.selectStream(1 + streamIndex);
        double percentage = Math.min(0.8, (0.444444 * queueSize - 291.555555)/100);
        return rngs.random() <= percentage;
    }


    // this function generate a true value with (percentage * 100) % probability, oth. false
    static boolean generateAbandon(Rngs rngs, int streamIndex, double percentage) {
        rngs.selectStream(1 + streamIndex);
        return rngs.random() <= percentage;
    }

    static boolean isSubscribed(Rngs rngs, int streamIndex, double percentage) {
        rngs.selectStream(224);
        return rngs.random() <= percentage;
    }

    /* this function returns the available turnstile server idle longest  */
    int findOneTurnstiles(MsqEvent[] events) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;

        int i = ALL_EVENTS_TICKET + ARRIVAL_EVENT_TURNSTILES;

        while (events[i].x == 1)       /* find the index of the first available */
            i++;                        /* (idle) server                         */
        s = i;

        while (i < ALL_EVENTS_TICKET + SERVERS_TURNSTILES) {         /* now, check the others to find which   */
            i++;                                                                                             /* has been idle longest                 */
            if ((events[i].x == 0) && (events[i].t < events[s].t))
                s = i;
        }
        return (s);
    }

    /* this function returns the available subscribed turnstile server idle longest  */
    int findOneSubscribedTurnstiles(MsqEvent[] events) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;

        int i = ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION+1;

        while (events[i].x == 1)       /* find the index of the first available */
            i++;                        /* (idle) server                         */

        s = i;

        while (i < ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION+SERVERS_SUBSCRIBED_TURNSTILES) {         /* now, check the others to find which   */
            i++;                                                                                             /* has been idle longest                 */
            if ((events[i].x == 0) && (events[i].t < events[s].t))
                s = i;
        }
        return (s);
    }
    /* this function returns the available turnstile server idle longest  */
    int findOneSubscribedPerquisition(MsqEvent[] events) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;

        int i = ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION+ALL_EVENTS_SUBSCRIBED_TURNSTILES;

        while (events[i].x == 1)       /* find the index of the first available */
            i++;                        /* (idle) server                         */

        s = i;

        while (i < ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION+ALL_EVENTS_SUBSCRIBED_TURNSTILES+SERVERS_SUBSCRIBED_PERQUISITION) {         /* now, check the others to find which   */
            i++;                                                                                             /* has been idle longest                 */
            if ((events[i].x == 0) && (events[i].t < events[s].t))
                s = i;
        }
        return (s);
    }


    int findOneTicketCheck(MsqEvent[] events) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;

        int i = 1;

        while (events[i].x == 1)       /* find the index of the first available */
            i++;                        /* (idle) server                         */
        s = i;


        while (i < SERVERS_TICKET) {         /* now, check the others to find which   */
            i++;                                     /* has been idle longest                 */
            if ((events[i].x == 0) && (events[i].t < events[s].t))
                s = i;
        }
        return (s);
    }


    int findPerquisition(MsqEvent[] events) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;

        int i = ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ARRIVAL_EVENT_PERQUISIION;

        while (events[i].x == 1)       /* find the index of the first available */
            i++;                      /* (idle) server                         */
        s = i;
        while (i < ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + SERVERS_PERQUISITION) {         /* now, check the others to find which   */
            i++;                                             /* has been idle longest                 */
            if ((events[i].x == 0) && (events[i].t < events[s].t))
                s = i;
        }
        return (s);
    }

    double exponential(double m, Rngs r) {
        /* ---------------------------------------------------
         * generate an Exponential random variate, use m > 0.0
         * ---------------------------------------------------
         */
        return (-m * Math.log(1.0 - r.random()));
    }

    double getArrival(Rngs r, int streamIndex, double currentTime) {
        /* --------------------------------------------------------------
         * generate the next arrival time, exponential with rate given by the current time slot
         * --------------------------------------------------------------
         */
        r.selectStream(1 + streamIndex);

        int index = TimeSlotController.timeSlotSwitch(slotList, currentTime);

        sarrival += exponential(1 / (slotList.get(index).getAveragePoisson() / 3600), r);

        return (sarrival);
    }

    double getService(Rngs r, int streamIndex, double serviceTime) {
        r.selectStream(1 + streamIndex);
        return (exponential(serviceTime, r));
    }

    int nextEvent(MsqEvent[] event) {
        /* ---------------------------------------
         * return the index of the next event type
         * ---------------------------------------
         */
        int e;
        int i = 0;

        while (event[i].x == 0)       /* find the index of the first 'active' */
            i++;                        /* element in the event list            */
        e = i;
        while (i < ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES+ ALL_EVENTS_SUBSCRIBED_PERQUISITION- 1) {         /* now, check the others to find which  */
            i++;                        /* event type is most imminent          */
            if ((event[i].x == 1) && (event[i].t < event[e].t))
                e = i;
        }
        return (e);
    }


}



