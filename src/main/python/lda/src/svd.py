import matplotlib

matplotlib.use('Agg')
import bitlist
import pandas as pd
import numpy as np
import math
import pickle
import matplotlib.pyplot as plt
from matplotlib.patches import Rectangle
import os

from sparsesvd import sparsesvd
import scipy.sparse
from collections import defaultdict

cnames = {
    'aliceblue': '#F0F8FF',
    'antiquewhite': '#FAEBD7',
    'aqua': '#00FFFF',
    'aquamarine': '#7FFFD4',
    'azure': '#F0FFFF',
    'beige': '#F5F5DC',
    'bisque': '#FFE4C4',
    'black': '#000000',
    'blanchedalmond': '#FFEBCD',
    'blue': '#0000FF',
    'blueviolet': '#8A2BE2',
    'brown': '#A52A2A',
    'burlywood': '#DEB887',
    'cadetblue': '#5F9EA0',
    'chartreuse': '#7FFF00',
    'chocolate': '#D2691E',
    'coral': '#FF7F50',
    'cornflowerblue': '#6495ED',
    'cornsilk': '#FFF8DC',
    'crimson': '#DC143C',
    'cyan': '#00FFFF',
    'darkblue': '#00008B',
    'darkcyan': '#008B8B',
    'darkgoldenrod': '#B8860B',
    'darkgray': '#A9A9A9',
    'darkgreen': '#006400',
    'darkkhaki': '#BDB76B',
    'darkmagenta': '#8B008B',
    'darkolivegreen': '#556B2F',
    'darkorange': '#FF8C00',
    'darkorchid': '#9932CC',
    'darkred': '#8B0000',
    'darksalmon': '#E9967A',
    'darkseagreen': '#8FBC8F',
    'darkslateblue': '#483D8B',
    'darkslategray': '#2F4F4F',
    'darkturquoise': '#00CED1',
    'darkviolet': '#9400D3',
    'deeppink': '#FF1493',
    'deepskyblue': '#00BFFF',
    'dimgray': '#696969',
    'dodgerblue': '#1E90FF',
    'firebrick': '#B22222',
    'floralwhite': '#FFFAF0',
    'forestgreen': '#228B22',
    'fuchsia': '#FF00FF',
    'gainsboro': '#DCDCDC',
    'ghostwhite': '#F8F8FF',
    'gold': '#FFD700',
    'goldenrod': '#DAA520',
    'gray': '#808080',
    'green': '#008000',
    'greenyellow': '#ADFF2F',
    'honeydew': '#F0FFF0',
    'hotpink': '#FF69B4',
    'indianred': '#CD5C5C',
    'indigo': '#4B0082',
    'ivory': '#FFFFF0',
    'khaki': '#F0E68C',
    'lavender': '#E6E6FA',
    'lavenderblush': '#FFF0F5',
    'lawngreen': '#7CFC00',
    'lemonchiffon': '#FFFACD',
    'lightblue': '#ADD8E6',
    'lightcoral': '#F08080',
    'lightcyan': '#E0FFFF',
    'lightgoldenrodyellow': '#FAFAD2',
    'lightgreen': '#90EE90',
    'lightgray': '#D3D3D3',
    'lightpink': '#FFB6C1',
    'lightsalmon': '#FFA07A',
    'lightseagreen': '#20B2AA',
    'lightskyblue': '#87CEFA',
    'lightslategray': '#778899',
    'lightsteelblue': '#B0C4DE',
    'lightyellow': '#FFFFE0',
    'lime': '#00FF00',
    'limegreen': '#32CD32',
    'linen': '#FAF0E6',
    'magenta': '#FF00FF',
    'maroon': '#800000',
    'mediumaquamarine': '#66CDAA',
    'mediumblue': '#0000CD',
    'mediumorchid': '#BA55D3',
    'mediumpurple': '#9370DB',
    'mediumseagreen': '#3CB371',
    'mediumslateblue': '#7B68EE',
    'mediumspringgreen': '#00FA9A',
    'mediumturquoise': '#48D1CC',
    'mediumvioletred': '#C71585',
    'midnightblue': '#191970',
    'mintcream': '#F5FFFA',
    'mistyrose': '#FFE4E1',
    'moccasin': '#FFE4B5',
    'navajowhite': '#FFDEAD',
    'navy': '#000080',
    'oldlace': '#FDF5E6',
    'olive': '#808000',
    'olivedrab': '#6B8E23',
    'orange': '#FFA500',
    'orangered': '#FF4500',
    'orchid': '#DA70D6',
    'palegoldenrod': '#EEE8AA',
    'palegreen': '#98FB98',
    'paleturquoise': '#AFEEEE',
    'palevioletred': '#DB7093',
    'papayawhip': '#FFEFD5',
    'peachpuff': '#FFDAB9',
    'peru': '#CD853F',
    'pink': '#FFC0CB',
    'plum': '#DDA0DD',
    'powderblue': '#B0E0E6',
    'purple': '#800080',
    'red': '#FF0000',
    'rosybrown': '#BC8F8F',
    'royalblue': '#4169E1',
    'saddlebrown': '#8B4513',
    'salmon': '#FA8072',
    'sandybrown': '#FAA460',
    'seagreen': '#2E8B57',
    'seashell': '#FFF5EE',
    'sienna': '#A0522D',
    'silver': '#C0C0C0',
    'skyblue': '#87CEEB',
    'slateblue': '#6A5ACD',
    'slategray': '#708090',
    'snow': '#FFFAFA',
    'springgreen': '#00FF7F',
    'steelblue': '#4682B4',
    'tan': '#D2B48C',
    'teal': '#008080',
    'thistle': '#D8BFD8',
    'tomato': '#FF6347',
    'turquoise': '#40E0D0',
    'violet': '#EE82EE',
    'wheat': '#F5DEB3',
    'white': '#FFFFFF',
    'whitesmoke': '#F5F5F5',
    'yellow': '#FFFF00',
    'yellowgreen': '#9ACD32'
}

