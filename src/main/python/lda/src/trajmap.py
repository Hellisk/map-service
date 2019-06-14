import pandas as pd
import numpy as np
import time
import math
import pickle
import random
import matplotlib.pyplot as plt
# %matplotlib inline
import scipy
from sparsesvd import sparsesvd
import scipy.sparse
from collections import defaultdict
import os
import sys
import matplotlib.cm as matcm
from itertools import tee, izip
import networkx as nx
from matplotlib.patches import Arrow
import svd
import plot
from plsa import plsa
import copy


# from plsa.plsa import pLSA

# print 'pLSA cythen extension:', plsa.HAVE_EXT

# import crash_on_ipy

def pairwise(iterable):
    "s -> (s0,s1), (s1,s2), (s2, s3), ..."
    a, b = tee(iterable)
    next(b, None)
    return izip(a, b)


def read_trips_from(dir):
    data = pd.DataFrame()
    tid = 0
    for f in os.listdir(dir):
        if f.endswith('.txt'):
            tmp = pd.read_csv(dir + f, sep=' ', names=['x', 'y', 't'])
            tmp['tid'] = tid
            data = pd.concat([data, tmp])
            tid += 1
    return data


def get_num(mat, h=0, w=0):
    if type(mat) == np.ndarray:
        sag = mat
    else:
        h, w = mat.height, mat.width
        sag = mat.sag
    num = defaultdict(int)
    for c, t in zip(*sag.nonzero()):
        num[c] += sag[c][t]
    return num


def update_M(mat, path_values, max_value=1.0):
    M = nx.DiGraph()
    for path, value in path_values:
        if value > max_value:
            continue
        for a, b in pairwise(path):
            M.add_edge(a, b)
    nodes_set = set(M.nodes())
    for n in nodes_set:
        in_set = set()
        out_set = set()
        for i, _i in M.in_edges(n):
            in_set.add(i)
        for _o, o in M.out_edges(n):
            out_set.add(o)
        if in_set == out_set:
            if len(in_set) == 2:
                in_set = list(in_set)
                M.add_edge(in_set[0], in_set[1])
                M.add_edge(in_set[1], in_set[0])
                M.remove_node(n)
            else:
                # print in_set,'and', out_set
                _z = 1
        elif len(in_set) == 1 and len(out_set) == 1:
            if direct(mat, n, o) == direct(mat, i, n):
                M.add_edge(i, o)
                M.remove_node(n)
    print M.number_of_nodes()
    return M


def dist(mat, a, b, gps):
    ax, ay = get_gps(mat, a, gps)
    bx, by = get_gps(mat, b, gps)
    return math.sqrt((ax - bx) ** 2 + (ay - by) ** 2)


def smooth_M(mat, M, gps, _dist=1.5):
    # use mean node
    _add_node, add_node = 1, 1
    while _add_node != add_node or add_node == 1:
        # print 'add node', add_node
        _add_node = add_node
        N = mat.height * mat.width
        nodes = set()
        for u, v in M.edges():
            if u in nodes or v in nodes:
                continue
            if dist(mat, u, v, gps) < _dist:
                # print u, v, dist(mat, u,v, gps)
                nodes.add(u)
                nodes.add(v)
                # '''
                n = N + add_node
                add_node += 1
                gps[n] = gps_mean(mat, u, v, gps)
                for i, _i in M.in_edges(u):
                    if i == n:
                        continue
                    M.add_edge(i, n)
                for _o, o in M.out_edges(u):
                    if o == n:
                        continue
                    M.add_edge(n, o)
                for i, _i in M.in_edges(v):
                    if i == n:
                        continue
                    M.add_edge(i, n)
                for _o, o in M.out_edges(v):
                    if n == o:
                        continue
                    M.add_edge(n, o)
                M.remove_node(u)
                M.remove_node(v)
    return M, gps


def get_gps(mat, a, gps={}):
    if a < mat.height * mat.width:
        ax = a % mat.width
        ay = int(a / mat.width)
    else:
        if a in gps:
            ax, ay = gps[a]
        else:
            ax = a % mat.width
            ay = int(a / mat.width)
        # print ax, ay
    return (ax, ay)


def gps_mean(mat, a, b, gps={}):
    ax, ay = get_gps(mat, a, gps)
    bx, by = get_gps(mat, b, gps)
    return ((ax + bx) / 2.0, (ay + by) / 2.0)


def get_true_gps(mat, a, gps={}):
    x, y = get_gps(mat, a, gps)
    x = (x - 1) * mat.side + mat.x0
    y = (y - 1) * mat.side + mat.y0
    return (x, y)


def direct(mat, a, b):
    ax = a % mat.width
    ay = int(a / mat.width)
    bx = b % mat.width
    by = int(b / mat.width)
    if ay == by:
        return 99999
    return (ax - bx) / (ay - by)


def get_rank(mat, alpha):
    mat.pagerank(alpha=alpha, trustrank=True, inverse=True, show=False)
    inv_rank = mat.rank
    inv_rank_norm = mat.rank_norm
    mat.pagerank(alpha=alpha, trustrank=True, inverse=False, show=False)
    rank = mat.rank
    rank_norm = mat.rank_norm
    rank_max = {}
    for key in rank_norm.keys():
        rank_max[key] = max(rank_norm[key], inv_rank_norm[key])
    mat.rank_max = rank_max
    return rank_max


def get_map(mat, width=9):
    G = nx.DiGraph()
    for u, v in mat.P.edges():
        G.add_edge(u, v)
        # G[u][v]['weight'] = ((1-mat.rank_max[u])*(1-mat.rank_max[v]))**4
        G[u][v]['weight'] = (((1.1 - mat.rank_max[u]) * (1.1 - mat.rank_max[v])) ** 4) * (1.1 - mat.P[u][v]['weight'])
        # G[u][v]['weight'] = (1-rank_max[v])**2
    N = mat.height * mat.width
    M = nx.DiGraph()
    G_nodes = set(G.nodes())
    scores = []
    path_values = []
    for c in mat.candidates:
        w = width
        nodes = []
        nb_cand = []
        x = c % mat.width
        y = int(c / mat.width)
        for dx in range(-1 * w, w + 1):
            for dy in range(-1 * w, w + 1):
                if dx + x > mat.width - 1 or dy + y > mat.height - 1 or dx + x < 0 or dy + y < 0:
                    continue
                v = (dx + x) + (dy + y) * mat.width
                if v not in G_nodes:
                    continue
                nodes.append(v)
                if v in mat.candidates:
                    nb_cand.append(v)
        sG = G.subgraph(nodes + [c])
        sG_nodes = set(sG.nodes())
        for cand in nb_cand:
            try:
                if not nx.has_path(sG, c, cand):
                    continue
            except Exception, data:
                print data
                continue
            path = nx.dijkstra_path(sG, c, cand)
            value = evaluate_path(path, sG, 0) / len(path)
            path_values.append((path, value))
            for a, b in pairwise(path):
                M.add_edge(a, b)
    return M, path_values


