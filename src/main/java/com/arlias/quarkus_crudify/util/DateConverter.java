package com.arlias.quarkus_crudify.util;

import lombok.extern.slf4j.Slf4j;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.GregorianCalendar;


@Slf4j
public class DateConverter {

    private String pattern = "yyyy-MM-dd'T'HH:mm:ss";

    public DateConverter() {
    }

    public DateConverter(String pattern) {
        this.pattern = pattern;
    }

    public Date toDateOrNull(String date){
        if(date == null)
            return null;
        try {
            return new SimpleDateFormat(pattern).parse(date);
        } catch (ParseException e) {
            log.warn("Impossible to parse date {} to Date.. Returning null.", date);
        }
        return null;
    }

    public String toStringOrNull(Date date){
        if(date == null)
            return null;
        return new SimpleDateFormat(pattern).format(date);
    }

    public static synchronized int getDay(Date date){
        if(date == null)
            return -1;
        return Integer.parseInt(new SimpleDateFormat("dd").format(date));
    }

    public static synchronized int getMonth(Date date){
        if(date == null)
            return -1;
        return Integer.parseInt(new SimpleDateFormat("MM").format(date));
    }

    public static synchronized String getDayMonthKey(Date date){
        if(date == null)
            return null;
        return new SimpleDateFormat("dd-MM").format(date);
    }


    public static XMLGregorianCalendar getGregorianCalendar(Date date){
        if(date == null)
            return null;
        GregorianCalendar gregory = new GregorianCalendar();
        gregory.setTime(date);

        try {
            return DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(
                            gregory);
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int placeNullToMinCompareTo(Date date, Date anotherDate) {
        long thisTime = (date == null) ? 0 : date.getTime();
        long anotherTime = (anotherDate == null) ? 0 : anotherDate.getTime();
        return thisTime < anotherTime ? -1 : (thisTime == anotherTime ? 0 : 1);
    }

    public static int placeNullToMaxCompareTo(Date date, Date anotherDate) {
        long thisTime = (date == null) ? Long.MAX_VALUE : date.getTime();
        long anotherTime = (anotherDate == null) ? Long.MAX_VALUE : anotherDate.getTime();
        return thisTime < anotherTime ? -1 : (thisTime == anotherTime ? 0 : 1);
    }

    public static boolean isBeforeOrEqual(Date before, Date after){
        if (before == null || after == null) {
            return false;
        }
        return !convertToLocalDate(before).isAfter(convertToLocalDate(after));
    }

    public static LocalDate convertToLocalDate(Date dateToConvert) {
        return new java.sql.Date(dateToConvert.getTime()).toLocalDate();
    }

}