color_map = {}
for i, k in zip(range(len(cnames)), cnames.keys()):
    color_map[i] = k


def build_trajectory_img(traj_num, depth, dtype, n_cluster):
    G = bitlist.read_AG(traj_num, depth, dtype, 'new', 1024)
    itv, gl = bitlist.read_Tl(traj_num, depth, 'g', dtype, 'new', 1024)
    U = get_U(G, n_cluster)
    dwg = gl.get_depth_with_gps()

    for c in range(10):
        print c, dtype, traj_num, depth, n_cluster
        plt.figure(figsize=(14, 14))
        currentAxis = plt.gca()

        maxc = U[:, c].max()
        minc = U[:, c].min()
        for line in dwg:
            x0, x1, y0, y1 = line[2], line[3], line[4], line[5]
            x, y, w, h = x0, y0, x1 - x0, y1 - y0
            # print x0, x1, y0, y1
            idx = line[0]
            _color = U[idx][c]
            currentAxis.add_patch(Rectangle((x, y),
                                            w,
                                            h,
                                            linewidth=0,
                                            facecolor=str((_color - minc) / (maxc - minc))))

        plt.axis([121.3, 121.6, 31.1, 31.4])
        plt.savefig('./img/traj_%s_%d_%d_%d_(%d).png' % (dtype, traj_num, depth, n_cluster, c))


def build_map(traj_num, depth, dtype, n_cluster, version='new'):
    G, dwg = read_SAG_DWG(traj_num, depth, dtype, version)
    U = get_U(G, n_cluster)
    cluster = {}
    for i in range(len(U)):
        _c = 0
        for j in range(len(U[i])):
            if U[i][j] > U[i][_c]:
                _c = j
        cluster[i] = _c

    plt.figure(figsize=(14, 14))
    currentAxis = plt.gca()

    if dtype.startswith('sh'):
        X0, X1, Y0, Y1 = 121.3, 121.6, 31.1, 31.4
    else:
        X0, X1, Y0, Y1 = 119.35, 119.55, 32.1, 32.3

    cnt = 1
    total = len(dwg)
    for line in dwg:
        if cnt % (total / 10) == 0:
            print cnt, total
        cnt += 1
        x0, x1, y0, y1 = line[2], line[3], line[4], line[5]
        if x0 < X0 or x1 > X1 or y0 < Y0 or y1 > Y1:
            continue
        x, y, w, h = x0, y0, x1 - x0, y1 - y0
        # print x0, x1, y0, y1
        idx = line[0]
        c = cluster[idx]
        currentAxis.add_patch(Rectangle((x, y), w, h, linewidth=0, facecolor=color_map[c]))

    plt.axis([X0, X1, Y0, Y1])
    plt.savefig('./img/map_%s_%d_%d_%d_%s.png' % (dtype, traj_num, depth, n_cluster, version))