def evaluate_path(path, G, theta):
    score = 0.
    n_turn = 0
    diff = 0
    for a, b in pairwise(path):
        score += G[a][b]['weight']
        if diff == 0:
            diff = b - a
        elif b - a != diff:
            n_turn += 1
            diff = b - a
    score += n_turn * theta
    return score


def get_P(self, inverse=False):
    sum_dict = defaultdict(int)
    for a, b in nx.edges_iter(self.tog):
        if inverse:
            sum_dict[b] += self.tog[a][b]['weight']
        else:
            sum_dict[a] += self.tog[a][b]['weight']
    if inverse:
        self.Q = nx.DiGraph()
    else:
        self.P = nx.DiGraph()
    for a, b in nx.edges_iter(self.tog):
        if inverse:
            self.Q.add_edge(b, a, weight=1.0 * self.tog[a][b]['weight'] / sum_dict[b])
        else:
            self.P.add_edge(a, b, weight=1.0 * self.tog[a][b]['weight'] / sum_dict[a])


def get_visited_cells(mat, w):
    half_cells = set()
    mat.visit = np.zeros(shape=(mat.height, mat.width))
    for c in mat.G.nodes():
        x, y = get_gps(mat, c)
        for dx in range(-1 * w, w + 1):
            for dy in range(-1 * w, w + 1):
                if mat.gps_overflow(dx + x, dy + y):
                    continue
                half_cells.add(c + dx + mat.width * dy)
                mat.visit[y + dy][x + dx] = 1
    return half_cells


def sel_topic_cands(self, i, cands_min_value, width):
    cid_rank = self.u[:, i].argsort()[::-1]
    self.new_candidates = set()
    self.rank = (self.u[:, i] - self.u[:, i].min()) / (self.u[:, i].max() - self.u[:, i].min())
    for cid in cid_rank:
        if self.rank[cid] < cands_min_value:
            break
        x, y = get_gps(self, cid)
        if self.gps_overflow(x, y):
            continue
        if self.visit[y][x] == 0:
            self.new_candidates.add(cid)
        else:
            continue
        for _y in range(y - width, y + width + 1):
            for _x in range(x - width, x + width + 1):
                if self.gps_overflow(_x, _y):
                    continue
                if self.visit[_y][_x] == 0:
                    _cid = _y * self.width + _x
                    self.visit[_y][_x] = _cid


def connect_cands_undirect(self, width, path_max_value, true_pass=False):
    G = nx.Graph()
    nbunch = set([w[0] for w in np.argwhere(self.rank > 0.00001)]) & set(self.P.nodes()) | set(self.G.nodes())
    for u, v in self.P.subgraph(nbunch).edges():
        G.add_edge(u, v)
        if self.G.has_edge(u, v):
            G[u][v]['weight'] = 0.
        else:
            G[u][v]['weight'] = ((1.1 - self.rank[u]) * (1.1 - self.rank[v])) ** 4
        # G[u][v]['weight'] = 1.1-self.rank[v]
    M = nx.DiGraph()
    G_nodes = set(G.nodes())
    for c in self.new_candidates:
        # for c in ((self.new_candidates |self.candidates) & nbunch):
        candidates = []
        w = width
        nodes = []
        nb_cand = []
        x, y = get_gps(self, c)
        for _y in range(y - width, y + width + 1):
            for _x in range(x - width, x + width + 1):
                if self.gps_overflow(_x, _y):
                    # print 'overflow', _x, _y,
                    continue
                v = _y * self.width + _x
                if v not in G_nodes:
                    continue
                if true_pass and v not in self.tcell:
                    continue
                nodes.append(v)
                if v in self.new_candidates:
                    nb_cand.append(v)
                # elif v in self.candidates:
                elif v in self.G.nodes():
                    nb_cand.append(v)
                    candidates.append(v)
        '''
        for _c in candidates:
            w = width
            x, y = get_gps(self, _c)
            for _y in range(y-width,y+width+1):
                for _x in range(x-width,x+width+1):
                    if self.gps_overflow(_x, _y):
                        #print 'overflow', _x, _y, 
                        continue
                    v = _y*self.width+_x
                    nodes.append(v)
        '''
        sG = G.subgraph(nodes + [c] + nb_cand)
        sG_nodes = set(sG.nodes())
        for nb in nb_cand:
            if c not in sG_nodes:
                continue
            # print c in sG_nodes, nb in sG_nodes,
            if nx.has_path(sG, c, nb):
                path = nx.dijkstra_path(sG, c, nb)
                value = evaluate_path(path, sG, 0) / len(path)
                if value > path_max_value:
                    continue
                for a, b in pairwise(path):
                    M.add_edge(a, b)
                    self.G.add_edge(a, b)
    return M


def connect_cands_undirect_old(self, width, path_max_value):
    G = nx.Graph()
    nbunch = set([w[0] for w in np.argwhere(self.rank > 0.00001)]) & set(self.P.nodes())
    for u, v in self.P.subgraph(nbunch).edges():
        G.add_edge(u, v)
        G[u][v]['weight'] = ((1.1 - self.rank[u]) * (1.1 - self.rank[v])) ** 4
        # G[u][v]['weight'] = 1.1-self.rank[v]
    M = nx.DiGraph()
    G_nodes = set(G.nodes())
    for c in self.new_candidates:
        # for c in ((self.new_candidates |self.candidates) & nbunch):
        candidates = []
        w = width
        nodes = []
        nb_cand = []
        x, y = get_gps(self, c)
        for _y in range(y - width, y + width + 1):
            for _x in range(x - width, x + width + 1):
                if self.gps_overflow(_x, _y):
                    # print 'overflow', _x, _y,
                    continue
                v = _y * self.width + _x
                if v not in G_nodes:
                    continue
                nodes.append(v)
                if v in self.new_candidates:
                    nb_cand.append(v)
                elif v in self.candidates:
                    nb_cand.append(v)
                    candidates.append(v)
        for c in candidates:
            w = width
            x, y = get_gps(self, c)
            for _y in range(y - width, y + width + 1):
                for _x in range(x - width, x + width + 1):
                    if self.gps_overflow(_x, _y):
                        # print 'overflow', _x, _y,
                        continue
                    v = _y * self.width + _x
                    nodes.append(v)
        sG = G.subgraph(nodes + [c])
        sG_nodes = set(sG.nodes())
        for nb in nb_cand:
            if nx.has_path(sG, c, nb):
                path = nx.dijkstra_path(sG, c, nb)
                value = evaluate_path(path, sG, 0) / len(path)
                if value > path_max_value:
                    continue
                for a, b in pairwise(path):
                    M.add_edge(a, b)
                    self.G.add_edge(a, b)

            if nx.has_path(sG, nb, c):
                path = nx.dijkstra_path(sG, nb, c)
                value = evaluate_path(path, sG, 0) / len(path)
                if value > path_max_value:
                    continue
                for a, b in pairwise(path):
                    M.add_edge(a, b)
                    self.G.add_edge(a, b)
    return M


