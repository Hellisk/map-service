import math
import matplotlib.pyplot as plt
# %matplotlib inline
import numpy as np
import pandas as pd
import sys
import utm
from math import radians, cos, sin, asin, sqrt
from matplotlib import lines
from shapely.geometry import LineString, Point

import trajmap

chc_utm_axis = (442551, 447326, 4634347, 4637377)
chc_gps_axis = (-87.69215, -87.634896, 41.858952, 41.886565)
minsh_utm_axis = (347500, 352500, 3447500, 3452500)
minsh_gps_axis = (121.4, 121.452, 31.1515, 31.197)
# maxsh_utm_axis = (345000, 365000, 3445000, 3465000)
maxsh_utm_axis = (340000, 365000, 3442500, 3467500)
maxsh_gps_axis = (121.322, 121.581, 31.105, 31.334)


def distance_cut(x0, y0, x1, y1, dist):
    # return new x1, y1 which distance(x0, y0, x1, y1) == dist
    _dist = math.sqrt((x0 - x1) ** 2 + (y0 - y1) ** 2)
    dx = (x1 - x0) * dist / _dist
    dy = (y1 - y0) * dist / _dist
    x1, y1 = x0 + dx, y0 + dy
    # print math.sqrt((x0-x1)**2+(y0-y1)**2)
    return x1, y1


def project((x, y), (x0, y0, x1, y1)):
    line = LineString([(x0, y0), (x1, y1)])
    p = Point(x, y)
    return list(line.interpolate(line.project(p)).coords)[0]


def dist((x, y), (x0, y0, x1, y1)):
    line = LineString([(x0, y0), (x1, y1)])
    p = Point(x, y)
    # _x, _y = list(line.interpolate(line.project(p)).coords)[0]
    # return distance(x, y, _x, _y, utm=True)
    return p.distance(line)


def scale_map_by_axis(map_df, axis):
    x0, x1, y0, y1 = axis
    rids = set(map_df[(map_df.x > x0) & (map_df.x < x1) \
                      & (map_df.y > y0) & (map_df.y < y1)].rid)
    map_df.index = map_df.rid
    map_df = map_df.ix[rids]
    return map_df


'''
build a r-tree index for map
'''


def build_index(map_df, axis=None):
    if axis is not None:
        map_df = scale_map_by_axis(map_df, axis)
    from rtree import index
    idx = index.Index()
    for rid in map_df.rid.unique():
        road = map_df[map_df.rid == rid][['x', 'y']]
        for u, v in trajmap.pairwise(road.values):
            idx.insert(rid, (min(u[0], v[0]), min(u[1], v[1]), max(u[0], v[0]), max(u[1], v[1])))
    return idx


'''
find the line segment with minimum distance to the point using the rtree index
'''


def min_dist_rtree_without_bound(p, map_idx, map_df):
    rids = set(map_idx.nearest((p[0], p[1], p[0], p[1]), 6)) | \
           set(map_idx.intersection((p[0], p[1], p[0], p[1])))
    _min_dist = -1
    _rid = -1
    # map_df.index = map_df.rid
    for rid in rids:
        road = map_df.ix[rid][['x', 'y']].values
        for u, v in trajmap.pairwise(road):
            _dist = dist((p[0], p[1]), (u[0], u[1], v[0], v[1]))
            if (_dist < _min_dist) or (_min_dist == -1):
                _min_dist = _dist
                _rid = rid
    # print _min_dist, _rid
    return _min_dist


def dist_to_road(p, map_df, rid):
    road = map_df.ix[rid][['x', 'y']].values
    _min_dist = -1
    for u, v in trajmap.pairwise(road):
        try:
            _dist = dist(p, (u[0], u[1], v[0], v[1]))
            if (_dist < _min_dist) or (_min_dist == -1):
                _min_dist = _dist
        except Exception, data:
            print 'Error: dist to road:', u, v
            continue
    return _min_dist