def get_U(A, n):
    smat = scipy.sparse.csc_matrix(A)
    ut, s, vt = sparsesvd(smat, n)
    print ut.shape, s.shape, vt.shape
    # print numpy.std(A - numpy.dot(ut.T, numpy.dot(numpy.diag(s), vt)))
    return ut.T


def compute_SAG(data, depth):
    dx = 360.0 / (2 ** depth)
    dy = 180.0 / (2 ** depth)
    cg_map = {}
    dwg = []
    cg_keys = set()
    cnt = 0
    cid = 0
    row = []
    col = []
    total = len(data)
    for idx, line in data.iterrows():
        _x, _y, _tid = line.x, line.y, line.tid
        if cnt % int(total / 10) == 0:
            print cnt, total
        tup = (math.floor(_x / dx), math.floor(_y / dy))
        if tup not in cg_keys:
            cg_map[tup] = cid
            cg_keys.add(tup)
            dwg.append((cid, 18, tup[0] * dx, (tup[0] + 1) * dx, tup[1] * dy, (tup[1] + 1) * dy))
            cid += 1
        row.append(cg_map[tup])
        col.append(_tid)
        cnt += 1
    sag = scipy.sparse.csc_matrix(([1] * len(row), (row, col)),
                                  shape=(max(row) + 1, max(col) + 1))
    return sag, dwg


def compute_dense_SAG(data, depth):
    dx = 360.0 / (2 ** depth)
    w = dx
    dy = dx
    # dy = 180.0/(2**depth)
    cg_map = {}
    dwg = []
    cg_keys = set()
    cnt = 0
    cid = 0
    row = []
    col = []
    total = len(data)

    _x = -1
    _y = -1
    _tid = -1

    for idx, line in data.iterrows():
        x, y, tid = line.x, line.y, line.tid
        if cnt % int(total / 10) == 0:
            print cnt, total

        if _tid == -1 or _tid != tid:
            _x = x
            _y = y
            _tid = tid
            continue

        ans = []

        if _x == x:
            k = 99999
        else:
            k = 1.0 * (_y - y) / (_x - x)
        b = y - k * x

        if abs(k) <= 0:
            step = (x - _x) / abs(x - _x) * w
            # print 'k: %f, b: %f, step: %f' % (k, b, step)
            # print np.arange(_x, x+step, step)
            for x_ in np.arange(_x, x + step, step):
                x_ = x_ + w * 0.5
                y_ = int((k * x_ + b) / w)
                x_ = int(x_ / w)
                # print x_, y_
                ans.append((x_, y_))
        else:
            step = (y - _y) / abs(y - _y) * w
            # print 'k: %f, b: %f, step: %f' % (k, b, step)
            # print np.arange(_y, y+step, step)
            for y_ in np.arange(_y, y + step, step):
                y_ = y_ + w * 0.5
                x_ = int((y_ - b) / k / w)
                y_ = int(y_ / w)
                # print x_, y_
                ans.append((x_, y_))

        for tup in ans:
            # tup = (math.floor(x/dx), math.floor(y/dy))
            if tup not in cg_keys:
                cg_map[tup] = cid
                cg_keys.add(tup)
                dwg.append((cid, depth, tup[0] * dx, (tup[0] + 1) * dx, tup[1] * dy, (tup[1] + 1) * dy))
                cid += 1
            row.append(cg_map[tup])
            col.append(_tid)

        cnt += 1

        _x = x
        _y = y

    sag = scipy.sparse.csc_matrix(([1] * len(row), (row, col)),
                                  shape=(max(row) + 1, max(col) + 1))
    return sag, dwg