def connect_cands(self, width, path_max_value):
    G = nx.DiGraph()
    nbunch = set([w[0] for w in np.argwhere(self.rank > 0)]) & set(self.P.nodes())
    for u, v in self.P.subgraph(nbunch).edges():
        G.add_edge(u, v)
        G[u][v]['weight'] = (((1.1 - self.rank[u]) * (1.1 - self.rank[v])) ** 4) * (1.1 - self.P[u][v]['weight'])
    M = nx.Graph()
    G_nodes = set(G.nodes())
    for c in self.new_candidates:
        # for c in ((self.new_candidates |self.candidates) & nbunch):
        candidates = []
        w = width
        nodes = []
        nb_cand = []
        x, y = get_gps(self, c)
        for _y in range(y - width, y + width + 1):
            for _x in range(x - width, x + width + 1):
                if self.gps_overflow(_x, _y):
                    # print 'overflow', _x, _y,
                    continue
                v = _y * self.width + _x
                if v not in G_nodes:
                    continue
                nodes.append(v)
                if v in self.new_candidates:
                    nb_cand.append(v)
                elif v in self.candidates:
                    nb_cand.append(v)
                    candidates.append(v)
        for c in candidates:
            w = width
            x, y = get_gps(self, c)
            for _y in range(y - width, y + width + 1):
                for _x in range(x - width, x + width + 1):
                    if self.gps_overflow(_x, _y):
                        # print 'overflow', _x, _y,
                        continue
                    v = _y * self.width + _x
                    nodes.append(v)
        sG = G.subgraph(nodes + [c])
        sG_nodes = set(sG.nodes())
        for nb in nb_cand:
            if nx.has_path(sG, c, nb):
                path = nx.dijkstra_path(sG, c, nb)
                value = evaluate_path(path, sG, 0) / len(path)
                if value > path_max_value:
                    continue
                for a, b in pairwise(path):
                    M.add_edge(a, b)
                    self.G.add_edge(a, b)

            if nx.has_path(sG, nb, c):
                path = nx.dijkstra_path(sG, nb, c)
                value = evaluate_path(path, sG, 0) / len(path)
                if value > path_max_value:
                    continue
                for a, b in pairwise(path):
                    M.add_edge(a, b)
                    self.G.add_edge(a, b)
    return M


def iteration(mat, w, k, cands_min_value=0.3, path_max_value=0.9, undirect=True, show=False, rank=None, map_df=None, true_pass=False):
    # mat.G = nx.DiGraph()
    # mat.candidates = set()
    # mat.visit = np.zeros(shape=(mat.height, mat.width))
    get_visited_cells(mat, w)
    for i in range(k):
        i = mat.s.argsort()[::-1][i]
        sel_topic_cands(mat, i, cands_min_value, w)
        global path_values
        if undirect:
            M = connect_cands_undirect(mat, 2 * w + 1, path_max_value, true_pass)
        else:
            M = connect_cands(mat, 2 * w + 1, path_max_value)
        # mat.candidates -= set(mat.candidates) - (set(mat.new_candidates) & set(M.nodes()))
        mat.candidates = mat.candidates | (mat.new_candidates & set(M.nodes()))
        get_visited_cells(mat, w)
        if show:
            plt.figure(figsize=(10, 10 * mat.height / mat.width))
            ca = plt.gca()
            plt.axis(mat.axis)
            plot.map_df(map_df, ca=ca, color='g')
            # plot.dots(mat, mat.candidates, size=7, gps=True)
            plot.graph(mat, mat.G, ca=ca, color='b', linewidth=2, gps=True)
            plot.graph(mat, M, ca=ca, color='r', linewidth=2, gps=True)
            d = {}
            for k in np.argwhere(mat.rank > 0.1):
                d[k[0]] = mat.rank[k[0]]
            plot.dots(mat, d, ca=ca, gps=True)
            plot.dots(mat, mat.candidates, ca=ca, color='b', gps=True)
            plot.dots(mat, mat.new_candidates, ca=ca, color='r', gps=True)
            if rank is not None:
                pairs = []
                for k in rank.keys():
                    if k in M.nodes():
                        pairs.append((k, rank[k]))
                pairs.sort(key=lambda x: x[1], reverse=True)
                inter = []
                for k, v in pairs[:3]:
                    inter.append(k)
                print 'Rank:', inter
                print 'axis:', ca.axis()
                plot.dots(mat, inter, ca=ca, rad=2.0, color='y', gps=True)
            plt.show()
            # plot.vector(mat, mat.u[:,i], size=7)

    if show:
        plot.graph_dots(mat, mat.G, mat.candidates, size=7)


def compute_SAG(self, data, side):
    self.min_x, self.max_x, self.min_y, self.max_y = (data.x.min(), data.x.max(), data.y.min(), data.y.max())
    self.width = int(self.max_x / side) - int(self.min_x / side) + 1
    self.height = int(self.max_y / side) - int(self.min_y / side) + 1
    col = []
    row = []
    for _idx, _row in data.iterrows():
        x, y = _row['x'], _row['y']
        oy = int((y - self.min_y) / side)
        ox = int((x - self.min_x) / side)
        # print ox, oy
        row.append(ox + oy * self.width)
        col.append(_row['tid'])
    return scipy.sparse.csc_matrix(([1] * len(row), (row, col)), shape=(max(row) + 1, max(col) + 1))


def generate_map(mat, w, k, maxiter, cands_min_value=0.3, path_max_value=0.1, show=False, map_df=None, road_inc=True):
    mat.G = nx.Graph()
    mat.candidates = set()
    nedges_delta = -1
    while nedges_delta != 0:
        mat.pLSA(k, maxiter)
        old_nedges = mat.G.number_of_edges()
        iteration(mat, w, k, cands_min_value, path_max_value, show=show, map_df=map_df)
        if road_inc == False:
            break
        now_nedges = mat.G.number_of_edges()
        nedges_delta = now_nedges - old_nedges
        print '%d nodes in G, %d edges in G' % (mat.G.number_of_nodes(), mat.G.number_of_edges())
        if nedges_delta == 0:
            break
        cells = get_visited_cells(mat, w)
        dc = set(cells) & set(mat.P.nodes())
        # to-do 2 graph
        P = mat.P.to_undirected()
        cut = set()
        for c in dc:
            nb = set(P.neighbors(c))
            _cut = nb - dc
            for n in _cut:
                cut.add(n)
        restore = set()
        for c in cut:
            x, y = get_gps(mat, c)
            for _y in range(y - w, y + w + 1):
                for _x in range(x - w, x + w + 1):
                    if mat.gps_overflow(_x, _y):
                        continue
                    v = _y * mat.width + _x
                    if v in cells:
                        restore.add(v)
        for c in restore:
            cells.remove(c)
        for c in cells:
            mat.sag[c, :] = 0


