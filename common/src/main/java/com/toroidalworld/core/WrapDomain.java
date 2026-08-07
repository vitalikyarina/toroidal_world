package com.toroidalworld.core;

import java.util.List;

// One wrapping axis in one unit: the half-open interval [lowerBound, upperBound) that coordinates fold into. The same
// math serves blocks and chunks — a WorldLoopTransformer holds one instance per axis per unit, all four built from the
// same chunk bounds. The int overloads exist beside the double ones so hot integer paths never round-trip through
// doubles.
public class WrapDomain {
    // Meaningless zeros on a Noop — a disabled axis has no bounds. A question about the shape of the axis — what fits
    // in it, how far a search has to reach to see all of it, what a crossing into another world scales by — goes
    // through one of the semantic answers below, which Noop overrides by meaning; the raw fields are only for code
    // already standing on a wrapping axis.
    public final int lowerBound;
    public final int upperBound;
    public final int domainLength;

    private final int seamRadius;

    // The exact half for the double paths: the int radius truncates on an odd width, and a double delta in the
    // (radius, length/2) band would fold to the longer way round. Integer deltas cannot land in that band, so the
    // int paths keep the cheaper int radius.
    private final double halfLength;

    public WrapDomain(int lowerBound, int upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.domainLength = Math.abs(upperBound - lowerBound);
        this.seamRadius = this.domainLength / 2;
        this.halfLength = this.domainLength / 2.0;
    }

    // Subtract the whole laps between the coordinate and the lower bound in one step. Floating rounding can misjudge
    // the lap count by one right at a bound, so both edges are guarded after the fact: a result still below the world
    // gets a width back, and one that rounds onto the excluded upper bound folds to the lower.
    public double wrap(double coord) {
        if (isOver(coord)) {
            double laps = Math.floor((coord - lowerBound) / domainLength);
            double wrapped = coord - laps * domainLength;
            if (wrapped < lowerBound) {
                wrapped += domainLength;
            }
            return wrapped >= upperBound ? lowerBound : wrapped;
        }
        return coord;
    }

    public int wrap(int coord) {
        return isOver(coord) ? lowerBound + Math.floorMod(coord - lowerBound, domainLength) : coord;
    }

    // The same fold into a window of the caller's own: the copy of a coordinate at or after an anchor, rather than the
    // one inside the world's bounds. A caller that has built a frame reaching past those bounds — a region it walks
    // from its low edge, a span it named itself — needs the single copy that lands inside that frame, and the bounds
    // the world keeps its own coordinates in are not where that copy is. An axis with no seam has one reading of every
    // coordinate and gives back the one it was handed.
    public int wrapFrom(int anchor, int coord) {
        return anchor + Math.floorMod(coord - anchor, domainLength);
    }

    // The nearest copy of a coordinate the caller already knows to be inside the world — the question unwrapAround
    // answers for one any number of laps out, under the name its callers ask it by. The promise buys neither overload
    // anything: the fold counts whole worlds off the difference between the two arguments, and a target inside the
    // world is simply one whose count is small.
    public double unwrap(double anchor, double wrapped) {
        return unwrapAround(anchor, wrapped);
    }

    public int unwrap(int anchor, int wrapped) {
        return unwrapAround(anchor, wrapped);
    }

    // Bring an arbitrary coordinate to the copy of itself nearest the reference: count the whole laps between the two
    // and take them off the coordinate itself.
    //
    // Not rebuilt from a difference — reference plus folded delta — however much that reads like the same value. In
    // IEEE 754 `a + (b - a)` is not `b`: the difference can land in a wider binade than either operand, where it does
    // not fit and is rounded, and adding it back rounds again. The coordinate then comes home about 1e-13 blocks from
    // where it started, which nothing sees as movement and every identity fast path above this one sees as a different
    // value — they compare the answer with == against the argument they passed in.
    public double unwrapAround(double refCoord, double coord) {
        double laps = lapsToward(refCoord, coord);
        return laps == 0.0 ? coord : coord - laps * domainLength;
    }

    public int unwrapAround(int refCoord, int coord) {
        long laps = lapsToward(refCoord, coord);
        return laps == 0 ? coord : (int) (coord - laps * domainLength);
    }

