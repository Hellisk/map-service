import json
import os
import pickle
import random

import math
import numpy as np
import pandas as pd
import time

import quad


def bitlist_origin(A, w):
    tnodes = A.shape[1]
    print 'Processing origin bitlist n_nodes:%d, base: %d' % (tnodes, w)
    undo = range(tnodes)
    undo_array = np.ones(A.shape[1])

    bit_list = []
    now = 0
    ans = []
    i = 0
    while len(undo) > 0:
        # print len(undo), len(undo_array.nonzero()[0])
        best = 0
        if now == w:
            now = 0
        if now == 0:
            best = undo.pop()
            bit_list = A[:, best]
        else:
            bs = -1
            visited = np.zeros(tnodes)
            to_visit = []
            for n in bit_list.nonzero()[0]:
                visited = np.logical_or(visited, A[n])
            to_visit = np.logical_and(visited, undo_array).nonzero()[0]
            for n in to_visit:
                score = np.sum(A[:, n] * bit_list)
                if score > bs:
                    best = n
                    bs = score
            if best == 0:
                best = undo[0]
            bit_list = bit_list + A[:, best]
            undo.remove(best)

        undo_array[best] = 0

        ans.append(best)
        i += 1
        now += 1

    return ans


"""
def bitlist_origin(A, w):
    tnodes = A.shape[1]
    print 'Processing origin bitlist n_nodes:%d, base: %d' % (tnodes, w)
    undo = range(tnodes)
    undo_array = np.ones(A.shape[1])

    bit_list = []
    now = 0
    ans = []
    i = 0
    upper = 0
    weights = []
    mean_d = len(A.nonzero())/A.shape[1]
    while len(undo) > 0:
        best = 0
        if now == w:
            now = 0
        if now == 0:
            best = undo.pop()
            bit_list = A[:, best]
            upper = 0
            weights = np.zeros((1, A.shape[0]))
        else:
            for c in bit_list:
                weights += c
            sorted_weights = weights.argsort()[0][::-1]
            upper = np.sum(weights[0][sorted_weights][:mean_d])
            bs = -1
            local_undo_array = undo_array
            for i in range(len(sorted_weights)):
                c = sorted_weights[i]
                to_visit = np.logical_and(A[c], local_undo_array).nonzero()[0]
                flag = False
                for n in to_visit:
                    score = np.sum(A[:, n]*bit_list)
                    if score > bs:
                        best = n
                        bs = score
                    if score >= upper:
                        flag = True
                        break
                    local_undo_array[n] = 0
                if flag:
                    break
                upper = upper - c + sorted_weights[i+mean_d]
            if best == 0:
                best = undo[0]
            bit_list = bit_list + A[:, best]
            undo.remove(best)

        undo_array[best] = 0

        ans.append(best)
        i += 1
        now += 1

    return ans
"""


def bitlist_sim_pairwise(A, w, sim):
    t_num = A.shape[1]
    undo = range(t_num)
    undo_array = np.ones(t_num)

    now = 0
    ans = []
    ret = {}
    i = 0
    pre = 0
    while len(undo) > 0:
        # print len(undo), len(undo_array.nonzero()[0]), pre
        best = 0
        if now == w:
            now = 0
        if now == 0:
            best = undo.pop()
            pre = best
        else:
            best = (undo_array * sim[pre]).argmax()
            # print sim[pre][best]
            if best == 0:
                best = undo.pop()
            else:
                undo.remove(best)
            pre = best

        undo_array[best] = 0

        ans.append(best)
        ret[best] = i
        i += 1
        now += 1

    return ans


def bitlist_sim_pairwise_middle(A, w, sim):
    t_num = A.shape[1]
    undo = range(t_num)
    undo_array = np.ones(t_num)

    bit_list = np.zeros(w + 1)
    now = 0
    ans = []
    ret = {}
    i = 0
    while len(undo) > 0:
        # print len(undo), len(undo_array.nonzero()[0]), pre
        best = 0
        if now == w:
            now = 0
        if now == 0:
            best = undo.pop()
        else:
            best = (undo_array * sim[bit_list[int(now / 2)]]).argmax()
            # print sim[pre][best]
            if best == 0:
                best = undo.pop()
            else:
                undo.remove(best)
        bit_list[now] = best
        undo_array[best] = 0
        ans.append(best)
        ret[best] = i
        i += 1
        now += 1

    return ans


