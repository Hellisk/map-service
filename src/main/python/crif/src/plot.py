from collections import defaultdict

import matplotlib.cm as matcm
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import sys
# %matplotlib inline
from matplotlib import lines
from matplotlib.patches import Arrow
from matplotlib.patches import Circle
from matplotlib.patches import Rectangle

import trajmap


def degree((x0, y0), (x1, y1)):
    from math import atan, pi
    dx = x1 - x0
    dy = y1 - y0
    if dx == 0:
        if dy == 0:
            ret = 0
        elif dy > 0:
            ret = pi / 2
        elif dy < 0:
            ret = 3 * pi / 2
    elif dx >= 0 and dy >= 0:
        ret = atan(dy / dx)
    elif dx < 0 and dy >= 0:
        ret = pi - atan((-1 * dy) / dx)
    elif dx < 0 and dy < 0:
        ret = pi + atan(dy / dx)
    elif dx >= 0 and dy < 0:
        ret = 2 * pi - atan(dy / (-1 * dx))
    return ret / pi * 180.0


def get_rg(x0, y0, x1, y1):
    deg = degree((x0, y0), (x1, y1))
    r = deg
    g = deg - 90.0
    if r > 180:
        r = 360 - r
    if g < 0:
        g += 360
    if g > 180:
        g = 360 - g
    return r / 180.0, g / 180.0


def num(mat, h=0, w=0, size=10.0):
    if type(mat) == np.ndarray:
        sag = mat
    else:
        h, w = mat.height, mat.width
        sag = mat.sag
    num = trajmap.get_num(mat, h, w)
    num_mat = np.zeros(shape=(h + 1, w + 1))
    for c in num.keys():
        num_mat[h - (int(c / w))][c % w] = num[c]
    # return num_mat
    plt.figure(figsize=(size, size / (w) * (h)))
    plt.imshow(num_mat)
    # plt.matshow(num_mat, cmap = matcm.gray)
    plt.show()
    return num_mat


def bool(mat, h=0, w=0, size=10):
    if type(mat) == np.ndarray:
        sag = mat
    else:
        h, w = mat.height, mat.width
        sag = mat.sag
    num = defaultdict(int)
    for c in sag.nonzero()[0]:
        num[c] = 1
    num_mat = np.ndarray(shape=(h, w))
    num_mat[:, :] = 0
    for c in num.keys():
        num_mat[h - (int(c / w))][c % w] = num[c]
    plt.figure(figsize=(size, size / (mat.x1 - mat.x0) * (mat.y1 - mat.y0)))
    # plt.imshow(num_mat, cmap = matcm.gray)
    plt.imshow(num_mat, cmap=matcm.gray)
    plt.show()


def trajectory(xs, ys, axis, figsize=(10, 10)):
    plt.figure(figsize=figsize)
    currentAxis = plt.gca()
    for a, b in trajmap.pairwise(zip(xs, ys)):
        ix, iy, jx, jy = a[0], a[1], b[0], b[1]
        arr = Arrow(ix, iy, jx - ix, jy - iy, width=0.0005, color='k')
        currentAxis.add_patch(arr)
    plt.axis(axis)
    plt.show()


def vector(mat, v, size=10):
    m = np.zeros(shape=(mat.height, mat.width))
    for c, value in zip(range(len(v)), v):
        h = mat.height - (c / mat.width) - 1
        w = c % mat.width
        m[h][w] = value
    plt.figure(figsize=(size, size * mat.height / mat.width))
    # plt.imshow(m, cmap = matcm.gray)
    plt.imshow(m)
    plt.show()


def matshow(mat, kv, ca=None, show=False, size=10):
    if type(kv) is not dict:
        kv = dict.fromkeys(kv, 1)
    m = np.zeros(shape=(mat.height, mat.width))
    for c in kv.keys():
        h = mat.height - (c / mat.width) - 1
        w = c % mat.width
        m[h][w] = kv[c]
    if show:
        if ca == None:
            plt.figure(figsize=(size, size * mat.height / mat.width))
            ca = plt.gca()
            plt.imshow(m, cmap=matcm.gray)
        else:
            plt.imshow(m, cmap=matcm.gray)
    # return m


