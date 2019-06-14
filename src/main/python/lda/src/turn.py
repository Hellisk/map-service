import pandas as pd
import pandas as pd
import numpy as np
import sys
import time
import math
import pickle
import random
import matplotlib.pyplot as plt
# %matplotlib inline
import json
import scipy
import numpy, scipy.sparse
import shapefile
from collections import defaultdict
import os
import sys
import matplotlib.cm as matcm
import trajmap

# import svd
reload(trajmap)
from matplotlib.patches import Circle
from matplotlib.patches import Arrow
import svd
# import mpld3
# from mpld3 import plugins, utils
from itertools import permutations
import plot
import gis
import networkx as nx
from plsa import plsa

sys.path.append('../matcher')

from graphdb_matcher import GraphDBMatcher

import matcher

reload(matcher)
import sqlite3
import pandas as pd
from cv2 import cv


def map_df2nx_old(map_df):
    # pset = set()
    pdict = {}
    idict = {}
    idx = 0
    for x, y in zip(map_df.lon, map_df.lat):
        pstr = str((x, y))
        if pdict.has_key(pstr) is False:
            pdict[pstr] = idx
            idict[idx] = (x, y)
            idx += 1
    G = nx.Graph()
    for rid in map_df.rid.unique():
        x = map_df[map_df.rid == rid].lon
        y = map_df[map_df.rid == rid].lat
        for (x0, y0), (x1, y1) in trajmap.pairwise(zip(x, y)):
            G.add_edge(pdict[str((x0, y0))], pdict[str((x1, y1))])
    return G, pdict, idict


def map_df2nx(map_df):
    # pset = set()
    pdict = {}
    idict = {}
    idx = 0
    for x, y in zip(map_df.lon, map_df.lat):
        # x = float('%.6f'%x)
        # y = float('%.6f'%y)
        _x, _y = x, y
        x = precision(float(x), 6)
        y = precision(float(y), 6)
        pstr = str((x, y))
        if pdict.has_key(pstr) is False:
            pdict[pstr] = idx
            idict[idx] = (x, y)
            idx += 1
    G = nx.Graph()
    for rid in map_df.rid.unique():
        x = map_df[map_df.rid == rid].lon
        y = map_df[map_df.rid == rid].lat
        for (x0, y0), (x1, y1) in trajmap.pairwise(zip(x, y)):
            # x0 = float('%.6f'%x0)
            # y0 = float('%.6f'%y0)
            # x1 = float('%.6f'%x1)
            # y1 = float('%.6f'%y1)
            x0 = precision(float(x0), 6)
            y0 = precision(float(y0), 6)
            x1 = precision(float(x1), 6)
            y1 = precision(float(y1), 6)
            G.add_edge(pdict[str((x0, y0))], pdict[str((x1, y1))])
    return G, pdict, idict


def save_db(map_df, db_file='sh_map.db'):
    gmap, pdict, idict = map_df2nx(map_df)
    nodes = []
    for nid in idict.keys():
        x, y = idict[nid]
        nodes.append((nid, x, y))
    edges = []
    for rid in map_df.rid.unique():
        r = map_df[map_df.rid == rid]
        for (x0, y0), (x1, y1) in trajmap.pairwise(zip(r.lon, r.lat)):
            # x0 = float('%.6f'%x0)
            # y0 = float('%.6f'%y0)
            # x1 = float('%.6f'%x1)
            # y1 = float('%.6f'%y1)
            x0 = precision(float(x0), 6)
            y0 = precision(float(y0), 6)
            x1 = precision(float(x1), 6)
            y1 = precision(float(y1), 6)
            pstr = str((x0, y0))
            u = pdict[pstr]
            pstr = str((x1, y1))
            v = pdict[pstr]
            edges.append((u, v))
    print len(nodes)
    print len(edges)

    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    cur.execute("CREATE TABLE nodes (id INTEGER, latitude FLOAT, longitude FLOAT, weight FLOAT)")
    cur.execute("CREATE TABLE edges (id INTEGER, in_node INTEGER, out_node INTEGER, weight FLOAT)")
    cur.execute("CREATE TABLE segments (id INTEGER, edge_ids TEXT)")
    cur.execute("CREATE TABLE intersections (node_id INTEGER)")
    conn.commit()
    nid_dict = {}
    nid = 1
    for _id, _lon, _lat in nodes:
        nid_dict[_id] = nid
        cur.execute("INSERT INTO nodes VALUES (" + str(nid) + "," + str(_lat) +
                    "," + str(_lon) + "," + str(1.0) + ")")
        nid += 1
    conn.commit()
    eid = 1
    for u, v in edges:
        cur.execute("INSERT INTO edges VALUES (" + str(eid) + "," + str(nid_dict[u]) + ","
                    + str(nid_dict[v]) + "," + str(1.0) + ")")
        eid += 1
    conn.commit()


