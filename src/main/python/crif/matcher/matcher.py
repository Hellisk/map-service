import sys

sys.path.append("/usr/local/Cellar/opencv/2.4.12_2/lib/python2.7/site-packages")
# %matplotlib inline
import sqlite3
import pickle
import pandas as pd
from shapely.geometry import LineString, Point
from graphdb_matcher_run import MatchGraphDB
import utm

sys.path.append('../src')
# import trajmap
import train


def pairwise(iterable):
    "s -> (s0,s1), (s1,s2), (s2, s3), ..."
    a, b = tee(iterable)
    next(b, None)
    return izip(a, b)


def split_data(data):
    data['id'] = range(len(data))
    tids = []
    tid = 0
    for i in range(len(data)):
        tids.append(tid)
        tid += 1
        if tid == 100:
            tid = 0
    data['tid'] = tids
    if 'time' in data.columns:
        data = data.sort_values(by=['tid', 'time'])
    else:
        data = data.sort_values(by=['tid', 'id'])
    return data


def convert_utm(data, x="pLon", y="pLat"):
    trip_counter = 0
    lats = []
    lons = []
    for idx, row in data.iterrows():
        if ((trip_counter % 1000 == 0) or (trip_counter == len(data))):
            sys.stdout.write("\rCompleted: " + str(trip_counter) + "/" + str(len(data)) + "... ")
            sys.stdout.flush()
        lat, lon, _a, _b = utm.from_latlon(row[y], row[x])
        lats.append(lat)
        lons.append(lon)
        trip_counter += 1
    return lons, lats


'''
def save_map(mat, M, gps, file_name):
    nodes = []
    for node in M.nodes():
        lon, lat = trajmap.get_true_gps(mat, node, gps)
        #nodes.append((node, lon, lat))
        gps_lat, gps_lon = utm.to_latlon(lon, lat, 51,'R')
        nodes.append((node, gps_lon, gps_lat))
    edges = []
    for u, v in M.edges():
        edges.append((u, v))
    with open(file_name, 'w') as f:
        pickle.dump([nodes, edges], f)
'''


def save_db(pickle_file='utm_map.pickle', db_file='map.db'):
    with open(pickle_file, 'r') as f:
        nodes, edges = pickle.load(f)
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


def dist((x, y), l):
    line = LineString([(l['x0'], l['y0']), (l['x1'], l['y1'])])
    p = Point(x, y)
    # print p.distance(line)
    return p.distance(line)


def project((x, y), l):
    line = LineString([(l['x0'], l['y0']), (l['x1'], l['y1'])])
    p = Point(x, y)
    return list(line.interpolate(line.project(p)).coords)[0]


def get_data(data_file='data.pickle'):
    with open(data_file, 'r') as f:
        data = pickle.load(f)
        return data


def matching(data, db_file, trip_folder, matched_folder):
    match_graphdb = MatchGraphDB(db_file, 500, 550)
    for tid in data.tid.unique():
        trip = data[data.tid == tid][['id', 'pLat', 'pLon']]
        trip['time'] = range(1, 1 + len(trip) * 5, 5)
        trip[['pLat', 'pLon', 'time']].to_csv(trip_folder + '/trip' + str(tid) + '.csv', header=False)
        match_graphdb.process_trip(trip_folder, 'trip' + str(tid) + '.csv', matched_folder)


def get_matched_trip(data, matched_folder):
    xs = []
    ys = []
    for tid in data.tid.unique():
        global matched_trip
        matched_trip = pd.read_csv(matched_folder + '/matched_trip' + str(tid) + '.csv', sep=' ',
                                   names=['x', 'y', 't', 'x0', 'y0', 'x1', 'y1'])
        matched_trip = matched_trip[matched_trip.t % 5 == 1]
        matched_trip = matched_trip.replace('unknown', 0)
        matched_trip.fillna(0)
        # print tid, len(trip), len(matched_trip), len(matched_trip[matched_trip.x0 == 'unknown'])
        for idx, row in matched_trip.iterrows():
            if row.x0 == 0:
                xs.append(0)
                ys.append(0)
                continue
            line = LineString([(float(row.x0), float(row.y0)), (float(row.x1), float(row.y1))])
            p = Point(row.x, row.y)
            _y, _x = list(line.interpolate(line.project(p)).coords)[0]
            xs.append(_x)
            ys.append(_y)
    data['mLon'] = xs
    data['mLat'] = ys

    # naive fillna
    xs = []
    ys = []
    for idx, row in data.iterrows():
        if row.mLon == 0:
            xs.append(xs[-1])
            ys.append(ys[-1])
        else:
            xs.append(row.mLon)
            ys.append(row.mLat)
    data.mLon = xs
    data.mLat = ys

    return data