def write_corpus(mat, corpus):
    ts = []
    for i in range(mat.sag.shape[1]):
        ts.append([])
    new_c = 1
    for c, t in zip(*mat.sag.nonzero()):
        ts[t].append(c)
        new_c = c
    with open(corpus, 'w') as f:
        f.write('%d\n' % (len(ts)))
        for t in ts:
            for c in t:
                f.write('%d ' % (c))
            if len(t) == 0:
                f.write('%d ' % (new_c))
            f.write('\n')
        f.flush()


def get_lda_model(mat, dirc):
    phi = []
    with open('%s/model-final.phi' % (dirc), 'r') as f:
        for s in f.readlines():
            phi.append([])
            for num in s.split(' '):
                if num.startswith('0'):
                    phi[-1].append(float(num))
    phi = np.array(phi)
    theta = []
    with open('%s/model-final.theta' % (dirc), 'r') as f:
        for s in f.readlines():
            theta.append([])
            for num in s.split(' '):
                if num.startswith('0'):
                    theta[-1].append(float(num))
    theta = np.array(theta)
    wordmap = {}
    with open('%s/wordmap.txt' % (dirc), 'r') as f:
        for s in f.readlines():
            _s = s.split()
            if len(_s) == 1:
                continue
            wordmap[int(_s[1])] = int(_s[0])
    # return phi, theta, wordmap
    mat.u = np.zeros(shape=((mat.height) * mat.width, phi.shape[0]))
    for x, y in zip(*phi.T.nonzero()):
        try:
            mat.u[wordmap[x]][y] = phi[y][x]
        except Exception, _data:
            print x, y,
            continue
            print 'phi:', phi[y][x]
            print 'wordmap:', wordmap[x]
    mat.s = np.zeros(shape=(theta.shape[1]))
    for i in range(theta.shape[1]):
        mat.s[i] = theta[:, i].sum()
    # return phi, theta, wordmap


'''
def generate_map(mat, w, k, cands_min_value=0.3, path_max_value = 0.1, undirect=False, show=False):
    #mat.G = nx.DiGraph()
    #mat.candidates = []
    mat.visit = np.zeros(shape=(mat.height, mat.width))
    for i in range(k):
        i = mat.s.argsort()[::-1][i]
        sel_topic_cands(mat, i, cands_min_value, w)
        global path_values
        if undirect:
            M = connect_cands_undirect(mat, 2*w+1, path_max_value)
        else:
            M = connect_cands(mat, 2*w+1, path_max_value)
        #mat.candidates -= set(mat.candidates) - (set(mat.new_candidates) & set(M.nodes()))
        mat.candidates = mat.candidates | (mat.new_candidates & set(M.nodes()))
        get_visited_cells(mat, w)
        if show:
            plt.figure(figsize=(7, 7*mat.height/mat.width))
            ca = plt.gca()
            #plot.graph_dots(mat, M, mat.candidates, size=7)
            plot.graph(mat, mat.G, ca=ca, size=7, color='b')
            plot.graph(mat, M, ca=ca, size=7, color='r')
            d = {}
            for k in np.argwhere(mat.rank > 0.1):
                d[k[0]] = mat.rank[k[0]]
            plot.dots(mat, d, ca=ca)
            plot.dots(mat, mat.candidates, ca=ca, color='b')
            plot.dots(mat, mat.new_candidates, ca=ca, color='r')
            plt.show()
            #plot.vector(mat, mat.u[:,i], size=7)

    if show:
        plot.graph_dots(mat, mat.G, mat.candidates, size=7)
'''