def bitlist_sim_columnwise(A, w, S, k=None):
    t_num = A.shape[1]
    undo = range(t_num)
    undo_array = np.ones(t_num)

    bit_list = []
    now = 0
    ans = []
    ret = {}
    i = 0
    while len(undo) > 0:
        best = 0
        if now == w:
            now = 0
        if now == 0:
            best = undo.pop()
            bit_list = [best]
        else:
            candidate = np.zeros(t_num)
            for i in bit_list:
                if k is None:
                    sorted_s = S[i].nonzero()[0]
                else:
                    sorted_s = S[i].argsort()[::-1][:k]
                for c in sorted_s:
                    candidate[c] += S[i][c]
            rst = undo_array * candidate
            best = rst.argmax()

            if best == 0:
                best = undo.pop()
            else:
                undo.remove(best)

            bit_list.append(best)

        undo_array[best] = 0

        ans.append(best)
        ret[best] = i
        i += 1
        now += 1

    return ans


def bitlist_tree_dfs(Q):
    # quad.QuadTree.get_tids = get_tid
    a = Q.get_tid()
    a_set = set()
    t = []
    for e in a:
        if e not in a_set:
            t.append(e)
            a_set.add(e)
    return t


def bitlist_tree_dfs_depth(Q):
    # quad.QuadTree.get_tids = get_tid
    a = Q.get_tid_depth()
    a.sort(key=lambda x: x[1], reverse=True)
    a_set = set()
    t = []
    for e, n in a:
        if e not in a_set:
            t.append(e)
            a_set.add(e)
    return t


def bitlist_sort(A):
    tups = []
    tups = zip(range(A.shape[1]), [np.sum(A[:, i]) for i in range(A.shape[1])])
    tups.sort(key=lambda x: x[1], reverse=True)
    ans = []
    for t in tups:
        ans.append(t[0])
    return ans


"""
def filled_bits(M, _interval):
    n = 0
    total = 0
    t_num = M.shape[1]
    c_num = M.shape[0]
    for i in xrange(c_num):
        for j in xrange(0,t_num,_interval):
            total += 1
            for k in xrange(_interval):
                if j+k >= t_num: break
                if M[i][j+k] == 1:
                    n += 1
                    break
    return n
"""


def filled_bits(M, _interval):
    bits_set = set()
    cids = M.nonzero()[0]
    t_columns = np.floor(M.nonzero()[1] * 1.0 / _interval)
    for c, t in zip(cids, t_columns):
        bits_set.add((c, t))
    return len(bits_set)


def build_quad_tree(d, max_nodes, max_depth):
    quad.max_id = len(d.tid.unique()) + 5
    quad.g_max_nodes = max_nodes
    quad.g_max_depth = max_depth
    Q = quad.QuadTree()
    d.index = range(len(d))
    for i in range(len(d)):
        x = d.ix[i]
        Q.insert(x.tid, x.x, x.y)
        if i % int((len(d)) / 10) == 0:
            print 'Proccessed %d from %d' % (i, len(d))
    # d.apply(lambda x: Q.insert(x.tid, x.x, x.y), axis=1)
    assert Q.leaf_number() == Q.cover_leaf_num(-180, 180, -90, 90)
    return Q