    // How many whole worlds lie between the two, rounded to the nearer copy — and at the antipode, where the two copies
    // are exactly as near, to whichever count is nearer zero, which is the one that leaves the coordinate where it is.
    // Math.round alone always takes the higher, so a tie is stepped back down only where that moves the count toward
    // zero rather than away from it.
    //
    // A tie is thereby settled by the pair alone, the way foldDelta's own strict comparisons settle a delta of exactly
    // half a width. Deciding it on the reference's wrapped image instead — which half of the world it falls in — hands
    // back whichever of the two copies the bounds happen to hold, and the same arrangement in a world whose bounds were
    // drawn a chunk further along folds the other way.
    private double lapsToward(double refCoord, double coord) {
        double quotient = (coord - refCoord) / domainLength;
        double laps = Math.round(quotient);
        if (quotient - laps == -0.5 && laps > 0) {
            return laps - 1;
        }

        return laps;
    }

    // The exact integer form of the same count. A pair already within half a world of each other — every ordinary call
    // — is answered by two comparisons before any division, and the antipode itself lands there, which is where the tie
    // leaves the coordinate untouched. Counted in longs: two coordinates at opposite ends of the int range are a pair
    // like any other, and their difference is not an int.
    private long lapsToward(int refCoord, int coord) {
        long delta = (long) coord - refCoord;
        if (delta >= -seamRadius && delta <= seamRadius) {
            return 0;
        }

        long laps = Math.floorDiv(delta, domainLength);
        long doubledRemainder = 2 * (delta - laps * domainLength);
        if (doubledRemainder > domainLength || (doubledRemainder == domainLength && laps < 0)) {
            return laps + 1;
        }

        return laps;
    }

    // The second reading of a coordinate already brought near a reference: one whole world further on, past the
    // reference rather than short of it. Within a lap a torus offers exactly these two, and where nothing says which
    // one was meant — a client that may be holding either side of the world — this is the one the nearest copy is not.
    // The step from the reference to the coordinate says which way that is. An axis with no seam offers no second copy
    // and answers with the coordinate itself.
    public int otherCopy(int coord, int delta) {
        return delta > 0 ? coord - domainLength : coord + domainLength;
    }

    public boolean isOver(double coord) {
        return coord < lowerBound || coord >= upperBound;
    }

    public boolean isOver(int coord) {
        return coord < lowerBound || coord >= upperBound;
    }

    // How far past the world a coordinate lies, measured from the last coordinate still inside it: 0 in bounds, 1 for
    // the first one beyond. The interval is half-open, so that last coordinate is upperBound - 1 on the high side and
    // lowerBound itself on the low one.
    public int overshoot(int coord) {
        if (coord >= upperBound) {
            return coord - (upperBound - 1);
        }

        if (coord < lowerBound) {
            return lowerBound - coord;
        }

        return 0;
    }

    // Whether a whole stretch lies inside the world, so that folding it would leave it exactly as it came. Not the same
    // question as neither end being over: the interval is half-open, so a stretch ending precisely on the upper bound
    // reads as out by isOver while every point it actually covers is still ground the world holds — its far edge is the
    // boundary plane, not a place.
    //
    // The low end is held to the stricter test for that same reason. A stretch of no width sitting exactly on the upper
    // bound covers one point and that point is outside, so it does have to fold; only a stretch with something in it
    // gets to end there.
    //
    // Asked of the bounds directly rather than of wrap, because the callers that need it are avoiding the work wrap
    // would do. An axis with no seam holds every stretch there is.
    public boolean containsSpan(double min, double max) {
        return min >= lowerBound && min < upperBound && max <= upperBound;
    }

    // Whether plain subtraction tells the truth for every pair inside a stretch this wide: it does as long as no pair
    // is closer the other way round, which holds while twice the stretch fits in the world. The question is about the
    // caller's arithmetic as a whole, not about one particular pair — an axis with no seam answers yes to any width.
    public boolean fitsInHalf(double span) {
        return 2 * span <= domainLength;
    }

    // Whether a stretch this wide is the whole world over again: its two edges then name the same ground, so there is
    // nothing left to clamp against. An axis with no seam has no width for a stretch to cover.
    public boolean coversWorld(double span) {
        return span >= domainLength;
    }

    // Whether a run of this many consecutive coordinates names some place twice once folded. The world holds exactly
    // domainLength of them, so a longer run has to repeat one, and a run of exactly that length is the longest that
    // still names each place once — counted rather than measured, because it is the coordinates that collide, not the
    // ground between them. Asked by callers that would otherwise deduplicate every result they gather, so that they
    // pay for it only where a repeat is possible at all. An axis with no seam never folds anything onto anything.
    public boolean foldsOntoItself(int coordinateCount) {
        return coordinateCount > domainLength;
    }