class Sag:
    def __init__(self, data, side, inc=0, cut_length=0, meta=None):
        self.side = side
        self.sag, self.tog = self.compute_dense_SAG_TOG_nx(data, side, inc=inc, cut_length=cut_length, meta=meta)

    def gps_overflow(self, x, y):
        if x < 0 or y < 0 or x >= self.width or y >= self.height:
            return True
        return False

    def SVD(self, n=3):
        smat = scipy.sparse.csc_matrix(self.sag)
        ut, s, vt = sparsesvd(smat, n)
        print ut.shape, s.shape, vt.shape
        # print numpy.std(A - numpy.dot(ut.T, numpy.dot(numpy.diag(s), vt)))
        self.u, self.s, self.v = ut.T, s, vt

    def dim_reduce(self, p, _run=None):
        k = 0
        square_sum = sum([s ** 2 for s in self.s])
        _sum = 0
        for _s in self.s:
            _sum += _s ** 2
            k += 1
            if 1.0 * _sum / square_sum >= p:
                break
        if _run != None:
            _run.info['dim_reduce'] = {'origin_dim': len(self.s),
                                       'reduced_dim': k,
                                       'ratio': p,
                                       'matained_sum': _sum,
                                       'origin_sum': square_sum}
        else:
            print 'reduced_dim:', k
        return k

    def SVD_tensor(self, reduce_dim=False, ratio=0.9, _run=None):
        # Normolization
        if reduce_dim:
            dim = self.dim_reduce(ratio, _run=_run)
        else:
            dim = self.u.shape[1]
        for k in range(dim):
            self.u[:, k] = (self.u[:, k] - self.u[:, k].min()) / (self.u[:, k].max() - self.u[:, k].min())
        self.tensor = np.zeros(shape=(self.height, self.width, dim))
        # self.tensor[:, :, :] = 0
        for i in range(self.u.shape[0]):
            if self.height <= (int(i / self.width)):
                continue
            # print int(i/length), i%length
            y = (int(i / self.width))
            x = i % self.width
            for k in range(dim):
                self.tensor[y][x][k] = self.u[i][k]

    def pLSA_tensor(self, _run=None):
        self.tensor = np.zeros(shape=(self.height, self.width, self.u.shape[1]))
        for i in range(self.u.shape[0]):
            if self.height <= (int(i / self.width)):
                continue
            y = (int(i / self.width))
            x = i % self.width
            for k in range(self.u.shape[1]):
                self.tensor[y][x][k] = self.u[i][k]

    def get_candidates(self, method, num):
        ret_dict = {}
        if method == 'num_sum':
            for i in range(self.sag.shape[0]):
                ret_dict[i] = self.sag[i].sum()
        elif method == 'sum':
            for i in range(self.u.shape[0]):
                ret_dict[i] = sum(self.u[i])
        elif method == 'max':
            for i in range(self.u.shape[0]):
                ret_dict[i] = max(self.u[i])
        cands = self.cands_from_vect(ret_dict.values(), num, show=False)
        return cands, ret_dict

    def pLSA(self, k, max_iter=500, data=None):
        plsa_m = plsa.pLSA()
        print 'Have cython extension?', plsa.HAVE_EXT
        print max_iter
        file_name = 'plsa_%s_%d_%d_%d.pickle' % (data, self.side, k, max_iter)
        folder = '../Data/plsa/'
        if data is not None:
            if os.path.exists(folder + file_name):
                with open(folder + file_name, 'r') as f:
                    delta_time, self.s, self.u, self.v = pickle.load(f)
                    print 'Read pickle', folder + file_name
                # self.pLSA_tensor()
                return delta_time
            else:
                time0 = time.time()
                self.s, self.u, self.v = plsa_m.train(self.sag, k, max_iter)
                print 'Compute pLSA'
                time1 = time.time()
                delta_time = time1 - time0
                with open(folder + file_name, 'w') as f:
                    pickle.dump([time1 - time0, self.s, self.u, self.v], f)
        else:
            self.s, self.u, self.v = plsa_m.train(self.sag, k, max_iter)
        # self.pLSA_tensor()

    def pLSA_candidates(self, min_value=0.3, width=3, show=False, fig_width=10):
        # self.candidates = defaultdict(list)
        visit = np.zeros(shape=(self.height, self.width))
        # tmp = np.zeros(shape=(self.height, self.width))

        self.candidates = []
        # for i in range(self.u.shape[1]):
        for i in self.s.argsort()[::-1]:
            cid_rank = self.u[:, i].argsort()[::-1]
            for cid in cid_rank:
                if self.u[cid, i] < min_value:
                    break
                if 0 == (int(cid / self.width)):
                    continue
                y = self.height - (int(cid / self.width))
                x = cid % self.width
                '''
                if visit[y][x] == 0:
                    tmp[y][x] = 1
                else:
                    continue
                '''
                if visit[y][x] == 0:
                    self.candidates.append(cid)
                else:
                    continue
                for _y in range(y - width, y + width + 1):
                    if _y < 0 or _y >= self.height:
                        continue
                    for _x in range(x - width, x + width + 1):
                        if _x < 0 or _x >= self.width:
                            continue
                        if visit[_y][_x] == 0:
                            # cid = y * mat.height + x
                            _cid = _y * self.width + _x
                            visit[_y][_x] = _cid
                            # candidates[cid].append(_cid)
            if show:
                plot.dots(self, self.candidates, fig_width)

    def fpLSA(self, k, max_iter=500, data=None):
        plsa_m = plsa.pLSA()
        print 'Have cython extension?', plsa.HAVE_EXT
        print max_iter
        self.s, self.u, self.v = plsa_m.train(self.sag, k, max_iter, folding_in=False)
        for i in range(self.v.shape[0]):
            self.s.append(self.v[i].sum())
        # self.pLSA_tensor()

    def LDA(self, k, niters, alpha=0.001, beta=0.001):
        io_time = 0
        random.seed()
        rand = int(random.random() * 1000)
        dirc = '/home/renj/Project/nTrajMap/Data/lda/%d_%d_%d_%f_%f_%d_%d_%d' % (
        self.sag.shape[0], self.sag.shape[1], self.side, alpha, beta, k, niters, rand)
        ini_time = time.time()
        if os.path.exists(dirc) is False:
            os.mkdir(dirc)
        corpus = dirc + '/corpus.txt'
        write_corpus(self, corpus)
        io_time += time.time() - ini_time
        print corpus
        _read = os.popen('/home/renj/Project/nTrajMap/gibbs-lda/out/lda -est -alpha %f -beta %f -ntopics %d -niters %d -dfile %s' % \
                         (alpha, beta, k, niters, corpus)).read()
        # print _read
        ini_time = time.time()
        # phi, theta, wordmap = get_lda_model(self, dirc)
        get_lda_model(self, dirc)
        io_time += time.time() - ini_time
        os.popen('rm -r %s' % (dirc))
        print io_time
        return io_time

    '''
    def LDA(self, k, passes, _run=None):
        texts = []
        for i in range(self.sag.shape[1]):
            texts.append([str(t) for t in list(self.sag[:,i].nonzero()[0])])
        dictionary = corpora.Dictionary(texts)
        corpus = [dictionary.doc2bow(text) for text in texts]
        corpora.BleiCorpus.serialize('/data/corpus-5.lda-c', corpus)
        corpus = corpora.BleiCorpus('/data/corpus-5.lda-c')
        dic = {}
        for _k in dictionary.token2id.keys():
            dic[dictionary.token2id[_k]] = _k
        model = models.ldamodel.LdaModel(corpus, num_topics=k, passes=passes, id2word=corpus.id2word)
        tensor = np.zeros(shape=(self.height, self.width, model.num_topics))
        ans = model.show_topics(num_topics=model.num_topics, num_words=model.num_terms,formatted=False)
        #cant_find = []
        u = np.zeros(shape=((self.height+1)*(self.width+1), k))
        for tid, cids in ans:
            #print len(cids)
            for cid, score in cids:
                cid = int(dic[int(cid)])
                u[cid][tid] = score
                """
                try:
                except Exception, data:
                    print data
                    cant_find.append(cid)
                    continue
                """
                h = cid/self.width
                w = cid%self.width
                if h == 0:
                    continue
                tensor[self.height-h][w][tid] = score
        self.u = u
        self.tensor = tensor
    '''

    def cands_from_vect(self, vect, max_num, width=3, show=False, fig_width=10):
        cands = []
        order = np.array(vect).argsort()[::-1]
        visit = np.zeros(shape=(self.height, self.width))
        n = 1
        for cid in order:
            if 0 == (int(cid / self.width)):
                continue
            if n > max_num:
                break
            y = self.height - (int(cid / self.width))
            x = cid % self.width
            if visit[y][x] != 0:
                continue
            cands.append(cid)
            for _y in range(y - width, y + width + 1):
                if _y < 0 or _y >= self.height:
                    continue
                for _x in range(x - width, x + width + 1):
                    if _x < 0 or _x >= self.width:
                        continue
                    if visit[_y][_x] == 0:
                        # cid = y * mat.height + x
                        _cid = _y * self.width + _x
                        visit[_y][_x] = _cid
                        # candidates[cid].append(_cid)
            n += 1
        if show:
            plot.dots(self, cands, fig_width)
        return cands

    def compute_candidates(self, percent=0.03, width=3, show=False, sizerate=0.05):
        vars = []
        for i in range(self.u.shape[1]):
            vars.append((self.u[:, i] - self.u[:, i].mean()) ** 2)
            vars[i] = (vars[i] - vars[i].min()) / (vars[i].max() - vars[i].min())
        visit = np.zeros(shape=(self.height, self.width))
        tmp = np.zeros(shape=(self.height, self.width))
        # self.candidates = defaultdict(list)
        self.candidates = []
        for i in range(len(vars)):
            var = vars[i]
            var_array = var.argsort()[::-1]
            cids = var_array[:int(len(var_array) * percent)]
            for cid in cids:
                if 0 == (int(cid / self.width)):
                    continue
                y = self.height - (int(cid / self.width))
                x = cid % self.width
                if visit[y][x] == 0:
                    tmp[y][x] = 1
                else:
                    continue
                self.candidates.append(cid)
                for _y in range(y - width, y + width + 1):
                    if _y < 0 or _y >= self.height:
                        continue
                    for _x in range(x - width, x + width + 1):
                        if _x < 0 or _x >= self.width:
                            continue
                        if visit[_y][_x] == 0:
                            # cid = y * mat.height + x
                            _cid = _y * self.width + _x
                            visit[_y][_x] = _cid
                            # candidates[cid].append(_cid)
            if show:
                plot.dots(self, self.candidates, sizerate)

    def pagerank(self, alpha=0.85, trustrank=True, show=True, sizerate=0.05, inverse=False):
        sum_dict = defaultdict(int)
        for a, b in nx.edges_iter(self.tog):
            if inverse:
                sum_dict[b] += self.tog[a][b]['weight']
            else:
                sum_dict[a] += self.tog[a][b]['weight']
        self.P = nx.DiGraph()
        for a, b in nx.edges_iter(self.tog):
            if inverse:
                self.P.add_edge(b, a, weight=1.0 * self.tog[a][b]['weight'] / sum_dict[b])
            else:
                self.P.add_edge(a, b, weight=1.0 * self.tog[a][b]['weight'] / sum_dict[a])

        if trustrank:
            cand_set = set(self.candidates)
            v = {}
            for c in self.P.nodes():
                if c in cand_set:
                    v[c] = 1
                else:
                    v[c] = 0
            self.rank = nx.pagerank(self.P, alpha=alpha, personalization=v, max_iter=200)
        else:
            self.rank = nx.pagerank(self.P, alpha=alpha, max_iter=200)
        min_v = min(self.rank.values())
        max_v = max(self.rank.values())
        self.rank_norm = {}
        for k in self.rank.keys():
            self.rank_norm[k] = 1.0 * (self.rank[k] - min_v) / (max_v - min_v)
        if show:
            plot.dots(self, self.rank, sizerate=sizerate)

    def densify(self):
        self.dens = np.zeros(shape=self.sag.shape)
        self.radius = 2
        for cid, tid in zip(self.sag.nonzero()[0], self.sag.nonzero()[1]):
            # print 'origin:', cid, tid
            y = self.height - (int(cid / self.width))
            x = cid % self.width
            for i in range(radius):
                for _y in range(y - i, y + i + 1):
                    if _y < 0 or _y > self.height:
                        continue
                    for _x in range(x - i, x + i + 1):
                        if _x < 0 or _x > self.width:
                            continue
                        # print _y, _x
                        _cid = _x + _y * self.width
                        self.dens[_cid][tid] += 1
        self._sag = copy.copy(self.sag)
        self.sag = self.dens

    def compute_SAG(self, data, side):
        self.min_x, self.max_x, self.min_y, self.max_y = (data.x.min(), data.x.max(), data.y.min(), data.y.max())
        self.width = int(self.max_x / side) - int(self.min_x / side) + 1
        self.height = int(self.max_y / side) - int(self.min_y / side) + 1
        col = []
        row = []
        for tid in data.tid.unique():
            themap = cv.CreateMat(self.height, self.width, cv.CV_16UC1)
            cv.SetZero(themap)
            for p in pairwise(data[data.tid == tid].values):
                x0, y0, x1, y1 = p[0][0], p[0][1], p[1][0], p[1][1]
                oy = self.height - int((y0 - self.min_y) / side)
                ox = int((x0 - self.min_x) / side)
                dy = self.height - int((y1 - self.min_y) / side)
                dx = int((x1 - self.min_x) / side)
                cv.Line(themap, (ox, oy), (dx, dy), (32), 1, cv.CV_AA)
            for tup in zip(*np.matrix(themap).nonzero()):
                row.append(tup[1] + (self.height - tup[0]) * self.width)
                col.append(tid)
        self.sag = scipy.sparse.csc_matrix(([1] * len(row), (row, col)), shape=(max(row) + 1, max(col) + 1))

    def compute_dense_SAG_TOG_nx(self, data, side, inc=0, cut_length=0, meta=None):
        x0, x1, y0, y1 = (data.x.min(), data.x.max(), data.y.min(), data.y.max())
        self.x0, self.x1, self.y0, self.y1 = x0, x1, y0, y1
        dx = side
        dy = side

        cnt = 0
        cid = 0

        total = len(data)
        width = int(x1 / dx) - int(x0 / dx) + 1
        self.width = width
        self.height = height = int(y1 / dy) - int(y0 / dy) + 1

        if meta is not None:
            self.width, self.height, self.x0, self.x1, self.y0, self.y1 = meta
            width, height, x0, x1, y0, y1 = meta

        _x0 = int(x0 / dx) - 1
        _y0 = int(y0 / dy) - 1

        # tog = defaultdict(lambda:  defaultdict(int))
        tog = nx.DiGraph()
        if inc == -1:
            tog = nx.Graph()
        row = []
        col = []

        _x = -1
        _y = -1
        _tid = -1
        # sag = np.zeros(shape=(self.height*self.width+1, max(data.tid.unique())+1))
        # tsag = np.zeros_like(sag)
        # sag = scipy.sparse.lil_matrix((self.height*self.width+1, max(data.tid.unique())+1))
        # tsag = scipy.sparse.lil_matrix((self.height*self.width+1, max(data.tid.unique())+1))
        # print tsag.shape

        for idx, line in data.iterrows():
            x, y, tid = line.x, line.y, line.tid
            oy = int((y - self.y0) / side)
            ox = int((x - self.x0) / side)
            # tsag[(ox+oy*self.width), tid] = 1
            if cnt % int(total / 10) == 0:
                sys.stdout.write("\rComplete (" + str(cnt) + "/" + str(total) + ")... ")
                sys.stdout.flush()
            cnt += 1

            if x <= x0 or x >= x1 or y <= y0 or y >= y1:
                _tid = -1
                continue

            if _tid == -1 or _tid != tid:
                _x = x
                _y = y
                _tid = tid
                continue

            ans = []

            if (_x == x) and (_y == y):
                continue
            elif (cut_length > 0) and ((x - _x) ** 2 + (y - _y) ** 2 > cut_length ** 2):
                continue
            else:
                if (_x == x):
                    k = 99999
                else:
                    k = 1.0 * (_y - y) / (_x - x)
                b = y - k * x
                if abs(k) <= 1:
                    step = (x - _x) / abs(x - _x) * dx
                    for x_ in np.arange(_x, x + step, step):
                        x_ = x_ + dx * 0.5
                        ox_ = x_
                        oy_ = (k * x_ + b)
                        if ox_ <= x0 or ox_ >= x1 or oy_ <= y0 or oy_ >= y1:
                            continue

                        y_ = int((k * x_ + b) / dy)
                        x_ = int(x_ / dx)
                        ans.append((x_ - _x0) + (y_ - _y0) * width)
                        if (x_ - _x0) + (y_ - _y0) * width < 0:
                            print x, -_x0, (y_ - _y0), (x_ - _x0) + (y_ - _y0) * width
                else:
                    step = (y - _y) / abs(y - _y) * dy
                    for y_ in np.arange(_y, y + step, step):
                        y_ = y_ + dy * 0.5
                        oy_ = y_
                        ox_ = (y_ - b) / k
                        if ox_ <= x0 or ox_ >= x1 or oy_ <= y0 or oy_ >= y1:
                            continue
                        x_ = int((y_ - b) / k / dy)
                        y_ = int(y_ / dy)
                        ans.append((x_ - _x0) + (y_ - _y0) * width)

                        if (x_ - _x0) + (y_ - _y0) * width < 0:
                            print x_, _x0, (y_ - _y0), (x_ - _x0) + (y_ - _y0) * width

            _cid = -1

            for cid in ans:
                if cid >= self.height * self.width:
                    continue
                # sag[cid, tid] = 1
                row.append(cid)
                col.append(tid)
                if cid < 0:
                    print 'Error', ans

                if inc == -1:
                    for a, b in zip(ans[:-1], ans[1:]):
                        tog.add_edge(a, b)

                if _cid != -1 and inc != -1:
                    # tog[_cid][cid] += 1
                    if tog.has_edge(_cid, cid):
                        tog[_cid][cid]['weight'] += 1
                    else:
                        tog.add_edge(_cid, cid, weight=1)
                    _cid = cid
                _cid = cid

            _x = x
            _y = y

        sag = scipy.sparse.csc_matrix(([1] * len(row), (row, col)), shape=(self.height * self.width + 1, max(col) + 1))
        '''
        self.tcell = set()
        for a, b in zip(*tsag.nonzero()):
            if inc > 0:
                sag[a, b] += inc
            self.tcell.add(a)
        if inc > 0:
            for a, b in zip(*tsag.nonzero()):
                sag[a, b] += inc
                self.tcell.add(a)
        '''
        return sag, tog

    def compute_dense_SAG_TOG(self, data, side):
        x0, x1, y0, y1 = (data.x.min(), data.x.max(), data.y.min(), data.y.max())
        self.x0, self.x1, self.y0, self.y1 = x0, x1, y0, y1
        dx = side
        dy = side

        cg_map = {}
        # dwg = []
        cg_keys = set()
        cnt = 0
        cid = 0
        row = []
        col = []
        total = len(data)
        width = int(x1 / dx) - int(x0 / dx) + 1
        self.width = width
        self.height = height = int(y1 / dy) - int(y0 / dy) + 1

        _x0 = int(x0 / dx) - 1
        _y0 = int(y0 / dy) - 1

        tog = defaultdict(lambda: defaultdict(set))

        _x = -1
        _y = -1
        _tid = -1

        for idx, line in data.iterrows():
            x, y, tid = line.x, line.y, line.tid
            if cnt % int(total / 10) == 0:
                sys.stdout.write("\rComplete (" + str(cnt) + "/" + str(total) + ")... ")
                sys.stdout.flush()
            cnt += 1

            if x <= x0 or x >= x1 or y <= y0 or y >= y1:
                _tid = -1
                continue

            if _tid == -1 or _tid != tid:
                _x = x
                _y = y
                _tid = tid
                continue

            ans = []

            if (_x == x) and (_y == y):
                continue
            else:
                if (_x == x):
                    k = 99999
                else:
                    k = 1.0 * (_y - y) / (_x - x)
                b = y - k * x
                if abs(k) <= 1:
                    step = (x - _x) / abs(x - _x) * dx
                    for x_ in np.arange(_x, x + step, step):
                        x_ = x_ + dx * 0.5
                        ox_ = x_
                        oy_ = (k * x_ + b)
                        if ox_ <= x0 or ox_ >= x1 or oy_ <= y0 or oy_ >= y1:
                            continue

                        y_ = int((k * x_ + b) / dy)
                        x_ = int(x_ / dx)
                        ans.append((x_ - _x0) + (y_ - _y0) * width)
                        if (x_ - _x0) + (y_ - _y0) * width < 0:
                            print x, -_x0, (y_ - _y0), (x_ - _x0) + (y_ - _y0) * width
                else:
                    step = (y - _y) / abs(y - _y) * dy
                    for y_ in np.arange(_y, y + step, step):
                        y_ = y_ + dy * 0.5
                        oy_ = y_
                        ox_ = (y_ - b) / k
                        if ox_ <= x0 or ox_ >= x1 or oy_ <= y0 or oy_ >= y1:
                            continue
                        x_ = int((y_ - b) / k / dy)
                        y_ = int(y_ / dy)
                        ans.append((x_ - _x0) + (y_ - _y0) * width)

                        if (x_ - _x0) + (y_ - _y0) * width < 0:
                            print x_, _x0, (y_ - _y0), (x_ - _x0) + (y_ - _y0) * width

            _cid = -1

            for cid in ans:
                row.append(cid)
                col.append(tid)
                if cid < 0:
                    print 'Error', ans

                if _cid != -1:
                    tog[_cid][cid].add(tid)
                    _cid = cid
                _cid = cid

            _x = x
            _y = y

        sag = scipy.sparse.csc_matrix(([1] * len(row), (row, col)),
                                      shape=(max(row) + 1, max(col) + 1))
        return sag, dict(tog)