def dots(mat, pr, size=10, ca=None, gps=False, rad=1.0, color='k'):
    if ca == None:
        plt.figure(figsize=(size, size * mat.height / mat.width))
        currentAxis = plt.gca()
    else:
        currentAxis = ca
    # M = Ms[-1]
    if len(pr) == 0:
        return
    if type(pr) is not dict:
        pr = dict.fromkeys(pr, 1)
    if abs(1 - max(pr.values())) > 0.05:
        _max = max(pr.values())
        _min = min(pr.values())
        for k in pr.keys():
            pr[k] = 1.0 * (pr[k] - _min) / (_max - _min)
    df = pd.DataFrame()
    df['k'] = pr.keys()
    df['v'] = pr.values()
    if gps:
        xs, ys = [], []
        for c in pr.keys():
            _x, _y = trajmap.get_true_gps(mat, c)
            xs.append(_x)
            ys.append(_y)
        df['x'] = xs
        df['y'] = ys
    else:
        df['y'] = df['k'].floordiv(mat.width)
        df['x'] = df['k'].mod(mat.width)
    _rad = (ca.axis()[1] - ca.axis()[0]) / 200.0
    # _rad = (max(xs) - min(xs))/200.0
    # currentAxis.plot(df.x, df.y, 'bo', alpha=0.8)
    # '''
    print len(df)
    for x, y, v in zip(df.x, df.y, df.v):
        # Tracer()()
        circle = Circle((x, y), radius=_rad * rad, color=color, alpha=v)
        currentAxis.add_patch(circle)
    '''
    for idx, row in df.iterrows():
        circle = Circle((row.x,row.y), radius=_rad*rad, color=color, alpha=row.v)
        currentAxis.add_patch(circle)
    '''
    # '''
    if ca == None:
        if gps:
            plt.axis('tight')
            print plt.axis()
            plt.show()
        else:
            plt.axis([0, mat.width, 0, mat.height])
            print plt.axis()
            plt.show()
    else:
        return currentAxis


def grids(mat, pr, size=10, ca=None, gps=False, color='k'):
    if ca == None:
        plt.figure(figsize=(size, size * mat.height / mat.width))
        currentAxis = plt.gca()
    else:
        currentAxis = ca
    # M = Ms[-1]
    if len(pr) == 0:
        return
    if type(pr) is not dict:
        pr = dict.fromkeys(pr, 1)
    if abs(1 - max(pr.values())) > 0.05:
        _max = max(pr.values())
        _min = min(pr.values())
        for k in pr.keys():
            pr[k] = 1.0 * (pr[k] - _min) / (_max - _min)
    df = pd.DataFrame()
    df['k'] = pr.keys()
    df['v'] = pr.values()
    if gps:
        xs, ys = [], []
        for c in pr.keys():
            _x, _y = trajmap.get_true_gps(mat, c)
            xs.append(_x)
            ys.append(_y)
        df['x'] = xs
        df['y'] = ys
    else:
        df['y'] = df['k'].floordiv(mat.width)
        df['x'] = df['k'].mod(mat.width)
    # _rad = (max(xs) - min(xs))/200.0
    # currentAxis.plot(df.x, df.y, 'bo', alpha=0.8)
    # '''
    print len(df)
    for x, y, v in zip(df.x, df.y, df.v):
        # Tracer()()
        circle = Rectangle((x - 0.5 * mat.side, y - 0.5 * mat.side), width=mat.side, height=mat.side, color=color, alpha=v)
        currentAxis.add_patch(circle)
    '''
    for idx, row in df.iterrows():
        circle = Circle((row.x,row.y), radius=_rad*rad, color=color, alpha=row.v)
        currentAxis.add_patch(circle)
    '''
    # '''
    if ca == None:
        if gps:
            plt.axis('tight')
            print plt.axis()
            plt.show()
        else:
            plt.axis([0, mat.width, 0, mat.height])
            print plt.axis()
            plt.show()
    else:
        return currentAxis


