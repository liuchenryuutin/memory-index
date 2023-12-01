package org.lccy.lucene.memory.query.funcation;

import org.apache.lucene.search.Explanation;

import java.util.Locale;

public enum CombineFunction {
    MULTIPLY {
        @Override
        public float combine(double queryScore, double funcScore, double maxBoost) {
            return (float) (queryScore * Math.min(funcScore, maxBoost));
        }

        @Override
        public Explanation explain(Explanation queryExpl, Explanation funcExpl, float maxBoost) {
            Explanation boostExpl = Explanation.match(maxBoost, "maxBoost");
            Explanation minExpl = Explanation.match(
                    Math.min(funcExpl.getValue().floatValue(), maxBoost),
                    "min of:",
                    funcExpl, boostExpl);
            return Explanation.match(queryExpl.getValue().floatValue() * minExpl.getValue().floatValue(),
                    "function score, product of:", queryExpl, minExpl);
        }
    },
    REPLACE {
        @Override
        public float combine(double queryScore, double funcScore, double maxBoost) {
            return (float) (Math.min(funcScore, maxBoost));
        }

        @Override
        public Explanation explain(Explanation queryExpl, Explanation funcExpl, float maxBoost) {
            Explanation boostExpl = Explanation.match(maxBoost, "maxBoost");
            return Explanation.match(
                    Math.min(funcExpl.getValue().floatValue(), maxBoost),
                    "min of:",
                    funcExpl, boostExpl);
        }

    },
    SUM {
        @Override
        public float combine(double queryScore, double funcScore, double maxBoost) {
            return (float) (queryScore + Math.min(funcScore, maxBoost));
        }

        @Override
        public Explanation explain(Explanation queryExpl, Explanation funcExpl, float maxBoost) {
            Explanation minExpl = Explanation.match(Math.min(funcExpl.getValue().floatValue(), maxBoost), "min of:",
                    funcExpl, Explanation.match(maxBoost, "maxBoost"));
            return Explanation.match(Math.min(funcExpl.getValue().floatValue(), maxBoost) + queryExpl.getValue().floatValue(), "sum of",
                    queryExpl, minExpl);
        }

    },
    AVG {
        @Override
        public float combine(double queryScore, double funcScore, double maxBoost) {
            return (float) ((Math.min(funcScore, maxBoost) + queryScore) / 2.0);
        }

        @Override
        public Explanation explain(Explanation queryExpl, Explanation funcExpl, float maxBoost) {
            Explanation minExpl = Explanation.match(Math.min(funcExpl.getValue().floatValue(), maxBoost), "min of:",
                    funcExpl, Explanation.match(maxBoost, "maxBoost"));
            return Explanation.match(
                    (float) ((Math.min(funcExpl.getValue().floatValue(), maxBoost) + queryExpl.getValue().floatValue()) / 2.0), "avg of",
                    queryExpl, minExpl);
        }

    },
    MIN {
        @Override
        public float combine(double queryScore, double funcScore, double maxBoost) {
            return (float) (Math.min(queryScore, Math.min(funcScore, maxBoost)));
        }

        @Override
        public Explanation explain(Explanation queryExpl, Explanation funcExpl, float maxBoost) {
            Explanation innerMinExpl = Explanation.match(
                    Math.min(funcExpl.getValue().floatValue(), maxBoost), "min of:",
                    funcExpl, Explanation.match(maxBoost, "maxBoost"));
            return Explanation.match(
                    Math.min(Math.min(funcExpl.getValue().floatValue(), maxBoost), queryExpl.getValue().floatValue()), "min of",
                    queryExpl, innerMinExpl);
        }

    },
    MAX {
        @Override
        public float combine(double queryScore, double funcScore, double maxBoost) {
            return (float) (Math.max(queryScore, Math.min(funcScore, maxBoost)));
        }

        @Override
        public Explanation explain(Explanation queryExpl, Explanation funcExpl, float maxBoost) {
            Explanation innerMinExpl = Explanation.match(
                    Math.min(funcExpl.getValue().floatValue(), maxBoost), "min of:",
                    funcExpl, Explanation.match(maxBoost, "maxBoost"));
            return Explanation.match(
                    Math.max(Math.min(funcExpl.getValue().floatValue(), maxBoost), queryExpl.getValue().floatValue()), "max of:",
                    queryExpl, innerMinExpl);
        }

    };

    public abstract float combine(double queryScore, double funcScore, double maxBoost);

    public abstract Explanation explain(Explanation queryExpl, Explanation funcExpl, float maxBoost);

    public static CombineFunction fromString(String combineFunction) {
        return valueOf(combineFunction.toUpperCase(Locale.ROOT));
    }
}
