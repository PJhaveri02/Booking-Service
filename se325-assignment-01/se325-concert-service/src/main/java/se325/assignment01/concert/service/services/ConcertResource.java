package se325.assignment01.concert.service.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se325.assignment01.concert.common.dto.*;
import se325.assignment01.concert.common.types.BookingStatus;
import se325.assignment01.concert.service.domain.*;
import se325.assignment01.concert.service.jaxrs.LocalDateTimeParam;
import se325.assignment01.concert.service.mapper.BookingMapper;
import se325.assignment01.concert.service.mapper.ConcertMapper;
import se325.assignment01.concert.service.mapper.PerformerMapper;
import se325.assignment01.concert.service.mapper.SeatMapper;
import se325.assignment01.concert.service.util.TheatreLayout;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.*;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Path("/concert-service")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConcertResource {

    private static final String AUTH_COOKIE = "auth";
    private static final ConcurrentHashMap<LocalDateTime, List<Subscription>> activeSubscriptions = new ConcurrentHashMap<>();
    private static final Logger _logger = LoggerFactory.getLogger(ConcertResource.class);
    private final ExecutorService threadpool = Executors.newCachedThreadPool();
    private static final int MAX_THEATRE_SEATS = 120;

    /**
     * Gets a specific concert
     * @param id Concert ID
     * @return A JSON response. If no Concert is found a 404 HTTP status code is returned
     */
    @GET
    @Path("/concerts/{id}")
    public Response getConcert(@PathParam("id") long id) {
        _logger.info("Retrieving concert with id: " + id);
        EntityManager em = PersistenceManager.instance().createEntityManager();
        Concert concert;

        try {
            em.getTransaction().begin();
            concert = em.find(Concert.class, id, LockModeType.PESSIMISTIC_READ);
            em.getTransaction().commit();

            if (concert == null)
                return Response.status(Response.Status.NOT_FOUND).build();

            ConcertDTO concertDTO = ConcertMapper.domainToDTO(concert);
            return Response.ok(concertDTO).build();
        } finally {
            em.close();
        }
    }

    /**
     * Gets a list of all the Concerts in the database
     * @return A list of ConcertDTO's.
     */
    @GET
    @Path("/concerts")
    public Response getAllConcerts() {
        _logger.info("GETTING ALL CONCERTS IN DATABASE");
        GenericEntity<List<ConcertDTO>> concertList;
        EntityManager em = PersistenceManager.instance().createEntityManager();

        try {
            em.getTransaction().begin();
            TypedQuery<Concert> concertsQuery = em.createQuery("select c from Concert c", Concert.class)
                    .setLockMode(LockModeType.PESSIMISTIC_READ);
            List<Concert> concerts = concertsQuery.getResultList();
            em.getTransaction().commit();

            if (concerts == null || concerts.isEmpty())
                return Response.noContent().build();

            List<ConcertDTO> concertDTOS = new ArrayList<>();
            for (Concert c : concerts) {
                ConcertDTO temp = ConcertMapper.domainToDTO(c);
                concertDTOS.add(temp);
            }

            concertList = new GenericEntity<List<ConcertDTO>>(concertDTOS) {};
            return Response.ok(concertList).build();
        } finally {
            em.close();
        }
    }

    /**
     * Gets a list of a summary of Concerts
     * @return list of ConcertSummartDTO's
     */
    @GET
    @Path("/concerts/summaries")
    public Response getSummaries() {
        GenericEntity<List<ConcertSummaryDTO>> concertList;
        EntityManager em = PersistenceManager.instance().createEntityManager();

        try {
            em.getTransaction().begin();
            TypedQuery<Concert> concertsQuery = em.createQuery("select c from Concert c", Concert.class)
                    .setLockMode(LockModeType.PESSIMISTIC_READ);
            List<Concert> concerts = concertsQuery.getResultList();
            em.getTransaction().commit();

            if (concerts == null || concerts.isEmpty())
                return Response.status(Response.Status.NO_CONTENT).build();

            List<ConcertSummaryDTO> concertSummary = new ArrayList<>();
            for (Concert c : concerts) {
                ConcertDTO temp = ConcertMapper.domainToDTO(c);
                concertSummary.add(new ConcertSummaryDTO(temp.getId(), temp.getTitle(), temp.getImageName()));
            }

            concertList = new GenericEntity<List<ConcertSummaryDTO>>(concertSummary) {};
            return Response.ok(concertList).build();
        } finally {
            em.close();
        }

    }

    /**
     * Gets a performer based on a specific id number. If not found a not found error is returned
     * @param id
     * @return
     */
    @GET
    @Path("/performers/{id}")
    public Response getPerformer(@PathParam("id") long id) {
        _logger.info("Getting Performer with id: " + id);
        EntityManager em = PersistenceManager.instance().createEntityManager();
        Performer performerDomain;

        try {
            em.getTransaction().begin();
            performerDomain = em.find(Performer.class, id, LockModeType.PESSIMISTIC_READ);
            em.getTransaction().commit();

            if (performerDomain == null)
                return Response.status(Response.Status.NOT_FOUND).build();

            PerformerDTO performerDTO = PerformerMapper.domainToDTO(performerDomain);
            return Response.ok(performerDTO).build();
        } finally {
            em.close();
        }
    }

    /**
     * Returns a list of all performers in the database. Returns no content found response if no performers in database
     * @return
     */
    @GET
    @Path("/performers")
    public Response getAllPerformers() {
        EntityManager em = PersistenceManager.instance().createEntityManager();
        GenericEntity<List<PerformerDTO>> performers;

        try {
            em.getTransaction().begin();
            TypedQuery<Performer> performerDomain = em.createQuery("select p from Performer p", Performer.class)
                    .setLockMode(LockModeType.PESSIMISTIC_READ);
            List<Performer> performersDomainList = performerDomain.getResultList();
            em.getTransaction().commit();

            if (performersDomainList == null || performersDomainList.isEmpty())
                return Response.noContent().build();

            List<PerformerDTO> performersList = new ArrayList<>();
            for (Performer p : performersDomainList) {
                PerformerDTO temp = PerformerMapper.domainToDTO(p);
                performersList.add(temp);
            }

            performers = new GenericEntity<List<PerformerDTO>>(performersList) {};
            return Response.ok(performers).build();
        } finally {
            em.close();
        }
    }

    /**
     * Logs in a user based on their username and password
     * If password and/or username is wrong, Unauthorised status is returned
     * @param userDTO
     * @return
     */
    @POST
    @Path("/login")
    public Response login(UserDTO userDTO) {
        User user;
        EntityManager em = PersistenceManager.instance().createEntityManager();

        try {
            em.getTransaction().begin();
            TypedQuery<User> userTypedQuery = em.createQuery("select u from User u where u.username = :username AND u.password" +
                    " = :password", User.class)
                    .setParameter("username", userDTO.getUsername())
                    .setParameter("password", userDTO.getPassword())
                    .setLockMode(LockModeType.OPTIMISTIC);

            user = userTypedQuery.getSingleResult();

            // Set Authentication Token for user
            String token = UUID.randomUUID().toString();
            user.setAuthenticationToken(token);
            em.merge(user);
            em.getTransaction().commit();

            NewCookie cookieNew = new NewCookie(AUTH_COOKIE, token);
            return Response.ok().cookie(cookieNew).build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().commit();
            em.close();
        }
    }

    /**
     * Helper function that determines if user exists with associates cookie
     * @param cookie HTTP cookie that stores user related data when logged in
     * @return
     */
    public static User authoriseUser(Cookie cookie) {

        // If cookie does not exist, the user has not logged in
        if (cookie == null)
            return null;

        EntityManager em = PersistenceManager.instance().createEntityManager();
        User user;
        try {
            em.getTransaction().begin();

            TypedQuery<User> userTypedQuery = em.createQuery("select u from User u where u.authenticationToken = :token", User.class)
                    .setParameter("token", cookie.getValue())
                    .setLockMode(LockModeType.OPTIMISTIC);
            user = userTypedQuery.getSingleResult();
            em.getTransaction().commit();

            return user;
        } catch (NoResultException e) {
            return null;
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().commit();
            em.close();
        }
    }

    /**
     * A helper function that notifies subscribers when seats for a particular concert reaches
     * a certain limit
     * @param date A Date of a specific Concert
     */
    public void subscriptionChecker(LocalDateTime date) {

        // A new thread pool is used so this long-running task does not impact user's experience
        threadpool.submit(() -> {
            EntityManager em = PersistenceManager.instance().createEntityManager();
            try {
                em.getTransaction().begin();
                List<Subscription> currentSubscriptions = activeSubscriptions.get(date);

                // If there are no subscriptions for "this" concert, then no need to notify as there is
                // no-one to notify
                if (currentSubscriptions == null)
                    return;

                // Get Seats for "this" concert that are booked (isBooked = true)
                TypedQuery<Seat> seatTypedQuery = em.createQuery("SELECT s FROM Seat s WHERE s.date = :date" +
                        " AND s.isBooked = :booked", Seat.class)
                        .setParameter("date", date)
                        .setParameter("booked", true);

                double bookedSeats = (seatTypedQuery.getResultList().size() / (double) MAX_THEATRE_SEATS);
                int bookedSeatsPercentage = (int) (bookedSeats * 100);
                List<Subscription> newSubscriptions = new ArrayList<>();

                // Looping through subscriptions and updating them
                for (Subscription s: currentSubscriptions) {
                        // Checking whether limit has been exceeded
                        int limit = s.subscriptionInfo.getPercentageBooked();
                        if (bookedSeatsPercentage >= limit) {
                            int numRemainingSeats = MAX_THEATRE_SEATS - seatTypedQuery.getResultList().size();
                            s.response.resume(new ConcertInfoNotificationDTO(numRemainingSeats));
                        } else {
                            newSubscriptions.add(s);
                        }
                }

                // Remove the current subscriptions and replace them with the updates subscriptions
                activeSubscriptions.remove(date);
                activeSubscriptions.put(date, newSubscriptions);
                em.getTransaction().commit();
            } finally {
                em.close();
            }
        });
    }

    /**
     * Makes a booking for a logged in user. Validates to make sure booking is correct and notifies user
     * @param cookie
     * @param bookingDTO
     * @return
     */
    @POST
    @Path("/bookings")
    public Response createNewBooking(@CookieParam("auth") Cookie cookie, BookingRequestDTO bookingDTO) {
        User user = authoriseUser(cookie);

        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        EntityManager em = PersistenceManager.instance().createEntityManager();

        try {
            em.getTransaction().begin();

            // Validate Concert
            Concert concert = em.find(Concert.class, bookingDTO.getConcertId(), LockModeType.PESSIMISTIC_READ);
            if (concert == null || (!(concert.getDates().contains(bookingDTO.getDate()))))
                return Response.status(Response.Status.BAD_REQUEST).build();
            em.getTransaction().commit();

            // validate seats
            em.getTransaction().begin();
            TypedQuery<Seat> seatTypedQuery = em.createQuery("select s from Seat s where s.label in :label" +
                        " AND s.date = :date AND s.isBooked = :target", Seat.class)
                        .setParameter("label", bookingDTO.getSeatLabels())
                        .setParameter("date", bookingDTO.getDate())
                        .setParameter("target", false)
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE);
            List<Seat> bookingSeats = seatTypedQuery.getResultList();

            if (bookingSeats == null || bookingSeats.isEmpty() || bookingSeats.size() != bookingDTO.getSeatLabels().size())
                return Response.status(Response.Status.FORBIDDEN).build();

            // Mark all the seats as booked as the seats and concerts are valid and merge it to the database
            for (Seat s : bookingSeats) {
                s.setBooked(true);
                em.merge(s);
            }
            em.getTransaction().commit();

            em.getTransaction().begin();
            // Convert List to Set
            Set<Seat> tempSeatSet = new HashSet<>(bookingSeats);
            tempSeatSet.addAll(bookingSeats);

            // As all the requirements have been met, now can process the booking
            Booking finalBooking = new Booking(bookingDTO.getConcertId(), bookingDTO.getDate(), tempSeatSet);
            finalBooking.setUser(user);
            em.persist(finalBooking);
            em.getTransaction().commit();

            // A method that determines whether to notify users about their subscription as the number of
            // available seats has changed because of "this" new booking.
            subscriptionChecker(finalBooking.getDate());

            return Response.created(URI.create("/concert-service/bookings/" + finalBooking.getId())).build();
        } finally {
            // If any transactions are still active then commit it so the pessimistic write locks are released
            if (em.getTransaction().isActive())
                em.getTransaction().commit();
            em.close();
        }
    }

    /**
     * Get All bookings of a specific user that is logged in into the webservice
     * @param cookie
     * @return
     */
    @GET
    @Path("/bookings")
    public Response getAllUserBookings(@CookieParam("auth") Cookie cookie) {
        User user = authoriseUser(cookie);

        // If cookie doesn't exist, then user is not logged in
        if (user == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {
            em.getTransaction().begin();
            TypedQuery<Booking> bookingTypedQuery = em.createQuery("select b from Booking b where b.user.id = :userID", Booking.class)
                    .setParameter("userID", user.getId());
            List<Booking> bookingDomain = bookingTypedQuery.getResultList();
            em.getTransaction().commit();

            // Converting Booking to BookingDTO
            List<BookingDTO> bookings = new ArrayList<>();
            for (Booking b : bookingDomain) {
                BookingDTO temp = BookingMapper.domainToDTO(b);
                bookings.add(temp);
            }

            GenericEntity<List<BookingDTO>> output = new GenericEntity<List<BookingDTO>>(bookings) {};
            return Response.ok(output).build();

        } finally {
            em.close();
        }
    }

    /**
     * Gets a User's booking based on the Booking ID. User must be logged in and cannot access another
     * users booking
     * @param cookie
     * @param id
     * @return
     */
    @GET
    @Path("/bookings/{id}")
    public Response getBooking(@CookieParam("auth") Cookie cookie, @PathParam("id") long id) {
        User user = authoriseUser(cookie);

        // If cookie doesn't exist, then user is not logged in
        if (user == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        EntityManager em = PersistenceManager.instance().createEntityManager();

        try {
            em.getTransaction().begin();
            Booking booking = em.find(Booking.class, id, LockModeType.PESSIMISTIC_READ);
            em.getTransaction().commit();

            if (booking == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            } else if (!(booking.getUser().equals(user))) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            BookingDTO bookingDTO = BookingMapper.domainToDTO(booking);
            return Response.ok(bookingDTO).build();
        } finally {
            em.close();
        }
    }

    /**
     * Get seats for a particular concert based on the time and status of seats required
     * @param dateArg Date of the seats in String format
     * @param seatStatus Whether seat has a status of booked, unbooked, or Any
     * @return
     */
    @GET
    @Path("/seats/{date}")
    public Response getSeat(@PathParam("date") String dateArg, @QueryParam("status") BookingStatus seatStatus) {
        GenericEntity<List<SeatDTO>> seats;
        LocalDateTime date = new LocalDateTimeParam(dateArg).getLocalDateTime();
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {
            List<Seat> seatList;
            em.getTransaction().begin();
            // Get Seats based on its status
            if (seatStatus != BookingStatus.Any) {
                boolean isBooked = seatStatus == BookingStatus.Booked;
                TypedQuery<Seat> seatTypedQuery = em.createQuery("SELECT s FROM Seat s WHERE s.date = :date AND s.isBooked = :isBooked", Seat.class)
                        .setParameter("date", date)
                        .setParameter("isBooked", isBooked)
                        .setLockMode(LockModeType.PESSIMISTIC_READ);
                seatList = seatTypedQuery.getResultList();

            } else {
                TypedQuery<Seat> seatTypedQuery = em.createQuery("SELECT s FROM Seat s WHERE s.date = :date", Seat.class)
                        .setParameter("date", date)
                        .setLockMode(LockModeType.PESSIMISTIC_READ);
                seatList = seatTypedQuery.getResultList();
            }

            // Converting Seat to SeatDTO
            List<SeatDTO> seatDTOList = new ArrayList<>();
            for (Seat seat : seatList) {
                SeatDTO temp = SeatMapper.domainToDTO(seat);
                seatDTOList.add(temp);
            }
            em.getTransaction().commit();

            seats = new GenericEntity<List<SeatDTO>>(seatDTOList) {};
            return Response.ok(seats).build();
        } finally {
            em.close();
        }
    }


    /**
     * User(s) Subscribing to a concert(s) and notifying them once limit is met
     * A 401 error is returned when trying to make a subscription while not authenticated.
     * A 400 error is returned when trying to make a subscription for a nonexistent concert.
     * A 400 error is returned when trying to make a subscription for a nonexistent date.
     * @param response
     * @param concertInfoSubscriptionDTO
     * @param cookie
     */
    @POST
    @Path("/subscribe/concertInfo")
    public void concertSubscription(@Suspended AsyncResponse response, ConcertInfoSubscriptionDTO concertInfoSubscriptionDTO, @CookieParam("auth") Cookie cookie) {
        User user = authoriseUser(cookie);

        // If user is not authenticated then 401 error code is returned
        if (user == null) {
            response.resume(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }

        EntityManager em = PersistenceManager.instance().createEntityManager();

        try {
            em.getTransaction().begin();

            //  Validating that user has provided correct Concert and Correct Date
            Concert concert = em.find(Concert.class, concertInfoSubscriptionDTO.getConcertId());
            em.getTransaction().commit();

            if (concert == null || !concert.getDates().contains(concertInfoSubscriptionDTO.getDate())) {
                response.resume(Response.status(Response.Status.BAD_REQUEST).build());
                return;
            }

            // As the user has meet all the required conditions they can subscribe to the concert
            synchronized (activeSubscriptions) {
                activeSubscriptions.computeIfAbsent(concertInfoSubscriptionDTO.getDate(), k -> new ArrayList<>());
            }
            activeSubscriptions.get(concertInfoSubscriptionDTO.getDate()).add(new Subscription(concertInfoSubscriptionDTO, response));
        } finally {
            em.close();
        }

    }

    /**
     * Inner Subscription class that notifies user(s) when number of seats left for sale reaches a specific
     * limit
     */
    public class Subscription {

        private final ConcertInfoSubscriptionDTO subscriptionInfo;
        private final AsyncResponse response;

        public Subscription(ConcertInfoSubscriptionDTO subscriptionInfo, AsyncResponse response) {
            this.subscriptionInfo = subscriptionInfo;
            this.response = response;
        }
    }
}
