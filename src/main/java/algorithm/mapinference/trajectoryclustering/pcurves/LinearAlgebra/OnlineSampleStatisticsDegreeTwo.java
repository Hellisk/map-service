package algorithm.mapinference.trajectoryclustering.pcurves.LinearAlgebra;

public class OnlineSampleStatisticsDegreeTwo extends OnlineSampleStatistics implements Weighted {
    protected Vektor prototypeVektor;
    protected double weight;
    protected Vektor sum;
    protected CovarianceMatrix cov;

    public OnlineSampleStatisticsDegreeTwo(Vektor in_prototypeVektor) {
        prototypeVektor = in_prototypeVektor.DefaultClone();
        Reset();
    }

    @Override
    final public void Reset() {
        weight = 0;
        sum = prototypeVektor.DefaultClone();
        cov = prototypeVektor.CovarianceMatrixClone();
    }

    @Override
    final public void AddPoint(Vektor vektor) {
        double w;
        try {
            w = ((Weighted) vektor).GetWeight();
        } catch (ClassCastException e) {
            w = 1;
        }
        sum.AddEqual(vektor.Mul(w));
        cov.AddEqual(vektor, w);
        weight += w;
    }

    @Override
    final public void DeletePoint(Vektor vektor) {
        double w;
        try {
            w = ((Weighted) vektor).GetWeight();
        } catch (ClassCastException e) {
            w = 1;
        }
        sum.SubEqual(vektor.Mul(w));
        cov.SubEqual(vektor, w);
        weight -= w;
    }

    @Override
    final public double GetWeight() {
        return weight;
    }

    final public Vektor GetSum() {
        return sum;
    }

    final public double GetSumNorm2Squared() {
        return cov.GetNorm2Squared();
    }

    // see /PrincipalCurves/Article/gradient3.0.tex, p.6.
    final public double GetMSETimesWeightFromLine(Vektor c, Vektor a) {
        Vektor A = c.Sub(a);
        double B = A.Norm2Squared();
        double KTimesB = A.Mul(a);
        double K = KTimesB / B;
        double mseTimesWeight = weight * (a.Norm2Squared() - KTimesB * K); // \sum_i=1^n (a^ta - (a^tA)^2/(A^tA))
        mseTimesWeight += sum.Mul(a.Mul(-2)); // \sum_i=1^n -2y_i^ta
        mseTimesWeight += sum.Mul(A.Mul(2 * K)); // \sum_i=1^n 2(y_i^tA)(a^tA))/(A^tA)
        mseTimesWeight += GetSumNorm2Squared(); // \sum_i=1^n y_i^ty_i
        mseTimesWeight -= cov.MulSquared(A) / B; // \sum_i=1^n (y_i^tA)^2/(A^tA)
        return mseTimesWeight;
    }

    // Roweis/Tipping-Bishop algorithm
    final public LineObject FirstPrincipalComponent() {
        Vektor a = sum;
        Vektor c = prototypeVektor;
        double mse = 0;
        double threshold = 0.00001;
        boolean change = true;
        while (change) {
            Vektor A = c.Sub(a);
            Vektor L = A.Div(A.Norm2Squared());
            double K = a.Mul(L);

            double denominator = 0;
            Vektor numerator = prototypeVektor.DefaultClone();

            Vektor S_1 = sum; // \sum_i=1^n y_i
            Vektor S_2 = cov.Mul(L); //  \sum_i=1^n (y_i^tL)y_i
            double S_3 = cov.MulSquared(L); //  \sum_i=1^n (y_i^tL)^2

            double L_S_1 = L.Mul(S_1);
            Vektor num_1 = a.Mul(weight * K * (1 + K) - L_S_1 + S_3 - L_S_1 * 2 * K);
            Vektor num_2 = S_1.Mul(-K);

            denominator += weight * K * K + S_3 - L_S_1 * 2 * K;
            numerator.AddEqual(num_1.Add(num_2).Add(S_2));

            double newMSE = GetMSETimesWeightFromLine(c, a) / weight;
            c = a;
            a = numerator.Div(denominator);
            change = Math.abs(mse / newMSE - 1.0) > threshold;
            mse = newMSE;
        }
        return new LineObject(a, c);
    }
}