def dist_to_bound(p, map_df, rid):
    road = map_df.ix[rid][['x', 'y']].values
    _min_dist = -1
    x, y = p
    for u, v in trajmap.pairwise(road):
        try:
            ux, uy, vx, vy = u[0], u[1], v[0], v[1]
        except Exception, data:
            print 'Error: dist to bound:', u, v
            continue
        if min(ux, vx) <= x <= max(ux, vx):
            w = 0
        else:
            w = min((ux - x) ** 2, (vx - x) ** 2)
        if min(uy, vy) <= y <= max(uy, vy):
            h = 0
        else:
            h = min((uy - y) ** 2, (vy - y) ** 2)
        _dist = math.sqrt(w + h)
        if _min_dist == -1 or _dist < _min_dist:
            _min_dist = _dist
    return _min_dist


def min_dist_rtree(p, map_idx, map_df):
    _min_dist = 9999999
    low_bound = 9999999
    upp_bound = 9999999
    for rid in map_idx.intersection((p[0], p[1], p[0], p[1])):
        _dist = dist_to_road(p, map_df, rid)
        if _dist < _min_dist:
            _min_dist = _dist

    for rid in map_idx.nearest((p[0], p[1], p[0], p[1]), 10):
        _dist = dist_to_bound(p, map_df, rid)
        if _dist > low_bound:
            low_bound = _dist
        if low_bound > upp_bound:
            break
        _dist = dist_to_road(p, map_df, rid)
        if _dist < _min_dist:
            _min_dist = _dist
        if _dist < upp_bound:
            upp_bound = _dist
        if low_bound > upp_bound:
            break
    return _min_dist


def road_within_dist((u, v), map_idx, map_df, dist):
    for rid in map_idx.intersection((u[0], u[1], u[0], u[1])):
        _dist = dist_to_road(u, map_df, rid)
        if _dist < dist:
            _dist = dist_to_road(v, map_df, rid)
            if _dist < dist:
                return True

    for rid in map_idx.nearest((u[0], u[1], u[0], u[1]), 40):
        lower_bound = dist_to_bound(u, map_df, rid)
        # print lower_bound
        if lower_bound > dist:
            break
        _dist = dist_to_road(u, map_df, rid)
        if _dist < dist:
            _dist = dist_to_road(v, map_df, rid)
            if _dist < dist:
                return True

    return False


def matched_length(gen_map_df, map_df, axis=None, max_dist=20, step=5.0):
    idx = build_index(gen_map_df, axis)
    cnt = 0
    total = len(map_df.rid.unique())
    map_df.index = map_df.rid
    matched_len = 0
    matched_list = []
    not_match = []

    for rid in map_df.rid.unique():
        if cnt % int(total / 20) == 0:
            sys.stdout.write("\rComplete (" + str(cnt) + "/" + str(total) + ")... ")
            sys.stdout.flush()
        cnt += 1
        road = map_df.ix[rid][['x', 'y']].values
        for u, v in trajmap.pairwise(road):
            # print u, v
            ux, uy, vx, vy = u[0], u[1], v[0], v[1]
            dist = math.sqrt((ux - vx) ** 2 + (uy - vy) ** 2)
            n = int(1.0 * dist / step)
            # print dist, n, step
            _x, _y = ux, uy
            tups = [(_x, _y)]
            for i in range(1, n):
                _x += (vx - ux) / n
                _y += (vy - uy) / n
                tups.append((_x, _y))
            tups.append((vx, vy))
            for u, v in trajmap.pairwise(tups):
                matched = road_within_dist((u, v), idx, gen_map_df, max_dist)
                matched_list.append(matched)
                if matched:
                    matched_len += math.sqrt((u[0] - v[0]) ** 2 + (u[1] - v[1]) ** 2)
                '''
                else:
                    not_match.append((u,v))
                '''
    return matched_len


def nearest_road_rtree(p, map_idx, map_df):
    _min_dist = 9999999
    low_bound = 9999999
    upp_bound = 9999999
    _rid = -1
    for rid in map_idx.intersection((p[0], p[1], p[0], p[1])):
        _dist = dist_to_road(p, map_df, rid)
        if _dist < _min_dist:
            _min_dist = _dist
            _rid = rid

    for rid in map_idx.nearest((p[0], p[1], p[0], p[1]), 10):
        _dist = dist_to_bound(p, map_df, rid)
        if _dist > low_bound:
            low_bound = _dist
        if low_bound > upp_bound:
            break
        _dist = dist_to_road(p, map_df, rid)
        if _dist < _min_dist:
            _min_dist = _dist
            _rid = rid
        if _dist < upp_bound:
            upp_bound = _dist
        if low_bound > upp_bound:
            break
    return _rid