    // How many steps of a given size it takes to reach every coordinate of the axis, wherever the walk starts. The
    // farthest ground on a closed axis is half a lap away, so that half divided into steps and rounded up arrives
    // everywhere — which is what lets a search spiralling outward stop instead of walking laps over ground it has
    // already seen.
    //
    // An axis that does not close is never covered: there is always more of it further out, so no number of steps is
    // enough and nothing may be skipped for having passed the end. A step of no width never advances and answers the
    // same, rather than dividing by itself.
    public int stepsToCoverTheWorld(int step) {
        if (step <= 0) {
            return Integer.MAX_VALUE;
        }

        int halfWorld = (domainLength + 1) / 2;
        return (halfWorld + step - 1) / step;
    }

    // Whether a stretch named by two coordinates has a shorter reading through the seam. On a torus a pair of endpoints
    // bounds two stretches — the one between them and the one the other way round — and the second is the shorter as
    // soon as the first covers more than half the world. Nothing in the pair says which was meant, which is why the
    // commands that write blocks refuse it rather than choose.
    //
    // Measured in longs against half the world rather than doubling the stretch against the whole of it: the two agree
    // for every length, odd or even, and this way neither the doubling nor Math.abs of the most negative int has a way
    // to turn a very long stretch into a very negative one.
    public boolean spansSeam(int fromCoord, int toCoord) {
        return Math.abs((long) toCoord - fromCoord) > seamRadius;
    }

    // The same pair of endpoints read the other way round: start at the far one and walk to the near one through the
    // seam. The far end then runs past the bounds by construction, which is how the stretch stays single and unbroken —
    // every read and write along it wraps on its way to a chunk. A stretch that is already the shorter one is returned
    // as it came, so an ordinary range keeps its exact coordinates.
    //
    // Two methods rather than one returning a pair: the caller wants a start and an end, and saying so in the name
    // beats handing back an array whose order has to be looked up.
    public int foldSpanStart(int minCoord, int maxCoord) {
        return spansSeam(minCoord, maxCoord) ? maxCoord : minCoord;
    }

    public int foldSpanEnd(int minCoord, int maxCoord) {
        return spansSeam(minCoord, maxCoord) ? minCoord + domainLength : maxCoord;
    }

    // Whether two stretches share a coordinate once the world is allowed to fold. A stretch running past the bounds
    // lies physically against the far edge, so it is offered at every lap, and one lap laying it over the other stretch
    // is enough. What the laps have to bridge is the gap between the two — any shift in [aMin - bMax, aMax - bMin]
    // brings them together — so the question is whether a whole number of world widths lands inside that interval, and
    // the largest one that is not above its top answers it. In longs, where the difference of two far-apart coordinates
    // cannot come back as its own negation.
    public boolean overlaps(int aMin, int aMax, int bMin, int bMax) {
        long lowestShift = (long) aMin - bMax;
        long highestShift = (long) aMax - bMin;
        return Math.floorDiv(highestShift, domainLength) * (long) domainLength >= lowestShift;
    }

    public double deltaFromBounds(double from, double to) {
        return foldDelta(to - wrap(from));
    }

    // The same difference measured the short way: anything longer than half the world is shorter through the seam.
    public double foldDelta(double delta) {
        if (delta > halfLength) {
            return delta - domainLength;
        }

        if (delta < -halfLength) {
            return delta + domainLength;
        }

        return delta;
    }

    public int foldDelta(int delta) {
        if (delta > seamRadius) {
            return delta - domainLength;
        }

        if (delta < -seamRadius) {
            return delta + domainLength;
        }

        return delta;
    }

    // Whether a folded step lands within margin of half the world — the band where the nearest copy of a coordinate
    // is about to flip to the other side. Only that last stretch is suspect: an absolute teleport may legitimately
    // step almost half a world, so anything short of the band is ordinary traffic. An axis with no seam has no
    // antipode to near.
    public boolean nearsAntipode(double delta, double margin) {
        return Math.abs(delta) > halfLength - margin;
    }

    // A coordinate carried in from the matching axis of another world. The two hold the same ground at different
    // sizes — a nether an eighth as wide as its overworld keeps in one chunk what the other spreads over eight — so
    // the coordinate is stretched by the ratio between them and then folded into these bounds, which is the copy of
    // that place this world names.
    public double mapFrom(WrapDomain source, double coord, double declaredScale) {
        return wrap(coord * scaleFrom(source, declaredScale));
    }

    // What that crossing stretches by: the ratio of the two widths, which is the whole truth about the two worlds
    // whatever their dimensions declare a coordinate scale to be — a player who asked for a nether a quarter the width
    // meant a quarter, not the eighth the dimension type still says. The declared scale is what remains where a width
    // is missing: an axis that does not close has none for a ratio to be read from, on either side of the crossing.
    //
    // Asked of the source rather than read off its field, so that a source without a width answers for itself.
    public double scaleFrom(WrapDomain source, double declaredScale) {
        return source.scaleTo(this, declaredScale);
    }