def compute_dense_SAG_TOG(data, depth):
    X0, X1, Y0, Y1 = (119.4, 119.5, 32.15, 32.25)
    dx = 360.0 / (2 ** depth)
    w = dx
    dy = dx
    # dy = 180.0/(2**depth)
    cg_map = {}
    # dwg = []
    cg_keys = set()
    cnt = 0
    cid = 0
    row = []
    col = []
    total = len(data)
    length = int(X1 / dx) - int(X0 / dx) + 1
    x0 = int(X0 / dx)
    y0 = int(Y0 / dx)
    # print x0, y0

    tog = defaultdict(lambda: defaultdict(set))

    _x = -1
    _y = -1
    _tid = -1

    for idx, line in data.iterrows():
        x, y, tid = line.x, line.y, line.tid
        if cnt % int(total / 10) == 0:
            print cnt, total
        cnt += 1

        if x <= X0 or x >= X1 or y <= Y0 or y >= Y1:
            _tid = -1
            continue

        if _tid == -1 or _tid != tid:
            _x = x
            _y = y
            _tid = tid
            continue

        ans = []

        # print cnt, _x, _y, x, y
        if (_x == x) and (_y == y):
            # print '2', cnt, _x, _y, x, y
            continue
        # elif (_x == x):
        #    print '1', cnt, _x, _y, x, y
        #    _tid = -1
        #    continue
        else:
            if (_x == x):
                k = 99999
            else:
                k = 1.0 * (_y - y) / (_x - x)
            b = y - k * x
            if abs(k) <= 1:
                step = (x - _x) / abs(x - _x) * w
                # print 'k: %f, b: %f, step: %f' % (k, b, step)
                # print np.arange(_x, x+step, step)
                for x_ in np.arange(_x, x + step, step):
                    x_ = x_ + w * 0.5
                    ox_ = x_
                    oy_ = (k * x_ + b)
                    if ox_ <= X0 or ox_ >= X1 or oy_ <= Y0 or oy_ >= Y1:
                        continue
                    y_ = int((k * x_ + b) / w)
                    x_ = int(x_ / w)
                    # print x_, y_
                    ans.append((x_ - x0) + (y_ - y0) * length)
                    # if cnt >= 68:
                    #    print cnt, ans[-1], x_, y_, ox_, oy_
            else:
                step = (y - _y) / abs(y - _y) * w
                # print 'k: %f, b: %f, step: %f' % (k, b, step)
                # print np.arange(_y, y+step, step)
                for y_ in np.arange(_y, y + step, step):
                    y_ = y_ + w * 0.5
                    oy_ = y_
                    ox_ = (y_ - b) / k
                    if ox_ <= X0 or ox_ >= X1 or oy_ <= Y0 or oy_ >= Y1:
                        continue
                    x_ = int((y_ - b) / k / w)
                    y_ = int(y_ / w)
                    # print x_, y_
                    ans.append((x_ - x0) + (y_ - y0) * length)
                    # if ox_ > 119.42:
                    # if cnt >= 68:
                    #    print cnt, ans[-1], x_, y_, ox_, oy_

        _cid = -1

        for cid in ans:
            row.append(cid)
            col.append(tid)

            if _cid != -1:
                tog[_cid][cid].add(tid)
                _cid = cid
            _cid = cid

        _x = x
        _y = y

    sag = scipy.sparse.csc_matrix(([1] * len(row), (row, col)),
                                  shape=(max(row) + 1, max(col) + 1))
    return sag, dict(tog)
    # return sag, tog