'''
find the line segment with minimum distance to the point by iterate all road segment
'''


def min_dist(p, map_df, axis=None):
    if axis != None:
        map_df = scale_map_by_axis(map_df, axis)
    _min_dist = -1
    _rid = -1
    for rid in map_df.rid.unique():
        road = map_df[map_df.rid == rid]
        for u, v in trajmap.pairwise(road.values):
            _dist = dist((p[0], p[1]), (u[1], u[2], v[1], v[2]))
            if _dist < _min_dist or (_min_dist == -1):
                _min_dist = _dist
                _rid = rid
    # print min_dist, _rid
    return _min_dist


'''
compute total length of a map by utm
'''


def map_length(map_df):
    dist = 0
    for rid in map_df.rid.unique():
        road = map_df[map_df.rid == rid][['x', 'y']].values
        for u, v in trajmap.pairwise(road):
            dist += math.sqrt((u[0] - v[0]) ** 2 + (u[1] - v[1]) ** 2)
    return dist


def convert_nx2map_df(mat, M, gps):
    rids, xs, ys = [], [], []
    i = 0
    for a, b in M.edges_iter():
        ix, iy = trajmap.get_true_gps(mat, a, gps)
        jx, jy = trajmap.get_true_gps(mat, b, gps)
        xs.append(ix)
        ys.append(iy)
        xs.append(jx)
        ys.append(jy)
        rids.append(i)
        rids.append(i)
        i += 1
    return pd.DataFrame({'rid': rids, 'x': xs, 'y': ys})


def candidates_error(mat, map_df):
    idx = build_index(map_df)
    error = []
    for c in mat.candidates:
        p = trajmap.get_true_gps(mat, c)
        error.append(min_dist_rtree(p, idx, map_df))
        # return
    error = sorted(error)
    result = {'min': min(error), 'max': max(error),
              'mean': sum(error) / len(error), 'p25': error[int(len(error) * 0.25)],
              'p50': error[int(len(error) * 0.5)], 'p75': error[int(len(error) * 0.75)],
              'length': len(error)}
    return result


def evaluate_map(gen_map_df, map_df, axis=None, step=5.0, max_dist=20.0):
    map_df = scale_map_by_axis(map_df, axis)
    map_df.index = map_df.rid
    gen_map_df = scale_map_by_axis(gen_map_df, axis)
    gen_map_df.index = gen_map_df.rid

    idx = build_index(map_df, axis)
    step = step
    error = []
    cnt = 0
    total = len(gen_map_df.rid.unique())

    for rid in gen_map_df.rid.unique():
        if cnt % int(total / 20) == 0:
            sys.stdout.write("\rComplete (" + str(cnt) + "/" + str(total) + ")... ")
            sys.stdout.flush()
        cnt += 1
        road = gen_map_df[gen_map_df.rid == rid][['x', 'y']].values
        for u, v in trajmap.pairwise(road):
            # print u, v
            ux, uy, vx, vy = u[0], u[1], v[0], v[1]
            dist = math.sqrt((ux - vx) ** 2 + (uy - vy) ** 2)
            n = int(1.0 * dist / step)
            # print dist, n, step
            _x, _y = ux, uy
            tups = [(_x, _y)]
            for i in range(1, n):
                _x += (vx - ux) / n
                _y += (vy - uy) / n
                tups.append((_x, _y))
            tups.append((vx, vy))
            for _x, _y in tups:
                error.append(min_dist_rtree((_x, _y), idx, map_df))

    error = sorted(error)
    result = {}
    result['error'] = {'min': min(error), 'max': max(error),
                       'mean': sum(error) / len(error), 'p25': error[int(len(error) * 0.25)],
                       'p50': error[int(len(error) * 0.5)], 'p75': error[int(len(error) * 0.75)]}
    print result['error']

    '''precision'''
    mlen = matched_length(map_df, gen_map_df, axis, max_dist)
    result['gen_matched_len'] = mlen
    result['gen_len'] = map_length(gen_map_df)
    result['precision'] = result['gen_matched_len'] / result['gen_len']
    print result['precision']

    '''recall'''
    mlen = matched_length(gen_map_df, map_df, axis, max_dist)
    result['map_matched_len'] = mlen
    result['map_len'] = map_length(map_df)
    result['recall'] = result['map_matched_len'] / result['map_len']
    print result['recall']

    result['fscore'] = 2 * result['precision'] * result['recall'] / (result['precision'] + result['recall'])

    return result


