package traminer.util.math;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@SuppressWarnings("serial")
public class Decimal extends BigDecimal {
    private static final RoundingMode ROUNDING_MODE =
            RoundingMode.HALF_UP;
    private static final int SCALE = 10;

    public Decimal(double val) {
        super(val, MathContext.UNLIMITED);
    }

    public Decimal(String val) {
        super(val, MathContext.UNLIMITED);
    }

    public Decimal sum(double val) {
        BigDecimal valBig = BigDecimal
                .valueOf(val)
                .setScale(SCALE, ROUNDING_MODE);
        return new Decimal(super.add(valBig).doubleValue());
    }

    public Decimal sum(Decimal val) {
        return new Decimal(
                super.add(val)
                        .setScale(SCALE, ROUNDING_MODE)
                        .doubleValue());
    }

    public Decimal sub(double val) {
        BigDecimal valBig = BigDecimal
                .valueOf(val)
                .setScale(SCALE, ROUNDING_MODE);
        return new Decimal(super.subtract(valBig).doubleValue());
    }

    public Decimal sub(Decimal val) {
        return new Decimal(
                super.subtract(val)
                        .setScale(SCALE, ROUNDING_MODE)
                        .doubleValue());
    }

    public Decimal divide(double val) {
        BigDecimal valBig = BigDecimal
                .valueOf(val)
                .setScale(SCALE, ROUNDING_MODE);
        return new Decimal(super.divide(valBig, ROUNDING_MODE).doubleValue());
    }

    public Decimal divide(Decimal val) {
        return new Decimal(
                super.divide(val, ROUNDING_MODE)
                        .setScale(SCALE, ROUNDING_MODE)
                        .doubleValue());
    }

    public Decimal multiply(double val) {
        BigDecimal valBig = BigDecimal
                .valueOf(val)
                .setScale(SCALE, ROUNDING_MODE);
        return new Decimal(super.multiply(valBig).doubleValue());
    }

    public Decimal multiply(Decimal val) {
        return new Decimal(
                super.multiply(val)
                        .setScale(SCALE, ROUNDING_MODE)
                        .doubleValue());
    }

    public Decimal min(double val) {
        BigDecimal valBig = BigDecimal
                .valueOf(val)
                .setScale(SCALE, ROUNDING_MODE);
        return new Decimal(valBig.min(this).doubleValue());
    }

    public Decimal min(Decimal val) {
        BigDecimal valBig = val;
        return new Decimal(valBig.min(this).doubleValue());
    }

    public Decimal max(double val) {
        BigDecimal valBig = BigDecimal
                .valueOf(val)
                .setScale(SCALE, ROUNDING_MODE);
        return new Decimal(valBig.max(this).doubleValue());
    }

    public Decimal max(Decimal val) {
        BigDecimal valBig = val;
        return new Decimal(valBig.max(this).doubleValue());
    }

    public Decimal sqrt() {
        BigDecimal val = BigDecimal.valueOf(
                Math.sqrt(this.doubleValue()))
                .setScale(SCALE, ROUNDING_MODE);
        return new Decimal(val.doubleValue());
    }

    public Decimal pow2() {
        return this.multiply(this);
    }

    public Decimal incr() {
        return this.sum(1);
    }

    public Decimal decr() {
        return this.sub(1);
    }

    public double value() {
        Double value = BigDecimal
                .valueOf(doubleValue())
                .setScale(SCALE, ROUNDING_MODE)
                .doubleValue();
        return value;
    }
}
