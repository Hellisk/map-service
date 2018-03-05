/*
 * Copyright (C) 2015, BMW Car IT GmbH
 *
 * Author: Sebastian Mattheis <sebastian.mattheis@bmw-carit.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.bmwcarit.barefoot.matcher;

import com.bmwcarit.barefoot.road.BaseRoad;
import com.bmwcarit.barefoot.road.RoadReader;
import com.bmwcarit.barefoot.roadmap.*;
import com.bmwcarit.barefoot.spatial.Geography;
import com.bmwcarit.barefoot.spatial.SpatialOperator;
import com.bmwcarit.barefoot.topology.Cost;
import com.bmwcarit.barefoot.topology.Dijkstra;
import com.bmwcarit.barefoot.topology.Router;
import com.bmwcarit.barefoot.util.Quintuple;
import com.bmwcarit.barefoot.util.SourceException;
import com.bmwcarit.barefoot.util.Tuple;
import com.esri.core.geometry.Geometry.Type;
import com.esri.core.geometry.*;
import org.json.JSONException;
import org.junit.Test;

import java.util.*;
import java.util.Map.Entry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MatcherTest {
    private final SpatialOperator spatial = new Geography();
    private final Router<Road, RoadPoint> router = new Dijkstra<>();
    private final Cost<Road> cost = new Time();
    private final RoadMap map = RoadMap.Load(new RoadReader() {
        class Entry extends Quintuple<Long, Long, Long, Boolean, String> {
            private static final long serialVersionUID = 1L;

            public Entry(Long one, Long two, Long three, Boolean four, String five) {
                super(one, two, three, four, five);
            }
        };

        private Set<Entry> entries = new HashSet<>(Arrays.asList(
                new Entry(0L, 0L, 1L, false, "LINESTRING(11.000 48.000, 11.010 48.000)"),
                new Entry(1L, 1L, 2L, false, "LINESTRING(11.010 48.000, 11.020 48.000)"),
                new Entry(2L, 2L, 3L, false, "LINESTRING(11.020 48.000, 11.030 48.000)"),
                new Entry(3L, 1L, 4L, true, "LINESTRING(11.010 48.000, 11.011 47.999)"),
                new Entry(4L, 4L, 5L, true, "LINESTRING(11.011 47.999, 11.021 47.999)"),
                new Entry(5L, 5L, 6L, true, "LINESTRING(11.021 47.999, 11.021 48.010)")));

        private Iterator<BaseRoad> iterator = null;

        private ArrayList<BaseRoad> roads = new ArrayList<>();

        @Override
        public boolean isOpen() {
            return (iterator != null);
        }

        @Override
        public void open() throws SourceException {
            if (roads.isEmpty()) {
                for (Entry entry : entries) {
                    Polyline geometry = (Polyline) GeometryEngine.geometryFromWkt(entry.five(),
                            WktImportFlags.wktImportDefaults, Type.Polyline);
                    roads.add(new BaseRoad(entry.one(), entry.two(), entry.three(), entry.one(),
                            entry.four(), (short) 0, 1.0f, 100.0f, 100.0f,
                            (float) spatial.length(geometry), geometry));
                }
            }

            iterator = roads.iterator();
        }

        @Override
        public void open(Polygon polygon, HashSet<Short> exclusions) throws SourceException {
            open();
        }

        @Override
        public void close() throws SourceException {
            iterator = null;
        }

        @Override
        public BaseRoad next() throws SourceException {
            return iterator.hasNext() ? iterator.next() : null;
        }
    });

    public MatcherTest() {
        map.construct();
    }

    private void assertCandidate(Tuple<MatcherCandidate, Double> candidate, Point sample) {
        Polyline polyline = map.get(candidate.one().point().edge().id()).geometry();
        double f = spatial.intercept(polyline, sample);
        Point i = spatial.interpolate(polyline, f);
        double l = spatial.distance(i, sample);
        double sig2 = Math.pow(5d, 2);
        double sqrt_2pi_sig2 = Math.sqrt(2d * Math.PI * sig2);
        double p = 1 / sqrt_2pi_sig2 * Math.exp((-1) * l * l / (2 * sig2));

        assertEquals(f, candidate.one().point().fraction(), 10E-6);
        assertEquals(p, candidate.two(), 10E-6);
    }

    private void assertTransition(Tuple<MatcherTransition, Double> transition,
            Tuple<MatcherCandidate, MatcherSample> source,
            Tuple<MatcherCandidate, MatcherSample> target, double lambda) {
        List<Road> edges = router.route(source.one().point(), target.one().point(), cost);

        if (edges == null) {
            // fail();
        }

        Route route = new Route(source.one().point(), target.one().point(), edges);

        assertEquals(route.length(), transition.one().route().length(), 10E-6);
        assertEquals(route.source().edge().id(), transition.one().route().source().edge().id());
        assertEquals(route.target().edge().id(), transition.one().route().target().edge().id());

        double beta = lambda == 0 ? (2.0 * (target.two().time() - source.two().time()) / 1000)
                : 1 / lambda;
        double base = 1.0 * spatial.distance(source.two().point(), target.two().point()) / 60;
        double p = (1 / beta)
                * Math.exp((-1.0) * Math.max(0, route.cost(new TimePriority()) - base) / beta);

        assertEquals(transition.two(), p, 10E-6);
    }

    @SuppressWarnings("unused")
    private Set<Long> refset(Point sample, double radius) {
        Set<Long> refset = new HashSet<>();
        Iterator<Road> roads = map.edges();
        while (roads.hasNext()) {
            Road road = roads.next();
            double f = spatial.intercept(road.geometry(), sample);
            Point i = spatial.interpolate(road.geometry(), f);
            double l = spatial.distance(i, sample);

            if (l <= radius) {
                refset.add(road.id());
            }
        }
        return refset;
    }

    @Test
    public void TestCandidates() throws JSONException {
        Matcher filter = new Matcher(map, router, cost, spatial);
        {
            filter.setMaxRadius(100);
            Point sample = new Point(11.001, 48.001);

            Set<Tuple<MatcherCandidate, Double>> candidates = filter
                    .candidates(new HashSet<MatcherCandidate>(), new MatcherSample(0, sample));

            assertEquals(0, candidates.size());
        }
        {
            double radius = 200;
            filter.setMaxRadius(radius);
            Point sample = new Point(11.001, 48.001);

            Set<Tuple<MatcherCandidate, Double>> candidates = filter
                    .candidates(new HashSet<MatcherCandidate>(), new MatcherSample(0, sample));

            Set<Long> refset = new HashSet<>(Arrays.asList(0L, 1L));
            Set<Long> set = new HashSet<>();

            for (Tuple<MatcherCandidate, Double> candidate : candidates) {
                assertTrue(refset.contains(candidate.one().point().edge().id()));
                assertCandidate(candidate, sample);
                set.add(candidate.one().point().edge().id());
            }

            assertEquals(refset.size(), candidates.size());
            assertTrue(set.containsAll(refset));
        }
        {
            double radius = 200;
            filter.setMaxRadius(radius);
            Point sample = new Point(11.010, 48.000);

            Set<Tuple<MatcherCandidate, Double>> candidates = filter
                    .candidates(new HashSet<MatcherCandidate>(), new MatcherSample(0, sample));

            Set<Long> refset = new HashSet<>(Arrays.asList(0L, 3L));
            Set<Long> set = new HashSet<>();

            for (Tuple<MatcherCandidate, Double> candidate : candidates) {
                assertTrue(Long.toString(candidate.one().point().edge().id()),
                        refset.contains(candidate.one().point().edge().id()));
                assertCandidate(candidate, sample);
                set.add(candidate.one().point().edge().id());
            }

            assertEquals(refset.size(), candidates.size());
            assertTrue(set.containsAll(refset));
        }
        {
            double radius = 200;
            filter.setMaxRadius(radius);
            Point sample = new Point(11.011, 48.001);

            Set<Tuple<MatcherCandidate, Double>> candidates = filter
                    .candidates(new HashSet<MatcherCandidate>(), new MatcherSample(0, sample));

            Set<Long> refset = new HashSet<>(Arrays.asList(0L, 2L, 3L));
            Set<Long> set = new HashSet<>();

            for (Tuple<MatcherCandidate, Double> candidate : candidates) {
                assertTrue(Long.toString(candidate.one().point().edge().id()),
                        refset.contains(candidate.one().point().edge().id()));
                assertCandidate(candidate, sample);
                set.add(candidate.one().point().edge().id());
            }

            assertEquals(refset.size(), candidates.size());
            assertTrue(set.containsAll(refset));
        }
        {
            double radius = 300;
            filter.setMaxRadius(radius);
            Point sample = new Point(11.011, 48.001);

            Set<Tuple<MatcherCandidate, Double>> candidates = filter
                    .candidates(new HashSet<MatcherCandidate>(), new MatcherSample(0, sample));

            Set<Long> refset = new HashSet<>(Arrays.asList(0L, 2L, 3L, 8L));
            Set<Long> set = new HashSet<>();

            for (Tuple<MatcherCandidate, Double> candidate : candidates) {
                assertTrue(Long.toString(candidate.one().point().edge().id()),
                        refset.contains(candidate.one().point().edge().id()));
                assertCandidate(candidate, sample);
                set.add(candidate.one().point().edge().id());
            }

            assertEquals(refset.size(), candidates.size());
            assertTrue(set.containsAll(refset));
        }
        {
            double radius = 200;
            filter.setMaxRadius(radius);
            Point sample = new Point(11.019, 48.001);

            Set<Tuple<MatcherCandidate, Double>> candidates = filter
                    .candidates(new HashSet<MatcherCandidate>(), new MatcherSample(0, sample));

            Set<Long> refset = new HashSet<>(Arrays.asList(2L, 3L, 5L, 10L));
            Set<Long> set = new HashSet<>();

            for (Tuple<MatcherCandidate, Double> candidate : candidates) {
                assertTrue(Long.toString(candidate.one().point().edge().id()),
                        refset.contains(candidate.one().point().edge().id()));
                assertCandidate(candidate, sample);
                set.add(candidate.one().point().edge().id());
            }

            assertEquals(refset.size(), candidates.size());
            assertTrue(set.containsAll(refset));
        }
    }

    @Test
    public void TestTransitions() throws JSONException {
        Matcher filter = new Matcher(map, router, cost, spatial);
        filter.setMaxRadius(200);
        {
            MatcherSample sample1 = new MatcherSample(0, new Point(11.001, 48.001));
            MatcherSample sample2 = new MatcherSample(60000, new Point(11.019, 48.001));

            Set<MatcherCandidate> predecessors = new HashSet<>();
            Set<MatcherCandidate> candidates = new HashSet<>();

            for (Tuple<MatcherCandidate, Double> candidate : filter
                    .candidates(new HashSet<MatcherCandidate>(), sample1)) {
                predecessors.add(candidate.one());
            }

            for (Tuple<MatcherCandidate, Double> candidate : filter
                    .candidates(new HashSet<MatcherCandidate>(), sample2)) {
                candidates.add(candidate.one());
            }

            assertEquals(2, predecessors.size());
            assertEquals(4, candidates.size());

            Map<MatcherCandidate, Map<MatcherCandidate, Tuple<MatcherTransition, Double>>> transitions =
                    filter.transitions(new Tuple<>(sample1, predecessors),
                            new Tuple<>(sample2, candidates));

            assertEquals(2, transitions.size());

            for (Entry<MatcherCandidate, Map<MatcherCandidate, Tuple<MatcherTransition, Double>>> source : transitions
                    .entrySet()) {
                assertEquals(4, source.getValue().size());

                for (Entry<MatcherCandidate, Tuple<MatcherTransition, Double>> target : source
                        .getValue().entrySet()) {
                    assertTransition(target.getValue(), new Tuple<>(source.getKey(), sample1),
                            new Tuple<>(target.getKey(), sample2), filter.getLambda());
                }

            }
        }
        {
            MatcherSample sample1 = new MatcherSample(0, new Point(11.019, 48.001));
            MatcherSample sample2 = new MatcherSample(60000, new Point(11.001, 48.001));

            Set<MatcherCandidate> predecessors = new HashSet<>();
            Set<MatcherCandidate> candidates = new HashSet<>();

            for (Tuple<MatcherCandidate, Double> candidate : filter
                    .candidates(new HashSet<MatcherCandidate>(), sample1)) {
                predecessors.add(candidate.one());
            }

            for (Tuple<MatcherCandidate, Double> candidate : filter
                    .candidates(new HashSet<MatcherCandidate>(), sample2)) {
                candidates.add(candidate.one());
            }

            assertEquals(4, predecessors.size());
            assertEquals(2, candidates.size());

            Map<MatcherCandidate, Map<MatcherCandidate, Tuple<MatcherTransition, Double>>> transitions =
                    filter.transitions(new Tuple<>(sample1, predecessors),
                            new Tuple<>(sample2, candidates));

            assertEquals(4, transitions.size());

            for (Entry<MatcherCandidate, Map<MatcherCandidate, Tuple<MatcherTransition, Double>>> source : transitions
                    .entrySet()) {
                if (source.getKey().point().edge().id() == 10) {
                    assertEquals(0, source.getValue().size());
                } else {
                    assertEquals(2, source.getValue().size());
                }

                for (Entry<MatcherCandidate, Tuple<MatcherTransition, Double>> target : source
                        .getValue().entrySet()) {
                    assertTransition(target.getValue(), new Tuple<>(source.getKey(), sample1),
                            new Tuple<>(target.getKey(), sample2), filter.getLambda());
                }
            }
        }
    }
}