def point_from_int(n, e, dist, gmap, road_dict, idict, transform_utm=True):
    visit = set()
    crt = n
    for _n in gmap[n].keys():
        if road_dict[_n] == e:
            nxt = _n
            break
    # print 'in1'
    _dist = 0
    while _dist < dist:
        # print 'in2'
        x0, y0 = idict[crt]
        x1, y1 = idict[nxt]
        # print x0, y0
        # print x1, y1
        if transform_utm:
            _x0, _y0 = gis.to_utm(x0, y0)
            _x1, _y1 = gis.to_utm(x1, y1)
        else:
            _x0, _y0 = x0, y0
            _x1, _y1 = x1, y1
        ldist = math.sqrt((_x1 - _x0) ** 2 + (_y1 - _y0) ** 2)
        _dist += ldist
        if _dist >= dist:
            x, y = gis.distance_cut(_x0, _y0, _x1, _y1, dist - _dist)
            # print 'ans:', x, y
            return gis.to_gps(x, y)
        visit.add(crt)
        crt = nxt
        nxt = set(gmap[nxt].keys()) - visit
        if len(nxt) != 1:
            print 'length error', nxt
            print set(gmap[crt].keys())
            print visit
            x, y = idict[crt]
            if transform_utm:
                return x, y
            else:
                return gis.to_gps(x, y)
        nxt = list(nxt)[0]
        # print _dist, dist


def color(road_dict, u, idx, gmap):
    if len(gmap[u].keys()) >= 3:
        return -1
    # global road_dict
    if u in road_dict:
        return road_dict[u]
    if idx == -1:
        if len(road_dict.values()) == 0:
            idx = 0
        else:
            idx = max(road_dict.values()) + 1
    road_dict[u] = idx
    for v in gmap[u].keys():
        color(road_dict, v, idx, gmap)
    return idx


def precision(f, length):
    return int(f * (10 ** length)) * 1.0 / (10 ** length)


