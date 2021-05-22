package gsrs.module.substance.utils;

public final class SanitizerUtil {

    private SanitizerUtil(){
        //can not instantiate
    }
    public static Integer sanitizeNumber(Integer i, int defaultValue) {
        if(i==null || i.intValue() <0){
            return defaultValue;
        }
        return i;

    }

    public static Double sanitizeCutOff(Double i, double defaultValue) {

        if(i==null || i.intValue() <0){
            return defaultValue;
        }
        return i;
    }
}