def get_matched_trip_smooth(data, matched_folder):
    xs, ys, mx0, mx1, my0, my1 = ([], [], [], [], [], [])
    for tid in data.tid.unique():
        matched_trip = pd.read_csv(matched_folder + '/matched_trip' + str(tid) + '.csv', sep=' ',
                                   names=['y', 'x', 't', 'y0', 'x0', 'y1', 'x1'])
        matched_trip = matched_trip[matched_trip.t % 5 == 1]
        matched_trip = matched_trip.replace('unknown', 0)
        matched_trip.fillna(0)
        # print tid, len(trip), len(matched_trip), len(matched_trip[matched_trip.x0 == 'unknown'])
        for idx, row in matched_trip.iterrows():
            # print row
            if row.y0 == 0 or row.x0 == 0:
                xs.append(0)
                ys.append(0)
                mx0.append(0)
                my0.append(0)
                mx1.append(0)
                my1.append(0)
                '''
                xs.append(xs[-1])
                ys.append(ys[-1])
                mx0.append(mx0[-1])
                my0.append(my0[-1])
                mx1.append(mx1[-1])
                my1.append(my1[-1])
                '''
                continue
            line = LineString([(float(row.x0), float(row.y0)), (float(row.x1), float(row.y1))])
            mx0.append(float(row.x0))
            my0.append(float(row.y0))
            mx1.append(float(row.x1))
            my1.append(float(row.y1))
            p = Point(row.x, row.y)
            # print row.x, row.y,
            # print list(line.interpolate(line.project(p)).coords)
            _x, _y = list(line.interpolate(line.project(p)).coords)[0]
            xs.append(_x)
            ys.append(_y)
    data['mLon'] = xs
    data['mLat'] = ys
    data['mx0'] = mx0
    data['my0'] = my0
    data['mx1'] = mx1
    data['my1'] = my1

    '''
    # naive fillna
    xs = []
    ys = []
    for idx, row in data.iterrows():
        if row.mLon == 0:
            xs.append(xs[-1])
            ys.append(ys[-1])
        else:
            xs.append(row.mLon)
            ys.append(row.mLat)
    data.mLon = xs
    data.mLat = ys
    '''

    return data


"""
def get_matched_trip_smooth(data):
    xs, ys, mx0, mx1, my0, my1 = ([], [], [], [], [], [])
    ys = []
    for tid in data.tid.unique():
        global matched_trip
        matched_trip = pd.read_csv('matched/matched_trip'+str(tid)+'.csv', sep=' ', names=['y','x','t','y0','x0','y1','x1'])
        matched_trip = matched_trip[matched_trip.t % 5 == 1]
        matched_trip = matched_trip.replace('unknown', 0)
        matched_trip.fillna(0)
        #print tid, len(trip), len(matched_trip), len(matched_trip[matched_trip.x0 == 'unknown'])
        for idx, row in matched_trip.iterrows():
            if row.y0 == 0:
                xs.append(xs[-1])
                ys.append(ys[-1])
                mx0.append(mx0[-1])
                my0.append(my0[-1])
                mx1.append(mx1[-1])
                my1.append(my1[-1])
                continue
            line = LineString([(float(row.y0),float(row.x0)),(float(row.y1),float(row.x1))])
            mx0.append(float(row.x0))
            my0.append(float(row.y0))
            mx1.append(float(row.x1))
            my1.append(float(row.y1))
            p = Point(row.y, row.x)
            #print row.y, row.x, row.y0, row.x0, row
            #print list(line.interpolate(line.project(p)).coords)
            _y, _x = list(line.interpolate(line.project(p)).coords)[0]
            xs.append(_x)
            ys.append(_y)
    data['mLon'] = xs
    data['mLat'] = ys
    data['mx0'] = mx0
    data['my0'] = my0
    data['mx1'] = mx1
    data['my1'] = my1

    '''
    # naive fillna
    xs = []
    ys = []
    for idx, row in data.iterrows():
        if row.mLon == 0:
            xs.append(xs[-1])
            ys.append(ys[-1])
        else:
            xs.append(row.mLon)
            ys.append(row.mLat)
    data.mLon = xs
    data.mLat = ys
    '''

    return data
"""