def M(mat, G, gps, ca=None, true_gps=False, sizerate=0.05, arrow=False):
    if ca is None:
        plt.figure(figsize=(int(mat.width * sizerate), int(mat.height * sizerate)))
        currentAxis = plt.gca()
    else:
        currentAxis = ca
    N = mat.height * mat.width
    for a, b in G.edges_iter():
        if true_gps is True:
            ix, iy = trajmap.get_true_gps(mat, a, gps)
            jx, jy = trajmap.get_true_gps(mat, b, gps)
        else:
            ix, iy = trajmap.get_gps(mat, a, gps)
            jx, jy = trajmap.get_gps(mat, b, gps)
            # currentAxis.text(ix+1, iy+1, a)
            # currentAxis.text(ix-1, iy-1, b)
            if ix >= mat.width:
                continue
            if iy >= mat.height:
                continue
        if arrow:
            arr = Arrow(ix, iy, jx - ix, jy - iy, hatch='+', linewidth=1, color='k')
            currentAxis.add_patch(arr)
        else:
            line = lines.Line2D([ix, jx], [iy, jy], linewidth=1, color='k')
            currentAxis.add_line(line)
    if ca is None:
        plt.axis([0, mat.width, 0, mat.height])
        plt.show()


def graph(mat, G, size=10, ca=None, color='k', linewidth=1, alpha=False, arrow=False, use_gps=False, axis=None):
    if ca is not None:
        currentAxis = ca
    else:
        plt.figure(figsize=(size, int(size * mat.height / mat.width)))
        currentAxis = plt.gca()

    for a, b in G.edges_iter():
        if use_gps:
            ix, iy = trajmap.get_true_gps(mat, a, {})
            jx, jy = trajmap.get_true_gps(mat, b, {})
        else:
            iy = (int(a / mat.width))
            ix = a % mat.width
            jy = (int(b / mat.width))
            jx = b % mat.width
            if ix >= mat.width:
                continue
            if iy >= mat.height:
                continue
        if arrow:
            arr = Arrow(ix, iy, jx - ix, jy - iy, hatch='+', linewidth=linewidth, color=color, alpha=G[a][b]['weight'])
            currentAxis.add_patch(arr)
        elif alpha:
            line = lines.Line2D([ix, jx], [iy, jy], linewidth=linewidth, color=color, alpha=G[a][b]['weight'])
            currentAxis.add_line(line)
        else:
            line = lines.Line2D([ix, jx], [iy, jy], linewidth=linewidth, color=color)
            currentAxis.add_line(line)
    if not use_gps:
        plt.axis([0, mat.width, 0, mat.height])
    elif axis is not None:
        plt.axis(axis)


def graph_dots(mat, G, pr, size=10, ca=None, arrow=False):
    if ca is not None:
        currentAxis = ca
    else:
        plt.figure(figsize=(size, int(size * mat.height / mat.width)))
        currentAxis = plt.gca()

    for a, b in G.edges_iter():
        iy = (int(a / mat.width))
        ix = a % mat.width
        jy = (int(b / mat.width))
        jx = b % mat.width
        if ix >= mat.width:
            continue
        if iy >= mat.height:
            continue
        if arrow:
            arr = Arrow(ix, iy, jx - ix, jy - iy, hatch='+', linewidth=1, color='k')
            currentAxis.add_patch(arr)
        else:
            line = lines.Line2D([ix, jx], [iy, jy], linewidth=1, color='k')
            currentAxis.add_line(line)
    if type(pr) is not dict:
        pr = dict.fromkeys(pr, 1)
    '''
    if abs(1 - max(pr.values())) > 0.05:
        _max = max(pr.values())
        _min = min(pr.values())
        for k in pr.keys():
            pr[k] = 1.0 * (pr[k] - _min)/(_max-_min)
    '''
    for k, v in pr.iteritems():
        # print v
        iy = (int(k / mat.width))
        ix = k % mat.width
        if ix >= mat.width:
            continue
        # circle = Circle((ix,iy), radius=0.3, color=svd.cnames.values()[int(v)])
        circle = Circle((ix, iy), radius=0.3, color='r')
        currentAxis.add_patch(circle)
    plt.axis([0, mat.width, 0, mat.height])
    plt.show()


def map_df(map_df, ca=None, axis=None, color='b', linewidth=1):
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