def get_turns_from_data(data, gen_map_df, dist=100, folder='test', limit=-1, get_time=False):
    # gen_map_df = gis.convert_nx2map_df(mat, M, gps)
    if folder not in os.listdir('./'):
        os.mkdir(folder)
    ret_time = {}
    time0 = time.time()

    ids = []
    idx = 0
    _tid = -1
    for tid in data.tid:
        if tid != _tid:
            idx = 0
            _tid = tid
        ids.append(idx)
        idx += 1
    data['id'] = ids

    lons, lats = [], []
    if 'lon' not in gen_map_df.columns:
        for x, y in zip(gen_map_df.x, gen_map_df.y):
            lon, lat = gis.to_gps(x, y)
            lons.append(lon)
            lats.append(lat)
        gen_map_df['lon'] = lons
        gen_map_df['lat'] = lats
    print "Build sqlite database"
    if 'map.db' not in os.listdir(folder):
        save_db(gen_map_df, folder + '/map.db')
    time1 = time.time()
    ret_time['save_db'] = time1 - time0

    if 'trips' not in os.listdir(folder):
        os.mkdir(folder + '/trips')
    if 'matched_trips' not in os.listdir(folder):
        os.mkdir(folder + '/matched_trips')

    time0 = time.time()
    lons, lats = [], []
    for x, y in zip(data.x, data.y):
        lon, lat = gis.to_gps(x, y)
        lons.append(lon)
        lats.append(lat)
    if 'pLon' not in data.columns:
        data['pLon'] = lons
        data['pLat'] = lats
    if 'lon' not in data.columns:
        data['lon'] = lons
        data['lat'] = lats

    print "Write %d data" % len(data)
    for tid in data.tid.unique()[:limit]:
        d = data[data.tid == tid][['lat', 'lon']]
        d.index = range(len(d))
        d.to_csv(folder + '/trips/trip%d.csv' % (tid), header=False)

    print "Map-matching"
    matcher.matching(data, folder + '/map.db', folder + '/trips', folder + '/matched_trips')
    time1 = time.time()
    ret_time['map_matching'] = time1 - time0

    time0 = time.time()
    gmap, pdict, idict = map_df2nx(gen_map_df)
    ints = []
    for n in gmap.nodes():
        if len(gmap[n]) >= 3:
            ints.append(n)
    road_dict = {}
    cand_turns = set()
    neg = nx.Graph()  # node, edge, graph
    for n in ints:
        roads = []
        for u in gmap[n].keys():
            ret = color(road_dict, u, -1, gmap)
            if ret != -1:
                roads.append(ret)
                neg.add_edge('n%d' % n, 'e%d' % ret)
        # print roads
        for u in roads:
            for v in roads:
                if u == v:
                    continue
                cand_turns.add((u, v))
    for n in ints:
        road_dict[n] = -1
    turns = set()
    turns_gps = []

    print "Read matched trips"
    counter = 0
    for f in os.listdir(folder + '/matched_trips/'):
        print counter, f
        # for f in range(1000):
        # f = 'matched_trip%d.csv'%f
        trip = pd.read_csv(folder + '/matched_trips/' + f, sep=' ', names=['y', 'x', 'dist', 'y0', 'x0', 'y1', 'x1'], dtype=str)
        trip = trip.dropna(axis=0)
        # trip.y0 = trip.y0.astype(float)
        us, vs = [], []
        for x, y in zip(trip.x0, trip.y0):
            _x, _y = x, y
            # print x, y
            # x = float('%.6f'%float(x))
            # y = float('%.6f'%float(y))
            x = precision(float(x), 6)
            y = precision(float(y), 6)
            # print str((x,y))
            try:
                us.append(pdict[str((x, y))])
            except Exception, _data:
                print _data, pdict.keys()[0]
                print x, y, _x, _y
                continue
        for x, y in zip(trip.x1, trip.y1):
            _x, _y = x, y
            # print x, y
            # x = float('%.6f'%float(x))
            # y = float('%.6f'%float(y))
            x = precision(float(x), 6)
            y = precision(float(y), 6)
            # print str((x,y))
            try:
                vs.append(pdict[str((x, y))])
            except Exception, _data:
                print _data, pdict.keys()[0]
                print x, y, _x, _y
                continue
        try:
            trip['u'] = us
            trip['v'] = vs
        except Exception, _data:
            # print _data
            continue
        for u0, v0, u1, v1 in zip(trip[:-1].u, trip[:-1].v, trip[1:].u, trip[1:].v):
            try:
                r0 = max(road_dict[u0], road_dict[v0])
                r1 = max(road_dict[u1], road_dict[v1])
            except Exception, _data:
                # print _data
                continue
            if r0 != r1:
                # print r0, r1
                if (r0, r1) in cand_turns:
                    turns.add((r0, r1))
        counter += 1
        '''
        if counter > 5000:
            break
        '''
    time1 = time.time()
    ret_time['read_matched'] = time1 - time0

    time0 = time.time()
    print "Turn inference"
    for u, v in turns:
        n = set(neg['e%d' % u].keys()) & set(neg['e%d' % v].keys())
        n = int(list(n)[0][1:])
        x0, y0 = point_from_int(n, u, dist, gmap, road_dict, idict)
        x1, y1 = point_from_int(n, v, dist, gmap, road_dict, idict)
        turns_gps.append((x0, y0, x1, y1))
    time1 = time.time()
    ret_time['inference'] = time1 - time0
    ret_time['total_time'] = ret_time['save_db'] + ret_time['map_matching'] + ret_time['read_matched'] + ret_time['inference']

    cand_gps = []
    for u, v in cand_turns:
        n = set(neg['e%d' % u].keys()) & set(neg['e%d' % v].keys())
        n = int(list(n)[0][1:])
        x0, y0 = point_from_int(n, u, dist, gmap, road_dict, idict)
        x1, y1 = point_from_int(n, v, dist, gmap, road_dict, idict)
        cand_gps.append((x0, y0, x1, y1))
    if get_time:
        return cand_gps, turns_gps, ret_time
    else:
        return cand_gps, turns_gps


def norm_distr(m, std=20.0):
    m = float(m)
    return (math.exp(-1 * (m ** 2) / (2 * (std ** 2)))) / (math.sqrt(2 * math.pi) * std)


