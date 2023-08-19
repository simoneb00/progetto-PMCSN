package model;

public class Events {
    //---- VIP EVENTS ----
    // Ticket check
    public static final int ARRIVAL_EVENT_VIP_TICKET = 1;
    public static final int DEPARTURE_EVENT_VIP_TICKET = 4; // todo change
    public static final int ABANDON_EVENT_VIP_TICKET = 1;

    // Perquisition
    public static final int ARRIVAL_EVENT_VIP_PERQUISIION = 1;
    public static final int DEPARTURE_EVENT_VIP_PERQUISITION = 8;
    public static final int ABANDON_EVENT_VIP_PERQUISITION = 1;


    //---- NOT VIP EVENTS ----
    // Ticket check
    public static final int ARRIVAL_EVENT_TICKET = 1;
    public static final int DEPARTURE_EVENT_TICKET = 14;
    public static final int ABANDON_EVENT_TICKET = 1;

    // First perquisition
    public static final int ARRIVAL_EVENT_FIRST_PERQUISIION = 1;
    public static final int DEPARTURE_EVENT_FIRST_PERQUISITION = 20;
    public static final int ABANDON_EVENT_FIRST_PERQUISITION = 1;

    // Turnstiles
    public static final int ARRIVAL_EVENT_TURNSTILES = 1;
    public static final int DEPARTURE_EVENT_TURNSTILES = 8;


    // Second perquisition
    public static final int ARRIVAL_EVENT_SECOND_PERQUISIION = 1;
    public static final int DEPARTURE_EVENT_SECOND_PERQUISITION = 5;
    public static final int ABANDON_EVENT_SECOND_PERQUISITION = 1;

    // ---- ALL EVENTS ----
    public static final int ALL_EVENTS_VIP_TICKET = ARRIVAL_EVENT_VIP_TICKET + DEPARTURE_EVENT_VIP_TICKET + ABANDON_EVENT_VIP_TICKET;
    public static final int ALL_EVENTS_VIP_PERQUISITION = ARRIVAL_EVENT_VIP_PERQUISIION + DEPARTURE_EVENT_VIP_PERQUISITION + ABANDON_EVENT_VIP_PERQUISITION;
    public static final int ALL_EVENTS_TICKET = ARRIVAL_EVENT_TICKET + DEPARTURE_EVENT_TICKET + ABANDON_EVENT_TICKET;
    public static final int ALL_EVENTS_FIRST_PERQUISITION = ARRIVAL_EVENT_FIRST_PERQUISIION + DEPARTURE_EVENT_FIRST_PERQUISITION + ABANDON_EVENT_FIRST_PERQUISITION;
    public static final int ALL_EVENTS_TURNSTILES = ARRIVAL_EVENT_TURNSTILES + DEPARTURE_EVENT_TURNSTILES ;
    public static final int ALL_EVENTS_SECOND_PERQUISITION = ARRIVAL_EVENT_SECOND_PERQUISIION + DEPARTURE_EVENT_SECOND_PERQUISITION + ABANDON_EVENT_SECOND_PERQUISITION;


}