    // The same ratio seen from the far end of the crossing. Only ever reached with a destination that has a width of
    // its own — one without answers scaleFrom itself, without asking.
    protected double scaleTo(WrapDomain destination, double declaredScale) {
        return (double) destination.domainLength / domainLength;
    }

    // The stretch [min, max] as it actually lies in the world: start at the wrapped low edge and walk the stretch's
    // own length; whatever runs off the top continues from the bottom. Anything longer than the world covers it all.
    public List<double[]> spans(double min, double max) {
        double length = Math.min(max - min, domainLength);
        double start = wrap(min);
        double end = start + length;
        if (end <= upperBound) {
            return List.of(new double[] {start, end});
        }

        return List.of(
                new double[] {start, upperBound},
                new double[] {lowerBound, lowerBound + (end - upperBound)});
    }

    public double sqrDistToBounds(double dist) {
        double folded = foldDelta(dist);
        return folded * folded;
    }

    public int sqrDistToBounds(int dist) {
        int folded = foldDelta(dist);
        return folded * folded;
    }

    // The disabled axis: every operation is the identity, every check says "in bounds". It carries no numeric bounds —
    // an axis that does not loop has no width — so the inherited fields hold zeros with no meaning, and each question
    // a caller could have asked of them is answered here by meaning instead: nothing spans the seam, every stretch
    // fits in half the world, none covers the whole of it.
    public static final class Noop extends WrapDomain {
        public Noop() {
            super(0, 0);
        }

        @Override
        public double wrap(double coord) {
            return coord;
        }

        @Override
        public int wrap(int coord) {
            return coord;
        }

        @Override
        public int wrapFrom(int anchor, int coord) {
            return coord;
        }

        @Override
        public double unwrap(double anchor, double wrapped) {
            return wrapped;
        }

        @Override
        public int unwrap(int anchor, int wrapped) {
            return wrapped;
        }

        @Override
        public double unwrapAround(double refCoord, double coord) {
            return coord;
        }

        @Override
        public int unwrapAround(int refCoord, int coord) {
            return coord;
        }

        // One world, one copy: there is nowhere else the same coordinate also is.
        @Override
        public int otherCopy(int coord, int delta) {
            return coord;
        }

        @Override
        public boolean isOver(double coord) {
            return false;
        }

        @Override
        public boolean isOver(int coord) {
            return false;
        }

        @Override
        public int overshoot(int coord) {
            return 0;
        }

        // An axis that does not wrap has no seam for anything to span, whatever the numbers say.
        @Override
        public boolean spansSeam(int fromCoord, int toCoord) {
            return false;
        }

        // No bounds to fall outside of, and the zeros standing in for them would answer this one catastrophically wrong.
        @Override
        public boolean containsSpan(double min, double max) {
            return true;
        }

        @Override
        public boolean fitsInHalf(double span) {
            return true;
        }

        @Override
        public boolean coversWorld(double span) {
            return false;
        }

        // Every coordinate is its own place, however many of them a caller runs through.
        @Override
        public boolean foldsOntoItself(int coordinateCount) {
            return false;
        }

        // Never covered, however far a search walks: an axis with no far side always has more of itself further out.
        @Override
        public int stepsToCoverTheWorld(int step) {
            return Integer.MAX_VALUE;
        }

        // No width on this side of a crossing, so nothing here sets a ratio and what the dimensions declare stands —
        // once as the destination a coordinate arrives at, once as the source it left.
        @Override
        public double scaleFrom(WrapDomain source, double declaredScale) {
            return declaredScale;
        }

        @Override
        protected double scaleTo(WrapDomain destination, double declaredScale) {
            return declaredScale;
        }

        // With no far edge to lay one stretch against, there is only the one reading: they overlap where they overlap.
        @Override
        public boolean overlaps(int aMin, int aMax, int bMin, int bMax) {
            return aMin <= bMax && bMin <= aMax;
        }

        @Override
        public double deltaFromBounds(double from, double to) {
            return to - from;
        }

        @Override
        public double foldDelta(double delta) {
            return delta;
        }

        @Override
        public int foldDelta(int delta) {
            return delta;
        }

        @Override
        public boolean nearsAntipode(double delta, double margin) {
            return false;
        }

        @Override
        public List<double[]> spans(double min, double max) {
            return List.of(new double[] {min, max});
        }

        @Override
        public double sqrDistToBounds(double dist) {
            return dist * dist;
        }

        @Override
        public int sqrDistToBounds(int dist) {
            return dist * dist;
        }
    }
}