def generate_matrix(traj_num, max_nodes, dtype, version, max_depth):
    if dtype != '' and dtype[-1] != '_':
        dtype += '_'
    time_interval, Q = read_Q(traj_num, max_nodes, dtype, version, max_depth)
    l = Q.generate_leaf_list()

    print 'Time:', time_interval, 'Leaf node number:', len(l)

    A = np.zeros((len(l), traj_num + 1), dtype=bool)
    for i in range(len(l)):
        for t in l[i]:
            A[i][t] = 1
    if version == 'new':
        sdir = './data/'
    elif version == 'old':
        sdir = './data/_backup/'
    if max_depth == 1024:
        np.save(sdir + ('A_%s%d_%d.npy' % (dtype, traj_num, max_nodes)), A)
    else:
        np.save(sdir + ('A_%s%d_%d_%d.npy' % (dtype, traj_num, max_nodes, max_depth)), A)
    return A


def read_A(traj_num, max_nodes, dtype, version, max_depth):
    if dtype != '' and dtype[-1] != '_':
        dtype += '_'

    if version == 'new':
        sdir = './data/'
    elif version == 'old':
        sdir = './data/_backup/'
    files = os.listdir(sdir)

    if max_depth == 1024:
        file_name = ('A_%s%d_%d.npy' % (dtype, traj_num, max_nodes))
    else:
        file_name = ('A_%s%d_%d_%d.npy' % (dtype, traj_num, max_nodes, max_depth))
    path = sdir + file_name
    if file_name not in files:
        return generate_matrix(traj_num, max_nodes, dtype, version, max_depth)
    A = np.load(path)
    return A


def read_S(traj_num, max_nodes, dtype, func):
    if dtype != '' and dtype[-1] != '_':
        dtype += '_'
    f = open(('./data/S_%s_%s%d_%d.data' % (func[0], dtype, traj_num, max_nodes)), 'r')
    time, S = pickle.load(f)
    f.close()
    return time, S


def read_D(traj_num, dtype, version):
    if dtype != '' and dtype[-1] != '_':
        dtype += '_'

    if version == 'new':
        sdir = './data/'
    elif version == 'old':
        sdir = './data/_backup/'

    file_name = ('Data_%s%d.pickle' % (dtype, traj_num))
    path = sdir + file_name
    d = pd.read_pickle(path)
    d.index = d.tid
    return d


def read_Q(traj_num, max_nodes, dtype, version, max_depth):
    if dtype != '' and dtype[-1] != '_':
        dtype += '_'

    if version == 'new':
        sdir = './data/'
    elif version == 'old':
        sdir = './data/_backup/'
    files = os.listdir(sdir)

    if max_depth == 1024:
        file_name = ('Q_%s%d_%d.pickle' % (dtype, traj_num, max_nodes))
        ql_file_name = ('QL_%s%d_%d.pickle' % (dtype, traj_num, max_nodes))
    else:
        file_name = ('Q_%s%d_%d_%d.pickle' % (dtype, traj_num, max_nodes, max_depth))
        ql_file_name = ('QL_%s%d_%d_%d.pickle' % (dtype, traj_num, max_nodes, max_depth))

    path = sdir + file_name
    if file_name not in files:
        d = read_D(traj_num, dtype, version)

        t1 = time.time()
        Q = build_quad_tree(d, max_nodes, max_depth)
        t2 = time.time()
        time_interval = t2 - t1

        tl = Q.tree_list()
        with open(sdir + ql_file_name, 'w') as f:
            pickle.dump([time_interval, tl], f)

        Q.remove_points()
        f = open(path, 'w')
        pickle.dump([time_interval, Q], f)
        f.close()
        return time_interval, Q

    f = open(path, 'r')
    t, Q = pickle.load(f)
    f.close()
    return t, Q


def read_G(traj_num, depth, dtype, version):
    if dtype != '' and dtype[-1] != '_':
        dtype += '_'
    if version == 'new':
        sdir = './data/'
    elif version == 'old':
        sdir = './data/_backup/'
    files = os.listdir(sdir)
    file_name = ('G_%s%d_%d.pickle' % (dtype, traj_num, depth))
    ql_file_name = ('GL_%s%d_%d.pickle' % (dtype, traj_num, depth))
    path = sdir + file_name
    if file_name in files:
        f = open(path, 'r')
        time_interval, G = pickle.load(f)
        f.close()
    else:
        d = pd.read_pickle(sdir + 'Data_%s%d.pickle' % (dtype, traj_num))
        G = quad.GridTree(depth)
        t1 = time.time()
        G.build(d)
        t2 = time.time()
        time_interval = t2 - t1

        tl = G.tree_list()
        with open(sdir + ql_file_name, 'w') as f:
            pickle.dump([time_interval, tl], f)

        G.remove_points()
        f = open(path, 'w')
        pickle.dump([time_interval, G], f)
        f.close()
    return time_interval, G