def distance(lon1, lat1, lon2, lat2, utm=False):
    if utm:
        # lon1, lat1 = to_gps(x=lon1, y=lat1)
        # lon2, lat2 = to_gps(x=lon2, y=lat2)
        return math.sqrt((lon1 - lon2) ** 2 + (lat1 - lat2) ** 2)
    # print lon1, lat1, lon2, lat2
    """
    Calculate the great circle distance between two points
    on the earth (specified in decimal degrees)
    http://boulter.com/gps/distance/
    """
    # convert decimal degrees to radians
    lon1, lat1, lon2, lat2 = map(radians, [lon1, lat1, lon2, lat2])
    # haversine formula
    dlon = lon2 - lon1
    dlat = lat2 - lat1
    a = sin(dlat / 2) ** 2 + cos(lat1) * cos(lat2) * sin(dlon / 2) ** 2
    c = 2 * asin(sqrt(a))
    m = 6367000.0 * c
    return m


def get_utm(x, y):
    trip_counter = 0
    lats = []
    lons = []
    print 'hi'
    sys.stdout.flush()
    for _x, _y in zip(x, y):
        if ((trip_counter % 1000 == 0) or (trip_counter == len(x))):
            sys.stdout.write("\rCompleted: " + str(trip_counter) + "/" + str(len(x)) + "... ")
            sys.stdout.flush()
        lon, lat = to_utm(y=_y, x=_x)
        lats.append(lat)
        lons.append(lon)
        trip_counter += 1
    return lons, lats


def to_gps(x=0.0, y=0.0):
    # print x, y, type(x), type(y)
    if x < 400000:
        lat, lon = utm.to_latlon(x, y, 51, 'R')
        return lon, lat
    elif x > 400000:
        lat, lon = utm.to_latlon(x, y, 16, 'T')
        return lon, lat


def to_utm(x=0, y=0):
    lon, lat, _a, _b = utm.from_latlon(y, x)
    return lon, lat


def shp_to_df(shapes, axis=None, convert_utm=False):
    map_df = pd.DataFrame()
    if axis != None:
        X0, X1, Y0, Y1 = axis
    rid = 0
    print 'Converting shapes'
    sys.stdout.flush()
    for i in range(len(shapes)):
        p = shapes[i]
        arr = np.array(p.points)
        x, y = arr[:, 0], arr[:, 1]
        if axis != None:
            if x.max() < X0 or x.min() > X1 or y.max() < Y0 or y.min() > Y1:
                continue
        df = pd.DataFrame(arr)
        df['rid'] = rid
        map_df = pd.concat([map_df, df])
        rid += 1
    map_df = map_df.rename(columns={0: 'lon', 1: 'lat'})
    if convert_utm:
        xs, ys = df_utm(map_df)
        map_df['x'] = xs
        map_df['y'] = ys
    return map_df


def plot_map_df(map_df, ca=None, axis=None, color='b', linewidth=1):
    if axis != None:
        x0, x1, y0, y1 = axis
        rids = set(map_df[(map_df.x > x0) & (map_df.x < x1) \
                          & (map_df.y > y0) & (map_df.y < y1)].rid)
        map_df.index = map_df.rid
        map_df = map_df.ix[rids]
    if ca == None:
        plt.figure(figsize=(10, 10))
        currentAxis = plt.gca()
        sys.stdout.flush()
    else:
        currentAxis = ca
    for rid in map_df.rid.unique():
        x = map_df[map_df.rid == rid].x
        y = map_df[map_df.rid == rid].y
        '''
        if axis != None:
            if x.max() < X0 or x.min() > X1 or y.max() < Y0 or y.min() > Y1:
                continue
        '''
        # currentAxis.plot(x, y,'b-')
        line = lines.Line2D(x, y, linewidth=linewidth, color=color)
        currentAxis.add_line(line)
    if ca == None:
        if axis == None:
            plt.axis('tight')
            plt.show()
        else:
            plt.axis([x0, x1, y0, y1])
            plt.show()
    else:
        return currentAxis
