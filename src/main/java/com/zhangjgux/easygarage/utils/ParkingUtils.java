package com.zhangjgux.easygarage.utils;

import org.springframework.stereotype.Component;

import java.sql.Timestamp;

@Component
public class ParkingUtils {

    public double getCost(Timestamp begin, Timestamp end, double normalPrice, double latePrice) {
        long diff = (end.getTime() - begin.getTime()) / 3600000;
        if (diff <= 5) {
            return diff * normalPrice;
        }
        return (diff - 5) * latePrice + 5 * normalPrice;
    }
}