def read_AG(traj_num, depth, dtype, version, max_depth):
    if dtype != '' and dtype[-1] != '_':
        dtype += '_'
    if version == 'new':
        sdir = './data/'
    elif version == 'old':
        sdir = './data/_backup/'
    files = os.listdir(sdir)
    file_name = ('AG_%s%d_%d.npy' % (dtype, traj_num, depth))
    path = sdir + file_name
    if file_name in files:
        A = np.load(path)
    else:
        time_interval, G = read_G(traj_num, depth, dtype, version)
        l = G.generate_leaf_list()

        A = np.zeros((len(l), traj_num + 1), dtype=bool)
        for i in range(len(l)):
            for t in l[i]:
                A[i][t] = 1
        np.save(sdir + file_name, A)
    return A


def read_Tl(traj_num, index_para, index_type, dtype, version, max_depth):
    if dtype is not '' and dtype[-1] is not '_':
        dtype += '_'
    if version == 'new':
        sdir = './data/'
    elif version == 'old':
        sdir = './data/_backup/'
    files = os.listdir(sdir)
    if index_type[0] == 'q':
        if max_depth == 1024:
            file_name = ('QL_%s%d_%d.pickle' % (dtype, traj_num, index_para))
        else:
            file_name = ('QL_%s%d_%d_%d.pickle' % (dtype, traj_num, index_para, max_depth))
    elif index_type[0] == 'g':
        file_name = ('GL_%s%d_%d.pickle' % (dtype, traj_num, index_para))
    else:
        print "Index type error!", index_type

    path = sdir + file_name
    if file_name not in files:
        if index_type[0] == 'q':
            itv_time, Q = read_Q(traj_num, index_para, dtype, version, max_depth)
        elif index_type[0] == 'g':
            itv_time, Q = read_G(traj_num, index_para, dtype, version)
    with open(path, 'r') as f:
        return pickle.load(f)


def read_Bl(traj_num, index_para, base, index_type, dtype, version, max_depth):
    if index_type[0] == 'q':
        A = read_A(traj_num, index_para, dtype, version, max_depth)
    elif index_type[0] == 'g':
        A = read_AG(traj_num, index_para, dtype, version, max_depth)
    time_interval, tl = read_Tl(traj_num, index_para, index_type, dtype, version, max_depth)
    t0 = time.time()
    bl = quad.Bitlist(A, base)
    index = quad.Index(tl, bl)
    t = bitlist_tree_dfs_depth(index)
    Ak = A[:, t]
    """
    May bug: Ak.shape != A.shape
    """
    bl = quad.Bitlist(Ak, base, t)
    time_interval = time.time() - t0
    return time_interval, bl


def jaccard(x, y):
    intersection = np.dot(x, y)
    return intersection * 1.0 / (np.sum(np.logical_or(x, y)))


def euclidean(x, y):
    return 1.0 - (np.linalg.norm(x - y) * 1.0 / math.sqrt(len(x)))


def cosine(x, y):
    return np.dot(x, y) / (np.linalg.norm(x) * np.linalg.norm(y))


def manhatten(x, y):
    return 1.0 - (np.sum(np.absolute(x - y)) * 1.0 / len(x))


def similarity(A, sim_func=jaccard):
    t_num = A.shape[1]
    S = np.zeros((t_num, t_num))
    for i in range(1, t_num):
        to_visit = np.zeros(t_num)
        for j in A[:, i].nonzero()[0]:
            to_visit = np.logical_or(to_visit, A[j])
        havent_visit = np.ones(t_num)
        havent_visit[0:i + 1] = 0
        to_visit = np.logical_and(to_visit, havent_visit)
        for j in to_visit.nonzero()[0]:
            S[i][j] = sim_func(A[:, i], A[:, j])
            S[j][i] = S[i][j]
    return S


