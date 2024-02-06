package com.arlias.quarkus_crudify.util;

import com.arlias.quarkus_crudify.exception.CustomException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class Utils {

    public static <T, U extends Comparable<? super U>> Comparator<T> comparing(Function<? super T, ? extends U> keyExtractor) {
        return (t1, t2) -> {

            if(t1 == null)
                return 1;
            else if(t2 == null)
                return  -1;

            U u1 = keyExtractor.apply(t1);
            U u2 = keyExtractor.apply(t2);

            if(u1 == null)
                return 1;
            else if(u2 == null)
                return  -1;

             return u1.compareTo(u2);
        };
    }

    public static  <T> T firstOrNull(List<T> list){
        if(list == null || list.size() == 0){
            return null;
        } else {
            return list.get(0);
        }
    }


    public static String hash(String data) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            CustomException.get(CustomException.ErrorCode.INTERNAL, e).boom();
        }
        md.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] digest = md.digest();
        return new String(digest, StandardCharsets.UTF_8);
    }


}