def get_gen_map_df():
    from pymongo import MongoClient
    client = MongoClient()
    client = MongoClient("mongodb://10.60.43.110:27017")
    db = client.Biagioni
    rec = list(db['default.runs'].find({'status': 'COMPLETED', 'config.data': 'minsh_4000'}))
    fmap = db['default.chunks'].find_one({'files_id': rec[-1]['artifacts'][-1]})['data']

    def convert2map_df(edge, node):
        rids, xs, ys = [], [], []
        i = 0
        for u, v in zip(edge.u, edge.v):
            _x, _y = node.ix[u].x, node.ix[u].y
            xs.append(_x)
            ys.append(_y)
            _x, _y = node.ix[v].x, node.ix[v].y
            xs.append(_x)
            ys.append(_y)
            rids.append(i)
            rids.append(i)
            i += 1
        return pd.DataFrame({'rid': rids, 'x': xs, 'y': ys})

    def read_biagioni_map(lines):
        vertices = []
        edges = []
        for i in range(0, len(lines), 3):
            if len(lines[i]) < 3:
                continue
            uy, ux = lines[i].split(',')
            vy, vx = lines[i + 1].split(',')
            # print ux, uy, vx, vy

            ux, uy, vx, vy = float(ux), float(uy), float(vx), float(vy)
            ux, uy = gis.to_utm(ux, uy)
            vx, vy = gis.to_utm(vx, vy)
            # print ux, uy, vx, vy
            vertices.append((ux, uy))
            vertices.append((vx, vy))
            edges.append((len(vertices) - 2, len(vertices) - 1, 1))
        vertices_df = pd.DataFrame(vertices)
        edges_df = pd.DataFrame(edges)
        return vertices_df, edges_df

    vdf, edf = read_biagioni_map(fmap.split('\n'))
    edf['u'] = edf[0]
    edf['v'] = edf[1]
    vdf['x'] = vdf[0]
    vdf['y'] = vdf[1]
    gen_map_df = convert2map_df(edf, vdf)
    line_set = set()
    single_set = set()
    for rid in gen_map_df.rid.unique():
        d = gen_map_df[gen_map_df.rid == rid]
        x0, y0, x1, y1 = d.values[0][1], d.values[0][2], d.values[1][1], d.values[1][2]
        if (x1, y1, x0, y0) in line_set:
            x0, y0, x1, y1
        else:
            single_set.add(rid)
        line_set.add((x0, y0, x1, y1))
    idx = []
    for i in single_set:
        idx.append(i * 2)
        idx.append(i * 2 + 1)
    gen_map_df = gen_map_df.ix[idx]
    return gen_map_df


def matched_turn_num(gen_cand_gps, real_cand_gps, dist=20):
    num = 0
    for x0, y0, x1, y1 in gen_cand_gps:
        for _x0, _y0, _x1, _y1 in real_cand_gps:
            if gis.distance(x0, y0, _x0, _y0) < dist and gis.distance(x1, y1, _x1, _y1) < dist:
                num += 1
    return num


def get_mat(data, side, gen_map_df):
    mat = trajmap.Sag(data, side)

    roads = gen_map_df.copy()
    roads.rid = roads.rid.astype(int)
    rid_dict = {}
    ntids = []
    for i in roads.rid:
        if i not in rid_dict.keys():
            if len(rid_dict.keys()) == 0:
                rid_dict[i] = 0
            else:
                rid_dict[i] = max(rid_dict.values()) + 1
        ntids.append(rid_dict[i])
    roads['tid'] = ntids
    roads.x = roads.x.astype(float)
    roads.y = roads.y.astype(float)

    _mat = trajmap.Sag(roads, side, inc=-1)
    mat.G = _mat.tog.copy()
    mat.width, mat.height, mat.x0, mat.y0, mat.x1, mat.y1 = _mat.width, _mat.height, _mat.x0, _mat.y0, _mat.x1, _mat.y1
    return mat


