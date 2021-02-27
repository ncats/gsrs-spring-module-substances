package gsrs.module.substance.utils;

import ix.ginas.models.v1.Amount;
import ix.ginas.models.v1.Property;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Configuration
@ConfigurationProperties("ix.gsrs.number.round")
@Data
public class MolWeightCalculatorProperties {
    //ix.gsrs.number.round.mode
    //ix.gsrs.number.round.digits
    private final static double DOUBLE_MATCH_CUTOFF = 0.001;

    private RoundMode mode = RoundMode.DEFAULT;
    private int digits = 3;


    public Property calculateMolWeightProperty(double avg){
        return calculateMolWeightProperty(avg, 0D,0D,0D,0D);
    }
    public Property calculateMolWeightProperty(double avg, double low, double high, double lowLimit, double highLimit){
        Property p= new Property();
        p.setName("MOL_WEIGHT:NUMBER(CALCULATED)");
        p.setType("amount");
        p.setPropertyType("CHEMICAL");

        p.setValue(calculateMolWeightAmount(avg, low, high, lowLimit, highLimit));
        return p;
    }

    public Amount calculateMolWeightAmount(double avg, double low, double high, double lowLimit, double highLimit) {
        Amount amt = new Amount();
        amt.type="ESTIMATED";
        amt.average = mode.round(avg, digits);
        if( Math.abs(low)> DOUBLE_MATCH_CUTOFF ) {
            amt.low = mode.round(avg - low, digits);
        }
        if( Math.abs(lowLimit)> DOUBLE_MATCH_CUTOFF ) {
            amt.lowLimit = mode.round(avg - lowLimit, digits);
        }
        if( Math.abs(high)> DOUBLE_MATCH_CUTOFF ) {
            amt.high = mode.round(avg + high, digits);
        }
        if( Math.abs(highLimit)> DOUBLE_MATCH_CUTOFF ) {
            amt.highLimit = mode.round(avg + highLimit, digits);
        }
        amt.units="Da";
        return amt;
    }

    public enum RoundMode{
        SIGFIGS{
            @Override
            public double round(double num, int n) {
                if(num == 0D) {
                    return 0D;
                }

                final double d = Math.ceil(Math.log10(num < 0 ? -num: num));
                final int power = n - (int) d;

                final double magnitude = Math.pow(10, power);
                final long shifted = Math.round(num*magnitude);
                return shifted/magnitude;
            }
        },
        DEFAULT,
        UP(RoundingMode.UP),
        DOWN(RoundingMode.DOWN)

        ;

        private RoundingMode roundingMode;
        RoundMode(){
            this(RoundingMode.HALF_EVEN);
        }
        RoundMode(RoundingMode mode){
            this.roundingMode = mode;
        }

        public double round(double input, int numPlaces) {
            return new BigDecimal(input)
                    .setScale(numPlaces, roundingMode)
                    .doubleValue();

        }
    }
}