def read_SAG_DWG(traj_num, depth, dtype, version):
    sdir = './data/'
    if version == 'new':
        file_name = 'SAG_%s_%d_%d.pickle' % (dtype, traj_num, depth)
    elif version == 'dense':
        file_name = 'SAG_%s_%d_%d_d.pickle' % (dtype, traj_num, depth)
    elif version == 'tog':
        file_name = 'SAG_%s_%d_%d_t.pickle' % (dtype, traj_num, depth)
    files = os.listdir(sdir)
    if file_name not in files:
        data = bitlist.read_D(traj_num, dtype, 'new')
        if version == 'new':
            sag, dwg = compute_SAG(data, depth)
        elif version == 'dense':
            sag, dwg = compute_dense_SAG(data, depth)
        elif version == 'tog':
            sag, tog = compute_dense_SAG_TOG(data, depth)
        with open(sdir + file_name, 'w') as f:
            if version == 'tog':
                pickle.dump([sag, tog], f)
            else:
                pickle.dump([sag, dwg], f)
    else:
        with open(sdir + file_name, 'r') as f:
            return pickle.load(f)


def draw():
    cids = list(set(tog2.keys()))
    print len(cids)

    u, s, v = get_USV(sag2, 3)
    X0, X1, Y0, Y1 = (119.4, 119.5, 32.15, 32.25)
    dx = 360.0 / (2 ** depth)
    length = int(X1 / dx) - int(X0 / dx) + 1

    print dx, length

    px0 = int(X0 / dx)
    py0 = int(Y0 / dx)
    print px0, py0

    num = defaultdict(int)
    for c in sag.nonzero()[0]:
        num[c] += 1
    _num = []
    for c in cids:
        _num.append(num[c])

    cells = pd.DataFrame(u[cids])
    cells['num'] = _num
    cells['m0'] = (cells[0] - cells[0].min()) / (cells[0].max() - cells[0].min())
    cells['m1'] = (cells[1] - cells[1].min()) / (cells[1].max() - cells[1].min())
    cells['m2'] = (cells[2] - cells[2].min()) / (cells[2].max() - cells[2].min())
    cells.index = cids
    m0, m1, m2 = (cells['m0'].mean(), cells['m1'].mean(), cells['m2'].mean())
    cells['var'] = (cells['m0'] - m0) ** 2 + (cells['m1'] - m1) ** 2 + (cells['m2'] - m2) ** 2

    cells['draw'] = cells['var'] > 0.001

    X0, X1, Y0, Y1 = (119.4, 119.5, 32.15, 32.25)
    # X0, X1, Y0, Y1 = (119.44, 119.45, 32.20, 32.21)
    plt.figure(figsize=(12, 12))
    currentAxis = plt.gca()

    cnt = 1
    total = len(cells)

    for cid, row in cells.iterrows():
        if cnt % (total / 10) == 0:
            print cnt, total
        cnt += 1

        # if row['num'] < 100:
        #    continue

        if row['draw'] == False:
            continue

        c0, c1, c2 = (row['m0'], row['m1'], row['m2'])
        # if (c0-m0)**2 + (c0-m0)**2 + (c0-m0)**2 < 0.5

        x0 = (cid % length + px0) * dx
        y0 = (int(cid / length) + py0) * dx
        x1 = (cid % length + px0 + 1) * dx
        y1 = (int(cid / length) + py0 + 1) * dx
        if x0 < X0 or x1 > X1 or y0 < Y0 or y1 > Y1:
            continue
        x, y, w, h = x0, y0, x1 - x0, y1 - y0

        currentAxis.add_patch(Rectangle((x, y), w, h, linewidth=0, facecolor=(c0, c1, c2)))
        # currentAxis.add_patch(Rectangle((x, y), w, h, linewidth=0, facecolor=((u[idx][0]+50)/65,(u[idx][1]+50)/65,(u[idx][2]+50)/65)))
        # plt.text(x,y,str('%.3f'%label))
        # plt.text(x,y,str('%.3f'%dist(u[p0][:], u[idx][:])[0][0]))
    # '''
    for i in range(len(shapes)):
        p = shapes[i]
        arr = np.array(p.points)
        arr = arr
        x, y = arr[:, 0], arr[:, 1]
        if x.max() < X0 or x.min() > X1 or y.max() < Y0 or y.min() > Y1:
            continue
        line = lines.Line2D(x, y, linewidth=1, color='b')
        currentAxis.add_line(line)
    # '''

    plt.axis([X0, X1, Y0, Y1])
