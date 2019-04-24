package algorithm.mapinference.trajectoryclustering.pcurves.Optimize;

final public class Optimizer {
    private static final double GOLD = 1.618034;
    private static final double GLIMIT = 100;
    private static final double TINY = 1.0e-20;
    private Optimizable objectToOptimize;

    public Optimizer(Optimizable in_objectToOptimize) {
        objectToOptimize = in_objectToOptimize;
    }

    // Golden Line Search from Numerical Recipies, p400
    final public double Optimize(double relativeChangeInCriterionThreshold, double stepSize) {

        double ulim, dum, u, r, q, fu, denom, last;

        // Initial bracketing
        double a = last = 0;
        double fa = objectToOptimize.GetCriterion();
        last = a; // = 0
        double b = a + stepSize;
        objectToOptimize.OptimizingStep(b - last);
        last = b;
        double fb = objectToOptimize.GetCriterion();
        double c, fc;
        if (fb > fa) {
            dum = a;
            a = b;
            b = dum;
            dum = fa;
            fa = fb;
            fb = dum;
            objectToOptimize.OptimizingStep(b - last);
            last = b;
        }
        c = b + GOLD * (b - a);
        objectToOptimize.OptimizingStep(c - last);
        last = c;
        fc = objectToOptimize.GetCriterion();
        while (fb > fc) {
            r = (b - a) * (fb - fc);
            q = (b - c) * (fb - fa);
            if (Math.abs(q - r) > TINY)
                denom = q - r;
            else {
                if (q - r > 0)
                    denom = TINY;
                else
                    denom = -TINY;
            }
            u = b - ((b - c) * q - (b - a) * r) / (2.0 * denom);
            ulim = b + GLIMIT * (c - b);
            if ((b - u) * (u - c) > 0.0) {
                objectToOptimize.OptimizingStep(u - last);
                last = u;
                fu = objectToOptimize.GetCriterion();
                if (fu < fc) {
                    a = b;
                    b = u;
                    fa = fb;
                    fb = fu;
                    break;
                } else if (fu > fb) {
                    c = u;
                    fc = fu;
                    break;
                }
                u = c + GOLD * (c - b);
                objectToOptimize.OptimizingStep(u - last);
                last = u;
                fu = objectToOptimize.GetCriterion();
            } else if ((c - u) * (u - ulim) > 0.0) {
                objectToOptimize.OptimizingStep(u - last);
                last = u;
                fu = objectToOptimize.GetCriterion();
                if (fu < fc) {
                    b = c;
                    c = u;
                    u = c + GOLD * (c - b);
                    fb = fc;
                    fc = fu;
                    objectToOptimize.OptimizingStep(u - last);
                    last = u;
                    fu = objectToOptimize.GetCriterion();
                }
            } else if ((u - ulim) * (ulim - c) >= 0.0) {
                u = ulim;
                objectToOptimize.OptimizingStep(u - last);
                last = u;
                fu = objectToOptimize.GetCriterion();
            } else {
                u = c + GOLD * (c - b);
                objectToOptimize.OptimizingStep(u - last);
                last = u;
                fu = objectToOptimize.GetCriterion();
            }
            a = b;
            b = c;
            c = u;
            fa = fb;
            fb = fc;
            fc = fu;
        }
        // Golden Section Search

        double R = 0.61803399;
        double C = 1.0 - R;
        double tol = 1.0e-10;
        double f1, f2, x0, x1, x2, x3;

        x0 = a;
        x3 = c;
        if (Math.abs(c - b) > Math.abs(b - a)) {
            x1 = b;
            x2 = b + C * (c - b);
        } else {
            x2 = b;
            x1 = b - C * (b - a);
        }
        objectToOptimize.OptimizingStep(x1 - last);
        last = x1;
        f1 = objectToOptimize.GetCriterion();
        objectToOptimize.OptimizingStep(x2 - last);
        last = x2;
        f2 = objectToOptimize.GetCriterion();
        while (Math.abs(x3 - x0) > tol * (Math.abs(x1) + Math.abs(x2))
                && Math.abs((f2 - f1) / f2) > relativeChangeInCriterionThreshold) {
            if (f2 < f1) {
                x0 = x1;
                x1 = x2;
                x2 = R * x1 + C * x3;
                f1 = f2;
                objectToOptimize.OptimizingStep(x2 - last);
                last = x2;
                f2 = objectToOptimize.GetCriterion();
            } else {
                x3 = x2;
                x2 = x1;
                x1 = R * x2 + C * x0;
                f2 = f1;
                objectToOptimize.OptimizingStep(x1 - last);
                last = x1;
                f1 = objectToOptimize.GetCriterion();
            }
        }

        if (f1 < f2) {
            if (last != x1)
                objectToOptimize.OptimizingStep(x1 - last);
            return f1;
        } else {
            if (last != x2)
                objectToOptimize.OptimizingStep(x2 - last);
            return f2;
        }
    }
}