def get_G(x0, x1, y0, y1, width, height, side, data):
    G = nx.Graph()
    min_x, max_x, min_y, max_y = x0, x1, y0, y1
    col = []
    row = []
    for tid in data.tid.unique():
        themap = cv.CreateMat(height, width, cv.CV_16UC1)
        cv.SetZero(themap)
        for p in trajmap.pairwise(data[data.tid == tid].values):
            x0, y0, x1, y1 = p[0][1], p[0][2], p[1][1], p[1][2]
            oy = height - int((y0 - min_y) / side)
            ox = int((x0 - min_x) / side)
            dy = height - int((y1 - min_y) / side)
            dx = int((x1 - min_x) / side)
            cv.Line(themap, (ox, oy), (dx, dy), (32), 1, cv.CV_AA)
        node_set = set()
        for y, x in zip(*np.matrix(themap).nonzero()):
            node_set.add((x, y))
            a = x + (height - y) * width
            for _x, _y in [(x - 1, y), (x, y - 1), (x - 1, y - 1), (x + 1, y), (x, y + 1), (x + 1, y + 1), (x - 1, y + 1), (x + 1, y - 1)]:
                if (_x, _y) in node_set:
                    _a = _x + (height - _y) * width
                    G.add_edge(a, _a)
        for tup in zip(*np.matrix(themap).nonzero()):
            row.append(tup[1] + (height - tup[0]) * width)
            col.append(tid)
    sag = scipy.sparse.csc_matrix(([1] * len(row), (row, col)), shape=(max(row) + 1, max(col) + 1))
    return sag, G


def get_tlist(mat, data):
    tlist = []
    for i in range(max(data.tid.unique()) + 1):
        tlist.append([])
    for tid in data.tid.unique():
        d = data[data.tid == tid]
        for x, y in zip(d.x, d.y):
            oy = int((y - mat.y0) / mat.side)
            ox = int((x - mat.x0) / mat.side)
            c = ox + oy * mat.width
            tlist[tid].append(c)
    return tlist


def get_turns_prob(mat):
    inters = []
    uG = mat.G.to_undirected()
    for n in uG.nodes():
        if len(uG[n]) > 2:
            inters.append(n)
    print len(inters)

    node_dict = {}
    idx_dict = defaultdict(set)
    visited = set()
    idx = 0

    def color(n, idx):
        if n in inters or n in visited:
            return
        node_dict[n] = idx
        idx_dict[idx].add(n)
        visited.add(n)
        for m in mat.G[n].keys():
            color(m, idx)

    for n in inters:
        for m in mat.G[n].keys():
            if m not in visited:
                color(m, idx)
                idx += 1
    print len(node_dict.keys()), len(visited), len(set(node_dict.values()))

    # width = 8
    width = 5
    color_dict = defaultdict(dict)
    for i in idx_dict.keys():
        for c in idx_dict[i]:
            x, y = trajmap.get_gps(mat, c)
            for _y in range(y - width, y + width + 1):
                for _x in range(x - width, x + width + 1):
                    _c = _y * mat.width + _x
                    dist = math.sqrt((x - _x) ** 2 + (y - _y) ** 2) * mat.side
                    perct = norm_distr(dist)
                    if color_dict[_c].has_key(i):
                        if color_dict[_c][i] < perct:
                            color_dict[_c][i] = perct
                    else:
                        color_dict[_c][i] = perct
    print len(color_dict)
    for c in color_dict.keys():
        total_perct = sum(color_dict[c].values())
        for idx in color_dict[c].keys():
            color_dict[c][idx] = color_dict[c][idx] / total_perct
    return inters, node_dict, color_dict, idx_dict


def compute_intersection(mat, node_dict, idx_dict, tlist, color_dict, _int, min_p, break_loop=True):
    turns = []
    roads = []
    for n in mat.G[_int]:
        # TODO: map problem
        if node_dict.has_key(n):
            roads.append(node_dict[n])
    print _int, roads

    for ra in roads:
        for rb in roads:

            if ra == rb: continue
            v0 = np.zeros_like(mat.sag[ra, :])
            v1 = np.zeros_like(mat.sag[rb, :])

            for row in idx_dict[ra]:
                v0 = v0 + mat.sag[row, :]
            for row in idx_dict[rb]:
                v1 = v1 + mat.sag[row, :]
            tids = np.multiply(v0.toarray(), v1.toarray()).nonzero()[1]
            # print ra, rb
            # print tids
            # return
            for tid in tids:
                p = prob_t2rab(tlist, color_dict, tid, ra, rb)
                # print tid, ra, rb, p
                if p > min_p:
                    # print tid, ra, rb, p
                    turns.append((ra, rb))
                    if break_loop:
                        break
    return turns


def prob(plist, u, v):
    p = 1
    for i in range(u, v + 1):
        if i >= len(plist):
            continue
        p = p * (1 - plist[i])
    return 1 - p