'''
class nSag:
    def __init__(self, data, side):
        self.side = side

    def get_USV(self, n=3):
        smat = scipy.sparse.csc_matrix(self.sag)
        ut, s, vt = sparsesvd(smat, n)
        print ut.shape, s.shape, vt.shape
        #print numpy.std(A - numpy.dot(ut.T, numpy.dot(numpy.diag(s), vt)))
        self.u, self.s, self.v = ut.T, s, vt

    def compute_amat(self):
        # Normolization
        for k in range(self.u.shape[1]):
            self.u[:, k] = (self.u[:, k] - self.u[:, k].min()) / (self.u[:, k].max() - self.u[:, k].min())
        self.amat = np.zeros(shape=(self.height, self.width, self.u.shape[1]))
        #self.amat[:, :, :] = 0
        for i in range(self.u.shape[0]):
            i = self.rid2cid[i]
            if 0 == (int(i/self.width)):
                continue
            #print int(i/length), i%length
            y = self.height - (int(i/self.width))
            x = i % self.width
            for k in range(self.u.shape[1]):
                self.amat[y][x][k] = self.u[i][k]

    def densify(self):
        self.dens = np.zeros(shape=mat.sag.shape)
        self.radius = 2
        for cid, tid in zip(self.sag.nonzero()[0], self.sag.nonzero()[1]):
            #print 'origin:', cid, tid
            y = self.height - (int(cid/self.width))
            x = cid % self.width
            for i in range(radius):
                for _y in range(y-i, y+i+1):
                    if _y < 0 or _y > self.height:
                        continue
                    for _x in range(x-i, x+i+1):
                        if _x < 0 or _x > self.width:
                            continue
                        #print _y, _x
                        _cid = _x + _y*self.width
                        self.dens[_cid][tid] += 1
        self._sag = copy.copy(self.sag)
        self.sag = self.dens

    def compute_SAG(self, data, side):
        self.min_x, self.max_x, self.min_y, self.max_y = (data.x.min(), data.x.max(), data.y.min(), data.y.max())
        self.width = int(self.max_x/side) - int(self.min_x/side) + 1
        self.height = int(self.max_y/side) - int(self.min_y/side) + 1
        col = []
        row = []
        self.rid2cid = {}
        cid2rid = {}
        rid = 0
        import cv2.cv as cv
        for tid in data.tid.unique():
            themap = cv.CreateMat(self.height, self.width, cv.CV_16UC1)
            cv.SetZero(themap)
            for p in pairwise(data[data.tid == tid].values):
                x0, y0, x1, y1 = p[0][0], p[0][1], p[1][0], p[1][1]
                oy = self.height-int((y0 - self.min_y) / side)
                ox = int((x0 - self.min_x) / side)
                dy = self.height-int((y1 - self.min_y) / side)
                dx = int((x1 - self.min_x) / side)
                cv.Line(themap, (ox, oy), (dx, dy), (32), 1, cv.CV_AA)
            for tup in zip(*np.matrix(themap).nonzero()):
                cid = tup[1]+(self.height-tup[0])*self.width
                if cid not in cid2rid.keys():
                    cid2rid[cid] = rid
                    self.rid2cid[rid] = cid
                    rid += 1
                row.append(cid2rid[cid])
                col.append(tid)
        self.sag = scipy.sparse.csc_matrix(([1]*len(row), (row, col)),shape=(max(row)+1, max(col)+1))


def sel_topic_cands(self, i, min_value, width):
    sort_cids = self.u[:,i].argsort()[::-1]
    self.new_candidates = set()
    self.rank_max = (self.u[:, i] - self.u[:, i].min()) / (self.u[:, i].max() - self.u[:, i].min())
    for cid in sort_cids:
        if self.rank_max[cid] < min_value:
        #if self.u[cid, i] <= 0:
            break
        if 0 == (int(cid/self.width)):
            continue
        #y = self.height-(int(cid/self.width))
        #x = cid % self.width
        x, y = get_gps(self, cid)
        if y >= self.height:
            'y overflow'
            continue
        #print x, y
        if self.visit[y][x] == 0:
            #self.candidates.add(cid)
            self.new_candidates.add(cid)
        else:
            continue
        for _y in range(y-width, y+width+1):
            if _y < 0 or _y >= self.height:
                continue
            for _x in range(x-width, x+width+1):
                if _x < 0 or _x >= self.width:
                    continue
                if self.visit[_y][_x] == 0:
                    #cid = y * mat.height + x
                    _cid = _y * self.width + _x
                    self.visit[_y][_x] = _cid

def get_visited_cells(mat, w):
    half_cells = set()
    mat.visit = np.zeros(shape=(mat.height, mat.width))
    for c in mat.G.nodes():
        x, y = get_gps(mat, c)
        for dx in range(-1*w,w+1):
            for dy in range(-1*w,w+1):
                if dx+x > mat.width-1 or dy+y > mat.height-1 or dx+x < 0 or dy+y < 0:
                    continue
                half_cells.add(c+dx+mat.width*dy)
                mat.visit[y+dy][x+dx] = 1
    return half_cells

def connect_cands(mat, width, max_value):
    G = nx.DiGraph()
    nbunch = set([w[0] for w in np.argwhere(mat.rank_max > 0)])&set(mat.P.nodes())
    for u, v in mat.P.subgraph(nbunch).edges():
        G.add_edge(u,v)
        #G[u][v]['weight'] = ((1-mat.rank_max[u])*(1-mat.rank_max[v]))**4
        G[u][v]['weight'] = (((1.1-mat.rank_max[u])*(1.1-mat.rank_max[v]))**4)*(1.1-mat.P[u][v]['weight'])
        #G[u][v]['weight'] = (1-rank_max[v])**2
    N = mat.height*mat.width
    M = nx.DiGraph()
    G_nodes = set(G.nodes())
    scores = []
    path_values = []
    for c in mat.new_candidates:
    #for c in ((mat.new_candidates |mat.candidates) & nbunch):
        w = width
        nodes = []
        nb_cand = []
        x = c % mat.width
        y = int(c / mat.width)
        for dx in range(-1*w,w+1):
            for dy in range(-1*w,w+1):
                if dx+x > mat.width-1 or dy+y > mat.height-1 or dx+x < 0 or dy+y < 0:
                    continue
                v = (dx+x)+(dy+y)*mat.width
                if v not in G_nodes:
                    continue
                nodes.append(v)
                if v in (mat.candidates | mat.new_candidates):
                    nb_cand.append(v)
        sG = G.subgraph(nodes+[c])
        sG_nodes = set(sG.nodes())
        for cand in nb_cand:
            try:
                if nx.has_path(sG, c, cand) is False and nx.has_path(sG, cand, c) is False:
                    continue
            except Exception, data:
                print data
                continue
            if nx.has_path(sG, c, cand):
                path = nx.dijkstra_path(sG, c, cand)
                value = evaluate_path(path, sG, 0)/len(path)
            elif nx.has_path(sG, cand, c):
                path = nx.dijkstra_path(sG, cand, c)
                #print cand, c
                value = evaluate_path(path, sG, 0)/len(path)
            if value > max_value:
                continue
            for a, b in pairwise(path):
                M.add_edge(a,b)
                mat.G.add_edge(a,b)
    #return M, path_values
    return M

def generate_map(mat, w, k, min_value=0.3, max_value = 0.1, show=False):
    #mat.G = nx.DiGraph()
    #mat.candidates = []
    mat.visit = np.zeros(shape=(mat.height, mat.width))
    for i in range(k):
        i = mat.s.argsort()[::-1][i]
        sel_topic_cands(mat, i, min_value, w)
        global path_values
        M = connect_cands(mat, 2*w+1, max_value)
        #mat.candidates -= set(mat.candidates) - (set(mat.new_candidates) & set(M.nodes()))
        mat.candidates = mat.candidates | (mat.new_candidates & set(M.nodes()))
        get_visited_cells(mat, w)
        if show:
            plt.figure(figsize=(7, 7*mat.height/mat.width))
            ca = plt.gca()
            #plot.graph_dots(mat, M, mat.candidates, size=7)
            plot.graph(mat, mat.G, ca=ca, size=7, color='b')
            plot.graph(mat, M, ca=ca, size=7, color='r')
            #plot.dots(mat, mat.candidates, ca=ca, color='b')
            #plot.dots(mat, mat.new_candidates, ca=ca, color='r')
            d = {}
            for k in np.argwhere(mat.rank_max > 0.1):
                d[k[0]] = mat.rank_max[k[0]]
            plot.dots(mat, d, ca=ca)
            plt.show()
            #plot.vector(mat, mat.u[:,i], size=7)

    if show:
        plot.graph_dots(mat, mat.G, mat.candidates, size=7)
'''
