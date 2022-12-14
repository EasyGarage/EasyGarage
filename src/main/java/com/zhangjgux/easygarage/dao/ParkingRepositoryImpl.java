package com.zhangjgux.easygarage.dao;

import com.zhangjgux.easygarage.entity.Parking;
import com.zhangjgux.easygarage.entity.Place;
import com.zhangjgux.easygarage.entity.Reservation;
import com.zhangjgux.easygarage.entity.Vehicle;
import com.zhangjgux.easygarage.service.PlaceService;
import com.zhangjgux.easygarage.service.UserService;
import com.zhangjgux.easygarage.service.VehicleService;
import com.zhangjgux.easygarage.utils.ParkingUtils;
import com.zhangjgux.easygarage.utils.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Repository
public class ParkingRepositoryImpl implements ParkingRepository {

    private EntityManager entityManager;
    private UserService userService;
    private VehicleService vehicleService;
    private PlaceService placeService;

    private ParkingUtils parkingUtils;

    @Autowired
    public ParkingRepositoryImpl(EntityManager entityManager, UserService userService, VehicleService vehicleService, PlaceService placeService, ParkingUtils parkingUtils) {
        this.entityManager = entityManager;
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.placeService = placeService;
        this.parkingUtils = parkingUtils;
    }

    @Override
    public List<Parking> findAll() {
        TypedQuery<Parking> theQuery =
                entityManager.createQuery("SELECT p FROM Parking p WHERE p.userID = :userID", Parking.class);
        theQuery.setParameter("userID", userService.getCurrent());
        return theQuery.getResultList();
    }

    @Override
    public Parking findById(int id) {
        TypedQuery<Parking> theQuery =
                entityManager.createQuery("SELECT p FROM Parking p WHERE p.id = :id AND p.userID = :userID", Parking.class);
        theQuery.setParameter("id", id);
        theQuery.setParameter("userID", userService.getCurrent());
        try {
            return theQuery.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public Parking findByTime(Timestamp begin) {
        List<Parking> pks = findAll();
        for (Parking pk : pks) {
            Timestamp tp = pk.getBegin();
            if (TimeUtils.timeEqual(begin, tp)) return pk;
        }
        return null;
    }

    @Override
    public Parking findReservationById(int id) {
        TypedQuery<Reservation> theQuery =
                entityManager.createQuery("SELECT r FROM Reservation r WHERE r.id = :id AND r.userID = :userID", Reservation.class);
        theQuery.setParameter("id", id);
        theQuery.setParameter("userID", userService.getCurrent());
        try {
            Reservation r = theQuery.getSingleResult();
            TypedQuery<Parking> theQuery2 =
                    entityManager.createQuery("SELECT p FROM Parking p WHERE p.userID = :userID AND p.reservationID = :reservationID", Parking.class);
            theQuery2.setParameter("reservationID", r);
            theQuery2.setParameter("userID", userService.getCurrent());
            try {
                return theQuery2.getSingleResult();
            } catch (NoResultException e2) {
                return null;
            }
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public void save(Map<String, Object> body) {
        if (body.containsKey("create")) {
            Parking p = new Parking();
            p.setId(0);
            p.setBegin(new Timestamp(System.currentTimeMillis()));
            p.setStatus(1);
            p.setUserID(userService.getCurrent());
            Vehicle v = vehicleService.findByName((String) body.get("vehicle_name"));
            if (v == null) return;
            v.setStatus(0);
            entityManager.merge(v);
            p.setVehicleID(v);
            Place place = placeService.findByPosition((int) body.get("floor"), (int) body.get("number"));
            p.setPlaceID(place);
            entityManager.merge(p);
            place.setStatus(2);
            place.setVehicleID(v);
            entityManager.merge(place);
        } else {
            Parking p = findById((int) body.get("id"));
            if (p == null) return;
            Place place = p.getPlaceID();
            place.setStatus(1);
            place.setVehicleID(null);
            entityManager.merge(place);
            Vehicle v = p.getVehicleID();
            v.setStatus(1);
            entityManager.merge(v);
            p.setStatus(2);
            p.setEnd(new Timestamp(System.currentTimeMillis()));
            p.setCost(parkingUtils.getCost(p.getBegin(), p.getEnd(),
                    p.getPlaceID().getNormalPrice(), p.getPlaceID().getLatePrice()));
            entityManager.merge(p);
        }
    }

    @Override
    public void deleteById(int id) {
        Parking parking = findById(id);
        if (parking != null) entityManager.remove(parking);
    }

    @Override
    public void deleteByTime(Timestamp begin) {
        Parking parking = findByTime(begin);
        if (parking != null) entityManager.remove(parking);
    }
}