def mean_filter(data, window):
    _w = (window - 1) / 2
    xs = []
    ys = []
    t, p, n = {}, {}, {}
    for tid in data.tid.unique():
        for i in range(_w):
            xs.append(data[data.tid == tid].mLon.values[i])
            ys.append(data[data.tid == tid].mLat.values[i])
        # xs.append(data[data.tid == tid].mLon.values[0])
        # ys.append(data[data.tid == tid].mLat.values[0])
        for i in data[data.tid == tid].index[_w:-1 * _w]:
            t['x0'], t['x1'], t['y0'], t['y1'] = (data.mx0[i], data.mx1[i], data.my0[i], data.my1[i])
            p['x0'], p['x1'], p['y0'], p['y1'] = (data.mx0[i - 1], data.mx1[i - 1], data.my0[i - 1], data.my1[i - 1])
            n['x0'], n['x1'], n['y0'], n['y1'] = (data.mx0[i + 1], data.mx1[i + 1], data.my0[i + 1], data.my1[i + 1])
            '''
            px, py = data.mLon[i-1], data.mLat[i-1]
            nx, ny = data.mLon[i+1], data.mLat[i+1]
            tx, ty = data.mLon[i], data.mLat[i]
            #mx = (px+nx)/2.0
            #my = (py+ny)/2.0
            mx = (px+nx+tx)/3.0
            my = (py+ny+ty)/3.0
            '''
            mx = data.mLon[i - _w:i + _w + 1].sum() / window
            my = data.mLat[i - _w:i + _w + 1].sum() / window
            '''
            if (p['x0'] == n['x0']) & (p['x1'] == n['x1']) & (p['y1'] == n['y1']) & (p['y0'] == n['y0']):
                xs.append(mx)
                ys.append(my)
            else:
            '''
            _d = 999999
            for l in [t, p, n]:
                if dist((mx, my), l) < _d:
                    _x, _y = project((mx, my), l)
                    _d = dist((mx, my), l)
            # _y, _x = project((mx, my), t)
            xs.append(_x)
            ys.append(_y)
        for i in range(_w * -1, 0):
            xs.append(data[data.tid == tid].mLon.values[i])
            ys.append(data[data.tid == tid].mLat.values[i])
        # xs.append(data[data.tid == tid].mLon.values[-1])
        # ys.append(data[data.tid == tid].mLat.values[-1])
        # print xs[-1], ys[-1]
    data.mLat = ys
    data.mLon = xs
    return data


def evaluate(x0, y0, x1, y1):
    mDist = []
    for tx, ty, x, y in zip(x0, y0, x1, y1):
        mDist.append(train.compute_error([x, y], [tx, ty]))
    mDist = sorted(mDist)
    print sum(mDist) / len(mDist), mDist[int(len(mDist) * 0.5)], mDist[int(len(mDist) * 0.67)], mDist[int(len(mDist) * 0.8)], mDist[
        int(len(mDist) * 0.9)]


def back_not_null(column, idx):
    idx += 1
    while column[idx] == 0:
        idx += 1
    return column[idx]


def forward_not_null(column, idx):
    idx -= 1
    while column[idx] == 0:
        idx -= 1
    return column[idx]


def fill_na(data):
    data.index = range(len(data))
    null = data[data.mLat == 0]
    for tid in null.tid.unique():
        for idx in null[null.tid == tid].index:
            if idx == 0 or data.tid[idx - 1] != data.tid[idx]:
                # data.mLon[idx] = data.mLon[idx+1]
                # data.mLat[idx] = data.mLat[idx+1]
                data.mLon[idx] = back_not_null(data.mLon, idx)
                data.mLat[idx] = back_not_null(data.mLat, idx)
            elif data.tid[idx + 1] != data.tid[idx]:
                # data.mLon[idx] = data.mLon[idx-1]
                # data.mLat[idx] = data.mLat[idx-1]
                data.mLon[idx] = forward_not_null(data.mLon, idx)
                data.mLat[idx] = forward_not_null(data.mLat, idx)
            else:
                # data.mLon[idx] = (data.mLon[idx+1]+data.mLon[idx-1])/2.0
                # data.mLat[idx] = (data.mLat[idx+1]+data.mLat[idx-1])/2.0
                data.mLon[idx] = (back_not_null(data.mLon, idx) + forward_not_null(data.mLon, idx)) / 2.0
                data.mLat[idx] = (back_not_null(data.mLat, idx) + forward_not_null(data.mLat, idx)) / 2.0
                # data.mLat[idx] = (data.mLat[idx+1]+data.mLat[idx-1])/2.0
    return data