def dump_similarity(traj_num, max_nodes, dtype, func='jaccard'):
    if dtype != '' and dtype[-1] != '_':
        dtype += '_'
    A = read_A(traj_num, max_nodes, dtype)
    t1 = time.time()
    if func == 'jaccard':
        S = similarity(A, jaccard)
    elif func == 'euclidean':
        S = similarity(A, euclidean)
    elif func == 'cosine':
        S = similarity(A, cosine)
    elif func == 'manhatten':
        S = similarity(A, manhatten)
    else:
        print 'Error'
    t2 = time.time()
    f = open(('./data/S_%s_%s%d_%d.data' % (func[0], dtype, traj_num, max_nodes)), 'w')
    pickle.dump([t2 - t1, S], f)
    f.close()


def get_answer(traj_num, max_nodes, dtype, version='old', max_depth=1024):
    if dtype != '' and dtype[-1] != '_':
        dtype += '_'

    ans = {}
    if version == 'new':
        folder_name = './result/'
    elif version == 'old':
        folder_name = './result/_backup/'

    if max_depth == 1024:
        file_name = ('R_%s%d_%d.txt' % (dtype, traj_num, max_nodes))
    else:
        file_name = ('R_%s%d_%d_%d.txt' % (dtype, traj_num, max_nodes, max_depth))

    if file_name in os.listdir(folder_name):
        f = open(folder_name + file_name, 'r')
        ans = json.load(f)
        f.close()

    A = read_A(traj_num, max_nodes, dtype, version, max_depth)
    ans['leaf_number'] = len(A)
    ans['non_zero_num'] = len(A.nonzero()[0])
    ans['max_depth'] = max_depth
    """
    ans['sim_time_j'], Sj = read_S( traj_num, max_nodes, dtype, 'jaccard')
    ans['sim_time_e'], Se = read_S( traj_num, max_nodes, dtype, 'euclidean')
    ans['sim_time_c'], Sc = read_S( traj_num, max_nodes, dtype, 'c')
    ans['sim_time_m'], Sm = read_S( traj_num, max_nodes, dtype, 'm')
    """

    ans['traj_num'] = traj_num
    ans['max_nodes'] = max_nodes
    if dtype == '':
        t = 'middle'
    else:
        t = dtype[:-1]
    ans['type'] = t

    t_random = range(traj_num + 1)
    random.shuffle(t_random)

    print 'Processing sort'
    t1 = time.time()
    t_sort = bitlist_sort(A)
    t2 = time.time()
    t_sort_time = t2 - t1

    time_interval, tl = read_Tl(traj_num, max_nodes, 'qt', dtype, version, max_depth)
    time_interval, bl = read_Bl(traj_num, max_nodes, 8, 'qt', dtype, version, max_depth)
    index = quad.Index(tl, bl)

    print 'Processing tree dfs'
    t1 = time.time()
    t_tree_dfs = bitlist_tree_dfs(index)
    t2 = time.time()
    t_tree_dfs_time = t2 - t1

    print 'Processing tree dfs depth'
    t1 = time.time()
    t_tree_dfs_depth = bitlist_tree_dfs_depth(index)
    t2 = time.time()
    t_tree_dfs_depth_time = t2 - t1

    for w in [8, 16, 32, 64]:
        if str(w) not in ans.keys():
            ans[str(w)] = {}

        row = ans[str(w)]
        k = 'bits_unsort'
        print 'Processing', k, w
        if k not in row.keys():
            row[k] = filled_bits(A, w)

        k = 'bits_random'
        print 'Processing', k, w
        if k not in row.keys():
            Ak = A[:, t_random]
            row[k] = filled_bits(Ak, w)

        k = 'bits_sort'
        print 'Processing', k, w
        if k not in row.keys():
            Ak = A[:, t_sort]
            row[k] = filled_bits(Ak, w)
        if True:
            row[k + '_time'] = t_sort_time

        k = 'bits_tree_dfs'
        print 'Processing', k, w
        if k not in row.keys():
            Ak = A[:, t_tree_dfs]
            row[k] = filled_bits(Ak, w)
        if True:
            row[k + '_time'] = t_tree_dfs_time

        k = 'bits_tree_dfs_depth'
        if k not in row.keys():
            Ak = A[:, t_tree_dfs_depth]
            row[k] = filled_bits(Ak, w)
        if True:
            row[k + '_time'] = t_tree_dfs_depth_time

        """
        k = 'bits_origin'
        if k not in row.keys():
            t1 = time.time()
            t = bitlist_origin(A, w)
            t2 = time.time()
            Ak = A[:,t]
            row[k] = filled_bits(Ak, w)
            row[k+'_time'] = t2 - t1
        """

        """
        def test(row, k, func, A, w, S):
            if k not in row.keys():
                t1 = time.time()
                t = bitlist_sim_pairwise(A, w, S)
                t2 = time.time()
                Ak = A[:, t]
                row[k] = filled_bits(Ak, w)
                row[k+'_time'] = t2 - t1
            return row

        k = 'bits_jaccard_pairwise'
        row = test(row, k, bitlist_sim_pairwise, A, w, Sj)
        k = 'bits_euclidean_pairwise'
        row = test(row, k, bitlist_sim_pairwise, A, w, Se)
        k = 'bits_cosine_pairwise'
        row = test(row, k, bitlist_sim_pairwise, A, w, Sc)
        k = 'bits_manhattan_pairwise'
        row = test(row, k, bitlist_sim_pairwise, A, w, Sm)

        k = 'bits_jaccard_first'
        row = test(row, k, bitlist_sim_pairwise_middle, A, w, Sj)
        k = 'bits_euclidean_first'
        row = test(row, k, bitlist_sim_pairwise_middle, A, w, Se)
        k = 'bits_cosine_first'
        row = test(row, k, bitlist_sim_pairwise_middle, A, w, Sc)
        k = 'bits_manhattan_first'
        row = test(row, k, bitlist_sim_pairwise_middle, A, w, Sm)

        k = 'bits_jaccard_columnwise'
        row = test(row, k, bitlist_sim_columnwise, A, w, Sj)
        k = 'bits_euclidean_columnwise'
        row = test(row, k, bitlist_sim_columnwise, A, w, Se)
        k = 'bits_cosine_columnwise'
        row = test(row, k, bitlist_sim_columnwise, A, w, Sc)
        k = 'bits_manhattan_columnwise'
        row = test(row, k, bitlist_sim_columnwise, A, w, Sm)
        """

    f = open(folder_name + file_name, 'w')
    s = json.dump(ans, f, indent=True)
    print s
    f.close()


def get_answer_bitlist(traj_num, max_nodes, dtype, version, max_depth):
    if dtype != '' and dtype[-1] != '_':
        dtype += '_'

    ans = {}
    folder_name = './result/'
    if max_depth == 1024:
        file_name = ('R_%s%d_%d.txt' % (dtype, traj_num, max_nodes))
    else:
        file_name = ('R_%s%d_%d_%d.txt' % (dtype, traj_num, max_nodes, max_depth))

    if file_name in os.listdir(folder_name):
        f = open(folder_name + file_name, 'r')
        ans = json.load(f)
        f.close()

    A = read_A(traj_num, max_nodes, dtype, version, max_depth)
    for w in [8, 16, 32, 64]:
        k = 'bits_origin'
        row = ans[str(w)]
        # if k not in row.keys():
        if True:
            t1 = time.time()
            t = bitlist_origin(A, w)
            t2 = time.time()
            Ak = A[:, t]
            row[k] = filled_bits(Ak, w)
            row[k + '_time'] = t2 - t1

    f = open(folder_name + file_name, 'w')
    s = json.dump(ans, f, indent=True)
    print s
    f.close()