def prob_t2rab(tlist, color_dict, tid, ra, rb):
    prob_list = []
    ha = -1
    hb = -1
    ga = []
    gb = []
    pa = []
    pb = []
    for idx, c in zip(range(len(tlist[tid])), tlist[tid]):
        prob_list.append(color_dict[c])
        if ra in color_dict[c].keys():
            pa.append(color_dict[c][ra])
            if ha == -1:
                ha = idx
        else:
            pa.append(0)
            if ha != -1:
                ga.append((ha, idx))
                ha = -1
        if rb in color_dict[c].keys():
            pb.append(color_dict[c][rb])
            if hb == -1:
                hb = idx
        else:
            pb.append(0)
            if hb != -1:
                gb.append((hb, idx))
                hb = -1
    if ha != -1:
        ga.append((ha, idx + 1))
        ha = -1
    if hb != -1:
        gb.append((hb, idx + 1))
        hb = -1
    '''
    print pa
    print pb
    print ga
    print gb
    print ha
    print hb
    '''
    p_max = 0
    for c, d in ga:
        for e, f in gb:
            if c <= e and e <= (d + 1) and d <= f:
                for i in range(e - 1, d + 1):
                    p = prob(pa, c, i) * prob(pb, i + 1, f)
                    # print c, i, i+1, f, p
                    if p > p_max:
                        p_max = p
    return p_max


def make_mat(data):
    side = 15
    mat = trajmap.Sag(data, side)
    gen_map_df = get_gen_map_df()
    roads = gen_map_df.copy()
    roads.rid = roads.rid.astype(int)
    rid_dict = {}
    ntids = []
    for i in roads.rid:
        if i not in rid_dict.keys():
            if len(rid_dict.keys()) == 0:
                rid_dict[i] = 0
            else:
                rid_dict[i] = max(rid_dict.values()) + 1
        ntids.append(rid_dict[i])
    roads['tid'] = ntids
    roads.x = roads.x.astype(float)
    roads.y = roads.y.astype(float)

    sag, G = get_G(mat.x0, mat.x1, mat.y0, mat.y1, mat.width, mat.height, 15, roads)
    mat.G = G
    tlist = get_tlist(mat, data)
    return mat, tlist


def get_turn_new(mat, tlist, p=0.5):
    inters, node_dict, color_dict, idx_dict = get_turns_prob(mat)
    turns = []
    for n in inters:
        t = compute_intersection(mat, node_dict, idx_dict, tlist, color_dict, n, p)
        for _t in t:
            turns.append(_t)
    for n in inters:
        node_dict[n] = -1
    cand_turns = set()
    neg = nx.Graph()  # node, edge, graph
    for n in inters:
        roads = []
        for u in mat.G[n].keys():
            # ret = turn.color(road_dict, u, -1, gmap)
            ret = node_dict[u]
            if ret != -1:
                roads.append(ret)
                neg.add_edge('n%d' % n, 'e%d' % ret)
        # print roads
        for u in roads:
            for v in roads:
                if u == v:
                    continue
                cand_turns.add((u, v))
    idict = {}
    for n in mat.G.nodes():
        idict[n] = trajmap.get_true_gps(mat, n, {})
    cand_turns = []
    for _int in inters:
        roads = []
        for n in mat.G[_int]:
            # TODO: map problem
            if node_dict.has_key(n):
                roads.append(node_dict[n])
        for u in roads:
            for v in roads:
                if u == -1 or v == -1:
                    continue
                if u == v:
                    continue
                cand_turns.append((u, v))
    cand_gps = []
    for u, v in cand_turns:
        # print u, v
        n = set(neg['e%d' % u].keys()) & set(neg['e%d' % v].keys())
        n = int(list(n)[0][1:])
        # print n, len(mat.G.edges()), len(node_dict.keys()), len(idict.keys())
        x0, y0 = point_from_int(n, u, 100, mat.G, node_dict, idict, transform_utm=False)
        x1, y1 = point_from_int(n, v, 100, mat.G, node_dict, idict, transform_utm=False)
        cand_gps.append((x0, y0, x1, y1))
    turn_gps = []
    for u, v in turns:
        # print u, v
        n = set(neg['e%d' % u].keys()) & set(neg['e%d' % v].keys())
        n = int(list(n)[0][1:])
        # print n, len(mat.G.edges()), len(node_dict.keys()), len(idict.keys())
        x0, y0 = point_from_int(n, u, 100, mat.G, node_dict, idict, transform_utm=False)
        x1, y1 = point_from_int(n, v, 100, mat.G, node_dict, idict, transform_utm=False)
        turn_gps.append((x0, y0, x1, y1))
    return cand_gps, turn_gps
