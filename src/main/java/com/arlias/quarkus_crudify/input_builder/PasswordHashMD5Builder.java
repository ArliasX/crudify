package com.arlias.quarkus_crudify.input_builder;


import com.arlias.quarkus_crudify.exception.CustomException;
import com.arlias.quarkus_crudify.input_builder.common.InputBuilder;
import io.quarkus.arc.Unremovable;

import javax.enterprise.context.ApplicationScoped;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@ApplicationScoped
@Unremovable
public class PasswordHashMD5Builder implements InputBuilder<String, String> {
    @Override
    public String build(String s) {
        if(s == null || s.isBlank()){
            CustomException.get(CustomException.ErrorCode.BAD_REQUEST, "Password is empty").boom();
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(s.getBytes());
            return new HexBinaryAdapter().marshal(messageDigest).toLowerCase();
        } catch (Exception e) {
            CustomException.get(CustomException.ErrorCode.BAD_REQUEST, e).boom();
            return null;
        }
    }
}
