package com.nourtech.ecareplus.shared.application;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
public final class Empreintes {
    private Empreintes(){}
    public static String sha256(String valeur){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valeur.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    public static Object canonique(Object valeur){
        if(valeur instanceof Map<?,?> map){var trie=new TreeMap<String,Object>();map.forEach((k,v)->trie.put(k.toString(),canonique(v)));return trie;}
        if(valeur instanceof List<?> liste)return liste.stream().map(Empreintes::canonique).toList();
        if(valeur instanceof Number n)return new java.math.BigDecimal(n.toString()).stripTrailingZeros();return valeur;
    }
}
